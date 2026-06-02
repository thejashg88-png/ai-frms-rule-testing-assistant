package com.thejas.ai_frms.testcase.repository;

import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCaseEntity, Long>,
        JpaSpecificationExecutor<TestCaseEntity> {

    boolean existsByTestCaseNameIgnoreCase(String testCaseName);

    boolean existsByTestCaseNameIgnoreCaseAndTestCaseIdNot(String testCaseName, Long testCaseId);

    List<TestCaseEntity> findByScenarioScenarioId(Long scenarioId);

    List<TestCaseEntity> findByScenarioScenarioIdAndStatus(Long scenarioId, RuleStatus status);

    List<TestCaseEntity> findByStatus(RuleStatus status);
}