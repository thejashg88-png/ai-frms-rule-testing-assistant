package com.thejas.ai_frms.ai.dto;

import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.testcase.dto.ExpectedResult;
import com.thejas.ai_frms.testcase.dto.TestInputData;

/**
 * Request body for the AI failure analysis endpoint.
 *
 * Two usage patterns:
 *   1. Frontend direct call — sends testCaseName, ruleType, expectedResult,
 *      actualResult (e.g. "FAILED"), and executionLogs as a plain string.
 *   2. analyzeFailureById() — builds this from a stored TestExecutionEntity;
 *      uses legacy fields (actualAction, failureMessage) as fallbacks.
 *
 * ruleType must come from the actual execution result, not defaulted to SINGLE_LARGE_TX.
 */
public class AiFailureAnalysisRequest {

    private Long executionId;
    private Long testCaseId;
    private String testCaseName;

    private String ruleName;
    // Must reflect the actual ruleType from the execution — never default to SINGLE_LARGE_TX
    private String ruleType;

    private TestInputData inputData;
    private ExpectedResult expectedResult;

    // Frontend sends "actualResult": "FAILED" directly
    private String actualResult;
    // Frontend sends "executionLogs": "..." directly (plain string describing what went wrong)
    private String executionLogs;

    // Legacy fields — kept for analyzeFailureById() compatibility; not sent by frontend
    private RuleAction actualAction;
    private String actualEvaluationStatus;
    private String actualRuleType;
    private String failureMessage;

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public Long getTestCaseId() { return testCaseId; }
    public void setTestCaseId(Long testCaseId) { this.testCaseId = testCaseId; }

    public String getTestCaseName() { return testCaseName; }
    public void setTestCaseName(String testCaseName) { this.testCaseName = testCaseName; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public TestInputData getInputData() { return inputData; }
    public void setInputData(TestInputData inputData) { this.inputData = inputData; }

    public ExpectedResult getExpectedResult() { return expectedResult; }
    public void setExpectedResult(ExpectedResult expectedResult) { this.expectedResult = expectedResult; }

    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }

    public String getExecutionLogs() { return executionLogs; }
    public void setExecutionLogs(String executionLogs) { this.executionLogs = executionLogs; }

    public RuleAction getActualAction() { return actualAction; }
    public void setActualAction(RuleAction actualAction) { this.actualAction = actualAction; }

    public String getActualEvaluationStatus() { return actualEvaluationStatus; }
    public void setActualEvaluationStatus(String actualEvaluationStatus) { this.actualEvaluationStatus = actualEvaluationStatus; }

    public String getActualRuleType() { return actualRuleType; }
    public void setActualRuleType(String actualRuleType) { this.actualRuleType = actualRuleType; }

    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
}