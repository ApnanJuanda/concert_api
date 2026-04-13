package com.concert.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConcertRequest {

    private String name;

    private String venue;

    private LocalDateTime saleOpen;

    private LocalDateTime saleClose;

    private Integer quota;

    private LocalDateTime eventDate;
}