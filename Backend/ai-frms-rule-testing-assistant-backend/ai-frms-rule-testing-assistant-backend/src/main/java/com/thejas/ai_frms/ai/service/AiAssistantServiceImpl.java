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
import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.common.exception.AiServiceException;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.common.util.JsonUtil;
import com.thejas.ai_frms.execution.dto.ComparisonResult;
import com.thejas.ai_frms.execution.dto.RuleEvaluationExplanationResponse;
import com.thejas.ai_frms.execution.entity.TestExecutionEntity;
import com.thejas.ai_frms.execution.entity.TestExecutionResultEntity;
import com.thejas.ai_frms.execution.repository.TestExecutionRepository;
import com.thejas.ai_frms.execution.repository.TestExecutionResultRepository;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.rule.repository.RuleRepository;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
import com.thejas.ai_frms.testcase.dto.ExpectedResult;
import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for AI assistant features.
 *
 * Acts as the bridge between the Spring Boot REST layer and the FastAPI Python AI service.
 *
 * Failure analysis enrichment:
 *   When executionId is provided, this service automatically fetches the failed execution
 *   result from the DB, extracts ruleType, counts, ruleConfig, masked input, ruleExplanation,
 *   and executionTrace — then sends the full enriched payload to FastAPI.
 *   This produces specific, accurate AI analysis rather than generic responses.
 */
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantServiceImpl.class);

    private final AiServiceClient aiServiceClient;
    private final AiResponseParserService aiResponseParserService;
    private final RuleRepository ruleRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestExecutionResultRepository testExecutionResultRepository;
    private final TestScenarioRepository testScenarioRepository;

    public AiAssistantServiceImpl(
            AiServiceClient aiServiceClient,
            AiResponseParserService aiResponseParserService,
            RuleRepository ruleRepository,
            TestExecutionRepository testExecutionRepository,
            TestExecutionResultRepository testExecutionResultRepository,
            TestScenarioRepository testScenarioRepository
    ) {
        this.aiServiceClient = aiServiceClient;
        this.aiResponseParserService = aiResponseParserService;
        this.ruleRepository = ruleRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.testExecutionResultRepository = testExecutionResultRepository;
        this.testScenarioRepository = testScenarioRepository;
    }

    // ── Test Case Generation ─────────────────────────────────────────────────

    @Override
    public AiTestCaseGenerationResponse generateTestCases(AiTestCaseGenerationRequest request) {
        if (request == null) {
            throw new BadRequestException("AI test case generation request cannot be null");
        }

        log.info("[AI BACKEND] Generate test cases: scenarioId={}, ruleId={}, ruleType={}, ruleName={}, action={}",
                request.getScenarioId(), request.getRuleId(), request.getRuleType(),
                request.getRuleName(), request.getAction());

        if (request.getScenarioId() != null) {
            log.info("[AI BACKEND] Using scenario-based generation: scenarioId={}", request.getScenarioId());
            enrichFromScenario(request);
        } else if (request.getRuleId() != null) {
            log.info("[AI BACKEND] Using rule-based generation via DB: ruleId={}", request.getRuleId());
            enrichFromRule(request);
        } else {
            log.info("[AI BACKEND] Using inline rule fields: ruleName={}, ruleType={}",
                    request.getRuleName(), request.getRuleType());
        }

        if (isBlank(request.getRuleType()) && isBlank(request.getRuleName())) {
            throw new BadRequestException(
                    "Either scenarioId or rule details (ruleName, ruleType, action) are required for AI test case generation");
        }

        if (request.getAction() == null) {
            request.setAction(RuleAction.MONITOR);
        }

        if (request.getNumberOfTestCases() == null || request.getNumberOfTestCases() <= 0) {
            request.setNumberOfTestCases(5);
        }

        if ("FREQUENCY".equalsIgnoreCase(request.getRuleType())) {
            request.setRuleType(resolveFrequencyRuleType(request.getRuleName()));
            log.info("[AI BACKEND] Normalized ruleType to: {}", request.getRuleType());
        }

        FastApiTestCasesRequest fastApiRequest = buildTestCasesRequest(request);
        log.info("[AI CLIENT] Generate test cases: ruleName={}, ruleType={}, action={}, txnCount={}, txnAmount={}, frequency={}, maxAmount={}",
                fastApiRequest.getRuleName(), fastApiRequest.getRuleType(), fastApiRequest.getAction(),
                fastApiRequest.getTxnCount(), fastApiRequest.getTxnAmount(),
                fastApiRequest.getFrequency(), fastApiRequest.getMaxAmount());

        String rawAiResponse = aiServiceClient.generateTestCases(fastApiRequest);
        List<TestCaseCreateRequest> generatedTestCases =
                aiResponseParserService.parseGeneratedTestCases(rawAiResponse);

        AiTestCaseGenerationResponse response = new AiTestCaseGenerationResponse();
        response.setMessage("AI test case generation completed");
        response.setRawAiResponse(rawAiResponse);
        response.setGeneratedTestCases(generatedTestCases);
        return response;
    }

    // ── Failure Analysis ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AiFailureAnalysisResponse analyzeFailure(AiFailureAnalysisRequest request) {
        if (request == null) {
            throw new BadRequestException("Failure analysis request cannot be null");
        }

        // Auto-enrich from DB if executionId is present
        if (request.getExecutionId() != null) {
            try {
                enrichFromExecution(request);
            } catch (Exception e) {
                log.warn("[AI FAILURE CONTEXT] Enrichment failed for executionId={}: {}",
                        request.getExecutionId(), e.getMessage());
            }
        }

        // Log enriched context (no sensitive data)
        log.info("[AI FAILURE CONTEXT] executionId={}", request.getExecutionId());
        log.info("[AI FAILURE CONTEXT] ruleType={}", request.getRuleType());
        log.info("[AI FAILURE CONTEXT] ruleConfig={}", request.getRuleConfig());
        log.info("[AI FAILURE CONTEXT] expectedAction={}",
                request.getExpectedResult() != null ? request.getExpectedResult().getExpectedAction() : null);
        log.info("[AI FAILURE CONTEXT] actualAction={}", request.getActualResult());
        log.info("[AI FAILURE CONTEXT] matchedCount={}", request.getMatchedCount());
        log.info("[AI FAILURE CONTEXT] historicalCount={}", request.getHistoricalTransactionCount());
        log.info("[AI FAILURE CONTEXT] requiredCount={}", request.getRequiredCount());

        FastApiAnalyzeFailureRequest fastApiRequest = buildAnalyzeFailureRequest(request);
        String rawAiResponse = aiServiceClient.analyzeFailure(fastApiRequest);
        AiFailureAnalysisResponse response = aiResponseParserService.parseFailureAnalysis(rawAiResponse);
        response.setMessage("AI failure analysis completed");
        return response;
    }

    @Override
    public AiFailureAnalysisResponse analyzeFailureById(Long executionId) {
        if (!testExecutionRepository.existsById(executionId)) {
            throw new ResourceNotFoundException("Execution not found with id: " + executionId);
        }
        AiFailureAnalysisRequest request = new AiFailureAnalysisRequest();
        request.setExecutionId(executionId);
        return analyzeFailure(request);
    }

    // ── Rule Explanation ─────────────────────────────────────────────────────

    @Override
    public AiRuleExplanationResponse explainRule(AiRuleExplanationRequest request) {
        if (request == null) {
            throw new BadRequestException("Rule explanation request cannot be null");
        }
        FastApiExplainRuleRequest fastApiRequest = buildExplainRuleRequest(request);
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

    // ── Generate Transaction ─────────────────────────────────────────────────

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

    // ── Chat ─────────────────────────────────────────────────────────────────

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

    // ── Generate Rule ────────────────────────────────────────────────────────

    @Override
    public AiGenerateRuleResponse generateRule(AiGenerateRuleRequest request) {
        if (request == null || request.getRequirement() == null || request.getRequirement().isBlank()) {
            throw new BadRequestException("Requirement cannot be empty for AI rule generation");
        }
        FastApiGenerateRuleRequest fastApiRequest = new FastApiGenerateRuleRequest();
        fastApiRequest.setRequirement(request.getRequirement());
        String rawResponse = aiServiceClient.generateRule(fastApiRequest);
        return aiResponseParserService.parseRuleSuggestion(rawResponse);
    }

    // ── Health ───────────────────────────────────────────────────────────────

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

    // ═════════════════════════════════════════════════════════════════════════
    //  Failure analysis enrichment
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Enriches the failure analysis request with full execution context from the DB.
     *
     * Must be called within a @Transactional context to allow lazy loading of
     * testCase → scenario → rule relationships.
     *
     * Enrichment priority: DB-fetched data fills missing fields only; caller-provided
     * values are never overwritten (so explicit request fields always take precedence).
     */
    private void enrichFromExecution(AiFailureAnalysisRequest req) {
        Long executionId = req.getExecutionId();
        log.info("[AI FAILURE CONTEXT] Enriching from executionId={}", executionId);

        List<TestExecutionResultEntity> results =
                testExecutionResultRepository.findByExecutionExecutionIdOrderByExecutedAtAsc(executionId);

        if (results.isEmpty()) {
            log.warn("[AI FAILURE CONTEXT] No results found for executionId={}", executionId);
            return;
        }

        // Pick first FAILED or ERROR result; fall back to first result available
        TestExecutionResultEntity result = results.stream()
                .filter(r -> r.getResultStatus() == ExecutionStatus.FAILED
                          || r.getResultStatus() == ExecutionStatus.ERROR)
                .findFirst()
                .orElse(results.get(0));

        log.info("[AI FAILURE CONTEXT] Using resultId={}, status={}", result.getResultId(), result.getResultStatus());

        // ── Basic fields from result entity ───────────────────────────────────
        if (result.getTestCase() != null) {
            if (isBlank(req.getTestCaseName()))  req.setTestCaseName(result.getTestCase().getTestCaseName());
            if (req.getTestCaseId() == null)     req.setTestCaseId(result.getTestCase().getTestCaseId());
        }

        if (isBlank(req.getActualResult()) && result.getActualAction() != null) {
            req.setActualResult(result.getActualAction().name());
        }

        if (req.getExpectedResult() == null && result.getExpectedAction() != null) {
            ExpectedResult expected = new ExpectedResult();
            expected.setExpectedAction(result.getExpectedAction());
            expected.setExpectedOutcome(result.getExpectedOutcome());
            req.setExpectedResult(expected);
        }

        // ── Parse comparisonResultJson ─────────────────────────────────────────
        ComparisonResult cr = parseComparisonResult(result.getComparisonResultJson());

        if (cr != null) {
            if (isBlank(req.getRuleType()) && cr.getRuleType() != null) {
                req.setRuleType(cr.getRuleType());
            }
            if (req.getRuleExplanation() == null && cr.getRuleExplanation() != null) {
                req.setRuleExplanation(cr.getRuleExplanation());
            }
            if (req.getExecutionTrace() == null && cr.getExecutionTrace() != null) {
                req.setExecutionTrace(cr.getExecutionTrace());
            }
        }

        // ── Extract counts from ruleExplanation ───────────────────────────────
        RuleEvaluationExplanationResponse expl = req.getRuleExplanation();
        if (expl != null) {
            if (req.getMatchedCount() == null)               req.setMatchedCount(expl.getMatchedCount());
            if (req.getHistoricalTransactionCount() == null) req.setHistoricalTransactionCount(expl.getHistoricalCount());
            if (req.getCurrentCount() == null)               req.setCurrentCount(expl.getCurrentCount());
            if (req.getRequiredCount() == null)              req.setRequiredCount(expl.getRequiredCount());
            if (isBlank(req.getFrequencyWindow()))           req.setFrequencyWindow(expl.getFrequencyWindow());
            if (isBlank(req.getRuleType()) && expl.getRuleType() != null) {
                req.setRuleType(expl.getRuleType());
            }
        }

        // ── Build failureReason string ────────────────────────────────────────
        if (isBlank(req.getFailureReason())) {
            req.setFailureReason(buildFailureReason(result, req));
        }

        // ── Navigate testCase → scenario → rule (lazy loads within @Transactional) ──
        if (req.getRuleConfig() == null && result.getTestCase() != null) {
            try {
                TestCaseEntity testCase = result.getTestCase();
                TestScenarioEntity scenario = testCase.getScenario();
                if (scenario != null) {
                    RuleEntity rule = scenario.getRule();
                    if (rule != null) {
                        if (isBlank(req.getRuleName()))  req.setRuleName(rule.getRuleName());
                        if (isBlank(req.getRuleType()))  req.setRuleType(rule.getRuleType());
                        req.setRuleConfig(buildRuleConfig(rule));
                        log.info("[AI FAILURE CONTEXT] Built ruleConfig for ruleType={}", rule.getRuleType());
                    }
                }
            } catch (Exception e) {
                log.warn("[AI FAILURE CONTEXT] Could not load rule via testCase chain: {}", e.getMessage());
            }
        }

        // ── Build masked testCaseInput from testCase.inputDataJson ────────────
        if (req.getTestCaseInput() == null && result.getTestCase() != null) {
            try {
                String inputJson = result.getTestCase().getInputDataJson();
                if (inputJson != null && !inputJson.isBlank()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawInput = JsonUtil.fromJson(inputJson, Map.class);
                    req.setTestCaseInput(maskSensitiveFields(rawInput));
                    log.info("[AI FAILURE CONTEXT] Built masked testCaseInput");
                }
            } catch (Exception e) {
                log.warn("[AI FAILURE CONTEXT] Could not parse/mask testCase input: {}", e.getMessage());
            }
        }
    }

    /**
     * Builds a human-readable failure reason from the result entity and enriched counts.
     * Format: "Expected action MONITOR but actual action ACCEPT. Matched count=1 (historical=0 + current=1), required=3. Window: last 1 hour."
     */
    private String buildFailureReason(TestExecutionResultEntity result, AiFailureAnalysisRequest req) {
        // Use stored failureReason if it already contains detailed info
        String stored = result.getFailureReason();

        StringBuilder sb = new StringBuilder();

        // Base: expected vs actual action
        if (result.getExpectedAction() != null && result.getActualAction() != null) {
            sb.append("Expected action ").append(result.getExpectedAction().name())
              .append(" but actual action ").append(result.getActualAction().name()).append(".");
        } else if (!isBlank(stored)) {
            return stored;
        }

        // Append count context
        if (req.getMatchedCount() != null && req.getRequiredCount() != null) {
            sb.append(" Matched count=").append(req.getMatchedCount());
            if (req.getHistoricalTransactionCount() != null && req.getCurrentCount() != null) {
                sb.append(" (historical=").append(req.getHistoricalTransactionCount())
                  .append(" + current=").append(req.getCurrentCount()).append(")");
            }
            sb.append(", required=").append(req.getRequiredCount()).append(".");
        }

        // Append frequency window
        if (!isBlank(req.getFrequencyWindow())) {
            sb.append(" Window: ").append(req.getFrequencyWindow()).append(".");
        }

        // Prefer richer stored reason over minimal constructed one
        if (sb.length() > 0 && !isBlank(stored) && stored.length() > sb.length()) {
            return stored;
        }

        return sb.length() > 0 ? sb.toString().trim() : stored;
    }

    /**
     * Builds a rule configuration map containing only the fields relevant to ruleType.
     * Unknown ruleTypes get all non-null config fields.
     */
    private Map<String, Object> buildRuleConfig(RuleEntity rule) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("ruleName", rule.getRuleName());
        config.put("ruleType", rule.getRuleType());
        if (rule.getAction() != null) config.put("action", rule.getAction().name());

        String ruleType = rule.getRuleType();
        if (ruleType == null) return config;

        switch (ruleType.toUpperCase()) {
            case "SINGLE_LARGE_TX" ->
                putIfNotNull(config, "maxAmount", rule.getMaxAmount());
            case "HIGH_FREQ_TXN", "SEQUENTIAL_TXN" -> {
                putIfNotNull(config, "txnCount", rule.getTxnCount());
                putIfNotNull(config, "frequencyHours", rule.getFrequencyHours());
            }
            case "UNUSUAL_AMT" ->
                putIfNotNull(config, "percentageThreshold", rule.getPercentageThreshold());
            case "STRUCTURING" -> {
                putIfNotNull(config, "txnCount", rule.getTxnCount());
                putIfNotNull(config, "txnAmount", rule.getTxnAmount());
                putIfNotNull(config, "frequencyHours", rule.getFrequencyHours());
            }
            default -> {
                // Include all available config for unknown rule types
                putIfNotNull(config, "txnCount", rule.getTxnCount());
                putIfNotNull(config, "txnAmount", rule.getTxnAmount());
                putIfNotNull(config, "maxAmount", rule.getMaxAmount());
                putIfNotNull(config, "frequencyHours", rule.getFrequencyHours());
                putIfNotNull(config, "percentageThreshold", rule.getPercentageThreshold());
            }
        }
        return config;
    }

    /**
     * Masks sensitive fields in an input map.
     * Fields with names containing "card", "track", "pan", "account", "cvv", "pin"
     * are masked: only the last 4 characters remain, prefixed with ****.
     */
    private Map<String, Object> maskSensitiveFields(Map<String, Object> input) {
        if (input == null) return null;
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String s && isSensitiveField(key)) {
                masked.put(key, maskValue(s));
            } else {
                masked.put(key, value);
            }
        }
        return masked;
    }

    /**
     * Converts a TestInputData object to a masked map for sending to the AI service.
     * Never sends full card numbers or track data.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> maskInputData(Object inputData) {
        if (inputData == null) return Collections.emptyMap();
        try {
            String json = JsonUtil.toJson(inputData);
            Map<String, Object> map = JsonUtil.fromJson(json, Map.class);
            return maskSensitiveFields(map);
        } catch (Exception e) {
            log.warn("[AI FAILURE CONTEXT] Could not convert/mask inputData: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private boolean isSensitiveField(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return lower.contains("card") || lower.contains("track")
            || lower.contains("pan")  || lower.contains("account")
            || lower.contains("cvv")  || lower.contains("pin");
    }

    private String maskValue(String value) {
        if (value == null || value.length() < 4) return "****";
        return "****" + value.substring(Math.max(0, value.length() - 4));
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }

    private ComparisonResult parseComparisonResult(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return JsonUtil.fromJson(json, ComparisonResult.class);
        } catch (Exception e) {
            log.warn("[AI FAILURE CONTEXT] Could not parse comparisonResultJson: {}", e.getMessage());
            return null;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  FastAPI payload builders
    // ═════════════════════════════════════════════════════════════════════════

    private FastApiAnalyzeFailureRequest buildAnalyzeFailureRequest(AiFailureAnalysisRequest req) {
        FastApiAnalyzeFailureRequest r = new FastApiAnalyzeFailureRequest();

        // Core
        r.setExecutionId(req.getExecutionId());
        r.setTestCaseName(req.getTestCaseName());
        r.setRuleType(req.getRuleType());

        // Expected result → simple string for FastAPI (e.g. "MONITOR")
        r.setExpectedResult(resolveExpectedResultString(req));

        // Actual result
        if (!isBlank(req.getActualResult())) {
            r.setActualResult(req.getActualResult());
        } else if (req.getActualAction() != null) {
            r.setActualResult(req.getActualAction().name());
        } else {
            r.setActualResult(req.getActualEvaluationStatus());
        }

        // Input data — prefer enriched masked map; fall back to masked legacy inputData
        Map<String, Object> maskedInput;
        if (req.getTestCaseInput() != null) {
            maskedInput = req.getTestCaseInput();
        } else if (req.getInputData() != null) {
            maskedInput = maskInputData(req.getInputData());
        } else {
            maskedInput = Collections.emptyMap();
        }
        r.setInputData(maskedInput);
        r.setTestCaseInput(maskedInput);

        // Execution logs / failure reason
        if (!isBlank(req.getFailureReason())) {
            r.setFailureReason(req.getFailureReason());
            r.setExecutionLogs(req.getFailureReason());
        } else if (!isBlank(req.getExecutionLogs())) {
            r.setExecutionLogs(req.getExecutionLogs());
        } else if (req.getFailureMessage() != null) {
            r.setExecutionLogs(req.getFailureMessage());
        } else {
            r.setExecutionLogs("Execution failed or actual result did not match expected result");
        }

        // Enriched context
        r.setRuleConfig(req.getRuleConfig());
        r.setMatchedCount(req.getMatchedCount());
        r.setRequiredCount(req.getRequiredCount());
        r.setHistoricalTransactionCount(req.getHistoricalTransactionCount());
        r.setCurrentCount(req.getCurrentCount());
        r.setFrequencyWindow(req.getFrequencyWindow());
        r.setRuleExplanation(req.getRuleExplanation());
        r.setExecutionTrace(req.getExecutionTrace());

        return r;
    }

    private String resolveExpectedResultString(AiFailureAnalysisRequest req) {
        if (req.getExpectedResult() == null) return null;
        if (req.getExpectedResult().getExpectedAction() != null) {
            return req.getExpectedResult().getExpectedAction().name();
        }
        return req.getExpectedResult().getExpectedOutcome();
    }

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

    // ═════════════════════════════════════════════════════════════════════════
    //  Enrichment helpers (test case generation)
    // ═════════════════════════════════════════════════════════════════════════

    private void enrichFromScenario(AiTestCaseGenerationRequest request) {
        TestScenarioEntity scenario = testScenarioRepository.findById(request.getScenarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test scenario not found with id: " + request.getScenarioId()));

        RuleEntity rule = scenario.getRule();
        if (rule != null) {
            if (isBlank(request.getRuleName()))           request.setRuleName(rule.getRuleName());
            if (isBlank(request.getRuleType()))           request.setRuleType(rule.getRuleType());
            if (request.getAction() == null)              request.setAction(rule.getAction());
            if (request.getTxnCount() == null)            request.setTxnCount(rule.getTxnCount());
            if (request.getTxnAmount() == null)           request.setTxnAmount(rule.getTxnAmount());
            if (request.getFrequencyHours() == null)      request.setFrequencyHours(rule.getFrequencyHours());
            if (request.getMaxAmount() == null)           request.setMaxAmount(rule.getMaxAmount());
            if (request.getPercentageThreshold() == null) request.setPercentageThreshold(rule.getPercentageThreshold());
            if (isBlank(request.getRuleDescription()))    request.setRuleDescription(rule.getRuleDescription());
        }
    }

    private void enrichFromRule(AiTestCaseGenerationRequest request) {
        RuleEntity rule = ruleRepository.findById(request.getRuleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rule not found with id: " + request.getRuleId()));

        if (isBlank(request.getRuleName()))           request.setRuleName(rule.getRuleName());
        if (isBlank(request.getRuleType()))           request.setRuleType(rule.getRuleType());
        if (request.getAction() == null)              request.setAction(rule.getAction());
        if (request.getTxnCount() == null)            request.setTxnCount(rule.getTxnCount());
        if (request.getTxnAmount() == null)           request.setTxnAmount(rule.getTxnAmount());
        if (request.getFrequencyHours() == null)      request.setFrequencyHours(rule.getFrequencyHours());
        if (request.getMaxAmount() == null)           request.setMaxAmount(rule.getMaxAmount());
        if (request.getPercentageThreshold() == null) request.setPercentageThreshold(rule.getPercentageThreshold());
        if (isBlank(request.getRuleDescription()))    request.setRuleDescription(rule.getRuleDescription());
    }

    private String resolveFrequencyRuleType(String ruleName) {
        if (ruleName == null) return "HIGH_FREQ_TXN";
        String lower = ruleName.toLowerCase();
        if (lower.contains("annual"))  return "ANNUAL_TXN_VOLUME";
        if (lower.contains("monthly")) return "MONTHLY_TXN_VOLUME";
        return "HIGH_FREQ_TXN";
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Fallback builders
    // ═════════════════════════════════════════════════════════════════════════

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

    // ── Utility ──────────────────────────────────────────────────────────────

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}