package com.thejas.ai_frms.ai.service;

import com.thejas.ai_frms.ai.client.AiServiceClient;
import com.thejas.ai_frms.ai.dto.AiFailureAnalysisRequest;
import com.thejas.ai_frms.ai.dto.AiFailureAnalysisResponse;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationRequest;
import com.thejas.ai_frms.ai.dto.AiRuleExplanationResponse;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationRequest;
import com.thejas.ai_frms.ai.dto.AiTestCaseGenerationResponse;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private final AiServiceClient aiServiceClient;
    private final PromptBuilderService promptBuilderService;
    private final AiResponseParserService aiResponseParserService;

    public AiAssistantServiceImpl(
            AiServiceClient aiServiceClient,
            PromptBuilderService promptBuilderService,
            AiResponseParserService aiResponseParserService
    ) {
        this.aiServiceClient = aiServiceClient;
        this.promptBuilderService = promptBuilderService;
        this.aiResponseParserService = aiResponseParserService;
    }

    @Override
    public AiTestCaseGenerationResponse generateTestCases(AiTestCaseGenerationRequest request) {
        validateTestCaseGenerationRequest(request);

        String prompt = promptBuilderService.buildTestCaseGenerationPrompt(request);
        String rawAiResponse = aiServiceClient.generateTestCases(prompt);

        List<TestCaseCreateRequest> generatedTestCases =
                aiResponseParserService.parseGeneratedTestCases(rawAiResponse);

        AiTestCaseGenerationResponse response = new AiTestCaseGenerationResponse();
        response.setMessage("AI test case generation completed");
        response.setPrompt(prompt);
        response.setRawAiResponse(rawAiResponse);
        response.setGeneratedTestCases(generatedTestCases);

        return response;
    }

    @Override
    public AiFailureAnalysisResponse analyzeFailure(AiFailureAnalysisRequest request) {
        if (request == null) {
            throw new BadRequestException("Failure analysis request cannot be null");
        }

        String prompt = promptBuilderService.buildFailureAnalysisPrompt(request);
        String rawAiResponse = aiServiceClient.analyzeFailure(prompt);
        String analysisText = aiResponseParserService.extractText(rawAiResponse);

        AiFailureAnalysisResponse response = new AiFailureAnalysisResponse();
        response.setMessage("AI failure analysis completed");
        response.setAnalysis(analysisText);
        response.setProbableCause("Refer AI analysis");
        response.setRecommendation("Refer AI recommendation");
        response.setRawAiResponse(rawAiResponse);

        return response;
    }

    @Override
    public AiRuleExplanationResponse explainRule(AiRuleExplanationRequest request) {
        if (request == null) {
            throw new BadRequestException("Rule explanation request cannot be null");
        }

        String prompt = promptBuilderService.buildRuleExplanationPrompt(request);
        String rawAiResponse = aiServiceClient.explainRule(prompt);
        String explanationText = aiResponseParserService.extractText(rawAiResponse);

        AiRuleExplanationResponse response = new AiRuleExplanationResponse();
        response.setMessage("AI rule explanation completed");
        response.setExplanation(explanationText);
        response.setBusinessMeaning("Refer AI explanation");
        response.setSuggestedTestCases(List.of("Refer AI explanation"));
        response.setEdgeCases(List.of("Refer AI explanation"));
        response.setRawAiResponse(rawAiResponse);

        return response;
    }

    private void validateTestCaseGenerationRequest(AiTestCaseGenerationRequest request) {
        if (request == null) {
            throw new BadRequestException("AI test case generation request cannot be null");
        }

        if (request.getRuleType() == null || request.getRuleType().isBlank()) {
            throw new BadRequestException("Rule type is required for AI test case generation");
        }

        if (request.getAction() == null) {
            throw new BadRequestException("Rule action is required for AI test case generation");
        }

        if (request.getScenarioId() == null) {
            throw new BadRequestException("Scenario id is required for generated test cases");
        }

        if (request.getNumberOfTestCases() == null || request.getNumberOfTestCases() <= 0) {
            request.setNumberOfTestCases(5);
        }
    }
}