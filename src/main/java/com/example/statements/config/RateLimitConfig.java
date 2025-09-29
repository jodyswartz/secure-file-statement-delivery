package com.example.statements.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    @Bean
    public OncePerRequestFilter rateLimitFilter() {
        return new OncePerRequestFilter() {
            private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
            private Bucket resolveBucket(String key) {
                return buckets.computeIfAbsent(key, k -> {
                    Bandwidth limit = Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1)));
                    return Bucket.builder().addLimit(limit).build();
                });
            }
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                String ip = request.getRemoteAddr();
                Bucket bucket = resolveBucket(ip);
                if (bucket.tryConsume(1)) {
                    filterChain.doFilter(request, response);
                } else {
                    response.setStatus(429);
                    response.getWriter().write("Too Many Requests");
                }
            }
        };
    }
}
