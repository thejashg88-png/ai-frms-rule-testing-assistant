package com.thejas.ai_frms.rule.mapper;

import com.thejas.ai_frms.rule.dto.RuleCreateRequest;
import com.thejas.ai_frms.rule.dto.RuleResponse;
import com.thejas.ai_frms.rule.dto.RuleUpdateRequest;
import com.thejas.ai_frms.rule.entity.RuleEntity;

public final class RuleMapper {

    private RuleMapper() {
    }

    public static RuleEntity toEntity(RuleCreateRequest request) {
        RuleEntity entity = new RuleEntity();

        entity.setRuleName(request.getRuleName());
        entity.setRuleType(request.getRuleType());
        entity.setAction(request.getAction());
        entity.setStatus(request.getStatus());
        entity.setMccCode(request.getMccCode());
        entity.setTxnCount(request.getTxnCount());
        entity.setFrequencyHours(request.getFrequencyHours());
        entity.setTxnAmount(request.getTxnAmount());
        entity.setMaxAmount(request.getMaxAmount());
        entity.setPercentageThreshold(request.getPercentageThreshold());
        entity.setRuleDescription(request.getRuleDescription());
        entity.setCreatedBy(request.getCreatedBy());

        return entity;
    }

    public static void updateEntity(RuleEntity entity, RuleUpdateRequest request) {
        if (request.getRuleName() != null) {
            entity.setRuleName(request.getRuleName());
        }

        if (request.getRuleType() != null) {
            entity.setRuleType(request.getRuleType());
        }

        if (request.getAction() != null) {
            entity.setAction(request.getAction());
        }

        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        if (request.getMccCode() != null) {
            entity.setMccCode(request.getMccCode());
        }

        if (request.getTxnCount() != null) {
            entity.setTxnCount(request.getTxnCount());
        }

        if (request.getFrequencyHours() != null) {
            entity.setFrequencyHours(request.getFrequencyHours());
        }

        if (request.getTxnAmount() != null) {
            entity.setTxnAmount(request.getTxnAmount());
        }

        if (request.getMaxAmount() != null) {
            entity.setMaxAmount(request.getMaxAmount());
        }

        if (request.getPercentageThreshold() != null) {
            entity.setPercentageThreshold(request.getPercentageThreshold());
        }

        if (request.getRuleDescription() != null) {
            entity.setRuleDescription(request.getRuleDescription());
        }

        if (request.getModifiedBy() != null) {
            entity.setModifiedBy(request.getModifiedBy());
        }
    }

    public static RuleResponse toResponse(RuleEntity entity) {
        RuleResponse response = new RuleResponse();

        response.setRuleId(entity.getRuleId());
        response.setRuleName(entity.getRuleName());
        response.setRuleType(entity.getRuleType());
        response.setAction(entity.getAction());
        response.setStatus(entity.getStatus());
        response.setMccCode(entity.getMccCode());
        response.setTxnCount(entity.getTxnCount());
        response.setFrequencyHours(entity.getFrequencyHours());
        response.setTxnAmount(entity.getTxnAmount());
        response.setMaxAmount(entity.getMaxAmount());
        response.setPercentageThreshold(entity.getPercentageThreshold());
        response.setRuleDescription(entity.getRuleDescription());
        response.setCreatedBy(entity.getCreatedBy());
        response.setModifiedBy(entity.getModifiedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}