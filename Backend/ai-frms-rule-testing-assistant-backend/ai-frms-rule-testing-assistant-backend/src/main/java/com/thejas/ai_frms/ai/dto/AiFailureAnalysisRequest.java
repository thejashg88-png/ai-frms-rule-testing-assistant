package com.thejas.ai_frms.ai.dto;

import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.testcase.dto.ExpectedResult;
import com.thejas.ai_frms.testcase.dto.TestInputData;

public class AiFailureAnalysisRequest {

    private Long executionId;
    private Long testCaseId;
    private String testCaseName;

    private String ruleName;
    private String ruleType;

    private TestInputData inputData;
    private ExpectedResult expectedResult;

    private RuleAction actualAction;
    private String actualEvaluationStatus;
    private String actualRuleType;

    private String failureMessage;

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public Long getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Long testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public TestInputData getInputData() {
        return inputData;
    }

    public void setInputData(TestInputData inputData) {
        this.inputData = inputData;
    }

    public ExpectedResult getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(ExpectedResult expectedResult) {
        this.expectedResult = expectedResult;
    }

    public RuleAction getActualAction() {
        return actualAction;
    }

    public void setActualAction(RuleAction actualAction) {
        this.actualAction = actualAction;
    }

    public String getActualEvaluationStatus() {
        return actualEvaluationStatus;
    }

    public void setActualEvaluationStatus(String actualEvaluationStatus) {
        this.actualEvaluationStatus = actualEvaluationStatus;
    }

    public String getActualRuleType() {
        return actualRuleType;
    }

    public void setActualRuleType(String actualRuleType) {
        this.actualRuleType = actualRuleType;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }
}