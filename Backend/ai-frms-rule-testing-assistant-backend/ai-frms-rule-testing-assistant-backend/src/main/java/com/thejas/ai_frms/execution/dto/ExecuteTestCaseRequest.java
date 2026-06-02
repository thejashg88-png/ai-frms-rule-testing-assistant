package com.thejas.ai_frms.execution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for executing a single test case.
 *
 * Runs the rule linked to the test case's scenario against the test case's inputData,
 * then compares the actual rule engine output to the expectedResult.
 * Result is stored in TestExecutionResultEntity with PASSED / FAILED / ERROR status.
 */
@Getter
@Setter
public class




ExecuteTestCaseRequest {

    @NotNull(message = "Test case id is required")
    private Long testCaseId;

    // Optional — identifies who triggered the execution (stored in the execution record)
    private String executedBy;
}