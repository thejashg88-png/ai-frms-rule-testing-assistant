package com.thejas.ai_frms.transaction.dto;

import java.util.List;

public class GenerateHistoryResponse {

    private String ruleType;
    private Long testCaseId;
    private int generatedCount;
    private String message;
    private List<GeneratedTransactionInfo> generatedTransactions;

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public Long getTestCaseId() { return testCaseId; }
    public void setTestCaseId(Long testCaseId) { this.testCaseId = testCaseId; }

    public int getGeneratedCount() { return generatedCount; }
    public void setGeneratedCount(int generatedCount) { this.generatedCount = generatedCount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<GeneratedTransactionInfo> getGeneratedTransactions() { return generatedTransactions; }
    public void setGeneratedTransactions(List<GeneratedTransactionInfo> generatedTransactions) {
        this.generatedTransactions = generatedTransactions;
    }
}