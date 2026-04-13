package com.cambiz.market.repository;

import com.cambiz.market.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<AuditLog> findByActionAndCreatedAtAfter(String action, LocalDateTime since);
    List<AuditLog> findByStatusAndCreatedAtAfter(String status, LocalDateTime since);
    long countByActionAndCreatedAtAfter(String action, LocalDateTime since);
}