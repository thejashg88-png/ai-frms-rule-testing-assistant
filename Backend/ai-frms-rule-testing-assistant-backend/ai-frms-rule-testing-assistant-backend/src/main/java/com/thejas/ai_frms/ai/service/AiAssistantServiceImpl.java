package com.thejas.ai_frms.ai.service;

import com.thejas.ai_frms.ai.client.AiServiceClient;
import com.thejas.ai_frms.ai.dto.AiChatRequest;
import com.thejas.ai_frms.ai.dto.AiChatResponse;
import com.thejas.ai_frms.ai.dto.AiFailureAnalysisRequest;
import com.thejas.ai_frms.ai.dto.AiFailureAnalysisResponse;
import com.thejas.ai_frms.ai.dto.AiGenerateTransactionRequest;
import com.thejas.ai_frms.ai.dto.AiGenerateTransactionResponse;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationRequest;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationResponse;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationRequest;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationResponse;
import com.thejas.ai_frms.ai.dto.AiGenerateRuleRequest;
import com.thejas.ai_frms.ai.dto.AiGenerateRuleResponse;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiAnalyzeFailureRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiExplainRuleRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiGenerateRuleRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiGenerateTransactionRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiTestCasesRequest;
import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.common.exception.AiServiceException;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.execution.entity.TestExecutionEntity;
import com.thejas.ai_frms.execution.repository.TestExecutionRepository;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.rule.repository.RuleRepository;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for AI assistant features.
 *
 * Acts as the bridge between the Spring Boot REST layer and the FastAPI Python AI service.
 * All AI computation happens inside FastAPI — this service only orchestrates the call:
 *   1. Enriches the request with data from the database (rule fields, scenario fields, execution data).
 *   2. Maps to a FastAPI-compatible DTO.
 *   3. Delegates to AiServiceClient (HTTP POST to FastAPI).
 *   4. Parses the raw FastAPI response via AiResponseParserService.
 *
 * Test case generation supports three flows:
 *   scenarioId provided → fetch scenario → fetch linked rule → enrich request
 *   ruleId provided     → fetch rule from DB → enrich request
 *   inline fields       → use directly as provided
 *
 * AiServiceException propagates to the controller, which converts it to a success:false response
 * rather than a 500 error — AI unavailability is expected during development.
 */
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantServiceImpl.class);

    private final AiServiceClient aiServiceClient;
    private final AiResponseParserService aiResponseParserService;
    private final RuleRepository ruleRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestScenarioRepository testScenarioRepository;

    public AiAssistantServiceImpl(
            AiServiceClient aiServiceClient,
            AiResponseParserService aiResponseParserService,
            RuleRepository ruleRepository,
            TestExecutionRepository testExecutionRepository,
            TestScenarioRepository testScenarioRepository
    ) {
        this.aiServiceClient = aiServiceClient;
        this.aiResponseParserService = aiResponseParserService;
        this.ruleRepository = ruleRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.testScenarioRepository = testScenarioRepository;
    }

    @Override
    public AiTestCaseGenerationResponse generateTestCases(AiTestCaseGenerationRequest request) {
        if (request == null) {
            throw new BadRequestException("AI test case generation request cannot be null");
        }

        log.info("[AI BACKEND] Generate test cases incoming request: scenarioId={}, ruleId={}, ruleType={}, ruleName={}, action={}",
                request.getScenarioId(), request.getRuleId(), request.getRuleType(),
                request.getRuleName(), request.getAction());

        // Flow 1: scenarioId present — enrich from scenario (which links to a rule)
        if (request.getScenarioId() != null) {
            log.info("[AI BACKEND] Using scenario-based generation: scenarioId={}", request.getScenarioId());
            enrichFromScenario(request);
        }
        // Flow 2: ruleId present, rule fields missing — enrich from rule
        else if (request.getRuleId() != null) {
            log.info("[AI BACKEND] Using rule-based generation via DB: ruleId={}", request.getRuleId());
            enrichFromRule(request);
        }
        // Flow 3: inline rule fields provided
        else {
            log.info("[AI BACKEND] Using rule-based generation with inline fields: ruleName={}, ruleType={}",
                    request.getRuleName(), request.getRuleType());
        }

        // Validate after enrichment
        if (isBlank(request.getRuleType()) && isBlank(request.getRuleName())) {
            throw new BadRequestException(
                    "Either scenarioId or rule details (ruleName, ruleType, action) are required for AI test case generation");
        }

        // Default action when still null
        if (request.getAction() == null) {
            request.setAction(RuleAction.MONITOR);
        }

        if (request.getNumberOfTestCases() == null || request.getNumberOfTestCases() <= 0) {
            request.setNumberOfTestCases(5);
        }

        // Normalize generic "FREQUENCY" ruleType to a specific backend type
        if ("FREQUENCY".equalsIgnoreCase(request.getRuleType())) {
            request.setRuleType(resolveFrequencyRuleType(request.getRuleName()));
            log.info("[AI BACKEND] Normalized ruleType to: {}", request.getRuleType());
        }

        FastApiTestCasesRequest fastApiRequest = buildTestCasesRequest(request);
        log.info("[AI CLIENT] Generate test cases request payload: ruleName={}, ruleType={}, action={}, txnCount={}, txnAmount={}, frequency={}, maxAmount={}",
                fastApiRequest.getRuleName(), fastApiRequest.getRuleType(), fastApiRequest.getAction(),
                fastApiRequest.getTxnCount(), fastApiRequest.getTxnAmount(),
                fastApiRequest.getFrequency(), fastApiRequest.getMaxAmount());

        // Let AiServiceException propagate — controller handles success:false response
        String rawAiResponse = aiServiceClient.generateTestCases(fastApiRequest);
        List<TestCaseCreateRequest> generatedTestCases =
                aiResponseParserService.parseGeneratedTestCases(rawAiResponse);

        AiTestCaseGenerationResponse response = new AiTestCaseGenerationResponse();
        response.setMessage("AI test case generation completed");
        response.setRawAiResponse(rawAiResponse);
        response.setGeneratedTestCases(generatedTestCases);
        return response;
    }

    private String resolveFrequencyRuleType(String ruleName) {
        if (ruleName == null) return "HIGH_FREQ_TXN";
        String lower = ruleName.toLowerCase();
        if (lower.contains("annual")) return "ANNUAL_TXN_VOLUME";
        if (lower.contains("monthly")) return "MONTHLY_TXN_VOLUME";
        return "HIGH_FREQ_TXN";
    }

    private void enrichFromScenario(AiTestCaseGenerationRequest request) {
        TestScenarioEntity scenario = testScenarioRepository.findById(request.getScenarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test scenario not found with id: " + request.getScenarioId()));

        RuleEntity rule = scenario.getRule();
        if (rule != null) {
            if (isBlank(request.getRuleName()))          request.setRuleName(rule.getRuleName());
            if (isBlank(request.getRuleType()))          request.setRuleType(rule.getRuleType());
            if (request.getAction() == null)             request.setAction(rule.getAction());
            if (request.getTxnCount() == null)           request.setTxnCount(rule.getTxnCount());
            if (request.getTxnAmount() == null)          request.setTxnAmount(rule.getTxnAmount());
            if (request.getFrequencyHours() == null)     request.setFrequencyHours(rule.getFrequencyHours());
            if (request.getMaxAmount() == null)          request.setMaxAmount(rule.getMaxAmount());
            if (request.getPercentageThreshold() == null) request.setPercentageThreshold(rule.getPercentageThreshold());
            if (isBlank(request.getRuleDescription()))   request.setRuleDescription(rule.getRuleDescription());
        }
    }

    private void enrichFromRule(AiTestCaseGenerationRequest request) {
        RuleEntity rule = ruleRepository.findById(request.getRuleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rule not found with id: " + request.getRuleId()));

        if (isBlank(request.getRuleName()))          request.setRuleName(rule.getRuleName());
        if (isBlank(request.getRuleType()))          request.setRuleType(rule.getRuleType());
        if (request.getAction() == null)             request.setAction(rule.getAction());
        if (request.getTxnCount() == null)           request.setTxnCount(rule.getTxnCount());
        if (request.getTxnAmount() == null)          request.setTxnAmount(rule.getTxnAmount());
        if (request.getFrequencyHours() == null)     request.setFrequencyHours(rule.getFrequencyHours());
        if (request.getMaxAmount() == null)          request.setMaxAmount(rule.getMaxAmount());
        if (request.getPercentageThreshold() == null) request.setPercentageThreshold(rule.getPercentageThreshold());
        if (isBlank(request.getRuleDescription()))   request.setRuleDescription(rule.getRuleDescription());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public AiFailureAnalysisResponse analyzeFailure(AiFailureAnalysisRequest request) {
        if (request == null) {
            throw new BadRequestException("Failure analysis request cannot be null");
        }

        log.info("[AI BACKEND] Failure analysis incoming request: executionId={}, testCaseName={}, ruleType={}, actualResult={}, executionLogs={}",
                request.getExecutionId(), request.getTestCaseName(), request.getRuleType(),
                request.getActualResult(), request.getExecutionLogs());

        FastApiAnalyzeFailureRequest fastApiRequest = buildAnalyzeFailureRequest(request);

        // Let AiServiceException propagate — controller handles success:false response
        String rawAiResponse = aiServiceClient.analyzeFailure(fastApiRequest);
        AiFailureAnalysisResponse response = aiResponseParserService.parseFailureAnalysis(rawAiResponse);
        response.setMessage("AI failure analysis completed");
        return response;
    }

    @Override
    public AiRuleExplanationResponse explainRule(AiRuleExplanationRequest request) {
        if (request == null) {
            throw new BadRequestException("Rule explanation request cannot be null");
        }

        FastApiExplainRuleRequest fastApiRequest = buildExplainRuleRequest(request);

        // Let AiServiceException propagate — controller handles the clean success:false response
        String rawAiResponse = aiServiceClient.explainRule(fastApiRequest);
        AiRuleExplanationResponse response = aiResponseParserService.parseRuleExplanation(rawAiResponse);
        response.setMessage("AI rule explanation completed");
        return response;
    }

    @Override
    public AiRuleExplanationResponse explainRuleById(Long ruleId) {
        RuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found with id: " + ruleId));

        AiRuleExplanationRequest request = new AiRuleExplanationRequest();
        request.setRuleId(rule.getRuleId());
        request.setRuleName(rule.getRuleName());
        request.setRuleType(rule.getRuleType());
        request.setAction(rule.getAction());
        request.setStatus(rule.getStatus());
        request.setMccCode(rule.getMccCode());
        request.setTxnCount(rule.getTxnCount());
        request.setFrequencyHours(rule.getFrequencyHours());
        request.setTxnAmount(rule.getTxnAmount());
        request.setMaxAmount(rule.getMaxAmount());
        request.setPercentageThreshold(rule.getPercentageThreshold());
        request.setRuleDescription(rule.getRuleDescription());

        return explainRule(request);
    }

    @Override
    public AiFailureAnalysisResponse analyzeFailureById(Long executionId) {
        TestExecutionEntity execution = testExecutionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found with id: " + executionId));

        AiFailureAnalysisRequest request = new AiFailureAnalysisRequest();
        request.setExecutionId(execution.getExecutionId());

        if (execution.getTestCase() != null) {
            request.setTestCaseId(execution.getTestCase().getTestCaseId());
            request.setTestCaseName(execution.getTestCase().getTestCaseName());
        }

        if (execution.getExecutionStatus() != null) {
            request.setFailureMessage("Execution status: " + execution.getExecutionStatus().name());
        }

        return analyzeFailure(request);
    }

    @Override
    public AiGenerateTransactionResponse generateTransaction(AiGenerateTransactionRequest request) {
        FastApiGenerateTransactionRequest fastApiRequest = buildGenerateTransactionRequest(request);

        try {
            String rawAiResponse = aiServiceClient.generateTransaction(fastApiRequest);
            String parsed = aiResponseParserService.extractText(rawAiResponse);

            AiGenerateTransactionResponse response = new AiGenerateTransactionResponse();
            response.setExplanation(parsed);
            response.setCurrency(request != null && request.getCurrency() != null ? request.getCurrency() : "USD");
            response.setTransactionType(request != null && request.getTransactionType() != null ? request.getTransactionType() : "PURCHASE");
            response.setChannel(request != null && request.getChannel() != null ? request.getChannel() : "ONLINE");
            response.setCountry(request != null && request.getCountry() != null ? request.getCountry() : "US");
            return response;
        } catch (AiServiceException e) {
            return buildFallbackTransaction(request);
        }
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Chat message cannot be empty");
        }

        try {
            String rawAiResponse = aiServiceClient.chat(request.getMessage());
            String replyText = aiResponseParserService.extractText(rawAiResponse);

            AiChatResponse response = new AiChatResponse();
            response.setReply(replyText);
            response.setContext(request.getContext());
            return response;
        } catch (AiServiceException e) {
            AiChatResponse response = new AiChatResponse();
            response.setReply("AI service is currently unavailable. Your question was: \""
                    + request.getMessage()
                    + "\". Please try again later or consult your FRMS documentation.");
            response.setContext(Map.of());
            return response;
        }
    }

    @Override
    public AiGenerateRuleResponse generateRule(AiGenerateRuleRequest request) {
        if (request == null || request.getRequirement() == null || request.getRequirement().isBlank()) {
            throw new BadRequestException("Requirement cannot be empty for AI rule generation");
        }

        FastApiGenerateRuleRequest fastApiRequest = new FastApiGenerateRuleRequest();
        fastApiRequest.setRequirement(request.getRequirement());

        // Let AiServiceException propagate — controller handles clean error response
        String rawResponse = aiServiceClient.generateRule(fastApiRequest);
        return aiResponseParserService.parseRuleSuggestion(rawResponse);
    }

    @Override
    public Map<String, Object> getHealth() {
        try {
            String rawResponse = aiServiceClient.getHealth();
            Map<String, Object> healthData = new LinkedHashMap<>();
            healthData.put("aiServiceStatus", "UP");
            healthData.put("aiServiceResponse", rawResponse);
            healthData.put("springBootStatus", "UP");
            return healthData;
        } catch (AiServiceException e) {
            Map<String, Object> healthData = new LinkedHashMap<>();
            healthData.put("aiServiceStatus", "DOWN");
            healthData.put("aiServiceError", "AI service is currently unavailable");
            healthData.put("springBootStatus", "UP");
            return healthData;
        }
    }

    // ── mapping helpers ──────────────────────────────────────────────────────

    private FastApiTestCasesRequest buildTestCasesRequest(AiTestCaseGenerationRequest req) {
        FastApiTestCasesRequest r = new FastApiTestCasesRequest();
        r.setRuleName(req.getRuleName());
        r.setRuleType(req.getRuleType());
        r.setAction(req.getAction() != null ? req.getAction().name() : null);
        r.setTxnCount(req.getTxnCount());
        r.setTxnAmount(req.getTxnAmount() != null ? req.getTxnAmount().doubleValue() : null);
        r.setFrequency(req.getFrequencyHours());
        r.setMaxAmount(req.getMaxAmount() != null ? req.getMaxAmount().doubleValue() : null);
        r.setPercentageThreshold(req.getPercentageThreshold() != null ? req.getPercentageThreshold().doubleValue() : null);
        r.setDescription(req.getRuleDescription());
        return r;
    }

    private FastApiExplainRuleRequest buildExplainRuleRequest(AiRuleExplanationRequest req) {
        FastApiExplainRuleRequest r = new FastApiExplainRuleRequest();
        r.setRuleName(req.getRuleName());
        r.setRuleType(req.getRuleType());
        r.setAction(req.getAction() != null ? req.getAction().name() : null);
        r.setMaxAmount(req.getMaxAmount() != null ? req.getMaxAmount().doubleValue() : null);
        r.setDescription(req.getRuleDescription());
        return r;
    }

    private FastApiAnalyzeFailureRequest buildAnalyzeFailureRequest(AiFailureAnalysisRequest req) {
        FastApiAnalyzeFailureRequest r = new FastApiAnalyzeFailureRequest();
        r.setTestCaseName(req.getTestCaseName());
        r.setRuleType(req.getRuleType());

        // FastAPI expects expectedResult as a simple string (e.g. "MONITOR", "FAIL")
        // Prefer expectedAction, fall back to expectedOutcome, then legacy actualAction
        r.setExpectedResult(resolveExpectedResultString(req));

        // Frontend sends "actualResult": "FAILED" — use it directly; legacy fallback for analyzeFailureById
        if (req.getActualResult() != null && !req.getActualResult().isBlank()) {
            r.setActualResult(req.getActualResult());
        } else if (req.getActualAction() != null) {
            r.setActualResult(req.getActualAction().name());
        } else {
            r.setActualResult(req.getActualEvaluationStatus());
        }

        // Pass inputData as Object so Jackson serializes it as {} not "{}"
        r.setInputData(req.getInputData() != null ? req.getInputData() : java.util.Collections.emptyMap());

        // Frontend sends "executionLogs": "..." — use it directly; legacy fallback for analyzeFailureById
        if (req.getExecutionLogs() != null && !req.getExecutionLogs().isBlank()) {
            r.setExecutionLogs(req.getExecutionLogs());
        } else {
            r.setExecutionLogs(req.getFailureMessage() != null
                    ? req.getFailureMessage()
                    : "Execution failed or actual result did not match expected result");
        }

        return r;
    }

    private String resolveExpectedResultString(AiFailureAnalysisRequest req) {
        if (req.getExpectedResult() == null) return null;
        // Prefer expectedAction (e.g. "MONITOR") over expectedOutcome (e.g. "FAIL")
        if (req.getExpectedResult().getExpectedAction() != null) {
            return req.getExpectedResult().getExpectedAction().name();
        }
        if (req.getExpectedResult().getExpectedOutcome() != null) {
            return req.getExpectedResult().getExpectedOutcome();
        }
        return null;
    }

    private FastApiGenerateTransactionRequest buildGenerateTransactionRequest(AiGenerateTransactionRequest req) {
        FastApiGenerateTransactionRequest r = new FastApiGenerateTransactionRequest();
        if (req != null) {
            r.setRuleType(req.getTransactionType());
            r.setCurrency(req.getCurrency());
            r.setAmount(req.getMaxAmount());
            r.setExpectedResult("ACCEPT");
        }
        return r;
    }

    // ── fallback builders ────────────────────────────────────────────────────

    private AiGenerateTransactionResponse buildFallbackTransaction(AiGenerateTransactionRequest request) {
        AiGenerateTransactionResponse response = new AiGenerateTransactionResponse();
        response.setCardNumber("4532015112830366");
        response.setAmount(500.00);
        response.setCurrency(request != null && request.getCurrency() != null ? request.getCurrency() : "USD");
        response.setMerchantId("M001");
        response.setMerchantName("Sample Merchant");
        response.setMerchantCategory("RETAIL");
        response.setTransactionType(request != null && request.getTransactionType() != null ? request.getTransactionType() : "PURCHASE");
        response.setChannel(request != null && request.getChannel() != null ? request.getChannel() : "ONLINE");
        response.setCountry(request != null && request.getCountry() != null ? request.getCountry() : "US");
        response.setExplanation("AI service unavailable — sample transaction returned.");
        return response;
    }

}