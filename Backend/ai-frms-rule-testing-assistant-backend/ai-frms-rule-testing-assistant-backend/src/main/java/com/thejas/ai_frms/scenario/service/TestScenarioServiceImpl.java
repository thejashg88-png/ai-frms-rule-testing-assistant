package com.thejas.ai_frms.scenario.service;

import com.thejas.ai_frms.audit.service.AuditLogService;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.execution.repository.TestExecutionRepository;
import com.thejas.ai_frms.execution.repository.TestExecutionResultRepository;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.rule.repository.RuleRepository;
import com.thejas.ai_frms.scenario.dto.TestScenarioCreateRequest;
import com.thejas.ai_frms.scenario.dto.TestScenarioResponse;
import com.thejas.ai_frms.scenario.dto.TestScenarioUpdateRequest;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.mapper.TestScenarioMapper;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service implementation for test scenario lifecycle management.
 *
 * A scenario is a container for test cases grouped under a single fraud rule.
 * Each scenario must link to exactly one rule (resolved by ruleId or ruleType).
 *
 * Rule resolution order on create:
 *   1. ruleId   — exact match; throws if not found
 *   2. ruleType — prefers ACTIVE rules; falls back to any status if none active
 *
 * Scenario names must be unique (case-insensitive).
 */
@Service
public class TestScenarioServiceImpl implements TestScenarioService {

    private static final Logger log = LoggerFactory.getLogger(TestScenarioServiceImpl.class);
    private static final String ENTITY_TYPE = "SCENARIO";

    private final TestScenarioRepository testScenarioRepository;
    private final RuleRepository ruleRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository executionRepository;
    private final TestExecutionResultRepository executionResultRepository;
    private final AuditLogService auditLogService;

