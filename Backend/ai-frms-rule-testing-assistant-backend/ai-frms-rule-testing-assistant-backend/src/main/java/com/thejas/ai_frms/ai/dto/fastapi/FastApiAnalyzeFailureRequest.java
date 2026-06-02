package com.thejas.ai_frms.ai.dto.fastapi;

/**
 * Request body sent to FastAPI POST /api/ai/analyze-failure.
 * FastAPI expects camelCase field names — no @JsonProperty annotations.
 * inputData is Object so it serializes as {} not "{}".
 */
public class FastApiAnalyzeFailureRequest {

    private String testCaseName;
    private String ruleType;
    private String expectedResult;
    private String actualResult;
    private Object inputData;
    private String executionLogs;

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public Object getInputData() {
        return inputData;
    }

    public void setInputData(Object inputData) {
        this.inputData = inputData;
    }

    public String getExecutionLogs() {
        return executionLogs;
    }

    public void setExecutionLogs(String executionLogs) {
        this.executionLogs = executionLogs;
    }
}