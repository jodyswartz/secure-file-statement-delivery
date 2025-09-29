package com.example.statements.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "statements", indexes = {
        @Index(name = "idx_statements_account_period", columnList = "accountId,period", unique = true)
})
public class Statement {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false, length = 64) private String accountId;
    @Column(nullable = false, length = 7) private String period; // YYYY-MM
    @Column(nullable = false, length = 256) private String objectKey;
    @Column(nullable = false, length = 128) private String checksumSha256;
    @Column(nullable = false) private long sizeBytes;
    @Column(nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(nullable = false, length = 64) private String uploadedBy;

    public UUID getId() { return id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}
