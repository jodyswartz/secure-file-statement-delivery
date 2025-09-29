package com.example.statements.repo;

import com.example.statements.domain.DownloadAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DownloadAuditRepository extends JpaRepository<DownloadAudit, UUID> { }
