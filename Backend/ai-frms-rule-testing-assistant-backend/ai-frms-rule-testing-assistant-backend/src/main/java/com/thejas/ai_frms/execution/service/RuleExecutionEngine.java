package com.thejas.ai_frms.execution.service;

import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.execution.dto.ComparisonResult;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.testcase.dto.TestInputData;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
public class RuleExecutionEngine {

    public ComparisonResult execute(TestCaseEntity testCase, TestInputData inputData) {
        if (testCase == null) {
            throw new BadRequestException("Test case cannot be null");
        }

        if (testCase.getScenario() == null || testCase.getScenario().getRule() == null) {
            throw new BadRequestException("No rule linked with this test case scenario");
        }

        RuleEntity rule = testCase.getScenario().getRule();

        RuleAction actualAction = evaluateRule(rule, inputData);

        ComparisonResult result = new ComparisonResult();
        result.setActualAction(actualAction);
        result.setActualEvaluationStatus(actualAction.name());
        result.setActualRuleType(rule.getRuleType());
        result.setActualAlertCodes(buildAlertCodes(rule, actualAction));
        result.setActualRiskScore(buildRiskScore(actualAction));

        return result;
    }

    private RuleAction evaluateRule(RuleEntity rule, TestInputData inputData) {
        if (rule == null || inputData == null || inputData.getAmount() == null) {
            return RuleAction.ACCEPT;
        }

        String ruleType = rule.getRuleType();
        BigDecimal amount = inputData.getAmount();

        if (ruleType == null || ruleType.isBlank()) {
            return RuleAction.ACCEPT;
        }

        return switch (ruleType) {
            case "SINGLE_LARGE_TX", "DAILY_LIMIT", "DAILY_TXN_VALUE", "MONTHLY_VOLUME", "ANNUAL_VOLUME" ->
                    evaluateAmountGreaterThanRule(rule, amount);

            case "STRUCTURING" ->
                    evaluateStructuringRule(rule, amount);

            case "UNUSUAL_AMOUNT", "UNUSUAL_AMT" ->
                    evaluateUnusualAmountRule(rule, amount);

            default ->
                    evaluateDefaultRule(rule, amount);
        };
    }

    private RuleAction evaluateAmountGreaterThanRule(RuleEntity rule, BigDecimal amount) {
        if (rule.getMaxAmount() != null && amount.compareTo(rule.getMaxAmount()) > 0) {
            return rule.getAction();
        }

        return RuleAction.ACCEPT;
    }

    private RuleAction evaluateStructuringRule(RuleEntity rule, BigDecimal amount) {
        if (rule.getTxnAmount() != null && amount.compareTo(rule.getTxnAmount()) < 0) {
            return rule.getAction();
        }

        return RuleAction.ACCEPT;
    }

    private RuleAction evaluateUnusualAmountRule(RuleEntity rule, BigDecimal amount) {
        if (rule.getMaxAmount() != null && amount.compareTo(rule.getMaxAmount()) > 0) {
            return rule.getAction();
        }

        if (rule.getPercentageThreshold() != null
                && rule.getPercentageThreshold().compareTo(BigDecimal.ZERO) > 0) {
            return rule.getAction();
        }

        return RuleAction.ACCEPT;
    }

    private RuleAction evaluateDefaultRule(RuleEntity rule, BigDecimal amount) {
        if (rule.getMaxAmount() != null && amount.compareTo(rule.getMaxAmount()) > 0) {
            return rule.getAction();
        }

        if (rule.getTxnAmount() != null && amount.compareTo(rule.getTxnAmount()) < 0) {
            return rule.getAction();
        }

        return RuleAction.ACCEPT;
    }

    private List<String> buildAlertCodes(RuleEntity rule, RuleAction actualAction) {
        if (actualAction == RuleAction.ACCEPT) {
            return Collections.emptyList();
        }

        return List.of(rule.getRuleType() + "_" + actualAction.name());
    }

    private BigDecimal buildRiskScore(RuleAction actualAction) {
        return switch (actualAction) {
            case ACCEPT -> BigDecimal.ZERO;
            case MONITOR -> BigDecimal.valueOf(60);
            case REJECT -> BigDecimal.valueOf(90);
        };
    }
}