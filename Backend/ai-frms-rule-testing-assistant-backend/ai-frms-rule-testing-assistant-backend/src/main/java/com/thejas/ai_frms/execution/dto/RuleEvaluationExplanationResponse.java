package com.thejas.ai_frms.execution.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detailed explanation of why a fraud rule triggered or did not trigger.
 *
 * Populated by RuleExecutionEngine (test case / scenario execution)
 * and by TransactionRuleEvaluationService (transaction risk evaluation).
 *
 * Only the fields relevant to the specific ruleType are populated;
 * all others are null. The frontend should render based on triggered=true/false
 * and ruleReason / resultExplanation for a human-readable summary.
 */
public class RuleEvaluationExplanationResponse {

    // ── Rule identity ────────────────────────────────────────────────────────
    private String ruleType;
    private String ruleName;

    // ── Actions ──────────────────────────────────────────────────────────────
    private String expectedAction;
    private String actualAction;

    // ── Count-based fields ────────────────────────────────────────────────────
    // Used by: HIGH_FREQ_TXN, SEQUENTIAL_TXN, MONTHLY_TXN_VOLUME, ANNUAL_TXN_VOLUME
    private Integer matchedCount;       // historicalCount + currentCount
    private Integer historicalCount;    // transactions found in history/window
    private Integer currentCount;       // always 1 (the current test transaction)
    private Integer requiredCount;      // txnCount threshold from rule configuration

    // ── Time window fields ────────────────────────────────────────────────────
    // Stored as ISO-8601 strings (e.g. "2026-06-01T10:00:00") to avoid Jackson JSR310 serialization issues
    private String frequencyWindow;     // e.g. "last 1 hour", "last 720 hours / monthly window"
    private String windowStart;         // ISO-8601 string, not LocalDateTime
    private String windowEnd;           // ISO-8601 string, not LocalDateTime

    // ── Safe matched transaction identifiers (no PAN, no full card number) ────
    // Format: "txnId=12, rrn=HF101, stan=220101, last4=3455, time=2026-06-01T10:20:00"
    private List<String> matchedTransactions;

    // ── Amount-based fields ───────────────────────────────────────────────────
    // Used by: SINGLE_LARGE_TX, UNUSUAL_AMT, STRUCTURING, DAILY_LIMIT
    private BigDecimal thresholdAmount;     // trigger threshold (maxAmount or avg+pct threshold)
    private BigDecimal actualAmount;        // the transaction amount being evaluated
    private BigDecimal averageAmount;       // baseline average (UNUSUAL_AMT only)
    private BigDecimal percentageThreshold; // deviation % (UNUSUAL_AMT only)
    private BigDecimal maxAmount;           // raw maxAmount from rule config

    // ── Conclusion fields ─────────────────────────────────────────────────────
    private String ruleReason;          // concise machine-style reason (why triggered or not)
    private String resultExplanation;   // human-readable explanation of the outcome
    private Boolean triggered;          // true if rule fired, false if it did not

    // ── Getters and Setters ───────────────────────────────────────────────────

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getExpectedAction() { return expectedAction; }
    public void setExpectedAction(String expectedAction) { this.expectedAction = expectedAction; }

    public String getActualAction() { return actualAction; }
    public void setActualAction(String actualAction) { this.actualAction = actualAction; }

    public Integer getMatchedCount() { return matchedCount; }
    public void setMatchedCount(Integer matchedCount) { this.matchedCount = matchedCount; }

    public Integer getHistoricalCount() { return historicalCount; }
    public void setHistoricalCount(Integer historicalCount) { this.historicalCount = historicalCount; }

    public Integer getCurrentCount() { return currentCount; }
    public void setCurrentCount(Integer currentCount) { this.currentCount = currentCount; }

    public Integer getRequiredCount() { return requiredCount; }
    public void setRequiredCount(Integer requiredCount) { this.requiredCount = requiredCount; }

    public String getFrequencyWindow() { return frequencyWindow; }
    public void setFrequencyWindow(String frequencyWindow) { this.frequencyWindow = frequencyWindow; }

    public String getWindowStart() { return windowStart; }
    public void setWindowStart(String windowStart) { this.windowStart = windowStart; }

    public String getWindowEnd() { return windowEnd; }
    public void setWindowEnd(String windowEnd) { this.windowEnd = windowEnd; }

    public List<String> getMatchedTransactions() { return matchedTransactions; }
    public void setMatchedTransactions(List<String> matchedTransactions) { this.matchedTransactions = matchedTransactions; }

    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }

    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }

    public BigDecimal getAverageAmount() { return averageAmount; }
    public void setAverageAmount(BigDecimal averageAmount) { this.averageAmount = averageAmount; }

    public BigDecimal getPercentageThreshold() { return percentageThreshold; }
    public void setPercentageThreshold(BigDecimal percentageThreshold) { this.percentageThreshold = percentageThreshold; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public String getRuleReason() { return ruleReason; }
    public void setRuleReason(String ruleReason) { this.ruleReason = ruleReason; }

    public String getResultExplanation() { return resultExplanation; }
    public void setResultExplanation(String resultExplanation) { this.resultExplanation = resultExplanation; }

    public Boolean getTriggered() { return triggered; }
    public void setTriggered(Boolean triggered) { this.triggered = triggered; }
}