package com.thejas.ai_frms.frmsintegration.client;

import com.thejas.ai_frms.frmsintegration.dto.FrmsRiskEvaluationResponse;
import com.thejas.ai_frms.frmsintegration.dto.FrmsRuleResponse;
import com.thejas.ai_frms.frmsintegration.dto.FrmsTransactionRequest;

public interface FrmsClient {

    FrmsRiskEvaluationResponse evaluateRisk(FrmsTransactionRequest request);

    FrmsRuleResponse getRuleById(Long ruleId);
}