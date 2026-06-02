package com.thejas.ai_frms.scenario.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.scenario.dto.TestScenarioCreateRequest;
import com.thejas.ai_frms.scenario.dto.TestScenarioResponse;
import com.thejas.ai_frms.scenario.dto.TestScenarioUpdateRequest;
import com.thejas.ai_frms.scenario.service.TestScenarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({ApiPathConstants.SCENARIOS, "/api/scenarios"})
public class TestScenarioController {

    private final TestScenarioService testScenarioService;

    public TestScenarioController(TestScenarioService testScenarioService) {
        this.testScenarioService = testScenarioService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TestScenarioResponse>> createScenario(
            @Valid @RequestBody TestScenarioCreateRequest request
    ) {
        TestScenarioResponse response = testScenarioService.createScenario(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scenario created successfully", response));
    }

    @PutMapping("/{scenarioId}")
    public ResponseEntity<ApiResponse<TestScenarioResponse>> updateScenario(
            @PathVariable Long scenarioId,
            @Valid @RequestBody TestScenarioUpdateRequest request
    ) {
        TestScenarioResponse response = testScenarioService.updateScenario(scenarioId, request);

        return ResponseEntity.ok(ApiResponse.success("Scenario updated successfully", response));
    }

    @GetMapping("/{scenarioId}")
    public ResponseEntity<ApiResponse<TestScenarioResponse>> getScenarioById(
            @PathVariable Long scenarioId
    ) {
        TestScenarioResponse response = testScenarioService.getScenarioById(scenarioId);

        return ResponseEntity.ok(ApiResponse.success("Scenario fetched successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TestScenarioResponse>>> searchScenarios(
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) String scenarioName,
            @RequestParam(required = false) RuleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        PageResponse<TestScenarioResponse> response = testScenarioService.searchScenarios(
                ruleId,
                scenarioName,
                status,
                page,
                size,
                sortBy,
                sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Scenarios fetched successfully", response));
    }

    @PatchMapping("/{scenarioId}/status")
    public ResponseEntity<ApiResponse<TestScenarioResponse>> changeScenarioStatus(
            @PathVariable Long scenarioId,
            @RequestParam RuleStatus status
    ) {
        TestScenarioResponse response = testScenarioService.changeScenarioStatus(scenarioId, status);

        return ResponseEntity.ok(ApiResponse.success("Scenario status changed successfully", response));
    }

    @DeleteMapping("/{scenarioId}")
    public ResponseEntity<ApiResponse<Void>> deleteScenario(@PathVariable Long scenarioId) {
        testScenarioService.deleteScenario(scenarioId);

        return ResponseEntity.ok(ApiResponse.success("Scenario deleted successfully"));
    }
}