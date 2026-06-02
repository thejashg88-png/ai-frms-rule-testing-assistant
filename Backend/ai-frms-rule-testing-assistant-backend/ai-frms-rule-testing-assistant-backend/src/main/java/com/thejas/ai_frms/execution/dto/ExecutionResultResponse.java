package com.thejas.ai_frms.execution.dto;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.common.enums.RuleAction;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
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

    private String expectedOutcome;    // "PASS" / "FAIL" — test designer's intent
    private String failureReason;      // e.g. "Expected action MONITOR but actual action ACCEPT"
    private String ruleType;           // which rule type was evaluated
    private BigDecimal inputAmount;    // the amount that was tested

    private String expectedEvaluationStatus;
    private String actualEvaluationStatus;

    private String message;

    private ComparisonResult comparisonResult;

    private LocalDateTime executedAt;
}