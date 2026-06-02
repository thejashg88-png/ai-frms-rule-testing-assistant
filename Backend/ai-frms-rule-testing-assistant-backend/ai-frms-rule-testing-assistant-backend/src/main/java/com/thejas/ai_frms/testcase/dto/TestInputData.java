package com.thejas.ai_frms.testcase.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction input data used by the rule execution engine when running a test case.
 *
 * Identifier priority for frequency, sequential, and unusual-amount rules:
 *   1. serialNumber — primary identifier (physical card/device serial stored in frms_transactions)
 *   2. track2Data   — magnetic stripe data; also used as normalized cardNumber fallback
 *   3. cardNumber   — normalized (spaces/hyphens removed) and matched against track2Data
 *
 * The engine looks up historical transactions in this priority order to build the evaluation window.
 * If none of the identifiers yield results, rules that need history will return ACCEPT.
 */
public class TestInputData {

    private String rrn;
    private String stan;
    // Primary card/device identifier — used first by HIGH_FREQ, SEQUENTIAL_TXN, UNUSUAL_AMT rules
    private String serialNumber;
    // Magnetic stripe track2 data — fallback identifier if serialNumber lookup returns no results
    private String track2Data;
    private String tid;
    private String mid;
    // Card number (PAN) — normalized and matched against track2Data as a last fallback
    private String cardNumber;
    private String merchantId;
    // Required for INCONSISTENT_MCC rules
    private String mccCode;
    // The transaction amount used by all rule type evaluations
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String channel;
    private String countryCode;
    private String responseCode;
    private String responseMessage;
    private String transactionStatus;
    private LocalDateTime transactionTime;

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

    public String getTrack2Data() {
        return track2Data;
    }

    public void setTrack2Data(String track2Data) {
        this.track2Data = track2Data;
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

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
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
}