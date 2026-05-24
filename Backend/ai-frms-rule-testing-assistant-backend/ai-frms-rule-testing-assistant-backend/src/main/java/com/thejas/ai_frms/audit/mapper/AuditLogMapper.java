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
        entity.setIpAddress(request.getIpAddress());
        entity.setUserAgent(request.getUserAgent());

        return entity;
    }

    public static AuditLogResponse toResponse(AuditLogEntity entity) {
        AuditLogResponse response = new AuditLogResponse();

        response.setAuditId(entity.getAuditId());
        response.setModuleName(entity.getModuleName());
        response.setAction(entity.getAction());
        response.setEntityName(entity.getEntityName());
        response.setEntityId(entity.getEntityId());
        response.setDescription(entity.getDescription());
        response.setPerformedBy(entity.getPerformedBy());
        response.setIpAddress(entity.getIpAddress());
        response.setUserAgent(entity.getUserAgent());
        response.setCreatedAt(entity.getCreatedAt());

        return response;
    }
}