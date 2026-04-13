package com.concert.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "concerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Concert {

    @Id
    private String id;
    private String name;
    private String venue;

    @Column(name = "sale_open")
    private LocalDateTime saleOpen;

    @Column(name = "sale_close")
    private LocalDateTime saleClose;

    private Integer quota;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
