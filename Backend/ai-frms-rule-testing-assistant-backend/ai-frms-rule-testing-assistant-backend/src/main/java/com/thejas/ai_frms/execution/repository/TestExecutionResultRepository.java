package com.thejas.ai_frms.execution.repository;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.execution.entity.TestExecutionResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestExecutionResultRepository extends JpaRepository<TestExecutionResultEntity, Long> {

    List<TestExecutionResultEntity> findByExecutionExecutionIdOrderByExecutedAtAsc(Long executionId);

    List<TestExecutionResultEntity> findByTestCaseTestCaseId(Long testCaseId);

    long countByTestCaseTestCaseId(Long testCaseId);

    long countByExecutionExecutionIdAndResultStatus(Long executionId, ExecutionStatus resultStatus);

    void deleteByExecutionExecutionId(Long executionId);
}