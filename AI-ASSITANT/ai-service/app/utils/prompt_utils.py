from typing import Any, Dict


def build_rule_context(rule_data: Dict[str, Any]) -> str:
    """Format rule parameters into a readable block for LLM prompts."""
    lines = []
    field_labels = {
        "ruleName": "Rule Name",
        "ruleType": "Rule Type",
        "action": "Action",
        "txnCount": "Transaction Count Threshold",
        "txnAmount": "Amount Threshold (zero-padded)",
        "frequency": "Time Window (hours)",
        "maxAmount": "Maximum Amount",
        "percentageThreshold": "Percentage Threshold",
        "description": "Description",
    }
    for key, label in field_labels.items():
        value = rule_data.get(key)
        if value is not None:
            lines.append(f"{label}: {value}")
    return "\n".join(lines)


def json_output_instruction() -> str:
    return (
        "Respond with valid JSON only. "
        "Do not include markdown code fences, explanations, or any text outside the JSON object."
    )
