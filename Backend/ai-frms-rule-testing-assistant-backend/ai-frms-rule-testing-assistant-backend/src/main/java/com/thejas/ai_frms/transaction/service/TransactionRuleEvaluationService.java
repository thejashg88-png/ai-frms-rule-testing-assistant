package com.thejas.ai_frms.transaction.service;

import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.transaction.dto.RuleEvaluationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Evaluates all ACTIVE fraud rules against a transaction amount and returns the highest-severity result.
 *
 * Priority when multiple rules match the same transaction:
 *   REJECT (3) > MONITOR (2) > ACCEPT (1)
 *
 * The rule with the highest severity action becomes the triggeredRule in the response.
 * If no rule matches, the result is ACCEPT with triggeredRuleName = null.
 *
 * Currently supported rule types: SINGLE_LARGE_TX
 * Other rule types (HIGH_FREQ, SEQUENTIAL_TXN, etc.) are handled by RuleExecutionEngine
 * during test case execution, not here (transaction listing only evaluates amount-based rules).
 */
@Service
public class TransactionRuleEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(TransactionRuleEvaluationService.class);

    private static final Map<String, Integer> ACTION_SEVERITY = Map.of(
            "ACCEPT", 1,
            "MONITOR", 2,
            "REJECT", 3
    );

    /**
     * Evaluates all active rules against the transaction amount and returns the highest-severity match.
     * REJECT (3) > MONITOR (2) > ACCEPT (1).
     */
    public RuleEvaluationResult evaluate(Long transactionId, BigDecimal amount, List<RuleEntity> activeRules) {
        log.info("[TRANSACTION RISK EVAL] transactionId={}, amount={}", transactionId, amount);

        if (amount == null || activeRules == null || activeRules.isEmpty()) {
            log.info("[TRANSACTION RISK EVAL] transactionId={} — no active rules, result=ACCEPT", transactionId);
            return acceptResult();
        }

        List<RuleEvaluationResult> matchedResults = new ArrayList<>();

        for (RuleEntity rule : activeRules) {
            log.info("[TRANSACTION RISK EVAL] checking rule={}, type={}, action={}, maxAmount={}",
                    rule.getRuleName(), rule.getRuleType(), rule.getAction(), rule.getMaxAmount());

            RuleEvaluationResult result = evaluateRule(amount, rule);
            boolean matched = result != null;
            log.info("[TRANSACTION RISK EVAL] matched={}", matched);

            if (matched) {
                matchedResults.add(result);
            }
        }

        List<String> matchedActions = matchedResults.stream()
                .map(RuleEvaluationResult::getTriggeredAction)
                .toList();
        log.info("[TRANSACTION RISK EVAL] matched actions={}", matchedActions);

        if (matchedResults.isEmpty()) {
            log.info("[TRANSACTION RISK EVAL] finalAction=ACCEPT, triggeredRule=null");
            return acceptResult();
        }

        // Select the result with the highest severity action
        RuleEvaluationResult finalResult = matchedResults.stream()
                .max((a, b) -> Integer.compare(
                        ACTION_SEVERITY.getOrDefault(a.getTriggeredAction(), 0),
                        ACTION_SEVERITY.getOrDefault(b.getTriggeredAction(), 0)
                ))
                .orElse(acceptResult());

        log.info("[TRANSACTION RISK EVAL] finalAction={}, triggeredRule={}",
                finalResult.getTriggeredAction(), finalResult.getTriggeredRuleName());

        return finalResult;
    }

    private RuleEvaluationResult evaluateRule(BigDecimal amount, RuleEntity rule) {
        if (rule.getRuleType() == null) return null;

        switch (rule.getRuleType().toUpperCase()) {
            case "SINGLE_LARGE_TX":
                return evaluateSingleLargeTx(amount, rule);
            default:
                return null;
        }
    }

    private RuleEvaluationResult evaluateSingleLargeTx(BigDecimal amount, RuleEntity rule) {
        BigDecimal maxAmount = rule.getMaxAmount();
        if (maxAmount == null) return null;

        if (amount.compareTo(maxAmount) > 0) {
            String action = rule.getAction() != null ? rule.getAction().name() : "MONITOR";
            String reason = "Transaction amount " + amount.toPlainString()
                    + " is greater than " + action.toLowerCase() + " threshold " + maxAmount.toPlainString();
            return new RuleEvaluationResult(action, rule.getRuleName(), rule.getRuleType(), action, reason);
        }
        return null;
    }

    private RuleEvaluationResult acceptResult() {
        return new RuleEvaluationResult("ACCEPT", null, null, "ACCEPT", "No active rule triggered");
    }
}