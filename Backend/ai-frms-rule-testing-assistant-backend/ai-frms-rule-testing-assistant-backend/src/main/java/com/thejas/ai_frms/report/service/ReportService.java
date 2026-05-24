package com.thejas.ai_frms.report.service;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.dashboard.dto.DashboardSummaryResponse;
import com.thejas.ai_frms.dashboard.dto.ExecutionTrendResponse;
import com.thejas.ai_frms.dashboard.dto.RuleWiseExecutionStats;
import com.thejas.ai_frms.dashboard.service.DashboardService;
import com.thejas.ai_frms.execution.entity.TestExecutionEntity;
import com.thejas.ai_frms.execution.entity.TestExecutionResultEntity;
import com.thejas.ai_frms.execution.repository.TestExecutionRepository;
import com.thejas.ai_frms.execution.repository.TestExecutionResultRepository;
import com.thejas.ai_frms.report.dto.ReportRequest;
import com.thejas.ai_frms.report.dto.ReportResponse;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final TestExecutionRepository testExecutionRepository;
    private final TestExecutionResultRepository testExecutionResultRepository;
    private final TestScenarioRepository testScenarioRepository;
    private final DashboardService dashboardService;
    private final PdfReportService pdfReportService;
    private final ExcelReportService excelReportService;

    public ReportService(
            TestExecutionRepository testExecutionRepository,
            TestExecutionResultRepository testExecutionResultRepository,
            TestScenarioRepository testScenarioRepository,
            DashboardService dashboardService,
            PdfReportService pdfReportService,
            ExcelReportService excelReportService
    ) {
        this.testExecutionRepository = testExecutionRepository;
        this.testExecutionResultRepository = testExecutionResultRepository;
        this.testScenarioRepository = testScenarioRepository;
        this.dashboardService = dashboardService;
        this.pdfReportService = pdfReportService;
        this.excelReportService = excelReportService;
    }

    @Transactional(readOnly = true)
    public ReportResponse generateExecutionReport(ReportRequest request) {
        if (request.getExecutionId() == null) {
            throw new BadRequestException("Execution id is required");
        }

        TestExecutionEntity execution = testExecutionRepository.findById(request.getExecutionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Execution not found with id: " + request.getExecutionId()
                ));

        List<TestExecutionResultEntity> results =
                testExecutionResultRepository.findByExecutionExecutionIdOrderByExecutedAtAsc(
                        request.getExecutionId()
                );

        List<String> lines = buildExecutionReportLines(execution, results);

        return generateReport(
                "Execution Report " + request.getExecutionId(),
                lines,
                request
        );
    }

    @Transactional(readOnly = true)
    public ReportResponse generateScenarioReport(ReportRequest request) {
        if (request.getScenarioId() == null) {
            throw new BadRequestException("Scenario id is required");
        }

        TestScenarioEntity scenario = testScenarioRepository.findById(request.getScenarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Scenario not found with id: " + request.getScenarioId()
                ));

        List<TestExecutionEntity> executions =
                testExecutionRepository.findByScenarioScenarioId(request.getScenarioId());

        List<String> lines = buildScenarioReportLines(scenario, executions);

        return generateReport(
                "Scenario Report " + request.getScenarioId(),
                lines,
                request
        );
    }

    @Transactional(readOnly = true)
    public ReportResponse generateDashboardReport(ReportRequest request) {
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
        List<RuleWiseExecutionStats> ruleStats = dashboardService.getRuleWiseExecutionStats();

        int days = request.getTrendDays() == null ? 7 : request.getTrendDays();
        List<ExecutionTrendResponse> trends = dashboardService.getExecutionTrend(days);

        List<String> lines = buildDashboardReportLines(summary, ruleStats, trends);

        return generateReport(
                "Dashboard Report",
                lines,
                request
        );
    }

    private ReportResponse generateReport(String reportName, List<String> lines, ReportRequest request) {
        String format = request.getReportFormat();

        if (format == null || format.isBlank()) {
            format = "PDF";
        }

        if ("EXCEL".equalsIgnoreCase(format) || "CSV".equalsIgnoreCase(format)) {
            return excelReportService.generateExcelReport(reportName, lines, request.getRequestedBy());
        }

        return pdfReportService.generatePdfReport(reportName, lines, request.getRequestedBy());
    }

    private List<String> buildExecutionReportLines(
            TestExecutionEntity execution,
            List<TestExecutionResultEntity> results
    ) {
        List<String> lines = new ArrayList<>();

        lines.add("Execution ID : " + execution.getExecutionId());
        lines.add("Execution Type : " + execution.getExecutionType());
        lines.add("Execution Status : " + execution.getExecutionStatus());
        lines.add("Total Count : " + execution.getTotalCount());
        lines.add("Passed Count : " + execution.getPassedCount());
        lines.add("Failed Count : " + execution.getFailedCount());
        lines.add("Error Count : " + execution.getErrorCount());
        lines.add("Executed By : " + execution.getExecutedBy());
        lines.add("Started At : " + execution.getStartedAt());
        lines.add("Completed At : " + execution.getCompletedAt());
        lines.add("");

        if (execution.getScenario() != null) {
            lines.add("Scenario ID : " + execution.getScenario().getScenarioId());
            lines.add("Scenario Name : " + execution.getScenario().getScenarioName());
        }

        if (execution.getTestCase() != null) {
            lines.add("Test Case ID : " + execution.getTestCase().getTestCaseId());
            lines.add("Test Case Name : " + execution.getTestCase().getTestCaseName());
        }

        lines.add("");
        lines.add("Execution Results:");

        for (TestExecutionResultEntity result : results) {
            lines.add(
                    "Result ID=" + result.getResultId()
                            + ", TestCase=" + safeTestCaseName(result)
                            + ", Status=" + result.getResultStatus()
                            + ", Expected=" + result.getExpectedAction()
                            + ", Actual=" + result.getActualAction()
                            + ", Message=" + result.getMessage()
            );
        }

        return lines;
    }

    private List<String> buildScenarioReportLines(
            TestScenarioEntity scenario,
            List<TestExecutionEntity> executions
    ) {
        List<String> lines = new ArrayList<>();

        lines.add("Scenario ID : " + scenario.getScenarioId());
        lines.add("Scenario Name : " + scenario.getScenarioName());
        lines.add("Scenario Status : " + scenario.getStatus());
        lines.add("Description : " + scenario.getScenarioDescription());

        if (scenario.getRule() != null) {
            lines.add("Rule ID : " + scenario.getRule().getRuleId());
            lines.add("Rule Name : " + scenario.getRule().getRuleName());
            lines.add("Rule Type : " + scenario.getRule().getRuleType());
            lines.add("Rule Action : " + scenario.getRule().getAction());
        }

        lines.add("");
        lines.add("Executions Summary:");

        long passed = countExecutions(executions, ExecutionStatus.PASSED);
        long failed = countExecutions(executions, ExecutionStatus.FAILED);
        long error = countExecutions(executions, ExecutionStatus.ERROR);

        lines.add("Total Executions : " + executions.size());
        lines.add("Passed Executions : " + passed);
        lines.add("Failed Executions : " + failed);
        lines.add("Error Executions : " + error);

        lines.add("");
        lines.add("Execution Details:");

        for (TestExecutionEntity execution : executions) {
            lines.add(
                    "Execution ID=" + execution.getExecutionId()
                            + ", Type=" + execution.getExecutionType()
                            + ", Status=" + execution.getExecutionStatus()
                            + ", Total=" + execution.getTotalCount()
                            + ", Passed=" + execution.getPassedCount()
                            + ", Failed=" + execution.getFailedCount()
                            + ", Error=" + execution.getErrorCount()
            );
        }

        return lines;
    }

    private List<String> buildDashboardReportLines(
            DashboardSummaryResponse summary,
            List<RuleWiseExecutionStats> ruleStats,
            List<ExecutionTrendResponse> trends
    ) {
        List<String> lines = new ArrayList<>();

        lines.add("Dashboard Summary:");
        lines.add("Total Rules : " + summary.getTotalRules());
        lines.add("Total Transactions : " + summary.getTotalTransactions());
        lines.add("Total Scenarios : " + summary.getTotalScenarios());
        lines.add("Total Test Cases : " + summary.getTotalTestCases());
        lines.add("Total Executions : " + summary.getTotalExecutions());
        lines.add("Passed Executions : " + summary.getPassedExecutions());
        lines.add("Failed Executions : " + summary.getFailedExecutions());
        lines.add("Error Executions : " + summary.getErrorExecutions());
        lines.add("Success Rate : " + summary.getSuccessRate() + "%");
        lines.add("");

        lines.add("Rule Wise Stats:");

        for (RuleWiseExecutionStats stats : ruleStats) {
            lines.add(
                    "Rule=" + stats.getRuleName()
                            + ", Type=" + stats.getRuleType()
                            + ", Scenarios=" + stats.getTotalScenarios()
                            + ", TestCases=" + stats.getTotalTestCases()
                            + ", Executions=" + stats.getTotalExecutions()
                            + ", SuccessRate=" + stats.getSuccessRate() + "%"
            );
        }

        lines.add("");
        lines.add("Execution Trend:");

        for (ExecutionTrendResponse trend : trends) {
            lines.add(
                    "Date=" + trend.getDate()
                            + ", Total=" + trend.getTotalExecutions()
                            + ", Passed=" + trend.getPassedCount()
                            + ", Failed=" + trend.getFailedCount()
                            + ", Error=" + trend.getErrorCount()
                            + ", SuccessRate=" + trend.getSuccessRate() + "%"
            );
        }

        return lines;
    }

    private String safeTestCaseName(TestExecutionResultEntity result) {
        if (result.getTestCase() == null) {
            return null;
        }

        return result.getTestCase().getTestCaseName();
    }

    private long countExecutions(List<TestExecutionEntity> executions, ExecutionStatus status) {
        return executions.stream()
                .filter(execution -> execution.getExecutionStatus() == status)
                .count();
    }
}