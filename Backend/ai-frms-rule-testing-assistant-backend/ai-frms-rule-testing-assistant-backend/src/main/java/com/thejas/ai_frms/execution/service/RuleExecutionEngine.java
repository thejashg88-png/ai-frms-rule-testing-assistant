package com.thejas.ai_frms.execution.service;

import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.execution.dto.ComparisonResult;
import com.thejas.ai_frms.execution.dto.RuleEvaluationExplanationResponse;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.testcase.dto.TestInputData;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import com.thejas.ai_frms.transaction.entity.TransactionEntity;
import com.thejas.ai_frms.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.thejas.ai_frms.execution.dto.ExecutionTraceStepResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Executes the fraud rule linked to a test case against the provided input data.
 *
 * Each rule type has its own evaluation logic:
 *   SINGLE_LARGE_TX / DAILY_LIMIT / MONTHLY_VOLUME / ANNUAL_VOLUME
 *       → triggers when amount > maxAmount
 *   STRUCTURING
 *       → triggers when amount < txnAmount (below-threshold pattern)
 *   HIGH_FREQ_TXN
 *       → counts historical transactions within frequencyHours window for the same card/device;
 *          triggers when (historicalCount + 1) > txnCount
 *   SEQUENTIAL_TXN
 *       → same window logic as HIGH_FREQ_TXN but uses >= threshold (inclusive)
 *   UNUSUAL_AMT
 *       → computes baseline average from historical transactions;
 *          triggers when amount > avg + (avg * percentageThreshold / 100)
 *   Default (unrecognized type)
 *       → triggers when amount > maxAmount OR amount < txnAmount
 *
 * For rules that query transaction history (HIGH_FREQ, SEQUENTIAL, UNUSUAL_AMT):
 *   Identifier lookup order: serialNumber → track2Data → cardNumber (as track2Data)
 *   If no history is found, the rule returns ACCEPT (cannot evaluate without a baseline).
 */
