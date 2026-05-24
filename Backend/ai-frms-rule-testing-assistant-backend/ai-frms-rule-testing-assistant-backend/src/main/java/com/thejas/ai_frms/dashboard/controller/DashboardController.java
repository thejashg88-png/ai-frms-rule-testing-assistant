package com.thejas.ai_frms.dashboard.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.dashboard.dto.DashboardSummaryResponse;
import com.thejas.ai_frms.dashboard.dto.ExecutionTrendResponse;
import com.thejas.ai_frms.dashboard.dto.RuleWiseExecutionStats;
import com.thejas.ai_frms.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}