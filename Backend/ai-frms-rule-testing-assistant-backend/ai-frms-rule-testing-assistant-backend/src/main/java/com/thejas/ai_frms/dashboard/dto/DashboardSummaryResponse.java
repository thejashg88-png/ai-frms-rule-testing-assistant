package com.thejas.ai_frms.dashboard.dto;

import java.util.Map;

public class DashboardSummaryResponse {

    // ── Existing fields (preserved for backwards compatibility) ──────────────
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

    // ── New analytics fields ─────────────────────────────────────────────────
    private long activeRules;
    private double passRate;

    private String mostFailedRuleType;
    private String mostTriggeredRule;

    private Map<String, Long> passFailDistribution;
    private Map<String, Long> executionsByRuleType;
    private Map<String, Long> riskActionDistribution;
    private Map<String, Long> transactionStatusDistribution;

    // ── Existing getters/setters ─────────────────────────────────────────────

    public long getTotalRules() { return totalRules; }
    public void setTotalRules(long totalRules) { this.totalRules = totalRules; }

    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }

    public long getTotalScenarios() { return totalScenarios; }
    public void setTotalScenarios(long totalScenarios) { this.totalScenarios = totalScenarios; }

    public long getTotalTestCases() { return totalTestCases; }
    public void setTotalTestCases(long totalTestCases) { this.totalTestCases = totalTestCases; }

    public long getTotalExecutions() { return totalExecutions; }
    public void setTotalExecutions(long totalExecutions) { this.totalExecutions = totalExecutions; }

    public long getPassedExecutions() { return passedExecutions; }
    public void setPassedExecutions(long passedExecutions) { this.passedExecutions = passedExecutions; }

    public long getFailedExecutions() { return failedExecutions; }
    public void setFailedExecutions(long failedExecutions) { this.failedExecutions = failedExecutions; }

    public long getErrorExecutions() { return errorExecutions; }
    public void setErrorExecutions(long errorExecutions) { this.errorExecutions = errorExecutions; }

    public long getRunningExecutions() { return runningExecutions; }
    public void setRunningExecutions(long runningExecutions) { this.runningExecutions = runningExecutions; }

    public long getPendingExecutions() { return pendingExecutions; }
    public void setPendingExecutions(long pendingExecutions) { this.pendingExecutions = pendingExecutions; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    // ── New getters/setters ──────────────────────────────────────────────────

    public long getActiveRules() { return activeRules; }
    public void setActiveRules(long activeRules) { this.activeRules = activeRules; }

    public double getPassRate() { return passRate; }
    public void setPassRate(double passRate) { this.passRate = passRate; }

    public String getMostFailedRuleType() { return mostFailedRuleType; }
    public void setMostFailedRuleType(String mostFailedRuleType) { this.mostFailedRuleType = mostFailedRuleType; }

    public String getMostTriggeredRule() { return mostTriggeredRule; }
    public void setMostTriggeredRule(String mostTriggeredRule) { this.mostTriggeredRule = mostTriggeredRule; }

    public Map<String, Long> getPassFailDistribution() { return passFailDistribution; }
    public void setPassFailDistribution(Map<String, Long> passFailDistribution) { this.passFailDistribution = passFailDistribution; }

    public Map<String, Long> getExecutionsByRuleType() { return executionsByRuleType; }
    public void setExecutionsByRuleType(Map<String, Long> executionsByRuleType) { this.executionsByRuleType = executionsByRuleType; }

    public Map<String, Long> getRiskActionDistribution() { return riskActionDistribution; }
    public void setRiskActionDistribution(Map<String, Long> riskActionDistribution) { this.riskActionDistribution = riskActionDistribution; }

    public Map<String, Long> getTransactionStatusDistribution() { return transactionStatusDistribution; }
    public void setTransactionStatusDistribution(Map<String, Long> transactionStatusDistribution) { this.transactionStatusDistribution = transactionStatusDistribution; }
}