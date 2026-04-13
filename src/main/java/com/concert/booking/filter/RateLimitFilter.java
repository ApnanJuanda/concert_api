package com.concert.booking.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final RedisScript<Long> rateLimitScript;
    private static final int MAX_REQ_PER_SEC = 100;

    public RateLimitFilter(StringRedisTemplate redis,
                           @Qualifier("rateLimitScript") RedisScript<Long> rateLimitScript) {
        this.redis = redis;
        this.rateLimitScript = rateLimitScript;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        if (path.contains("/api/admin")) {
            chain.doFilter(req, res);
            return;
        }

        String key = "rl:global:" + Instant.now().getEpochSecond();

        Long ok = redis.execute(rateLimitScript,
                List.of(key),
                String.valueOf(MAX_REQ_PER_SEC));

        if (ok == null || ok == 0L) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"rate_limit_exceeded\"}");
            return;
        }

        chain.doFilter(req, res);

    }
}
