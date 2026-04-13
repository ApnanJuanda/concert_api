package com.concert.booking.worker;

import com.concert.booking.dto.BookingJob;
import com.concert.booking.model.Concert;
import com.concert.booking.model.Order;
import com.concert.booking.repository.ConcertRepository;
import com.concert.booking.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamWorker implements SmartLifecycle {

    private static final String STREAM_KEY  = "booking:stream";
    private static final String GROUP_NAME  = "booking-workers";
    private static final String CONSUMER_ID =
            "worker-" + UUID.randomUUID().toString().substring(0, 8);

    private final StringRedisTemplate redis;
    private final ConcertRepository concertRepo;
    private final OrderRepository orderRepo;
    private final TransactionTemplate txTemplate;

    private final ExecutorService executor = new ThreadPoolExecutor(
            10,
            10,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100),
            r -> {
                Thread t = new Thread(r);
                t.setName("booking-worker-" + t.getId());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
        initConsumerGroup();
        for (int i = 0; i < 10; i++) {
            executor.submit(this::consumeLoop);
        }
        log.info("RedisStreamWorker started — consumer: {}", CONSUMER_ID);
    }


    @Override
    public void stop() {
        log.info("RedisStreamWorker stopping...");
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("RedisStreamWorker stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void initConsumerGroup() {
        try {
            redis.opsForStream()
                    .createGroup(STREAM_KEY, ReadOffset.latest(), GROUP_NAME);
            log.info("Consumer group '{}' created", GROUP_NAME);
        } catch (Exception e) {
            log.debug("Consumer group already exists: {}", e.getMessage());
        }
    }

    private void consumeLoop() {
        while (running) {
            try {
                List<MapRecord<String, Object, Object>> messages =
                        redis.opsForStream().read(
                                Consumer.from(GROUP_NAME, CONSUMER_ID),
                                StreamReadOptions.empty()
                                        .count(10)
                                        .block(Duration.ofSeconds(2)),
                                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

                if (messages == null || messages.isEmpty()) continue;

                for (MapRecord<String, Object, Object> msg : messages) {
                    processMessage(msg);
                }

            } catch (Exception e) {
                if (!running) {
                    log.info("Consumer loop stopping gracefully");
                    break;
                }
                log.error("Consumer loop error, retrying in 1s...", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processMessage(MapRecord<String, Object, Object> msg) {
        BookingJob job = parseJob(msg.getValue());

        int seats = Integer.parseInt(job.getSeats());

        try {
            txTemplate.execute(status -> {
                Concert concert = concertRepo
                        .findByConcertIdWithLock(job.getConcertId())
                        .orElseThrow(() -> new RuntimeException(
                                "Concert not found: " + job.getConcertId()));

                if (concert.getQuota() < seats) {
                    redis.opsForValue().increment(
                            "ticket:quota:" + job.getConcertId(),
                            seats);
                    log.warn("Inventory conflict, order {} rejected", job.getOrderId());
                    return null;
                }

                concert.setQuota(concert.getQuota() - seats);
                concertRepo.save(concert);

                orderRepo.save(Order.builder()
                        .id(job.getOrderId())
                        .userId(job.getUserId())
                        .concertId(job.getConcertId())
                        .status("CONFIRMED")
                        .createdAt(LocalDateTime.now())
                        .build());

                return null;
            });

            redis.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, msg.getId());
            log.info("Order {} confirmed", job.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process order {}, will retry", job.getOrderId(), e);
        }
    }

    private BookingJob parseJob(Map<Object, Object> vals) {
        return new BookingJob(
                (String) vals.get("orderId"),
                (String) vals.get("userId"),
                (String) vals.get("concertId"),
                (String) vals.get("seats")
        );
    }
}