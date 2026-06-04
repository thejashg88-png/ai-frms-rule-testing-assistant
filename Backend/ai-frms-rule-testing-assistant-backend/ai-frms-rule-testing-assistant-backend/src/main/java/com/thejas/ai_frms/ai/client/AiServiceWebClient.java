package com.thejas.ai_frms.ai.client;

import com.thejas.ai_frms.ai.dto.fastapi.FastApiAnalyzeFailureRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiExplainRuleRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiGenerateRuleRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiGenerateTransactionRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiTestCasesRequest;
import com.thejas.ai_frms.common.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP client that calls the FastAPI AI service endpoints.
 *
 * All base URL and path values are injected from application properties
 * (prefix: app.integration.ai.*), with sensible defaults for localhost development.
 * Final URL = baseUrl + path (e.g. http://localhost:8000 + /api/ai/chat).
 *
 * Timeout: 60 seconds per request — AI model inference can be slow,
 * especially for large test case generation tasks.
 *
 * Error handling:
 *   WebClientResponseException (4xx/5xx from FastAPI) → wrapped in AiServiceException
 *   Connection refused / timeout                      → wrapped in AiServiceException
 *   In both cases the caller receives AiServiceException, not a raw HTTP exception.
 */
@Component("aiApiClient")
public class AiServiceWebClient implements AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceWebClient.class);

    private final WebClient aiWebClient;
    private final String baseUrl;
    private final String generateTestCasesPath;
    private final String analyzeFailurePath;
    private final String explainRulePath;
    private final String generateTransactionPath;
    private final String generateRulePath;
    private final String chatPath;

    public AiServiceWebClient(
            @Qualifier("aiWebClient") WebClient aiWebClient,
            @Value("${app.integration.ai.base-url:http://localhost:8000}") String baseUrl,
            @Value("${app.integration.ai.generate-test-cases-path:/api/ai/generate-test-cases}") String generateTestCasesPath,
            @Value("${app.integration.ai.analyze-failure-path:/api/ai/analyze-failure}") String analyzeFailurePath,
            @Value("${app.integration.ai.explain-rule-path:/api/ai/explain-rule}") String explainRulePath,
            @Value("${app.integration.ai.generate-transaction-path:/api/ai/generate-transaction}") String generateTransactionPath,
            @Value("${app.integration.ai.generate-rule-path:/api/ai/generate-rule}") String generateRulePath,
            @Value("${app.integration.ai.chat-path:/api/ai/chat}") String chatPath
    ) {
        this.aiWebClient = aiWebClient;
        this.baseUrl = baseUrl;
        this.generateTestCasesPath = generateTestCasesPath;
        this.analyzeFailurePath = analyzeFailurePath;
        this.explainRulePath = explainRulePath;
        this.generateTransactionPath = generateTransactionPath;
        this.generateRulePath = generateRulePath;
        this.chatPath = chatPath;
    }

    @Override
    public String generateTestCases(FastApiTestCasesRequest request) {
        log.info("[AI CLIENT] Calling FastAPI generate-test-cases: {}{}", baseUrl, generateTestCasesPath);
        try {
            String response = callAiServicePost(generateTestCasesPath, request, "AI test case generation failed");
            log.info("[AI CLIENT] Generate test cases FastAPI response: {}", response);
            return response;
        } catch (AiServiceException e) {
            log.error("[AI CLIENT] Generate test cases failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public String analyzeFailure(FastApiAnalyzeFailureRequest request) {
        log.info("[AI CLIENT] Calling FastAPI analyze-failure: {}{}", baseUrl, analyzeFailurePath);
        log.info("[AI CLIENT] Failure analysis payload: executionId={}, ruleType={}, expectedResult={}, actualResult={}, matchedCount={}, requiredCount={}, historicalCount={}, hasRuleConfig={}, hasTrace={}",
                request.getExecutionId(), request.getRuleType(), request.getExpectedResult(),
                request.getActualResult(), request.getMatchedCount(), request.getRequiredCount(),
                request.getHistoricalTransactionCount(),
                request.getRuleConfig() != null, request.getExecutionTrace() != null);
        try {
            String response = callAiServicePost(analyzeFailurePath, request, "AI failure analysis failed");
            log.info("[AI CLIENT] Failure analysis FastAPI response: {}", response);
            return response;
        } catch (AiServiceException e) {
            log.error("[AI CLIENT] Failure analysis failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public String explainRule(FastApiExplainRuleRequest request) {
        log.info("[AI CLIENT] Calling FastAPI explain-rule: {}{}", baseUrl, explainRulePath);
        try {
            String response = callAiServicePost(explainRulePath, request, "AI rule explanation failed");
            log.info("[AI CLIENT] Explain-rule FastAPI response: {}", response);
            return response;
        } catch (AiServiceException e) {
            log.error("[AI CLIENT] Explain-rule failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public String generateTransaction(FastApiGenerateTransactionRequest request) {
        return callAiServicePost(generateTransactionPath, request, "AI transaction generation failed");
    }

    @Override
    public String generateRule(FastApiGenerateRuleRequest request) {
        return callAiServicePost(generateRulePath, request, "AI rule generation failed");
    }

    @Override
    public String chat(String message) {
        return callAiServicePost(chatPath, Map.of("message", message), "AI chat failed");
    }

    @Override
    public String getHealth() {
        log.debug("[AI CLIENT] Calling FastAPI health: {}/health", baseUrl);
        try {
            String response = aiWebClient
                    .get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.debug("[AI CLIENT] Health response: {}", response);
            return response;
        } catch (Exception exception) {
            log.error("[AI CLIENT] Health check failed: {}", exception.getMessage());
            throw new AiServiceException("AI service health check failed. Service may be unavailable.", exception);
        }
    }

    private String callAiServicePost(String path, Object requestBody, String errorMessage) {
        log.debug("[AI CLIENT] POST {}{}", baseUrl, path);
        try {
            String response = aiWebClient
                    .post()
                    .uri(path)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            log.debug("[AI CLIENT] POST {}{} — OK", baseUrl, path);
            return response;
        } catch (WebClientResponseException exception) {
            log.error("[AI CLIENT] POST {}{} — HTTP {} | body: {}",
                    baseUrl, path,
                    exception.getStatusCode().value(),
                    exception.getResponseBodyAsString());
            throw new AiServiceException(
                    errorMessage + ". Status: "
                            + exception.getStatusCode()
                            + ", Response: "
                            + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            log.error("[AI CLIENT] POST {}{} — error: {}", baseUrl, path, exception.getMessage());
            throw new AiServiceException(errorMessage + ". Unable to call AI service", exception);
        }
    }
}