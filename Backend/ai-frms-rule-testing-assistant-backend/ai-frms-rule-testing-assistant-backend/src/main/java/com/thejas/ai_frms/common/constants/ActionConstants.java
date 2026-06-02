package com.thejas.ai_frms.common.constants;

/**
 * String constants for rule action values used in risk evaluation.
 *
 * Severity priority when multiple active rules match the same transaction:
 *   REJECT (3) > MONITOR (2) > ACCEPT (1)
 *
 * The highest-severity action always wins.
 * See TransactionRuleEvaluationService for the priority selection logic.
 */
public final class ActionConstants {

    private ActionConstants() {
    }

    // No action taken — transaction is allowed through without any flag
    public static final String ACCEPT = "ACCEPT";

    // Transaction allowed but flagged for manual review
    public static final String MONITOR = "MONITOR";

    // Transaction blocked — highest severity action
    public static final String REJECT = "REJECT";
}