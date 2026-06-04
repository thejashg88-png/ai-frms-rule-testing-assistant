package com.thejas.ai_frms.transaction.dto;

public class GenerateHistoryRequest {

    private Long testCaseId;
    private Long ruleId;
    private String ruleType;
    private String cardNumber;
    private String serialNumber;
    private String mid;
    private String tid;
    private String mccCode;
    private String currency;
    private Integer numberOfTransactions;
    private Boolean force = false;

    public Long getTestCaseId() { return testCaseId; }
    public void setTestCaseId(Long testCaseId) { this.testCaseId = testCaseId; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getMid() { return mid; }
    public void setMid(String mid) { this.mid = mid; }

    public String getTid() { return tid; }
    public void setTid(String tid) { this.tid = tid; }

    public String getMccCode() { return mccCode; }
    public void setMccCode(String mccCode) { this.mccCode = mccCode; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getNumberOfTransactions() { return numberOfTransactions; }
    public void setNumberOfTransactions(Integer numberOfTransactions) { this.numberOfTransactions = numberOfTransactions; }

    public Boolean getForce() { return force; }
    public void setForce(Boolean force) { this.force = force; }
}