package com.concert.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingJob {
    private String orderId;
    private String userId;
    private String concertId;
    private String seats;
}
