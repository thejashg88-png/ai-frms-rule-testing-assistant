package com.thejas.ai_frms.testcase.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.thejas.ai_frms.common.enums.RuleAction;

import java.math.BigDecimal;
import java.util.List;

@JsonDeserialize(using = ExpectedResultDeserializer.class)
public class ExpectedResult {

    private String expectedOutcome;       // "PASS", "FAIL"
    private RuleAction expectedAction;    // MONITOR, ACCEPT, REJECT
    private String expectedRiskLevel;     // "HIGH", "MEDIUM", "LOW"
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