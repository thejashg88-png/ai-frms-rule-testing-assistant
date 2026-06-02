package com.thejas.ai_frms.execution.service;

import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.execution.dto.ComparisonResult;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.testcase.dto.TestInputData;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import com.thejas.ai_frms.transaction.entity.TransactionEntity;
import com.thejas.ai_frms.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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

        RuleAction actualAction;
        String engineNote = null;

        String ruleTypeUpper = ruleType != null ? ruleType.toUpperCase() : "";

        if (ruleTypeUpper.equals("UNUSUAL_AMT") || ruleTypeUpper.equals("UNUSUAL_AMOUNT")) {
            UnusualAmtResult r = evaluateUnusualAmountRule(rule, inputData);
            actualAction = r.action();
            engineNote = r.note();
        } else if (ruleTypeUpper.equals("SEQUENTIAL_TXN")) {
            SequentialTxnResult r = evaluateSequentialTransactionRule(rule, inputData);
            actualAction = r.action();
            engineNote = r.note();
        } else {
            actualAction = evaluateRule(rule, inputData);
        }

        log.info("[EXECUTION] Actual action={}", actualAction);

        ComparisonResult result = new ComparisonResult();
        result.setActualAction(actualAction);
        result.setActualEvaluationStatus(actualAction.name());
        result.setActualRuleType(ruleType);
        result.setActualAlertCodes(buildAlertCodes(rule, actualAction));
        result.setActualRiskScore(buildRiskScore(actualAction));
        result.setRuleType(ruleType);
        result.setInputAmount(amount);
        result.setEngineNote(engineNote);

        return result;
    }

    private RuleAction evaluateRule(RuleEntity rule, TestInputData inputData) {
        if (rule == null || inputData == null || inputData.getAmount() == null) {
            return RuleAction.ACCEPT;
        }

        String ruleType = rule.getRuleType();
        BigDecimal amount = inputData.getAmount();

        if (ruleType == null || ruleType.isBlank()) {
            return RuleAction.ACCEPT;
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

    // Used by SINGLE_LARGE_TX, DAILY_LIMIT, MONTHLY_VOLUME, ANNUAL_VOLUME, ANNUAL_TXN_VOLUME, MONTHLY_TXN_VOLUME
    private RuleAction evaluateAmountGreaterThanRule(RuleEntity rule, BigDecimal amount) {
        if (rule.getMaxAmount() != null && amount.compareTo(rule.getMaxAmount()) > 0) {
            return rule.getAction();
        }
        return RuleAction.ACCEPT;
    }

    // ── HIGH_FREQ_TXN ──────────────────────────────────────────────────────────

    private RuleAction evaluateHighFrequencyRule(RuleEntity rule, TestInputData inputData) {
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
        long successfulCount = windowTransactions.stream()
                .filter(t -> "SUCCESS".equalsIgnoreCase(t.getTransactionStatus())
                        || "00".equals(t.getResponseCode()))
                .count();

        // Include the current test transaction in the count (+1)
        long totalCount = successfulCount + 1;

        log.info("[HIGH_FREQ_TXN] matched historical transaction count={} (successful={})",
                windowTransactions.size(), successfulCount);
        log.info("[HIGH_FREQ_TXN] total count (historical + current)={}", totalCount);

        int threshold = (txnCountThreshold != null && txnCountThreshold > 0) ? txnCountThreshold : Integer.MAX_VALUE;

        if (totalCount > threshold) {
            log.info("[HIGH_FREQ_TXN] totalCount {} > threshold {} → actualAction={}", totalCount, threshold, rule.getAction());
            return rule.getAction();
        }

        log.info("[HIGH_FREQ_TXN] totalCount {} <= threshold {} → actualAction=ACCEPT", totalCount, threshold);
        return RuleAction.ACCEPT;
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

    // STRUCTURING triggers when amount is BELOW txnAmount (opposite of most other rules)
    private RuleAction evaluateStructuringRule(RuleEntity rule, BigDecimal amount) {
        if (rule.getTxnAmount() != null && amount.compareTo(rule.getTxnAmount()) < 0) {
            return rule.getAction();
        }
        return RuleAction.ACCEPT;
    }

    private RuleAction evaluateDefaultRule(RuleEntity rule, BigDecimal amount) {
        if (rule.getMaxAmount() != null && amount.compareTo(rule.getMaxAmount()) > 0) {
            return rule.getAction();
        }
        if (rule.getTxnAmount() != null && amount.compareTo(rule.getTxnAmount()) < 0) {
            return rule.getAction();
        }
        return RuleAction.ACCEPT;
    }

    // ── UNUSUAL_AMT ────────────────────────────────────────────────────────────

    private record UnusualAmtResult(RuleAction action, String note) {}

    private UnusualAmtResult evaluateUnusualAmountRule(RuleEntity rule, TestInputData inputData) {
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

        if (amount == null) {
            return new UnusualAmtResult(RuleAction.ACCEPT, "Input amount is null — cannot evaluate UNUSUAL_AMT");
        }

        List<TransactionEntity> baseline = fetchBaselineTransactions(cardNumber, serialNumber, track2Data);

        log.info("[UNUSUAL_AMT] baseline transactions count={}", baseline.size());

        if (baseline.isEmpty()) {
            String note = "No baseline transaction history found for unusual amount evaluation";
            log.info("[UNUSUAL_AMT] no baseline found → actualAction=ACCEPT");
            return new UnusualAmtResult(RuleAction.ACCEPT, note);
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

        if (amount.compareTo(threshold) > 0) {
            log.info("[UNUSUAL_AMT] amount {} > threshold {} → actualAction={}", amount, threshold, rule.getAction());
            return new UnusualAmtResult(rule.getAction(), null);
        } else {
            String note = "Current amount " + amount + " did not exceed unusual amount threshold "
                    + threshold + " (baseline avg=" + avg + ", percentageThreshold=" + percentageThreshold + "%)";
            log.info("[UNUSUAL_AMT] amount {} <= threshold {} → actualAction=ACCEPT", amount, threshold);
            return new UnusualAmtResult(RuleAction.ACCEPT, note);
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

    private record SequentialTxnResult(RuleAction action, String note) {}

    private SequentialTxnResult evaluateSequentialTransactionRule(RuleEntity rule, TestInputData inputData) {
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

        // Use >= : txnCount is the minimum count that triggers the rule
        if (totalCount >= threshold) {
            log.info("[SEQUENTIAL_TXN] totalCount {} >= threshold {} → actualAction={}",
                    totalCount, threshold, rule.getAction());
            return new SequentialTxnResult(rule.getAction(), null);
        }

        String note = "Sequential transaction threshold not reached. Matched count="
                + totalCount + " (historical=" + historicalCount + " + current=1), required=" + threshold;
        log.info("[SEQUENTIAL_TXN] totalCount {} < threshold {} → actualAction=ACCEPT", totalCount, threshold);
        return new SequentialTxnResult(RuleAction.ACCEPT, note);
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