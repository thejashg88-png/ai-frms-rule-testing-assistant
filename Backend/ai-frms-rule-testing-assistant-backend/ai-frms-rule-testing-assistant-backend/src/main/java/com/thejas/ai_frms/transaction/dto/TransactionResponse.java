package com.thejas.ai_frms.transaction.dto;

import com.thejas.ai_frms.execution.dto.RuleEvaluationExplanationResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO returned by all transaction endpoints.
 *
 * Contains two independent status dimensions:
 *
 *   Payment status (from acquirer/POS):
 *     transactionStatus — e.g. "SUCCESS", "FAILED", "PENDING"
 *     responseCode      — ISO 8583 response code (e.g. "00" = approved)
 *     responseMessage   — human-readable acquirer message
 *
 *   Risk evaluation status (computed live from active fraud rules):
 *     riskEvaluationStatus — ACCEPT / MONITOR / REJECT
 *     triggeredRuleName    — name of the rule that caused the final action (null if no rule matched)
 *     triggeredRuleType    — type of the triggered rule (e.g. "SINGLE_LARGE_TX")
 *     triggeredAction      — the action of the triggered rule (same as riskEvaluationStatus)
 *     riskReason           — human-readable explanation (e.g. "amount 400000 exceeds threshold 200000")
 *
 * Risk fields are NOT stored in the database — they are re-calculated at query time
 * by evaluating all currently ACTIVE rules against the transaction amount.
 */
public class TransactionResponse {

    private Long transactionId;
    private String rrn;
    private String stan;
    private String serialNumber;
    private String maskedTrack2Data;
    private String tid;
    private String mid;
    private String mccCode;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String responseCode;
    private String responseMessage;
    private String transactionStatus;
    private LocalDateTime transactionTime;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Risk evaluation fields — populated at query time from active rules
    private String riskEvaluationStatus;
    private String triggeredRuleName;
    private String triggeredRuleType;
    private String triggeredAction;
    private String riskReason;

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getRrn() {
        return rrn;
    }

    public void setRrn(String rrn) {
        this.rrn = rrn;
    }

    public String getStan() {
        return stan;
    }

    public void setStan(String stan) {
        this.stan = stan;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getMaskedTrack2Data() {
        return maskedTrack2Data;
    }

    public void setMaskedTrack2Data(String maskedTrack2Data) {
        this.maskedTrack2Data = maskedTrack2Data;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getMccCode() {
        return mccCode;
    }

    public void setMccCode(String mccCode) {
        this.mccCode = mccCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getRiskEvaluationStatus() { return riskEvaluationStatus; }
    public void setRiskEvaluationStatus(String riskEvaluationStatus) { this.riskEvaluationStatus = riskEvaluationStatus; }

    public String getTriggeredRuleName() { return triggeredRuleName; }
    public void setTriggeredRuleName(String triggeredRuleName) { this.triggeredRuleName = triggeredRuleName; }

    public String getTriggeredRuleType() { return triggeredRuleType; }
    public void setTriggeredRuleType(String triggeredRuleType) { this.triggeredRuleType = triggeredRuleType; }

    public String getTriggeredAction() { return triggeredAction; }
    public void setTriggeredAction(String triggeredAction) { this.triggeredAction = triggeredAction; }

    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }

    // Detailed rule evaluation explanation for the highest-severity triggered rule
    private RuleEvaluationExplanationResponse ruleExplanation;
    public RuleEvaluationExplanationResponse getRuleExplanation() { return ruleExplanation; }
    public void setRuleExplanation(RuleEvaluationExplanationResponse ruleExplanation) { this.ruleExplanation = ruleExplanation; }
}