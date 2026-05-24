package com.thejas.ai_frms.scenario.dto;

import com.thejas.ai_frms.common.enums.RuleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TestScenarioCreateRequest {

    @NotBlank(message = "Scenario name is required")
    private String scenarioName;

    private String scenarioDescription;

    private RuleStatus status = RuleStatus.ACTIVE;

    @NotNull(message = "Rule id is required")
    private Long ruleId;

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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}