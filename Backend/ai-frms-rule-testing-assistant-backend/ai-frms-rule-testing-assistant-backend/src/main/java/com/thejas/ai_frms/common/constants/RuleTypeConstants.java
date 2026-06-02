package com.thejas.ai_frms.common.constants;

/**
 * String constants for the rule_type column in frms_rules table.
 * Matched case-insensitively in RuleExecutionEngine and TransactionRuleEvaluationService.
 *
 * Required configuration fields per type:
 *   SINGLE_LARGE_TX  — maxAmount       (triggers when txn amount > maxAmount)
 *   STRUCTURING      — txnAmount       (triggers when txn amount < txnAmount)
 *   HIGH_FREQUENCY   — txnCount, frequencyHours
 *   UNUSUAL_AMOUNT   — percentageThreshold
 *   SEQUENTIAL_TXN   — txnCount, frequencyHours
 *   INCONSISTENT_MCC — mccCode
 *   DAILY_LIMIT      — maxAmount
 *   MONTHLY_VOLUME   — maxAmount, frequencyHours=720  (720h = 30 days)
 *   ANNUAL_VOLUME    — maxAmount, frequencyHours=8760 (8760h = 365 days)
 */
public final class RuleTypeConstants {

    private RuleTypeConstants() {
    }

    // Triggers when a single transaction amount exceeds maxAmount
    public static final String SINGLE_LARGE_TX = "SINGLE_LARGE_TX";

    // Triggers when a transaction amount is BELOW txnAmount (breaking large amounts into smaller ones)
    public static final String STRUCTURING = "STRUCTURING";

    // Triggers when transaction count within frequencyHours exceeds txnCount
    public static final String HIGH_FREQUENCY = "HIGH_FREQUENCY";

    // Triggers when the current amount deviates from historical average by more than percentageThreshold%
    public static final String UNUSUAL_AMOUNT = "UNUSUAL_AMOUNT";

    // Triggers when the same card/device makes repeated transactions within a time window
    public static final String SEQUENTIAL_TXN = "SEQUENTIAL_TXN";

    // Triggers when the transaction MCC code differs from the configured expected mccCode
    public static final String INCONSISTENT_MCC = "INCONSISTENT_MCC";

    // Triggers when cumulative daily transaction value exceeds maxAmount
    public static final String DAILY_LIMIT = "DAILY_LIMIT";

    // Triggers when monthly (720h) cumulative transaction value exceeds maxAmount
    public static final String MONTHLY_VOLUME = "MONTHLY_VOLUME";

    // Triggers when annual (8760h) cumulative transaction value exceeds maxAmount
    public static final String ANNUAL_VOLUME = "ANNUAL_VOLUME";
}