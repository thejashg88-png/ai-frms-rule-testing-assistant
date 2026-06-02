package com.thejas.ai_frms.dashboard.service;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.dashboard.dto.DashboardSummaryResponse;
import com.thejas.ai_frms.dashboard.dto.ExecutionTrendResponse;
import com.thejas.ai_frms.dashboard.dto.RuleWiseExecutionStats;
import com.thejas.ai_frms.execution.dto.ExecuteTestResponse;
import com.thejas.ai_frms.execution.entity.TestExecutionEntity;
import com.thejas.ai_frms.execution.mapper.TestExecutionMapper;
import com.thejas.ai_frms.execution.repository.TestExecutionRepository;
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
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final RuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final TestScenarioRepository testScenarioRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository testExecutionRepository;

    public DashboardServiceImpl(
            RuleRepository ruleRepository,
            TransactionRepository transactionRepository,
            TestScenarioRepository testScenarioRepository,
            TestCaseRepository testCaseRepository,
            TestExecutionRepository testExecutionRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
        this.testScenarioRepository = testScenarioRepository;
        this.testCaseRepository = testCaseRepository;
        this.testExecutionRepository = testExecutionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        long totalRules = ruleRepository.count();
        long totalTransactions = transactionRepository.count();
        long totalScenarios = testScenarioRepository.count();
        long totalTestCases = testCaseRepository.count();
        long totalExecutions = testExecutionRepository.count();

        log.info("[DASHBOARD] Total rules count={}", totalRules);
        log.info("[DASHBOARD] Total transactions count={}", totalTransactions);
        log.info("[DASHBOARD] Total scenarios count={}", totalScenarios);
        log.info("[DASHBOARD] Total test cases count={}", totalTestCases);
        log.info("[DASHBOARD] Total executions count={}", totalExecutions);

        long passedExecutions = countExecutionsByStatus(ExecutionStatus.PASSED);
        long failedExecutions = countExecutionsByStatus(ExecutionStatus.FAILED);
        long errorExecutions = countExecutionsByStatus(ExecutionStatus.ERROR);
        long runningExecutions = countExecutionsByStatus(ExecutionStatus.RUNNING);
        long pendingExecutions = countExecutionsByStatus(ExecutionStatus.PENDING);

        log.info("[DASHBOARD] Execution breakdown passed={}, failed={}, error={}, running={}, pending={}",
                passedExecutions, failedExecutions, errorExecutions, runningExecutions, pendingExecutions);

        DashboardSummaryResponse response = new DashboardSummaryResponse();
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
        response.setSuccessRate(calculatePercentage(passedExecutions, totalExecutions));

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
}