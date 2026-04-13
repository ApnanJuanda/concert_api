package com.concert.booking.service.impl;

import com.concert.booking.dto.LoginAdminRequest;
import com.concert.booking.dto.RegisterAdminRequest;
import com.concert.booking.model.Admin;
import com.concert.booking.repository.AdminRepository;
import com.concert.booking.security.JwtUtil;
import com.concert.booking.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository repository;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public Admin register(RegisterAdminRequest req) {
        if (repository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        String encryptedPassword = encoder.encode(req.getPassword());
        LocalDateTime now   = LocalDateTime.now();
        Admin newAdmin = new Admin(UUID.randomUUID().toString(), req.getName(), req.getEmail(), encryptedPassword, now, null);

        return repository.save(newAdmin);
    }

    @Override
    public String login(LoginAdminRequest req) {
        Admin admin = repository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!encoder.matches(req.getPassword(), admin.getEncryptedPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return JwtUtil.generateToken(req.getEmail(), "ADMIN");
    }
}