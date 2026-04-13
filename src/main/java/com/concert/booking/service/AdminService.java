package com.concert.booking.service;

import com.concert.booking.dto.LoginAdminRequest;
import com.concert.booking.dto.RegisterAdminRequest;
import com.concert.booking.model.Admin;

public interface AdminService {
    Admin register(RegisterAdminRequest req);

    String login(LoginAdminRequest req);
}