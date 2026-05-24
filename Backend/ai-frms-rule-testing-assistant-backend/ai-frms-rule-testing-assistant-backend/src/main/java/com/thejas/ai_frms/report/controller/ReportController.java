package com.thejas.ai_frms.report.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.report.dto.ReportRequest;
import com.thejas.ai_frms.report.dto.ReportResponse;
import com.thejas.ai_frms.report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPathConstants.REPORTS)
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
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
}