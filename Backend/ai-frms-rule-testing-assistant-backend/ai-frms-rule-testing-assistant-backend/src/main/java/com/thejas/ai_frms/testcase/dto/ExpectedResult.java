package com.thejas.ai_frms.testcase.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.thejas.ai_frms.common.enums.RuleAction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Structured expected result for a test case.
 *
 * This is intentionally an object, not a plain string, because:
 *   - AI-generated test cases may produce either a JSON object or a plain string
 *   - ExpectedResultDeserializer handles both forms transparently
 *   - It carries multiple dimensions: action (MONITOR/REJECT/ACCEPT), outcome (PASS/FAIL),
 *     risk level, and optionally alert codes and risk score
 *
 * Key distinction:
 *   expectedAction  = the fraud rule action the engine should produce (MONITOR, REJECT, ACCEPT)
 *   expectedOutcome = the test designer's pass/fail intent ("PASS" means the test should pass)
 *
 * During execution, the engine compares expectedAction against the actual rule engine output.
 */
@JsonDeserialize(using = ExpectedResultDeserializer.class)
public class ExpectedResult {

    // "PASS" or "FAIL" — the test designer's intent, not the rule action
    private String expectedOutcome;
    // The fraud rule action the engine must produce for this test to pass (MONITOR, ACCEPT, REJECT)
    private RuleAction expectedAction;
    // Optional risk level expectation: "HIGH", "MEDIUM", "LOW"
    private String expectedRiskLevel;
    private String expectedEvaluationStatus;
    private String expectedRuleType;
    private List<String> expectedAlertCodes;
    private BigDecimal expectedRiskScore;
    private String remarks;

    public String getExpectedOutcome() { return expectedOutcome; }
    public void setExpectedOutcome(String expectedOutcome) { this.expectedOutcome = expectedOutcome; }

    public RuleAction getExpectedAction() { return expectedAction; }
    public void setExpectedAction(RuleAction expectedAction) { this.expectedAction = expectedAction; }

    public String getExpectedRiskLevel() { return expectedRiskLevel; }
    public void setExpectedRiskLevel(String expectedRiskLevel) { this.expectedRiskLevel = expectedRiskLevel; }

    public String getExpectedEvaluationStatus() { return expectedEvaluationStatus; }
    public void setExpectedEvaluationStatus(String expectedEvaluationStatus) { this.expectedEvaluationStatus = expectedEvaluationStatus; }

    public String getExpectedRuleType() { return expectedRuleType; }
    public void setExpectedRuleType(String expectedRuleType) { this.expectedRuleType = expectedRuleType; }

    public List<String> getExpectedAlertCodes() { return expectedAlertCodes; }
    public void setExpectedAlertCodes(List<String> expectedAlertCodes) { this.expectedAlertCodes = expectedAlertCodes; }

    public BigDecimal getExpectedRiskScore() { return expectedRiskScore; }
    public void setExpectedRiskScore(BigDecimal expectedRiskScore) { this.expectedRiskScore = expectedRiskScore; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}