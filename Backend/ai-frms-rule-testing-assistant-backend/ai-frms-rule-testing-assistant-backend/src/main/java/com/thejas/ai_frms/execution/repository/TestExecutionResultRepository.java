package com.thejas.ai_frms.execution.repository;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.execution.entity.TestExecutionResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TestExecutionResultRepository extends JpaRepository<TestExecutionResultEntity, Long> {

    List<TestExecutionResultEntity> findByExecutionExecutionIdOrderByExecutedAtAsc(Long executionId);

    List<TestExecutionResultEntity> findByTestCaseTestCaseId(Long testCaseId);

    long countByTestCaseTestCaseId(Long testCaseId);

    long countByExecutionExecutionIdAndResultStatus(Long executionId, ExecutionStatus resultStatus);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TestExecutionResultEntity r WHERE r.execution.executionId = :id")
    void deleteByExecutionExecutionId(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TestExecutionResultEntity r WHERE r.execution.executionId IN :ids")
    void deleteByExecutionExecutionIdIn(@Param("ids") Collection<Long> ids);

    // Dashboard: count results grouped by rule type (joins testCase → scenario → rule)
    @Query("SELECT r.testCase.scenario.rule.ruleType, COUNT(r) " +
           "FROM TestExecutionResultEntity r " +
           "WHERE r.testCase.scenario.rule IS NOT NULL " +
           "GROUP BY r.testCase.scenario.rule.ruleType")
    List<Object[]> countResultsByRuleType();

    // Dashboard: count results grouped by rule type filtered by resultStatus (for mostFailedRuleType)
    @Query("SELECT r.testCase.scenario.rule.ruleType, COUNT(r) " +
           "FROM TestExecutionResultEntity r " +
           "WHERE r.testCase.scenario.rule IS NOT NULL " +
           "AND r.resultStatus = :status " +
           "GROUP BY r.testCase.scenario.rule.ruleType " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> countResultsByRuleTypeAndStatus(@Param("status") ExecutionStatus status);

    // Dashboard: rule name with most triggered results (actualAction != ACCEPT) for mostTriggeredRule
    @Query("SELECT r.testCase.scenario.rule.ruleName, COUNT(r) " +
           "FROM TestExecutionResultEntity r " +
           "WHERE r.testCase.scenario.rule IS NOT NULL " +
           "AND r.actualAction <> :acceptAction " +
           "GROUP BY r.testCase.scenario.rule.ruleName " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> findMostTriggeredRuleName(@Param("acceptAction") RuleAction acceptAction);

    // Dashboard: count results grouped by actualAction for risk action distribution
    @Query("SELECT r.actualAction, COUNT(r) FROM TestExecutionResultEntity r " +
           "WHERE r.actualAction IS NOT NULL GROUP BY r.actualAction")
    List<Object[]> countByActualAction();
}