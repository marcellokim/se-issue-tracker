package com.github.marcellokim.issuetracker.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("sequential issue id provider")
class SequentialIssueIdProviderTest {

    @Test
    @DisplayName("generates unique ids under concurrent access")
    void generatesUniqueIdsUnderConcurrentAccess() throws InterruptedException {
        SequentialIssueIdProvider provider = new SequentialIssueIdProvider();
        int threads = 32;
        int idsPerThread = 500;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Set<String> ids = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int thread = 0; thread < threads; thread++) {
            executor.execute(() -> {
                try {
                    start.await();
                    IntStream.range(0, idsPerThread)
                            .mapToObj(ignored -> provider.nextIssueId())
                            .forEach(ids::add);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(threads * idsPerThread, ids.size());
    }
}
