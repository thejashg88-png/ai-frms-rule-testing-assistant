package com.thejas.ai_frms.testcase.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.thejas.ai_frms.common.enums.RuleAction;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles both string and object forms of expectedResult:
 *   "expectedResult": "PASS"
 *   "expectedResult": {"expectedOutcome": "PASS", "expectedAction": "MONITOR", ...}
 *
 * Class-level @JsonDeserialize on ExpectedResult ensures this is always used.
 * No treeToValue() calls — manual extraction avoids any recursion risk.
 */
public class ExpectedResultDeserializer extends StdDeserializer<ExpectedResult> {

    public ExpectedResultDeserializer() {
        super(ExpectedResult.class);
    }

    @Override
    public ExpectedResult deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node == null || node.isNull()) {
            return null;
        }

        ExpectedResult result = new ExpectedResult();

        // ── String form: "PASS", "MONITOR", "FAIL", etc. ─────────────────────
        if (node.isTextual()) {
            String text = node.asText().trim();
            result.setExpectedOutcome(text);
            try {
                result.setExpectedAction(RuleAction.valueOf(text.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // e.g. "PASS" / "FAIL" — not a RuleAction, stored as outcome only
            }
            return result;
        }

        // ── Object form: {"expectedOutcome": "PASS", "expectedAction": "MONITOR", ...} ──
        if (node.isObject()) {
            result.setExpectedOutcome(textOrNull(node, "expectedOutcome"));
            result.setExpectedEvaluationStatus(textOrNull(node, "expectedEvaluationStatus"));
            result.setExpectedRuleType(textOrNull(node, "expectedRuleType"));
            result.setExpectedRiskLevel(textOrNull(node, "expectedRiskLevel"));
            result.setRemarks(textOrNull(node, "remarks"));

            JsonNode actionNode = node.get("expectedAction");
            if (actionNode != null && !actionNode.isNull()) {
                try {
                    result.setExpectedAction(RuleAction.valueOf(actionNode.asText().trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }

            JsonNode scoreNode = node.get("expectedRiskScore");
            if (scoreNode != null && !scoreNode.isNull()) {
                try {
                    result.setExpectedRiskScore(new BigDecimal(scoreNode.asText()));
                } catch (NumberFormatException ignored) {}
            }

            JsonNode alertsNode = node.get("expectedAlertCodes");
            if (alertsNode != null && alertsNode.isArray()) {
                List<String> codes = new ArrayList<>();
                alertsNode.forEach(n -> codes.add(n.asText()));
                result.setExpectedAlertCodes(codes);
            }

            return result;
        }

        return result;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull()) ? child.asText() : null;
    }
}