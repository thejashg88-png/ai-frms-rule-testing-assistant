package com.thejas.ai_frms.execution.dto;

/**
 * One step in the rule execution trace.
 *
 * status values:
 *   SUCCESS  — step completed and indicates a passing condition
 *   INFO     — informational step with no pass/fail judgement
 *   FAILED   — step indicates a failure or mismatch
 *   WARNING  — unexpected situation that did not stop execution
 */
public class ExecutionTraceStepResponse {

    private Integer stepNumber;
    private String title;
    private String detail;
    private String status;
    private String ruleType;
    private String timestamp;

    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}