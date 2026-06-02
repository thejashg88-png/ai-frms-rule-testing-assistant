package com.thejas.ai_frms.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejas.ai_frms.ai.dto.AiFailureAnalysisResponse;
import com.thejas.ai_frms.ai.dto.AiGenerateRuleResponse;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationResponse;
import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses raw JSON responses from the FastAPI AI service into typed Spring Boot DTOs.
 *
 * FastAPI always returns responses in the envelope format:
 *   { "success": true, "message": "...", "data": { ... } }
 *
 * Each parse method handles this envelope, extracts the "data" payload, and maps it
 * to the appropriate DTO. Both camelCase and snake_case keys from FastAPI are supported.
 *
 * Fallback behavior: if the response is not JSON or has an unexpected structure,
 * parsers degrade gracefully — they set the explanation/analysis fields to the raw string
 * rather than throwing an exception.
 */
@Service
public class AiResponseParserService {

    private final ObjectMapper objectMapper;

    public AiResponseParserService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Parses the AI-generated test cases from a FastAPI response string.
     * Tries three strategies in order:
     *   1. FastAPI envelope: data is a JSON array directly
     *   2. FastAPI envelope: data is an object with a nested test_cases / testCases array
     *   3. Fallback: scan the raw response string for any JSON array bracket
     */
    public List<TestCaseCreateRequest> parseGeneratedTestCases(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return Collections.emptyList();
        }

