package com.concert.booking.controller;

import com.concert.booking.dto.AddConcertRequest;
import com.concert.booking.dto.GeneralResponse;
import com.concert.booking.dto.LoginAdminRequest;
import com.concert.booking.dto.LoginAdminResponse;
import com.concert.booking.dto.RegisterAdminRequest;
import com.concert.booking.dto.UpdateConcertRequest;
import com.concert.booking.model.Admin;
import com.concert.booking.model.Concert;
import com.concert.booking.service.AdminService;
import com.concert.booking.service.ConcertService;
import com.concert.booking.service.SaleWindowInitService;
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
@RequestMapping("api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SaleWindowInitService initService;

    private final AdminService adminService;

    private final ConcertService concertService;

    @PostMapping("/register")
    public ResponseEntity<GeneralResponse> register(@RequestBody RegisterAdminRequest request) {
        Admin admin = adminService.register(request);
        if (!Objects.equals(admin.getId(), "")) {
            return ResponseEntity.created(null).body(new GeneralResponse("success create admin account"));
        }
        return ResponseEntity.internalServerError().body(new GeneralResponse("failed create admin account"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginAdminResponse> login(@RequestBody LoginAdminRequest request) {
        String token = adminService.login(request);
        if (!Objects.equals(token, "")) {
            return ResponseEntity.ok(new LoginAdminResponse(token));
        }
        return ResponseEntity.badRequest().body(null);
    }

    @PostMapping("/concerts/{concertId}/init-sale")
    public ResponseEntity<String> initSale(@PathVariable String concertId) {
        initService.init(concertId);
        return ResponseEntity.ok("Sale window initialized");
    }
}
