package com.thejas.ai_frms.execution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class




ExecuteTestCaseRequest {

    @NotNull(message = "Test case id is required")
    private Long testCaseId;

    private String executedBy;
}