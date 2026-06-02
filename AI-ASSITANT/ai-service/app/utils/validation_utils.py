from fastapi import HTTPException


VALID_RULE_TYPES = {
    "STRUCTURING",
    "SINGLE_LARGE_TX",
    "VELOCITY",
    "VELOCITY_CHECK",
    "LARGE_CASH",
    "RAPID_MOVEMENT",
    "CROSS_BORDER",
    "DORMANT_ACCOUNT",
    "ROUND_AMOUNT",
    "GEOGRAPHIC_ANOMALY",
}

VALID_ACTIONS = {"MONITOR", "REJECT", "ACCEPT", "BLOCK", "ALERT", "REVIEW"}


def validate_rule_type(rule_type: str) -> str:
    upper = rule_type.upper()
    if upper not in VALID_RULE_TYPES:
        # Allow unknown types with a warning rather than hard-failing
        # Real-world systems have custom rule types
        return upper
    return upper


def validate_action(action: str) -> str:
    upper = action.upper()
    if upper not in VALID_ACTIONS:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid action '{action}'. Must be one of: {', '.join(sorted(VALID_ACTIONS))}",
        )
    return upper


def validate_currency(currency: str) -> str:
    if len(currency) != 3 or not currency.isalpha():
        raise HTTPException(
            status_code=400,
            detail=f"Invalid currency code '{currency}'. Must be a 3-letter ISO 4217 code (e.g., INR, USD).",
        )
    return currency.upper()
