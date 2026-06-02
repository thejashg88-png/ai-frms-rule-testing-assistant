package com.thejas.ai_frms.common.enums;

/**
 * Enum representing the action a fraud rule takes when it matches a transaction.
 *
 * Severity order used when multiple rules match the same transaction:
 *   REJECT (highest) > MONITOR > ACCEPT (lowest)
 *
 * The highest-severity action among all matching rules wins.
 * See TransactionRuleEvaluationService and RuleExecutionEngine.
 */
public enum RuleAction {
    // Transaction is allowed through with no flag
    ACCEPT,
    // Transaction is allowed but flagged for manual review
    MONITOR,
    // Transaction is blocked — no further processing
    REJECT
}