package com.thejas.ai_frms.ai.controller;

import com.thejas.ai_frms.ai.dto.AiFailureAnalysisRequest;
import com.thejas.ai_frms.ai.dto.AiFailureAnalysisResponse;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationRequest;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationResponse;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationRequest;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationResponse;
import com.thejas.ai_frms.ai.service.AiAssistantService;
import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPathConstants.AI_ASSISTANT)
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/generate-test-cases")
    public ResponseEntity<ApiResponse<AiTestCaseGenerationResponse>> generateTestCases(
            @RequestBody AiTestCaseGenerationRequest request
    ) {
        AiTestCaseGenerationResponse response = aiAssistantService.generateTestCases(request);

        return ResponseEntity.ok(ApiResponse.success("AI generated test cases successfully", response));
    }

    @PostMapping("/analyze-failure")
    public ResponseEntity<ApiResponse<AiFailureAnalysisResponse>> analyzeFailure(
            @RequestBody AiFailureAnalysisRequest request
    ) {
        AiFailureAnalysisResponse response = aiAssistantService.analyzeFailure(request);

        return ResponseEntity.ok(ApiResponse.success("AI analyzed failure successfully", response));
    }

    @PostMapping("/explain-rule")
    public ResponseEntity<ApiResponse<AiRuleExplanationResponse>> explainRule(
            @RequestBody AiRuleExplanationRequest request
    ) {
        AiRuleExplanationResponse response = aiAssistantService.explainRule(request);

        return ResponseEntity.ok(ApiResponse.success("AI explained rule successfully", response));
    }
}