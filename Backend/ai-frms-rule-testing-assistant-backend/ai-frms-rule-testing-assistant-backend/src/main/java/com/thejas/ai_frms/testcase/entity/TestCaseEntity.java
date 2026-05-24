package com.thejas.ai_frms.testcase.entity;

import com.thejas.ai_frms.common.enums.GeneratedBy;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.enums.TestCaseType;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.transaction.entity.TransactionEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "frms_test_cases")
public class TestCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_case_id")
    private Long testCaseId;

    @Column(name = "test_case_name", nullable = false, unique = true, length = 150)
    private String testCaseName;

    @Column(name = "test_case_description", length = 500)
    private String testCaseDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_case_type", nullable = false, length = 40)
    private TestCaseType testCaseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RuleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 30)
    private GeneratedBy generatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private TestScenarioEntity scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private TransactionEntity transaction;

    @Column(name = "input_data", nullable = false, columnDefinition = "TEXT")
    private String inputDataJson;

    @Column(name = "expected_result", nullable = false, columnDefinition = "TEXT")
    private String expectedResultJson;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = RuleStatus.ACTIVE;
        }

        if (this.testCaseType == null) {
            this.testCaseType = TestCaseType.POSITIVE;
        }

        if (this.generatedBy == null) {
            this.generatedBy = GeneratedBy.MANUAL;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Long testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getTestCaseDescription() {
        return testCaseDescription;
    }

    public void setTestCaseDescription(String testCaseDescription) {
        this.testCaseDescription = testCaseDescription;
    }

    public TestCaseType getTestCaseType() {
        return testCaseType;
    }

    public void setTestCaseType(TestCaseType testCaseType) {
        this.testCaseType = testCaseType;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    public GeneratedBy getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(GeneratedBy generatedBy) {
        this.generatedBy = generatedBy;
    }

    public TestScenarioEntity getScenario() {
        return scenario;
    }

    public void setScenario(TestScenarioEntity scenario) {
        this.scenario = scenario;
    }

    public TransactionEntity getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionEntity transaction) {
        this.transaction = transaction;
    }

    public String getInputDataJson() {
        return inputDataJson;
    }

    public void setInputDataJson(String inputDataJson) {
        this.inputDataJson = inputDataJson;
    }

    public String getExpectedResultJson() {
        return expectedResultJson;
    }

    public void setExpectedResultJson(String expectedResultJson) {
        this.expectedResultJson = expectedResultJson;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
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
}