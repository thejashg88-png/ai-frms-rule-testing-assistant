package com.thejas.ai_frms.scenario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.thejas.ai_frms.common.enums.RuleStatus;
import jakarta.validation.constraints.NotBlank;

public class TestScenarioCreateRequest {

    @NotBlank(message = "Scenario name is required")
    private String scenarioName;

    @JsonAlias("description")
    private String scenarioDescription;

    private RuleStatus status = RuleStatus.ACTIVE;

    // optional when ruleType is provided
    private Long ruleId;

    // used to look up the rule when ruleId is not provided
    private String ruleType;

    // accepted from frontend but not persisted — no DB column for this
    private String expectedResult;

    private String createdBy;

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getScenarioDescription() {
        return scenarioDescription;
    }

    public void setScenarioDescription(String scenarioDescription) {
        this.scenarioDescription = scenarioDescription;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}