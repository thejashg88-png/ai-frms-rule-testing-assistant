package com.thejas.ai_frms.execution.entity;

import com.thejas.ai_frms.common.enums.ExecutionStatus;
import com.thejas.ai_frms.common.enums.RuleAction;
import com.thejas.ai_frms.testcase.entity.TestCaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "frms_test_execution_results")
public class TestExecutionResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private TestExecutionEntity execution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCaseEntity testCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 40)
    private ExecutionStatus resultStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_action", length = 40)
    private RuleAction expectedAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "actual_action", length = 40)
    private RuleAction actualAction;

    @Column(name = "expected_evaluation_status", length = 80)
    private String expectedEvaluationStatus;

    @Column(name = "actual_evaluation_status", length = 80)
    private String actualEvaluationStatus;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "expected_outcome", length = 50)
    private String expectedOutcome;

    @Column(name = "comparison_result", columnDefinition = "TEXT")
    private String comparisonResultJson;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @PrePersist
    public void prePersist() {
        if (this.executedAt == null) {
            this.executedAt = LocalDateTime.now();
        }

        if (this.resultStatus == null) {
            this.resultStatus = ExecutionStatus.PENDING;
        }
    }
}