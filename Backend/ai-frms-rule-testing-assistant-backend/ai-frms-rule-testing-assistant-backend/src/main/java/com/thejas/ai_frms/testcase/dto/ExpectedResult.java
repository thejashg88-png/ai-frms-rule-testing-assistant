package com.thejas.ai_frms.testcase.dto;

import com.thejas.ai_frms.common.enums.RuleAction;

import java.math.BigDecimal;
import java.util.List;

public class ExpectedResult {

    private RuleAction expectedAction;
    private String expectedEvaluationStatus;
    private String expectedRuleType;
    private List<String> expectedAlertCodes;
    private BigDecimal expectedRiskScore;
    private String remarks;

    public RuleAction getExpectedAction() {
        return expectedAction;
    }

    public void setExpectedAction(RuleAction expectedAction) {
        this.expectedAction = expectedAction;
    }

    public String getExpectedEvaluationStatus() {
        return expectedEvaluationStatus;
    }

    public void setExpectedEvaluationStatus(String expectedEvaluationStatus) {
        this.expectedEvaluationStatus = expectedEvaluationStatus;
    }

    public String getExpectedRuleType() {
        return expectedRuleType;
    }

    public void setExpectedRuleType(String expectedRuleType) {
        this.expectedRuleType = expectedRuleType;
    }

    public List<String> getExpectedAlertCodes() {
        return expectedAlertCodes;
    }

    public void setExpectedAlertCodes(List<String> expectedAlertCodes) {
        this.expectedAlertCodes = expectedAlertCodes;
    }

    public BigDecimal getExpectedRiskScore() {
        return expectedRiskScore;
    }

    public void setExpectedRiskScore(BigDecimal expectedRiskScore) {
        this.expectedRiskScore = expectedRiskScore;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}