        try {
            // Try parsing FastAPI format: {success, message, data: [...]}
            Map<String, Object> responseMap = objectMapper.readValue(
                    aiResponse, new TypeReference<Map<String, Object>>() {}
            );
            Object data = responseMap.get("data");
            if (data != null) {
                String dataJson = objectMapper.writeValueAsString(data);
                if (dataJson.trim().startsWith("[")) {
                    return objectMapper.readValue(dataJson, new TypeReference<List<TestCaseCreateRequest>>() {});
                }
                // data might be an object with a test_cases or testCases field
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    for (String key : Arrays.asList("test_cases", "testCases", "cases", "items")) {
                        Object nested = dataMap.get(key);
                        if (nested != null) {
                            String nestedJson = objectMapper.writeValueAsString(nested);
                            if (nestedJson.trim().startsWith("[")) {
                                return objectMapper.readValue(nestedJson, new TypeReference<List<TestCaseCreateRequest>>() {});
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through to array extraction
        }

        // Fallback: find raw JSON array anywhere in the response
        try {
            String jsonArray = extractJsonArray(aiResponse);
            if (jsonArray != null) {
                return objectMapper.readValue(jsonArray, new TypeReference<List<TestCaseCreateRequest>>() {});
            }
        } catch (Exception ignored) {
        }

        return Collections.emptyList();
    }

    /**
     * Extracts a human-readable text string from an AI service response.
     * Handles both plain text and FastAPI structured responses: {success, message, data}.
     */
    public String extractText(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return "";
        }

        try {
            Map<String, Object> responseMap = objectMapper.readValue(
                    aiResponse, new TypeReference<Map<String, Object>>() {}
            );

            // FastAPI format: {success, message, data: {explanation/analysis/...}}
            Object data = responseMap.get("data");
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                for (String key : Arrays.asList("explanation", "analysis", "result", "response", "content", "text", "output")) {
                    Object val = dataMap.get(key);
                    if (val != null && !val.toString().isBlank()) {
                        return val.toString();
                    }
                }
                // Return entire data object as JSON if no known text field found
                try {
                    return objectMapper.writeValueAsString(data);
                } catch (Exception ignored) {
                    return data.toString();
                }
            }
            if (data instanceof String && !((String) data).isBlank()) {
                return (String) data;
            }

            // Legacy/other formats
            for (String key : Arrays.asList("response", "content", "text", "result", "output")) {
                Object val = responseMap.get(key);
                if (val != null && !val.toString().isBlank()) {
                    return val.toString();
                }
            }

            // message field last (FastAPI uses this for status messages, not AI content)
            Object message = responseMap.get("message");
            if (message != null && !message.toString().isBlank()) {
                return message.toString();
            }

            return aiResponse;
        } catch (Exception exception) {
            return aiResponse;
        }
    }

    /**
     * Parses FastAPI explain-rule response into AiRuleExplanationResponse.
     * FastAPI format: {success, message, data: {summary, businessMeaning, technicalMeaning, exampleScenario, riskNotes, ...}}
     * Accepts both camelCase and snake_case keys from FastAPI.
     */
    public AiRuleExplanationResponse parseRuleExplanation(String aiResponse) {
        AiRuleExplanationResponse response = new AiRuleExplanationResponse();
        // safe defaults
        response.setRiskLevel("MEDIUM");
        response.setRiskNotes(Collections.emptyList());
        response.setRecommendations(Collections.emptyList());
        response.setSuggestedTestCases(Collections.emptyList());
        response.setEdgeCases(Collections.emptyList());
        response.setRawAiResponse(aiResponse);

        if (aiResponse == null || aiResponse.isBlank()) {
            return response;
        }

        try {
            Map<String, Object> responseMap = objectMapper.readValue(
                    aiResponse, new TypeReference<Map<String, Object>>() {}
            );

            Object data = responseMap.get("data");
            if (!(data instanceof Map)) {
                // plain text or unknown format — put everything in explanation
                response.setExplanation(aiResponse);
                response.setSummary(aiResponse);
                return response;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> d = (Map<String, Object>) data;

            String summary       = stringOrNull(d, "summary");
            String businessMean  = stringOrNull(d, "businessMeaning", "business_meaning");
            String techMean      = stringOrNull(d, "technicalMeaning", "technical_meaning");
            String example       = stringOrNull(d, "exampleScenario", "example_scenario");
            String riskLevel     = stringOrNull(d, "riskLevel", "risk_level");

            response.setSummary(summary);
            response.setExplanation(summary != null ? summary : stringOrNull(d, "explanation", "content", "text"));
            response.setBusinessMeaning(businessMean);
            response.setTechnicalMeaning(techMean);
            response.setExampleScenario(example);
            response.setRiskLevel(riskLevel != null ? riskLevel : "MEDIUM");

            List<String> riskNotes = toStringList(d, "riskNotes", "risk_notes");
            response.setRiskNotes(riskNotes);

            List<String> recommendations = toStringList(d, "recommendations");
            // default recommendations to riskNotes when absent
            response.setRecommendations(!recommendations.isEmpty() ? recommendations : riskNotes);

        } catch (Exception e) {
            response.setExplanation(aiResponse);
            response.setSummary(aiResponse);
        }

        return response;
    }

    /**
     * Parses a FastAPI generate-rule response into an AiGenerateRuleResponse.
     * FastAPI format: {success, message, data: {rule_name, rule_type, ...}}
     */
    public AiGenerateRuleResponse parseRuleSuggestion(String aiResponse) {
        AiGenerateRuleResponse response = new AiGenerateRuleResponse();

        if (aiResponse == null || aiResponse.isBlank()) {
            return response;
        }

        try {
            Map<String, Object> responseMap = objectMapper.readValue(
                    aiResponse, new TypeReference<Map<String, Object>>() {}
            );

            Object data = responseMap.get("data");
            if (!(data instanceof Map)) {
                return response;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> d = (Map<String, Object>) data;

            response.setRuleName(stringOrNull(d, "rule_name", "ruleName"));
            response.setRuleDescription(stringOrNull(d, "rule_description", "ruleDescription", "description"));
            response.setRuleType(stringOrNull(d, "rule_type", "ruleType"));
            response.setAction(stringOrNull(d, "action"));
            response.setStatus(stringOrNull(d, "status"));
            response.setExplanation(stringOrNull(d, "explanation"));
            response.setRiskNotes(stringOrNull(d, "risk_notes", "riskNotes"));
            response.setTxnCount(intOrNull(d, "txn_count", "txnCount"));
            response.setFrequency(intOrNull(d, "frequency"));
            response.setTxnAmount(decimalOrNull(d, "txn_amount", "txnAmount"));
            response.setMaxAmount(decimalOrNull(d, "max_amount", "maxAmount"));
            response.setPercentageThreshold(decimalOrNull(d, "percentage_threshold", "percentageThreshold"));
        } catch (Exception ignored) {
        }

        return response;
    }

    private List<String> toStringList(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof List) {
                return ((List<?>) val).stream()
                        .filter(item -> item != null)
                        .map(Object::toString)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private String stringOrNull(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) return val.toString();
        }
        return null;
    }

    private Integer intOrNull(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof Number) return ((Number) val).intValue();
            if (val instanceof String) {
                try { return Integer.parseInt((String) val); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private BigDecimal decimalOrNull(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
            if (val instanceof String) {
                try { return new BigDecimal((String) val); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    /**
     * Parses FastAPI analyze-failure response into AiFailureAnalysisResponse.
     * FastAPI format: {success, message, data: {possibleReasons, debuggingSteps, recommendedFix, riskImpact}}
     */
    public AiFailureAnalysisResponse parseFailureAnalysis(String aiResponse) {
        AiFailureAnalysisResponse response = new AiFailureAnalysisResponse();
        response.setRawAiResponse(aiResponse);
        response.setConfidence(80);

        if (aiResponse == null || aiResponse.isBlank()) {
            return response;
        }

        try {
            Map<String, Object> responseMap = objectMapper.readValue(
                    aiResponse, new TypeReference<Map<String, Object>>() {}
            );

            Object data = responseMap.get("data");
            if (!(data instanceof Map)) {
                response.setAnalysis(aiResponse);
                return response;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> d = (Map<String, Object>) data;

            List<String> possibleReasons  = toStringList(d, "possibleReasons",  "possible_reasons");
            List<String> debuggingSteps   = toStringList(d, "debuggingSteps",   "debugging_steps");
            String recommendedFix         = stringOrNull(d, "recommendedFix",   "recommended_fix");
            String riskImpact             = stringOrNull(d, "riskImpact",       "risk_impact");
            String rootCause              = stringOrNull(d, "rootCause",        "root_cause", "probableCause", "probable_cause");
            String summary                = stringOrNull(d, "summary",          "analysis",   "content");

            response.setPossibleReasons(possibleReasons);
            response.setDebuggingSteps(debuggingSteps);
            response.setRecommendedFix(recommendedFix);
            response.setRiskImpact(riskImpact);
            response.setRootCause(rootCause);
            response.setSummary(summary);
            response.setSuggestions(debuggingSteps);

            // Populate legacy fields so older frontend code still works
            response.setAnalysis(summary != null ? summary : (possibleReasons.isEmpty() ? null : String.join("; ", possibleReasons)));
            response.setProbableCause(rootCause != null ? rootCause : (possibleReasons.isEmpty() ? null : possibleReasons.get(0)));
            response.setRecommendation(recommendedFix);

        } catch (Exception e) {
            response.setAnalysis(aiResponse);
        }

        return response;
    }

    private String extractJsonArray(String text) {
        int startIndex = text.indexOf('[');
        int endIndex = text.lastIndexOf(']');

        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
            return null;
        }

        return text.substring(startIndex, endIndex + 1);
    }
}