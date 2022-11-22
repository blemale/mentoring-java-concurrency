package com.github.blemale.mentoring.concurrency.thread;

import static com.github.blemale.mentoring.concurrency.thread.ThreadUtils.safeInterruptible;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SemaphoreExercises {

  final static class ConcurrencyLimit {

    ConcurrencyLimit(int maxConcurrency) {
    }

    void run(Runnable runnable) {
      runnable.run();
    }

    public boolean tryRun(Runnable runnable) {
      runnable.run();
      return true;
    }
  }

  @Test
  void ex1_control_concurrent_calls_using_a_semaphore() {
    var currentConcurrency = new AtomicInteger();
    var maxConcurrency = new AtomicInteger();
    var concurrencyLimit = new ConcurrencyLimit(25);

    Runnable call = () -> {
      var current = currentConcurrency.incrementAndGet();
      maxConcurrency.accumulateAndGet(current, Math::max);
      System.out.printf("Starting %s concurrent call...\n", current);
      safeInterruptible(() -> Thread.sleep(1_000));
      currentConcurrency.decrementAndGet();
    };

    Runnable runnable = () -> concurrencyLimit.run(call);
    var threads = IntStream.range(0, 100).mapToObj(__ -> new Thread(runnable)).toList();
    threads.forEach(Thread::start);
    threads.forEach(thread -> safeInterruptible(thread::join));

    assertThat(maxConcurrency.get()).isLessThanOrEqualTo(25);
  }

  @Test
  void ex2_control_concurrent_calls_using_a_semaphore_in_a_non_blocking_way() {
    var currentConcurrency = new AtomicInteger();
    var maxConcurrency = new AtomicInteger();
    var concurrencyLimit = new ConcurrencyLimit(25);

    Runnable call = () -> {
      var current = currentConcurrency.incrementAndGet();
      maxConcurrency.accumulateAndGet(current, Math::max);
      System.out.printf("Starting %s concurrent call...\n", current);
      safeInterruptible(() -> Thread.sleep(1_000));
      currentConcurrency.decrementAndGet();
    };

    Runnable runnable = () -> {
      if (!concurrencyLimit.tryRun(call)) {
        System.out.println("Too much concurrent calls");
      }
    };
    var threads = IntStream.range(0, 100).mapToObj(__ -> new Thread(runnable)).toList();
    threads.forEach(Thread::start);
    threads.forEach(thread -> safeInterruptible(thread::join));

    assertThat(maxConcurrency.get()).isLessThanOrEqualTo(25);
  }

}
