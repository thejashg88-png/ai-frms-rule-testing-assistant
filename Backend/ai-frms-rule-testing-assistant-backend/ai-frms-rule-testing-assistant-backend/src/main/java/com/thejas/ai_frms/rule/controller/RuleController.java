package com.thejas.ai_frms.rule.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.rule.dto.RuleCreateRequest;
import com.thejas.ai_frms.rule.dto.RuleResponse;
import com.thejas.ai_frms.rule.dto.RuleSearchRequest;
import com.thejas.ai_frms.rule.dto.RuleUpdateRequest;
import com.thejas.ai_frms.rule.service.RuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing fraud detection rules (CRUD + status management).
 *
 * Rules are the core configuration of the FRMS system. Each rule has a type, action,
 * and type-specific thresholds (maxAmount, txnCount, frequencyHours, etc.).
 *
 * Key behavior:
 *   - Only ACTIVE rules are evaluated during transaction risk assessment and test case execution.
 *   - PATCH /{ruleId}/status can set a rule to INACTIVE to disable it without deleting it.
 *   - Rule names must be unique (case-insensitive).
 *   - Deleting a rule that has linked scenarios or test cases will fail with a 400 (FK constraint).
 */
@RestController
@RequestMapping(ApiPathConstants.RULES)
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RuleResponse>> createRule(@Valid @RequestBody RuleCreateRequest request) {
        RuleResponse response = ruleService.createRule(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rule created successfully", response));
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<RuleResponse>> updateRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody RuleUpdateRequest request
    ) {
        RuleResponse response = ruleService.updateRule(ruleId, request);

        return ResponseEntity.ok(ApiResponse.success("Rule updated successfully", response));
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<RuleResponse>> getRuleById(@PathVariable Long ruleId) {
        RuleResponse response = ruleService.getRuleById(ruleId);

        return ResponseEntity.ok(ApiResponse.success("Rule fetched successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RuleResponse>>> searchRules(
            @ModelAttribute RuleSearchRequest request
    ) {
        PageResponse<RuleResponse> response = ruleService.searchRules(request);

        return ResponseEntity.ok(ApiResponse.success("Rules fetched successfully", response));
    }

    @PatchMapping("/{ruleId}/status")
    public ResponseEntity<ApiResponse<RuleResponse>> changeRuleStatus(
            @PathVariable Long ruleId,
            @RequestParam RuleStatus status
    ) {
        RuleResponse response = ruleService.changeRuleStatus(ruleId, status);

        return ResponseEntity.ok(ApiResponse.success("Rule status changed successfully", response));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long ruleId) {
        ruleService.deleteRule(ruleId);

        return ResponseEntity.ok(ApiResponse.success("Rule deleted successfully"));
    }
}