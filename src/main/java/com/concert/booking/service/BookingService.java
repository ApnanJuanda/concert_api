package com.concert.booking.service;

import com.concert.booking.dto.BookingRequest;
import com.concert.booking.dto.BookingResponse;

public interface BookingService {
    BookingResponse reserve(BookingRequest req);
}
