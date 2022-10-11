package com.github.blemale.mentoring.concurrency.thread;

import static com.github.blemale.mentoring.concurrency.thread.ThreadUtils.safeInterruptible;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class Samples {

  @Test
  void thread_api() throws InterruptedException {
    var thread =
        new Thread(() -> System.out.println("Hi from " + Thread.currentThread()), "my-thread");
    thread.setDaemon(true);
    thread.start();
    thread.join();
  }

  @Test
  void thread_local_api() throws InterruptedException {
    var id = new AtomicInteger();
    var threadLocal = ThreadLocal.withInitial(id::incrementAndGet);

    Runnable runnable =
        () -> {
          System.out.println("Hi from id " + threadLocal.get());
          threadLocal.set(-threadLocal.get());
          System.out.println("Hi from id " + threadLocal.get());
          threadLocal.remove();
          System.out.println("Hi from id " + threadLocal.get());
        };

    var thread = new Thread(runnable);
    var anotherThread = new Thread(runnable);

    thread.start();
    anotherThread.start();

    thread.join();
    anotherThread.join();
  }

  @Test
  void executor_service_api() throws ExecutionException, InterruptedException {
    var executor = Executors.newCachedThreadPool();

    executor.execute(() -> System.out.printf("Hi from %s%n", Thread.currentThread()));

    executor.submit(() -> System.out.printf("Hi from %s%n", Thread.currentThread())).get();
    System.out.println("Task is done");

    var result =
        executor.submit(
            () -> {
              System.out.printf("Hi from %s%n", Thread.currentThread());
              return ThreadLocalRandom.current().nextInt();
            });
    System.out.printf("Result is %s%n", result.get());

    executor.shutdownNow();
    var successful = executor.awaitTermination(1, TimeUnit.SECONDS);
    System.out.printf("Termination is %s%n", successful ? "successful" : "unsuccessful");
  }

  @Test
  void how_many_thread() {
    var index = new AtomicInteger();
    while (!Thread.interrupted()) {
      var thread =
          new Thread(
              () -> {
                System.out.printf("Starting thread nb %s%n", index.incrementAndGet());
                while (!Thread.currentThread().isInterrupted()) {
                  safeInterruptible(() -> Thread.sleep(1_000));
                }
              });
      thread.setDaemon(true);
      thread.start();
    }
  }

  @Test
  void unprotected_shared_state() throws InterruptedException {
    final class Counter {
      int value = 0;

      void increment() {
        value = value + 1;
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

    System.out.printf("Counter value should be 2000 and is %s%n", counter.value);
  }

  @Test
  void synchronized_api() {
    final class SynchronizedState {
      private static final List<String> STATE = new ArrayList<>();

      static synchronized void addStatic(String value) {
        STATE.add(value);
      }

      static List<String> getStatic() {
        synchronized (SynchronizedState.class) {
          return List.copyOf(STATE);
        }
      }

      private final List<String> state = new ArrayList<>();

      synchronized void add(String value) {
        state.add(value);
      }

      List<String> get() {
        synchronized (this) {
          return List.copyOf(state);
        }
      }
    }
  }
}
