package com.concert.booking.service;

import com.concert.booking.dto.BookingRequest;
import com.concert.booking.model.Concert;
import com.concert.booking.repository.ConcertRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class RaceConditionTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ConcertRepository concertRepo;

    private static final String CONCERT_ID = "concert-race-test";
    private static final int TOTAL_QUOTA   = 10;
    private static final int TOTAL_THREADS = 100; // 100 user berebut 10 tiket

    @BeforeEach
    void setup() {
        redis.delete("ticket:quota:" + CONCERT_ID);

        // Create concert dengan quota 10
        Concert concert = new Concert();
        concert.setId(CONCERT_ID);
        concert.setName("Race Condition Test Concert");
        concert.setQuota(TOTAL_QUOTA);
        concert.setSaleOpen(LocalDateTime.now().minusHours(1));
        concert.setSaleClose(LocalDateTime.now().plusHours(1));
        concertRepo.save(concert);

        // Init quota di Redis
        redis.opsForValue().set(
                "ticket:quota:" + CONCERT_ID,
                String.valueOf(TOTAL_QUOTA));
    }

    @Test
    void shouldNotOversellWhen100UsersBookSimultaneously() throws InterruptedException {
        // Setup
        CountDownLatch startLatch  = new CountDownLatch(1);
        CountDownLatch doneLatch   = new CountDownLatch(TOTAL_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        for (int i = 0; i < TOTAL_THREADS; i++) {
            final String userId = "user-" + i;
            new Thread(() -> {
                try {
                    startLatch.await();

                    BookingRequest req = new BookingRequest();
                    req.setConcertId(CONCERT_ID);
                    req.setUserId(userId);
                    req.setSeats(1);

                    bookingService.reserve(req);
                    successCount.incrementAndGet();

                } catch (ResponseStatusException e) {
                    failCount.incrementAndGet(); // sold out atau error
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);

        System.out.println("Success: " + successCount.get());
        System.out.println("Failed : " + failCount.get());

        assertThat(successCount.get()).isLessThanOrEqualTo(TOTAL_QUOTA); // expected: 10

        assertThat(successCount.get() + failCount.get()).isEqualTo(TOTAL_THREADS); // expected: 100

        String remaining = redis.opsForValue().get("ticket:quota:" + CONCERT_ID);
        assertThat(Integer.parseInt(remaining)).isGreaterThanOrEqualTo(0);
    }

    @AfterEach
    void cleanup() {
        redis.delete("ticket:quota:" + CONCERT_ID);
        concertRepo.deleteById(CONCERT_ID);
    }
}
