package com.thejas.ai_frms.scheduler.dto;

import com.thejas.ai_frms.common.enums.RuleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ScheduledScenarioCreateRequest {

    @NotBlank(message = "Schedule name is required")
    private String scheduleName;

    private String description;

    @NotNull(message = "Scenario id is required")
    private Long scenarioId;

    @NotBlank(message = "Cron expression is required")
    private String cronExpression;

    private RuleStatus status = RuleStatus.ACTIVE;

    private String createdBy;

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}