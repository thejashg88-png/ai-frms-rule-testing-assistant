package com.thejas.ai_frms.ai.dto;

import java.util.List;

public class AiFailureAnalysisResponse {

    private String message;

    // Structured fields from FastAPI response
    private String summary;
    private String rootCause;
    private List<String> suggestions;
    private List<String> possibleReasons;
    private List<String> debuggingSteps;
    private String recommendedFix;
    private String riskImpact;
    private Integer confidence;

    // Legacy fields — kept for backward compatibility
    private String analysis;
    private String probableCause;
    private String recommendation;
    private String rawAiResponse;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public List<String> getPossibleReasons() { return possibleReasons; }
    public void setPossibleReasons(List<String> possibleReasons) { this.possibleReasons = possibleReasons; }

    public List<String> getDebuggingSteps() { return debuggingSteps; }
    public void setDebuggingSteps(List<String> debuggingSteps) { this.debuggingSteps = debuggingSteps; }

    public String getRecommendedFix() { return recommendedFix; }
    public void setRecommendedFix(String recommendedFix) { this.recommendedFix = recommendedFix; }

    public String getRiskImpact() { return riskImpact; }
    public void setRiskImpact(String riskImpact) { this.riskImpact = riskImpact; }

    public Integer getConfidence() { return confidence; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    public String getProbableCause() { return probableCause; }
    public void setProbableCause(String probableCause) { this.probableCause = probableCause; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getRawAiResponse() { return rawAiResponse; }
    public void setRawAiResponse(String rawAiResponse) { this.rawAiResponse = rawAiResponse; }
}