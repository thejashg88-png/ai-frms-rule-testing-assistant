package com.thejas.ai_frms.transaction.service;

import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.common.util.JsonUtil;
import com.thejas.ai_frms.common.util.MaskingUtil;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.testcase.dto.TestInputData;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import com.thejas.ai_frms.testcase.repository.TestCaseRepository;
import com.thejas.ai_frms.transaction.dto.GenerateHistoryRequest;
import com.thejas.ai_frms.transaction.dto.GenerateHistoryResponse;
import com.thejas.ai_frms.transaction.dto.GeneratedTransactionInfo;
import com.thejas.ai_frms.transaction.entity.TransactionEntity;
import com.thejas.ai_frms.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Generates synthetic historical transactions for rule types that require prior
 * transaction history to trigger during test case execution.
 *
 * Supported rule types: HIGH_FREQ_TXN, SEQUENTIAL_TXN, STRUCTURING, UNUSUAL_AMT,
 *                       INCONSISTENT_MCC, ROUND_AMT_TXN, TXN_VELOCITY
 *
 * All other rule types (SINGLE_LARGE_TX, DAILY_LIMIT, etc.) are amount-based and
 * do not use transaction history — this generator returns early for those.
 *
 * Identifier strategy for generated transactions:
 *   - serialNumber is set to match the test case inputData so the execution engine
 *     finds history via findBySerialNumberAndTransactionTimeBetween (primary lookup)
 *   - track2Data is set to the normalised cardNumber so the fallback lookup path
 *     (findByTrack2DataAndTransactionTimeBetween) also works
 *
 * Duplicate-guard: before generating, the existing successful-transaction count within
 * the relevant window (or all-time for UNUSUAL_AMT) is checked. If already sufficient,
 * generation is skipped unless request.force = true.
 */
