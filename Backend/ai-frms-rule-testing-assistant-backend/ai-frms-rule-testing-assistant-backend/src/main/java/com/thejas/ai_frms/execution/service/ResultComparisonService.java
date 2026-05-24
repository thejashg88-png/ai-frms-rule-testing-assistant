package com.thejas.ai_frms.execution.service;

import com.thejas.ai_frms.execution.dto.ComparisonResult;
import com.thejas.ai_frms.testcase.dto.ExpectedResult;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ResultComparisonService {

    public ComparisonResult compare(ExpectedResult expectedResult, ComparisonResult actualResult) {
        ComparisonResult comparisonResult = new ComparisonResult();

        comparisonResult.setExpectedAction(expectedResult.getExpectedAction());
        comparisonResult.setActualAction(actualResult.getActualAction());

        comparisonResult.setExpectedEvaluationStatus(expectedResult.getExpectedEvaluationStatus());
        comparisonResult.setActualEvaluationStatus(actualResult.getActualEvaluationStatus());

        comparisonResult.setExpectedRuleType(expectedResult.getExpectedRuleType());
        comparisonResult.setActualRuleType(actualResult.getActualRuleType());

        comparisonResult.setExpectedAlertCodes(expectedResult.getExpectedAlertCodes());
        comparisonResult.setActualAlertCodes(actualResult.getActualAlertCodes());

        comparisonResult.setExpectedRiskScore(expectedResult.getExpectedRiskScore());
        comparisonResult.setActualRiskScore(actualResult.getActualRiskScore());

        boolean actionMatched = Objects.equals(
                expectedResult.getExpectedAction(),
                actualResult.getActualAction()
        );

        boolean evaluationStatusMatched = isBlank(expectedResult.getExpectedEvaluationStatus())
                || Objects.equals(
                expectedResult.getExpectedEvaluationStatus(),
                actualResult.getActualEvaluationStatus()
        );

        boolean ruleTypeMatched = isBlank(expectedResult.getExpectedRuleType())
                || Objects.equals(
                expectedResult.getExpectedRuleType(),
                actualResult.getActualRuleType()
        );

        boolean matched = actionMatched && evaluationStatusMatched && ruleTypeMatched;

        comparisonResult.setMatched(matched);

        if (matched) {
            comparisonResult.setMessage("Expected result matched with actual result");
        } else {
            comparisonResult.setMessage("Expected result did not match with actual result");
        }

        return comparisonResult;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}