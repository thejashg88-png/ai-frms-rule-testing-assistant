package com.thejas.ai_frms.testcase.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.GeneratedBy;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.enums.TestCaseType;
import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;
import com.thejas.ai_frms.testcase.dto.TestCaseResponse;
import com.thejas.ai_frms.testcase.dto.TestCaseUpdateRequest;
import com.thejas.ai_frms.testcase.service.TestCaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing test cases.
 *
 * Role access:
 *   ADMIN  — full CRUD + status changes
 *   TESTER — create, update, status change (no delete)
 *   VIEWER — read-only
 */
@RestController
@RequestMapping({ApiPathConstants.TEST_CASES, "/api/testcases"})
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping
    public ResponseEntity<ApiResponse<TestCaseResponse>> createTestCase(
            @Valid @RequestBody TestCaseCreateRequest request
    ) {
        TestCaseResponse response = testCaseService.createTestCase(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Test case created successfully", response));
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PutMapping("/{testCaseId}")
    public ResponseEntity<ApiResponse<TestCaseResponse>> updateTestCase(
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseUpdateRequest request
    ) {
        TestCaseResponse response = testCaseService.updateTestCase(testCaseId, request);
        return ResponseEntity.ok(ApiResponse.success("Test case updated successfully", response));
    }

    @GetMapping("/{testCaseId}")
    public ResponseEntity<ApiResponse<TestCaseResponse>> getTestCaseById(
            @PathVariable Long testCaseId
    ) {
        TestCaseResponse response = testCaseService.getTestCaseById(testCaseId);
        return ResponseEntity.ok(ApiResponse.success("Test case fetched successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TestCaseResponse>>> searchTestCases(
            @RequestParam(required = false) Long scenarioId,
            @RequestParam(required = false) String testCaseName,
            @RequestParam(required = false) TestCaseType testCaseType,
            @RequestParam(required = false) RuleStatus status,
            @RequestParam(required = false) GeneratedBy generatedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        PageResponse<TestCaseResponse> response = testCaseService.searchTestCases(
                scenarioId, testCaseName, testCaseType, status, generatedBy, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success("Test cases fetched successfully", response));
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PatchMapping("/{testCaseId}/status")
    public ResponseEntity<ApiResponse<TestCaseResponse>> changeTestCaseStatus(
            @PathVariable Long testCaseId,
            @RequestParam RuleStatus status
    ) {
        TestCaseResponse response = testCaseService.changeTestCaseStatus(testCaseId, status);
        return ResponseEntity.ok(ApiResponse.success("Test case status changed successfully", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{testCaseId}")
    public ResponseEntity<ApiResponse<Void>> deleteTestCase(@PathVariable Long testCaseId) {
        String message = testCaseService.deleteTestCase(testCaseId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}