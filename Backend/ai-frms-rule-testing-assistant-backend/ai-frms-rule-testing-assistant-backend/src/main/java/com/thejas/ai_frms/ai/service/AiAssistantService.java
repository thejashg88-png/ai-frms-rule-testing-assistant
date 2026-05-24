package com.thejas.ai_frms.ai.service;

import com.thejas.ai_frms.ai.dto.AiFailureAnalysisRequest;
import com.thejas.ai_frms.ai.dto.AiFailureAnalysisResponse;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationRequest;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationResponse;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationRequest;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationResponse;

public interface AiAssistantService {

    AiTestCaseGenerationResponse generateTestCases(AiTestCaseGenerationRequest request);

    AiFailureAnalysisResponse analyzeFailure(AiFailureAnalysisRequest request);

    AiRuleExplanationResponse explainRule(AiRuleExplanationRequest request);
}