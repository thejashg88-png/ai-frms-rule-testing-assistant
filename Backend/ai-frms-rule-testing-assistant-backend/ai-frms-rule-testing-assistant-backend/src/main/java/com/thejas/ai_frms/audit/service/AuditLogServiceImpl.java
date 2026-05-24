package com.thejas.ai_frms.audit.service;

import com.thejas.ai_frms.audit.dto.AuditLogRequest;
import com.thejas.ai_frms.audit.dto.AuditLogResponse;
import com.thejas.ai_frms.audit.dto.AuditLogSearchRequest;
import com.thejas.ai_frms.audit.entity.AuditLogEntity;
import com.thejas.ai_frms.audit.mapper.AuditLogMapper;
import com.thejas.ai_frms.audit.repository.AuditLogRepository;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
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
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public AuditLogResponse createAuditLog(AuditLogRequest request) {
        AuditLogEntity entity = AuditLogMapper.toEntity(request);
        AuditLogEntity savedEntity = auditLogRepository.save(entity);

        return AuditLogMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> searchAuditLogs(AuditLogSearchRequest request) {
        Pageable pageable = buildPageable(request);

        Page<AuditLogResponse> responsePage = auditLogRepository
                .findAll(buildSpecification(request), pageable)
                .map(AuditLogMapper::toResponse);

        return PageResponse.fromPage(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long auditId) {
        AuditLogEntity entity = auditLogRepository.findById(auditId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found with id: " + auditId));

        return AuditLogMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void logAction(
            String moduleName,
            String action,
            String entityName,
            Long entityId,
            String description,
            String performedBy
    ) {
        AuditLogRequest request = new AuditLogRequest();
        request.setModuleName(moduleName);
        request.setAction(action);
        request.setEntityName(entityName);
        request.setEntityId(entityId);
        request.setDescription(description);
        request.setPerformedBy(performedBy);

        createAuditLog(request);
    }

    private Pageable buildPageable(AuditLogSearchRequest request) {
        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() <= 0 ? 10 : request.getSize();

        String sortBy = request.getSortBy() == null || request.getSortBy().isBlank()
                ? "createdAt"
                : request.getSortBy();

        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    private Specification<AuditLogEntity> buildSpecification(AuditLogSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getModuleName() != null && !request.getModuleName().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("moduleName"), request.getModuleName()));
            }

            if (request.getAction() != null && !request.getAction().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("action"), request.getAction()));
            }

            if (request.getEntityName() != null && !request.getEntityName().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("entityName"), request.getEntityName()));
            }

            if (request.getEntityId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityId"), request.getEntityId()));
            }

            if (request.getPerformedBy() != null && !request.getPerformedBy().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("performedBy"), request.getPerformedBy()));
            }

            if (request.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), request.getFromDate()));
            }

            if (request.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), request.getToDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}