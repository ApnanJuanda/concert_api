package com.concert.booking.service.impl;

import com.concert.booking.dto.LoginUserRequest;
import com.concert.booking.dto.RegisterUserRequest;
import com.concert.booking.model.User;
import com.concert.booking.repository.UserRepository;
import com.concert.booking.security.JwtUtil;
import com.concert.booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public User register(RegisterUserRequest req) {
        if (repository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        String encryptedPassword = encoder.encode(req.getPassword());
        LocalDateTime now   = LocalDateTime.now();
        User newUser = new User(UUID.randomUUID().toString(), req.getName(), req.getEmail(), encryptedPassword, now, null);

        return repository.save(newUser);
    }

    @Override
    public String login(LoginUserRequest req) {
        User user = repository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(req.getPassword(), user.getEncryptedPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return JwtUtil.generateToken(req.getEmail(), "USER");
    }
}
