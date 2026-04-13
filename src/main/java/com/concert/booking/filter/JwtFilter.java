package com.concert.booking.filter;

import com.concert.booking.security.JwtUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(4)
@RequiredArgsConstructor
public class JwtFilter implements Filter {


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        String path = req.getRequestURI();
        String authHeader = req.getHeader("Authorization");

        if (path.contains("/register") || path.contains("/login") || path.contains("/api/concert")) {
            chain.doFilter(request, response);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing token");
        }

        String token = authHeader.substring(7);

        try {
            String role = JwtUtil.extractRole(token);
            if (path.contains("/api/admin/concert") && !role.equals("ADMIN")) {
                throw new RuntimeException("Access denied (ADMIN only)");
            }
            if (path.contains("/api/user/concert") && !role.equals("USER")) {
                throw new RuntimeException("Access denied (USER only)");
            }

        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT: ", e);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
