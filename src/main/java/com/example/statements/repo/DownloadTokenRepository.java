package com.example.statements.repo;

import com.example.statements.domain.DownloadToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DownloadTokenRepository extends JpaRepository<DownloadToken, UUID> {
    Optional<DownloadToken> findByTokenAndExpiresAtAfter(String token, OffsetDateTime now);
}
