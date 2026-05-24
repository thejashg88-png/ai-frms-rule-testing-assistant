package com.thejas.ai_frms.dashboard.dto;

public class RuleWiseExecutionStats {

    private Long ruleId;
    private String ruleName;
    private String ruleType;

    private long totalScenarios;
    private long totalTestCases;
    private long totalExecutions;

    private long passedExecutions;
    private long failedExecutions;
    private long errorExecutions;

    private double successRate;

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

    public long getTotalScenarios() {
        return totalScenarios;
    }

    public void setTotalScenarios(long totalScenarios) {
        this.totalScenarios = totalScenarios;
    }

    public long getTotalTestCases() {
        return totalTestCases;
    }

    public void setTotalTestCases(long totalTestCases) {
        this.totalTestCases = totalTestCases;
    }

    public long getTotalExecutions() {
        return totalExecutions;
    }

    public void setTotalExecutions(long totalExecutions) {
        this.totalExecutions = totalExecutions;
    }

    public long getPassedExecutions() {
        return passedExecutions;
    }

    public void setPassedExecutions(long passedExecutions) {
        this.passedExecutions = passedExecutions;
    }

    public long getFailedExecutions() {
        return failedExecutions;
    }

    public void setFailedExecutions(long failedExecutions) {
        this.failedExecutions = failedExecutions;
    }

    public long getErrorExecutions() {
        return errorExecutions;
    }

    public void setErrorExecutions(long errorExecutions) {
        this.errorExecutions = errorExecutions;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
}