package com.thejas.ai_frms.ai.dto.fastapi;

/**
 * Request body sent to FastAPI POST /api/ai/explain-rule.
 * FastAPI expects camelCase field names — no @JsonProperty annotations.
 */
public class FastApiExplainRuleRequest {

    private String ruleName;
    private String ruleType;
    private String action;
    private Double maxAmount;
    private String description;

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(Double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}