@Service
public class RuleExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleExecutionEngine.class);

    private final TransactionRepository transactionRepository;

    public RuleExecutionEngine(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Unified internal result type for all rule evaluators.
     * Carries the actual action, an optional engine note (for failureReason enrichment),
     * and the detailed explanation DTO.
     */
    private record RuleEvalResult(
            RuleAction action,
            String engineNote,
            RuleEvaluationExplanationResponse explanation
    ) {}

    public ComparisonResult execute(TestCaseEntity testCase, TestInputData inputData) {
        if (testCase == null) {
            throw new BadRequestException("Test case cannot be null");
        }

        if (testCase.getScenario() == null || testCase.getScenario().getRule() == null) {
            throw new BadRequestException("No rule linked with this test case scenario");
        }

        RuleEntity rule = testCase.getScenario().getRule();
        String ruleType = rule.getRuleType();

        BigDecimal amount = inputData != null ? inputData.getAmount() : null;

        log.info("[EXECUTION] Running testCaseId={}", testCase.getTestCaseId());
        log.info("[EXECUTION] Rule type={}, action={}, maxAmount={}, txnAmount={}",
                ruleType, rule.getAction(), rule.getMaxAmount(), rule.getTxnAmount());
        log.info("[EXECUTION] Input amount={}", amount);

        String ruleTypeUpper = ruleType != null ? ruleType.toUpperCase() : "";

        RuleEvalResult evalResult;
        if (ruleTypeUpper.equals("UNUSUAL_AMT") || ruleTypeUpper.equals("UNUSUAL_AMOUNT")) {
            evalResult = evaluateUnusualAmountRule(rule, inputData);
        } else if (ruleTypeUpper.equals("SEQUENTIAL_TXN")) {
            evalResult = evaluateSequentialTransactionRule(rule, inputData);
        } else {
            evalResult = evaluateRule(rule, inputData);
        }

        RuleAction actualAction = evalResult.action();
        String engineNote = evalResult.engineNote();
        RuleEvaluationExplanationResponse explanation = evalResult.explanation();

        // Populate explanation action fields
        if (explanation != null) {
            explanation.setActualAction(actualAction.name());
        }

        log.info("[EXECUTION] Actual action={}", actualAction);
        log.info("[RULE EXPLANATION] ruleType={}", ruleType);
        log.info("[RULE EXPLANATION] actualAction={}", actualAction);
        if (explanation != null) {
            log.info("[RULE EXPLANATION] matchedCount={}", explanation.getMatchedCount());
            log.info("[RULE EXPLANATION] reason={}", explanation.getRuleReason());
        }

        ComparisonResult result = new ComparisonResult();
        result.setActualAction(actualAction);
        result.setActualEvaluationStatus(actualAction.name());
        result.setActualRuleType(ruleType);
        result.setActualAlertCodes(buildAlertCodes(rule, actualAction));
        result.setActualRiskScore(buildRiskScore(actualAction));
        result.setRuleType(ruleType);
        result.setInputAmount(amount);
        result.setEngineNote(engineNote);
        result.setRuleExplanation(explanation);

        try {
            List<ExecutionTraceStepResponse> trace = buildExecutionTrace(
                    ruleTypeUpper, inputData, rule, explanation, actualAction);
            result.setExecutionTrace(trace);
        } catch (Exception traceEx) {
            log.warn("[EXECUTION TRACE] trace generation failed for testCaseId={}: {}",
                    testCase.getTestCaseId(), traceEx.getMessage());
        }

        return result;
    }

    // ── Rule dispatch ─────────────────────────────────────────────────────────

    private RuleEvalResult evaluateRule(RuleEntity rule, TestInputData inputData) {
        if (rule == null || inputData == null || inputData.getAmount() == null) {
            RuleEvaluationExplanationResponse expl = new RuleEvaluationExplanationResponse();
            expl.setRuleType(rule != null ? rule.getRuleType() : null);
            expl.setRuleName(rule != null ? rule.getRuleName() : null);
            expl.setTriggered(false);
            expl.setRuleReason("No input data provided — cannot evaluate rule.");
            expl.setResultExplanation("Rule could not be evaluated due to missing transaction amount.");
            return new RuleEvalResult(RuleAction.ACCEPT, null, expl);
        }

        String ruleType = rule.getRuleType();
        BigDecimal amount = inputData.getAmount();

        if (ruleType == null || ruleType.isBlank()) {
            RuleEvaluationExplanationResponse expl = new RuleEvaluationExplanationResponse();
            expl.setTriggered(false);
            expl.setRuleReason("Rule type is not configured.");
            expl.setResultExplanation("Rule did not trigger because ruleType is missing.");
            return new RuleEvalResult(RuleAction.ACCEPT, null, expl);
        }

        return switch (ruleType.toUpperCase()) {
            case "SINGLE_LARGE_TX", "DAILY_LIMIT", "DAILY_TXN_VALUE", "MONTHLY_VOLUME", "ANNUAL_VOLUME",
                    "ANNUAL_TXN_VOLUME", "MONTHLY_TXN_VOLUME" ->
                    evaluateAmountGreaterThanRule(rule, amount);

            case "HIGH_FREQ_TXN" ->
                    evaluateHighFrequencyRule(rule, inputData);

            case "STRUCTURING" ->
                    evaluateStructuringRule(rule, amount);

            default ->
                    evaluateDefaultRule(rule, amount);
        };
    }

    // ── SINGLE_LARGE_TX / DAILY_LIMIT / MONTHLY_VOLUME / ANNUAL_VOLUME ────────

    // Used by SINGLE_LARGE_TX, DAILY_LIMIT, MONTHLY_VOLUME, ANNUAL_VOLUME, ANNUAL_TXN_VOLUME, MONTHLY_TXN_VOLUME
    private RuleEvalResult evaluateAmountGreaterThanRule(RuleEntity rule, BigDecimal amount) {
        BigDecimal maxAmount = rule.getMaxAmount();

        RuleEvaluationExplanationResponse expl = new RuleEvaluationExplanationResponse();
        expl.setRuleType(rule.getRuleType());
        expl.setRuleName(rule.getRuleName());
        expl.setActualAmount(amount);
        expl.setMaxAmount(maxAmount);
        expl.setThresholdAmount(maxAmount);

        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            expl.setTriggered(true);
            expl.setRuleReason("Transaction amount " + amount.toPlainString()
                    + " is greater than configured maxAmount " + maxAmount.toPlainString() + ".");
            expl.setResultExplanation("Rule triggered because the transaction amount exceeds the configured maximum threshold.");
            return new RuleEvalResult(rule.getAction(), null, expl);
        }

        expl.setTriggered(false);
        expl.setRuleReason("Transaction amount " + amount.toPlainString()
                + " is not greater than configured maxAmount "
                + (maxAmount != null ? maxAmount.toPlainString() : "null") + ".");
        expl.setResultExplanation("Rule did not trigger because the transaction amount is within the configured maximum threshold.");
        return new RuleEvalResult(RuleAction.ACCEPT, null, expl);
    }

    // ── HIGH_FREQ_TXN ──────────────────────────────────────────────────────────

    private RuleEvalResult evaluateHighFrequencyRule(RuleEntity rule, TestInputData inputData) {
        // Normalize card number — remove spaces
        String rawCardNumber = inputData.getCardNumber();
        String cardNumber = rawCardNumber != null ? rawCardNumber.replaceAll("\\s+", "") : null;
        String serialNumber = inputData.getSerialNumber();
        String track2Data = inputData.getTrack2Data();

        Integer txnCountThreshold = rule.getTxnCount();
        Integer frequencyHours = rule.getFrequencyHours();

        log.info("[HIGH_FREQ_TXN] input cardNumber={}", rawCardNumber);
        log.info("[HIGH_FREQ_TXN] normalizedCardNumber={}", cardNumber);
        log.info("[HIGH_FREQ_TXN] serialNumber={}", serialNumber);
        log.info("[HIGH_FREQ_TXN] frequencyHours={}", frequencyHours);
        log.info("[HIGH_FREQ_TXN] txnCountThreshold={}", txnCountThreshold);

        // Default window to 1 hour if frequencyHours is not configured
        int windowHours = (frequencyHours != null && frequencyHours > 0) ? frequencyHours : 1;
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusHours(windowHours);

        log.info("[HIGH_FREQ_TXN] windowStart={}", windowStart);
        log.info("[HIGH_FREQ_TXN] windowEnd={}", windowEnd);

        List<TransactionEntity> windowTransactions = fetchWindowTransactions(
                cardNumber, serialNumber, track2Data, windowStart, windowEnd);

        // Filter to successful transactions only (SUCCESS status or response code 00)
        List<TransactionEntity> successfulTxns = windowTransactions.stream()
                .filter(t -> "SUCCESS".equalsIgnoreCase(t.getTransactionStatus())
                        || "00".equals(t.getResponseCode()))
                .collect(Collectors.toList());
        long successfulCount = successfulTxns.size();

        // Include the current test transaction in the count (+1)
        long totalCount = successfulCount + 1;

        log.info("[HIGH_FREQ_TXN] matched historical transaction count={} (successful={})",
                windowTransactions.size(), successfulCount);
        log.info("[HIGH_FREQ_TXN] total count (historical + current)={}", totalCount);

        int threshold = (txnCountThreshold != null && txnCountThreshold > 0) ? txnCountThreshold : Integer.MAX_VALUE;

        // Build explanation
        RuleEvaluationExplanationResponse expl = new RuleEvaluationExplanationResponse();
        expl.setRuleType(rule.getRuleType());
        expl.setRuleName(rule.getRuleName());
        expl.setHistoricalCount((int) successfulCount);
        expl.setCurrentCount(1);
        expl.setMatchedCount((int) totalCount);
        expl.setRequiredCount(txnCountThreshold != null && txnCountThreshold > 0 ? txnCountThreshold : null);
        expl.setFrequencyWindow("last " + windowHours + " hour" + (windowHours == 1 ? "" : "s"));
        expl.setWindowStart(windowStart.toString());
        expl.setWindowEnd(windowEnd.toString());
        expl.setMatchedTransactions(buildSafeTxnList(successfulTxns));

        log.info("[RULE EXPLANATION] created for ruleType=HIGH_FREQ_TXN, matchedTransactions size={}",
                successfulTxns.size());

        String thresholdDisplay = txnCountThreshold != null && txnCountThreshold > 0
                ? String.valueOf(txnCountThreshold)
                : "not configured";

        if (totalCount > threshold) {
            log.info("[HIGH_FREQ_TXN] totalCount {} > threshold {} → actualAction={}", totalCount, threshold, rule.getAction());
            expl.setTriggered(true);
            expl.setRuleReason("High frequency threshold reached. Matched count=" + totalCount
                    + " (historical=" + successfulCount + " + current=1), required=" + thresholdDisplay + ".");
            expl.setResultExplanation("Rule triggered because the number of transactions within the frequency window exceeded the configured threshold.");
            return new RuleEvalResult(rule.getAction(), null, expl);
        }

        log.info("[HIGH_FREQ_TXN] totalCount {} <= threshold {} → actualAction=ACCEPT", totalCount, threshold);
        expl.setTriggered(false);
        expl.setRuleReason("High frequency threshold not reached. Matched count=" + totalCount
                + " (historical=" + successfulCount + " + current=1), required=" + thresholdDisplay + ".");
        expl.setResultExplanation("Rule did not trigger because not enough matching historical transactions were found within the frequency window.");
        return new RuleEvalResult(RuleAction.ACCEPT, null, expl);
    }

    private List<TransactionEntity> fetchWindowTransactions(
            String cardNumber,
            String serialNumber,
            String track2Data,
            LocalDateTime windowStart,
            LocalDateTime windowEnd
    ) {
        // Try serialNumber first
        if (serialNumber != null && !serialNumber.isBlank()) {
            List<TransactionEntity> result = transactionRepository
                    .findBySerialNumberAndTransactionTimeBetween(serialNumber, windowStart, windowEnd);
            if (!result.isEmpty()) {
                log.info("[HIGH_FREQ_TXN] window transactions found by serialNumber={}, count={}",
                        serialNumber, result.size());
                return result;
            }
        }

        // Try track2Data
        if (track2Data != null && !track2Data.isBlank()) {
            List<TransactionEntity> result = transactionRepository
                    .findByTrack2DataAndTransactionTimeBetween(track2Data, windowStart, windowEnd);
            if (!result.isEmpty()) {
                log.info("[HIGH_FREQ_TXN] window transactions found by track2Data={}, count={}",
                        track2Data, result.size());
                return result;
            }
        }

        // Try normalized cardNumber as track2Data
        if (cardNumber != null && !cardNumber.isBlank()) {
            List<TransactionEntity> result = transactionRepository
                    .findByTrack2DataAndTransactionTimeBetween(cardNumber, windowStart, windowEnd);
            if (!result.isEmpty()) {
                log.info("[HIGH_FREQ_TXN] window transactions found by cardNumber(as track2Data)={}, count={}",
                        cardNumber, result.size());
                return result;
            }
        }

        log.info("[HIGH_FREQ_TXN] no window transactions found for any identifier");
        return Collections.emptyList();
    }

    // ── STRUCTURING ───────────────────────────────────────────────────────────

    // STRUCTURING triggers when amount is BELOW txnAmount (opposite of most other rules)
    private RuleEvalResult evaluateStructuringRule(RuleEntity rule, BigDecimal amount) {
        BigDecimal txnAmount = rule.getTxnAmount();

        RuleEvaluationExplanationResponse expl = new RuleEvaluationExplanationResponse();
        expl.setRuleType(rule.getRuleType());
        expl.setRuleName(rule.getRuleName());
        expl.setActualAmount(amount);
        expl.setThresholdAmount(txnAmount);

        if (txnAmount != null && amount.compareTo(txnAmount) < 0) {
            expl.setTriggered(true);
            expl.setRuleReason("Transaction amount " + amount.toPlainString()
                    + " is below the configured structuring threshold " + txnAmount.toPlainString() + ".");
            expl.setResultExplanation("Rule triggered — small transaction below structuring threshold detected. This pattern may indicate attempt to avoid reporting thresholds.");
            return new RuleEvalResult(rule.getAction(), null, expl);
        }

        expl.setTriggered(false);
        expl.setRuleReason("Transaction amount " + amount.toPlainString()
                + " is not below the configured structuring threshold "
                + (txnAmount != null ? txnAmount.toPlainString() : "null") + ".");
        expl.setResultExplanation("Rule did not trigger because the transaction amount is above the structuring threshold.");
        return new RuleEvalResult(RuleAction.ACCEPT, null, expl);
    }

    // ── Default (fallback for unrecognized rule types) ────────────────────────

    private RuleEvalResult evaluateDefaultRule(RuleEntity rule, BigDecimal amount) {
        BigDecimal maxAmount = rule.getMaxAmount();
        BigDecimal txnAmount = rule.getTxnAmount();

        RuleEvaluationExplanationResponse expl = new RuleEvaluationExplanationResponse();
        expl.setRuleType(rule.getRuleType());
        expl.setRuleName(rule.getRuleName());
        expl.setActualAmount(amount);
        expl.setMaxAmount(maxAmount);
        expl.setThresholdAmount(txnAmount != null ? txnAmount : maxAmount);

        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            expl.setTriggered(true);
            expl.setRuleReason("Transaction amount " + amount.toPlainString()
                    + " exceeds configured maxAmount " + maxAmount.toPlainString() + ".");
            expl.setResultExplanation("Rule triggered because transaction amount exceeded the configured limit.");
            return new RuleEvalResult(rule.getAction(), null, expl);
        }
        if (txnAmount != null && amount.compareTo(txnAmount) < 0) {
            expl.setTriggered(true);
            expl.setRuleReason("Transaction amount " + amount.toPlainString()
                    + " is below configured txnAmount " + txnAmount.toPlainString() + ".");
            expl.setResultExplanation("Rule triggered because transaction amount is below the configured threshold.");
            return new RuleEvalResult(rule.getAction(), null, expl);
        }

        expl.setTriggered(false);
        expl.setRuleReason("Transaction amount " + amount.toPlainString()
                + " did not trigger any configured threshold for rule type " + rule.getRuleType() + ".");
        expl.setResultExplanation("Rule did not trigger.");
        return new RuleEvalResult(RuleAction.ACCEPT, null, expl);
    }

    // ── UNUSUAL_AMT ────────────────────────────────────────────────────────────

    private RuleEvalResult evaluateUnusualAmountRule(RuleEntity rule, TestInputData inputData) {
        BigDecimal amount = inputData != null ? inputData.getAmount() : null;
        BigDecimal percentageThreshold = rule.getPercentageThreshold();

        // Normalize card number — remove spaces ("1234 1234 1234 3455" → "1234123412343455")
        String rawCardNumber = inputData != null ? inputData.getCardNumber() : null;
        String cardNumber = rawCardNumber != null ? rawCardNumber.replaceAll("\\s+", "") : null;
        String serialNumber = inputData != null ? inputData.getSerialNumber() : null;
        String track2Data = inputData != null ? inputData.getTrack2Data() : null;

        log.info("[UNUSUAL_AMT] input amount={}", amount);
        log.info("[UNUSUAL_AMT] cardNumber={}", cardNumber);
        log.info("[UNUSUAL_AMT] serialNumber={}", serialNumber);
        log.info("[UNUSUAL_AMT] track2Data={}", track2Data);
        log.info("[UNUSUAL_AMT] percentageThreshold={}", percentageThreshold);

        RuleEvaluationExplanationResponse expl = new RuleEvaluationExplanationResponse();
        expl.setRuleType(rule.getRuleType());
        expl.setRuleName(rule.getRuleName());
        expl.setActualAmount(amount);
        expl.setPercentageThreshold(percentageThreshold);

        if (amount == null) {
            expl.setTriggered(false);
            expl.setRuleReason("Input amount is null — cannot evaluate UNUSUAL_AMT.");
            expl.setResultExplanation("Rule could not be evaluated due to missing transaction amount.");
            return new RuleEvalResult(RuleAction.ACCEPT, "Input amount is null — cannot evaluate UNUSUAL_AMT", expl);
        }

        List<TransactionEntity> baseline = fetchBaselineTransactions(cardNumber, serialNumber, track2Data);

        log.info("[UNUSUAL_AMT] baseline transactions count={}", baseline.size());

        if (baseline.isEmpty()) {
            expl.setTriggered(false);
            expl.setHistoricalCount(0);
            expl.setMatchedTransactions(Collections.emptyList());
            expl.setRuleReason("No baseline transaction history found for unusual amount evaluation.");
            expl.setResultExplanation("Rule did not trigger because no historical transactions were found for comparison.");
            String note = "No baseline transaction history found for unusual amount evaluation";
            log.info("[UNUSUAL_AMT] no baseline found → actualAction=ACCEPT");
            return new RuleEvalResult(RuleAction.ACCEPT, note, expl);
        }

        // Calculate baseline average
        BigDecimal sum = baseline.stream()
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(baseline.size()), 2, RoundingMode.HALF_UP);

        log.info("[UNUSUAL_AMT] baseline average={}", avg);

        // Calculate trigger threshold: average + (average * percentageThreshold / 100)
        BigDecimal threshold;
        if (percentageThreshold != null && percentageThreshold.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal premium = avg.multiply(percentageThreshold)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            threshold = avg.add(premium);
        } else {
            threshold = avg;
        }

        log.info("[UNUSUAL_AMT] trigger threshold amount (avg + threshold%)={}", threshold);

        expl.setAverageAmount(avg);
        expl.setThresholdAmount(threshold);
        expl.setHistoricalCount(baseline.size());
        expl.setMatchedTransactions(buildSafeTxnList(baseline));

        if (amount.compareTo(threshold) > 0) {
            log.info("[UNUSUAL_AMT] amount {} > threshold {} → actualAction={}", amount, threshold, rule.getAction());
            expl.setTriggered(true);
            expl.setRuleReason("Current amount " + amount.toPlainString()
                    + " is greater than unusual threshold " + threshold.toPlainString()
                    + " based on average amount " + avg.toPlainString()
                    + " and threshold " + percentageThreshold + "%.");
            expl.setResultExplanation("Rule triggered because the transaction amount significantly deviates from the historical baseline.");
            return new RuleEvalResult(rule.getAction(), null, expl);
        } else {
            String note = "Current amount " + amount + " did not exceed unusual amount threshold "
                    + threshold + " (baseline avg=" + avg + ", percentageThreshold=" + percentageThreshold + "%)";
            log.info("[UNUSUAL_AMT] amount {} <= threshold {} → actualAction=ACCEPT", amount, threshold);
            expl.setTriggered(false);
            expl.setRuleReason("Current amount did not exceed unusual amount threshold or no baseline history was found.");
            expl.setResultExplanation("Rule did not trigger because the amount is within the expected range based on historical transactions (avg=" + avg.toPlainString() + ", threshold=" + threshold.toPlainString() + ").");
            return new RuleEvalResult(RuleAction.ACCEPT, note, expl);
        }
    }

    private List<TransactionEntity> fetchBaselineTransactions(
            String cardNumber,
            String serialNumber,
            String track2Data
    ) {
        // Try serialNumber first — it is the primary identifier in frms_transactions
        if (serialNumber != null && !serialNumber.isBlank()) {
            List<TransactionEntity> result = transactionRepository.findBySerialNumber(serialNumber);
            if (!result.isEmpty()) {
                log.info("[UNUSUAL_AMT] baseline found by serialNumber={}, count={}", serialNumber, result.size());
                return result;
            }
        }

        // Try track2Data
        if (track2Data != null && !track2Data.isBlank()) {
            List<TransactionEntity> result = transactionRepository.findByTrack2Data(track2Data);
            if (!result.isEmpty()) {
                log.info("[UNUSUAL_AMT] baseline found by track2Data={}, count={}", track2Data, result.size());
                return result;
            }
        }

        // Try normalized cardNumber as track2Data (PAN is often stored in track2Data)
        if (cardNumber != null && !cardNumber.isBlank()) {
            List<TransactionEntity> result = transactionRepository.findByTrack2Data(cardNumber);
            if (!result.isEmpty()) {
                log.info("[UNUSUAL_AMT] baseline found by cardNumber(as track2Data)={}, count={}", cardNumber, result.size());
                return result;
            }
        }

        return Collections.emptyList();
    }

    // ── SEQUENTIAL_TXN ────────────────────────────────────────────────────────

    private RuleEvalResult evaluateSequentialTransactionRule(RuleEntity rule, TestInputData inputData) {
        String rawCardNumber = inputData.getCardNumber();
        // Normalize — remove spaces and hyphens
        String normalizedCard = rawCardNumber != null
                ? rawCardNumber.replaceAll("[\\s\\-]", "")
                : null;
        String serialNumber = inputData.getSerialNumber();
        String tid = inputData.getTid();

        Integer txnCountThreshold = rule.getTxnCount();
        Integer frequencyHours = rule.getFrequencyHours();

        log.info("[SEQUENTIAL_TXN] raw cardNumber={}", rawCardNumber);
        log.info("[SEQUENTIAL_TXN] normalizedCardNumber={}", normalizedCard);
        log.info("[SEQUENTIAL_TXN] serialNumber={}", serialNumber);
        log.info("[SEQUENTIAL_TXN] tid={}", tid);
        log.info("[SEQUENTIAL_TXN] frequencyHours={}", frequencyHours);
        log.info("[SEQUENTIAL_TXN] txnCountThreshold={}", txnCountThreshold);

        int windowHours = (frequencyHours != null && frequencyHours > 0) ? frequencyHours : 1;
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusHours(windowHours);

        log.info("[SEQUENTIAL_TXN] windowStart={}", windowStart);
        log.info("[SEQUENTIAL_TXN] windowEnd={}", windowEnd);

        List<TransactionEntity> windowTransactions = fetchSequentialWindowTransactions(
                normalizedCard, serialNumber, tid, windowStart, windowEnd);

        // Filter to successful transactions only
        List<TransactionEntity> successful = windowTransactions.stream()
                .filter(t -> "SUCCESS".equalsIgnoreCase(t.getTransactionStatus())
                        || "00".equals(t.getResponseCode()))
                .toList();

        long historicalCount = successful.size();
        long totalCount = historicalCount + 1; // +1 for current test transaction

        log.info("[SEQUENTIAL_TXN] matched historical transaction count={} (successful={})",
                windowTransactions.size(), historicalCount);
        log.info("[SEQUENTIAL_TXN] matched transactions stans/ids={}",
                successful.stream()
                        .map(t -> t.getStan() != null ? t.getStan() : String.valueOf(t.getTransactionId()))
                        .toList());

        int threshold = (txnCountThreshold != null && txnCountThreshold > 0)
                ? txnCountThreshold
                : Integer.MAX_VALUE;

        String thresholdDisplay = txnCountThreshold != null && txnCountThreshold > 0
                ? String.valueOf(txnCountThreshold)
                : "not configured";

        // Build explanation
        RuleEvaluationExplanationResponse expl = new RuleEvaluationExplanationResponse();
        expl.setRuleType(rule.getRuleType());
        expl.setRuleName(rule.getRuleName());
        expl.setHistoricalCount((int) historicalCount);
        expl.setCurrentCount(1);
        expl.setMatchedCount((int) totalCount);
        expl.setRequiredCount(txnCountThreshold != null && txnCountThreshold > 0 ? txnCountThreshold : null);
        expl.setFrequencyWindow("last " + windowHours + " hour" + (windowHours == 1 ? "" : "s"));
        expl.setWindowStart(windowStart.toString());
        expl.setWindowEnd(windowEnd.toString());
        expl.setMatchedTransactions(buildSafeTxnList(new java.util.ArrayList<>(successful)));

        log.info("[RULE EXPLANATION] created for ruleType=SEQUENTIAL_TXN, matchedTransactions size={}",
                successful.size());

        // Use >= : txnCount is the minimum count that triggers the rule
        if (totalCount >= threshold) {
            log.info("[SEQUENTIAL_TXN] totalCount {} >= threshold {} → actualAction={}",
                    totalCount, threshold, rule.getAction());
            expl.setTriggered(true);
            expl.setRuleReason("Sequential transaction threshold reached for same card/device within the configured window. Matched count="
                    + totalCount + " (historical=" + historicalCount + " + current=1), required=" + thresholdDisplay + ".");
            expl.setResultExplanation("Rule triggered because repeated transactions from the same card or device were detected within the configured time window.");
            return new RuleEvalResult(rule.getAction(), null, expl);
        }

        String note = "Sequential transaction threshold not reached. Matched count="
                + totalCount + " (historical=" + historicalCount + " + current=1), required=" + thresholdDisplay;
        log.info("[SEQUENTIAL_TXN] totalCount {} < threshold {} → actualAction=ACCEPT", totalCount, threshold);
        expl.setTriggered(false);
        expl.setRuleReason("Sequential transaction threshold not reached. Matched count=" + totalCount
                + " (historical=" + historicalCount + " + current=1), required=" + thresholdDisplay + ".");
        expl.setResultExplanation("Rule did not trigger because the number of sequential transactions is below the configured threshold.");
        return new RuleEvalResult(RuleAction.ACCEPT, note, expl);
    }

    private List<TransactionEntity> fetchSequentialWindowTransactions(
            String normalizedCard,
            String serialNumber,
            String tid,
            LocalDateTime windowStart,
            LocalDateTime windowEnd
    ) {
        // Try serialNumber first — most reliable identifier
        if (serialNumber != null && !serialNumber.isBlank()) {
            List<TransactionEntity> result = transactionRepository
                    .findBySerialNumberAndTransactionTimeBetween(serialNumber, windowStart, windowEnd);
            if (!result.isEmpty()) {
                log.info("[SEQUENTIAL_TXN] window transactions found by serialNumber={}, count={}",
                        serialNumber, result.size());
                return result;
            }
        }

        // Try card number — detect masked vs plain
        if (normalizedCard != null && !normalizedCard.isBlank()) {
            boolean isMasked = normalizedCard.contains("*") || normalizedCard.contains("X")
                    || normalizedCard.contains("x");

            if (isMasked) {
                // Extract only digits — last 4 are the visible suffix of a masked card
                String digitsOnly = normalizedCard.replaceAll("[^0-9]", "");
                String last4 = digitsOnly.length() >= 4
                        ? digitsOnly.substring(digitsOnly.length() - 4)
                        : digitsOnly;

                if (!last4.isBlank()) {
                    List<TransactionEntity> result = transactionRepository
                            .findByTrack2DataEndingWithAndTransactionTimeBetween(last4, windowStart, windowEnd);
                    if (!result.isEmpty()) {
                        log.info("[SEQUENTIAL_TXN] window transactions found by last4={}, count={}",
                                last4, result.size());
                        return result;
                    }
                }
            } else {
                // Exact match on full normalized card number
                List<TransactionEntity> result = transactionRepository
                        .findByTrack2DataAndTransactionTimeBetween(normalizedCard, windowStart, windowEnd);
                if (!result.isEmpty()) {
                    log.info("[SEQUENTIAL_TXN] window transactions found by card(track2Data)={}, count={}",
                            normalizedCard, result.size());
                    return result;
                }
            }
        }

        // Fallback: tid
        if (tid != null && !tid.isBlank()) {
            List<TransactionEntity> result = transactionRepository
                    .findByTidAndTransactionTimeBetween(tid, windowStart, windowEnd);
            if (!result.isEmpty()) {
                log.info("[SEQUENTIAL_TXN] window transactions found by tid={}, count={}", tid, result.size());
                return result;
            }
        }

        log.info("[SEQUENTIAL_TXN] no window transactions found for any identifier");
        return Collections.emptyList();
    }

    // ── Safe transaction list builder ─────────────────────────────────────────

    /**
     * Builds a list of safe transaction identifiers for the explanation response.
     * Does NOT expose full PAN, track2Data, or card number — only last 4 digits.
     * Format: "txnId=12, rrn=HF101, stan=220101, last4=3455, time=2026-06-01T10:20:00"
     */
    private List<String> buildSafeTxnList(List<TransactionEntity> txns) {
        if (txns == null || txns.isEmpty()) return Collections.emptyList();
        return txns.stream()
                .map(t -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("txnId=").append(t.getTransactionId());
                    if (t.getRrn() != null) sb.append(", rrn=").append(t.getRrn());
                    if (t.getStan() != null) sb.append(", stan=").append(t.getStan());
                    String last4 = extractLast4(t.getTrack2Data());
                    if (last4 != null) sb.append(", last4=").append(last4);
                    if (t.getTransactionTime() != null) sb.append(", time=").append(t.getTransactionTime());
                    return sb.toString();
                })
                .collect(Collectors.toList());
    }

    /** Extracts the last 4 digits from a track2Data / card number string (digits only). */
    private String extractLast4(String track2Data) {
        if (track2Data == null || track2Data.isBlank()) return null;
        String digits = track2Data.replaceAll("[^0-9]", "");
        return digits.length() >= 4 ? digits.substring(digits.length() - 4) : null;
    }

    // ── Execution trace builder ────────────────────────────────────────────────

    private List<ExecutionTraceStepResponse> buildExecutionTrace(
            String ruleType,
            TestInputData inputData,
            RuleEntity rule,
            RuleEvaluationExplanationResponse explanation,
            RuleAction actualAction) {

        List<ExecutionTraceStepResponse> trace = new ArrayList<>();
        int s = 1;

        trace.add(ts(s++, "Loaded test case input", buildInputDetail(inputData), "SUCCESS", ruleType));
        trace.add(ts(s++, "Loaded active rule", buildRuleDetail(rule), "SUCCESS", ruleType));
        trace.add(ts(s++, "Identified rule type", "Rule type = " + ruleType, "SUCCESS", ruleType));

        if ("SINGLE_LARGE_TX".equals(ruleType) || "DAILY_LIMIT".equals(ruleType)
                || "DAILY_TXN_VALUE".equals(ruleType) || "MONTHLY_VOLUME".equals(ruleType)
                || "ANNUAL_VOLUME".equals(ruleType) || "ANNUAL_TXN_VOLUME".equals(ruleType)
                || "MONTHLY_TXN_VOLUME".equals(ruleType)) {
            s = addAmountThresholdTrace(trace, s, ruleType, inputData, rule);
        } else if ("HIGH_FREQ_TXN".equals(ruleType)) {
            s = addHighFreqTrace(trace, s, ruleType, inputData, rule, explanation);
        } else if ("SEQUENTIAL_TXN".equals(ruleType)) {
            s = addSequentialTrace(trace, s, ruleType, inputData, rule, explanation);
        } else if ("UNUSUAL_AMT".equals(ruleType) || "UNUSUAL_AMOUNT".equals(ruleType)) {
            s = addUnusualAmtTrace(trace, s, ruleType, inputData, rule, explanation);
        } else if ("STRUCTURING".equals(ruleType)) {
            s = addStructuringTrace(trace, s, ruleType, inputData, rule);
        } else {
            s = addDefaultTrace(trace, s, ruleType, rule, explanation);
        }

        trace.add(ts(s, "Calculated actual action",
                "Final action = " + actualAction.name(), "INFO", ruleType));

        return trace;
    }

    private int addAmountThresholdTrace(List<ExecutionTraceStepResponse> trace, int s,
            String ruleType, TestInputData inputData, RuleEntity rule) {
        BigDecimal maxAmt = rule.getMaxAmount();
        BigDecimal inputAmt = inputData != null ? inputData.getAmount() : null;

        trace.add(ts(s++, "Loaded amount threshold",
                "maxAmount = " + (maxAmt != null ? maxAmt.toPlainString() : "not configured"),
                "SUCCESS", ruleType));

        String detail;
        if (maxAmt != null && inputAmt != null) {
            boolean triggered = inputAmt.compareTo(maxAmt) > 0;
            detail = "Transaction amount " + inputAmt.toPlainString()
                    + (triggered
                    ? " > maxAmount " + maxAmt.toPlainString() + ", rule triggered"
                    : " <= maxAmount " + maxAmt.toPlainString() + ", rule did not trigger");
        } else {
            detail = "Comparison skipped — maxAmount or input amount is null";
        }
        trace.add(ts(s++, "Compared amount with threshold", detail, "INFO", ruleType));
        return s;
    }

    private int addHighFreqTrace(List<ExecutionTraceStepResponse> trace, int s,
            String ruleType, TestInputData inputData, RuleEntity rule,
            RuleEvaluationExplanationResponse expl) {
        int windowHours = (rule.getFrequencyHours() != null && rule.getFrequencyHours() > 0)
                ? rule.getFrequencyHours() : 1;
        Integer txnCount = rule.getTxnCount();

        trace.add(ts(s++, "Loaded frequency configuration",
                "txnCount threshold = " + (txnCount != null ? txnCount : "not configured")
                        + ", window = last " + windowHours + " hour(s)",
                "SUCCESS", ruleType));

        trace.add(ts(s++, "Searching historical transactions",
                "Searching successful transactions for " + cardIdentifier(inputData)
                        + " in last " + windowHours + " hour(s)",
                "SUCCESS", ruleType));

        if (expl != null) {
            int historical = expl.getHistoricalCount() != null ? expl.getHistoricalCount() : 0;
            trace.add(ts(s++, "Historical transaction count",
                    "Found " + historical + " historical successful transaction(s)", "INFO", ruleType));

            int total = expl.getMatchedCount() != null ? expl.getMatchedCount() : historical + 1;
            String required = txnCount != null ? String.valueOf(txnCount) : "not configured";
            Boolean triggered = expl.getTriggered();
            String detail = "Total count = " + total
                    + " (historical=" + historical + " + current=1), required=" + required
                    + (triggered != null ? (triggered ? " → TRIGGERED" : " → not triggered") : "");
            trace.add(ts(s++, "Total matched count", detail, "INFO", ruleType));
        }
        return s;
    }

    private int addSequentialTrace(List<ExecutionTraceStepResponse> trace, int s,
            String ruleType, TestInputData inputData, RuleEntity rule,
            RuleEvaluationExplanationResponse expl) {
        int windowHours = (rule.getFrequencyHours() != null && rule.getFrequencyHours() > 0)
                ? rule.getFrequencyHours() : 1;
        Integer txnCount = rule.getTxnCount();

        trace.add(ts(s++, "Loaded sequential rule configuration",
                "txnCount threshold = " + (txnCount != null ? txnCount : "not configured")
                        + ", window = last " + windowHours + " hour(s)",
                "SUCCESS", ruleType));

        trace.add(ts(s++, "Searching sequential transactions",
                "Searching transactions for " + cardIdentifier(inputData)
                        + " in last " + windowHours + " hour(s)",
                "SUCCESS", ruleType));

        if (expl != null) {
            int historical = expl.getHistoricalCount() != null ? expl.getHistoricalCount() : 0;
            trace.add(ts(s++, "Historical transaction count",
                    "Found " + historical + " historical successful transaction(s)", "INFO", ruleType));

            int total = expl.getMatchedCount() != null ? expl.getMatchedCount() : historical + 1;
            String required = txnCount != null ? String.valueOf(txnCount) : "not configured";
            Boolean triggered = expl.getTriggered();
            String detail = "Total count = " + total
                    + " (historical=" + historical + " + current=1), required=" + required
                    + " (uses >= comparison)"
                    + (triggered != null ? (triggered ? " → TRIGGERED" : " → not triggered") : "");
            trace.add(ts(s++, "Total matched count", detail, "INFO", ruleType));
        }
        return s;
    }

    private int addUnusualAmtTrace(List<ExecutionTraceStepResponse> trace, int s,
            String ruleType, TestInputData inputData, RuleEntity rule,
            RuleEvaluationExplanationResponse expl) {

        trace.add(ts(s++, "Searching baseline transactions",
                "Looking up historical transactions for " + cardIdentifier(inputData)
                        + " to compute spending baseline",
                "SUCCESS", ruleType));

        if (expl == null || expl.getHistoricalCount() == null || expl.getHistoricalCount() == 0) {
            trace.add(ts(s++, "Baseline result",
                    "No historical transactions found — unusual amount rule cannot evaluate without baseline",
                    "WARNING", ruleType));
        } else {
            int historical = expl.getHistoricalCount();
            trace.add(ts(s++, "Baseline transaction count",
                    "Found " + historical + " historical transaction(s) for baseline", "INFO", ruleType));

            if (expl.getAverageAmount() != null) {
                trace.add(ts(s++, "Calculated baseline average",
                        "Baseline average amount = " + expl.getAverageAmount().toPlainString(),
                        "INFO", ruleType));
            }

            if (expl.getThresholdAmount() != null) {
                String pct = rule.getPercentageThreshold() != null
                        ? rule.getPercentageThreshold().toPlainString() + "%" : "N/A";
                trace.add(ts(s++, "Calculated deviation threshold",
                        "Threshold = " + expl.getThresholdAmount().toPlainString()
                                + " (average + " + pct + " deviation)",
                        "INFO", ruleType));
            }

            if (inputData != null && inputData.getAmount() != null && expl.getThresholdAmount() != null) {
                BigDecimal amt = inputData.getAmount();
                boolean exceeded = amt.compareTo(expl.getThresholdAmount()) > 0;
                trace.add(ts(s++, "Compared amount with unusual threshold",
                        "Current amount " + amt.toPlainString()
                                + (exceeded
                                ? " > threshold " + expl.getThresholdAmount().toPlainString() + ", rule triggered"
                                : " <= threshold " + expl.getThresholdAmount().toPlainString() + ", within normal range"),
                        "INFO", ruleType));
            }
        }
        return s;
    }

    private int addStructuringTrace(List<ExecutionTraceStepResponse> trace, int s,
            String ruleType, TestInputData inputData, RuleEntity rule) {
        BigDecimal txnAmount = rule.getTxnAmount();

        trace.add(ts(s++, "Loaded structuring threshold",
                "txnAmount = " + (txnAmount != null ? txnAmount.toPlainString() : "not configured")
                        + " — transactions below this value trigger the rule",
                "SUCCESS", ruleType));

        if (txnAmount != null && inputData != null && inputData.getAmount() != null) {
            BigDecimal amt = inputData.getAmount();
            boolean below = amt.compareTo(txnAmount) < 0;
            trace.add(ts(s++, "Compared amount with structuring threshold",
                    "Transaction amount " + amt.toPlainString()
                            + (below
                            ? " < txnAmount " + txnAmount.toPlainString() + ", rule triggered (structuring pattern detected)"
                            : " >= txnAmount " + txnAmount.toPlainString() + ", rule did not trigger"),
                    "INFO", ruleType));
        } else {
            trace.add(ts(s++, "Compared amount with structuring threshold",
                    "Comparison not possible — txnAmount or input amount is null", "WARNING", ruleType));
        }
        return s;
    }

    private int addDefaultTrace(List<ExecutionTraceStepResponse> trace, int s,
            String ruleType, RuleEntity rule, RuleEvaluationExplanationResponse expl) {
        StringBuilder detail = new StringBuilder("Evaluating rule type '").append(ruleType)
                .append("' using default threshold comparison");
        if (rule.getMaxAmount() != null) detail.append(", maxAmount=").append(rule.getMaxAmount().toPlainString());
        if (rule.getTxnAmount() != null) detail.append(", txnAmount=").append(rule.getTxnAmount().toPlainString());
        if (expl != null && expl.getRuleReason() != null) detail.append(". ").append(expl.getRuleReason());
        trace.add(ts(s++, "Evaluated rule condition", detail.toString(), "INFO", ruleType));
        return s;
    }

    // ── Trace step factory and formatting helpers ──────────────────────────────

    private ExecutionTraceStepResponse ts(int stepNumber, String title, String detail,
            String status, String ruleType) {
        ExecutionTraceStepResponse step = new ExecutionTraceStepResponse();
        step.setStepNumber(stepNumber);
        step.setTitle(title);
        step.setDetail(detail != null ? detail : "");
        step.setStatus(status);
        step.setRuleType(ruleType);
        step.setTimestamp(LocalDateTime.now().toString());
        return step;
    }

    private String buildInputDetail(TestInputData inputData) {
        if (inputData == null) return "No input data";
        StringBuilder sb = new StringBuilder();
        if (inputData.getAmount() != null) sb.append("amount=").append(inputData.getAmount().toPlainString());
        String card = inputData.getCardNumber() != null ? inputData.getCardNumber() : inputData.getTrack2Data();
        if (card != null && !card.isBlank()) {
            sb.append(", card=").append(maskCardForTrace(card));
        }
        if (inputData.getSerialNumber() != null && !inputData.getSerialNumber().isBlank()) {
            sb.append(", serialNumber=").append(inputData.getSerialNumber());
        }
        if (inputData.getMid() != null) sb.append(", MID=").append(inputData.getMid());
        sb.append(", currency=").append(inputData.getCurrency() != null ? inputData.getCurrency() : "INR");
        return sb.toString();
    }

    private String buildRuleDetail(RuleEntity rule) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rule=").append(rule.getRuleName());
        sb.append(", action=").append(rule.getAction() != null ? rule.getAction().name() : "null");
        if (rule.getMaxAmount() != null) sb.append(", maxAmount=").append(rule.getMaxAmount().toPlainString());
        if (rule.getTxnAmount() != null) sb.append(", txnAmount=").append(rule.getTxnAmount().toPlainString());
        if (rule.getTxnCount() != null) sb.append(", txnCount=").append(rule.getTxnCount());
        if (rule.getFrequencyHours() != null) sb.append(", frequency=").append(rule.getFrequencyHours()).append(" hour(s)");
        if (rule.getPercentageThreshold() != null)
            sb.append(", percentageThreshold=").append(rule.getPercentageThreshold()).append("%");
        return sb.toString();
    }

    private String cardIdentifier(TestInputData inputData) {
        if (inputData == null) return "no identifier";
        if (inputData.getSerialNumber() != null && !inputData.getSerialNumber().isBlank()) {
            return "serialNumber=" + inputData.getSerialNumber();
        }
        String card = inputData.getCardNumber() != null ? inputData.getCardNumber() : inputData.getTrack2Data();
        if (card != null && !card.isBlank()) {
            String digits = card.replaceAll("[^0-9]", "");
            if (digits.length() >= 4) return "card ending " + digits.substring(digits.length() - 4);
        }
        return "no card identifier";
    }

    private String maskCardForTrace(String card) {
        if (card == null) return "****";
        String digits = card.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) return "****" + digits.substring(digits.length() - 4);
        return "****";
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<String> buildAlertCodes(RuleEntity rule, RuleAction actualAction) {
        if (actualAction == RuleAction.ACCEPT) {
            return Collections.emptyList();
        }
        return List.of(rule.getRuleType() + "_" + actualAction.name());
    }

    // Fixed risk scores per action: ACCEPT=0, MONITOR=60, REJECT=90
    private BigDecimal buildRiskScore(RuleAction actualAction) {
        return switch (actualAction) {
            case ACCEPT -> BigDecimal.ZERO;
            case MONITOR -> BigDecimal.valueOf(60);
            case REJECT -> BigDecimal.valueOf(90);
        };
    }
}