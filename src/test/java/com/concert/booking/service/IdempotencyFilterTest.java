package com.concert.booking.service;

import com.concert.booking.model.Concert;
import com.concert.booking.repository.ConcertRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdempotencyFilterTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ConcertRepository concertRepo;

    private static final String CONCERT_ID    = "concert-idem-test";
    private static final String IDEMPOTENCY_KEY = "idem-key-" + UUID.randomUUID();

    @BeforeEach
    void setup() {
        redis.delete("ticket:quota:" + CONCERT_ID);
        redis.delete("idem:" + IDEMPOTENCY_KEY);
        redis.delete("idem:" + IDEMPOTENCY_KEY + ":resp");

        Concert concert = new Concert();
        concert.setId(CONCERT_ID);
        concert.setName("Idempotency Test Concert");
        concert.setQuota(100);
        concert.setSaleOpen(LocalDateTime.now().minusHours(1));
        concert.setSaleClose(LocalDateTime.now().plusHours(1));
        concertRepo.save(concert);

        redis.opsForValue().set("ticket:quota:" + CONCERT_ID, "100");
    }

    @Test
    void shouldProcessOnlyOnceForSameIdempotencyKey() throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-User-ID", "user-idem-001");
        headers.set("Idempotency-Key", IDEMPOTENCY_KEY);
        headers.set("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcG5hbmp1YW5kYUBtYWlsc2FjLmNvbSIsInJvbGUiOiJVU0VSIiwiaWF0IjoxNzc2MDg0OTgwLCJleHAiOjE3NzYxNzEzODB9.WmJdL8M6se5JcAQTknE_4EHMcwGmMaShIMU_d_Ft7Uo");

        String body = """
                {
                    "concertId": "%s",
                    "seats": 1
                }
                """.formatted(CONCERT_ID);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // Request pertama — harus diproses
        ResponseEntity<String> response1 = restTemplate.postForEntity(
                "/api/user/concert/" + CONCERT_ID + "/book", request, String.class);

        // Request kedua — key sama, harus return cached response
        ResponseEntity<String> response2 = restTemplate.postForEntity(
                "/api/user/concert/" + CONCERT_ID + "/book", request, String.class);

        // Request ketiga — key sama, harus return cached response
        ResponseEntity<String> response3 = restTemplate.postForEntity(
                "/api/user/concert/" + CONCERT_ID + "/book", request, String.class);

        assertThat(response1.getStatusCode().value()).isEqualTo(202);
        assertThat(response2.getStatusCode().value()).isEqualTo(200);
        assertThat(response3.getStatusCode().value()).isEqualTo(200);

        assertThat(response2.getBody()).isEqualTo(response1.getBody());
        assertThat(response3.getBody()).isEqualTo(response1.getBody());

        assertThat(response2.getHeaders().getFirst("X-Idempotent-Replayed")).isEqualTo("true");
        assertThat(response3.getHeaders().getFirst("X-Idempotent-Replayed")).isEqualTo("true");

        String remaining = redis.opsForValue().get("ticket:quota:" + CONCERT_ID);
        assertThat(Integer.parseInt(remaining)).isEqualTo(99);
    }

    @AfterEach
    void cleanup() {
        redis.delete("ticket:quota:" + CONCERT_ID);
        redis.delete("idem:" + IDEMPOTENCY_KEY);
        redis.delete("idem:" + IDEMPOTENCY_KEY + ":resp");
        concertRepo.deleteById(CONCERT_ID);
    }
}
