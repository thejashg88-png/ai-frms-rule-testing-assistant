package com.thejas.ai_frms.dashboard.dto;

public class DashboardSummaryResponse {

    private long totalRules;
    private long totalTransactions;
    private long totalScenarios;
    private long totalTestCases;
    private long totalExecutions;

    private long passedExecutions;
    private long failedExecutions;
    private long errorExecutions;
    private long runningExecutions;
    private long pendingExecutions;

    private double successRate;

    public long getTotalRules() {
        return totalRules;
    }

    public void setTotalRules(long totalRules) {
        this.totalRules = totalRules;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
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

    public long getRunningExecutions() {
        return runningExecutions;
    }

    public void setRunningExecutions(long runningExecutions) {
        this.runningExecutions = runningExecutions;
    }

    public long getPendingExecutions() {
        return pendingExecutions;
    }

    public void setPendingExecutions(long pendingExecutions) {
        this.pendingExecutions = pendingExecutions;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
}