    public TestScenarioServiceImpl(
            TestScenarioRepository testScenarioRepository,
            RuleRepository ruleRepository,
            TestCaseRepository testCaseRepository,
            TestExecutionRepository executionRepository,
            TestExecutionResultRepository executionResultRepository,
            AuditLogService auditLogService
    ) {
        this.testScenarioRepository = testScenarioRepository;
        this.ruleRepository = ruleRepository;
        this.testCaseRepository = testCaseRepository;
        this.executionRepository = executionRepository;
        this.executionResultRepository = executionResultRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public TestScenarioResponse createScenario(TestScenarioCreateRequest request) {
        log.info("[SCENARIO CREATE] Incoming request: scenarioName={}, ruleId={}, ruleType={}, status={}, expectedResult={}",
                request.getScenarioName(), request.getRuleId(), request.getRuleType(),
                request.getStatus(), request.getExpectedResult());

        if (testScenarioRepository.existsByScenarioNameIgnoreCase(request.getScenarioName())) {
            throw new BadRequestException("Scenario name already exists: " + request.getScenarioName());
        }

        RuleEntity rule = resolveRule(request);

        log.info("[SCENARIO CREATE] Validation passed — linked to ruleId={}, ruleType={}",
                rule.getRuleId(), rule.getRuleType());

        TestScenarioEntity entity = TestScenarioMapper.toEntity(request, rule);
        TestScenarioEntity savedEntity = testScenarioRepository.save(entity);

        log.info("[SCENARIO CREATE] Saved scenario id: {}", savedEntity.getScenarioId());

        auditLogService.logCreate(
                request.getCreatedBy(),
                ENTITY_TYPE,
                savedEntity.getScenarioId(),
                savedEntity.getScenarioName(),
                TestScenarioMapper.toResponse(savedEntity)
        );

        return TestScenarioMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional
    public TestScenarioResponse updateScenario(Long scenarioId, TestScenarioUpdateRequest request) {
        TestScenarioEntity entity = getScenarioEntity(scenarioId);

        if (request.getScenarioName() != null
                && testScenarioRepository.existsByScenarioNameIgnoreCaseAndScenarioIdNot(
                request.getScenarioName(),
                scenarioId
        )) {
            throw new BadRequestException("Scenario name already exists: " + request.getScenarioName());
        }

        // Capture old state before applying changes
        TestScenarioResponse oldState = TestScenarioMapper.toResponse(entity);

        RuleEntity rule = null;
        if (request.getRuleId() != null) {
            rule = getRuleEntity(request.getRuleId());
        }

        TestScenarioMapper.updateEntity(entity, request, rule);
        TestScenarioEntity updatedEntity = testScenarioRepository.save(entity);

        auditLogService.logUpdate(
                request.getModifiedBy(),
                ENTITY_TYPE,
                scenarioId,
                updatedEntity.getScenarioName(),
                oldState,
                TestScenarioMapper.toResponse(updatedEntity)
        );

        return TestScenarioMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public TestScenarioResponse getScenarioById(Long scenarioId) {
        TestScenarioEntity entity = getScenarioEntity(scenarioId);
        return TestScenarioMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TestScenarioResponse> searchScenarios(
            Long ruleId,
            String scenarioName,
            RuleStatus status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<TestScenarioResponse> responsePage = testScenarioRepository
                .findAll(buildSpecification(ruleId, scenarioName, status), pageable)
                .map(TestScenarioMapper::toResponse);

        return PageResponse.fromPage(responsePage);
    }

    @Override
    @Transactional
    public TestScenarioResponse changeScenarioStatus(Long scenarioId, RuleStatus status) {
        TestScenarioEntity entity = getScenarioEntity(scenarioId);
        entity.setStatus(status);

        TestScenarioEntity updatedEntity = testScenarioRepository.save(entity);

        String action = status == RuleStatus.INACTIVE ? "INACTIVATE" : "ACTIVATE";
        auditLogService.logEvent(
                null,
                action,
                ENTITY_TYPE,
                scenarioId,
                entity.getScenarioName(),
                String.format("User changed scenario '%s' status to %s", entity.getScenarioName(), status)
        );

        return TestScenarioMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public String deleteScenario(Long scenarioId) {
        TestScenarioEntity entity = getScenarioEntity(scenarioId);
        log.info("[SCENARIO DELETE] scenarioId={}", scenarioId);

        // Snapshot for audit before any deletions
        TestScenarioResponse snapshot = TestScenarioMapper.toResponse(entity);

        // Step 1: Find test case IDs under this scenario
        List<Long> testCaseIds = testCaseRepository.findTestCaseIdsByScenarioId(scenarioId);
        log.info("[SCENARIO DELETE] testCaseIds={}", testCaseIds);

        // Step 2: Find execution IDs linked to this scenario or its test cases (deduplicated)
        Set<Long> executionIdSet = new HashSet<>(executionRepository.findExecutionIdsByScenarioId(scenarioId));
        if (!testCaseIds.isEmpty()) {
            executionIdSet.addAll(executionRepository.findExecutionIdsByTestCaseIdIn(testCaseIds));
        }
        List<Long> executionIds = new ArrayList<>(executionIdSet);
        log.info("[SCENARIO DELETE] executionIds={}", executionIds);

        // Step 3: Delete execution results
        if (!executionIds.isEmpty()) {
            executionResultRepository.deleteByExecutionExecutionIdIn(executionIds);
        }
        log.info("[SCENARIO DELETE] executionResultsDeleted for {} executions", executionIds.size());

        // Step 4: Delete executions
        if (!executionIds.isEmpty()) {
            executionRepository.deleteByExecutionIdIn(executionIds);
        }
        log.info("[SCENARIO DELETE] executionsDeleted={}", executionIds.size());

        // Step 5: Delete test cases
        if (!testCaseIds.isEmpty()) {
            testCaseRepository.deleteByTestCaseIdIn(testCaseIds);
        }
        log.info("[SCENARIO DELETE] testCasesDeleted={}", testCaseIds.size());

        // Step 6: Audit then delete scenario
        auditLogService.logDelete(null, "DELETE", ENTITY_TYPE, scenarioId, entity.getScenarioName(), snapshot);
        testScenarioRepository.deleteById(scenarioId);

        // Step 7: Verify scenario is gone
        boolean stillExists = testScenarioRepository.existsById(scenarioId);
        log.info("[SCENARIO DELETE VERIFY] existsAfterDelete={}", stillExists);
        if (stillExists) {
            throw new IllegalStateException("Scenario delete failed — record still exists after delete. scenarioId=" + scenarioId);
        }

        return "Scenario deleted successfully.";
    }

    private TestScenarioEntity getScenarioEntity(Long scenarioId) {
        return testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with id: " + scenarioId));
    }

    private RuleEntity getRuleEntity(Long ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found with id: " + ruleId));
    }

    private RuleEntity resolveRule(TestScenarioCreateRequest request) {
        // Prefer explicit ruleId
        if (request.getRuleId() != null) {
            return getRuleEntity(request.getRuleId());
        }

        // Fall back to ruleType lookup
        if (request.getRuleType() != null && !request.getRuleType().isBlank()) {
            // Prefer ACTIVE rules first
            List<RuleEntity> activeRules = ruleRepository.findByRuleTypeAndStatus(
                    request.getRuleType(), RuleStatus.ACTIVE);
            if (!activeRules.isEmpty()) {
                return activeRules.get(0);
            }
            // Accept any status if no ACTIVE rule found
            List<RuleEntity> anyRules = ruleRepository.findByRuleType(request.getRuleType());
            if (!anyRules.isEmpty()) {
                return anyRules.get(0);
            }
            throw new ResourceNotFoundException(
                    "No rule found with type: " + request.getRuleType()
                    + ". Create a rule with this type first.");
        }

        throw new BadRequestException(
                "Either ruleId or ruleType is required to create a scenario");
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

    private Specification<TestScenarioEntity> buildSpecification(
            Long ruleId,
            String scenarioName,
            RuleStatus status
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // When no explicit status filter, exclude soft-deleted records so they never reappear
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            } else {
                predicates.add(criteriaBuilder.notEqual(root.get("status"), RuleStatus.INACTIVE));
                predicates.add(criteriaBuilder.notEqual(root.get("status"), RuleStatus.DELETED));
            }

            if (ruleId != null) {
                predicates.add(criteriaBuilder.equal(root.get("rule").get("ruleId"), ruleId));
            }

            if (scenarioName != null && !scenarioName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("scenarioName")),
                        "%" + scenarioName.toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}