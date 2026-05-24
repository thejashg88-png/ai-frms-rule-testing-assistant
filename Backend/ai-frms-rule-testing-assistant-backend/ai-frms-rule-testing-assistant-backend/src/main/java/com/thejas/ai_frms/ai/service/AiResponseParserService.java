package com.thejas.ai_frms.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejas.ai_frms.testcase.dto.TestCaseCreateRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AiResponseParserService {

    private final ObjectMapper objectMapper;

    public AiResponseParserService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public List<TestCaseCreateRequest> parseGeneratedTestCases(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return Collections.emptyList();
        }

        try {
            String jsonArray = extractJsonArray(aiResponse);

            if (jsonArray == null) {
                return Collections.emptyList();
            }

            return objectMapper.readValue(
                    jsonArray,
                    new TypeReference<List<TestCaseCreateRequest>>() {
                    }
            );
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    public String extractText(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return "";
        }

        try {
            Map<String, Object> responseMap = objectMapper.readValue(
                    aiResponse,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            Object response = responseMap.get("response");
            if (response != null) {
                return response.toString();
            }

            Object content = responseMap.get("content");
            if (content != null) {
                return content.toString();
            }

            Object message = responseMap.get("message");
            if (message != null) {
                return message.toString();
            }

            Object text = responseMap.get("text");
            if (text != null) {
                return text.toString();
            }

            return aiResponse;
        } catch (Exception exception) {
            return aiResponse;
        }
    }

    private String extractJsonArray(String text) {
        int startIndex = text.indexOf('[');
        int endIndex = text.lastIndexOf(']');

        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
            return null;
        }

        return text.substring(startIndex, endIndex + 1);
    }
}