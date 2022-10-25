package com.github.blemale.mentoring.concurrency.thread;

import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SynchronizedExercises {

  @Test
  void ex1_protect_counter_class_with_synchronized_to_be_thread_safe() throws InterruptedException {
    final class Counter {

      private int value = 0;

      void increment() {
        value = value + 1;
      }

      int value() {
        return value;
      }
    }

    var counter = new Counter();

    Runnable increment = () -> IntStream.range(0, 1_000).forEach(__ -> counter.increment());

    var thread1 = new Thread(increment);
    var thread2 = new Thread(increment);

    thread1.start();
    thread2.start();

    thread1.join();
    thread2.join();

    Assertions.assertThat(counter.value()).isEqualTo(2_000);
  }

}
