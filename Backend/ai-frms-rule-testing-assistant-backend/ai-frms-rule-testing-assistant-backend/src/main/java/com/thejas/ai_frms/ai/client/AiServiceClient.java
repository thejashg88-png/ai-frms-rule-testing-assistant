package com.thejas.ai_frms.ai.client;

public interface AiServiceClient {

    String generateTestCases(String prompt);

    String analyzeFailure(String prompt);

    String explainRule(String prompt);
}