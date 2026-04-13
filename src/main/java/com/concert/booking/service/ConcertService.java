package com.concert.booking.service;

import com.concert.booking.dto.AddConcertRequest;
import com.concert.booking.dto.UpdateConcertRequest;
import com.concert.booking.model.Concert;

import java.util.List;

public interface ConcertService {
    Concert add(AddConcertRequest req);

    Concert updateConcert(String concertId, UpdateConcertRequest request);

    void deleteConcert(String concertId);

    List<Concert> getAllConcerts();

    List<Concert> getConcertAvailable();

    Concert getConcertById(String concertId);
}
