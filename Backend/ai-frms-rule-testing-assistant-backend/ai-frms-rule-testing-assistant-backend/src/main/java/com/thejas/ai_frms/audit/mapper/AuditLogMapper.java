package com.thejas.ai_frms.audit.mapper;

import com.thejas.ai_frms.audit.dto.AuditLogRequest;
import com.thejas.ai_frms.audit.dto.AuditLogResponse;
import com.thejas.ai_frms.audit.entity.AuditLogEntity;

public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogEntity toEntity(AuditLogRequest request) {
        AuditLogEntity entity = new AuditLogEntity();

        entity.setModuleName(request.getModuleName());
        entity.setAction(request.getAction());
        entity.setEntityName(request.getEntityName());
        entity.setEntityId(request.getEntityId());
        entity.setDescription(request.getDescription());
        entity.setPerformedBy(request.getPerformedBy());
        entity.setOldValue(request.getOldValue());
        entity.setNewValue(request.getNewValue());
        entity.setIpAddress(request.getIpAddress());
        entity.setUserAgent(request.getUserAgent());

        return entity;
    }

    public static AuditLogResponse toResponse(AuditLogEntity entity) {
        AuditLogResponse response = new AuditLogResponse();

        response.setAuditId(entity.getAuditId());
        // performedBy in DB → actor in response; fall back to "SYSTEM" for old null records
        response.setActor(entity.getPerformedBy() != null ? entity.getPerformedBy() : "SYSTEM");
        // moduleName in DB → entityType in response; fall back to "N/A" for old null records
        response.setEntityType(entity.getModuleName() != null ? entity.getModuleName() : "N/A");
        response.setAction(entity.getAction());
        response.setEntityName(entity.getEntityName());
        response.setEntityId(entity.getEntityId());
        response.setDescription(entity.getDescription());
        response.setOldValue(entity.getOldValue());
        response.setNewValue(entity.getNewValue());
        response.setIpAddress(entity.getIpAddress());
        response.setUserAgent(entity.getUserAgent());
        response.setCreatedAt(entity.getCreatedAt());

        return response;
    }
}