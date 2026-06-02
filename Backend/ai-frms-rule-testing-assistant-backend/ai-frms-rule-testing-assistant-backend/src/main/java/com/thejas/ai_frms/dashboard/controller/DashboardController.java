package com.thejas.ai_frms.dashboard.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.dashboard.dto.DashboardSummaryResponse;
import com.thejas.ai_frms.dashboard.dto.ExecutionTrendResponse;
import com.thejas.ai_frms.dashboard.dto.RuleWiseExecutionStats;
import com.thejas.ai_frms.dashboard.service.DashboardService;
import com.thejas.ai_frms.execution.dto.ExecuteTestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for dashboard summary data.
 *
 * Provides aggregated statistics for the frontend dashboard:
 *   summary          — total rules, test cases, scenarios, executions and pass rate
 *   rule-wise-stats  — breakdown of pass/fail counts per rule
 *   execution-trend  — daily execution counts over the last N days (default 7)
 *   recent-executions — latest N execution records (default 10)
 *
 * All endpoints are read-only and do not modify any data.
 */
@RestController
@RequestMapping(ApiPathConstants.DASHBOARD)
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary() {
        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        return ResponseEntity.ok(ApiResponse.success("Dashboard summary fetched successfully", response));
    }

    @GetMapping("/rule-wise-stats")
    public ResponseEntity<ApiResponse<List<RuleWiseExecutionStats>>> getRuleWiseExecutionStats() {
        List<RuleWiseExecutionStats> response = dashboardService.getRuleWiseExecutionStats();

        return ResponseEntity.ok(ApiResponse.success("Rule-wise execution stats fetched successfully", response));
    }

    @GetMapping("/execution-trend")
    public ResponseEntity<ApiResponse<List<ExecutionTrendResponse>>> getExecutionTrend(
            @RequestParam(defaultValue = "7") int days
    ) {
        List<ExecutionTrendResponse> response = dashboardService.getExecutionTrend(days);

        return ResponseEntity.ok(ApiResponse.success("Execution trend fetched successfully", response));
    }

    @GetMapping("/recent-executions")
    public ResponseEntity<ApiResponse<List<ExecuteTestResponse>>> getRecentExecutions(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ExecuteTestResponse> response = dashboardService.getRecentExecutions(limit);

        return ResponseEntity.ok(ApiResponse.success("Recent executions fetched successfully", response));
    }
}