package com.thejas.ai_frms.common.enums;

/**
 * Represents the lifecycle and outcome status of a test execution.
 *
 * Lifecycle: PENDING → RUNNING → PASSED / FAILED / ERROR
 *
 * At scenario level, the final status is decided by priority:
 *   ERROR (if any test case errored) > FAILED (if any failed) > PASSED (all passed)
 */
public enum ExecutionStatus {
    // Execution created but not yet started
    PENDING,
    // Execution is currently running
    RUNNING,
    // All test cases passed (expectedAction == actualAction for every case)
    PASSED,
    // One or more test cases failed (expectedAction != actualAction)
    FAILED,
    // Execution completed (used as a general terminal state)
    COMPLETED,
    // Execution encountered an unexpected error (e.g. missing inputData, null rule)
    ERROR
}