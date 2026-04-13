package com.concert.booking.service.impl;

import com.concert.booking.dto.BookingJob;
import com.concert.booking.dto.BookingRequest;
import com.concert.booking.dto.BookingResponse;
import com.concert.booking.model.Concert;
import com.concert.booking.repository.ConcertRepository;
import com.concert.booking.service.BookingService;
import com.concert.booking.worker.RedisStreamPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final StringRedisTemplate redis;

    private final RedisScript<Long> reserveScript;
    private final ConcertRepository concertRepo;
    private final RedisStreamPublisher publisher;

    public BookingServiceImpl(StringRedisTemplate redis,
                          @Qualifier("reserveTicketScript") RedisScript<Long> reserveScript,
                          ConcertRepository concertRepo,
                          RedisStreamPublisher publisher) {
        this.redis = redis;
        this.reserveScript = reserveScript;
        this.concertRepo = concertRepo;
        this.publisher = publisher;
    }

    @Override
    public BookingResponse reserve(BookingRequest req) {
        Concert concert = concertRepo.getConcertAvailableById(LocalDateTime.now(), req.getConcertId());
        if (concert == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Concert not found");
        }

        String quotaKey = "ticket:quota:" + req.getConcertId();

        // check redis quota key is exist
        if (!Boolean.TRUE.equals(redis.hasKey(quotaKey))) {
            Concert inv = concertRepo.findByConcertIdWithLock(req.getConcertId())
                    .orElseThrow();

            redis.opsForValue().set(quotaKey,
                    String.valueOf(inv.getQuota()),
                    Duration.ofHours(24));
        }

        long now   = LocalDateTime.now()
                .toEpochSecond(ZoneOffset.UTC);
        long open  = concert.getSaleOpen()
                .toEpochSecond(ZoneOffset.UTC);
        long close = concert.getSaleClose()
                .toEpochSecond(ZoneOffset.UTC);

        Long result = redis.execute(reserveScript,
                List.of(quotaKey),
                String.valueOf(now),
                String.valueOf(open),
                String.valueOf(close),
                String.valueOf(req.getSeats()));

        if (result == -1L)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Outside sale window");
        if (result == -2L) {
            log.error("ini error");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sold out");
        }

        String orderId = UUID.randomUUID().toString();
        BookingJob job = new BookingJob(orderId, req.getUserId(), req.getConcertId(), String.valueOf(req.getSeats()));

        try {
            publisher.publish(job);
        } catch (Exception e) {
            // Rollback quota in redis
            redis.opsForValue().increment(quotaKey);
            log.error("Failed to publish job, quota rolled back", e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Try again later");
        }

        return new BookingResponse(orderId, "PENDING");
    }
}
