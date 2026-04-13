package com.concert.booking.controller;

import com.concert.booking.dto.GeneralResponse;
import com.concert.booking.dto.LoginUserRequest;
import com.concert.booking.dto.LoginUserResponse;
import com.concert.booking.dto.RegisterUserRequest;
import com.concert.booking.model.User;
import com.concert.booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<GeneralResponse> register(@RequestBody RegisterUserRequest request) {
        User user = userService.register(request);
        if (!Objects.equals(user.getId(), "")) {
            return ResponseEntity.created(null).body(new GeneralResponse("success create user account"));
        }
        return ResponseEntity.internalServerError().body(new GeneralResponse("failed create user account"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginUserResponse> login(@RequestBody LoginUserRequest request) {
        String token = userService.login(request);
        if (!Objects.equals(token, "")) {
            return ResponseEntity.ok(new LoginUserResponse(token));
        }
        return ResponseEntity.badRequest().body(null);
    }
}