package com.github.blemale.mentoring.concurrency.thread;

import static org.assertj.core.api.Assertions.assertThat;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AkkaExercises {

  record Release(String name, List<CompletableFutureExercises.Pod> pods) {}

  record Pod(String name) {}

  record PodDetail(String name, int cpu, int memory, boolean healthy) {}

  sealed interface Result<S, F> {
    record Success<S, F>(S value) implements Result<S, F> {}

    record Failure<S, F>(F value) implements Result<S, F> {}
  }

  static final class ReleaseClient {
    private static final ScheduledExecutorService EXECUTOR =
        Executors.newSingleThreadScheduledExecutor();

    CompletableFuture<Release> fetchRelease(String name) {
      var promise = new CompletableFuture<Release>();
      EXECUTOR.schedule(
          () -> {
            if (promise.isDone()) return;

            var random = ThreadLocalRandom.current();
            if (random.nextInt(100) < 10) {
              promise.completeExceptionally(new RuntimeException("Error while fetching release"));
            } else {
              var pods =
                  IntStream.range(0, random.nextInt(10, 20))
                      .mapToObj(
                          index ->
                              new CompletableFutureExercises.Pod("%s-%s".formatted(name, index)))
                      .toList();
              promise.complete(new Release(name, pods));
            }
          },
          ThreadLocalRandom.current().nextInt(1_000),
          TimeUnit.MILLISECONDS);
      return promise;
    }
  }

  static final class PodClient extends AbstractActor {

    sealed interface Protocol {
      record GetPodDetail(Pod pod) implements Protocol {}

      record PodDetailSuccess(PodDetail podDetail) implements Protocol {}

      record PodDetailFailure(Throwable error) implements Protocol {}
    }

    @Override
    public Receive createReceive() {
      return receiveBuilder()
          .match(
              Protocol.GetPodDetail.class,
              getPodDetail -> {
                var sender = sender();
                var random = ThreadLocalRandom.current();
                context()
                    .system()
                    .scheduler()
                    .scheduleOnce(
                        Duration.ofMillis(random.nextInt(1_000)),
                        () ->
                            sender.tell(
                                random.nextInt(100) < 30
                                    ? new Protocol.PodDetailFailure(new RuntimeException())
                                    : new Protocol.PodDetailSuccess(
                                        new PodDetail(
                                            getPodDetail.pod().name(),
                                            random.nextInt(10),
                                            random.nextInt(5_000),
                                            random.nextBoolean())),
                                self()),
                        context().dispatcher());
              })
          .build();
    }
  }

  static final class Client extends AbstractActor {

    sealed interface Protocol {
      record GetPodDetails(String releaseName) implements Protocol {}

      record PodDetails(List<Result<PodDetail, Throwable>> results) implements Protocol {}
    }

    @Override
    public Receive createReceive() {
      return null; // TODO
    }
  }

  @Test
  void ex1_fetch_pod_details() {
    var system = ActorSystem.create();
    var client = system.actorOf(Props.create(Client::new));
    var result =
        Patterns.ask(client, new Client.Protocol.GetPodDetails("a-release"), Duration.ofMinutes(1))
            .toCompletableFuture()
            .join();

    assertThat(result)
        .isInstanceOfSatisfying(
            Client.Protocol.PodDetails.class,
            details -> assertThat(details.results()).isNotEmpty());
  }
}
