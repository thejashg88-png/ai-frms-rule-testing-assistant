package com.thejas.ai_frms.execution.repository;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.execution.entity.TestExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TestExecutionRepository extends JpaRepository<TestExecutionEntity, Long>,
        JpaSpecificationExecutor<TestExecutionEntity> {

    List<TestExecutionEntity> findByExecutionStatus(ExecutionStatus executionStatus);

    List<TestExecutionEntity> findByScenarioScenarioId(Long scenarioId);

    List<TestExecutionEntity> findByTestCaseTestCaseId(Long testCaseId);

    // Used by dummy cleanup — batch lookup to avoid N+1
    List<TestExecutionEntity> findByTestCaseTestCaseIdIn(List<Long> testCaseIds);
    List<TestExecutionEntity> findByScenarioScenarioIdIn(List<Long> scenarioIds);

    @Query("SELECT e.executionId FROM TestExecutionEntity e WHERE e.scenario.scenarioId = :scenarioId")
    List<Long> findExecutionIdsByScenarioId(@Param("scenarioId") Long scenarioId);

    @Query("SELECT e.executionId FROM TestExecutionEntity e WHERE e.testCase IS NOT NULL AND e.testCase.testCaseId IN :testCaseIds")
    List<Long> findExecutionIdsByTestCaseIdIn(@Param("testCaseIds") Collection<Long> testCaseIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TestExecutionEntity e WHERE e.executionId IN :ids")
    void deleteByExecutionIdIn(@Param("ids") Collection<Long> ids);
}