package com.thejas.ai_frms.testcase.dto;

import com.thejas.ai_frms.common.enums.GeneratedBy;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.enums.TestCaseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TestCaseCreateRequest {

    @NotBlank(message = "Test case name is required")
    private String testCaseName;

    private String testCaseDescription;

    private TestCaseType testCaseType = TestCaseType.POSITIVE;

    private RuleStatus status = RuleStatus.ACTIVE;

    private GeneratedBy generatedBy = GeneratedBy.MANUAL;

    @NotNull(message = "Scenario id is required")
    private Long scenarioId;

    private Long transactionId;

    @NotNull(message = "Input data is required")
    @Valid
    private TestInputData inputData;

    @NotNull(message = "Expected result is required")
    @Valid
    private ExpectedResult expectedResult;

    private String createdBy;

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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}