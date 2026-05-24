package com.thejas.ai_frms.testcase.mapper;

import com.thejas.ai_frms.common.util.JsonUtil;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.testcase.dto.ExpectedResult;
import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;
import com.thejas.ai_frms.testcase.dto.TestCaseResponse;
import com.thejas.ai_frms.testcase.dto.TestCaseUpdateRequest;
import com.thejas.ai_frms.testcase.dto.TestInputData;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import com.thejas.ai_frms.transaction.entity.TransactionEntity;

public final class TestCaseMapper {

    private TestCaseMapper() {
    }

    public static TestCaseEntity toEntity(
            TestCaseCreateRequest request,
            TestScenarioEntity scenario,
            TransactionEntity transaction
    ) {
        TestCaseEntity entity = new TestCaseEntity();

        entity.setTestCaseName(request.getTestCaseName());
        entity.setTestCaseDescription(request.getTestCaseDescription());
        entity.setTestCaseType(request.getTestCaseType());
        entity.setStatus(request.getStatus());
        entity.setGeneratedBy(request.getGeneratedBy());
        entity.setScenario(scenario);
        entity.setTransaction(transaction);
        entity.setInputDataJson(JsonUtil.toJson(request.getInputData()));
        entity.setExpectedResultJson(JsonUtil.toJson(request.getExpectedResult()));
        entity.setCreatedBy(request.getCreatedBy());

        return entity;
    }

    public static void updateEntity(
            TestCaseEntity entity,
            TestCaseUpdateRequest request,
            TestScenarioEntity scenario,
            TransactionEntity transaction
    ) {
        if (request.getTestCaseName() != null) {
            entity.setTestCaseName(request.getTestCaseName());
        }

        if (request.getTestCaseDescription() != null) {
            entity.setTestCaseDescription(request.getTestCaseDescription());
        }

        if (request.getTestCaseType() != null) {
            entity.setTestCaseType(request.getTestCaseType());
        }

        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        if (request.getGeneratedBy() != null) {
            entity.setGeneratedBy(request.getGeneratedBy());
        }

        if (scenario != null) {
            entity.setScenario(scenario);
        }

        if (request.getTransactionId() != null) {
            entity.setTransaction(transaction);
        }

        if (request.getInputData() != null) {
            entity.setInputDataJson(JsonUtil.toJson(request.getInputData()));
        }

        if (request.getExpectedResult() != null) {
            entity.setExpectedResultJson(JsonUtil.toJson(request.getExpectedResult()));
        }

        if (request.getModifiedBy() != null) {
            entity.setModifiedBy(request.getModifiedBy());
        }
    }

    public static TestCaseResponse toResponse(TestCaseEntity entity) {
        TestCaseResponse response = new TestCaseResponse();

        response.setTestCaseId(entity.getTestCaseId());
        response.setTestCaseName(entity.getTestCaseName());
        response.setTestCaseDescription(entity.getTestCaseDescription());
        response.setTestCaseType(entity.getTestCaseType());
        response.setStatus(entity.getStatus());
        response.setGeneratedBy(entity.getGeneratedBy());

        if (entity.getScenario() != null) {
            response.setScenarioId(entity.getScenario().getScenarioId());
            response.setScenarioName(entity.getScenario().getScenarioName());

            if (entity.getScenario().getRule() != null) {
                response.setRuleId(entity.getScenario().getRule().getRuleId());
                response.setRuleName(entity.getScenario().getRule().getRuleName());
                response.setRuleType(entity.getScenario().getRule().getRuleType());
            }
        }

        if (entity.getTransaction() != null) {
            response.setTransactionId(entity.getTransaction().getTransactionId());
        }

        response.setInputData(JsonUtil.fromJson(entity.getInputDataJson(), TestInputData.class));
        response.setExpectedResult(JsonUtil.fromJson(entity.getExpectedResultJson(), ExpectedResult.class));

        response.setCreatedBy(entity.getCreatedBy());
        response.setModifiedBy(entity.getModifiedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}