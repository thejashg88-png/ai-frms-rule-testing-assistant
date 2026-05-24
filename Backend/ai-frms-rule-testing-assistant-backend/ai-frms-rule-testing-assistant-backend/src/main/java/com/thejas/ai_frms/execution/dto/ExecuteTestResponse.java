package com.thejas.ai_frms.execution.dto;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ExecuteTestResponse {

    private Long executionId;
    private String executionType;
    private ExecutionStatus executionStatus;

    private Long scenarioId;
    private String scenarioName;

    private Long testCaseId;
    private String testCaseName;

    private Integer totalCount;
    private Integer passedCount;
    private Integer failedCount;
    private Integer errorCount;

    private String executedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private List<ExecutionResultResponse> results;
}