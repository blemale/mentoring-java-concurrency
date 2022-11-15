package com.github.blemale.mentoring.concurrency.thread;

import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ReadWriteLockExercises {

  @Test
  void ex1_protect_counter_class_with_read_write_lock_to_be_thread_safe()
      throws InterruptedException {
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
    Runnable read = () -> System.out.println(
        IntStream.range(0, 1_000).map(__ -> counter.value()).sum());

    var thread1 = new Thread(increment);
    var thread2 = new Thread(increment);
    var thread3 = new Thread(read);
    var thread4 = new Thread(read);

    thread1.start();
    thread2.start();
    thread3.start();
    thread4.start();

    thread1.join();
    thread2.join();
    thread3.join();
    thread4.join();

    Assertions.assertThat(counter.value()).isEqualTo(2_000);
  }

}
