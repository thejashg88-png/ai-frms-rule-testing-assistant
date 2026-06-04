package com.thejas.ai_frms.execution.dto;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.common.enums.RuleAction;
import lombok.Getter;
import lombok.Setter;
import com.thejas.ai_frms.execution.dto.RuleEvaluationExplanationResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Per-test-case result within a test execution.
 *
 * Key field semantics:
 *   expectedAction        — the action the rule engine was supposed to produce (from test case design)
 *   actualAction          — the action the rule engine actually produced at execution time
 *   resultStatus          — PASSED if expectedAction == actualAction, FAILED otherwise, ERROR on exception
 *   expectedOutcome       — test designer's meta-intent ("PASS" = this test should succeed, "FAIL" = expected to fail)
 *   failureReason         — human-readable mismatch description (e.g. "Expected MONITOR but got ACCEPT")
 *   ruleType              — the rule type that was evaluated (from the scenario's linked rule)
 *   inputAmount           — the transaction amount value used in this execution
 */
@Getter
@Setter
public class ExecutionResultResponse {

    private Long resultId;
    private Long executionId;

    private Long testCaseId;
    private String testCaseName;

    private ExecutionStatus resultStatus;

    private RuleAction expectedAction;
    private RuleAction actualAction;

    private String expectedOutcome;    // "PASS" / "FAIL" — test designer's intent
    private String failureReason;      // e.g. "Expected action MONITOR but actual action ACCEPT"
    private String ruleType;           // which rule type was evaluated
    private BigDecimal inputAmount;    // the amount that was tested

    private String expectedEvaluationStatus;
    private String actualEvaluationStatus;

    private String message;

    private ComparisonResult comparisonResult;

    private LocalDateTime executedAt;

    // Detailed rule evaluation explanation — populated from ComparisonResult JSON
    private RuleEvaluationExplanationResponse ruleExplanation;

    public RuleEvaluationExplanationResponse getRuleExplanation() { return ruleExplanation; }
    public void setRuleExplanation(RuleEvaluationExplanationResponse ruleExplanation) { this.ruleExplanation = ruleExplanation; }

    // Step-by-step execution trace — populated from ComparisonResult JSON (no extra DB column needed)
    private java.util.List<ExecutionTraceStepResponse> executionTrace;

    public java.util.List<ExecutionTraceStepResponse> getExecutionTrace() { return executionTrace; }
    public void setExecutionTrace(java.util.List<ExecutionTraceStepResponse> executionTrace) { this.executionTrace = executionTrace; }
}