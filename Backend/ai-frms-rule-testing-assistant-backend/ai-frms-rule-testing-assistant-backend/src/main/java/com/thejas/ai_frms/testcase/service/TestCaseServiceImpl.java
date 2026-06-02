package com.thejas.ai_frms.testcase.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.GeneratedBy;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.enums.TestCaseType;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.testcase.dto.ExpectedResult;
import com.thejas.ai_frms.testcase.dto.TestInputData;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;
import com.thejas.ai_frms.testcase.dto.TestCaseResponse;
import com.thejas.ai_frms.testcase.dto.TestCaseUpdateRequest;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import com.thejas.ai_frms.testcase.mapper.TestCaseMapper;
import com.thejas.ai_frms.execution.repository.TestExecutionResultRepository;
import com.thejas.ai_frms.testcase.repository.TestCaseRepository;
import com.thejas.ai_frms.transaction.entity.TransactionEntity;
import com.thejas.ai_frms.transaction.repository.TransactionRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class TestCaseServiceImpl implements TestCaseService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseServiceImpl.class);

    private final TestCaseRepository testCaseRepository;
    private final TestScenarioRepository testScenarioRepository;
    private final TransactionRepository transactionRepository;
    private final TestExecutionResultRepository testExecutionResultRepository;

    public TestCaseServiceImpl(
            TestCaseRepository testCaseRepository,
            TestScenarioRepository testScenarioRepository,
            TransactionRepository transactionRepository,
            TestExecutionResultRepository testExecutionResultRepository
    ) {
        this.testCaseRepository = testCaseRepository;
        this.testScenarioRepository = testScenarioRepository;
        this.transactionRepository = transactionRepository;
        this.testExecutionResultRepository = testExecutionResultRepository;
    }

    @Override
    @Transactional
    public TestCaseResponse createTestCase(TestCaseCreateRequest request) {
        log.info("[TEST CASE CREATE] Incoming request: testCaseName={}, scenarioId={}, ruleId={}",
                request.getTestCaseName(), request.getScenarioId(), request.getRuleId());
        log.info("[TEST CASE CREATE] inputData: {}, flat fields: cardNumber={}, amount={}, merchantId={}",
                request.getInputData() != null ? "nested object" : "absent",
                request.getCardNumber(), request.getAmount(), request.getMerchantId());
        log.info("[TEST CASE CREATE] Expected result: outcome={}, action={}, riskLevel={}",
                request.getExpectedResult() != null ? request.getExpectedResult().getExpectedOutcome() : "null",
                request.getExpectedResult() != null ? request.getExpectedResult().getExpectedAction() : "null",
                request.getExpectedResult() != null ? request.getExpectedResult().getExpectedRiskLevel() : "null");

        if (request.getScenarioId() == null) {
            throw new BadRequestException("Valid scenarioId is required");
        }

        if (testCaseRepository.existsByTestCaseNameIgnoreCase(request.getTestCaseName())) {
            throw new BadRequestException("Test case name already exists: " + request.getTestCaseName());
        }

        TestScenarioEntity scenario = getScenarioEntity(request.getScenarioId());

        // Build inputData from flat fields if nested object not provided
        if (request.getInputData() == null) {
            request.setInputData(buildInputDataFromFlatFields(request));
        }
        if (request.getInputData() == null) {
            throw new BadRequestException(
                    "inputData is required. Provide a nested inputData object or flat fields (cardNumber, amount, etc.)");
        }
        log.info("[TEST CASE CREATE] inputData resolved: cardNumber={}, amount={}",
                request.getInputData().getCardNumber(), request.getInputData().getAmount());

        TransactionEntity transaction = null;
        if (request.getTransactionId() != null) {
            transaction = getTransactionEntity(request.getTransactionId());
        }

        TestCaseEntity entity = TestCaseMapper.toEntity(request, scenario, transaction);
        TestCaseEntity savedEntity = testCaseRepository.save(entity);

        log.info("[TEST CASE CREATE] Saved test case id: {}", savedEntity.getTestCaseId());

        return TestCaseMapper.toResponse(savedEntity);
    }

    private TestInputData buildInputDataFromFlatFields(TestCaseCreateRequest request) {
        boolean hasAnyFlatField = request.getCardNumber() != null
                || request.getAmount() != null
                || request.getMerchantId() != null
                || request.getTransactionType() != null
                || request.getChannel() != null
                || request.getCountryCode() != null;

        if (!hasAnyFlatField) {
            return null;
        }

        TestInputData data = new TestInputData();
        data.setCardNumber(request.getCardNumber());
        data.setMerchantId(request.getMerchantId());
        data.setTransactionType(request.getTransactionType());
        data.setChannel(request.getChannel());
        data.setCountryCode(request.getCountryCode());

        if (request.getAmount() != null) {
            try {
                data.setAmount(new BigDecimal(request.getAmount()));
            } catch (NumberFormatException ignored) {
                log.warn("[TEST CASE CREATE] Could not parse amount '{}' as BigDecimal", request.getAmount());
            }
        }
        return data;
    }

    @Override
    @Transactional
    public TestCaseResponse updateTestCase(Long testCaseId, TestCaseUpdateRequest request) {
        TestCaseEntity entity = getTestCaseEntity(testCaseId);

        if (request.getTestCaseName() != null
                && testCaseRepository.existsByTestCaseNameIgnoreCaseAndTestCaseIdNot(
                request.getTestCaseName(),
                testCaseId
        )) {
            throw new BadRequestException("Test case name already exists: " + request.getTestCaseName());
        }

        TestScenarioEntity scenario = null;
        if (request.getScenarioId() != null) {
            scenario = getScenarioEntity(request.getScenarioId());
        }

        TransactionEntity transaction = null;
        if (request.getTransactionId() != null) {
            transaction = getTransactionEntity(request.getTransactionId());
        }

        TestCaseMapper.updateEntity(entity, request, scenario, transaction);

        TestCaseEntity updatedEntity = testCaseRepository.save(entity);
        return TestCaseMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public TestCaseResponse getTestCaseById(Long testCaseId) {
        TestCaseEntity entity = getTestCaseEntity(testCaseId);
        return TestCaseMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TestCaseResponse> searchTestCases(
            Long scenarioId,
            String testCaseName,
            TestCaseType testCaseType,
            RuleStatus status,
            GeneratedBy generatedBy,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<TestCaseResponse> responsePage = testCaseRepository
                .findAll(buildSpecification(scenarioId, testCaseName, testCaseType, status, generatedBy), pageable)
                .map(TestCaseMapper::toResponse);

        return PageResponse.fromPage(responsePage);
    }

    @Override
    @Transactional
    public TestCaseResponse changeTestCaseStatus(Long testCaseId, RuleStatus status) {
        TestCaseEntity entity = getTestCaseEntity(testCaseId);
        entity.setStatus(status);

        TestCaseEntity updatedEntity = testCaseRepository.save(entity);
        return TestCaseMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public String deleteTestCase(Long testCaseId) {
        TestCaseEntity entity = getTestCaseEntity(testCaseId);
        log.info("[TEST CASE DELETE] Requested delete for testCaseId={}", testCaseId);
        log.info("[TEST CASE DELETE] Found test case={}", entity.getTestCaseName());

        long executionResultCount = testExecutionResultRepository.countByTestCaseTestCaseId(testCaseId);
        log.info("[TEST CASE DELETE] Execution result count={}", executionResultCount);

        if (executionResultCount > 0) {
            log.info("[TEST CASE DELETE] Delete mode=soft (test case has execution history)");
            entity.setStatus(RuleStatus.INACTIVE);
            testCaseRepository.save(entity);
            log.info("[TEST CASE DELETE] Completed soft delete for testCaseId={}", testCaseId);
            return "Test case marked as deleted because execution history exists";
        }

        log.info("[TEST CASE DELETE] Delete mode=hard (no execution history)");
        testCaseRepository.delete(entity);
        log.info("[TEST CASE DELETE] Completed delete for testCaseId={}", testCaseId);
        return "Test case deleted successfully";
    }

    private TestCaseEntity getTestCaseEntity(Long testCaseId) {
        return testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + testCaseId));
    }

    private TestScenarioEntity getScenarioEntity(Long scenarioId) {
        return testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with id: " + scenarioId));
    }

    private TransactionEntity getTransactionEntity(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
    }

    private Pageable buildPageable(
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        int pageNumber = Math.max(page, 0);
        int pageSize = size <= 0 ? 10 : size;

        String sortProperty = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy;

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortProperty));
    }

    private Specification<TestCaseEntity> buildSpecification(
            Long scenarioId,
            String testCaseName,
            TestCaseType testCaseType,
            RuleStatus status,
            GeneratedBy generatedBy
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (scenarioId != null) {
                predicates.add(criteriaBuilder.equal(root.get("scenario").get("scenarioId"), scenarioId));
            }

            if (testCaseName != null && !testCaseName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("testCaseName")),
                        "%" + testCaseName.toLowerCase() + "%"
                ));
            }

            if (testCaseType != null) {
                predicates.add(criteriaBuilder.equal(root.get("testCaseType"), testCaseType));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            } else {
                // Exclude soft-deleted (INACTIVE) test cases from default listing
                predicates.add(criteriaBuilder.notEqual(root.get("status"), RuleStatus.INACTIVE));
            }

            if (generatedBy != null) {
                predicates.add(criteriaBuilder.equal(root.get("generatedBy"), generatedBy));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}