package com.thejas.ai_frms.ai.client;

import com.thejas.ai_frms.ai.dto.fastapi.FastApiAnalyzeFailureRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiExplainRuleRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiGenerateRuleRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiGenerateTransactionRequest;
import com.thejas.ai_frms.ai.dto.fastapi.FastApiTestCasesRequest;

public interface AiServiceClient {

    String generateTestCases(FastApiTestCasesRequest request);

    String analyzeFailure(FastApiAnalyzeFailureRequest request);

    String explainRule(FastApiExplainRuleRequest request);

    String generateTransaction(FastApiGenerateTransactionRequest request);

    String generateRule(FastApiGenerateRuleRequest request);

    String chat(String message);

    String getHealth();
}