@Service
public class TransactionHistoryGeneratorServiceImpl implements TransactionHistoryGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(TransactionHistoryGeneratorServiceImpl.class);

    private static final Set<String> HISTORY_NEEDED = Set.of(
            "HIGH_FREQ_TXN", "TXN_VELOCITY", "SEQUENTIAL_TXN", "STRUCTURING",
            "UNUSUAL_AMT", "INCONSISTENT_MCC", "ROUND_AMT_TXN"
    );

    private final TransactionRepository transactionRepository;
    private final TestCaseRepository testCaseRepository;

    public TransactionHistoryGeneratorServiceImpl(
            TransactionRepository transactionRepository,
            TestCaseRepository testCaseRepository) {
        this.transactionRepository = transactionRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional
    public GenerateHistoryResponse generateHistory(GenerateHistoryRequest request) {
        if (request.getTestCaseId() == null) {
            throw new BadRequestException("testCaseId is required");
        }

        TestCaseEntity testCase = testCaseRepository.findById(request.getTestCaseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test case not found: " + request.getTestCaseId()));

        if (testCase.getScenario() == null || testCase.getScenario().getRule() == null) {
            throw new BadRequestException(
                    "Test case has no linked scenario or rule — cannot derive rule type");
        }

        RuleEntity rule = testCase.getScenario().getRule();
        String ruleType = rule.getRuleType() != null ? rule.getRuleType().toUpperCase() : "";

        log.info("[HISTORY GENERATOR] testCaseId={}", request.getTestCaseId());
        log.info("[HISTORY GENERATOR] ruleType={}", ruleType);

        if (!HISTORY_NEEDED.contains(ruleType)) {
            return buildResponse(ruleType, request.getTestCaseId(), 0,
                    "This rule type does not require generated historical transactions.",
                    Collections.emptyList());
        }

        TestInputData inputData = JsonUtil.fromJson(testCase.getInputDataJson(), TestInputData.class);

        String serialNumber = resolveStr(request.getSerialNumber(), inputData.getSerialNumber(),
                "SN_HISTORY_" + request.getTestCaseId());
        String cardNumber = resolveCard(request.getCardNumber(), inputData);
        String mid = resolveStr(request.getMid(), inputData.getMid(), "MID_HISTORY_001");
        String tid = resolveStr(request.getTid(), inputData.getTid(), "TID_HISTORY_001");
        String mccCode = resolveStr(request.getMccCode(), inputData.getMccCode(), "5999");
        String currency = resolveStr(request.getCurrency(), inputData.getCurrency(), "INR");

        log.info("[HISTORY GENERATOR] card={}", MaskingUtil.maskCardNumber(cardNumber != null ? cardNumber : ""));
        log.info("[HISTORY GENERATOR] serialNumber={}", serialNumber);

        return switch (ruleType) {
            case "HIGH_FREQ_TXN", "TXN_VELOCITY" ->
                    generateWindowCountHistory(request, rule, ruleType,
                            serialNumber, cardNumber, mid, tid, mccCode, currency);
            case "SEQUENTIAL_TXN" ->
                    generateWindowCountHistory(request, rule, ruleType,
                            serialNumber, cardNumber, mid, tid, mccCode, currency);
            case "STRUCTURING" ->
                    generateStructuringHistory(request, rule,
                            serialNumber, cardNumber, mid, tid, mccCode, currency);
            case "UNUSUAL_AMT" ->
                    generateUnusualAmtHistory(request, rule,
                            serialNumber, cardNumber, mid, tid, mccCode, currency);
            case "INCONSISTENT_MCC" ->
                    generateInconsistentMccHistory(request, rule,
                            serialNumber, cardNumber, mid, tid, currency);
            case "ROUND_AMT_TXN" ->
                    generateRoundAmtHistory(request, rule,
                            serialNumber, cardNumber, mid, tid, mccCode, currency);
            default -> buildResponse(ruleType, request.getTestCaseId(), 0,
                    "This rule type does not require generated historical transactions.",
                    Collections.emptyList());
        };
    }

    // ── HIGH_FREQ_TXN / TXN_VELOCITY / SEQUENTIAL_TXN ────────────────────────

    private GenerateHistoryResponse generateWindowCountHistory(
            GenerateHistoryRequest request,
            RuleEntity rule,
            String ruleType,
            String serialNumber,
            String cardNumber,
            String mid,
            String tid,
            String mccCode,
            String currency) {

        int requiredCount = resolveCount(request.getNumberOfTransactions(), rule.getTxnCount(), 5);
        int windowHours = resolveWindowHours(rule.getFrequencyHours(), 1);
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusHours(windowHours);

        log.info("[HISTORY GENERATOR] requiredHistoryCount={}", requiredCount);

        if (!Boolean.TRUE.equals(request.getForce())) {
            long existing = countWindowSuccess(serialNumber, cardNumber, windowStart, windowEnd);
            log.info("[HISTORY GENERATOR] existingHistoryCount={}", existing);
            if (existing >= requiredCount) {
                return buildResponse(ruleType, request.getTestCaseId(), 0,
                        "Required history already exists", Collections.emptyList());
            }
        }

        long baseTs = System.currentTimeMillis();
        long intervalMinutes = Math.max(1, (long) windowHours * 60 / (requiredCount + 1));
        List<TransactionEntity> toSave = new ArrayList<>();

        for (int i = 0; i < requiredCount; i++) {
            LocalDateTime txnTime = windowEnd.minusMinutes(intervalMinutes * (requiredCount - i));
            BigDecimal amount = BigDecimal.valueOf(10000L + (long) i * 1000);
            toSave.add(buildSuccessEntity(
                    generateRrn(baseTs, i), generateStan(baseTs, i),
                    serialNumber, cardNumber, mid, tid, mccCode, amount, currency, txnTime));
        }

        List<TransactionEntity> saved = transactionRepository.saveAll(toSave);
        log.info("[HISTORY GENERATOR] generatedCount={}", saved.size());

        return buildResponse(ruleType, request.getTestCaseId(), saved.size(),
                "Generated required history for " + ruleType,
                saved.stream().map(this::toInfo).toList());
    }

    // ── STRUCTURING ───────────────────────────────────────────────────────────

    private GenerateHistoryResponse generateStructuringHistory(
            GenerateHistoryRequest request,
            RuleEntity rule,
            String serialNumber,
            String cardNumber,
            String mid,
            String tid,
            String mccCode,
            String currency) {

        String ruleType = rule.getRuleType();
        int requiredCount = resolveCount(request.getNumberOfTransactions(), rule.getTxnCount(), 4);
        int windowHours = resolveWindowHours(rule.getFrequencyHours(), 1);
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusHours(windowHours);

        log.info("[HISTORY GENERATOR] requiredHistoryCount={}", requiredCount);

        if (!Boolean.TRUE.equals(request.getForce())) {
            long existing = countWindowSuccess(serialNumber, cardNumber, windowStart, windowEnd);
            log.info("[HISTORY GENERATOR] existingHistoryCount={}", existing);
            if (existing >= requiredCount) {
                return buildResponse(ruleType, request.getTestCaseId(), 0,
                        "Required history already exists", Collections.emptyList());
            }
        }

        // Generate amounts below the structuring threshold
        BigDecimal threshold = rule.getTxnAmount() != null
                ? rule.getTxnAmount()
                : BigDecimal.valueOf(50000);

        BigDecimal[] amounts = {
                threshold.multiply(BigDecimal.valueOf(0.60)).setScale(2, RoundingMode.HALF_UP),
                threshold.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP),
                threshold.multiply(BigDecimal.valueOf(0.40)).setScale(2, RoundingMode.HALF_UP),
                threshold.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP),
                threshold.multiply(BigDecimal.valueOf(0.25)).setScale(2, RoundingMode.HALF_UP)
        };

        long baseTs = System.currentTimeMillis();
        long intervalMinutes = Math.max(1, (long) windowHours * 60 / (requiredCount + 1));
        List<TransactionEntity> toSave = new ArrayList<>();

        for (int i = 0; i < requiredCount; i++) {
            LocalDateTime txnTime = windowEnd.minusMinutes(intervalMinutes * (requiredCount - i));
            toSave.add(buildSuccessEntity(
                    generateRrn(baseTs, i), generateStan(baseTs, i),
                    serialNumber, cardNumber, mid, tid, mccCode,
                    amounts[i % amounts.length], currency, txnTime));
        }

        List<TransactionEntity> saved = transactionRepository.saveAll(toSave);
        log.info("[HISTORY GENERATOR] generatedCount={}", saved.size());

        return buildResponse(ruleType, request.getTestCaseId(), saved.size(),
                "Generated required history for STRUCTURING",
                saved.stream().map(this::toInfo).toList());
    }

    // ── UNUSUAL_AMT ───────────────────────────────────────────────────────────

    private GenerateHistoryResponse generateUnusualAmtHistory(
            GenerateHistoryRequest request,
            RuleEntity rule,
            String serialNumber,
            String cardNumber,
            String mid,
            String tid,
            String mccCode,
            String currency) {

        String ruleType = rule.getRuleType();
        int requiredCount = resolveCount(request.getNumberOfTransactions(), null, 3);

        log.info("[HISTORY GENERATOR] requiredHistoryCount={}", requiredCount);

        if (!Boolean.TRUE.equals(request.getForce())) {
            List<TransactionEntity> existing = findBaseline(serialNumber, cardNumber);
            log.info("[HISTORY GENERATOR] existingHistoryCount={}", existing.size());
            if (existing.size() >= requiredCount) {
                return buildResponse(ruleType, request.getTestCaseId(), 0,
                        "Required history already exists", Collections.emptyList());
            }
        }

        // Low baseline amounts so the current test case amount appears unusually high
        BigDecimal[] amounts = {
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(12000),
                BigDecimal.valueOf(9000),
                BigDecimal.valueOf(11000),
                BigDecimal.valueOf(10500)
        };

        long baseTs = System.currentTimeMillis();
        List<TransactionEntity> toSave = new ArrayList<>();

        for (int i = 0; i < requiredCount; i++) {
            // Spread over the past 24 hours so UNUSUAL_AMT baseline includes them
            LocalDateTime txnTime = LocalDateTime.now().minusHours(24 - (long) i * 6);
            toSave.add(buildSuccessEntity(
                    generateRrn(baseTs, i), generateStan(baseTs, i),
                    serialNumber, cardNumber, mid, tid, mccCode,
                    amounts[i % amounts.length], currency, txnTime));
        }

        List<TransactionEntity> saved = transactionRepository.saveAll(toSave);
        log.info("[HISTORY GENERATOR] generatedCount={}", saved.size());

        return buildResponse(ruleType, request.getTestCaseId(), saved.size(),
                "Generated required history for UNUSUAL_AMT",
                saved.stream().map(this::toInfo).toList());
    }

    // ── INCONSISTENT_MCC ─────────────────────────────────────────────────────

    private GenerateHistoryResponse generateInconsistentMccHistory(
            GenerateHistoryRequest request,
            RuleEntity rule,
            String serialNumber,
            String cardNumber,
            String mid,
            String tid,
            String currency) {

        String ruleType = rule.getRuleType();
        String[] mccs = {"5411", "5812", "7399", "4111", "5999"};
        int requiredCount = resolveCount(request.getNumberOfTransactions(), null, 3);
        int windowHours = resolveWindowHours(rule.getFrequencyHours(), 1);
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusHours(windowHours);

        log.info("[HISTORY GENERATOR] requiredHistoryCount={}", requiredCount);

        if (!Boolean.TRUE.equals(request.getForce())) {
            long existing = countWindowSuccess(serialNumber, cardNumber, windowStart, windowEnd);
            log.info("[HISTORY GENERATOR] existingHistoryCount={}", existing);
            if (existing >= requiredCount) {
                return buildResponse(ruleType, request.getTestCaseId(), 0,
                        "Required history already exists", Collections.emptyList());
            }
        }

        long baseTs = System.currentTimeMillis();
        long intervalMinutes = Math.max(1, (long) windowHours * 60 / (requiredCount + 1));
        List<TransactionEntity> toSave = new ArrayList<>();

        for (int i = 0; i < requiredCount; i++) {
            LocalDateTime txnTime = windowEnd.minusMinutes(intervalMinutes * (requiredCount - i));
            String mcc = mccs[i % mccs.length];
            toSave.add(buildSuccessEntity(
                    generateRrn(baseTs, i), generateStan(baseTs, i),
                    serialNumber, cardNumber, mid, tid, mcc,
                    BigDecimal.valueOf(15000), currency, txnTime));
        }

        List<TransactionEntity> saved = transactionRepository.saveAll(toSave);
        log.info("[HISTORY GENERATOR] generatedCount={}", saved.size());

        return buildResponse(ruleType, request.getTestCaseId(), saved.size(),
                "Generated required history for INCONSISTENT_MCC",
                saved.stream().map(this::toInfo).toList());
    }

    // ── ROUND_AMT_TXN ─────────────────────────────────────────────────────────

    private GenerateHistoryResponse generateRoundAmtHistory(
            GenerateHistoryRequest request,
            RuleEntity rule,
            String serialNumber,
            String cardNumber,
            String mid,
            String tid,
            String mccCode,
            String currency) {

        String ruleType = rule.getRuleType();
        int requiredCount = resolveCount(request.getNumberOfTransactions(), rule.getTxnCount(), 5);
        int windowHours = resolveWindowHours(rule.getFrequencyHours(), 1);
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusHours(windowHours);

        log.info("[HISTORY GENERATOR] requiredHistoryCount={}", requiredCount);

        if (!Boolean.TRUE.equals(request.getForce())) {
            long existing = countWindowSuccess(serialNumber, cardNumber, windowStart, windowEnd);
            log.info("[HISTORY GENERATOR] existingHistoryCount={}", existing);
            if (existing >= requiredCount) {
                return buildResponse(ruleType, request.getTestCaseId(), 0,
                        "Required history already exists", Collections.emptyList());
            }
        }

        BigDecimal[] roundAmounts = {
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(40000),
                BigDecimal.valueOf(50000)
        };

        long baseTs = System.currentTimeMillis();
        long intervalMinutes = Math.max(1, (long) windowHours * 60 / (requiredCount + 1));
        List<TransactionEntity> toSave = new ArrayList<>();

        for (int i = 0; i < requiredCount; i++) {
            LocalDateTime txnTime = windowEnd.minusMinutes(intervalMinutes * (requiredCount - i));
            toSave.add(buildSuccessEntity(
                    generateRrn(baseTs, i), generateStan(baseTs, i),
                    serialNumber, cardNumber, mid, tid, mccCode,
                    roundAmounts[i % roundAmounts.length], currency, txnTime));
        }

        List<TransactionEntity> saved = transactionRepository.saveAll(toSave);
        log.info("[HISTORY GENERATOR] generatedCount={}", saved.size());

        return buildResponse(ruleType, request.getTestCaseId(), saved.size(),
                "Generated required history for ROUND_AMT_TXN",
                saved.stream().map(this::toInfo).toList());
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private long countWindowSuccess(String serialNumber, String cardNumber,
            LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (serialNumber != null && !serialNumber.isBlank()) {
            List<TransactionEntity> txns = transactionRepository
                    .findBySerialNumberAndTransactionTimeBetween(serialNumber, windowStart, windowEnd);
            long count = txns.stream()
                    .filter(t -> "SUCCESS".equalsIgnoreCase(t.getTransactionStatus())
                            || "00".equals(t.getResponseCode()))
                    .count();
            if (count > 0) return count;
        }
        if (cardNumber != null && !cardNumber.isBlank()) {
            List<TransactionEntity> txns = transactionRepository
                    .findByTrack2DataAndTransactionTimeBetween(cardNumber, windowStart, windowEnd);
            return txns.stream()
                    .filter(t -> "SUCCESS".equalsIgnoreCase(t.getTransactionStatus())
                            || "00".equals(t.getResponseCode()))
                    .count();
        }
        return 0;
    }

    private List<TransactionEntity> findBaseline(String serialNumber, String cardNumber) {
        if (serialNumber != null && !serialNumber.isBlank()) {
            List<TransactionEntity> result = transactionRepository.findBySerialNumber(serialNumber);
            if (!result.isEmpty()) return result;
        }
        if (cardNumber != null && !cardNumber.isBlank()) {
            return transactionRepository.findByTrack2Data(cardNumber);
        }
        return Collections.emptyList();
    }

    private TransactionEntity buildSuccessEntity(
            String rrn,
            String stan,
            String serialNumber,
            String track2Data,
            String mid,
            String tid,
            String mccCode,
            BigDecimal amount,
            String currency,
            LocalDateTime txnTime) {

        TransactionEntity entity = new TransactionEntity();
        entity.setRrn(rrn);
        entity.setStan(stan);
        entity.setSerialNumber(serialNumber);
        entity.setTrack2Data(track2Data);
        entity.setMid(mid);
        entity.setTid(tid);
        entity.setMccCode(mccCode != null ? mccCode : "5999");
        entity.setAmount(amount);
        entity.setCurrency(currency != null ? currency : "INR");
        entity.setTransactionType("PURCHASE");
        entity.setResponseCode("00");
        entity.setResponseMessage("Approved");
        entity.setTransactionStatus("SUCCESS");
        entity.setTransactionTime(txnTime);
        entity.setCreatedBy("SYSTEM_HISTORY_GENERATOR");
        return entity;
    }

    private GeneratedTransactionInfo toInfo(TransactionEntity entity) {
        GeneratedTransactionInfo info = new GeneratedTransactionInfo();
        info.setTransactionId(entity.getTransactionId());
        info.setRrn(entity.getRrn());
        info.setStan(entity.getStan());
        info.setAmount(entity.getAmount());
        info.setMaskedCard(MaskingUtil.maskCardNumber(
                entity.getTrack2Data() != null ? entity.getTrack2Data() : ""));
        info.setSerialNumber(entity.getSerialNumber());
        info.setMccCode(entity.getMccCode());
        info.setTransactionTime(entity.getTransactionTime() != null
                ? entity.getTransactionTime().toString() : null);
        return info;
    }

    private GenerateHistoryResponse buildResponse(String ruleType, Long testCaseId,
            int count, String message, List<GeneratedTransactionInfo> txns) {
        GenerateHistoryResponse resp = new GenerateHistoryResponse();
        resp.setRuleType(ruleType);
        resp.setTestCaseId(testCaseId);
        resp.setGeneratedCount(count);
        resp.setMessage(message);
        resp.setGeneratedTransactions(txns);
        return resp;
    }

    private String resolveStr(String override, String fromInput, String defaultVal) {
        if (override != null && !override.isBlank()) return override;
        if (fromInput != null && !fromInput.isBlank()) return fromInput;
        return defaultVal;
    }

    private String resolveCard(String requestCard, TestInputData inputData) {
        String raw;
        if (requestCard != null && !requestCard.isBlank()) {
            raw = requestCard;
        } else if (inputData.getCardNumber() != null && !inputData.getCardNumber().isBlank()) {
            raw = inputData.getCardNumber();
        } else if (inputData.getTrack2Data() != null && !inputData.getTrack2Data().isBlank()) {
            return inputData.getTrack2Data();
        } else {
            return null;
        }
        return raw.replaceAll("[\\s\\-]", "");
    }

    private int resolveCount(Integer requestOverride, Integer ruleCount, int defaultCount) {
        if (requestOverride != null && requestOverride > 0) return requestOverride;
        if (ruleCount != null && ruleCount > 0) return ruleCount;
        return defaultCount;
    }

    private int resolveWindowHours(Integer ruleFrequency, int defaultHours) {
        return (ruleFrequency != null && ruleFrequency > 0) ? ruleFrequency : defaultHours;
    }

    // RRN: "HIST" + 9-digit timestamp suffix + 2-digit index = max 15 chars (well within 50)
    private String generateRrn(long baseTs, int index) {
        return "HIST" + String.valueOf(baseTs % 1_000_000_000L) + String.format("%02d", index % 100);
    }

    // STAN: 6-digit numeric, unique per transaction in a batch
    private String generateStan(long baseTs, int index) {
        return String.format("%06d", (int) ((baseTs + index) % 1_000_000L));
    }
}