package com.thejas.ai_frms.ai.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FastApiGenerateTransactionRequest {

    @JsonProperty("rule_type")
    private String ruleType;

    @JsonProperty("expected_result")
    private String expectedResult;

    private Double amount;

    private String currency;

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}