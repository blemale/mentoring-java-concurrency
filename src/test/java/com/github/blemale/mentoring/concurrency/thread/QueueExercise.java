package com.github.blemale.mentoring.concurrency.thread;

import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class QueueExercise {

  static final class Scheduler implements Runnable {

    private static final AtomicInteger TASK_ID = new AtomicInteger();
    private final Queue<Runnable> queue;

    Scheduler(Queue<Runnable> queue) {
      this.queue = queue;
    }

    private Runnable newTask() {
      var id = TASK_ID.incrementAndGet();
      return () -> {
        System.out.printf("Starting task %s...%n", id);
        ThreadUtils.safeInterruptible(
            () -> Thread.sleep(ThreadLocalRandom.current().nextInt(1_000)));
        System.out.printf("Task %s finished%n", id);
      };
    }

    @Override
    public void run() {
      // TODO: Should produce a new task every second
    }
  }

  static final class Worker implements Runnable {

    private final Queue<Runnable> queue;

    Worker(Queue<Runnable> queue) {
      this.queue = queue;
    }

    @Override
    public void run() {
      // TODO: Should consume task
    }
  }

  @Test
  void ex1_implement_a_work_scheduling_system() throws InterruptedException {
    Queue<Runnable> queue = null;

    var scheduler = new Scheduler(queue);
    var workers = IntStream.range(0, 10).mapToObj(__ -> new Worker(queue)).toList();

    var executor = Executors.newCachedThreadPool();
    executor.execute(scheduler);
    workers.forEach(executor::execute);

    Thread.sleep(10_000);

    executor.shutdownNow();
    Assertions.assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
  }

}
