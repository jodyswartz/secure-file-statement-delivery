package com.example.statements.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "download_audits", indexes = {
        @Index(name = "idx_download_audits_statement", columnList = "statementId")
})
public class DownloadAudit {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private UUID statementId;
    @Column(nullable = false, length = 64) private String token;
    @Column(nullable = false) private OffsetDateTime downloadedAt = OffsetDateTime.now();
    @Column(length = 64) private String userId;
    @Column(length = 45) private String ip;
    @Column(length = 256) private String userAgent;

    public UUID getId() { return id; }
    public UUID getStatementId() { return statementId; }
    public void setStatementId(UUID statementId) { this.statementId = statementId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public OffsetDateTime getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(OffsetDateTime downloadedAt) { this.downloadedAt = downloadedAt; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
