package com.thejas.ai_frms.execution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for executing a full test scenario.
 *
 * Scenario execution runs all ACTIVE test cases linked to the scenario in sequence.
 * INACTIVE test cases are skipped — they are excluded from execution even if they exist.
 * The final executionStatus is determined by the worst result across all test cases:
 *   ERROR > FAILED > PASSED
 */
@Getter
@Setter
public class ExecuteScenarioRequest {

    @NotNull(message = "Scenario id is required")
    private Long scenarioId;

    // Optional — identifies who triggered the execution (stored in the execution record)
    private String executedBy;
}