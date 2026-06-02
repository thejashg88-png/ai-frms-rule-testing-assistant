package com.thejas.ai_frms.rule.repository;

import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RuleRepository extends JpaRepository<RuleEntity, Long>, JpaSpecificationExecutor<RuleEntity> {

    // Used to enforce unique rule name constraint before saving (case-insensitive)
    boolean existsByRuleNameIgnoreCase(String ruleName);

    // Used during update to allow the same rule to keep its own name while preventing duplicates
    boolean existsByRuleNameIgnoreCaseAndRuleIdNot(String ruleName, Long ruleId);

    // Used by TestScenarioServiceImpl to resolve a scenario's rule by type; prefers ACTIVE rules
    List<RuleEntity> findByRuleTypeAndStatus(String ruleType, RuleStatus status);

    // Fallback when no ACTIVE rule exists for a given type (returns any status)
    List<RuleEntity> findByRuleType(String ruleType);

    // Used by TransactionServiceImpl and TransactionRuleEvaluationService to get rules for risk evaluation
    List<RuleEntity> findByStatus(RuleStatus status);
}