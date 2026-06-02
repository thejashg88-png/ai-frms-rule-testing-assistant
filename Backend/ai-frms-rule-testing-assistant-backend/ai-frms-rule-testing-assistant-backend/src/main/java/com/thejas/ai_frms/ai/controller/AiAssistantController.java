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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * REST controller for AI assistant features.
 *
 * This controller does NOT implement any AI logic itself — it delegates all AI tasks
 * to the FastAPI Python service via AiAssistantService → AiServiceWebClient (HTTP POST).
 *
 * Available AI operations:
 *   generate-test-cases   — generates test cases for a rule or scenario
 *   analyze-failure       — explains why a test case failed
 *   explain-rule          — produces a business/technical explanation for a rule
 *   generate-transaction  — produces a sample transaction for a given rule context
 *   generate-rule         — suggests a rule definition from a plain-language requirement
 *   chat                  — free-form FRMS assistant chat
 *   health                — checks whether the FastAPI AI service is reachable
 *
 * If the FastAPI service is unavailable, endpoints return success:false with a clear message.
 * They never throw a 500 — unavailability is expected in development environments.
 *
 * Frontend-friendly aliases are provided for path variations the UI may send.
 */
@RestController
@RequestMapping(ApiPathConstants.AI_ASSISTANT)
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    // ── Health ───────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiServiceHealth() {
        Map<String, Object> health = aiAssistantService.getHealth();
        boolean isUp = "UP".equals(health.get("aiServiceStatus"));
        return ResponseEntity.ok(ApiResponse.success(
                isUp ? "AI service is healthy" : "AI service is currently unavailable",
                health
        ));
    }

    // ── Primary endpoints ────────────────────────────────────────────────────

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

    @PostMapping("/generate-transaction")
    public ResponseEntity<ApiResponse<AiGenerateTransactionResponse>> generateTransaction(
            @RequestBody(required = false) AiGenerateTransactionRequest request
    ) {
        AiGenerateTransactionResponse response = aiAssistantService.generateTransaction(request);
        return ResponseEntity.ok(ApiResponse.success("AI generated transaction successfully", response));
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestBody AiChatRequest request
    ) {
        AiChatResponse response = aiAssistantService.chat(request);
        return ResponseEntity.ok(ApiResponse.success("AI chat response returned", response));
    }

    // ── Frontend-friendly aliases ────────────────────────────────────────────

    // ── Generate Rule ────────────────────────────────────────────────────────

    /**
     * POST /api/ai/generate-rule
     * Generates an AI-suggested rule based on a plain-language requirement.
     * Never saves to the database — user reviews and calls POST /api/rules to persist.
     */
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

    /** Alias: /api/ai/generate-testcases → generate-test-cases */
    @PostMapping("/generate-testcases")
    public ResponseEntity<ApiResponse<AiTestCaseGenerationResponse>> generateTestCasesAlias(
            @RequestBody AiTestCaseGenerationRequest request
    ) {
        return generateTestCases(request);  // already has try-catch
    }

    /** Alias: /api/ai/failure-analysis → analyze-failure */
    @PostMapping("/failure-analysis")
    public ResponseEntity<ApiResponse<AiFailureAnalysisResponse>> analyzeFailureAlias(
            @RequestBody AiFailureAnalysisRequest request
    ) {
        return analyzeFailure(request);
    }

    /** Alias: /api/ai/rule-explanation → explain-rule */
    @PostMapping("/rule-explanation")
    public ResponseEntity<ApiResponse<AiRuleExplanationResponse>> explainRuleAlias(
            @RequestBody AiRuleExplanationRequest request
    ) {
        return explainRule(request);
    }

    /** Alias: /api/ai/transaction-generator → generate-transaction */
    @PostMapping("/transaction-generator")
    public ResponseEntity<ApiResponse<AiGenerateTransactionResponse>> generateTransactionAlias(
            @RequestBody(required = false) AiGenerateTransactionRequest request
    ) {
        return generateTransaction(request);
    }
}