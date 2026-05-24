package com.thejas.ai_frms.frmsintegration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thejas.ai_frms.common.enums.RuleAction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FrmsRiskEvaluationResponse {

    private RuleAction action;
    private String evaluationStatus;
    private String ruleType;
    private String ruleName;
    private Long ruleId;
    private String mccCode;
    private String mccCategory;
    private BigDecimal riskScore;
    private List<String> alertCodes;
    private String message;

    /*
     * This field is useful when actual FRMS returns extra data
     * which we have not mapped yet.
     */
    private Map<String, Object> additionalData;

    public RuleAction getAction() {
        return action;
    }

    public void setAction(RuleAction action) {
        this.action = action;
    }

    public String getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(String evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getMccCode() {
        return mccCode;
    }

    public void setMccCode(String mccCode) {
        this.mccCode = mccCode;
    }

    public String getMccCategory() {
        return mccCategory;
    }

    public void setMccCategory(String mccCategory) {
        this.mccCategory = mccCategory;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public List<String> getAlertCodes() {
        return alertCodes;
    }

    public void setAlertCodes(List<String> alertCodes) {
        this.alertCodes = alertCodes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }
}