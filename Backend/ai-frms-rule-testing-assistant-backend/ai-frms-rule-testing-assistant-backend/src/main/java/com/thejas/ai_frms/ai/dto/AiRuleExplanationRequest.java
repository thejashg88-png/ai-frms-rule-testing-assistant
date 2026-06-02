package com.thejas.ai_frms.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.common.enums.RuleStatus;

import java.math.BigDecimal;

public class AiRuleExplanationRequest {

    private Long ruleId;
    private String ruleName;
    private String ruleType;
    private RuleAction action;
    private RuleStatus status;

    private String mccCode;
    private Integer txnCount;
    private Integer frequencyHours;
    private BigDecimal txnAmount;
    private BigDecimal maxAmount;
    private BigDecimal percentageThreshold;

    @JsonAlias("description")
    private String ruleDescription;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

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

    public RuleAction getAction() {
        return action;
    }

    public void setAction(RuleAction action) {
        this.action = action;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    public String getMccCode() {
        return mccCode;
    }

    public void setMccCode(String mccCode) {
        this.mccCode = mccCode;
    }

    public Integer getTxnCount() {
        return txnCount;
    }

    public void setTxnCount(Integer txnCount) {
        this.txnCount = txnCount;
    }

    public Integer getFrequencyHours() {
        return frequencyHours;
    }

    public void setFrequencyHours(Integer frequencyHours) {
        this.frequencyHours = frequencyHours;
    }

    public BigDecimal getTxnAmount() {
        return txnAmount;
    }

    public void setTxnAmount(BigDecimal txnAmount) {
        this.txnAmount = txnAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public BigDecimal getPercentageThreshold() {
        return percentageThreshold;
    }

    public void setPercentageThreshold(BigDecimal percentageThreshold) {
        this.percentageThreshold = percentageThreshold;
    }

    public String getRuleDescription() {
        return ruleDescription;
    }

    public void setRuleDescription(String ruleDescription) {
        this.ruleDescription = ruleDescription;
    }
}