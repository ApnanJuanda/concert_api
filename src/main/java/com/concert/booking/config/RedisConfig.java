package com.concert.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfig {

    // Script to prevent race condition
    @Bean
    public RedisScript<Long> reserveTicketScript() {
        String lua = """
                    local key   = KEYS[1]
                    local now   = tonumber(ARGV[1])
                    local open  = tonumber(ARGV[2])
                    local close = tonumber(ARGV[3])
                    local seats = tonumber(ARGV[4])

                    if now < open or now > close then
                        return -1
                    end

                    local remaining = redis.call('DECRBY', key, seats)
                    if remaining < 0 then
                        redis.call('INCRBY', key, seats)
                        return -2
                    end

                    return remaining
                """;
        return RedisScript.of(lua, Long.class);
    }

    // Script to rate limiting
    @Bean
    public RedisScript<Long> rateLimitScript() {
        String lua = """
                    local key   = KEYS[1]
                    local limit = tonumber(ARGV[1])
                    local count = redis.call('INCR', key)
                    if count == 1 then
                        redis.call('EXPIRE', key, 1)
                    end
                    if count > limit then return 0 end
                    return 1
                """;
        return RedisScript.of(lua, Long.class);
    }
}
