package com.github.blemale.mentoring.concurrency.thread;

import static com.github.blemale.mentoring.concurrency.thread.ThreadUtils.synchronousIoCall;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ThreadExercises {

  @Test
  void ex_1_handle_tasks_concurrently_using_threads() {
    var tasks = tasks();
  }

  @Test
  void ex_2_compute_array_sum_in_parallel_using_at_least_2_threads() {
    var array =
        IntStream.generate(() -> ThreadLocalRandom.current().nextInt(100))
            .limit(1_000_000)
            .toArray();
  }

  Stream<Runnable> tasks() {
    return IntStream.range(0, 1_000)
        .mapToObj(
            id ->
                () -> {
                  System.out.printf(
                      "Running task %s on thread %s...%n", id, Thread.currentThread());
                  var result = synchronousIoCall();
                  System.out.printf("Task %s is done with result %s%n", id, result);
                });
  }
}
