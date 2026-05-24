package com.thejas.ai_frms.rule.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.rule.dto.RuleCreateRequest;
import com.thejas.ai_frms.rule.dto.RuleResponse;
import com.thejas.ai_frms.rule.dto.RuleSearchRequest;
import com.thejas.ai_frms.rule.dto.RuleUpdateRequest;

public interface RuleService {

    RuleResponse createRule(RuleCreateRequest request);

    RuleResponse updateRule(Long ruleId, RuleUpdateRequest request);

    RuleResponse getRuleById(Long ruleId);

    PageResponse<RuleResponse> searchRules(RuleSearchRequest request);

    RuleResponse changeRuleStatus(Long ruleId, RuleStatus status);

    void deleteRule(Long ruleId);
}