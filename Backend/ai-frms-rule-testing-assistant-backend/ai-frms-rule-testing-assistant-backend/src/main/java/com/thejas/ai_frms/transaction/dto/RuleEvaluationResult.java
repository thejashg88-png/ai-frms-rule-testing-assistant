package com.thejas.ai_frms.transaction.dto;

import com.thejas.ai_frms.execution.dto.RuleEvaluationExplanationResponse;

public class RuleEvaluationResult {

    private final String riskEvaluationStatus;
    private final String triggeredRuleName;
    private final String triggeredRuleType;
    private final String triggeredAction;
    private final String riskReason;

    public RuleEvaluationResult(
            String riskEvaluationStatus,
            String triggeredRuleName,
            String triggeredRuleType,
            String triggeredAction,
            String riskReason
    ) {
        this.riskEvaluationStatus = riskEvaluationStatus;
        this.triggeredRuleName = triggeredRuleName;
        this.triggeredRuleType = triggeredRuleType;
        this.triggeredAction = triggeredAction;
        this.riskReason = riskReason;
    }

    public String getRiskEvaluationStatus() { return riskEvaluationStatus; }
    public String getTriggeredRuleName() { return triggeredRuleName; }
    public String getTriggeredRuleType() { return triggeredRuleType; }
    public String getTriggeredAction() { return triggeredAction; }
    public String getRiskReason() { return riskReason; }

    // Non-final — set after construction via setRuleExplanation()
    private RuleEvaluationExplanationResponse ruleExplanation;
    public RuleEvaluationExplanationResponse getRuleExplanation() { return ruleExplanation; }
    public void setRuleExplanation(RuleEvaluationExplanationResponse ruleExplanation) { this.ruleExplanation = ruleExplanation; }
}