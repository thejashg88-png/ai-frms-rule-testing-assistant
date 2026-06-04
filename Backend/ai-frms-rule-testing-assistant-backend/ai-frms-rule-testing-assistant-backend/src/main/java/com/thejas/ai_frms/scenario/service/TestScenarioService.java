package com.thejas.ai_frms.scenario.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.scenario.dto.TestScenarioCreateRequest;
import com.thejas.ai_frms.scenario.dto.TestScenarioResponse;
import com.thejas.ai_frms.scenario.dto.TestScenarioUpdateRequest;

public interface TestScenarioService {

    TestScenarioResponse createScenario(TestScenarioCreateRequest request);

    TestScenarioResponse updateScenario(Long scenarioId, TestScenarioUpdateRequest request);

    TestScenarioResponse getScenarioById(Long scenarioId);

    PageResponse<TestScenarioResponse> searchScenarios(
            Long ruleId,
            String scenarioName,
            RuleStatus status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    TestScenarioResponse changeScenarioStatus(Long scenarioId, RuleStatus status);

    String deleteScenario(Long scenarioId);
}