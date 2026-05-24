package com.thejas.ai_frms.frmsintegration.service;

import com.thejas.ai_frms.frmsintegration.dto.FrmsRiskEvaluationResponse;
import com.thejas.ai_frms.frmsintegration.dto.FrmsRuleResponse;
import com.thejas.ai_frms.frmsintegration.dto.FrmsTransactionRequest;
import com.thejas.ai_frms.testcase.dto.TestInputData;

public interface FrmsIntegrationService {

    FrmsRiskEvaluationResponse evaluateRisk(FrmsTransactionRequest request);

    FrmsRiskEvaluationResponse evaluateRiskFromTestInput(TestInputData inputData);

    FrmsRuleResponse getRuleById(Long ruleId);
}