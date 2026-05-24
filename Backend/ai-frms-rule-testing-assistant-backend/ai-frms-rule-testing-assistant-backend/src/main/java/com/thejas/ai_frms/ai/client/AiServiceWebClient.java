package com.thejas.ai_frms.ai.client;

import com.thejas.ai_frms.common.exception.AiServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Component("aiApiClient")
public class AiServiceWebClient implements AiServiceClient {

    private final WebClient aiWebClient;
    private final String generateTestCasesPath;
    private final String analyzeFailurePath;
    private final String explainRulePath;

    public AiServiceWebClient(
            @Qualifier("aiWebClient") WebClient aiWebClient,
            @Value("${app.integration.ai.generate-test-cases-path:/ai/generate-test-cases}") String generateTestCasesPath,
            @Value("${app.integration.ai.analyze-failure-path:/ai/analyze-failure}") String analyzeFailurePath,
            @Value("${app.integration.ai.explain-rule-path:/ai/explain-rule}") String explainRulePath
    ) {
        this.aiWebClient = aiWebClient;
        this.generateTestCasesPath = generateTestCasesPath;
        this.analyzeFailurePath = analyzeFailurePath;
        this.explainRulePath = explainRulePath;
    }

    @Override
    public String generateTestCases(String prompt) {
        return callAiService(generateTestCasesPath, prompt, "AI test case generation failed");
    }

    @Override
    public String analyzeFailure(String prompt) {
        return callAiService(analyzeFailurePath, prompt, "AI failure analysis failed");
    }

    @Override
    public String explainRule(String prompt) {
        return callAiService(explainRulePath, prompt, "AI rule explanation failed");
    }

    private String callAiService(String path, String prompt, String errorMessage) {
        try {
            return aiWebClient
                    .post()
                    .uri(path)
                    .bodyValue(Map.of("prompt", prompt))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException exception) {
            throw new AiServiceException(
                    errorMessage + ". Status: "
                            + exception.getStatusCode()
                            + ", Response: "
                            + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            throw new AiServiceException(errorMessage + ". Unable to call AI service", exception);
        }
    }
}