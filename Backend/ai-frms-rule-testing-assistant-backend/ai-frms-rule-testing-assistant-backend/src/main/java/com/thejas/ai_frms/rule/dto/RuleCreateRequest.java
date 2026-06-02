package com.thejas.ai_frms.rule.dto;

import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.common.enums.RuleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for creating a new fraud detection rule.
 *
 * Not all fields are required — each ruleType uses a specific subset:
 *   SINGLE_LARGE_TX / DAILY_LIMIT / MONTHLY_VOLUME / ANNUAL_VOLUME  → maxAmount
 *   STRUCTURING                                                       → txnAmount
 *   HIGH_FREQUENCY / SEQUENTIAL_TXN                                  → txnCount + frequencyHours
 *   UNUSUAL_AMT                                                       → percentageThreshold
 *   INCONSISTENT_MCC                                                  → mccCode
 */
public class RuleCreateRequest {

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    // Must match a value from RuleTypeConstants (e.g. "SINGLE_LARGE_TX", "HIGH_FREQUENCY")
    @NotBlank(message = "Rule type is required")
    private String ruleType;

    @NotNull(message = "Rule action is required")
    private RuleAction action;

    private RuleStatus status = RuleStatus.ACTIVE;

    // Required only for INCONSISTENT_MCC rules — the expected Merchant Category Code
    private String mccCode;

    // Number of transactions that triggers HIGH_FREQUENCY and SEQUENTIAL_TXN rules
    @Min(value = 1, message = "Transaction count must be greater than 0")
    private Integer txnCount;

    // Time window in hours for frequency-based rules. Monthly = 720, Annual = 8760
    @Min(value = 1, message = "Frequency hours must be greater than 0")
    private Integer frequencyHours;

    // Used by STRUCTURING rules — triggers when transaction amount is BELOW this value
    @DecimalMin(value = "0.0", inclusive = false, message = "Transaction amount must be greater than 0")
    private BigDecimal txnAmount;

    // Used by SINGLE_LARGE_TX, DAILY_LIMIT, MONTHLY_VOLUME — triggers when amount EXCEEDS this value
    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum amount must be greater than 0")
    private BigDecimal maxAmount;

    // Used by UNUSUAL_AMT — triggers when amount > (baselineAvg + baselineAvg * threshold / 100)
    @DecimalMin(value = "0.0", inclusive = false, message = "Percentage threshold must be greater than 0")
    private BigDecimal percentageThreshold;

    private String ruleDescription;

    private String createdBy;

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public RuleAction getAction() {
        return action;
    }

    public void setAction(RuleAction action) {
        this.action = action;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    public String getMccCode() {
        return mccCode;
    }

    public void setMccCode(String mccCode) {
        this.mccCode = mccCode;
    }

    public Integer getTxnCount() {
        return txnCount;
    }

    public void setTxnCount(Integer txnCount) {
        this.txnCount = txnCount;
    }

    public Integer getFrequencyHours() {
        return frequencyHours;
    }

    public void setFrequencyHours(Integer frequencyHours) {
        this.frequencyHours = frequencyHours;
    }

    public BigDecimal getTxnAmount() {
        return txnAmount;
    }

    public void setTxnAmount(BigDecimal txnAmount) {
        this.txnAmount = txnAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public BigDecimal getPercentageThreshold() {
        return percentageThreshold;
    }

    public void setPercentageThreshold(BigDecimal percentageThreshold) {
        this.percentageThreshold = percentageThreshold;
    }

    public String getRuleDescription() {
        return ruleDescription;
    }

    public void setRuleDescription(String ruleDescription) {
        this.ruleDescription = ruleDescription;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}