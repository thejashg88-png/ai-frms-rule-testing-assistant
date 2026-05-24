package com.thejas.ai_frms.scenario.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.rule.entity.RuleEntity;
import com.thejas.ai_frms.rule.repository.RuleRepository;
import com.thejas.ai_frms.scenario.dto.TestScenarioCreateRequest;
import com.thejas.ai_frms.scenario.dto.TestScenarioResponse;
import com.thejas.ai_frms.scenario.dto.TestScenarioUpdateRequest;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.mapper.TestScenarioMapper;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
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
public class TestScenarioServiceImpl implements TestScenarioService {

    private final TestScenarioRepository testScenarioRepository;
    private final RuleRepository ruleRepository;

    public TestScenarioServiceImpl(
            TestScenarioRepository testScenarioRepository,
            RuleRepository ruleRepository
    ) {
        this.testScenarioRepository = testScenarioRepository;
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional
    public TestScenarioResponse createScenario(TestScenarioCreateRequest request) {
        if (testScenarioRepository.existsByScenarioNameIgnoreCase(request.getScenarioName())) {
            throw new BadRequestException("Scenario name already exists: " + request.getScenarioName());
        }

        RuleEntity rule = getRuleEntity(request.getRuleId());

        TestScenarioEntity entity = TestScenarioMapper.toEntity(request, rule);
        TestScenarioEntity savedEntity = testScenarioRepository.save(entity);

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

        RuleEntity rule = null;

        if (request.getRuleId() != null) {
            rule = getRuleEntity(request.getRuleId());
        }

        TestScenarioMapper.updateEntity(entity, request, rule);

        TestScenarioEntity updatedEntity = testScenarioRepository.save(entity);
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
        return TestScenarioMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public void deleteScenario(Long scenarioId) {
        TestScenarioEntity entity = getScenarioEntity(scenarioId);
        testScenarioRepository.delete(entity);
    }

    private TestScenarioEntity getScenarioEntity(Long scenarioId) {
        return testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with id: " + scenarioId));
    }

    private RuleEntity getRuleEntity(Long ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found with id: " + ruleId));
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

            if (ruleId != null) {
                predicates.add(criteriaBuilder.equal(root.get("rule").get("ruleId"), ruleId));
            }

            if (scenarioName != null && !scenarioName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("scenarioName")),
                        "%" + scenarioName.toLowerCase() + "%"
                ));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}