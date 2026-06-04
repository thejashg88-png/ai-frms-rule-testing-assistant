package com.thejas.ai_frms.ai.controller;

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
import com.thejas.ai_frms.common.exception.AiServiceException;
import com.thejas.ai_frms.ai.service.AiAssistantService;
import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * REST controller for AI assistant features.
 *
 * Role access:
 *   ADMIN  — all AI endpoints
 *   TESTER — all AI endpoints
 *   VIEWER — no AI access (403 on all POST endpoints; GET /health is public)
 */
@RestController
@RequestMapping(ApiPathConstants.AI_ASSISTANT)
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    // ── Health (public — permitted in SecurityConfig) ─────────────────────────

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiServiceHealth() {
        Map<String, Object> health = aiAssistantService.getHealth();
        boolean isUp = "UP".equals(health.get("aiServiceStatus"));
        return ResponseEntity.ok(ApiResponse.success(
                isUp ? "AI service is healthy" : "AI service is currently unavailable", health));
    }

    // ── Primary endpoints ─────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/generate-test-cases")
    public ResponseEntity<ApiResponse<AiTestCaseGenerationResponse>> generateTestCases(
            @RequestBody AiTestCaseGenerationRequest request
    ) {
        try {
            AiTestCaseGenerationResponse response = aiAssistantService.generateTestCases(request);
            return ResponseEntity.ok(ApiResponse.success("AI generated test cases successfully", response));
        } catch (AiServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof WebClientResponseException wce) {
                int status = wce.getStatusCode().value();
                if (status == 400 || status == 422) {
                    return ResponseEntity.ok(ApiResponse.failure(
                            "AI test case generation request validation failed: " + wce.getResponseBodyAsString()));
                }
            }
            return ResponseEntity.ok(ApiResponse.failure("AI service is currently unavailable"));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/analyze-failure")
    public ResponseEntity<ApiResponse<AiFailureAnalysisResponse>> analyzeFailure(
            @RequestBody AiFailureAnalysisRequest request
    ) {
        try {
            AiFailureAnalysisResponse response = aiAssistantService.analyzeFailure(request);
            return ResponseEntity.ok(ApiResponse.success("AI analyzed failure successfully", response));
        } catch (AiServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof WebClientResponseException wce) {
                int status = wce.getStatusCode().value();
                if (status == 400 || status == 422) {
                    return ResponseEntity.ok(ApiResponse.failure(
                            "AI failure analysis request validation failed: " + wce.getResponseBodyAsString()));
                }
            }
            return ResponseEntity.ok(ApiResponse.failure("AI service is currently unavailable"));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/explain-rule")
    public ResponseEntity<ApiResponse<AiRuleExplanationResponse>> explainRule(
            @RequestBody AiRuleExplanationRequest request
    ) {
        try {
            AiRuleExplanationResponse response = aiAssistantService.explainRule(request);
            return ResponseEntity.ok(ApiResponse.success("AI explained rule successfully", response));
        } catch (AiServiceException e) {
            return ResponseEntity.ok(ApiResponse.failure("AI service is currently unavailable"));
        }
    }

    @GetMapping("/explain-rule/{ruleId}")
    public ResponseEntity<ApiResponse<AiRuleExplanationResponse>> explainRuleById(
            @PathVariable Long ruleId
    ) {
        try {
            AiRuleExplanationResponse response = aiAssistantService.explainRuleById(ruleId);
            return ResponseEntity.ok(ApiResponse.success("AI explained rule successfully", response));
        } catch (AiServiceException e) {
            return ResponseEntity.ok(ApiResponse.failure("AI service is currently unavailable"));
        }
    }

    @GetMapping("/analyze-failure/{executionId}")
    public ResponseEntity<ApiResponse<AiFailureAnalysisResponse>> analyzeFailureById(
            @PathVariable Long executionId
    ) {
        try {
            AiFailureAnalysisResponse response = aiAssistantService.analyzeFailureById(executionId);
            return ResponseEntity.ok(ApiResponse.success("AI analyzed failure successfully", response));
        } catch (AiServiceException e) {
            return ResponseEntity.ok(ApiResponse.failure("AI service is currently unavailable"));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/generate-transaction")
    public ResponseEntity<ApiResponse<AiGenerateTransactionResponse>> generateTransaction(
            @RequestBody(required = false) AiGenerateTransactionRequest request
    ) {
        AiGenerateTransactionResponse response = aiAssistantService.generateTransaction(request);
        return ResponseEntity.ok(ApiResponse.success("AI generated transaction successfully", response));
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestBody AiChatRequest request
    ) {
        AiChatResponse response = aiAssistantService.chat(request);
        return ResponseEntity.ok(ApiResponse.success("AI chat response returned", response));
    }

    // ── Generate Rule ─────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/generate-rule")
    public ResponseEntity<ApiResponse<AiGenerateRuleResponse>> generateRule(
            @RequestBody AiGenerateRuleRequest request
    ) {
        try {
            AiGenerateRuleResponse response = aiAssistantService.generateRule(request);
            return ResponseEntity.ok(ApiResponse.success("Rule suggestion generated successfully", response));
        } catch (AiServiceException e) {
            return ResponseEntity.ok(ApiResponse.failure("AI service is currently unavailable"));
        }
    }

    // ── Frontend-friendly aliases ─────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/generate-testcases")
    public ResponseEntity<ApiResponse<AiTestCaseGenerationResponse>> generateTestCasesAlias(
            @RequestBody AiTestCaseGenerationRequest request
    ) {
        return generateTestCases(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/failure-analysis")
    public ResponseEntity<ApiResponse<AiFailureAnalysisResponse>> analyzeFailureAlias(
            @RequestBody AiFailureAnalysisRequest request
    ) {
        return analyzeFailure(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/rule-explanation")
    public ResponseEntity<ApiResponse<AiRuleExplanationResponse>> explainRuleAlias(
            @RequestBody AiRuleExplanationRequest request
    ) {
        return explainRule(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/transaction-generator")
    public ResponseEntity<ApiResponse<AiGenerateTransactionResponse>> generateTransactionAlias(
            @RequestBody(required = false) AiGenerateTransactionRequest request
    ) {
        return generateTransaction(request);
    }
}