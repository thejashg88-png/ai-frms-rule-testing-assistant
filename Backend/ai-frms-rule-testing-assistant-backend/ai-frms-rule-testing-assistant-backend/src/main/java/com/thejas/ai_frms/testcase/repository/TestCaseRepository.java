package com.thejas.ai_frms.testcase.repository;

import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCaseEntity, Long>,
        JpaSpecificationExecutor<TestCaseEntity> {

    boolean existsByTestCaseNameIgnoreCase(String testCaseName);

    boolean existsByTestCaseNameIgnoreCaseAndTestCaseIdNot(String testCaseName, Long testCaseId);

    List<TestCaseEntity> findByScenarioScenarioId(Long scenarioId);

    List<TestCaseEntity> findByScenarioScenarioIdAndStatus(Long scenarioId, RuleStatus status);

    List<TestCaseEntity> findByStatus(RuleStatus status);

    List<TestCaseEntity> findByScenarioScenarioIdIn(List<Long> scenarioIds);

    @Query("SELECT tc.testCaseId FROM TestCaseEntity tc WHERE tc.scenario.scenarioId = :scenarioId")
    List<Long> findTestCaseIdsByScenarioId(@Param("scenarioId") Long scenarioId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TestCaseEntity tc WHERE tc.testCaseId IN :ids")
    void deleteByTestCaseIdIn(@Param("ids") Collection<Long> ids);
}