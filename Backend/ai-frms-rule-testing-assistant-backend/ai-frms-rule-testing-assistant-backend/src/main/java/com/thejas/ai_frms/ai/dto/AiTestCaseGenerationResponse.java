package com.thejas.ai_frms.ai.dto;

import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;

import java.util.List;

public class AiTestCaseGenerationResponse {

    private String message;
    private String prompt;
    private String rawAiResponse;
    private List<TestCaseCreateRequest> generatedTestCases;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getRawAiResponse() {
        return rawAiResponse;
    }

    public void setRawAiResponse(String rawAiResponse) {
        this.rawAiResponse = rawAiResponse;
    }

    public List<TestCaseCreateRequest> getGeneratedTestCases() {
        return generatedTestCases;
    }

    public void setGeneratedTestCases(List<TestCaseCreateRequest> generatedTestCases) {
        this.generatedTestCases = generatedTestCases;
    }
}