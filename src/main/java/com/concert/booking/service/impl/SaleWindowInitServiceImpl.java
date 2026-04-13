package com.concert.booking.service.impl;

import com.concert.booking.model.Concert;
import com.concert.booking.repository.ConcertRepository;
import com.concert.booking.service.SaleWindowInitService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleWindowInitServiceImpl implements SaleWindowInitService {
    private final StringRedisTemplate redis;
    private final ConcertRepository concertRepo;


    @Override
    @Transactional
    public void init(String concertId) {
        Concert concert = concertRepo.findById(concertId)
                .orElseThrow(() -> new RuntimeException("Concert not found"));

        Concert inv = concertRepo.findByConcertIdWithLock(concertId)
                .orElseThrow();

        String quotaKey = "ticket:quota:" + concertId;

        redis.opsForValue().set(quotaKey,
                String.valueOf(inv.getQuota()),
                Duration.ofHours(24));

        log.info("Sale window initialized: concertId={}, quota={}",
                concertId, inv.getQuota());
    }
}
