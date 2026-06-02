package com.thejas.ai_frms.rule.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.rule.dto.RuleCreateRequest;
import com.thejas.ai_frms.rule.dto.RuleResponse;
import com.thejas.ai_frms.rule.dto.RuleSearchRequest;
import com.thejas.ai_frms.rule.dto.RuleUpdateRequest;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.rule.mapper.RuleMapper;
import com.thejas.ai_frms.rule.repository.RuleRepository;
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

    private final RuleRepository ruleRepository;

    public RuleServiceImpl(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
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

        RuleMapper.updateEntity(entity, request);
        RuleEntity updatedEntity = ruleRepository.save(entity);

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

        Page<RuleResponse> responsePage = ruleRepository
                .findAll(buildSpecification(request), pageable)
                .map(RuleMapper::toResponse);

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
        RuleEntity entity = getRuleEntity(ruleId);
        entity.setStatus(status);

        RuleEntity updatedEntity = ruleRepository.save(entity);
        return RuleMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public void deleteRule(Long ruleId) {
        RuleEntity entity = getRuleEntity(ruleId);
        ruleRepository.delete(entity);
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