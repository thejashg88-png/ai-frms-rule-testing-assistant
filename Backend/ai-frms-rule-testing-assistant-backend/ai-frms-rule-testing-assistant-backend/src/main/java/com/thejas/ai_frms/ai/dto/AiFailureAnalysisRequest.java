package com.thejas.ai_frms.ai.dto;

import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.execution.dto.ExecutionTraceStepResponse;
import com.thejas.ai_frms.execution.dto.RuleEvaluationExplanationResponse;
import com.thejas.ai_frms.testcase.dto.ExpectedResult;
import com.thejas.ai_frms.testcase.dto.TestInputData;

import java.util.List;
import java.util.Map;

/**
 * Request body for the AI failure analysis endpoint.
 *
 * Two usage patterns:
 *   1. Minimal call (executionId only) — backend auto-enriches all context from DB.
 *      The frontend just sends {"executionId": 118} and gets a rich AI explanation.
 *   2. Explicit call — caller provides ruleType, expectedResult, actualResult etc. directly.
 *      Backend still enriches any missing fields from DB if executionId is present.
 *
 * Enrichment flow (when executionId present):
 *   execution results → first FAILED result → comparisonResultJson → ruleExplanation, executionTrace
 *   testCase → inputDataJson (masked) → testCaseInput
 *   testCase.scenario.rule → ruleConfig
 *
 * Sensitive fields (cardNumber, track2Data, pan, accountNumber) are masked before
 * being sent to the AI service — only last-4 digits are visible.
 */
public class AiFailureAnalysisRequest {

    // ── Identifiers ──────────────────────────────────────────────────────────
    private Long executionId;
    private Long testCaseId;
    private String testCaseName;

    // ── Rule info ─────────────────────────────────────────────────────────────
    private String ruleName;
    private String ruleType;

    // ── Input data (legacy — raw TestInputData object) ────────────────────────
    private TestInputData inputData;

    // ── Result fields ─────────────────────────────────────────────────────────
    private ExpectedResult expectedResult;
    private String actualResult;
    private String executionLogs;

    // ── Legacy fields kept for analyzeFailureById() backward compat ────────────
    private RuleAction actualAction;
    private String actualEvaluationStatus;
    private String actualRuleType;
    private String failureMessage;

    // ── Enriched context fields (new) ─────────────────────────────────────────

    /** Rule config relevant to ruleType (e.g. txnCount, maxAmount, frequencyHours). */
    private Map<String, Object> ruleConfig;

    /** Masked test case input data — sensitive fields replaced with ****last4. */
    private Map<String, Object> testCaseInput;

    /** Human-readable failure description built from actual vs expected + counts. */
    private String failureReason;

    /** Total matched transactions (historicalCount + currentCount). */
    private Integer matchedCount;

    /** Threshold from rule config — how many transactions are needed to trigger. */
    private Integer requiredCount;

    /** Transactions found in history window before the current test transaction. */
    private Integer historicalTransactionCount;

    /** Always 1 — the current test transaction being evaluated. */
    private Integer currentCount;

    /** Description of the time window (e.g. "last 1 hour", "last 24 hours"). */
    private String frequencyWindow;

    /** Full rule evaluation explanation from the execution engine. */
    private RuleEvaluationExplanationResponse ruleExplanation;

    /** Step-by-step execution trace from the rule execution engine. */
    private List<ExecutionTraceStepResponse> executionTrace;

    // ── Getters and setters ───────────────────────────────────────────────────

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public Long getTestCaseId() { return testCaseId; }
    public void setTestCaseId(Long testCaseId) { this.testCaseId = testCaseId; }

    public String getTestCaseName() { return testCaseName; }
    public void setTestCaseName(String testCaseName) { this.testCaseName = testCaseName; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public TestInputData getInputData() { return inputData; }
    public void setInputData(TestInputData inputData) { this.inputData = inputData; }

    public ExpectedResult getExpectedResult() { return expectedResult; }
    public void setExpectedResult(ExpectedResult expectedResult) { this.expectedResult = expectedResult; }

    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }

    public String getExecutionLogs() { return executionLogs; }
    public void setExecutionLogs(String executionLogs) { this.executionLogs = executionLogs; }

    public RuleAction getActualAction() { return actualAction; }
    public void setActualAction(RuleAction actualAction) { this.actualAction = actualAction; }

    public String getActualEvaluationStatus() { return actualEvaluationStatus; }
    public void setActualEvaluationStatus(String actualEvaluationStatus) { this.actualEvaluationStatus = actualEvaluationStatus; }

    public String getActualRuleType() { return actualRuleType; }
    public void setActualRuleType(String actualRuleType) { this.actualRuleType = actualRuleType; }

    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }

    public Map<String, Object> getRuleConfig() { return ruleConfig; }
    public void setRuleConfig(Map<String, Object> ruleConfig) { this.ruleConfig = ruleConfig; }

    public Map<String, Object> getTestCaseInput() { return testCaseInput; }
    public void setTestCaseInput(Map<String, Object> testCaseInput) { this.testCaseInput = testCaseInput; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

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

    public RuleEvaluationExplanationResponse getRuleExplanation() { return ruleExplanation; }
    public void setRuleExplanation(RuleEvaluationExplanationResponse ruleExplanation) {
        this.ruleExplanation = ruleExplanation;
    }

    public List<ExecutionTraceStepResponse> getExecutionTrace() { return executionTrace; }
    public void setExecutionTrace(List<ExecutionTraceStepResponse> executionTrace) {
        this.executionTrace = executionTrace;
    }
}