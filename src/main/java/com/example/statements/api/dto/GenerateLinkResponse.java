package com.example.statements.api.dto;

import java.time.OffsetDateTime;

public class GenerateLinkResponse {
    private String url;
    private OffsetDateTime expiresAt;
    public GenerateLinkResponse(String url, OffsetDateTime expiresAt) { this.url = url; this.expiresAt = expiresAt; }
    public String getUrl() { return url; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
