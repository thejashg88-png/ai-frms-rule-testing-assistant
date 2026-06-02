package com.thejas.ai_frms.ai.dto;

import java.util.Map;

public class AiChatResponse {

    private String reply;
    private Map<String, Object> context;

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}