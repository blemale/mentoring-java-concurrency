package com.github.blemale.mentoring.concurrency.thread;

import static com.github.blemale.mentoring.concurrency.thread.ThreadUtils.safeInterruptible;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

  @Test
  void lock_api() {

    final class LockedState {

      private final Lock lock = new ReentrantLock(false /* fairness */);
      private final List<String> state = new ArrayList<>();

      void add(String value) {
        lock.lock();
        try {
          state.add(value);
        } finally {
          lock.unlock();
        }
      }

      List<String> tryGet() {
        if (lock.tryLock()) {
          try {
            return List.copyOf(state);
          } finally {
            lock.unlock();
          }
        } else {
          return null;
        }
      }
    }
  }

  @Test
  void read_write_lock_api() {

    final class LockedState {

      private final ReadWriteLock lock = new ReentrantReadWriteLock(false /* fairness */);
      private final List<String> state = new ArrayList<>();

      void add(String value) {
        lock.writeLock().lock();
        try {
          state.add(value);
        } finally {
          lock.writeLock().unlock();
        }
      }

      List<String> tryGet() {
        if (lock.readLock().tryLock()) {
          try {
            return List.copyOf(state);
          } finally {
            lock.readLock().unlock();
          }
        } else {
          return null;
        }
      }
    }
  }

  @Test
  void semaphore_api() throws InterruptedException {
    var semaphore = new Semaphore(42, true /* fairness*/);

    semaphore.acquire();
    try {
      System.out.println("Accessing a costly resource...");
    } finally {
      semaphore.release();
    }

    if (semaphore.tryAcquire()) {
      try {
        System.out.println("Accessing a costly resource in a non blocking way...");
      } finally {
        semaphore.release();
      }
    }

    semaphore.acquire(3);
    try {
      System.out.println("Accessing a costly resource requiring 3 permits...");
    } finally {
      semaphore.release(3);
    }
  }

  @Test
  void concurrent_map_api() {
    var map = new ConcurrentHashMap<String, Integer>();

    map.put("foo", 1);
    System.out.println(map.get("foo"));
    System.out.println(map.getOrDefault("bar", 0));

    // ConcurrentHashMap Javadoc:
    // The entire method invocation is performed atomically.
    // The supplied function is invoked exactly once per invocation of this method.
    //
    // ConcurrentMap Javadoc:
    // When multiple threads attempt updates,
    // map operations and the remapping function may be called multiple times.
    map.compute("foo", (key, current) -> key.hashCode() + (current == null ? 0 : current));
    map.computeIfAbsent("bar", String::hashCode);
    map.computeIfPresent("foo", (key, current) -> key.hashCode() + current);
    map.merge("foo", 42, Integer::sum);
  }

  @Test
  void queue_api() {
    var queue = new ConcurrentLinkedQueue<String>();

    // Non-blocking, throws exception if full
    queue.add("foo");
    // Non-blocking, returns false if full
    queue.offer("bar");

    // Non-blocking, return null if no element available
    System.out.println(queue.poll());
  }

  @Test
  void blocking_queue_api() throws InterruptedException {
    var blockingQueue = new ArrayBlockingQueue<String>(10);

    // Non-blocking, throws exception if full
    blockingQueue.add("foo");
    // Non-blocking, returns false if full
    blockingQueue.offer("bar");
    // Blocking with timeout, returns false if it cannot offer before timeout
    blockingQueue.offer("baz", 1, TimeUnit.SECONDS);
    // Blocking
    blockingQueue.put("qux");

    // Non-blocking
    System.out.println(blockingQueue.poll());
    // Blocking with timeout
    System.out.println(blockingQueue.poll(1, TimeUnit.SECONDS));
    // Blocking
    System.out.println(blockingQueue.take());
  }
}
