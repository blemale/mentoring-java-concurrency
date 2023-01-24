package com.github.blemale.mentoring.concurrency.thread;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CompletableFutureExercises {

  record Release(String name, List<Pod> pods) {}

  record Pod(String name) {}

  record PodDetail(String name, int cpu, int memory, boolean healthy) {}

  sealed interface Result<S, F> {
    record Success<S, F>(S value) implements Result<S, F> {}

    record Failure<S, F>(F value) implements Result<S, F> {}
  }

  static final class ReleaseClient {
    private static final ScheduledExecutorService EXECUTOR =
        Executors.newSingleThreadScheduledExecutor();

    void fetchRelease(String name, BiConsumer<Release, Throwable> callback) {
      EXECUTOR.schedule(
          () -> {
            var random = ThreadLocalRandom.current();
            if (random.nextInt(100) < 10) {
              callback.accept(null, new RuntimeException("Error while fetching release"));
            } else {
              var pods =
                  IntStream.range(0, random.nextInt(10, 20))
                      .mapToObj(index -> new Pod("%s-%s".formatted(name, index)))
                      .toList();
              callback.accept(new Release(name, pods), null);
            }
          },
          ThreadLocalRandom.current().nextInt(1_000),
          TimeUnit.MILLISECONDS);
    }
  }

  static final class BlockingPodClient {

    PodDetail fetchPodDetail(Pod pod) {
      var random = ThreadLocalRandom.current();
      ThreadUtils.safeInterruptible(() -> Thread.sleep(random.nextInt(1_000)));
      if (random.nextInt(100) < 30) {
        throw new RuntimeException("Error while fetching pod detail");
      }
      return new PodDetail(
          pod.name(), random.nextInt(10), random.nextInt(5_000), random.nextBoolean());
    }
  }

  CompletableFuture<List<Result<PodDetail, Throwable>>> fetchPodDetails(String releaseName) {
    // TODO: Implement this method
    return null;
  }

  @Test
  void ex1_fetch_pod_details() {
    var result = fetchPodDetails("a-release").join();

    System.out.println(result);
    assertThat(result).isNotEmpty();
  }
}
