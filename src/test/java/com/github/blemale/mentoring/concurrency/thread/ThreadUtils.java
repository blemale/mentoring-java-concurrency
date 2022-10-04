package com.github.blemale.mentoring.concurrency.thread;

import java.util.concurrent.ThreadLocalRandom;

final class ThreadUtils {

    interface InterruptibleRunnable {
        void run() throws InterruptedException;
    }

    static void safeInterruptible(InterruptibleRunnable runnable) {
        try {
            runnable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static int synchronousIoCall() {
        safeInterruptible(() -> Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1_000)));
        return ThreadLocalRandom.current().nextInt();
    }

}
