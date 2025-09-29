package com.example.statements.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Value("${admin.api.key:change-me}") private String adminApiKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/health", "/download/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")   // was .authenticated()
                .anyRequest().permitAll()
            )
                .addFilterBefore(new ApiKeyFilter(adminApiKey), AbstractPreAuthenticatedProcessingFilter.class)
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    static class ApiKeyFilter extends OncePerRequestFilter {
        private final String expectedKey;
        ApiKeyFilter(String expectedKey) { this.expectedKey = expectedKey; }
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String path = request.getRequestURI();
          if (path.startsWith("/admin/")) {
            String key = request.getHeader("X-API-Key");
            if (key == null || !key.equals(expectedKey)) {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              return;
            }
            // Mark request as authenticated so /admin/** passes authorization
            var auth = new UsernamePasswordAuthenticationToken(
                "api-key-admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
          }
          filterChain.doFilter(request, response);
        }
    }
}
