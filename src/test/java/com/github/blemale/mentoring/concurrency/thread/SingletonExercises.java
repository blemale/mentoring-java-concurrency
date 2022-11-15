package com.github.blemale.mentoring.concurrency.thread;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SingletonExercises {

  static final class EagerSingleton {

    static final AtomicInteger INSTANCE_COUNT = new AtomicInteger();

    private EagerSingleton() {
      INSTANCE_COUNT.incrementAndGet();
    }

    static EagerSingleton getInstance() {
      return null;
    }

  }

  @Test
  void implement_an_eager_singleton() {
    assertThat(EagerSingleton.INSTANCE_COUNT.get()).isEqualTo(1);
    assertThat(EagerSingleton.getInstance()).isNotNull();
    assertThat(EagerSingleton.getInstance()).isSameAs(EagerSingleton.getInstance());
    assertThat(EagerSingleton.INSTANCE_COUNT.get()).isEqualTo(1);
  }

  static final class LazySingleton {

    static final AtomicInteger INSTANCE_COUNT = new AtomicInteger();

    private LazySingleton() {
      INSTANCE_COUNT.incrementAndGet();
    }

    static LazySingleton getInstance() {
      return null;
    }

  }

  @Test
  void implement_a_lazy_singleton() {
    assertThat(LazySingleton.INSTANCE_COUNT.get()).isEqualTo(0);
    assertThat(LazySingleton.getInstance()).isNotNull();
    assertThat(LazySingleton.getInstance()).isSameAs(LazySingleton.getInstance());
    assertThat(LazySingleton.INSTANCE_COUNT.get()).isEqualTo(1);
  }


}
