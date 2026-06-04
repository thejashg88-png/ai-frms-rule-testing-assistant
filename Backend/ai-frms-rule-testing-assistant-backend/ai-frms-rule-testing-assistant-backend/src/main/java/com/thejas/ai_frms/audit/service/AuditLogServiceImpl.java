package com.thejas.ai_frms.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejas.ai_frms.audit.dto.AuditLogRequest;
import com.thejas.ai_frms.audit.dto.AuditLogResponse;
import com.thejas.ai_frms.audit.dto.AuditLogSearchRequest;
import com.thejas.ai_frms.audit.entity.AuditLogEntity;
import com.thejas.ai_frms.audit.mapper.AuditLogMapper;
import com.thejas.ai_frms.audit.repository.AuditLogRepository;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.common.util.UserContextHolder;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Audit logging service implementation.
 *
 * Key guarantees:
 *   - Audit logging NEVER breaks the main business flow. All save operations are wrapped
 *     in try/catch; failures are logged as errors but not re-thrown.
 *   - Old/new values are stored as JSON for full traceability of what changed.
 *   - Sensitive fields (cardNumber, track2Data, PAN, password, token) are masked
 *     before storing to prevent exposure of payment credentials.
 *   - Runs in REQUIRES_NEW propagation so a business transaction rollback does not
 *     roll back audit records.
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    // findAndRegisterModules() handles LocalDateTime serialization automatically
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    // Pattern to find and mask sensitive JSON field values
    private static final Pattern CARD_NUMBER_PATTERN =
            Pattern.compile("(\"cardNumber\"\\s*:\\s*\")([^\"]{10,})(\")");
    private static final Pattern TRACK2_PATTERN =
            Pattern.compile("(\"track2Data\"\\s*:\\s*\")([^\"]+)(\")");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("(\"password(?:Hash)?\"\\s*:\\s*\")([^\"]+)(\")");
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("(\"(?:token|apiKey|jwtToken)\"\\s*:\\s*\")([^\"]+)(\")");

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
        try {
            Pageable pageable = buildPageable(request);
            log.info("[AUDIT LOGS] Repository query started page={}, size={}", request.getPage(), request.getSize());

            Page<AuditLogEntity> entityPage = auditLogRepository.findAll(buildSpecification(request), pageable);
            log.info("[AUDIT LOGS] Total records={}", entityPage.getTotalElements());

            Page<AuditLogResponse> responsePage = entityPage.map(entity -> {
                log.debug("[AUDIT LOGS] Mapping audit log id={}", entity.getAuditId());
                return AuditLogMapper.toResponse(entity);
            });

            return PageResponse.fromPage(responsePage);
        } catch (Exception ex) {
            log.error("[AUDIT LOGS] Failed to fetch audit logs: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long auditId) {
        AuditLogEntity entity = auditLogRepository.findById(auditId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found with id: " + auditId));
        return AuditLogMapper.toResponse(entity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(
            String moduleName,
            String action,
            String entityName,
            Long entityId,
            String description,
            String performedBy
    ) {
        try {
            AuditLogEntity entity = new AuditLogEntity();
            entity.setModuleName(moduleName);
            entity.setAction(action);
            entity.setEntityName(entityName);
            entity.setEntityId(entityId);
            entity.setDescription(description);
            entity.setPerformedBy(resolveActor(performedBy));

            AuditLogEntity saved = auditLogRepository.save(entity);
            log.info("[AUDIT] action={}, entityType={}, entityId={}, actor={} — saved auditId={}",
                    action, moduleName, entityId, entity.getPerformedBy(), saved.getAuditId());
        } catch (Exception ex) {
            log.error("[AUDIT] failed to save audit log: action={}, entityType={}, entityId={} — {}",
                    action, moduleName, entityId, ex.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(String actor, String entityType, Long entityId, String entityName, Object newValue) {
        try {
            String resolvedActor = resolveActor(actor);
            String newJson = toSafeJson(newValue);
            String description = String.format("User %s created %s '%s' (id=%d)",
                    resolvedActor, entityType, entityName, entityId);

            AuditLogEntity entity = buildEntity(resolvedActor, "CREATE", entityType, entityId, entityName,
                    description, null, newJson);
            AuditLogEntity saved = auditLogRepository.save(entity);
            log.info("[AUDIT] action=CREATE, actor={}, entityType={}, entityId={} — saved auditId={}",
                    resolvedActor, entityType, entityId, saved.getAuditId());
        } catch (Exception ex) {
            log.error("[AUDIT] failed to save audit log: action=CREATE, entityType={}, entityId={} — {}",
                    entityType, entityId, ex.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String actor, String entityType, Long entityId, String entityName,
                          Object oldValue, Object newValue) {
        try {
            String resolvedActor = resolveActor(actor);
            String oldJson = toSafeJson(oldValue);
            String newJson = toSafeJson(newValue);
            String description = String.format("User %s updated %s '%s' (id=%d)",
                    resolvedActor, entityType, entityName, entityId);

            AuditLogEntity entity = buildEntity(resolvedActor, "UPDATE", entityType, entityId, entityName,
                    description, oldJson, newJson);
            AuditLogEntity saved = auditLogRepository.save(entity);
            log.info("[AUDIT] action=UPDATE, actor={}, entityType={}, entityId={} — saved auditId={}",
                    resolvedActor, entityType, entityId, saved.getAuditId());
        } catch (Exception ex) {
            log.error("[AUDIT] failed to save audit log: action=UPDATE, entityType={}, entityId={} — {}",
                    entityType, entityId, ex.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String actor, String action, String entityType, Long entityId,
                          String entityName, Object oldValue) {
        try {
            String resolvedActor = resolveActor(actor);
            String oldJson = toSafeJson(oldValue);
            String description = String.format("User %s performed %s on %s '%s' (id=%d)",
                    resolvedActor, action, entityType, entityName, entityId);

            AuditLogEntity entity = buildEntity(resolvedActor, action, entityType, entityId, entityName,
                    description, oldJson, null);
            AuditLogEntity saved = auditLogRepository.save(entity);
            log.info("[AUDIT] action={}, actor={}, entityType={}, entityId={} — saved auditId={}",
                    action, resolvedActor, entityType, entityId, saved.getAuditId());
        } catch (Exception ex) {
            log.error("[AUDIT] failed to save audit log: action={}, entityType={}, entityId={} — {}",
                    action, entityType, entityId, ex.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(String actor, String action, String entityType, Long entityId,
                         String entityName, String description) {
        try {
            String resolvedActor = resolveActor(actor);
            AuditLogEntity entity = buildEntity(resolvedActor, action, entityType, entityId, entityName,
                    description, null, null);
            AuditLogEntity saved = auditLogRepository.save(entity);
            log.info("[AUDIT] action={}, actor={}, entityType={}, entityId={} — saved auditId={}",
                    action, resolvedActor, entityType, entityId, saved.getAuditId());
        } catch (Exception ex) {
            log.error("[AUDIT] failed to save audit log: action={}, entityType={}, entityId={} — {}",
                    action, entityType, entityId, ex.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logReportDownload(String actor, String reportType) {
        try {
            String resolvedActor = resolveActor(actor);
            String description = String.format("User %s downloaded %s report", resolvedActor, reportType);

            AuditLogEntity entity = buildEntity(resolvedActor, "DOWNLOAD_REPORT", "REPORT", null,
                    reportType, description, null, null);
            AuditLogEntity saved = auditLogRepository.save(entity);
            log.info("[AUDIT] action=DOWNLOAD_REPORT, actor={}, reportType={} — saved auditId={}",
                    resolvedActor, reportType, saved.getAuditId());
        } catch (Exception ex) {
            log.error("[AUDIT] failed to save audit log: action=DOWNLOAD_REPORT, reportType={} — {}",
                    reportType, ex.getMessage());
        }
    }

    /**
     * Resolves the actor for an audit log entry in priority order:
     *   1. X-Actor-Username request header (sent by the frontend after login)
     *   2. Explicit body field passed by the calling service (createdBy/modifiedBy/executedBy)
     *   3. UserContextHolder ThreadLocal (legacy interceptor-based fallback)
     *   4. "SYSTEM" — backend/scheduled operations with no user context
     */
    private String resolveActor(String bodyActor) {
        // 1. X-Actor-Username header — highest priority
        String headerActor = readActorFromHeader();
        if (headerActor != null) {
            log.info("[AUDIT ACTOR] header actor={}, final actor={}", headerActor, headerActor);
            return headerActor;
        }

        // 2. Explicit body field (createdBy / modifiedBy / executedBy)
        if (bodyActor != null && !bodyActor.isBlank()) {
            String trimmed = bodyActor.trim();
            log.info("[AUDIT ACTOR] body field actor={}, final actor={}", trimmed, trimmed);
            return trimmed;
        }

        // 3. UserContextHolder (legacy interceptor-set ThreadLocal)
        String holderActor = UserContextHolder.getUsername();
        if (holderActor != null && !holderActor.isBlank()) {
            log.info("[AUDIT ACTOR] UserContextHolder actor={}, final actor={}", holderActor, holderActor);
            return holderActor;
        }

        log.info("[AUDIT ACTOR] final actor=SYSTEM (fallback — no header, no body field, no holder)");
        return "SYSTEM";
    }

    // Reads the X-Actor-Username header from the current HTTP request via RequestContextHolder.
    // Returns null if no request is available (e.g. async/scheduled context) or header is absent.
    private String readActorFromHeader() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String actor = request.getHeader("X-Actor-Username");
                if (actor != null && !actor.isBlank()) {
                    return actor.trim();
                }
            }
        } catch (Exception ex) {
            log.debug("[AUDIT ACTOR] Could not read X-Actor-Username header: {}", ex.getMessage());
        }
        return null;
    }

    private String getCurrentActor() {
        return resolveActor(null);
    }

    // Convert an object to JSON string with sensitive field masking.
    // On failure, returns the object's toString() as a fallback — never throws.
    private String toSafeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String json = OBJECT_MAPPER.writeValueAsString(value);
            return maskSensitiveFields(json);
        } catch (Exception ex) {
            log.warn("[AUDIT] JSON serialization failed for {}: {} — using toString fallback",
                    value.getClass().getSimpleName(), ex.getMessage());
            return String.valueOf(value);
        }
    }

    /**
     * Masks sensitive fields in a JSON string.
     * Fields masked: cardNumber (→ first6****last4), track2Data (→ masked PAN),
     * password/passwordHash (→ ****), token/apiKey (→ ****).
     * This prevents payment credentials from being stored in audit logs.
     */
    private String maskSensitiveFields(String json) {
        if (json == null) {
            return null;
        }

        json = CARD_NUMBER_PATTERN.matcher(json).replaceAll(m -> {
            String card = m.group(2);
            return m.group(1) + maskCard(card) + m.group(3);
        });

        json = TRACK2_PATTERN.matcher(json).replaceAll(m -> {
            String track2 = m.group(2);
            int sepIdx = track2.indexOf('=');
            if (sepIdx == -1) sepIdx = track2.indexOf('D');
            String masked = sepIdx > 0 ? maskCard(track2.substring(0, sepIdx)) + "****" : "****";
            return m.group(1) + masked + m.group(3);
        });

        json = PASSWORD_PATTERN.matcher(json).replaceAll("$1****$3");
        json = TOKEN_PATTERN.matcher(json).replaceAll("$1****$3");

        return json;
    }

    // Mask card number: show first 6 + ****** + last 4
    private String maskCard(String pan) {
        if (pan == null || pan.length() < 10) {
            return "****";
        }
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }

    private AuditLogEntity buildEntity(String actor, String action, String entityType,
                                        Long entityId, String entityName, String description,
                                        String oldValue, String newValue) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setPerformedBy(actor);
        entity.setAction(action);
        entity.setModuleName(entityType);
        entity.setEntityId(entityId);
        entity.setEntityName(entityName);
        entity.setDescription(description);
        entity.setOldValue(oldValue);
        entity.setNewValue(newValue);
        return entity;
    }

    private Pageable buildPageable(AuditLogSearchRequest request) {
        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() <= 0 ? 20 : request.getSize();

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
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("performedBy")),
                        "%" + request.getPerformedBy().toLowerCase() + "%"
                ));
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