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
}