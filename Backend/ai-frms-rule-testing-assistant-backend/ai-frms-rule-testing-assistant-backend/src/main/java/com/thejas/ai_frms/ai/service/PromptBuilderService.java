package com.thejas.ai_frms.ai.service;

import com.thejas.ai_frms.ai.dto.AiFailureAnalysisRequest;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationRequest;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationRequest;
import com.thejas.ai_frms.common.util.JsonUtil;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilderService {

    public String buildTestCaseGenerationPrompt(AiTestCaseGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert payment domain QA engineer.\n");
        prompt.append("Generate FRMS rule test cases for the following fraud/risk rule.\n\n");

        prompt.append("Rule Details:\n");
        prompt.append("Rule ID: ").append(request.getRuleId()).append("\n");
        prompt.append("Scenario ID: ").append(request.getScenarioId()).append("\n");
        prompt.append("Rule Name: ").append(request.getRuleName()).append("\n");
        prompt.append("Rule Type: ").append(request.getRuleType()).append("\n");
        prompt.append("Action: ").append(request.getAction()).append("\n");
        prompt.append("Status: ").append(request.getStatus()).append("\n");
        prompt.append("MCC Code: ").append(request.getMccCode()).append("\n");
        prompt.append("Transaction Count: ").append(request.getTxnCount()).append("\n");
        prompt.append("Frequency Hours: ").append(request.getFrequencyHours()).append("\n");
        prompt.append("Transaction Amount: ").append(request.getTxnAmount()).append("\n");
        prompt.append("Max Amount: ").append(request.getMaxAmount()).append("\n");
        prompt.append("Percentage Threshold: ").append(request.getPercentageThreshold()).append("\n");
        prompt.append("Description: ").append(request.getRuleDescription()).append("\n\n");

        prompt.append("Generate ").append(request.getNumberOfTestCases()).append(" test cases.\n");
        prompt.append("Include positive cases: ").append(request.getIncludePositiveCases()).append("\n");
        prompt.append("Include negative cases: ").append(request.getIncludeNegativeCases()).append("\n");
        prompt.append("Include boundary cases: ").append(request.getIncludeBoundaryCases()).append("\n");
        prompt.append("Include edge cases: ").append(request.getIncludeEdgeCases()).append("\n\n");

        prompt.append("Return ONLY a valid JSON array.\n");
        prompt.append("Each item must match this Java DTO structure:\n");
        prompt.append("""
                {
                  "testCaseName": "string",
                  "testCaseDescription": "string",
                  "testCaseType": "POSITIVE | NEGATIVE | BOUNDARY | EDGE_CASE | AI_GENERATED",
                  "status": "ACTIVE",
                  "generatedBy": "AI",
                  "scenarioId": 1,
                  "transactionId": null,
                  "inputData": {
                    "rrn": "string",
                    "stan": "string",
                    "serialNumber": "string",
                    "track2Data": "string",
                    "tid": "string",
                    "mid": "string",
                    "mccCode": "string",
                    "amount": 1000,
                    "currency": "INR",
                    "transactionType": "SALE",
                    "responseCode": "00",
                    "responseMessage": "Approved",
                    "transactionStatus": "SUCCESS"
                  },
                  "expectedResult": {
                    "expectedAction": "ACCEPT | MONITOR | REJECT",
                    "expectedEvaluationStatus": "ACCEPT | MONITOR | REJECT",
                    "expectedRuleType": "string",
                    "expectedAlertCodes": ["string"],
                    "expectedRiskScore": 0,
                    "remarks": "string"
                  },
                  "createdBy": "AI"
                }
                """);

        return prompt.toString();
    }

    public String buildFailureAnalysisPrompt(AiFailureAnalysisRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert FRMS debugging assistant.\n");
        prompt.append("Analyze why this test case failed.\n\n");

        prompt.append("Execution ID: ").append(request.getExecutionId()).append("\n");
        prompt.append("Test Case ID: ").append(request.getTestCaseId()).append("\n");
        prompt.append("Test Case Name: ").append(request.getTestCaseName()).append("\n");
        prompt.append("Rule Name: ").append(request.getRuleName()).append("\n");
        prompt.append("Rule Type: ").append(request.getRuleType()).append("\n");
        prompt.append("Failure Message: ").append(request.getFailureMessage()).append("\n\n");

        prompt.append("Input Data:\n");
        prompt.append(JsonUtil.toJson(request.getInputData())).append("\n\n");

        prompt.append("Expected Result:\n");
        prompt.append(JsonUtil.toJson(request.getExpectedResult())).append("\n\n");

        prompt.append("Actual Result:\n");
        prompt.append("Actual Action: ").append(request.getActualAction()).append("\n");
        prompt.append("Actual Evaluation Status: ").append(request.getActualEvaluationStatus()).append("\n");
        prompt.append("Actual Rule Type: ").append(request.getActualRuleType()).append("\n\n");

        prompt.append("Return explanation in this format:\n");
        prompt.append("Analysis:\nProbable Cause:\nRecommendation:\n");

        return prompt.toString();
    }

    public String buildRuleExplanationPrompt(AiRuleExplanationRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a payment fraud/risk rule expert.\n");
        prompt.append("Explain the below FRMS rule in simple language.\n\n");

        prompt.append("Rule Details:\n");
        prompt.append("Rule ID: ").append(request.getRuleId()).append("\n");
        prompt.append("Rule Name: ").append(request.getRuleName()).append("\n");
        prompt.append("Rule Type: ").append(request.getRuleType()).append("\n");
        prompt.append("Action: ").append(request.getAction()).append("\n");
        prompt.append("Status: ").append(request.getStatus()).append("\n");
        prompt.append("MCC Code: ").append(request.getMccCode()).append("\n");
        prompt.append("Transaction Count: ").append(request.getTxnCount()).append("\n");
        prompt.append("Frequency Hours: ").append(request.getFrequencyHours()).append("\n");
        prompt.append("Transaction Amount: ").append(request.getTxnAmount()).append("\n");
        prompt.append("Max Amount: ").append(request.getMaxAmount()).append("\n");
        prompt.append("Percentage Threshold: ").append(request.getPercentageThreshold()).append("\n");
        prompt.append("Description: ").append(request.getRuleDescription()).append("\n\n");

        prompt.append("Explain:\n");
        prompt.append("1. What this rule does\n");
        prompt.append("2. Business meaning\n");
        prompt.append("3. Suggested test cases\n");
        prompt.append("4. Edge cases\n");

        return prompt.toString();
    }
}