package com.thejas.ai_frms.common.constants;

/**
 * String constants for status values used across the application.
 *
 * Rule/Scenario/TestCase status:
 *   ACTIVE   — included in risk evaluation and scenario execution
 *   INACTIVE — temporarily disabled; still visible in filtered list views
 *   DELETED  — soft-deleted; permanently hidden from all default list views (rules only)
 *
 * Execution status:
 *   PENDING   — not yet started
 *   RUNNING   — currently executing
 *   PASSED    — all test cases passed
 *   FAILED    — one or more test cases failed
 *   COMPLETED — finished (used at scenario level)
 */
public final class StatusConstants {

    private StatusConstants() {
    }

    // Rule, scenario, test case is active and will be evaluated
    public static final String ACTIVE = "ACTIVE";

    // Rule, scenario, or test case is disabled; still appears when filtering by INACTIVE
    public static final String INACTIVE = "INACTIVE";

    // Rule is soft-deleted (referenced so cannot hard-delete); hidden from all default list views
    public static final String DELETED = "DELETED";

    // Test execution result: expected action matched actual action
    public static final String PASSED = "PASSED";

    // Test execution result: expected action did NOT match actual action
    public static final String FAILED = "FAILED";

    // Execution has been created but not yet started
    public static final String PENDING = "PENDING";

    // Execution is currently in progress
    public static final String RUNNING = "RUNNING";

    // Execution finished (used at scenario level to indicate all test cases ran)
    public static final String COMPLETED = "COMPLETED";
}