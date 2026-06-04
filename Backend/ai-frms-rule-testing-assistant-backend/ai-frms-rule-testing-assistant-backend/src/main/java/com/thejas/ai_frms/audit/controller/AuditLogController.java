package com.thejas.ai_frms.audit.controller;

import com.thejas.ai_frms.audit.dto.AuditLogRequest;
import com.thejas.ai_frms.audit.dto.AuditLogResponse;
import com.thejas.ai_frms.audit.dto.AuditLogSearchRequest;
import com.thejas.ai_frms.audit.service.AuditLogService;
import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for audit log access.
 *
 * Role access: ADMIN only. No other role may read or create audit log entries directly.
 */
@RestController
@RequestMapping(ApiPathConstants.AUDIT)
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private static final Logger log = LoggerFactory.getLogger(AuditLogController.class);

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AuditLogResponse>> createAuditLog(
            @Valid @RequestBody AuditLogRequest request,
            HttpServletRequest servletRequest
    ) {
        request.setIpAddress(servletRequest.getRemoteAddr());
        request.setUserAgent(servletRequest.getHeader("User-Agent"));

        AuditLogResponse response = auditLogService.createAuditLog(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Audit log created successfully", response));
    }

    @GetMapping("/{auditId}")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(@PathVariable Long auditId) {
        AuditLogResponse response = auditLogService.getAuditLogById(auditId);
        return ResponseEntity.ok(ApiResponse.success("Audit log fetched successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> searchAuditLogs(
            @ModelAttribute AuditLogSearchRequest request
    ) {
        log.info("[AUDIT LOGS] Fetch request page={}, size={}", request.getPage(), request.getSize());
        PageResponse<AuditLogResponse> response = auditLogService.searchAuditLogs(request);
        log.info("[AUDIT LOGS] Fetch completed totalElements={}", response.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched successfully", response));
    }
}