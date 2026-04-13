package com.concert.booking.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitFilterTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redis;

    private static final String USER_ID = "user-ratelimit-test";

    @BeforeEach
    void setup() {
        String pattern = "rl:global" + ":*";
        redis.keys(pattern).forEach(redis::delete);
    }

    @Test
    void shouldAllow100RequestsPerSecond() throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-ID", USER_ID);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(150);

        int totalRequests = 150;
        CountDownLatch readyLatch = new CountDownLatch(totalRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    ResponseEntity<String> response = restTemplate.exchange(
                            "/api/concert/available",
                            HttpMethod.GET,
                            request,
                            String.class
                    );

                    if (response.getStatusCode().value() == 429) {
                        rejectedCount.incrementAndGet();
                    } else {
                        allowedCount.incrementAndGet();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.await();


        System.out.println("Allowed : " + allowedCount.get());
        System.out.println("Rejected: " + rejectedCount.get());

        assertThat(allowedCount.get()).isLessThanOrEqualTo(100);

        assertThat(rejectedCount.get()).isGreaterThan(0);
    }

    @Test
    void shouldResetAfterOneSecond() throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-ID", USER_ID);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        for (int i = 0; i < 100; i++) {
            restTemplate.exchange("/api/concert/available", HttpMethod.GET, request, String.class);
        }

        ResponseEntity<String> rejected = restTemplate.exchange(
                "/api/concert/available", HttpMethod.GET, request, String.class);
        assertThat(rejected.getStatusCode().value()).isEqualTo(429);

        Thread.sleep(1100);

        ResponseEntity<String> allowed = restTemplate.exchange(
                "/api/concert/available", HttpMethod.GET, request, String.class);
        assertThat(allowed.getStatusCode().value()).isNotEqualTo(429);
    }

    @AfterEach
    void cleanup() {
        String pattern = "rl:global" + ":*";
        Set<String> keys = redis.keys(pattern);
        if (keys != null) redis.delete(keys);
    }
}
