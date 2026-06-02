package com.thejas.ai_frms.report.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.dashboard.dto.DashboardSummaryResponse;
import com.thejas.ai_frms.dashboard.dto.ExecutionTrendResponse;
import com.thejas.ai_frms.dashboard.service.DashboardService;
import com.thejas.ai_frms.execution.entity.TestExecutionEntity;
import com.thejas.ai_frms.execution.repository.TestExecutionRepository;
import com.thejas.ai_frms.report.dto.ReportRequest;
import com.thejas.ai_frms.report.dto.ReportResponse;
import com.thejas.ai_frms.report.service.ReportService;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.rule.repository.RuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for report generation and download.
 *
 * Provides two types of endpoints:
 *   1. JSON summary endpoints (/summary, /executions, /rules) — returns structured data
 *      for the frontend reports page; no file is generated.
 *   2. CSV download endpoints (/download/rules, /download/executions) — streams a CSV file
 *      as a binary download; uses Content-Disposition: attachment.
 *
 * POST endpoints (/execution, /scenario, /dashboard) generate PDF/Excel reports via ReportService.
 * These are currently separate from the GET summary endpoints above.
 */
@RestController
@RequestMapping(ApiPathConstants.REPORTS)
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;
    private final DashboardService dashboardService;
    private final RuleRepository ruleRepository;
    private final TestExecutionRepository testExecutionRepository;

    public ReportController(
            ReportService reportService,
            DashboardService dashboardService,
            RuleRepository ruleRepository,
            TestExecutionRepository testExecutionRepository
    ) {
        this.reportService = reportService;
        this.dashboardService = dashboardService;
        this.ruleRepository = ruleRepository;
        this.testExecutionRepository = testExecutionRepository;
    }

    @PostMapping("/execution")
    public ResponseEntity<ApiResponse<ReportResponse>> generateExecutionReport(
            @RequestBody ReportRequest request
    ) {
        ReportResponse response = reportService.generateExecutionReport(request);
        return ResponseEntity.ok(ApiResponse.success("Execution report generated successfully", response));
    }

    @PostMapping("/scenario")
    public ResponseEntity<ApiResponse<ReportResponse>> generateScenarioReport(
            @RequestBody ReportRequest request
    ) {
        ReportResponse response = reportService.generateScenarioReport(request);
        return ResponseEntity.ok(ApiResponse.success("Scenario report generated successfully", response));
    }

    @PostMapping("/dashboard")
    public ResponseEntity<ApiResponse<ReportResponse>> generateDashboardReport(
            @RequestBody ReportRequest request
    ) {
        ReportResponse response = reportService.generateDashboardReport(request);
        return ResponseEntity.ok(ApiResponse.success("Dashboard report generated successfully", response));
    }

    /** GET /api/reports/summary — combined summary for frontend reports page */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportSummary() {
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalExecutions", summary.getTotalExecutions());
        report.put("passed", summary.getPassedExecutions());
        report.put("failed", summary.getFailedExecutions());
        report.put("error", summary.getErrorExecutions());
        report.put("passRate", summary.getSuccessRate());
        report.put("totalRules", summary.getTotalRules());
        report.put("totalTestCases", summary.getTotalTestCases());
        report.put("totalScenarios", summary.getTotalScenarios());

        return ResponseEntity.ok(ApiResponse.success("Report summary fetched successfully", report));
    }

    @GetMapping("/executions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutionReport() {
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
        List<ExecutionTrendResponse> trend = dashboardService.getExecutionTrend(7);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalExecutions", summary.getTotalExecutions());
        report.put("passed", summary.getPassedExecutions());
        report.put("failed", summary.getFailedExecutions());
        report.put("passRate", summary.getSuccessRate());

        List<Map<String, Object>> byStatus = new ArrayList<>();
        byStatus.add(Map.of("status", "PASSED", "count", summary.getPassedExecutions()));
        byStatus.add(Map.of("status", "FAILED", "count", summary.getFailedExecutions()));
        byStatus.add(Map.of("status", "ERROR", "count", summary.getErrorExecutions()));
        report.put("byStatus", byStatus);

        List<Map<String, Object>> trendData = trend.stream()
                .map(t -> {
                    Map<String, Object> day = new LinkedHashMap<>();
                    day.put("date", t.getDate().toString());
                    day.put("passed", t.getPassedCount());
                    day.put("failed", t.getFailedCount());
                    return day;
                })
                .collect(java.util.stream.Collectors.toList());
        report.put("trend", trendData);

        return ResponseEntity.ok(ApiResponse.success("Execution report fetched successfully", report));
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRuleReport() {
        List<RuleEntity> rules = ruleRepository.findAll();

        long totalRules = rules.size();
        long activeRules = rules.stream()
                .filter(r -> r.getStatus() != null && "ACTIVE".equals(r.getStatus().name()))
                .count();
        long inactiveRules = totalRules - activeRules;

        Map<String, long[]> byTypeMap = new LinkedHashMap<>();
        Map<String, Long> byActionMap = new LinkedHashMap<>();

        for (RuleEntity rule : rules) {
            String type = rule.getRuleType() != null ? rule.getRuleType() : "UNKNOWN";
            byTypeMap.computeIfAbsent(type, k -> new long[]{0, 0});
            byTypeMap.get(type)[0]++;
            if (rule.getStatus() != null && "ACTIVE".equals(rule.getStatus().name())) {
                byTypeMap.get(type)[1]++;
            }
            String action = rule.getAction() != null ? rule.getAction().name() : "UNKNOWN";
            byActionMap.merge(action, 1L, Long::sum);
        }

        List<Map<String, Object>> byType = byTypeMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("type", e.getKey());
                    entry.put("count", e.getValue()[0]);
                    entry.put("activeCount", e.getValue()[1]);
                    return entry;
                })
                .collect(java.util.stream.Collectors.toList());

        List<Map<String, Object>> byAction = byActionMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("action", e.getKey());
                    entry.put("count", e.getValue());
                    return entry;
                })
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalRules", totalRules);
        report.put("activeRules", activeRules);
        report.put("inactiveRules", inactiveRules);
        report.put("byType", byType);
        report.put("byAction", byAction);

        return ResponseEntity.ok(ApiResponse.success("Rule report fetched successfully", report));
    }

    @GetMapping("/download/rules")
    public ResponseEntity<byte[]> downloadRulesReport() {
        log.info("[REPORT DOWNLOAD] Rules CSV download requested");
        try {
            List<RuleEntity> rules = ruleRepository.findAll();
            log.info("[REPORT DOWNLOAD] Rules count: {}", rules.size());

            StringBuilder csv = new StringBuilder();
            csv.append("ID,Rule Name,Rule Description,Rule Type,Action,Status,")
               .append("Txn Count,Txn Amount,Max Amount,Frequency Hours,Percentage Threshold,")
               .append("Created At,Updated At\n");

            for (RuleEntity rule : rules) {
                csv.append(rule.getRuleId()).append(",");
                csv.append(escapeCsv(rule.getRuleName())).append(",");
                csv.append(escapeCsv(rule.getRuleDescription())).append(",");
                csv.append(escapeCsv(rule.getRuleType())).append(",");
                csv.append(rule.getAction() != null ? rule.getAction().name() : "").append(",");
                csv.append(rule.getStatus() != null ? rule.getStatus().name() : "").append(",");
                csv.append(rule.getTxnCount() != null ? rule.getTxnCount() : "").append(",");
                csv.append(rule.getTxnAmount() != null ? rule.getTxnAmount() : "").append(",");
                csv.append(rule.getMaxAmount() != null ? rule.getMaxAmount() : "").append(",");
                csv.append(rule.getFrequencyHours() != null ? rule.getFrequencyHours() : "").append(",");
                csv.append(rule.getPercentageThreshold() != null ? rule.getPercentageThreshold() : "").append(",");
                csv.append(rule.getCreatedAt() != null ? rule.getCreatedAt() : "").append(",");
                csv.append(rule.getUpdatedAt() != null ? rule.getUpdatedAt() : "").append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            log.info("[REPORT DOWNLOAD] Rules CSV size: {} bytes", bytes.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rules-report.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(bytes);
        } catch (Exception e) {
            log.error("[REPORT DOWNLOAD] Failed to generate rules CSV: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(("Failed to generate rules report: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/download/executions")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadExecutionsReport() {
        log.info("[REPORT DOWNLOAD] Executions CSV download requested");
        try {
            List<TestExecutionEntity> executions = testExecutionRepository.findAll();
            log.info("[REPORT DOWNLOAD] Executions count: {}", executions.size());

            StringBuilder csv = new StringBuilder();
            csv.append("ID,Execution Type,Status,Scenario Name,Test Case Name,")
               .append("Total,Passed,Failed,Error,Executed By,Started At,Completed At\n");

            for (TestExecutionEntity exec : executions) {
                String scenarioName = exec.getScenario() != null ? exec.getScenario().getScenarioName() : "";
                String testCaseName = exec.getTestCase() != null ? exec.getTestCase().getTestCaseName() : "";

                csv.append(exec.getExecutionId()).append(",");
                csv.append(escapeCsv(exec.getExecutionType())).append(",");
                csv.append(exec.getExecutionStatus() != null ? exec.getExecutionStatus().name() : "").append(",");
                csv.append(escapeCsv(scenarioName)).append(",");
                csv.append(escapeCsv(testCaseName)).append(",");
                csv.append(exec.getTotalCount() != null ? exec.getTotalCount() : 0).append(",");
                csv.append(exec.getPassedCount() != null ? exec.getPassedCount() : 0).append(",");
                csv.append(exec.getFailedCount() != null ? exec.getFailedCount() : 0).append(",");
                csv.append(exec.getErrorCount() != null ? exec.getErrorCount() : 0).append(",");
                csv.append(escapeCsv(exec.getExecutedBy())).append(",");
                csv.append(exec.getStartedAt() != null ? exec.getStartedAt() : "").append(",");
                csv.append(exec.getCompletedAt() != null ? exec.getCompletedAt() : "").append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            log.info("[REPORT DOWNLOAD] Executions CSV size: {} bytes", bytes.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"executions-report.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(bytes);
        } catch (Exception e) {
            log.error("[REPORT DOWNLOAD] Failed to generate executions CSV: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(("Failed to generate executions report: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}