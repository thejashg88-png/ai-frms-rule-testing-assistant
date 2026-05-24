package com.thejas.ai_frms.testcase.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.GeneratedBy;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.enums.TestCaseType;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;
import com.thejas.ai_frms.testcase.dto.TestCaseResponse;
import com.thejas.ai_frms.testcase.dto.TestCaseUpdateRequest;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import com.thejas.ai_frms.testcase.mapper.TestCaseMapper;
import com.thejas.ai_frms.testcase.repository.TestCaseRepository;
import com.thejas.ai_frms.transaction.entity.TransactionEntity;
import com.thejas.ai_frms.transaction.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final TestScenarioRepository testScenarioRepository;
    private final TransactionRepository transactionRepository;

    public TestCaseServiceImpl(
            TestCaseRepository testCaseRepository,
            TestScenarioRepository testScenarioRepository,
            TransactionRepository transactionRepository
    ) {
        this.testCaseRepository = testCaseRepository;
        this.testScenarioRepository = testScenarioRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public TestCaseResponse createTestCase(TestCaseCreateRequest request) {
        if (testCaseRepository.existsByTestCaseNameIgnoreCase(request.getTestCaseName())) {
            throw new BadRequestException("Test case name already exists: " + request.getTestCaseName());
        }

        TestScenarioEntity scenario = getScenarioEntity(request.getScenarioId());

        TransactionEntity transaction = null;
        if (request.getTransactionId() != null) {
            transaction = getTransactionEntity(request.getTransactionId());
        }

        TestCaseEntity entity = TestCaseMapper.toEntity(request, scenario, transaction);
        TestCaseEntity savedEntity = testCaseRepository.save(entity);

        return TestCaseMapper.toResponse(savedEntity);
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
    public void deleteTestCase(Long testCaseId) {
        TestCaseEntity entity = getTestCaseEntity(testCaseId);
        testCaseRepository.delete(entity);
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
            }

            if (generatedBy != null) {
                predicates.add(criteriaBuilder.equal(root.get("generatedBy"), generatedBy));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}