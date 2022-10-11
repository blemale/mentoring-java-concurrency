package com.github.blemale.mentoring.concurrency.thread;

import static com.github.blemale.mentoring.concurrency.thread.ThreadUtils.synchronousIoCall;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ThreadLocalExercises {

  @Test
  void ex_1_propagate_a_transaction_id_using_thread_local() {
    var tasks = tasksWithTransactionId();
  }

  static final class TransactionId {
    static int get() {
      return 0;
    }
  }

  Stream<Runnable> tasksWithTransactionId() {
    return IntStream.range(0, 1_000)
        .mapToObj(
            id ->
                () -> {
                  System.out.printf(
                      "Running task %s with transaction id %s...%n", id, TransactionId.get());
                  var result = synchronousIoCall();
                  System.out.printf(
                      "Task %s with transaction id %s is done with result %s%n",
                      id, TransactionId.get(), result);
                });
  }
}
