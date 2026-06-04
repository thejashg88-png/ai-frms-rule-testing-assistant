package com.thejas.ai_frms.execution.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.execution.dto.ExecuteScenarioRequest;
import com.thejas.ai_frms.execution.dto.ExecuteTestCaseRequest;
import com.thejas.ai_frms.execution.dto.ExecuteTestResponse;
import com.thejas.ai_frms.execution.dto.ExecutionResultResponse;
import com.thejas.ai_frms.execution.service.TestExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for test and scenario execution.
 *
 * Role access:
 *   ADMIN  — run + delete executions, view all
 *   TESTER — run executions, view all
 *   VIEWER — view executions only
 */
@RestController
@RequestMapping(ApiPathConstants.EXECUTIONS)
public class TestExecutionController {

    private final TestExecutionService testExecutionService;

    public TestExecutionController(TestExecutionService testExecutionService) {
        this.testExecutionService = testExecutionService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/test-case")
    public ResponseEntity<ApiResponse<ExecuteTestResponse>> executeTestCase(
            @Valid @RequestBody ExecuteTestCaseRequest request
    ) {
        ExecuteTestResponse response = testExecutionService.executeTestCase(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Test case executed successfully", response));
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/test-case/{testCaseId}")
    public ResponseEntity<ApiResponse<ExecuteTestResponse>> executeTestCaseById(
            @PathVariable Long testCaseId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        ExecuteTestCaseRequest request = new ExecuteTestCaseRequest();
        request.setTestCaseId(testCaseId);
        request.setExecutedBy(body != null ? body.get("executedBy") : null);
        ExecuteTestResponse response = testExecutionService.executeTestCase(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Test case executed successfully", response));
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/run-testcase/{testCaseId}")
    public ResponseEntity<ApiResponse<ExecuteTestResponse>> runTestCaseById(
            @PathVariable Long testCaseId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        return executeTestCaseById(testCaseId, body);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/scenario")
    public ResponseEntity<ApiResponse<ExecuteTestResponse>> executeScenario(
            @Valid @RequestBody ExecuteScenarioRequest request
    ) {
        ExecuteTestResponse response = testExecutionService.executeScenario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scenario executed successfully", response));
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/scenario/{scenarioId}")
    public ResponseEntity<ApiResponse<ExecuteTestResponse>> executeScenarioById(
            @PathVariable Long scenarioId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        ExecuteScenarioRequest request = new ExecuteScenarioRequest();
        request.setScenarioId(scenarioId);
        request.setExecutedBy(body != null ? body.get("executedBy") : null);
        ExecuteTestResponse response = testExecutionService.executeScenario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scenario executed successfully", response));
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/run-scenario/{scenarioId}")
    public ResponseEntity<ApiResponse<ExecuteTestResponse>> runScenarioById(
            @PathVariable Long scenarioId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        return executeScenarioById(scenarioId, body);
    }

    @GetMapping("/{executionId}")
    public ResponseEntity<ApiResponse<ExecuteTestResponse>> getExecutionById(
            @PathVariable Long executionId
    ) {
        ExecuteTestResponse response = testExecutionService.getExecutionById(executionId);
        return ResponseEntity.ok(ApiResponse.success("Execution fetched successfully", response));
    }

    @GetMapping("/{executionId}/results")
    public ResponseEntity<ApiResponse<List<ExecutionResultResponse>>> getResultsByExecutionId(
            @PathVariable Long executionId
    ) {
        List<ExecutionResultResponse> response = testExecutionService.getResultsByExecutionId(executionId);
        return ResponseEntity.ok(ApiResponse.success("Execution results fetched successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExecuteTestResponse>>> searchExecutions(
            @RequestParam(required = false) Long scenarioId,
            @RequestParam(required = false) Long testCaseId,
            @RequestParam(required = false) ExecutionStatus executionStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        PageResponse<ExecuteTestResponse> response = testExecutionService.searchExecutions(
                scenarioId, testCaseId, executionStatus, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success("Executions fetched successfully", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{executionId}")
    public ResponseEntity<ApiResponse<Void>> deleteExecution(@PathVariable Long executionId) {
        testExecutionService.deleteExecution(executionId);
        return ResponseEntity.ok(ApiResponse.success("Execution deleted successfully"));
    }
}