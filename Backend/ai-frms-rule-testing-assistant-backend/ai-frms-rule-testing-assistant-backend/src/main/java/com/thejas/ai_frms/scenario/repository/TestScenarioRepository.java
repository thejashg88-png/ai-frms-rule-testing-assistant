package com.thejas.ai_frms.scenario.repository;

import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TestScenarioRepository extends JpaRepository<TestScenarioEntity, Long>,
        JpaSpecificationExecutor<TestScenarioEntity> {

    boolean existsByScenarioNameIgnoreCase(String scenarioName);

    boolean existsByScenarioNameIgnoreCaseAndScenarioIdNot(String scenarioName, Long scenarioId);

    List<TestScenarioEntity> findByRuleRuleId(Long ruleId);

    boolean existsByRuleRuleId(Long ruleId);

    List<TestScenarioEntity> findByStatus(RuleStatus status);

    @Query("SELECT s.scenarioId FROM TestScenarioEntity s WHERE s.rule.ruleId = :ruleId")
    List<Long> findScenarioIdsByRuleId(@Param("ruleId") Long ruleId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TestScenarioEntity s WHERE s.scenarioId IN :ids")
    void deleteByScenarioIdIn(@Param("ids") Collection<Long> ids);
}