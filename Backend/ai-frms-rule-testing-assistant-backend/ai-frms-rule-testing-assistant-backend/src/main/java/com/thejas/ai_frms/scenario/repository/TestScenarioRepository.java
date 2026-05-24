package com.thejas.ai_frms.scenario.repository;

import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TestScenarioRepository extends JpaRepository<TestScenarioEntity, Long>,
        JpaSpecificationExecutor<TestScenarioEntity> {

    boolean existsByScenarioNameIgnoreCase(String scenarioName);

    boolean existsByScenarioNameIgnoreCaseAndScenarioIdNot(String scenarioName, Long scenarioId);

    List<TestScenarioEntity> findByRuleRuleId(Long ruleId);

    List<TestScenarioEntity> findByStatus(RuleStatus status);
}