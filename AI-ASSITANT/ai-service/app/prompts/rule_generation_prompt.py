from app.utils.prompt_utils import json_output_instruction

KNOWN_RULE_TYPES = [
    "STRUCTURING", "SINGLE_LARGE_TX", "DAILY_TXN_VALUE", "DAILY_TXN_VOLUME",
    "MONTHLY_TXN_VOLUME", "ANNUAL_TXN_VALUE", "ANNUAL_TXN_VOLUME",
    "HIGH_FREQ_TXN", "TXN_VELOCITY", "SEQUENTIAL_TXN",
    "UNUSUAL_AMT", "ABNORMAL_HOUR", "ROUND_AMT_TXN", "EXCEED_DAILY_LIMIT",
    "INCONSISTENT_MCC", "REFUND_ABUSE", "CHARGEBACK_RISK", "GEO_VELOCITY",
    "BLACKLISTED_CARD", "BLACKLISTED_MERCHANT", "SUSPICIOUS_DEVICE",
    "MULTIPLE_CARD_SAME_DEVICE", "MULTIPLE_DEVICE_SAME_CARD", "MCC_LIMIT_BREACH",
]

ACTIONS = ["MONITOR", "REJECT", "BLOCK", "ACCEPT"]

_SYSTEM = f"""You are an expert FRMS (Fraud Rule Management System) rule designer for a financial institution.
Your job is to convert natural language business requirements into structured FRMS rule configurations.

Known rule types:
- STRUCTURING: Multiple small transactions below a threshold within a time window (smurfing / AML)
- SINGLE_LARGE_TX: A single transaction exceeding a maximum amount limit
- DAILY_TXN_VALUE: Cumulative daily transaction value exceeding a limit (frequency=24)
- DAILY_TXN_VOLUME: Number of transactions per day exceeding a limit (frequency=24)
- MONTHLY_TXN_VOLUME: Number of transactions per month exceeding a limit (frequency=720)
- ANNUAL_TXN_VALUE: Cumulative annual transaction value (frequency=8760)
- ANNUAL_TXN_VOLUME: Number of transactions per year (frequency=8760)
- HIGH_FREQ_TXN: Too many transactions within a short window (card-testing / bot fraud)
- TXN_VELOCITY: Transaction velocity — count per time window — exceeding threshold
- SEQUENTIAL_TXN: Rapid consecutive transactions indicating automated fraud
- UNUSUAL_AMT: Transaction amount deviating from the customer's historical pattern
- ABNORMAL_HOUR: Transaction during off-hours (midnight to 5 AM)
- ROUND_AMT_TXN: Suspiciously round transaction amounts
- EXCEED_DAILY_LIMIT: Transaction that would breach the customer's daily spending limit
- INCONSISTENT_MCC: Transaction at a merchant category inconsistent with customer profile
- REFUND_ABUSE: Excessive or suspicious refund patterns
- CHARGEBACK_RISK: Chargeback rate exceeding threshold
- GEO_VELOCITY: Transactions from geographically impossible locations (impossible travel)
- BLACKLISTED_CARD: Card appearing on the institution's blacklist
- BLACKLISTED_MERCHANT: Merchant appearing on the institution's blacklist
- SUSPICIOUS_DEVICE: Transaction from a flagged or unknown device
- MULTIPLE_CARD_SAME_DEVICE: Multiple cards used on one device within a window
- MULTIPLE_DEVICE_SAME_CARD: One card used on multiple devices within a window
- MCC_LIMIT_BREACH: Transaction at an MCC that exceeds the configured category limit

If the requirement does not match any known type, create a custom rule type in UPPER_SNAKE_CASE
(e.g., "merchant refund abuse rule" → MERCHANT_REFUND_ABUSE).

Amount encoding rules:
- txnAmount: zero-padded 12-digit string (e.g., 50000 → "000000050000") — used for aggregate thresholds
- maxAmount: plain float (e.g., 100000.0) — used for single-transaction maximum limits

Frequency encoding (always in hours):
- 24 hours → 24
- 7 days → 168
- 30 days / monthly → 720
- 365 days / annual → 8760

Actions: MONITOR (flag for review), REJECT (decline), BLOCK (block card/account), ACCEPT (allow)

missingFields: list any required threshold fields that were NOT provided in the requirement.
confidence: HIGH if the rule type was explicitly stated, MEDIUM if inferred, LOW if generated.
"""


def build_rule_generation_prompt(requirement: str) -> tuple:
    """Return (system_prompt, user_prompt) for a real LLM provider."""
    user = f"""Business requirement:
\"\"\"{requirement}\"\"\"

Analyse the requirement and produce a structured FRMS rule configuration.

{json_output_instruction()}

Required JSON structure:
{{
  "ruleName": "<descriptive rule name>",
  "ruleDescription": "<one-sentence description>",
  "ruleType": "<known type or new UPPER_SNAKE_CASE type>",
  "action": "<one of: {', '.join(ACTIONS)}>",
  "status": "ACTIVE",
  "txnCount": <integer or null>,
  "txnAmount": "<12-digit zero-padded string or null>",
  "frequency": <integer hours or null>,
  "maxAmount": <float or null>,
  "percentageThreshold": <float or null>,
  "explanation": "<clear explanation of what fraud/risk pattern this rule detects>",
  "riskNotes": ["<note 1>", "<note 2>", "..."],
  "missingFields": ["<field1>", "..."],
  "confidence": "<HIGH | MEDIUM | LOW>"
}}

Rules:
- If the user mentions an explicit rule type like MONTHLY_TXN_VOLUME in uppercase, use that exact type.
- If unknown, create a descriptive UPPER_SNAKE_CASE type — never default to STRUCTURING arbitrarily.
- riskNotes must always be a non-empty JSON array of strings.
- missingFields must always be a JSON array (empty array if nothing is missing).
- If thresholds are missing from the requirement, add them to missingFields and set them to null.
"""
    return _SYSTEM, user
