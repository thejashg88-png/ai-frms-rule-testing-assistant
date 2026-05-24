package com.thejas.ai_frms.scheduler.mapper;

import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioCreateRequest;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioResponse;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioUpdateRequest;
import com.thejas.ai_frms.scheduler.entity.ScheduledScenarioEntity;

public final class ScheduledScenarioMapper {

    private ScheduledScenarioMapper() {
    }

    public static ScheduledScenarioEntity toEntity(
            ScheduledScenarioCreateRequest request,
            TestScenarioEntity scenario
    ) {
        ScheduledScenarioEntity entity = new ScheduledScenarioEntity();

        entity.setScheduleName(request.getScheduleName());
        entity.setDescription(request.getDescription());
        entity.setScenario(scenario);
        entity.setCronExpression(request.getCronExpression());
        entity.setStatus(request.getStatus());
        entity.setCreatedBy(request.getCreatedBy());

        return entity;
    }

    public static void updateEntity(
            ScheduledScenarioEntity entity,
            ScheduledScenarioUpdateRequest request,
            TestScenarioEntity scenario
    ) {
        if (request.getScheduleName() != null) {
            entity.setScheduleName(request.getScheduleName());
        }

        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        if (scenario != null) {
            entity.setScenario(scenario);
        }

        if (request.getCronExpression() != null) {
            entity.setCronExpression(request.getCronExpression());
        }

        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        if (request.getModifiedBy() != null) {
            entity.setModifiedBy(request.getModifiedBy());
        }
    }

    public static ScheduledScenarioResponse toResponse(ScheduledScenarioEntity entity) {
        ScheduledScenarioResponse response = new ScheduledScenarioResponse();

        response.setScheduleId(entity.getScheduleId());
        response.setScheduleName(entity.getScheduleName());
        response.setDescription(entity.getDescription());

        if (entity.getScenario() != null) {
            response.setScenarioId(entity.getScenario().getScenarioId());
            response.setScenarioName(entity.getScenario().getScenarioName());
        }

        response.setCronExpression(entity.getCronExpression());
        response.setStatus(entity.getStatus());
        response.setLastRunAt(entity.getLastRunAt());
        response.setNextRunAt(entity.getNextRunAt());
        response.setLastExecutionId(entity.getLastExecutionId());
        response.setRunCount(entity.getRunCount());
        response.setCreatedBy(entity.getCreatedBy());
        response.setModifiedBy(entity.getModifiedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}