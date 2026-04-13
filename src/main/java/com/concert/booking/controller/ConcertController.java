package com.concert.booking.controller;

import com.concert.booking.model.Concert;
import com.concert.booking.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/concert")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @GetMapping("")
    public ResponseEntity<List<Concert>> getAllConcert() {
        List<Concert> concerts = concertService.getAllConcerts();
        if (concerts == null || concerts.isEmpty()) {
            throw new RuntimeException("Concert is not found");
        }
        return ResponseEntity.ok().body(concerts);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Concert>> getConcertAvailable() {
        List<Concert> concerts = concertService.getConcertAvailable();
        if (concerts == null || concerts.isEmpty()) {
            throw new RuntimeException("Concert is not found");
        }
        return ResponseEntity.ok().body(concerts);
    }

    @GetMapping("/{concertId}")
    public ResponseEntity<Concert> getConcertById(@PathVariable String concertId) {
        return ResponseEntity.ok(concertService.getConcertById(concertId));
    }
}
