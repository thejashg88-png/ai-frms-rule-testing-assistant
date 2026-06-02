package com.thejas.ai_frms.execution.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.common.util.JsonUtil;
import com.thejas.ai_frms.execution.dto.ComparisonResult;
import com.thejas.ai_frms.execution.dto.ExecuteScenarioRequest;
import com.thejas.ai_frms.execution.dto.ExecuteTestCaseRequest;
import com.thejas.ai_frms.execution.dto.ExecuteTestResponse;
import com.thejas.ai_frms.execution.dto.ExecutionResultResponse;
import com.thejas.ai_frms.execution.entity.TestExecutionEntity;
import com.thejas.ai_frms.execution.entity.TestExecutionResultEntity;
import com.thejas.ai_frms.execution.mapper.TestExecutionMapper;
import com.thejas.ai_frms.execution.repository.TestExecutionRepository;
import com.thejas.ai_frms.execution.repository.TestExecutionResultRepository;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
import com.thejas.ai_frms.testcase.dto.ExpectedResult;
import com.thejas.ai_frms.testcase.dto.TestInputData;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import com.thejas.ai_frms.testcase.repository.TestCaseRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TestExecutionServiceImpl implements TestExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionServiceImpl.class);

    private static final String EXECUTION_TYPE_TEST_CASE = "TEST_CASE";
    private static final String EXECUTION_TYPE_SCENARIO = "SCENARIO";

    private final TestExecutionRepository testExecutionRepository;
    private final TestExecutionResultRepository testExecutionResultRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestScenarioRepository testScenarioRepository;
    private final RuleExecutionEngine ruleExecutionEngine;
    private final ResultComparisonService resultComparisonService;

    public TestExecutionServiceImpl(
            TestExecutionRepository testExecutionRepository,
            TestExecutionResultRepository testExecutionResultRepository,
            TestCaseRepository testCaseRepository,
            TestScenarioRepository testScenarioRepository,
            RuleExecutionEngine ruleExecutionEngine,
            ResultComparisonService resultComparisonService
    ) {
        this.testExecutionRepository = testExecutionRepository;
        this.testExecutionResultRepository = testExecutionResultRepository;
        this.testCaseRepository = testCaseRepository;
        this.testScenarioRepository = testScenarioRepository;
        this.ruleExecutionEngine = ruleExecutionEngine;
        this.resultComparisonService = resultComparisonService;
    }

    @Override
    @Transactional
    public ExecuteTestResponse executeTestCase(ExecuteTestCaseRequest request) {
        TestCaseEntity testCase = getTestCaseEntity(request.getTestCaseId());

        TestExecutionEntity execution = new TestExecutionEntity();
        execution.setExecutionType(EXECUTION_TYPE_TEST_CASE);
        execution.setExecutionStatus(ExecutionStatus.RUNNING);
        execution.setTestCase(testCase);
        execution.setScenario(testCase.getScenario());
        execution.setExecutedBy(request.getExecutedBy());
        execution.setStartedAt(LocalDateTime.now());
        execution.setTotalCount(1);

        TestExecutionEntity savedExecution = testExecutionRepository.save(execution);

        TestExecutionResultEntity result = executeSingleTestCase(savedExecution, testCase);
        TestExecutionResultEntity savedResult = testExecutionResultRepository.save(result);

        updateExecutionSummary(savedExecution, List.of(savedResult));

        TestExecutionEntity completedExecution = testExecutionRepository.save(savedExecution);

        List<TestExecutionResultEntity> results =
                testExecutionResultRepository.findByExecutionExecutionIdOrderByExecutedAtAsc(
                        completedExecution.getExecutionId()
                );

        return TestExecutionMapper.toExecutionResponse(completedExecution, results);
    }

    @Override
    @Transactional
    public ExecuteTestResponse executeScenario(ExecuteScenarioRequest request) {
        log.info("[SCENARIO EXECUTION] scenarioId={}, executedBy={}", request.getScenarioId(), request.getExecutedBy());

        TestScenarioEntity scenario = getScenarioEntity(request.getScenarioId());

        List<TestCaseEntity> allTestCases = testCaseRepository.findByScenarioScenarioId(request.getScenarioId());
        List<TestCaseEntity> activeTestCases = testCaseRepository.findByScenarioScenarioIdAndStatus(
                request.getScenarioId(), RuleStatus.ACTIVE);

        log.info("[SCENARIO EXECUTION] total test cases in scenario={}, active test cases count={}",
                allTestCases.size(), activeTestCases.size());

        int skippedCount = allTestCases.size() - activeTestCases.size();
        if (skippedCount > 0) {
            log.info("[SCENARIO EXECUTION] skipping {} inactive/deleted test cases", skippedCount);
        }

        if (activeTestCases.isEmpty()) {
            throw new BadRequestException("No active test cases found for this scenario");
        }

        TestExecutionEntity execution = new TestExecutionEntity();
        execution.setExecutionType(EXECUTION_TYPE_SCENARIO);
        execution.setExecutionStatus(ExecutionStatus.RUNNING);
        execution.setScenario(scenario);
        execution.setExecutedBy(request.getExecutedBy());
        execution.setStartedAt(LocalDateTime.now());
        execution.setTotalCount(activeTestCases.size());

        TestExecutionEntity savedExecution = testExecutionRepository.save(execution);

        List<TestExecutionResultEntity> resultList = activeTestCases.stream()
                .map(testCase -> {
                    log.info("[SCENARIO EXECUTION] executing testCaseId={}, status={}",
                            testCase.getTestCaseId(), testCase.getStatus());
                    return executeSingleTestCase(savedExecution, testCase);
                })
                .toList();

        List<TestExecutionResultEntity> savedResults = testExecutionResultRepository.saveAll(resultList);

        updateExecutionSummary(savedExecution, savedResults);

        log.info("[SCENARIO EXECUTION] summary total={}, passed={}, failed={}, errors={}",
                savedExecution.getTotalCount(), savedExecution.getPassedCount(),
                savedExecution.getFailedCount(), savedExecution.getErrorCount());

        TestExecutionEntity completedExecution = testExecutionRepository.save(savedExecution);

        List<TestExecutionResultEntity> results =
                testExecutionResultRepository.findByExecutionExecutionIdOrderByExecutedAtAsc(
                        completedExecution.getExecutionId()
                );

        return TestExecutionMapper.toExecutionResponse(completedExecution, results);
    }

    @Override
    @Transactional(readOnly = true)
    public ExecuteTestResponse getExecutionById(Long executionId) {
        TestExecutionEntity execution = getExecutionEntity(executionId);

        List<TestExecutionResultEntity> results =
                testExecutionResultRepository.findByExecutionExecutionIdOrderByExecutedAtAsc(executionId);

        return TestExecutionMapper.toExecutionResponse(execution, results);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecutionResultResponse> getResultsByExecutionId(Long executionId) {
        if (!testExecutionRepository.existsById(executionId)) {
            throw new ResourceNotFoundException("Execution not found with id: " + executionId);
        }

        return testExecutionResultRepository.findByExecutionExecutionIdOrderByExecutedAtAsc(executionId)
                .stream()
                .map(TestExecutionMapper::toResultResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExecuteTestResponse> searchExecutions(
            Long scenarioId,
            Long testCaseId,
            ExecutionStatus executionStatus,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<ExecuteTestResponse> responsePage = testExecutionRepository
                .findAll(buildSpecification(scenarioId, testCaseId, executionStatus), pageable)
                .map(execution -> {
                    List<TestExecutionResultEntity> results =
                            testExecutionResultRepository.findByExecutionExecutionIdOrderByExecutedAtAsc(
                                    execution.getExecutionId()
                            );

                    return TestExecutionMapper.toExecutionResponse(execution, results);
                });

        return PageResponse.fromPage(responsePage);
    }

    @Override
    @Transactional
    public void deleteExecution(Long executionId) {
        TestExecutionEntity execution = getExecutionEntity(executionId);

        testExecutionResultRepository.deleteByExecutionExecutionId(executionId);
        testExecutionRepository.delete(execution);
    }

    private TestExecutionResultEntity executeSingleTestCase(
            TestExecutionEntity execution,
            TestCaseEntity testCase
    ) {
        log.info("[EXECUTION] Running testCaseId={}, testCaseName={}",
                testCase.getTestCaseId(), testCase.getTestCaseName());
        try {
            TestInputData inputData = JsonUtil.fromJson(testCase.getInputDataJson(), TestInputData.class);
            ExpectedResult expectedResult = JsonUtil.fromJson(testCase.getExpectedResultJson(), ExpectedResult.class);

            // Default null currency — does not affect rule evaluation but keeps data clean
            if (inputData != null && (inputData.getCurrency() == null || inputData.getCurrency().isBlank())) {
                inputData.setCurrency("INR");
            }

            if (inputData == null || inputData.getAmount() == null) {
                throw new IllegalArgumentException("inputData.amount is required for rule evaluation");
            }

            log.info("[EXECUTION] Expected action={}, expectedOutcome={}",
                    expectedResult != null ? expectedResult.getExpectedAction() : "null",
                    expectedResult != null ? expectedResult.getExpectedOutcome() : "null");

            ComparisonResult actualResult = ruleExecutionEngine.execute(testCase, inputData);
            ComparisonResult comparisonResult = resultComparisonService.compare(expectedResult, actualResult);

            TestExecutionResultEntity result = new TestExecutionResultEntity();
            result.setExecution(execution);
            result.setTestCase(testCase);
            result.setResultStatus(comparisonResult.isMatched() ? ExecutionStatus.PASSED : ExecutionStatus.FAILED);
            result.setExpectedAction(comparisonResult.getExpectedAction());
            result.setActualAction(comparisonResult.getActualAction());
            result.setExpectedEvaluationStatus(comparisonResult.getExpectedEvaluationStatus());
            result.setActualEvaluationStatus(comparisonResult.getActualEvaluationStatus());
            result.setMessage(comparisonResult.getMessage());
            result.setFailureReason(comparisonResult.getFailureReason());
            result.setExpectedOutcome(comparisonResult.getExpectedOutcome());
            result.setComparisonResultJson(JsonUtil.toJson(comparisonResult));
            result.setExecutedAt(LocalDateTime.now());

            return result;
        } catch (Exception exception) {
            log.error("[EXECUTION] testCaseId={} — execution error: {}",
                    testCase.getTestCaseId(), exception.getMessage(), exception);

            TestExecutionResultEntity result = new TestExecutionResultEntity();
            result.setExecution(execution);
            result.setTestCase(testCase);
            result.setResultStatus(ExecutionStatus.ERROR);
            result.setMessage("Execution error: " + exception.getMessage());
            result.setFailureReason(exception.getMessage());
            result.setExecutedAt(LocalDateTime.now());

            return result;
        }
    }

    private void updateExecutionSummary(
            TestExecutionEntity execution,
            List<TestExecutionResultEntity> results
    ) {
        int passedCount = 0;
        int failedCount = 0;
        int errorCount = 0;

        for (TestExecutionResultEntity result : results) {
            if (result.getResultStatus() == ExecutionStatus.PASSED) {
                passedCount++;
            } else if (result.getResultStatus() == ExecutionStatus.FAILED) {
                failedCount++;
            } else if (result.getResultStatus() == ExecutionStatus.ERROR) {
                errorCount++;
            }
        }

        execution.setPassedCount(passedCount);
        execution.setFailedCount(failedCount);
        execution.setErrorCount(errorCount);
        execution.setCompletedAt(LocalDateTime.now());

        if (errorCount > 0) {
            execution.setExecutionStatus(ExecutionStatus.ERROR);
        } else if (failedCount > 0) {
            execution.setExecutionStatus(ExecutionStatus.FAILED);
        } else {
            execution.setExecutionStatus(ExecutionStatus.PASSED);
        }
    }

    private TestCaseEntity getTestCaseEntity(Long testCaseId) {
        return testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + testCaseId));
    }

    private TestScenarioEntity getScenarioEntity(Long scenarioId) {
        return testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with id: " + scenarioId));
    }

    private TestExecutionEntity getExecutionEntity(Long executionId) {
        return testExecutionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found with id: " + executionId));
    }

    private Pageable buildPageable(
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        int pageNumber = Math.max(page, 0);
        int pageSize = size <= 0 ? 10 : size;

        String sortProperty = sortBy == null || sortBy.isBlank() ? "startedAt" : sortBy;

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortProperty));
    }

    private Specification<TestExecutionEntity> buildSpecification(
            Long scenarioId,
            Long testCaseId,
            ExecutionStatus executionStatus
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (scenarioId != null) {
                predicates.add(criteriaBuilder.equal(root.get("scenario").get("scenarioId"), scenarioId));
            }

            if (testCaseId != null) {
                predicates.add(criteriaBuilder.equal(root.get("testCase").get("testCaseId"), testCaseId));
            }

            if (executionStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("executionStatus"), executionStatus));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}