package com.thejas.ai_frms.execution.repository;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.execution.entity.TestExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TestExecutionRepository extends JpaRepository<TestExecutionEntity, Long>,
        JpaSpecificationExecutor<TestExecutionEntity> {

    List<TestExecutionEntity> findByExecutionStatus(ExecutionStatus executionStatus);

    List<TestExecutionEntity> findByScenarioScenarioId(Long scenarioId);

    List<TestExecutionEntity> findByTestCaseTestCaseId(Long testCaseId);
}