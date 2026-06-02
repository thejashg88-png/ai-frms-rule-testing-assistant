package com.thejas.ai_frms.ai.dto;

import java.util.Map;

public class AiChatRequest {

    private String message;
    private Map<String, Object> context;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}