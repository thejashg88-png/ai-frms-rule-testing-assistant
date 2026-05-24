package com.thejas.ai_frms.report.dto;

public class ReportRequest {

    private Long executionId;
    private Long scenarioId;
    private String reportFormat = "PDF";
    private String requestedBy;
    private Integer trendDays = 7;

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Integer getTrendDays() {
        return trendDays;
    }

    public void setTrendDays(Integer trendDays) {
        this.trendDays = trendDays;
    }
}