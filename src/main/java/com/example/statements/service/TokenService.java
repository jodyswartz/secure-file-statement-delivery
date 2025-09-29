package com.example.statements.service;

import com.example.statements.domain.DownloadAudit;
import com.example.statements.domain.DownloadToken;
import com.example.statements.domain.Statement;
import com.example.statements.repo.DownloadAuditRepository;
import com.example.statements.repo.DownloadTokenRepository;
import com.example.statements.repo.StatementRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {

    private final DownloadTokenRepository tokenRepo;
    private final DownloadAuditRepository auditRepo;
    private final StatementRepository statementRepo;
    private final S3StorageService storageService;

    public TokenService(DownloadTokenRepository tokenRepo, DownloadAuditRepository auditRepo,
                        StatementRepository statementRepo, S3StorageService storageService) {
        this.tokenRepo = tokenRepo; this.auditRepo = auditRepo; this.statementRepo = statementRepo; this.storageService = storageService;
    }

    public String createDownloadUrl(UUID statementId, int expiresMinutes, String createdBy) {
        Statement s = statementRepo.findById(statementId).orElseThrow();
        DownloadToken t = new DownloadToken();
        t.setStatementId(statementId);
        t.setToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        t.setExpiresAt(OffsetDateTime.now().plusMinutes(expiresMinutes));
        t.setCreatedBy(createdBy == null ? "system" : createdBy);
        tokenRepo.save(t);
        return "/download/" + t.getToken();
    }

    public Optional<String> resolveAndLog(String token, String userId, String ip, String ua, int innerPresignMinutes) {
        return tokenRepo.findByTokenAndExpiresAtAfter(token, OffsetDateTime.now()).map(t -> {
            Statement s = statementRepo.findById(t.getStatementId()).orElseThrow();
            String url = storageService.presignedGetUrl(s.getObjectKey(), innerPresignMinutes);
            DownloadAudit a = new DownloadAudit();
            a.setStatementId(s.getId());
            a.setToken(token);
            a.setUserId(userId);
            a.setIp(ip);
            a.setUserAgent(ua);
            auditRepo.save(a);
            return url;
        });
    }
}
