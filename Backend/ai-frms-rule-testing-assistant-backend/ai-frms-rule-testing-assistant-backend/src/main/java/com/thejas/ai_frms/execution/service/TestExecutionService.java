package com.thejas.ai_frms.execution.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.execution.dto.ExecuteScenarioRequest;
import com.thejas.ai_frms.execution.dto.ExecuteTestCaseRequest;
import com.thejas.ai_frms.execution.dto.ExecuteTestResponse;
import com.thejas.ai_frms.execution.dto.ExecutionResultResponse;

import java.util.List;

public interface TestExecutionService {

    ExecuteTestResponse executeTestCase(ExecuteTestCaseRequest request);

    ExecuteTestResponse executeScenario(ExecuteScenarioRequest request);

    ExecuteTestResponse getExecutionById(Long executionId);

    List<ExecutionResultResponse> getResultsByExecutionId(Long executionId);

    PageResponse<ExecuteTestResponse> searchExecutions(
            Long scenarioId,
            Long testCaseId,
            ExecutionStatus executionStatus,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    void deleteExecution(Long executionId);
}