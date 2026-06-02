package com.thejas.ai_frms.testcase.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.thejas.ai_frms.common.enums.GeneratedBy;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.enums.TestCaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a test case.
 *
 * Input data can be supplied in two ways (service merges them automatically):
 *   1. Nested object: "inputData": { "amount": 5000, "cardNumber": "...", ... }
 *   2. Flat top-level fields: "cardNumber": "...", "amount": "5000", etc.
 *   If both are present, the nested inputData object takes priority.
 *
 * expectedResult must be a structured object, not a plain string.
 * It carries expectedAction (MONITOR/REJECT/ACCEPT) and optionally expectedOutcome (PASS/FAIL).
 * The AI-generated test cases may send a plain string; ExpectedResultDeserializer handles that case.
 */
public class TestCaseCreateRequest {

    @NotBlank(message = "Test case name is required")
    private String testCaseName;

    @JsonAlias("description")
    private String testCaseDescription;

    private TestCaseType testCaseType = TestCaseType.POSITIVE;

    private RuleStatus status = RuleStatus.ACTIVE;

    private GeneratedBy generatedBy = GeneratedBy.MANUAL;

    // Scenario this test case belongs to — required because a test case must be linked to a scenario
    @NotNull(message = "Valid scenarioId is required")
    private Long scenarioId;

    // optional informational fields — not persisted independently
    private String scenarioName;
    private Long ruleId;
    private String ruleName;

    private Long transactionId;

    // optional nested input data — service builds this from flat fields when absent
    private TestInputData inputData;

    // ExpectedResult class carries @JsonDeserialize — handles both string and object forms from AI
    @NotNull(message = "Expected result is required")
    private ExpectedResult expectedResult;

    // ── flat input fields (frontend sends these at top level) ─────────────────
    // These are merged into a TestInputData object by the service when inputData is absent
    private String cardNumber;
    private String amount;
    private String merchantId;
    private String transactionType;
    private String channel;
    private String countryCode;

    // ── flat expected result fields ───────────────────────────────────────────
    // Convenience fields populated by the AI response; merged into expectedResult when set
    private String expectedAction;
    private String expectedRiskLevel;

    private String createdBy;

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getTestCaseName() { return testCaseName; }
    public void setTestCaseName(String testCaseName) { this.testCaseName = testCaseName; }

    public String getTestCaseDescription() { return testCaseDescription; }
    public void setTestCaseDescription(String testCaseDescription) { this.testCaseDescription = testCaseDescription; }

    public TestCaseType getTestCaseType() { return testCaseType; }
    public void setTestCaseType(TestCaseType testCaseType) { this.testCaseType = testCaseType; }

    public RuleStatus getStatus() { return status; }
    public void setStatus(RuleStatus status) { this.status = status; }

    public GeneratedBy getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(GeneratedBy generatedBy) { this.generatedBy = generatedBy; }

    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public TestInputData getInputData() { return inputData; }
    public void setInputData(TestInputData inputData) { this.inputData = inputData; }

    public ExpectedResult getExpectedResult() { return expectedResult; }
    public void setExpectedResult(ExpectedResult expectedResult) { this.expectedResult = expectedResult; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getExpectedAction() { return expectedAction; }
    public void setExpectedAction(String expectedAction) { this.expectedAction = expectedAction; }

    public String getExpectedRiskLevel() { return expectedRiskLevel; }
    public void setExpectedRiskLevel(String expectedRiskLevel) { this.expectedRiskLevel = expectedRiskLevel; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}