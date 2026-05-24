package com.thejas.ai_frms.rule.dto;

import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.common.enums.RuleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public class RuleUpdateRequest {

    private String ruleName;

    private String ruleType;

    private RuleAction action;

    private RuleStatus status;

    private String mccCode;

    @Min(value = 1, message = "Transaction count must be greater than 0")
    private Integer txnCount;

    @Min(value = 1, message = "Frequency hours must be greater than 0")
    private Integer frequencyHours;

    @DecimalMin(value = "0.0", inclusive = false, message = "Transaction amount must be greater than 0")
    private BigDecimal txnAmount;

    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum amount must be greater than 0")
    private BigDecimal maxAmount;

    @DecimalMin(value = "0.0", inclusive = false, message = "Percentage threshold must be greater than 0")
    private BigDecimal percentageThreshold;

    private String ruleDescription;

    private String modifiedBy;

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

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}