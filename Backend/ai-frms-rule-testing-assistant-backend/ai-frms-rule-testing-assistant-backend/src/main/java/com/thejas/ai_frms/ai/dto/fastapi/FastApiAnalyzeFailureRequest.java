package com.thejas.ai_frms.ai.dto.fastapi;

import java.util.List;
import java.util.Map;

/**
 * Request body sent to FastAPI POST /api/ai/analyze-failure.
 *
 * FastAPI expects camelCase field names — no @JsonProperty annotations needed.
 *
 * Fields enriched by Spring Boot before sending:
 *   ruleConfig        — rule thresholds relevant to ruleType (e.g. txnCount, frequencyHours)
 *   testCaseInput     — masked transaction input (card numbers replaced with ****last4)
 *   failureReason     — human-readable failure description built from actual vs expected + counts
 *   matchedCount      — historicalCount + currentCount
 *   requiredCount     — txnCount threshold from rule config
 *   historicalTransactionCount — transactions found in history window
 *   currentCount      — always 1 (the current test transaction)
 *   frequencyWindow   — time window description (e.g. "last 1 hour")
 *   ruleExplanation   — full RuleEvaluationExplanationResponse object
 *   executionTrace    — step-by-step execution trace from rule engine
 *
 * FastAPI may use additional fields or ignore unknown ones as needed.
 */
public class FastApiAnalyzeFailureRequest {

    // ── Core identification ──────────────────────────────────────────────────
    private Long executionId;
    private String testCaseName;
    private String ruleType;

    // ── Result comparison ────────────────────────────────────────────────────
    private String expectedResult;   // plain string, e.g. "MONITOR"
    private String actualResult;     // plain string, e.g. "ACCEPT"

    // ── Input data ────────────────────────────────────────────────────────────
    private Object inputData;                    // legacy — raw object (backward compat)
    private Map<String, Object> testCaseInput;   // enriched — masked map (preferred by FastAPI)

    // ── Execution logs / failure reason ──────────────────────────────────────
    private String executionLogs;
    private String failureReason;

    // ── Rule configuration ────────────────────────────────────────────────────
    private Map<String, Object> ruleConfig;

    // ── Count-based enriched context ──────────────────────────────────────────
    private Integer matchedCount;
    private Integer requiredCount;
    private Integer historicalTransactionCount;
    private Integer currentCount;
    private String frequencyWindow;

    // ── Detailed execution context ────────────────────────────────────────────
    private Object ruleExplanation;   // RuleEvaluationExplanationResponse (as Object for JSON flexibility)
    private Object executionTrace;    // List<ExecutionTraceStepResponse> (as Object for JSON flexibility)

    // ── Getters and setters ───────────────────────────────────────────────────

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public String getTestCaseName() { return testCaseName; }
    public void setTestCaseName(String testCaseName) { this.testCaseName = testCaseName; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }

    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }

    public Object getInputData() { return inputData; }
    public void setInputData(Object inputData) { this.inputData = inputData; }

    public Map<String, Object> getTestCaseInput() { return testCaseInput; }
    public void setTestCaseInput(Map<String, Object> testCaseInput) { this.testCaseInput = testCaseInput; }

    public String getExecutionLogs() { return executionLogs; }
    public void setExecutionLogs(String executionLogs) { this.executionLogs = executionLogs; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Map<String, Object> getRuleConfig() { return ruleConfig; }
    public void setRuleConfig(Map<String, Object> ruleConfig) { this.ruleConfig = ruleConfig; }

    public Integer getMatchedCount() { return matchedCount; }
    public void setMatchedCount(Integer matchedCount) { this.matchedCount = matchedCount; }

    public Integer getRequiredCount() { return requiredCount; }
    public void setRequiredCount(Integer requiredCount) { this.requiredCount = requiredCount; }

    public Integer getHistoricalTransactionCount() { return historicalTransactionCount; }
    public void setHistoricalTransactionCount(Integer historicalTransactionCount) {
        this.historicalTransactionCount = historicalTransactionCount;
    }

    public Integer getCurrentCount() { return currentCount; }
    public void setCurrentCount(Integer currentCount) { this.currentCount = currentCount; }

    public String getFrequencyWindow() { return frequencyWindow; }
    public void setFrequencyWindow(String frequencyWindow) { this.frequencyWindow = frequencyWindow; }

    public Object getRuleExplanation() { return ruleExplanation; }
    public void setRuleExplanation(Object ruleExplanation) { this.ruleExplanation = ruleExplanation; }

    public Object getExecutionTrace() { return executionTrace; }
    public void setExecutionTrace(Object executionTrace) { this.executionTrace = executionTrace; }
}