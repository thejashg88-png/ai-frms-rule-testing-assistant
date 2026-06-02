package com.thejas.ai_frms.testcase.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.GeneratedBy;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.enums.TestCaseType;
import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;
import com.thejas.ai_frms.testcase.dto.TestCaseResponse;
import com.thejas.ai_frms.testcase.dto.TestCaseUpdateRequest;

public interface TestCaseService {

    TestCaseResponse createTestCase(TestCaseCreateRequest request);

    TestCaseResponse updateTestCase(Long testCaseId, TestCaseUpdateRequest request);

    TestCaseResponse getTestCaseById(Long testCaseId);

    PageResponse<TestCaseResponse> searchTestCases(
            Long scenarioId,
            String testCaseName,
            TestCaseType testCaseType,
            RuleStatus status,
            GeneratedBy generatedBy,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    TestCaseResponse changeTestCaseStatus(Long testCaseId, RuleStatus status);

    String deleteTestCase(Long testCaseId);
}