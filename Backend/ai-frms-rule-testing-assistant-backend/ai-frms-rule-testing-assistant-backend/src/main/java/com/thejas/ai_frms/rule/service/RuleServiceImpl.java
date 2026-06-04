package com.thejas.ai_frms.rule.service;

import com.thejas.ai_frms.audit.service.AuditLogService;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.execution.repository.TestExecutionRepository;
import com.thejas.ai_frms.execution.repository.TestExecutionResultRepository;
import com.thejas.ai_frms.rule.dto.RuleCreateRequest;
import com.thejas.ai_frms.rule.dto.RuleResponse;
import com.thejas.ai_frms.rule.dto.RuleSearchRequest;
import com.thejas.ai_frms.rule.dto.RuleUpdateRequest;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.rule.mapper.RuleMapper;
import com.thejas.ai_frms.rule.repository.RuleRepository;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
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
 * Service implementation for fraud rule lifecycle management.
 *
 * Rules are the core configuration driving both the transaction risk evaluation
 * (TransactionRuleEvaluationService) and test case execution (RuleExecutionEngine).
 *
 * Important: Setting a rule to INACTIVE stops it from being evaluated against transactions
 * and from being executed in test cases immediately — no restart required.
 */
@Service
public class RuleServiceImpl implements RuleService {

    private static final Logger log = LoggerFactory.getLogger(RuleServiceImpl.class);
    private static final String ENTITY_TYPE = "RULE";

    private final RuleRepository ruleRepository;
    private final TestScenarioRepository scenarioRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository executionRepository;
    private final TestExecutionResultRepository executionResultRepository;
    private final AuditLogService auditLogService;

