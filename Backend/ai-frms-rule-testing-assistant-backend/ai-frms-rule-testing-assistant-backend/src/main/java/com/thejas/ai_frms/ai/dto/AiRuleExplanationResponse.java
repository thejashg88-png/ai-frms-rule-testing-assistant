package com.thejas.ai_frms.ai.dto;

import java.util.List;

public class AiRuleExplanationResponse {

    private String message;
    private String explanation;
    private String businessMeaning;
    private List<String> suggestedTestCases;
    private List<String> edgeCases;
    private String rawAiResponse;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getBusinessMeaning() {
        return businessMeaning;
    }

    public void setBusinessMeaning(String businessMeaning) {
        this.businessMeaning = businessMeaning;
    }

    public List<String> getSuggestedTestCases() {
        return suggestedTestCases;
    }

    public void setSuggestedTestCases(List<String> suggestedTestCases) {
        this.suggestedTestCases = suggestedTestCases;
    }

    public List<String> getEdgeCases() {
        return edgeCases;
    }

    public void setEdgeCases(List<String> edgeCases) {
        this.edgeCases = edgeCases;
    }

    public String getRawAiResponse() {
        return rawAiResponse;
    }

    public void setRawAiResponse(String rawAiResponse) {
        this.rawAiResponse = rawAiResponse;
    }
}