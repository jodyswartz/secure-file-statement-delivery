package com.example.statements.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "download_tokens", indexes = {
        @Index(name = "idx_download_token_token", columnList = "token", unique = true)
})
public class DownloadToken {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private UUID statementId;
    @Column(nullable = false, length = 64, unique = true) private String token;
    @Column(nullable = false) private OffsetDateTime expiresAt;
    @Column(nullable = false, length = 64) private String createdBy;

    public UUID getId() { return id; }
    public UUID getStatementId() { return statementId; }
    public void setStatementId(UUID statementId) { this.statementId = statementId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
