package com.concert.booking.filter;

import com.concert.booking.model.Concert;
import com.concert.booking.repository.ConcertRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@Order(2)
@RequiredArgsConstructor
public class TimeWindowFilter extends OncePerRequestFilter {
    private final ConcertRepository concertRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (!req.getRequestURI().contains("/book")) {
            chain.doFilter(req, res);
            return;
        }

        String[] parts = req.getRequestURI().split("/");
        String concertId = parts.length >= 4 ? parts[4] : "";

        LocalDateTime now = LocalDateTime.now();
        Concert concert = concertRepo.getConcertAvailableById(now, concertId);
        if (concert == null) {
            sendError(res, 404, "concert_not_found");
            return;
        }

        if (now.isBefore(concert.getSaleOpen())) {
            sendError(res, 403, "sale_not_open_yet");
            return;
        }
        if (now.isAfter(concert.getSaleClose())) {
            sendError(res, 410, "sale_closed");
            return;
        }

        chain.doFilter(req, res);
    }

    private void sendError(HttpServletResponse res, int status, String code)
            throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"" + code + "\"}");
    }
}
