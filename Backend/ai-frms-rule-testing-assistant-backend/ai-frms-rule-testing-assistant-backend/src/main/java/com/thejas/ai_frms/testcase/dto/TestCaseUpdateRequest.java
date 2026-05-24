package com.thejas.ai_frms.testcase.dto;

import com.thejas.ai_frms.common.enums.GeneratedBy;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.enums.TestCaseType;
import jakarta.validation.Valid;

public class TestCaseUpdateRequest {

    private String testCaseName;

    private String testCaseDescription;

    private TestCaseType testCaseType;

    private RuleStatus status;

    private GeneratedBy generatedBy;

    private Long scenarioId;

    private Long transactionId;

    @Valid
    private TestInputData inputData;

    @Valid
    private ExpectedResult expectedResult;

    private String modifiedBy;

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getTestCaseDescription() {
        return testCaseDescription;
    }

    public void setTestCaseDescription(String testCaseDescription) {
        this.testCaseDescription = testCaseDescription;
    }

    public TestCaseType getTestCaseType() {
        return testCaseType;
    }

    public void setTestCaseType(TestCaseType testCaseType) {
        this.testCaseType = testCaseType;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    public GeneratedBy getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(GeneratedBy generatedBy) {
        this.generatedBy = generatedBy;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
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

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}