package com.thejas.ai_frms.audit.service;

import com.thejas.ai_frms.audit.dto.AuditLogRequest;
import com.thejas.ai_frms.audit.dto.AuditLogResponse;
import com.thejas.ai_frms.audit.dto.AuditLogSearchRequest;
import com.thejas.ai_frms.common.dto.PageResponse;

public interface AuditLogService {

    AuditLogResponse createAuditLog(AuditLogRequest request);

    PageResponse<AuditLogResponse> searchAuditLogs(AuditLogSearchRequest request);

    AuditLogResponse getAuditLogById(Long auditId);

    void logAction(
            String moduleName,
            String action,
            String entityName,
            Long entityId,
            String description,
            String performedBy
    );
}