    public RuleServiceImpl(RuleRepository ruleRepository,
                           TestScenarioRepository scenarioRepository,
                           TestCaseRepository testCaseRepository,
                           TestExecutionRepository executionRepository,
                           TestExecutionResultRepository executionResultRepository,
                           AuditLogService auditLogService) {
        this.ruleRepository = ruleRepository;
        this.scenarioRepository = scenarioRepository;
        this.testCaseRepository = testCaseRepository;
        this.executionRepository = executionRepository;
        this.executionResultRepository = executionResultRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public RuleResponse createRule(RuleCreateRequest request) {
        validateCreateRequest(request);

        if (ruleRepository.existsByRuleNameIgnoreCase(request.getRuleName())) {
            throw new BadRequestException("Rule name already exists: " + request.getRuleName());
        }

        RuleEntity entity = RuleMapper.toEntity(request);
        RuleEntity savedEntity = ruleRepository.save(entity);

        auditLogService.logCreate(
                request.getCreatedBy(),
                ENTITY_TYPE,
                savedEntity.getRuleId(),
                savedEntity.getRuleName(),
                savedEntity
        );

        return RuleMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional
    public RuleResponse updateRule(Long ruleId, RuleUpdateRequest request) {
        RuleEntity entity = getRuleEntity(ruleId);

        if (request.getRuleName() != null
                && ruleRepository.existsByRuleNameIgnoreCaseAndRuleIdNot(request.getRuleName(), ruleId)) {
            throw new BadRequestException("Rule name already exists: " + request.getRuleName());
        }

        // Capture old state for audit before applying changes
        RuleResponse oldState = RuleMapper.toResponse(entity);

        RuleMapper.updateEntity(entity, request);
        RuleEntity updatedEntity = ruleRepository.save(entity);

        auditLogService.logUpdate(
                request.getModifiedBy(),
                ENTITY_TYPE,
                ruleId,
                updatedEntity.getRuleName(),
                oldState,
                RuleMapper.toResponse(updatedEntity)
        );

        return RuleMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public RuleResponse getRuleById(Long ruleId) {
        RuleEntity entity = getRuleEntity(ruleId);
        return RuleMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RuleResponse> searchRules(RuleSearchRequest request) {
        Pageable pageable = buildPageable(request);

        Page<RuleEntity> page = ruleRepository.findAll(buildSpecification(request), pageable);
        long totalFetched = page.getTotalElements();
        Page<RuleResponse> responsePage = page.map(RuleMapper::toResponse);

        log.info("[RULE LIST] totalFetched={} returned={} excludedDeleted=ALWAYS",
                totalFetched, responsePage.getNumberOfElements());

        return PageResponse.fromPage(responsePage);
    }

    /**
     * Changes the active/inactive status of a rule.
     * Setting to INACTIVE immediately excludes the rule from transaction risk evaluation
     * and scenario execution without deleting it or its history.
     */
    @Override
    @Transactional
    public RuleResponse changeRuleStatus(Long ruleId, RuleStatus status) {
        if (status == RuleStatus.DELETED) {
            throw new BadRequestException("Use the DELETE endpoint to remove a rule. Status DELETED cannot be set directly.");
        }

        RuleEntity entity = getRuleEntity(ruleId);
        entity.setStatus(status);

        RuleEntity updatedEntity = ruleRepository.save(entity);

        String action = status == RuleStatus.INACTIVE ? "INACTIVATE" : "ACTIVATE";
        auditLogService.logEvent(
                null,
                action,
                ENTITY_TYPE,
                ruleId,
                entity.getRuleName(),
                String.format("User changed %s rule '%s' status to %s", ENTITY_TYPE, entity.getRuleName(), status)
        );

        return RuleMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public String deleteRule(Long ruleId) {
        RuleEntity entity = getRuleEntity(ruleId);
        String ruleName = entity.getRuleName();

        log.info("[RULE DELETE] ruleId={}", ruleId);

        // Find all scenarios linked to this rule
        List<TestScenarioEntity> allScenarios = scenarioRepository.findByRuleRuleId(ruleId);
        List<Long> scenarioIds = allScenarios.stream().map(TestScenarioEntity::getScenarioId).toList();
        log.info("[RULE DELETE] scenarioIds={}", scenarioIds);

        // Find test case IDs for all linked scenarios
        List<Long> testCaseIds = scenarioIds.isEmpty()
                ? List.of()
                : testCaseRepository.findByScenarioScenarioIdIn(scenarioIds)
                        .stream().map(tc -> tc.getTestCaseId()).toList();
        log.info("[RULE DELETE] testCaseIds={}", testCaseIds);

        // Find execution IDs (by scenario + by test cases, deduplicated)
        Set<Long> executionIdSet = new HashSet<>();
        for (Long sid : scenarioIds) {
            executionIdSet.addAll(executionRepository.findExecutionIdsByScenarioId(sid));
        }
        if (!testCaseIds.isEmpty()) {
            executionIdSet.addAll(executionRepository.findExecutionIdsByTestCaseIdIn(testCaseIds));
        }
        List<Long> executionIds = new ArrayList<>(executionIdSet);
        log.info("[RULE DELETE] executionIds={}", executionIds);

        // Step 1: Delete execution results
        if (!executionIds.isEmpty()) {
            executionResultRepository.deleteByExecutionExecutionIdIn(executionIds);
        }
        log.info("[RULE DELETE] executionResultsDeleted=batch for {} executionIds", executionIds.size());

        // Step 2: Delete executions
        if (!executionIds.isEmpty()) {
            executionRepository.deleteByExecutionIdIn(executionIds);
        }
        log.info("[RULE DELETE] executionsDeleted={}", executionIds.size());

        // Step 3: Delete test cases
        if (!testCaseIds.isEmpty()) {
            testCaseRepository.deleteByTestCaseIdIn(testCaseIds);
        }
        log.info("[RULE DELETE] testCasesDeleted={}", testCaseIds.size());

        // Step 4: Delete scenarios
        if (!scenarioIds.isEmpty()) {
            scenarioRepository.deleteByScenarioIdIn(scenarioIds);
        }
        log.info("[RULE DELETE] scenariosDeleted={}", scenarioIds.size());

        // Step 5: Delete rule — audit snapshot before deletion
        RuleResponse snapshot = RuleMapper.toResponse(entity);
        ruleRepository.deleteById(ruleId);

        // Step 6: Verify deletion
        boolean stillExists = ruleRepository.existsById(ruleId);
        log.info("[RULE DELETE VERIFY] existsAfterDelete={}", stillExists);
        if (stillExists) {
            throw new IllegalStateException("Rule delete failed — record still exists after delete. ruleId=" + ruleId);
        }

        log.info("[RULE DELETE] ruleDeleted=true");
        auditLogService.logDelete(null, "DELETE", ENTITY_TYPE, ruleId, ruleName, snapshot);
        return "Rule deleted successfully.";
    }

    private RuleEntity getRuleEntity(Long ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found with id: " + ruleId));
    }

    // Additional business-level validation beyond @NotBlank/@NotNull on the DTO
    private void validateCreateRequest(RuleCreateRequest request) {
        if (request.getRuleType() == null || request.getRuleType().isBlank()) {
            throw new BadRequestException("Rule type is required");
        }

        if (request.getAction() == null) {
            throw new BadRequestException("Rule action is required");
        }

        if (request.getStatus() == null) {
            request.setStatus(RuleStatus.ACTIVE);
        }
    }

    private Pageable buildPageable(RuleSearchRequest request) {
        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() <= 0 ? 10 : request.getSize();

        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, request.getSortBy()));
    }

    private Specification<RuleEntity> buildSpecification(RuleSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude DELETED rules — they are soft-deleted and must never appear in any list
            predicates.add(criteriaBuilder.notEqual(root.get("status"), RuleStatus.DELETED));

            if (request.getRuleName() != null && !request.getRuleName().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ruleName")),
                        "%" + request.getRuleName().toLowerCase() + "%"
                ));
            }

            if (request.getRuleType() != null && !request.getRuleType().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("ruleType"), request.getRuleType()));
            }

            if (request.getAction() != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), request.getAction()));
            }

            if (request.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }

            if (request.getMccCode() != null && !request.getMccCode().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("mccCode"), request.getMccCode()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}