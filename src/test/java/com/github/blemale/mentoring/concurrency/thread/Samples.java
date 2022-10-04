package com.github.blemale.mentoring.concurrency.thread;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
}
