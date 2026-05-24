package com.thejas.ai_frms.frmsintegration.client;

import com.thejas.ai_frms.common.exception.FrmsIntegrationException;
import com.thejas.ai_frms.frmsintegration.dto.FrmsRiskEvaluationResponse;
import com.thejas.ai_frms.frmsintegration.dto.FrmsRuleResponse;
import com.thejas.ai_frms.frmsintegration.dto.FrmsTransactionRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component("frmsApiClient")
public class FrmsWebClient implements FrmsClient {

    private final WebClient frmsWebClient;
    private final String evaluateRiskPath;
    private final String ruleByIdPath;

    public FrmsWebClient(
            @Qualifier("frmsWebClient") WebClient frmsWebClient,
            @Value("${app.integration.frms.evaluate-risk-path:/frm/evaluateRisk}") String evaluateRiskPath,
            @Value("${app.integration.frms.rule-by-id-path:/frm/rules/{ruleId}}") String ruleByIdPath
    ) {
        this.frmsWebClient = frmsWebClient;
        this.evaluateRiskPath = evaluateRiskPath;
        this.ruleByIdPath = ruleByIdPath;
    }

    @Override
    public FrmsRiskEvaluationResponse evaluateRisk(FrmsTransactionRequest request) {
        try {
            return frmsWebClient
                    .post()
                    .uri(evaluateRiskPath)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FrmsRiskEvaluationResponse.class)
                    .block();
        } catch (WebClientResponseException exception) {
            throw new FrmsIntegrationException(
                    "FRMS risk evaluation failed. Status: "
                            + exception.getStatusCode()
                            + ", Response: "
                            + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            throw new FrmsIntegrationException(
                    "Unable to call FRMS risk evaluation API",
                    exception
            );
        }
    }

    @Override
    public FrmsRuleResponse getRuleById(Long ruleId) {
        try {
            return frmsWebClient
                    .get()
                    .uri(ruleByIdPath, ruleId)
                    .retrieve()
                    .bodyToMono(FrmsRuleResponse.class)
                    .block();
        } catch (WebClientResponseException exception) {
            throw new FrmsIntegrationException(
                    "FRMS get rule API failed. Status: "
                            + exception.getStatusCode()
                            + ", Response: "
                            + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            throw new FrmsIntegrationException(
                    "Unable to call FRMS get rule API for ruleId: " + ruleId,
                    exception
            );
        }
    }
}