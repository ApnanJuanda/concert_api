package com.concert.booking.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@Order(3)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private static final Duration TTL = Duration.ofHours(24);

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (!"POST".equals(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String path = req.getRequestURI();
        if (path.contains("/api/admin") || path.contains("/register") || path.contains("/login")) {
            chain.doFilter(req, res);
            return;
        }

        String idemKey = req.getHeader("Idempotency-Key");
        if (idemKey == null || idemKey.isBlank()) {
            res.setStatus(400);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"idempotency_key_required\"}");
            return;
        }

        String lockKey = "idem:" + idemKey;
        String respKey = lockKey + ":resp";

        Boolean isNew = redis.opsForValue().setIfAbsent(lockKey, "processing", TTL);

        if (Boolean.FALSE.equals(isNew)) {
            String cached = redis.opsForValue().get(respKey);
            res.setContentType("application/json");
            if (cached != null) {
                res.setHeader("X-Idempotent-Replayed", "true");
                res.getWriter().write(cached);
            } else {
                res.setStatus(409);
                res.getWriter().write("{\"error\":\"duplicate_request\"}");
            }
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(res);
        chain.doFilter(req, wrapped);

        if (wrapped.getStatus() < 500) {
            String body = new String(wrapped.getContentAsByteArray(), StandardCharsets.UTF_8);
            redis.opsForValue().set(respKey, body, TTL);
        }

        wrapped.copyBodyToResponse();
    }
}
