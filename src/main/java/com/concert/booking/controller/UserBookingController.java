package com.concert.booking.controller;

import com.concert.booking.dto.BookingRequest;
import com.concert.booking.dto.BookingResponse;
import com.concert.booking.model.Concert;
import com.concert.booking.repository.ConcertRepository;
import com.concert.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/user/concert")
@RequiredArgsConstructor
public class UserBookingController {

    private final BookingService bookingService;
    private final ConcertRepository concertRepo;

    @GetMapping
    public List<Concert> list() {
        return concertRepo.findAll();
    }

    @PostMapping("/{concertId}/book")
    public ResponseEntity<BookingResponse> book(
            @PathVariable String         concertId,
            @RequestHeader("X-User-ID") String userId,
            @RequestBody @Valid BookingRequest req) {

        req.setConcertId(concertId);
        req.setUserId(userId);

        BookingResponse result = bookingService.reserve(req);
        return ResponseEntity.accepted().body(result);
    }
}

