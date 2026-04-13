package com.concert.booking.controller;

import com.concert.booking.dto.AddConcertRequest;
import com.concert.booking.dto.UpdateConcertRequest;
import com.concert.booking.model.Concert;
import com.concert.booking.service.ConcertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("api/admin/concert")
@RequiredArgsConstructor
public class AdminConcertController {

    private final ConcertService concertService;

    @PostMapping("/add")
    public ResponseEntity<Concert> addConcert(@RequestBody AddConcertRequest request) {
        Concert concert = concertService.add(request);
        if (!Objects.equals(concert.getId(), "")) {
            return ResponseEntity.created(null).body(concert);
        }
        return ResponseEntity.internalServerError().body(null);
    }

    @PutMapping("/{concertId}")
    public ResponseEntity<Concert> updateConcert(@PathVariable String concertId,
                                              @Valid @RequestBody UpdateConcertRequest request) {
        return ResponseEntity.ok(concertService.updateConcert(concertId, request));
    }

    @DeleteMapping("/{concertId}")
    public ResponseEntity<Void> deleteConcert(@PathVariable String concertId) {
        concertService.deleteConcert(concertId);
        return ResponseEntity.noContent().build();
    }
}
