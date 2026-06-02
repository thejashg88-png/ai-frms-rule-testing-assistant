package com.thejas.ai_frms.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.thejas.ai_frms.common.enums.RuleAction;

import java.math.BigDecimal;

public class AiTestCaseGenerationRequest {

    private Long ruleId;
    private Long scenarioId;

    private String ruleName;
    private String ruleType;
    private RuleAction action;
    private String status;
    private String mccCode;

    private Integer txnCount;
    @JsonAlias("frequency")
    private Integer frequencyHours;
    private BigDecimal txnAmount;
    private BigDecimal maxAmount;
    private BigDecimal percentageThreshold;

    @JsonAlias("description")
    private String ruleDescription;

    private Integer numberOfTestCases = 5;
    private Boolean includePositiveCases = true;
    private Boolean includeNegativeCases = true;
    private Boolean includeBoundaryCases = true;
    private Boolean includeEdgeCases = true;

    private String createdBy;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public Integer getNumberOfTestCases() {
        return numberOfTestCases;
    }

    public void setNumberOfTestCases(Integer numberOfTestCases) {
        this.numberOfTestCases = numberOfTestCases;
    }

    public Boolean getIncludePositiveCases() {
        return includePositiveCases;
    }

    public void setIncludePositiveCases(Boolean includePositiveCases) {
        this.includePositiveCases = includePositiveCases;
    }

    public Boolean getIncludeNegativeCases() {
        return includeNegativeCases;
    }

    public void setIncludeNegativeCases(Boolean includeNegativeCases) {
        this.includeNegativeCases = includeNegativeCases;
    }

    public Boolean getIncludeBoundaryCases() {
        return includeBoundaryCases;
    }

    public void setIncludeBoundaryCases(Boolean includeBoundaryCases) {
        this.includeBoundaryCases = includeBoundaryCases;
    }

    public Boolean getIncludeEdgeCases() {
        return includeEdgeCases;
    }

    public void setIncludeEdgeCases(Boolean includeEdgeCases) {
        this.includeEdgeCases = includeEdgeCases;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}