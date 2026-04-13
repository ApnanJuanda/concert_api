package com.concert.booking.service;

import com.concert.booking.dto.LoginUserRequest;
import com.concert.booking.dto.RegisterUserRequest;
import com.concert.booking.model.User;

public interface UserService {
    User register(RegisterUserRequest req);

    String login(LoginUserRequest req);
}
