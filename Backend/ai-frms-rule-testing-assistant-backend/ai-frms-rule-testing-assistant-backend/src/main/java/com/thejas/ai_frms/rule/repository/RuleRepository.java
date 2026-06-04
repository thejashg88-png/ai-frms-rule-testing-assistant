package com.thejas.ai_frms.rule.repository;

import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RuleRepository extends JpaRepository<RuleEntity, Long>, JpaSpecificationExecutor<RuleEntity> {

    boolean existsByRuleNameIgnoreCase(String ruleName);

    boolean existsByRuleNameIgnoreCaseAndRuleIdNot(String ruleName, Long ruleId);

    List<RuleEntity> findByRuleTypeAndStatus(String ruleType, RuleStatus status);

    List<RuleEntity> findByRuleType(String ruleType);

    List<RuleEntity> findByStatus(RuleStatus status);

    long countByStatus(RuleStatus status);
}