package com.thejas.ai_frms.execution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecuteScenarioRequest {

    @NotNull(message = "Scenario id is required")
    private Long scenarioId;

    private String executedBy;
}