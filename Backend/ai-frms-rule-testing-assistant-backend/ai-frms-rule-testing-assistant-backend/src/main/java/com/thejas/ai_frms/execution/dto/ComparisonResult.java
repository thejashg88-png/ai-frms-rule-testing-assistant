package com.thejas.ai_frms.execution.dto;

import com.thejas.ai_frms.common.enums.RuleAction;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ComparisonResult {

    private boolean matched;

    private RuleAction expectedAction;
    private RuleAction actualAction;

    private String expectedEvaluationStatus;
    private String actualEvaluationStatus;

    private String expectedRuleType;
    private String actualRuleType;

    private List<String> expectedAlertCodes;
    private List<String> actualAlertCodes;

    private BigDecimal expectedRiskScore;
    private BigDecimal actualRiskScore;

    private String message;

    // Enriched fields for clear response and debugging
    private String expectedOutcome;   // "PASS" or "FAIL" — test designer's intent
    private String failureReason;     // e.g. "Expected action MONITOR but actual action ACCEPT"
    private String ruleType;          // which rule type was evaluated
    private BigDecimal inputAmount;   // the amount that was tested
    private String engineNote;        // additional context from rule engine (e.g., baseline evaluation details)
}