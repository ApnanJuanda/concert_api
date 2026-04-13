package com.concert.booking.service.impl;

import com.concert.booking.dto.AddConcertRequest;
import com.concert.booking.dto.UpdateConcertRequest;
import com.concert.booking.model.Concert;
import com.concert.booking.repository.ConcertRepository;
import com.concert.booking.service.ConcertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcertServiceImpl implements ConcertService {

    private final ConcertRepository repository;

    private final StringRedisTemplate redis;

    @Override
    public Concert add(AddConcertRequest req) {
        List<Concert> listExistingConcert = repository.getByName(req.getName());
        if (listExistingConcert != null && !listExistingConcert.isEmpty()) {
            throw new RuntimeException("Concert already registered");
        }

        LocalDateTime now   = LocalDateTime.now();
        Concert newConcert = new Concert(UUID.randomUUID().toString(), req.getName(), req.getVenue(),
                req.getSaleOpen(), req.getSaleClose(), req.getQuota(), req.getEventDate(), now, null);
        return repository.save(newConcert);
    }

    @Override
    public Concert updateConcert(String concertId, UpdateConcertRequest request) {
        Concert concert = repository.findById(concertId)
                .orElseThrow(() -> new RuntimeException("Concert is not found: " + concertId));

        if (!Objects.equals(request.getName(), "")) {
            concert.setName(request.getName());
        }
        if (!Objects.equals(request.getVenue(), "")) {
            concert.setVenue(request.getVenue());
        }
        if (request.getQuota() > 0) {
            concert.setQuota(request.getQuota());
        }
        if (request.getSaleOpen() != null && request.getSaleClose() != null) {
            concert.setSaleOpen(request.getSaleOpen());
            concert.setSaleClose(request.getSaleClose());
        }
        if (request.getEventDate() != null) {
            concert.setEventDate(request.getEventDate());
        }

        LocalDateTime now   = LocalDateTime.now();
        concert.setUpdatedAt(now);
        return repository.save(concert);
    }

    @Override
    public void deleteConcert(String concertId) {
        if (!repository.existsById(concertId)) {
            throw new RuntimeException("Concert is not found: " + concertId);
        }
        repository.deleteById(concertId);
    }

    @Override
    public List<Concert> getAllConcerts() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public List<Concert> getConcertAvailable() {
        List<Concert> availableConcerts = repository.getConcertAvailable(LocalDateTime.now());
        if (availableConcerts != null && !availableConcerts.isEmpty()) {
            availableConcerts.forEach(concert ->
                    CompletableFuture.runAsync(() -> syncQuotaConcertToRedis(concert))
            );
        }
        return repository.getConcertAvailable(LocalDateTime.now());
    }

    @Override
    public Concert getConcertById(String concertId) {
        return repository.findById(concertId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Concert not found: " + concertId));
    }

    private void syncQuotaConcertToRedis(Concert concert) {
        try {
            String quotaKey = "ticket:quota:" + concert.getId();
            String redisVal = redis.opsForValue().get(quotaKey);

            boolean keyNotExists = !Boolean.TRUE.equals(redis.hasKey(quotaKey));
            boolean valueNotSync = redisVal == null || !Objects.equals(concert.getQuota(), Integer.parseInt(redisVal));

            if (keyNotExists || valueNotSync) {
                redis.opsForValue().set(
                        quotaKey,
                        String.valueOf(concert.getQuota()),
                        Duration.ofHours(24));
                log.info("Quota synced: concertId={}, quota={}", concert.getId(), concert.getQuota());
            }
        } catch (Exception e) {
            log.error("Failed to sync quota for concertId={}", concert.getId(), e);
        }
    }
}
