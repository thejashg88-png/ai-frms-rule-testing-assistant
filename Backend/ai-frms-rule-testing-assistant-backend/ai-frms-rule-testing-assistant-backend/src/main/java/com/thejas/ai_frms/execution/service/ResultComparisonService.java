package com.thejas.ai_frms.execution.service;

import com.thejas.ai_frms.execution.dto.ComparisonResult;
import com.thejas.ai_frms.execution.dto.ExecutionTraceStepResponse;
import com.thejas.ai_frms.testcase.dto.ExpectedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ResultComparisonService {

    private static final Logger log = LoggerFactory.getLogger(ResultComparisonService.class);

    public ComparisonResult compare(ExpectedResult expectedResult, ComparisonResult actualResult) {
        ComparisonResult comparisonResult = new ComparisonResult();

        // Carry forward expected fields
        comparisonResult.setExpectedAction(expectedResult != null ? expectedResult.getExpectedAction() : null);
        comparisonResult.setExpectedEvaluationStatus(expectedResult != null ? expectedResult.getExpectedEvaluationStatus() : null);
        comparisonResult.setExpectedRuleType(expectedResult != null ? expectedResult.getExpectedRuleType() : null);
        comparisonResult.setExpectedAlertCodes(expectedResult != null ? expectedResult.getExpectedAlertCodes() : null);
        comparisonResult.setExpectedRiskScore(expectedResult != null ? expectedResult.getExpectedRiskScore() : null);
        comparisonResult.setExpectedOutcome(expectedResult != null ? expectedResult.getExpectedOutcome() : null);

        // Carry forward actual fields
        comparisonResult.setActualAction(actualResult.getActualAction());
        comparisonResult.setActualEvaluationStatus(actualResult.getActualEvaluationStatus());
        comparisonResult.setActualRuleType(actualResult.getActualRuleType());
        comparisonResult.setActualAlertCodes(actualResult.getActualAlertCodes());
        comparisonResult.setActualRiskScore(actualResult.getActualRiskScore());
        comparisonResult.setRuleType(actualResult.getRuleType());
        comparisonResult.setInputAmount(actualResult.getInputAmount());
        comparisonResult.setEngineNote(actualResult.getEngineNote());
        // Carry forward the rule explanation built by the engine
        comparisonResult.setRuleExplanation(actualResult.getRuleExplanation());

        log.info("[EXECUTION] Expected action={}", comparisonResult.getExpectedAction());
        log.info("[EXECUTION] Actual action={}", comparisonResult.getActualAction());

        // ── Comparison logic ─────────────────────────────────────────────────
        // expectedAction null means "not specified" — skip that check (don't count as mismatch)
        boolean actionMatched = comparisonResult.getExpectedAction() == null
                || Objects.equals(comparisonResult.getExpectedAction(), comparisonResult.getActualAction());

        // expectedEvaluationStatus blank/null means "not specified" — skip
        boolean evaluationStatusMatched = isBlank(comparisonResult.getExpectedEvaluationStatus())
                || Objects.equals(comparisonResult.getExpectedEvaluationStatus(), comparisonResult.getActualEvaluationStatus());

        // expectedRuleType blank/null means "not specified" — skip
        boolean ruleTypeMatched = isBlank(comparisonResult.getExpectedRuleType())
                || Objects.equals(comparisonResult.getExpectedRuleType(), comparisonResult.getActualRuleType());

        boolean matched = actionMatched && evaluationStatusMatched && ruleTypeMatched;
        comparisonResult.setMatched(matched);

        // ── Build failure reason ─────────────────────────────────────────────
        if (matched) {
            comparisonResult.setMessage("Test passed: actual result matched expected result");
            log.info("[EXECUTION] Final result=PASS");
        } else {
            List<String> mismatches = new ArrayList<>();

            if (!actionMatched) {
                mismatches.add("Expected action "
                        + comparisonResult.getExpectedAction().name()
                        + " but actual action "
                        + (comparisonResult.getActualAction() != null ? comparisonResult.getActualAction().name() : "null"));
            }

            if (!evaluationStatusMatched) {
                mismatches.add("Expected evaluation status "
                        + comparisonResult.getExpectedEvaluationStatus()
                        + " but actual "
                        + comparisonResult.getActualEvaluationStatus());
            }

            if (!ruleTypeMatched) {
                mismatches.add("Expected rule type "
                        + comparisonResult.getExpectedRuleType()
                        + " but actual "
                        + comparisonResult.getActualRuleType());
            }

            String failureReason = String.join("; ", mismatches);

            // Append engine-level context (e.g., baseline missing for UNUSUAL_AMT)
            if (comparisonResult.getEngineNote() != null && !comparisonResult.getEngineNote().isBlank()) {
                failureReason = failureReason + ". " + comparisonResult.getEngineNote();
            }

            comparisonResult.setFailureReason(failureReason);
            comparisonResult.setMessage("Test failed: " + failureReason);
            log.info("[EXECUTION] Final result=FAILED — {}", failureReason);
        }

        // Append final comparison and result steps to trace from engine
        try {
            List<ExecutionTraceStepResponse> trace = actualResult.getExecutionTrace() != null
                    ? new ArrayList<>(actualResult.getExecutionTrace())
                    : new ArrayList<>();
            appendComparisonSteps(trace, comparisonResult, matched);
            comparisonResult.setExecutionTrace(trace);
        } catch (Exception e) {
            log.warn("[EXECUTION TRACE] failed to append comparison steps: {}", e.getMessage());
        }

        return comparisonResult;
    }

    private void appendComparisonSteps(List<ExecutionTraceStepResponse> trace,
            ComparisonResult result, boolean matched) {
        int nextStep = trace.size() + 1;
        String ruleType = result.getRuleType() != null ? result.getRuleType() : "";

        String expected = result.getExpectedAction() != null
                ? result.getExpectedAction().name() : "not specified";
        String actual = result.getActualAction() != null
                ? result.getActualAction().name() : "null";

        ExecutionTraceStepResponse compareStep = new ExecutionTraceStepResponse();
        compareStep.setStepNumber(nextStep++);
        compareStep.setTitle("Compared expected and actual action");
        compareStep.setDetail("Expected action=" + expected + ", actual action=" + actual);
        compareStep.setStatus(matched ? "SUCCESS" : "FAILED");
        compareStep.setRuleType(ruleType);
        compareStep.setTimestamp(LocalDateTime.now().toString());
        trace.add(compareStep);

        ExecutionTraceStepResponse finalStep = new ExecutionTraceStepResponse();
        finalStep.setStepNumber(nextStep);
        finalStep.setTitle("Final execution result");
        finalStep.setDetail("Result = " + (matched ? "PASSED" : "FAILED"));
        finalStep.setStatus(matched ? "SUCCESS" : "FAILED");
        finalStep.setRuleType(ruleType);
        finalStep.setTimestamp(LocalDateTime.now().toString());
        trace.add(finalStep);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}