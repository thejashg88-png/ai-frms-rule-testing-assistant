package com.thejas.ai_frms.ai.dto;

import java.util.List;

public class AiRuleExplanationResponse {

    private String message;

    // Legacy field — kept for backward compatibility
    private String explanation;

    // Fields from FastAPI data map
    private String summary;
    private String businessMeaning;
    private String technicalMeaning;
    private String exampleScenario;
    private List<String> riskNotes;
    private String riskLevel;
    private List<String> recommendations;

    // Legacy fields — kept for existing callers
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getBusinessMeaning() {
        return businessMeaning;
    }

    public void setBusinessMeaning(String businessMeaning) {
        this.businessMeaning = businessMeaning;
    }

    public String getTechnicalMeaning() {
        return technicalMeaning;
    }

    public void setTechnicalMeaning(String technicalMeaning) {
        this.technicalMeaning = technicalMeaning;
    }

    public String getExampleScenario() {
        return exampleScenario;
    }

    public void setExampleScenario(String exampleScenario) {
        this.exampleScenario = exampleScenario;
    }

    public List<String> getRiskNotes() {
        return riskNotes;
    }

    public void setRiskNotes(List<String> riskNotes) {
        this.riskNotes = riskNotes;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
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