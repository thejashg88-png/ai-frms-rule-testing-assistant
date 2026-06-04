package com.thejas.ai_frms.audit.service;

import com.thejas.ai_frms.audit.dto.AuditLogRequest;
import com.thejas.ai_frms.audit.dto.AuditLogResponse;
import com.thejas.ai_frms.audit.dto.AuditLogSearchRequest;
import com.thejas.ai_frms.common.dto.PageResponse;

public interface AuditLogService {

    AuditLogResponse createAuditLog(AuditLogRequest request);

    PageResponse<AuditLogResponse> searchAuditLogs(AuditLogSearchRequest request);

    AuditLogResponse getAuditLogById(Long auditId);

    // Legacy method — retained for backward compatibility
    void logAction(
            String moduleName,
            String action,
            String entityName,
            Long entityId,
            String description,
            String performedBy
    );

    // Log a CREATE action — newValue holds the created entity as masked JSON
    void logCreate(String actor, String entityType, Long entityId, String entityName, Object newValue);

    // Log an UPDATE action — oldValue and newValue hold before/after entity states as masked JSON
    void logUpdate(String actor, String entityType, Long entityId, String entityName, Object oldValue, Object newValue);

    // Log a DELETE or INACTIVATE action — oldValue holds the deleted entity state as masked JSON
    void logDelete(String actor, String action, String entityType, Long entityId, String entityName, Object oldValue);

    // Log any arbitrary action with a plain description (e.g. RUN_EXECUTION, status change)
    void logEvent(String actor, String action, String entityType, Long entityId, String entityName, String description);

    // Log a report download action
    void logReportDownload(String actor, String reportType);
}