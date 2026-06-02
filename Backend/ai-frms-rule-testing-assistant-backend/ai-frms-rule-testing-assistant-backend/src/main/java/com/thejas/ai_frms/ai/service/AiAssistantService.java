package com.thejas.ai_frms.ai.service;

import com.thejas.ai_frms.ai.dto.AiChatRequest;
import com.thejas.ai_frms.ai.dto.AiChatResponse;
import com.thejas.ai_frms.ai.dto.AiFailureAnalysisRequest;
import com.thejas.ai_frms.ai.dto.AiFailureAnalysisResponse;
import com.thejas.ai_frms.ai.dto.AiGenerateRuleRequest;
import com.thejas.ai_frms.ai.dto.AiGenerateRuleResponse;
import com.thejas.ai_frms.ai.dto.AiGenerateTransactionRequest;
import com.thejas.ai_frms.ai.dto.AiGenerateTransactionResponse;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationRequest;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationResponse;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationRequest;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationResponse;

import java.util.Map;

public interface AiAssistantService {

    AiTestCaseGenerationResponse generateTestCases(AiTestCaseGenerationRequest request);

    AiFailureAnalysisResponse analyzeFailure(AiFailureAnalysisRequest request);

    AiRuleExplanationResponse explainRule(AiRuleExplanationRequest request);

    AiRuleExplanationResponse explainRuleById(Long ruleId);

    AiFailureAnalysisResponse analyzeFailureById(Long executionId);

    AiGenerateTransactionResponse generateTransaction(AiGenerateTransactionRequest request);

    AiChatResponse chat(AiChatRequest request);

    AiGenerateRuleResponse generateRule(AiGenerateRuleRequest request);

    Map<String, Object> getHealth();
}