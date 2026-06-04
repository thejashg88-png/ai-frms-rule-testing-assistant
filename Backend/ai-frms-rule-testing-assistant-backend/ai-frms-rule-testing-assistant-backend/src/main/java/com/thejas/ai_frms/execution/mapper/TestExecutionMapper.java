package com.thejas.ai_frms.execution.mapper;

import com.thejas.ai_frms.common.util.JsonUtil;
import com.thejas.ai_frms.execution.dto.ComparisonResult;
import com.thejas.ai_frms.execution.dto.ExecuteTestResponse;
import com.thejas.ai_frms.execution.dto.ExecutionResultResponse;
import com.thejas.ai_frms.execution.entity.TestExecutionEntity;
import com.thejas.ai_frms.execution.entity.TestExecutionResultEntity;

import java.util.List;

public final class TestExecutionMapper {

    private TestExecutionMapper() {
    }

    public static ExecuteTestResponse toExecutionResponse(
            TestExecutionEntity execution,
            List<TestExecutionResultEntity> results
    ) {
        ExecuteTestResponse response = new ExecuteTestResponse();

        response.setExecutionId(execution.getExecutionId());
        response.setExecutionType(execution.getExecutionType());
        response.setExecutionStatus(execution.getExecutionStatus());

        if (execution.getScenario() != null) {
            response.setScenarioId(execution.getScenario().getScenarioId());
            response.setScenarioName(execution.getScenario().getScenarioName());
        }

        if (execution.getTestCase() != null) {
            response.setTestCaseId(execution.getTestCase().getTestCaseId());
            response.setTestCaseName(execution.getTestCase().getTestCaseName());
        }

        response.setTotalCount(execution.getTotalCount());
        response.setPassedCount(execution.getPassedCount());
        response.setFailedCount(execution.getFailedCount());
        response.setErrorCount(execution.getErrorCount());
        response.setExecutedBy(execution.getExecutedBy());
        response.setStartedAt(execution.getStartedAt());
        response.setCompletedAt(execution.getCompletedAt());

        response.setResults(
                results.stream()
                        .map(TestExecutionMapper::toResultResponse)
                        .toList()
        );

        return response;
    }

    public static ExecutionResultResponse toResultResponse(TestExecutionResultEntity result) {
        ExecutionResultResponse response = new ExecutionResultResponse();

        response.setResultId(result.getResultId());

        if (result.getExecution() != null) {
            response.setExecutionId(result.getExecution().getExecutionId());
        }

        if (result.getTestCase() != null) {
            response.setTestCaseId(result.getTestCase().getTestCaseId());
            response.setTestCaseName(result.getTestCase().getTestCaseName());
        }

        response.setResultStatus(result.getResultStatus());
        response.setExpectedAction(result.getExpectedAction());
        response.setActualAction(result.getActualAction());
        response.setExpectedEvaluationStatus(result.getExpectedEvaluationStatus());
        response.setActualEvaluationStatus(result.getActualEvaluationStatus());
        response.setMessage(result.getMessage());
        response.setFailureReason(result.getFailureReason());
        response.setExpectedOutcome(result.getExpectedOutcome());

        if (result.getComparisonResultJson() != null && !result.getComparisonResultJson().isBlank()) {
            ComparisonResult cr = JsonUtil.fromJson(result.getComparisonResultJson(), ComparisonResult.class);
            response.setComparisonResult(cr);
            if (cr != null) {
                response.setRuleType(cr.getRuleType());
                response.setInputAmount(cr.getInputAmount());
                // Backfill in case entity columns were not yet persisted (old records)
                if (response.getFailureReason() == null) response.setFailureReason(cr.getFailureReason());
                if (response.getExpectedOutcome() == null) response.setExpectedOutcome(cr.getExpectedOutcome());
                // Map rule explanation (stored in ComparisonResult JSON — no extra DB column needed)
                response.setRuleExplanation(cr.getRuleExplanation());
                // Map execution trace (stored in ComparisonResult JSON — no extra DB column needed)
                response.setExecutionTrace(cr.getExecutionTrace());
            }
        }

        response.setExecutedAt(result.getExecutedAt());

        return response;
    }
}