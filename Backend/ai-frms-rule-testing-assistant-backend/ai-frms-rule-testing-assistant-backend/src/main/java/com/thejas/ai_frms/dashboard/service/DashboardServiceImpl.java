package com.thejas.ai_frms.dashboard.service;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.dashboard.dto.DashboardSummaryResponse;
import com.thejas.ai_frms.dashboard.dto.ExecutionTrendResponse;
import com.thejas.ai_frms.dashboard.dto.RuleWiseExecutionStats;
import com.thejas.ai_frms.execution.dto.ExecuteTestResponse;
import com.thejas.ai_frms.execution.entity.TestExecutionEntity;
import com.thejas.ai_frms.execution.mapper.TestExecutionMapper;
import com.thejas.ai_frms.execution.repository.TestExecutionRepository;
import com.thejas.ai_frms.execution.repository.TestExecutionResultRepository;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.rule.repository.RuleRepository;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
import com.thejas.ai_frms.testcase.repository.TestCaseRepository;
import com.thejas.ai_frms.transaction.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final RuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final TestScenarioRepository testScenarioRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestExecutionResultRepository testExecutionResultRepository;

    public DashboardServiceImpl(
            RuleRepository ruleRepository,
            TransactionRepository transactionRepository,
            TestScenarioRepository testScenarioRepository,
            TestCaseRepository testCaseRepository,
            TestExecutionRepository testExecutionRepository,
            TestExecutionResultRepository testExecutionResultRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
        this.testScenarioRepository = testScenarioRepository;
        this.testCaseRepository = testCaseRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.testExecutionResultRepository = testExecutionResultRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        long totalRules = ruleRepository.count();
        long totalTransactions = transactionRepository.count();
        long totalScenarios = testScenarioRepository.count();
        long totalTestCases = testCaseRepository.count();
        long totalExecutions = testExecutionRepository.count();

        log.info("[DASHBOARD] totalRules={}", totalRules);
        log.info("[DASHBOARD] totalTransactions={}", totalTransactions);
        log.info("[DASHBOARD] totalScenarios={}", totalScenarios);
        log.info("[DASHBOARD] totalTestCases={}", totalTestCases);
        log.info("[DASHBOARD] totalExecutions={}", totalExecutions);

        long passedExecutions = countExecutionsByStatus(ExecutionStatus.PASSED);
        long failedExecutions = countExecutionsByStatus(ExecutionStatus.FAILED);
        long errorExecutions = countExecutionsByStatus(ExecutionStatus.ERROR);
        long runningExecutions = countExecutionsByStatus(ExecutionStatus.RUNNING);
        long pendingExecutions = countExecutionsByStatus(ExecutionStatus.PENDING);

        double passRate = calculatePercentage(passedExecutions, totalExecutions);
        log.info("[DASHBOARD] passed={}, failed={}, passRate={}", passedExecutions, failedExecutions, passRate);

        // ── Active rules ──────────────────────────────────────────────────────
        long activeRules = 0;
        try {
            activeRules = ruleRepository.countByStatus(RuleStatus.ACTIVE);
            log.info("[DASHBOARD] activeRules={}", activeRules);
        } catch (Exception e) {
            log.warn("[DASHBOARD] Could not count active rules: {}", e.getMessage());
        }

        // ── Pass/fail distribution chart ──────────────────────────────────────
        Map<String, Long> passFailDistribution = new LinkedHashMap<>();
        passFailDistribution.put("PASSED", passedExecutions);
        passFailDistribution.put("FAILED", failedExecutions);

        // ── Executions by rule type (per test-case result) ────────────────────
        Map<String, Long> executionsByRuleType = new LinkedHashMap<>();
        try {
            List<Object[]> rows = testExecutionResultRepository.countResultsByRuleType();
            for (Object[] row : rows) {
                String ruleType = row[0] != null ? String.valueOf(row[0]) : "UNKNOWN";
                long count = ((Number) row[1]).longValue();
                executionsByRuleType.put(ruleType, count);
            }
            log.info("[DASHBOARD] executionsByRuleType size={}", executionsByRuleType.size());
        } catch (Exception e) {
            log.warn("[DASHBOARD] Could not compute executionsByRuleType: {}", e.getMessage());
        }

        // ── Most failed rule type ─────────────────────────────────────────────
        String mostFailedRuleType = "N/A";
        try {
            List<Object[]> failedRows = testExecutionResultRepository
                    .countResultsByRuleTypeAndStatus(ExecutionStatus.FAILED);
            if (!failedRows.isEmpty() && failedRows.get(0)[0] != null) {
                mostFailedRuleType = String.valueOf(failedRows.get(0)[0]);
            }
            log.info("[DASHBOARD] mostFailedRuleType={}", mostFailedRuleType);
        } catch (Exception e) {
            log.warn("[DASHBOARD] Could not compute mostFailedRuleType: {}", e.getMessage());
        }

        // ── Most triggered rule (rule name with most non-ACCEPT results) ───────
        String mostTriggeredRule = "N/A";
        try {
            List<Object[]> triggeredRows = testExecutionResultRepository
                    .findMostTriggeredRuleName(RuleAction.ACCEPT);
            if (!triggeredRows.isEmpty() && triggeredRows.get(0)[0] != null) {
                mostTriggeredRule = String.valueOf(triggeredRows.get(0)[0]);
            }
            log.info("[DASHBOARD] mostTriggeredRule={}", mostTriggeredRule);
        } catch (Exception e) {
            log.warn("[DASHBOARD] Could not compute mostTriggeredRule: {}", e.getMessage());
        }

        // ── Risk action distribution (ACCEPT / MONITOR / REJECT) ─────────────
        Map<String, Long> riskActionDistribution = new LinkedHashMap<>();
        riskActionDistribution.put("ACCEPT", 0L);
        riskActionDistribution.put("MONITOR", 0L);
        riskActionDistribution.put("REJECT", 0L);
        try {
            List<Object[]> actionRows = testExecutionResultRepository.countByActualAction();
            for (Object[] row : actionRows) {
                if (row[0] != null) {
                    String action = String.valueOf(row[0]);
                    long count = ((Number) row[1]).longValue();
                    riskActionDistribution.put(action, count);
                }
            }
            log.info("[DASHBOARD] riskActionDistribution={}", riskActionDistribution);
        } catch (Exception e) {
            log.warn("[DASHBOARD] Could not compute riskActionDistribution: {}", e.getMessage());
        }

        // ── Transaction status distribution (APPROVED / DECLINED / PENDING) ───
        Map<String, Long> transactionStatusDistribution = new LinkedHashMap<>();
        transactionStatusDistribution.put("APPROVED", 0L);
        transactionStatusDistribution.put("DECLINED", 0L);
        transactionStatusDistribution.put("PENDING", 0L);
        try {
            List<Object[]> statusRows = transactionRepository.countGroupedByTransactionStatus();
            for (Object[] row : statusRows) {
                if (row[0] != null) {
                    String rawStatus = String.valueOf(row[0]).toUpperCase();
                    long count = ((Number) row[1]).longValue();
                    String bucket = mapTransactionStatus(rawStatus);
                    transactionStatusDistribution.merge(bucket, count, Long::sum);
                }
            }
            log.info("[DASHBOARD] transactionStatusDistribution={}", transactionStatusDistribution);
        } catch (Exception e) {
            log.warn("[DASHBOARD] Could not compute transactionStatusDistribution: {}", e.getMessage());
        }

        // ── Build response ────────────────────────────────────────────────────
        DashboardSummaryResponse response = new DashboardSummaryResponse();

        // Existing fields
        response.setTotalRules(totalRules);
        response.setTotalTransactions(totalTransactions);
        response.setTotalScenarios(totalScenarios);
        response.setTotalTestCases(totalTestCases);
        response.setTotalExecutions(totalExecutions);
        response.setPassedExecutions(passedExecutions);
        response.setFailedExecutions(failedExecutions);
        response.setErrorExecutions(errorExecutions);
        response.setRunningExecutions(runningExecutions);
        response.setPendingExecutions(pendingExecutions);
        response.setSuccessRate(passRate);

        // New fields
        response.setActiveRules(activeRules);
        response.setPassRate(passRate);
        response.setMostFailedRuleType(mostFailedRuleType);
        response.setMostTriggeredRule(mostTriggeredRule);
        response.setPassFailDistribution(passFailDistribution);
        response.setExecutionsByRuleType(executionsByRuleType);
        response.setRiskActionDistribution(riskActionDistribution);
        response.setTransactionStatusDistribution(transactionStatusDistribution);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleWiseExecutionStats> getRuleWiseExecutionStats() {
        List<RuleEntity> rules = ruleRepository.findAll();

        return rules.stream()
                .map(this::buildRuleWiseStats)
                .sorted(Comparator.comparing(RuleWiseExecutionStats::getTotalExecutions).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecutionTrendResponse> getExecutionTrend(int days) {
        int trendDays = days <= 0 ? 7 : days;

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(trendDays - 1L);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        List<TestExecutionEntity> executions = testExecutionRepository.findAll(
                buildExecutionTrendSpecification(startDateTime)
        );

        List<ExecutionTrendResponse> trendResponses = new ArrayList<>();

        for (int i = 0; i < trendDays; i++) {
            LocalDate currentDate = startDate.plusDays(i);

            List<TestExecutionEntity> dayExecutions = executions.stream()
                    .filter(execution -> execution.getStartedAt() != null)
                    .filter(execution -> execution.getStartedAt().toLocalDate().equals(currentDate))
                    .toList();

            long total = dayExecutions.size();
            long passed = countStatus(dayExecutions, ExecutionStatus.PASSED);
            long failed = countStatus(dayExecutions, ExecutionStatus.FAILED);
            long error = countStatus(dayExecutions, ExecutionStatus.ERROR);

            ExecutionTrendResponse response = new ExecutionTrendResponse();
            response.setDate(currentDate);
            response.setTotalExecutions(total);
            response.setPassedCount(passed);
            response.setFailedCount(failed);
            response.setErrorCount(error);
            response.setSuccessRate(calculatePercentage(passed, total));

            trendResponses.add(response);
        }

        return trendResponses;
    }

    private RuleWiseExecutionStats buildRuleWiseStats(RuleEntity rule) {
        List<TestScenarioEntity> scenarios = testScenarioRepository.findByRuleRuleId(rule.getRuleId());

        long totalScenarios = scenarios.size();

        long totalTestCases = 0;
        long totalExecutions = 0;
        long passedExecutions = 0;
        long failedExecutions = 0;
        long errorExecutions = 0;

        for (TestScenarioEntity scenario : scenarios) {
            totalTestCases += testCaseRepository.findByScenarioScenarioId(scenario.getScenarioId()).size();

            List<TestExecutionEntity> executions =
                    testExecutionRepository.findByScenarioScenarioId(scenario.getScenarioId());

            totalExecutions += executions.size();
            passedExecutions += countStatus(executions, ExecutionStatus.PASSED);
            failedExecutions += countStatus(executions, ExecutionStatus.FAILED);
            errorExecutions += countStatus(executions, ExecutionStatus.ERROR);
        }

        RuleWiseExecutionStats stats = new RuleWiseExecutionStats();
        stats.setRuleId(rule.getRuleId());
        stats.setRuleName(rule.getRuleName());
        stats.setRuleType(rule.getRuleType());
        stats.setTotalScenarios(totalScenarios);
        stats.setTotalTestCases(totalTestCases);
        stats.setTotalExecutions(totalExecutions);
        stats.setPassedExecutions(passedExecutions);
        stats.setFailedExecutions(failedExecutions);
        stats.setErrorExecutions(errorExecutions);
        stats.setSuccessRate(calculatePercentage(passedExecutions, totalExecutions));

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecuteTestResponse> getRecentExecutions(int limit) {
        int maxLimit = limit > 0 ? limit : 10;
        PageRequest pageable = PageRequest.of(0, maxLimit, Sort.by(Sort.Direction.DESC, "startedAt"));
        return testExecutionRepository.findAll(pageable).getContent()
                .stream()
                .map(e -> TestExecutionMapper.toExecutionResponse(e, List.of()))
                .toList();
    }

    private long countExecutionsByStatus(ExecutionStatus status) {
        return testExecutionRepository.count((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("executionStatus"), status)
        );
    }

    private long countStatus(List<TestExecutionEntity> executions, ExecutionStatus status) {
        return executions.stream()
                .filter(execution -> execution.getExecutionStatus() == status)
                .count();
    }

    private Specification<TestExecutionEntity> buildExecutionTrendSpecification(LocalDateTime startDateTime) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startedAt"), startDateTime));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private double calculatePercentage(long value, long total) {
        if (total == 0) {
            return 0.0;
        }

        return Math.round(((double) value / total) * 10000.0) / 100.0;
    }

    private String mapTransactionStatus(String upperStatus) {
        return switch (upperStatus) {
            case "SUCCESS", "APPROVED", "COMPLETED" -> "APPROVED";
            case "PENDING" -> "PENDING";
            default -> "DECLINED";
        };
    }
}