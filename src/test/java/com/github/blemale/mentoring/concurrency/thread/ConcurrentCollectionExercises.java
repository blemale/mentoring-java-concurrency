package com.github.blemale.mentoring.concurrency.thread;

import static com.github.blemale.mentoring.concurrency.thread.ThreadUtils.safeInterruptible;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ConcurrentCollectionExercises {

  static final class ServerRegistry {

    private final Map<String, List<String>> serverByCluster = new HashMap<>();

    void addServer(String cluster, String server) {
      var servers = serverByCluster.get(cluster);
      if (servers == null) {
        servers = new ArrayList<>();
        serverByCluster.put(cluster, servers);
      }
      servers.add(server);
    }

    List<String> serversOf(String cluster) {
      var servers = serverByCluster.get(cluster);
      return servers == null ? List.of() : servers;
    }

  }

  @Test
  void ex1_make_server_registry_thread_safe_using_concurrent_collections() {
    var registry = new ServerRegistry();
    var failures = new AtomicInteger();

    Runnable cluster1 = () -> {
      try {
        registry.addServer("cluster1", UUID.randomUUID().toString());
        var hash = 0;
        for (var server : registry.serversOf("cluster1")) {
          hash += server.hashCode();
        }
        System.out.println(hash);
      } catch (Exception __) {
        failures.incrementAndGet();
      }
    };

    Runnable cluster2 = () -> {
      try {
        registry.addServer("cluster2", UUID.randomUUID().toString());
        var hash = 0;
        for (var server : registry.serversOf("cluster2")) {
          hash += server.hashCode();
        }
        System.out.println(hash);
      } catch (Exception __) {
        failures.incrementAndGet();
      }
    };

    var threads = Stream.concat(IntStream.range(0, 100).mapToObj(__ -> new Thread(cluster1)),
        IntStream.range(0, 100).mapToObj(__ -> new Thread(cluster2))).toList();
    threads.forEach(Thread::start);
    threads.forEach(thread -> safeInterruptible(thread::join));

    assertThat(registry.serversOf("cluster1")).hasSize(100);
    assertThat(registry.serversOf("cluster2")).hasSize(100);
    assertThat(failures.get()).isEqualTo(0);
  }

}
