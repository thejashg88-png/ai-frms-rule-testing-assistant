package com.thejas.ai_frms.scenario.mapper;

import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.scenario.dto.TestScenarioCreateRequest;
import com.thejas.ai_frms.scenario.dto.TestScenarioResponse;
import com.thejas.ai_frms.scenario.dto.TestScenarioUpdateRequest;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;

public final class TestScenarioMapper {

    private TestScenarioMapper() {
    }

    public static TestScenarioEntity toEntity(TestScenarioCreateRequest request, RuleEntity rule) {
        TestScenarioEntity entity = new TestScenarioEntity();

        entity.setScenarioName(request.getScenarioName());
        entity.setScenarioDescription(request.getScenarioDescription());
        entity.setStatus(request.getStatus());
        entity.setRule(rule);
        entity.setCreatedBy(request.getCreatedBy());

        return entity;
    }

    public static void updateEntity(
            TestScenarioEntity entity,
            TestScenarioUpdateRequest request,
            RuleEntity rule
    ) {
        if (request.getScenarioName() != null) {
            entity.setScenarioName(request.getScenarioName());
        }

        if (request.getScenarioDescription() != null) {
            entity.setScenarioDescription(request.getScenarioDescription());
        }

        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        if (rule != null) {
            entity.setRule(rule);
        }

        if (request.getModifiedBy() != null) {
            entity.setModifiedBy(request.getModifiedBy());
        }
    }

    public static TestScenarioResponse toResponse(TestScenarioEntity entity) {
        TestScenarioResponse response = new TestScenarioResponse();

        response.setScenarioId(entity.getScenarioId());
        response.setScenarioName(entity.getScenarioName());
        response.setScenarioDescription(entity.getScenarioDescription());
        response.setStatus(entity.getStatus());

        if (entity.getRule() != null) {
            response.setRuleId(entity.getRule().getRuleId());
            response.setRuleName(entity.getRule().getRuleName());
            response.setRuleType(entity.getRule().getRuleType());
            response.setRuleAction(entity.getRule().getAction());
        }

        response.setCreatedBy(entity.getCreatedBy());
        response.setModifiedBy(entity.getModifiedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}