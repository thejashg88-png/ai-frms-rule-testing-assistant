package com.thejas.ai_frms.execution.dto;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.common.enums.RuleAction;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ExecutionResultResponse {

    private Long resultId;
    private Long executionId;

    private Long testCaseId;
    private String testCaseName;

    private ExecutionStatus resultStatus;

    private RuleAction expectedAction;
    private RuleAction actualAction;

    private String expectedEvaluationStatus;
    private String actualEvaluationStatus;

    private String message;

    private ComparisonResult comparisonResult;

    private LocalDateTime executedAt;
}