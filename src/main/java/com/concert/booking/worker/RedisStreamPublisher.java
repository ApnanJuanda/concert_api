package com.concert.booking.worker;

import com.concert.booking.dto.BookingJob;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisStreamPublisher {
    private static final String STREAM_KEY = "booking:stream";
    private final StringRedisTemplate redis;

    @PostConstruct
    public void initGroup() {
        try {
            redis.opsForStream()
                    .createGroup(STREAM_KEY, ReadOffset.latest(), "booking-workers");
        } catch (Exception ignored) {
        }
    }

    public void publish(BookingJob job) {
        redis.opsForStream().add(STREAM_KEY, Map.of(
                "orderId", job.getOrderId(),
                "userId", job.getUserId(),
                "concertId", job.getConcertId(),
                "seats", job.getSeats()
        ));
    }
}
