package com.thejas.ai_frms.audit.repository;

import com.thejas.ai_frms.audit.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>,
        JpaSpecificationExecutor<AuditLogEntity> {

    List<AuditLogEntity> findByModuleNameOrderByCreatedAtDesc(String moduleName);

    List<AuditLogEntity> findByPerformedByOrderByCreatedAtDesc(String performedBy);

    List<AuditLogEntity> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, Long entityId);
}