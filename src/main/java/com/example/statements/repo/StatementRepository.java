package com.example.statements.repo;

import com.example.statements.domain.Statement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface StatementRepository extends JpaRepository<Statement, UUID> {
    Page<Statement> findByAccountId(String accountId, Pageable pageable);
    Optional<Statement> findByAccountIdAndPeriod(String accountId, String period);
}
