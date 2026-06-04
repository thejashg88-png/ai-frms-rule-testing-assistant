import random
import re
import string
from abc import ABC, abstractmethod
from datetime import datetime, timedelta

from loguru import logger

from app.config.settings import get_settings
from app.models.request_models import (
    AnalyzeFailureRequest,
    ExplainRuleRequest,
    GenerateRuleRequest,
    GenerateTestCasesRequest,
    GenerateTransactionRequest,
)
from app.utils.json_utils import parse_llm_json


# ---------------------------------------------------------------------------
# Module: llm_service.py
#
# Implements the LLM provider abstraction layer:
#   BaseLLMProvider     — abstract interface all providers must implement
#   MockLLMProvider     — deterministic, no API key required; used in dev/test
#   GroqLLMProvider     — real LLM via Groq's OpenAI-compatible API (AsyncOpenAI)
#   OpenAIProvider      — placeholder, not yet implemented
#   AnthropicProvider   — placeholder, not yet implemented
#   get_llm_provider()  — factory function, reads AI_PROVIDER from settings
#
# SECURITY: API keys (GROQ_API_KEY, etc.) must NEVER be logged, returned
# in responses, or included in error messages. Only log provider name/model.
# ---------------------------------------------------------------------------


class LLMProviderUnavailableError(Exception):
    """Raised when a real LLM provider fails and AI_FALLBACK_TO_MOCK=false."""


# ---------------------------------------------------------------------------
# Abstract base
# ---------------------------------------------------------------------------

class BaseLLMProvider(ABC):
    @abstractmethod
    async def generate_test_cases(self, request: GenerateTestCasesRequest) -> dict: ...

    @abstractmethod
    async def explain_rule(self, request: ExplainRuleRequest) -> dict: ...

    @abstractmethod
    async def analyze_failure(self, request: AnalyzeFailureRequest) -> dict: ...

    @abstractmethod
    async def generate_transaction(self, request: GenerateTransactionRequest) -> dict: ...

    @abstractmethod
    async def generate_rule_from_requirement(self, request: GenerateRuleRequest) -> dict: ...

    @abstractmethod
    async def chat(self, message: str, context: dict) -> str: ...


# ---------------------------------------------------------------------------
# Rule-generation metadata  (consumed by MockLLMProvider._rule_from_requirement)
# ---------------------------------------------------------------------------

# Implied frequency (hours) for periodic rule types whose window is part of the name
_FREQUENCY_DEFAULTS: dict = {
    "DAILY_TXN_VALUE": 24,
    "DAILY_TXN_VOLUME": 24,
    "EXCEED_DAILY_LIMIT": 24,
    "MONTHLY_TXN_VOLUME": 720,
    "ANNUAL_TXN_VALUE": 8760,
    "ANNUAL_TXN_VOLUME": 8760,
}

# Catalog: rule_type → (display_name, default_action, relevant_fields, amount_type, domain_description)
# amount_type: "txn" → txnAmount (zero-padded str), "max" → maxAmount (float), "none" → neither
_RULE_CATALOG: dict = {
    "STRUCTURING":              ("Structuring Transaction Monitor Rule",        "MONITOR", ["txnCount", "txnAmount", "frequency"], "txn",  "multiple small transactions below a threshold within a rolling time window to evade reporting limits (smurfing / money laundering)"),
    "SINGLE_LARGE_TX":          ("Single Large Transaction Rule",               "REJECT",  ["maxAmount"],                          "max",  "a single transaction whose amount exceeds the configured maximum limit"),
    "DAILY_TXN_VALUE":          ("Daily Transaction Value Limit Rule",          "MONITOR", ["txnAmount"],                          "txn",  "cumulative transaction value within a 24-hour rolling window exceeding a threshold"),
    "DAILY_TXN_VOLUME":         ("Daily Transaction Volume Rule",               "MONITOR", ["txnCount"],                           "none", "number of transactions within a 24-hour window exceeding a threshold"),
    "MONTHLY_TXN_VOLUME":       ("Monthly Transaction Volume Rule",             "MONITOR", ["txnCount"],                           "none", "number of transactions within a 30-day rolling window exceeding a threshold"),
    "ANNUAL_TXN_VALUE":         ("Annual Transaction Value Rule",               "MONITOR", ["txnAmount"],                          "txn",  "cumulative transaction value within a 365-day window exceeding a threshold"),
    "ANNUAL_TXN_VOLUME":        ("Annual Transaction Volume Rule",              "MONITOR", ["txnCount"],                           "none", "number of transactions within a 365-day window exceeding a threshold"),
    "HIGH_FREQ_TXN":            ("High Frequency Transaction Rule",             "MONITOR", ["txnCount", "frequency"],              "none", "abnormally high number of transactions within a short time window (card-testing / bot fraud)"),
    "TXN_VELOCITY":             ("Transaction Velocity Rule",                   "MONITOR", ["txnCount", "frequency"],              "none", "transaction velocity — count per time window — exceeding a configured threshold"),
    "SEQUENTIAL_TXN":           ("Sequential Transaction Rule",                 "MONITOR", ["txnCount", "frequency"],              "none", "consecutive transactions in rapid succession indicating automated or scripted fraud"),
    "UNUSUAL_AMT":              ("Unusual Amount Transaction Rule",             "MONITOR", ["percentageThreshold"],                "none", "transaction amount deviating significantly from the customer's historical spending pattern"),
    "ABNORMAL_HOUR":            ("Abnormal Hour Transaction Rule",              "MONITOR", [],                                     "none", "transaction occurring during off-hours when legitimate activity is statistically rare"),
    "ROUND_AMT_TXN":            ("Round Amount Transaction Rule",               "MONITOR", ["txnAmount"],                         "txn",  "transaction with a suspiciously round amount — associated with cash-back fraud and money-mule activity"),
    "EXCEED_DAILY_LIMIT":       ("Exceed Daily Limit Rule",                     "REJECT",  ["maxAmount"],                         "max",  "transaction that would cause the customer to exceed their configured daily spending limit"),
    "INCONSISTENT_MCC":         ("Inconsistent MCC Rule",                       "MONITOR", [],                                     "none", "transaction at a merchant category code inconsistent with the customer's usual spending profile"),
    "REFUND_ABUSE":             ("Refund Abuse Rule",                           "MONITOR", ["txnCount", "frequency"],              "none", "excessive refund requests or refund amounts inconsistent with normal purchase patterns"),
    "CHARGEBACK_RISK":          ("Chargeback Risk Rule",                        "MONITOR", ["percentageThreshold"],                "none", "merchant or customer chargeback rate exceeding an acceptable threshold"),
    "GEO_VELOCITY":             ("Geographic Velocity Rule",                    "MONITOR", ["frequency"],                         "none", "transactions from geographically impossible locations within a short time window (impossible travel)"),
    "BLACKLISTED_CARD":         ("Blacklisted Card Rule",                       "REJECT",  [],                                     "none", "transaction attempted by a card appearing on the institution's blacklist"),
    "BLACKLISTED_MERCHANT":     ("Blacklisted Merchant Rule",                   "REJECT",  [],                                     "none", "transaction at a merchant appearing on the institution's blacklist"),
    "SUSPICIOUS_DEVICE":        ("Suspicious Device Rule",                      "MONITOR", [],                                     "none", "transaction initiated from a device flagged as suspicious or associated with previous fraud"),
    "MULTIPLE_CARD_SAME_DEVICE":("Multiple Cards on Same Device Rule",          "MONITOR", ["txnCount", "frequency"],              "none", "multiple different cards used on the same device within a time window — indicating a fraud ring"),
    "MULTIPLE_DEVICE_SAME_CARD":("Multiple Devices Same Card Rule",             "MONITOR", ["txnCount", "frequency"],              "none", "a single card used across multiple different devices — indicating card-sharing or credential theft"),
    "MCC_LIMIT_BREACH":         ("MCC Limit Breach Rule",                       "REJECT",  ["maxAmount"],                         "max",  "transaction at a specific merchant category code that exceeds the configured category spending limit"),
}

# Risk notes per rule type
_RISK_NOTES: dict = {
    "STRUCTURING": [
        "Useful for detecting split transactions designed to stay below regulatory reporting thresholds",
        "Time window should be extended to 48–72 hours to catch structuring spread across multiple days",
        "Cross-account structuring (family members, associates) requires entity-linkage analysis",
        "SAR filing obligations under PMLA apply when structuring is confirmed — coordinate with compliance",
        "Combine with cumulative amount monitoring for higher detection accuracy",
    ],
    "SINGLE_LARGE_TX": [
        "Threshold must be reviewed periodically against inflation and evolving transaction value trends",
        "Too-low a threshold causes false positives for legitimate high-value business transactions",
        "Whitelist rule recommended for known high-value merchants (B2B, auto dealers, real estate)",
        "Ensure amount is parsed consistently across all channels (ATM, POS, UPI, online)",
        "Pair with customer segment profiles — different thresholds for retail vs. business accounts",
    ],
    "DAILY_TXN_VALUE": [
        "Aggregate across all channels (ATM, POS, UPI, NEFT) for accurate daily total",
        "Use a rolling 24-hour window rather than a calendar-day reset to prevent end-of-day burst fraud",
        "Threshold should be calibrated against the 90th-percentile customer daily spend",
        "Exclude same-customer inter-bank transfers from the daily total to avoid false positives",
        "Consider tiered thresholds by customer segment (retail vs. corporate accounts)",
    ],
    "DAILY_TXN_VOLUME": [
        "Calibrate count threshold against normal daily transaction patterns for your customer base",
        "High-volume merchants and businesses may legitimately exceed this threshold — consider whitelisting",
        "Combine with average transaction amount to distinguish bulk-small from bulk-large patterns",
        "Use a rolling 24-hour window to prevent burst exploitation at day boundaries",
        "Exclude recurring automated transactions (subscriptions, standing instructions) from the count",
    ],
    "MONTHLY_TXN_VOLUME": [
        "Baseline monthly counts should be computed per customer segment for accurate thresholds",
        "Monthly limits are effective for detecting dormant account activation abuse",
        "Combine with transaction value monitoring for comprehensive monthly exposure controls",
        "Frequency defaults to 720 hours (30 days) — use a rolling window, not a calendar-month reset",
        "Review threshold quarterly as customer spending behaviours change seasonally",
    ],
    "ANNUAL_TXN_VALUE": [
        "Annual limits align with income-tax reporting thresholds — coordinate with the compliance team",
        "Use a rolling 8760-hour (365-day) window, not a calendar-year reset",
        "Useful for detecting gradual money-laundering spread across a full year",
        "Cross-reference against declared income for high-risk customers undergoing enhanced due diligence",
        "Alert the compliance team before the annual limit is reached, not after it is breached",
    ],
    "ANNUAL_TXN_VOLUME": [
        "High annual volume may indicate professional money-mule activity",
        "Baseline annual counts should be drawn from customer segment historical data",
        "Flag accounts where annual volume is 3× or more above the segment median",
        "Coordinate with KYC refresh cycles — high-volume accounts warrant enhanced due diligence",
        "Exclude institutional and business accounts that may have legitimately high annual volumes",
    ],
    "HIGH_FREQ_TXN": [
        "Card-testing attacks typically use very small amounts — combine with a minimum-amount filter",
        "Counter must reset on a rolling window, not a fixed-time boundary reset",
        "High-frequency merchants (market vendors, fuel stations) may need whitelisting",
        "Cross-channel velocity requires a unified counter across ATM, POS, UPI, and online channels",
        "Pair with geo-velocity check to detect bot-driven distributed fraud patterns",
    ],
    "TXN_VELOCITY": [
        "Velocity thresholds should be set per card type (debit vs. credit vs. prepaid)",
        "Velocity counters must be maintained in low-latency storage (e.g. Redis) for real-time enforcement",
        "Evaluate both per-card and per-account velocity for comprehensive coverage",
        "Combine velocity check with BIN-level risk scoring for enhanced accuracy",
        "Review velocity limits after major fraud incidents to ensure thresholds remain appropriate",
    ],
    "SEQUENTIAL_TXN": [
        "Define 'sequential' precisely — time gap between transactions (e.g. < 30 seconds)",
        "Legitimate tap-and-pay users may trigger this rule at transport terminals",
        "Combine with identical-amount check — sequential identical amounts are higher risk",
        "ATM card-trapping attacks often produce sequential withdrawal patterns",
        "Consider MCC context — sequential transactions at the same merchant are less suspicious",
    ],
    "UNUSUAL_AMT": [
        "Requires at least 30 days of transaction history to establish a reliable baseline",
        "Percentage deviation threshold may need tuning per customer segment",
        "Exclude known periodic large payments (rent, insurance premiums) from the baseline",
        "New customers have no history — apply a conservative default limit for the first 30 days",
        "Retrain baseline models quarterly to account for lifestyle and spending changes",
    ],
    "ABNORMAL_HOUR": [
        "Adjust the off-hour window based on customer demographics and time zones",
        "International travellers and night-shift workers will generate false positives",
        "Overseas transactions during off-hours are statistically higher risk — combine with geo check",
        "Use MONITOR rather than REJECT initially to avoid blocking genuine customers",
        "Review the time window definition to align with local market activity patterns",
    ],
    "ROUND_AMT_TXN": [
        "Define 'round' precisely — multiples of 1,000 or 10,000 depending on risk appetite",
        "Apply only above the configured threshold to avoid flooding alerts on small round amounts",
        "Round amounts are normal for some MCCs (utilities, tolls, subscriptions) — add MCC exclusions",
        "High volume of round-amount alerts may indicate a widespread cash-conversion scheme",
        "Tune the rounding threshold based on alert-to-SAR conversion rates from prior periods",
    ],
    "EXCEED_DAILY_LIMIT": [
        "Daily limit must be communicated clearly to customers to manage expectations",
        "Enforce in real-time, not batch — batch enforcement allows temporary limit breaches",
        "Consider a soft-limit warning before the hard REJECT to improve customer experience",
        "Business accounts typically require higher daily limits — implement tiered limits",
        "Temporary limit overrides for travel or high-value purchases should be logged and reviewed",
    ],
    "INCONSISTENT_MCC": [
        "Build spending profiles over at least 90 days before activating this rule",
        "New customers and customers with diverse spending will generate initial false positives",
        "MCC inconsistency alone is a weak signal — combine with other risk indicators",
        "Whitelisting for occasional legitimate MCC outliers reduces alert fatigue",
        "Fraudsters increasingly mirror the cardholder's MCC profile — consider additional signals",
    ],
    "REFUND_ABUSE": [
        "Track refund-to-purchase ratio per customer — normal is below 5–10%",
        "Coordinate with the merchant acquiring team to identify merchant-side refund manipulation",
        "Automated refund schemes often involve collusion between cardholder and merchant",
        "Cross-reference refund amounts against original purchase transactions for consistency",
        "Alert the chargeback team when refund abuse is detected to prevent double-recovery fraud",
    ],
    "CHARGEBACK_RISK": [
        "VISA/Mastercard thresholds (1% chargeback rate) should inform this rule's configuration",
        "High-chargeback merchants may be enrolled in card network monitoring programs",
        "Coordinate chargeback risk alerts with the merchant risk and acquiring teams",
        "First-party fraud (friendly fraud) is a major driver of chargebacks — investigate patterns",
        "Review chargeback rates monthly and escalate merchants exceeding network thresholds",
    ],
    "GEO_VELOCITY": [
        "Minimum travel time between locations must be calibrated to real-world transport speeds",
        "VPN and proxy usage can artificially trigger geo-velocity rules — combine with device checks",
        "International roaming and card-present vs. card-not-present location discrepancy is common",
        "Consider a grace period for the first transaction after a geo-velocity alert before blocking",
        "Airlines and border cities have legitimate rapid location changes — tune accordingly",
    ],
    "BLACKLISTED_CARD": [
        "Maintain blacklist synchronization with card network bulletins and internal fraud records",
        "Blacklist entries should have expiry dates to prevent permanent false positives",
        "Lost/stolen card reports must be processed within minutes to prevent fraudulent misuse",
        "Card numbers should be stored as tokenized hashes, not plain text, in the blacklist",
        "Coordinate with issuer fraud teams for cross-institution blacklist sharing where permitted",
    ],
    "BLACKLISTED_MERCHANT": [
        "Blacklist merchants based on confirmed fraud, regulatory sanctions, or network disqualification",
        "Merchant blacklist entries should reference the underlying reason for auditability",
        "Differentiate between full-block and enhanced-monitoring categories in the blacklist",
        "Coordinate with payment network bulletins for merchant MATCH-list entries",
        "Review and refresh the merchant blacklist monthly with input from fraud operations",
    ],
    "SUSPICIOUS_DEVICE": [
        "Device fingerprinting must be consistent across app versions — regression-test after updates",
        "Shared or public devices (internet cafés, hotels) will generate false positives",
        "Combine device risk score with behavioural biometrics for improved accuracy",
        "Rooted / jailbroken devices represent higher risk and should be scored accordingly",
        "Ensure device data is collected at every login and transaction — not just at onboarding",
    ],
    "MULTIPLE_CARD_SAME_DEVICE": [
        "A device used by multiple family members may legitimately have several cards",
        "Card-on-file aggregator apps may appear as one device for multiple cardholders",
        "Define the time window carefully — a 30-day window is more meaningful than 1 hour",
        "Fraud rings often use a single device to test dozens of stolen card credentials",
        "Cross-reference with card-not-present transaction patterns for higher confidence",
    ],
    "MULTIPLE_DEVICE_SAME_CARD": [
        "Card-sharing within a household is legitimate but should be limited to 2–3 devices",
        "Credential-stuffing attacks result in a single compromised card used on many devices",
        "OTP-based step-up authentication should be triggered when a new device is detected",
        "Track device registration dates — sudden appearance of many devices is a strong fraud signal",
        "Coordinate with card network device intelligence services for device risk scoring",
    ],
    "MCC_LIMIT_BREACH": [
        "Different MCCs have very different typical transaction sizes — calibrate limits per MCC",
        "Coordinate MCC limits with the corporate expense policy for commercial cards",
        "Gambling and adult-entertainment MCCs warrant lower limits and enhanced monitoring",
        "MCC limit breaches at high-risk MCCs (e.g. 7995 gambling) may have regulatory implications",
        "Review MCC limits annually against fraud loss data segmented by merchant category",
    ],
}

# Priority-ordered keyword → rule type matching
_KEYWORD_MAP: list = [
    ("STRUCTURING",              ["structuring", "smurfing", "split transaction", "multiple small transaction", "multiple transactions below", "multiple transactions under", "transactions below threshold"]),
    ("SINGLE_LARGE_TX",          ["single large", "large transaction", "single transaction above", "single transaction over", "single transaction exceeding", "high value transaction", "high-value transaction", "transaction above", "one large transaction"]),
    ("MONTHLY_TXN_VOLUME",       ["monthly transaction volume", "monthly volume", "monthly transaction count", "monthly txn", "per month transaction", "transactions per month", "monthly limit", "monthly transaction"]),
    ("ANNUAL_TXN_VALUE",         ["annual transaction value", "annual value", "yearly transaction value", "yearly value", "annual txn value", "per year value"]),
    ("ANNUAL_TXN_VOLUME",        ["annual transaction volume", "annual volume", "yearly transaction count", "yearly volume", "annual txn volume", "per year transaction", "annual transaction count"]),
    ("DAILY_TXN_VALUE",          ["daily transaction value", "daily total", "daily aggregate", "daily value", "per day value", "daily txn value", "total daily value", "daily amount limit", "daily amount"]),
    ("DAILY_TXN_VOLUME",         ["daily transaction volume", "daily volume", "daily transaction count", "daily count", "transactions per day", "daily txn volume", "daily txn count", "daily transaction limit"]),
    ("EXCEED_DAILY_LIMIT",       ["exceed daily limit", "daily limit exceeded", "over daily limit", "exceed spending limit", "daily spending limit", "daily limit breach"]),
    ("HIGH_FREQ_TXN",            ["high frequency", "high-frequency", "rapid transaction", "frequent transaction", "many transactions in", "too many transaction"]),
    ("TXN_VELOCITY",             ["transaction velocity", "txn velocity", "velocity check", "velocity limit", "velocity rule", "velocity monitor"]),
    ("SEQUENTIAL_TXN",           ["sequential transaction", "consecutive transaction", "sequential payment", "back-to-back transaction", "rapid consecutive"]),
    ("ABNORMAL_HOUR",            ["abnormal hour", "off hour", "off-hour", "unusual hour", "unusual time", "midnight", "late night", "early morning", "night transaction", "wee hour", "2 am", "3 am", "after midnight", "off hours"]),
    ("ROUND_AMT_TXN",            ["round amount", "round number", "round figure", "round transaction", "suspiciously round", "round value"]),
    ("UNUSUAL_AMT",              ["unusual amount", "abnormal amount", "irregular amount", "suspicious amount", "amount deviation", "deviat", "anomalous amount", "amount anomaly", "amount outlier"]),
    ("INCONSISTENT_MCC",         ["inconsistent mcc", "unusual merchant category", "mcc mismatch", "merchant category mismatch", "mcc inconsistency", "inconsistent merchant"]),
    ("REFUND_ABUSE",             ["refund abuse", "merchant refund", "refund fraud", "excessive refund", "refund manipulation", "refund rate", "abuse refund", "refund limit"]),
    ("CHARGEBACK_RISK",          ["chargeback", "charge back", "dispute risk", "chargeback risk", "chargeback rate", "dispute rate"]),
    ("GEO_VELOCITY",             ["geo velocity", "geo-velocity", "geographic velocity", "impossible travel", "location velocity", "different location", "impossible location", "cross-location", "two location"]),
    ("BLACKLISTED_CARD",         ["blacklisted card", "blacklist card", "blocked card", "card blacklist", "card on blacklist", "denied card"]),
    ("BLACKLISTED_MERCHANT",     ["blacklisted merchant", "blacklist merchant", "blocked merchant", "merchant blacklist", "merchant on blacklist", "denied merchant"]),
    ("SUSPICIOUS_DEVICE",        ["suspicious device", "device fraud", "device anomaly", "unknown device", "untrusted device", "new device risk", "risky device"]),
    ("MULTIPLE_CARD_SAME_DEVICE",["multiple card same device", "multiple cards on same device", "many cards same device", "cards on single device", "multiple cards one device", "several cards same device"]),
    ("MULTIPLE_DEVICE_SAME_CARD",["multiple device same card", "multiple devices on same card", "many devices same card", "devices on single card", "multiple devices one card", "several devices same card"]),
    ("MCC_LIMIT_BREACH",         ["mcc limit", "merchant category limit", "mcc breach", "mcc limit breach", "merchant category code limit", "mcc spending limit"]),
]


# ---------------------------------------------------------------------------
# Deterministic test case builder — always returns ACCEPT / MONITOR / REJECT
# ---------------------------------------------------------------------------

def _txn(n: int, amount: int, extra: dict = None) -> dict:
    """Build a realistic ISO 8583-style transaction payload for test case inputData."""
    d = {
        "rrn": f"20240115000{n}",
        "stan": f"10000{n}",
        "amount": str(amount).zfill(11),
        "currency": "INR",
        "transactionType": "PURCHASE",
        "mid": f"MID0000{n}",
        "tid": f"TID0000{n}",
        "customerId": f"CUST00{n}",
    }
    if extra:
        d.update(extra)
    return d


def _cases_for_rule_type(rule_type: str, req: "GenerateTestCasesRequest") -> list:  # noqa: F821
    """Return exactly 3 test cases: ACCEPT, MONITOR, REJECT for any rule type."""

    if rule_type == "SINGLE_LARGE_TX":
        max_amt = int(req.maxAmount or 100000)
        accept_amt = int(max_amt * 0.40)
        monitor_amt = int(max_amt * 1.10)
        reject_amt  = int(max_amt * 2.00)
        return [
            {
                "testCaseName": f"Single Large Transaction — ACCEPT: Amount Below Threshold",
                "description":  f"Transaction ₹{accept_amt:,} is below the configured limit ₹{max_amt:,}. Rule does not trigger → ACCEPT.",
                "inputData":    _txn(1, accept_amt),
                "expectedResult": "ACCEPT",
            },
            {
                "testCaseName": f"Single Large Transaction — MONITOR: Amount Near Threshold",
                "description":  f"Transaction ₹{monitor_amt:,} exceeds the limit ₹{max_amt:,}. Rule triggers for monitoring → MONITOR.",
                "inputData":    _txn(2, monitor_amt),
                "expectedResult": "MONITOR",
            },
            {
                "testCaseName": f"Single Large Transaction — REJECT: Amount Far Above Threshold",
                "description":  f"Transaction ₹{reject_amt:,} is well above the limit ₹{max_amt:,}. Rule triggers with rejection → REJECT.",
                "inputData":    _txn(3, reject_amt),
                "expectedResult": "REJECT",
            },
        ]

    if rule_type == "EXCEED_DAILY_LIMIT":
        max_amt = int(req.maxAmount or 50000)
        accept_amt = int(max_amt * 0.50)
        monitor_amt = int(max_amt * 0.95)
        reject_amt  = int(max_amt * 1.30)
        return [
            {
                "testCaseName": "Exceed Daily Limit — ACCEPT: Within Daily Limit",
                "description":  f"Transaction ₹{accept_amt:,} keeps customer within the daily limit ₹{max_amt:,} → ACCEPT.",
                "inputData":    _txn(1, accept_amt, {"dailyCumulativeAmount": accept_amt}),
                "expectedResult": "ACCEPT",
            },
            {
                "testCaseName": "Exceed Daily Limit — MONITOR: Approaching Daily Limit",
                "description":  f"Transaction would bring cumulative daily total to ₹{monitor_amt:,}, near the limit ₹{max_amt:,} → MONITOR.",
                "inputData":    _txn(2, monitor_amt, {"dailyCumulativeAmount": monitor_amt}),
                "expectedResult": "MONITOR",
            },
            {
                "testCaseName": "Exceed Daily Limit — REJECT: Daily Limit Breached",
                "description":  f"Transaction would push daily total to ₹{reject_amt:,}, exceeding the limit ₹{max_amt:,} → REJECT.",
                "inputData":    _txn(3, reject_amt, {"dailyCumulativeAmount": reject_amt}),
                "expectedResult": "REJECT",
            },
        ]

    if rule_type == "STRUCTURING":
        count  = req.txnCount or 3
        window = req.frequency or 24
        try:
            threshold = int(req.txnAmount or "000000050000")
        except (ValueError, TypeError):
            threshold = 50000
        per_txn = max(1000, threshold // max(count, 1))
        return [
            {
                "testCaseName": "Structuring — ACCEPT: Single Transaction",
                "description":  f"One transaction of ₹{per_txn:,} within {window}h. Count (1) is below structuring threshold ({count}) → ACCEPT.",
                "inputData":    _txn(1, per_txn, {"transactionCount": 1, "timeWindowHours": window}),
                "expectedResult": "ACCEPT",
            },
            {
                "testCaseName": f"Structuring — MONITOR: {count} Transactions (Threshold Reached)",
                "description":  f"{count} transactions of ₹{per_txn:,} each within {window}h. Structuring pattern detected → MONITOR.",
                "inputData":    _txn(2, per_txn, {"transactionCount": count, "timeWindowHours": window}),
                "expectedResult": "MONITOR",
            },
            {
                "testCaseName": f"Structuring — REJECT: {count + 2} Transactions (Threshold Exceeded)",
                "description":  f"{count + 2} transactions of ₹{per_txn:,} each within {window}h. Confirmed structuring pattern → REJECT.",
                "inputData":    _txn(3, per_txn, {"transactionCount": count + 2, "timeWindowHours": window}),
                "expectedResult": "REJECT",
            },
        ]

    if rule_type in ("HIGH_FREQ_TXN", "TXN_VELOCITY", "SEQUENTIAL_TXN"):
        count  = req.txnCount or 5
        window = req.frequency or 1
        low    = max(1, count // 2)
        high   = count * 2
        label  = rule_type.replace("_", " ").title()
        return [
            {
                "testCaseName": f"{label} — ACCEPT: Low Frequency",
                "description":  f"{low} transactions in {window}h. Below velocity threshold of {count} → ACCEPT.",
                "inputData":    _txn(1, 10000, {"transactionCount": low, "timeWindowHours": window}),
                "expectedResult": "ACCEPT",
            },
            {
                "testCaseName": f"{label} — MONITOR: At Threshold",
                "description":  f"{count} transactions in {window}h. Velocity threshold reached → MONITOR.",
                "inputData":    _txn(2, 10000, {"transactionCount": count, "timeWindowHours": window}),
                "expectedResult": "MONITOR",
            },
            {
                "testCaseName": f"{label} — REJECT: Threshold Significantly Exceeded",
                "description":  f"{high} transactions in {window}h. Velocity threshold exceeded significantly → REJECT.",
                "inputData":    _txn(3, 10000, {"transactionCount": high, "timeWindowHours": window}),
                "expectedResult": "REJECT",
            },
        ]

    if rule_type in ("MONTHLY_TXN_VOLUME", "DAILY_TXN_VOLUME", "ANNUAL_TXN_VOLUME"):
        count  = req.txnCount or 50
        period = {"MONTHLY_TXN_VOLUME": "month", "DAILY_TXN_VOLUME": "day", "ANNUAL_TXN_VOLUME": "year"}.get(rule_type, "period")
        low    = max(1, count // 2)
        high   = int(count * 1.5)
        label  = rule_type.replace("_", " ").title()
        return [
            {
                "testCaseName": f"{label} — ACCEPT: Count Below Limit",
                "description":  f"{low} transactions in the {period}. Below volume threshold of {count} → ACCEPT.",
                "inputData":    _txn(1, 10000, {"transactionCount": low}),
                "expectedResult": "ACCEPT",
            },
            {
                "testCaseName": f"{label} — MONITOR: Count At Limit",
                "description":  f"{count} transactions in the {period}. Volume threshold reached → MONITOR.",
                "inputData":    _txn(2, 10000, {"transactionCount": count}),
                "expectedResult": "MONITOR",
            },
            {
                "testCaseName": f"{label} — REJECT: Count Exceeds Limit",
                "description":  f"{high} transactions in the {period}. Volume threshold exceeded → REJECT.",
                "inputData":    _txn(3, 10000, {"transactionCount": high}),
                "expectedResult": "REJECT",
            },
        ]

    if rule_type in ("DAILY_TXN_VALUE", "ANNUAL_TXN_VALUE"):
        try:
            threshold = int(req.txnAmount or ("000000500000" if rule_type == "DAILY_TXN_VALUE" else "001000000000"))
        except (ValueError, TypeError):
            threshold = 500000
        accept_val  = int(threshold * 0.40)
        monitor_val = int(threshold * 1.00)
        reject_val  = int(threshold * 1.60)
        period = "day" if rule_type == "DAILY_TXN_VALUE" else "year"
        label  = rule_type.replace("_", " ").title()
        return [
            {
                "testCaseName": f"{label} — ACCEPT: Cumulative Value Below Limit",
                "description":  f"Cumulative {period}ly value ₹{accept_val:,} is below threshold ₹{threshold:,} → ACCEPT.",
                "inputData":    _txn(1, accept_val, {"cumulativeValue": accept_val}),
                "expectedResult": "ACCEPT",
            },
            {
                "testCaseName": f"{label} — MONITOR: Cumulative Value At Limit",
                "description":  f"Cumulative {period}ly value ₹{monitor_val:,} reaches threshold ₹{threshold:,} → MONITOR.",
                "inputData":    _txn(2, monitor_val, {"cumulativeValue": monitor_val}),
                "expectedResult": "MONITOR",
            },
            {
                "testCaseName": f"{label} — REJECT: Cumulative Value Exceeds Limit",
                "description":  f"Cumulative {period}ly value ₹{reject_val:,} exceeds threshold ₹{threshold:,} → REJECT.",
                "inputData":    _txn(3, reject_val, {"cumulativeValue": reject_val}),
                "expectedResult": "REJECT",
            },
        ]

    if rule_type == "ABNORMAL_HOUR":
        return [
            {
                "testCaseName": "Abnormal Hour — ACCEPT: Normal Business Hour",
                "description":  "Transaction at 14:30. Within normal business hours → ACCEPT.",
                "inputData":    _txn(1, 25000, {"transactionHour": 14, "transactionTime": "14:30"}),
                "expectedResult": "ACCEPT",
            },
            {
                "testCaseName": "Abnormal Hour — MONITOR: Late Night Transaction",
                "description":  "Transaction at 01:00 AM. Off-hours transaction flagged for monitoring → MONITOR.",
                "inputData":    _txn(2, 25000, {"transactionHour": 1, "transactionTime": "01:00"}),
                "expectedResult": "MONITOR",
            },
            {
                "testCaseName": "Abnormal Hour — REJECT: Deep Off-Hours High-Value Transaction",
                "description":  "High-value transaction at 03:00 AM. Deep off-hours with suspicious amount → REJECT.",
                "inputData":    _txn(3, 250000, {"transactionHour": 3, "transactionTime": "03:00"}),
                "expectedResult": "REJECT",
            },
        ]

    if rule_type == "UNUSUAL_AMT":
        pct = req.percentageThreshold or 200.0
        return [
            {
                "testCaseName": "Unusual Amount — ACCEPT: Amount Within Normal Range",
                "description":  f"Transaction amount is within {pct:.0f}% of customer baseline. No anomaly → ACCEPT.",
                "inputData":    _txn(1, 5000, {"deviationPercent": 10}),
                "expectedResult": "ACCEPT",
            },
            {
                "testCaseName": "Unusual Amount — MONITOR: Amount Slightly Unusual",
                "description":  f"Transaction amount deviates {pct:.0f}% above customer baseline. Anomaly flagged → MONITOR.",
                "inputData":    _txn(2, int(5000 * (1 + pct / 100)), {"deviationPercent": pct}),
                "expectedResult": "MONITOR",
            },
            {
                "testCaseName": "Unusual Amount — REJECT: Extreme Deviation",
                "description":  f"Transaction amount deviates {pct * 3:.0f}% above customer baseline. Extreme anomaly → REJECT.",
                "inputData":    _txn(3, int(5000 * (1 + pct * 3 / 100)), {"deviationPercent": pct * 3}),
                "expectedResult": "REJECT",
            },
        ]

    if rule_type == "ROUND_AMT_TXN":
        try:
            threshold = int(req.txnAmount or "000000010000")
        except (ValueError, TypeError):
            threshold = 10000
        return [
            {
                "testCaseName": "Round Amount — ACCEPT: Non-Round Amount",
                "description":  "Transaction amount ₹1,247 is not suspiciously round → ACCEPT.",
                "inputData":    _txn(1, 1247),
                "expectedResult": "ACCEPT",
            },
            {
                "testCaseName": f"Round Amount — MONITOR: Round Amount At Threshold",
                "description":  f"Transaction amount ₹{threshold:,} is a round number at the configured threshold → MONITOR.",
                "inputData":    _txn(2, threshold),
                "expectedResult": "MONITOR",
            },
            {
                "testCaseName": f"Round Amount — REJECT: Large Round Amount",
                "description":  f"Transaction amount ₹{threshold * 5:,} is a large, suspiciously round amount → REJECT.",
                "inputData":    _txn(3, threshold * 5),
                "expectedResult": "REJECT",
            },
        ]

    if rule_type == "INCONSISTENT_MCC":
        return [
            {
                "testCaseName": "Inconsistent MCC — ACCEPT: Consistent MCC",
                "description":  "Transaction MCC 5411 (Grocery) matches customer's historical spending profile → ACCEPT.",
                "inputData":    _txn(1, 5000, {"mcc": "5411", "historicalMccMatch": True}),
                "expectedResult": "ACCEPT",
            },
            {
                "testCaseName": "Inconsistent MCC — MONITOR: Unusual MCC",
                "description":  "Transaction at MCC 7995 (Gambling) which the customer rarely uses. Inconsistency flagged → MONITOR.",
                "inputData":    _txn(2, 15000, {"mcc": "7995", "historicalMccMatch": False}),
                "expectedResult": "MONITOR",
            },
            {
                "testCaseName": "Inconsistent MCC — REJECT: High-Risk Inconsistent MCC",
                "description":  "Transaction at high-risk MCC 7995 never used by customer, high value → REJECT.",
                "inputData":    _txn(3, 50000, {"mcc": "7995", "historicalMccMatch": False, "highRiskMcc": True}),
                "expectedResult": "REJECT",
            },
        ]

    # Generic fallback for any other rule type
    rule_label = rule_type.replace("_", " ").title()
    return [
        {
            "testCaseName": f"{rule_label} — ACCEPT: Safe Transaction",
            "description":  f"Transaction that does NOT trigger the {rule_type} rule → ACCEPT.",
            "inputData":    _txn(1, 5000),
            "expectedResult": "ACCEPT",
        },
        {
            "testCaseName": f"{rule_label} — MONITOR: Threshold Reached",
            "description":  f"Transaction that triggers the {rule_type} rule for monitoring → MONITOR.",
            "inputData":    _txn(2, 50000),
            "expectedResult": "MONITOR",
        },
        {
            "testCaseName": f"{rule_label} — REJECT: Threshold Exceeded",
            "description":  f"Transaction that breaches the {rule_type} threshold and should be rejected → REJECT.",
            "inputData":    _txn(3, 200000),
            "expectedResult": "REJECT",
        },
    ]


def _build_deterministic_test_cases(request: "GenerateTestCasesRequest") -> dict:  # noqa: F821
    """Always returns exactly 3 test cases (ACCEPT, MONITOR, REJECT) with no external calls."""
    rule_type = (request.ruleType or "UNKNOWN").upper()
    logger.info(f"[AI TEST CASES] Returning deterministic ACCEPT/MONITOR/REJECT test cases for {rule_type}")
    return {"ruleType": rule_type, "testCases": _cases_for_rule_type(rule_type, request)}


def _normalize_to_three_cases(
    groq_cases: list, request: "GenerateTestCasesRequest"  # noqa: F821
) -> list:
    """Merge Groq output into exactly ACCEPT / MONITOR / REJECT cases.

    Uses deterministic fallback for any missing outcome.
    """
    det = _build_deterministic_test_cases(request)["testCases"]
    det_by_result = {c["expectedResult"]: c for c in det}

    groq_by_result: dict = {}
    for case in groq_cases:
        result = (case.get("expectedResult") or "").upper().strip()
        if result == "BLOCK" and "REJECT" not in groq_by_result:
            result = "REJECT"
        if result in ("ACCEPT", "MONITOR", "REJECT") and result not in groq_by_result:
            groq_by_result[result] = case

    final = []
    for expected in ("ACCEPT", "MONITOR", "REJECT"):
        if expected in groq_by_result:
            c = groq_by_result[expected]
            fallback = det_by_result[expected]
            final.append({
                "testCaseName":   c.get("testCaseName") or fallback["testCaseName"],
                "description":    c.get("description")  or fallback["description"],
                "inputData":      c.get("inputData")     or fallback["inputData"],
                "expectedResult": expected,
            })
        else:
            final.append(det_by_result[expected])
    return final


# ---------------------------------------------------------------------------
# Mock provider — fully functional, no API key required
# ---------------------------------------------------------------------------

class MockLLMProvider(BaseLLMProvider):
    """
    Fully deterministic provider that requires no API key or network access.
    Used when AI_PROVIDER=mock, or as the fallback target when Groq fails
    and AI_FALLBACK_TO_MOCK=true.

    Test case generation always returns exactly 3 cases (ACCEPT/MONITOR/REJECT)
    derived from the rule's threshold parameters. Explanations and failure
    analyses are template-based and keyed on ruleType.
    """

    async def generate_test_cases(self, request: GenerateTestCasesRequest) -> dict:
        rule_type = (request.ruleType or "UNKNOWN").upper()
        logger.info(f"[AI TEST CASES] Provider selected: mock")
        logger.debug(f"[MOCK] generate_test_cases | rule={request.ruleName} | type={rule_type}")
        return _build_deterministic_test_cases(request)

    async def explain_rule(self, request: ExplainRuleRequest) -> dict:
        logger.debug(f"[MOCK] explain_rule | rule={request.ruleName}")
        return self._explanation(request.ruleType.upper(), request.action.upper(), request)

    async def analyze_failure(self, request: AnalyzeFailureRequest) -> dict:
        logger.debug(f"[MOCK] analyze_failure | case={request.testCaseName}")
        return self._failure_analysis(request)

    async def generate_transaction(self, request: GenerateTransactionRequest) -> dict:
        logger.debug(f"[MOCK] generate_transaction | type={request.ruleType} | result={request.expectedResult}")
        return self._transaction(request)

    async def generate_rule_from_requirement(self, request: GenerateRuleRequest) -> dict:
        logger.debug(f"[MOCK] generate_rule_from_requirement | req={request.requirement[:80]}")
        return self._rule_from_requirement(request.requirement)

    async def chat(self, message: str, context: dict) -> str:
        logger.info("[AI CHAT] provider=mock")
        logger.info(f"[AI CHAT] message={message[:120]}")
        logger.info(f"[AI CHAT] context={context}")
        reply = self._mock_chat_reply(message)
        logger.info("[AI CHAT] response generated")
        return reply

    def _mock_chat_reply(self, message: str) -> str:
        # Keyword-based routing — each branch returns a domain-specific explanation.
        # Order matters: more specific keywords appear before generic ones.
        msg = message.lower()

        if any(k in msg for k in ["high freq", "high frequency", "high_freq_txn"]):
            return (
                "Your HIGH_FREQ_TXN test likely failed because the rule engine did not detect "
                "enough matching transactions within the configured frequency window.\n\n"
                "HIGH_FREQ_TXN evaluates transaction COUNT, not amount. Key things to check:\n\n"
                "1. **txnCount threshold** — the rule triggers when the number of transactions "
                "for the same card/customer/device reaches or exceeds txnCount within frequency hours. "
                "Verify your test input includes enough historical transactions.\n\n"
                "2. **Matching criteria** — transactions are grouped by customerId, track2_data, or "
                "serialNumber. A mismatch in any of these fields means transactions are counted separately "
                "and the threshold may never be reached.\n\n"
                "3. **Time window** — all transactions must fall within the configured frequency (hours) "
                "window. Transactions outside this window are excluded from the count.\n\n"
                "4. **ACCEPT result** — if the test returned ACCEPT, it means the threshold was not reached, "
                "not that the rule is broken. Increase the transaction count in your test data or reduce "
                "the txnCount threshold in the rule configuration.\n\n"
                "5. **transaction_time field** — ensure the transactionTime or timestamp field is correctly "
                "formatted so the rule engine can compute the rolling window correctly."
            )

        if any(k in msg for k in ["structuring", "smurfing", "split transaction"]):
            return (
                "STRUCTURING rule failures are usually caused by the transaction count or cumulative "
                "amount not reaching the configured thresholds within the time window.\n\n"
                "Things to check:\n\n"
                "1. **txnCount** — the rule triggers when the customer makes at least txnCount transactions "
                "within frequency hours. Verify your test provides exactly that many transactions for the "
                "same customerId.\n\n"
                "2. **txnAmount** — each individual transaction must be below the txnAmount threshold "
                "(structuring means intentionally keeping each transaction small). If a transaction "
                "exceeds this, it may be evaluated differently.\n\n"
                "3. **frequency window** — all transactions must have timestamps within the rolling "
                "window. A single transaction outside the window reduces the effective count.\n\n"
                "4. **Same customer** — the rule groups transactions by customerId. Mismatched IDs "
                "mean transactions are not counted together."
            )

        if any(k in msg for k in ["single large", "single_large_tx", "large transaction"]):
            return (
                "SINGLE_LARGE_TX failure is almost always a threshold mismatch.\n\n"
                "This rule is purely amount-based — it checks a single transaction's amount against maxAmount. "
                "No historical data or time windows are involved.\n\n"
                "Things to check:\n\n"
                "1. **maxAmount** — confirm the rule's maxAmount matches what you expect. "
                "If the test transaction amount is below maxAmount, the rule will ACCEPT.\n\n"
                "2. **Amount format** — transaction amounts are stored as zero-padded strings "
                "(e.g. '00000150000' = 150,000 INR). Verify the inputData amount field is correctly "
                "formatted and parsed.\n\n"
                "3. **Operator** — the rule usually triggers when amount >= maxAmount. "
                "Check whether the boundary is >= or > (strictly greater)."
            )

        if any(k in msg for k in ["structuring", "txn_velocity", "velocity", "sequential"]):
            return (
                "Velocity-based rules (TXN_VELOCITY, SEQUENTIAL_TXN) evaluate transaction count "
                "within a time window — not the amounts.\n\n"
                "If your test returned ACCEPT when you expected MONITOR or REJECT:\n"
                "- The transaction count in your test input may be below the txnCount threshold.\n"
                "- The transactions may not all share the same customerId or track2_data.\n"
                "- The timestamps may span beyond the frequency window.\n\n"
                "Ensure your test input provides enough transactions with matching identifiers "
                "within the configured time window."
            )

        if any(k in msg for k in ["abnormal hour", "abnormal_hour", "off hour", "night transaction"]):
            return (
                "ABNORMAL_HOUR rule checks the hour of the transaction timestamp.\n\n"
                "If your test failed:\n"
                "1. Verify the transactionHour or transactionTime field in inputData is set correctly.\n"
                "2. The rule typically defines off-hours (e.g. 00:00–06:00). Transactions within "
                "business hours will return ACCEPT.\n"
                "3. Ensure the timestamp is in the correct timezone — the rule engine may interpret "
                "UTC timestamps differently from local time.\n\n"
                "To trigger MONITOR/REJECT: set transactionHour to a value in the off-hours range "
                "(e.g. 2 for 2 AM)."
            )

        if any(k in msg for k in ["unusual amount", "unusual_amt", "amount deviation", "deviat"]):
            return (
                "UNUSUAL_AMT is a behavioral rule — it requires a customer transaction history baseline.\n\n"
                "If your test returned ACCEPT when you expected MONITOR:\n"
                "1. **No baseline** — new customers or customers with fewer than 30 days of history "
                "will not have a reliable baseline. The rule may default to ACCEPT.\n"
                "2. **percentageThreshold** — the deviationPercent in your test inputData must meet "
                "or exceed this threshold to trigger the rule.\n"
                "3. **Historical data** — some implementations require actual transaction history "
                "records in the database; mock inputData alone may not satisfy the rule condition.\n\n"
                "Set deviationPercent in inputData to a value >= percentageThreshold to trigger the rule."
            )

        if any(k in msg for k in ["accept", "monitor", "reject", "expected result", "test case", "test fail"]):
            return (
                "When a test case returns an unexpected result, the most common causes are:\n\n"
                "1. **Threshold not reached** — ACCEPT means the rule conditions were not satisfied. "
                "Check that your inputData values (amount, count, time) match or exceed the rule thresholds.\n\n"
                "2. **Data format mismatch** — amounts must be zero-padded strings, timestamps must be "
                "ISO 8601 format, and IDs (customerId, track2_data) must match exactly.\n\n"
                "3. **Rule configuration** — verify the rule's txnCount, frequency, maxAmount, or "
                "txnAmount in the rule configuration matches your test assumptions.\n\n"
                "4. **Stateful rules** — velocity and frequency rules depend on historical transaction data. "
                "If the rule engine doesn't find prior transactions for the same customer/card, "
                "the count will be low and the rule may not trigger.\n\n"
                "5. **Wrong rule active** — confirm the rule is ACTIVE in the test environment and "
                "its action (MONITOR/REJECT) is set correctly."
            )

        if any(k in msg for k in ["explain", "what is", "how does", "what does"]):
            return (
                "I can explain any fraud rule in this system. The supported rule types are:\n\n"
                "- **SINGLE_LARGE_TX** — blocks/monitors single transactions above a max amount\n"
                "- **STRUCTURING** — detects split transactions (smurfing) within a time window\n"
                "- **HIGH_FREQ_TXN / TXN_VELOCITY / SEQUENTIAL_TXN** — count-based velocity rules\n"
                "- **DAILY/MONTHLY/ANNUAL_TXN_VOLUME** — cumulative count within time periods\n"
                "- **DAILY/ANNUAL_TXN_VALUE** — cumulative amount within time periods\n"
                "- **EXCEED_DAILY_LIMIT** — blocks transactions that would breach the daily limit\n"
                "- **ABNORMAL_HOUR** — flags off-hours transactions\n"
                "- **UNUSUAL_AMT** — detects amount anomalies relative to customer baseline\n"
                "- **ROUND_AMT_TXN** — flags suspiciously round transaction amounts\n"
                "- **INCONSISTENT_MCC** — detects unusual merchant categories for the customer\n\n"
                "Ask me about a specific rule type and I'll explain how it works and how to test it."
            )

        # Generic fallback
        return (
            "I'm here to help with AI-FRMS Rule Testing. I can assist with:\n\n"
            "- **Rule failures** — explain why a test case returned ACCEPT/MONITOR/REJECT unexpectedly\n"
            "- **Rule configuration** — explain what each field (txnCount, frequency, maxAmount) controls\n"
            "- **Test case design** — advise on what inputData to use to trigger specific rule outcomes\n"
            "- **Debugging steps** — guide you through diagnosing rule engine behavior\n\n"
            "Try asking: 'Why did my High Frequency Transaction test fail?' or "
            "'How does the STRUCTURING rule work?'"
        )

    # ------------------------------------------------------------------
    # Internal builders
    # ------------------------------------------------------------------

    def _test_cases(self, rule_type: str, action: str, req: GenerateTestCasesRequest) -> list:
        base = datetime.now()

        if rule_type == "STRUCTURING":
            count = req.txnCount or 3
            window = req.frequency or 24
            total_str = req.txnAmount or "000000050000"
            per_txn = int(total_str) // count

            trigger_txns = [
                {
                    "amount": str(per_txn).zfill(11),
                    "currency": "INR",
                    "timestamp": (base + timedelta(hours=i * (window // count))).isoformat(),
                }
                for i in range(count)
            ]

            return [
                {
                    "testCaseName": f"Structuring — {count} Transactions Triggering {action}",
                    "description": (
                        f"Customer performs {count} transactions of ₹{per_txn:,} each within "
                        f"{window} hours. Individually below threshold but collectively triggers "
                        f"the structuring rule → {action}."
                    ),
                    "inputData": {
                        "customerId": "CUST001",
                        "transactions": trigger_txns,
                        "timeWindowHours": window,
                        "transactionCount": count,
                    },
                    "expectedResult": action,
                },
                {
                    "testCaseName": "Structuring — Single Transaction (ACCEPT)",
                    "description": (
                        "Only one transaction in the time window. Structuring rule does not trigger."
                    ),
                    "inputData": {
                        "customerId": "CUST002",
                        "transactions": [
                            {
                                "amount": str(per_txn).zfill(11),
                                "currency": "INR",
                                "timestamp": base.isoformat(),
                            }
                        ],
                        "timeWindowHours": window,
                        "transactionCount": 1,
                    },
                    "expectedResult": "ACCEPT",
                },
                {
                    "testCaseName": f"Structuring — Boundary: {count - 1} Transactions (ACCEPT)",
                    "description": (
                        f"Customer performs {count - 1} transactions — one below the trigger "
                        f"count of {count}. Rule should NOT activate."
                    ),
                    "inputData": {
                        "customerId": "CUST003",
                        "transactions": [
                            {
                                "amount": str(per_txn).zfill(11),
                                "currency": "INR",
                                "timestamp": (base + timedelta(hours=i * 4)).isoformat(),
                            }
                            for i in range(max(1, count - 1))
                        ],
                        "timeWindowHours": window,
                        "transactionCount": max(1, count - 1),
                    },
                    "expectedResult": "ACCEPT",
                },
            ]

        elif rule_type == "SINGLE_LARGE_TX":
            max_amt = int(req.maxAmount or 100000)
            above = int(max_amt * 1.5)
            below = int(max_amt * 0.5)

            return [
                {
                    "testCaseName": f"Large Tx — Above Limit → {action}",
                    "description": (
                        f"Transaction ₹{above:,} exceeds maxAmount ₹{max_amt:,}. Rule triggers {action}."
                    ),
                    "inputData": {
                        "rrn": "202401151001",
                        "stan": "100001",
                        "amount": str(above).zfill(11),
                        "currency": "INR",
                        "transactionType": "PURCHASE",
                        "mid": "MID00001",
                        "tid": "TID00001",
                    },
                    "expectedResult": action,
                },
                {
                    "testCaseName": f"Large Tx — Exactly At Limit → {action}",
                    "description": (
                        f"Transaction ₹{max_amt:,} equals maxAmount. Rule triggers {action}."
                    ),
                    "inputData": {
                        "rrn": "202401151002",
                        "stan": "100002",
                        "amount": str(max_amt).zfill(11),
                        "currency": "INR",
                        "transactionType": "PURCHASE",
                        "mid": "MID00002",
                        "tid": "TID00002",
                    },
                    "expectedResult": action,
                },
                {
                    "testCaseName": "Large Tx — Below Limit → ACCEPT",
                    "description": (
                        f"Transaction ₹{below:,} is below maxAmount ₹{max_amt:,}. Should ACCEPT."
                    ),
                    "inputData": {
                        "rrn": "202401151003",
                        "stan": "100003",
                        "amount": str(below).zfill(11),
                        "currency": "INR",
                        "transactionType": "PURCHASE",
                        "mid": "MID00003",
                        "tid": "TID00003",
                    },
                    "expectedResult": "ACCEPT",
                },
            ]

        elif rule_type in ("VELOCITY", "VELOCITY_CHECK"):
            count = req.txnCount or 5
            window = req.frequency or 1

            return [
                {
                    "testCaseName": f"Velocity — {count} Transactions Exceeding Limit → {action}",
                    "description": (
                        f"{count} transactions within {window} hour(s) exceeds velocity limit. "
                        f"Expected: {action}."
                    ),
                    "inputData": {
                        "customerId": "CUST001",
                        "transactionCount": count,
                        "timeWindowHours": window,
                        "transactions": [
                            {
                                "amount": "00000010000",
                                "currency": "INR",
                                "timestamp": (base + timedelta(minutes=i * 10)).isoformat(),
                            }
                            for i in range(count)
                        ],
                    },
                    "expectedResult": action,
                },
                {
                    "testCaseName": "Velocity — Normal Count → ACCEPT",
                    "description": "Transaction count is within velocity limit. Should ACCEPT.",
                    "inputData": {
                        "customerId": "CUST002",
                        "transactionCount": max(1, count - 2),
                        "timeWindowHours": window,
                        "transactions": [
                            {
                                "amount": "00000010000",
                                "currency": "INR",
                                "timestamp": (base + timedelta(minutes=i * 20)).isoformat(),
                            }
                            for i in range(max(1, count - 2))
                        ],
                    },
                    "expectedResult": "ACCEPT",
                },
                {
                    "testCaseName": f"Velocity — Boundary: {count - 1} Transactions → ACCEPT",
                    "description": (
                        f"One transaction below the velocity limit of {count}. Rule should not fire."
                    ),
                    "inputData": {
                        "customerId": "CUST003",
                        "transactionCount": max(1, count - 1),
                        "timeWindowHours": window,
                        "transactions": [
                            {
                                "amount": "00000010000",
                                "currency": "INR",
                                "timestamp": (base + timedelta(minutes=i * 15)).isoformat(),
                            }
                            for i in range(max(1, count - 1))
                        ],
                    },
                    "expectedResult": "ACCEPT",
                },
            ]

        else:
            # Generic fallback for custom/unknown rule types
            return [
                {
                    "testCaseName": f"{rule_type} — Positive Case → {action}",
                    "description": f"Transaction that should trigger the {rule_type} rule with action {action}.",
                    "inputData": {
                        "rrn": "202401151001",
                        "stan": "100001",
                        "amount": "00000100000",
                        "currency": "INR",
                        "transactionType": "PURCHASE",
                        "customerId": "CUST001",
                    },
                    "expectedResult": action,
                },
                {
                    "testCaseName": f"{rule_type} — Negative Case → ACCEPT",
                    "description": f"Transaction that should NOT trigger the {rule_type} rule.",
                    "inputData": {
                        "rrn": "202401151002",
                        "stan": "100002",
                        "amount": "00000001000",
                        "currency": "INR",
                        "transactionType": "PURCHASE",
                        "customerId": "CUST002",
                    },
                    "expectedResult": "ACCEPT",
                },
                {
                    "testCaseName": f"{rule_type} — Boundary Case → {action}",
                    "description": f"Edge case transaction sitting exactly on the trigger boundary for {rule_type}.",
                    "inputData": {
                        "rrn": "202401151003",
                        "stan": "100003",
                        "amount": "00000050000",
                        "currency": "INR",
                        "transactionType": "PURCHASE",
                        "customerId": "CUST003",
                    },
                    "expectedResult": action,
                },
            ]

    # ------------------------------------------------------------------

    def _explanation(self, rule_type: str, action: str, req: ExplainRuleRequest) -> dict:
        templates = {
            "SINGLE_LARGE_TX": {
                "summary": (
                    f"The '{req.ruleName}' rule monitors individual transactions and applies "
                    f"{action} when a single transaction exceeds the configured maximum amount "
                    f"of ₹{int(req.maxAmount or 100000):,}."
                ),
                "businessMeaning": (
                    f"Protects the institution from high-value fraud exposure by automatically "
                    f"{action.lower()}ing transactions above ₹{int(req.maxAmount or 100000):,}. "
                    "Large single transactions can indicate stolen card usage, account takeover, "
                    "or money laundering. Regulatory compliance (PCI-DSS, PMLA) often mandates "
                    "enhanced scrutiny or blocking of such transactions."
                ),
                "technicalMeaning": (
                    f"Each incoming transaction is evaluated in real-time. The rule parses the "
                    f"zero-padded amount field and compares it numerically against `maxAmount` "
                    f"({int(req.maxAmount or 100000)}). If amount >= maxAmount, action {action} "
                    "is triggered. No time-window or aggregation logic is involved — purely a "
                    "single-transaction threshold check."
                ),
                "exampleScenario": (
                    f"A customer attempts a PURCHASE of ₹1,50,000 at a POS terminal. "
                    f"The rule engine parses amount='00000150000' → 150000. "
                    f"Comparison: 150000 >= {int(req.maxAmount or 100000)} → TRUE. "
                    f"Action {action} is applied. Transaction is {action.lower()}ed and flagged "
                    "for review."
                ),
                "riskNotes": [
                    f"All transactions above ₹{int(req.maxAmount or 100000):,} are {action.lower()}ed regardless of customer history.",
                    "Too-low threshold causes false positives affecting legitimate high-value business transactions.",
                    "Too-high threshold allows large fraud losses before detection.",
                    "Consider pairing with a whitelist rule for known high-value merchants (B2B, auto dealers).",
                    "Ensure denomination consistency — amount must be in same unit (paise vs rupees) across system.",
                ],
            },
            "STRUCTURING": {
                "summary": (
                    f"The '{req.ruleName}' rule detects structuring behavior — splitting large amounts "
                    f"into smaller transactions to evade single-transaction thresholds. Applies {action} "
                    "when suspicious aggregation patterns are identified."
                ),
                "businessMeaning": (
                    "Structuring (smurfing) is a money laundering technique classified as a financial crime "
                    "under PMLA and FEMA in India. Financial institutions are legally required to detect, "
                    "flag, and report structuring to FIU-IND. Failure to detect structuring can result in "
                    "regulatory penalties, reputational damage, and facilitation of money laundering."
                ),
                "technicalMeaning": (
                    f"The rule aggregates transaction counts per customer within a rolling time window "
                    f"of {req.frequency or 24} hours. When the count reaches {req.txnCount or 3} or "
                    f"more within the window, the rule triggers {action}. Optionally, cumulative amount "
                    "can also be monitored. Requires efficient time-series aggregation storage (Redis/DB)."
                ),
                "exampleScenario": (
                    f"Customer CUST001 makes {req.txnCount or 3} transactions of ₹15,000 each "
                    f"within {req.frequency or 24} hours. Each is below the individual large-tx threshold. "
                    f"The rule counts {req.txnCount or 3} transactions in the window → threshold met → "
                    f"{action} applied. A SAR (Suspicious Activity Report) is filed with FIU-IND."
                ),
                "riskNotes": [
                    f"A {req.frequency or 24}-hour window may miss structuring spread across multiple days.",
                    "Cross-account structuring (family members, business associates) may evade single-customer rules.",
                    "Transaction count threshold must be calibrated against normal customer behavior to avoid false positives.",
                    "SAR filing obligation under PMLA applies when structuring is confirmed.",
                    "Multi-channel aggregation (ATM + POS + online) is critical for accurate detection.",
                ],
            },
            "VELOCITY": {
                "summary": (
                    f"The '{req.ruleName}' rule limits the number of transactions per customer "
                    f"within a defined time window. Applies {action} when the velocity threshold "
                    f"of {req.txnCount or 5} transactions per {req.frequency or 1} hour(s) is exceeded."
                ),
                "businessMeaning": (
                    "High transaction velocity is a key fraud indicator for card testing attacks "
                    "(making micro-transactions to verify compromised card validity) and bot-driven fraud. "
                    "Velocity checks are a standard first-line defense in payment fraud prevention, "
                    "protecting cardholders from rapid unauthorized usage of compromised credentials."
                ),
                "technicalMeaning": (
                    f"The rule maintains a per-customer/card transaction counter with a rolling time window "
                    f"of {req.frequency or 1} hour(s). Each incoming transaction increments the counter. "
                    f"When the counter exceeds {req.txnCount or 5}, action {action} is triggered for "
                    "the current and subsequent transactions until the window resets."
                ),
                "exampleScenario": (
                    f"A compromised debit card is used at {req.txnCount or 5} different POS terminals "
                    f"within 30 minutes. The velocity rule detects {req.txnCount or 5} transactions "
                    f"within {req.frequency or 1} hour(s) → threshold exceeded → {action} applied. "
                    "Further usage of the card is blocked pending customer verification."
                ),
                "riskNotes": [
                    "May block legitimate high-frequency users (market vendors, fuel station operators).",
                    "Counter storage must support low-latency reads for real-time enforcement.",
                    "Card testing attacks use very small amounts — combine with minimum amount filter.",
                    "Cross-channel velocity requires unified counter across ATM, POS, and online channels.",
                    "Rolling window vs. fixed window reset strategy significantly affects rule sensitivity.",
                ],
            },
        }

        return templates.get(
            rule_type,
            {
                "summary": (
                    f"The '{req.ruleName}' rule implements {rule_type} fraud detection logic "
                    f"and applies {action} when trigger conditions are met."
                ),
                "businessMeaning": (
                    f"This rule monitors for {rule_type.lower().replace('_', ' ')} patterns in "
                    "transaction data to protect against financial fraud and regulatory non-compliance."
                ),
                "technicalMeaning": (
                    f"The rule engine evaluates incoming transactions against configured {rule_type} "
                    f"parameters. When conditions are met, {action} action is applied."
                ),
                "exampleScenario": (
                    f"Transaction matches {rule_type} rule criteria → Rule engine evaluates "
                    f"all conditions → {action} action triggered → Transaction processed accordingly."
                ),
                "riskNotes": [
                    "Calibrate rule parameters based on your transaction volume and risk appetite.",
                    "Monitor false positive rate to balance fraud prevention with customer experience.",
                    "Schedule regular rule performance reviews as fraud patterns evolve.",
                    "Coordinate with compliance team for SAR/CTR reporting obligations.",
                    "Test all rule changes in staging before production deployment.",
                ],
            },
        )

    # ------------------------------------------------------------------

    def _failure_analysis(self, req: AnalyzeFailureRequest) -> dict:  # noqa: C901
        expected = req.expectedResult.upper()
        actual = req.actualResult.upper()
        rule_type = req.ruleType.upper()
        logs = req.executionLogs or ""

        # Build a count summary string from enriched fields when available
        count_context = ""
        if req.matchedCount is not None and req.requiredCount is not None:
            hist = req.historicalTransactionCount if req.historicalTransactionCount is not None else "?"
            window = f" within {req.frequencyWindow}" if req.frequencyWindow else ""
            count_context = (
                f"The rule requires {req.requiredCount} transactions{window} "
                f"but only {req.matchedCount} were matched ({hist} historical)."
            )

        failure_reason_str = f" Rule engine reason: {req.failureReason}." if req.failureReason else ""

        # ----------------------------------------------------------------
        # Branch 1 — False negative: rule should have fired but returned ACCEPT
        # ----------------------------------------------------------------
        if expected in ("MONITOR", "REJECT", "BLOCK") and actual == "ACCEPT":

            # HIGH_FREQ_TXN / TXN_VELOCITY / SEQUENTIAL_TXN: count-based, needs history
            if rule_type in ("HIGH_FREQ_TXN", "TXN_VELOCITY", "SEQUENTIAL_TXN") and req.requiredCount is not None:
                hist = req.historicalTransactionCount if req.historicalTransactionCount is not None else 0
                matched = req.matchedCount if req.matchedCount is not None else hist
                window = req.frequencyWindow or "the configured time window"
                needed = max(0, req.requiredCount - matched)
                summary = (
                    f"The {rule_type} test failed because the rule requires "
                    f"{req.requiredCount} transactions in {window}, but only "
                    f"{matched} total were matched "
                    f"({'no historical transactions found' if hist == 0 else f'{hist} historical'}). "
                    f"Threshold not reached."
                )
                root_cause = (
                    f"No prior transactions found for this card/customer/device in {window}."
                    if hist == 0 else
                    f"Only {hist} historical transaction(s) matched — {needed} more needed to reach threshold of {req.requiredCount}."
                )
                possible_reasons = [
                    f"{rule_type} requires {req.requiredCount} transactions in {window} — only {matched} total were evaluated{' (0 historical)' if hist == 0 else f' ({hist} historical)'}.",
                    "Test database has no prior transactions matching this card/customer/device in the time window." if hist == 0 else f"Insufficient history: {hist} of {req.requiredCount - 1} required prior transactions exist.",
                    "The rule matches transactions by customerId, track2_data, or serialNumber — confirm all test transactions share the same matching key.",
                    "Transactions with timestamps outside the rolling time window are excluded — verify all test transactions fall within the window.",
                    f"Rule configuration may differ from test assumptions — verify txnCount={req.requiredCount} is the correct threshold.",
                ]
                debugging_steps = [
                    f"1. Check the test database: does it contain {req.requiredCount - 1} prior transactions for the same card/customer/device in {window}?",
                    "2. Identify which field the rule uses as the matching key (customerId, track2_data, or serialNumber) and confirm all test transactions share that exact value.",
                    f"3. Verify timestamps: all {req.requiredCount} transactions (historical + current) must have timestamps within the configured frequency window.",
                    "4. Enable rule engine trace logging to confirm how many transactions were counted in the evaluation.",
                    f"5. Confirm the rule's txnCount configuration is {req.requiredCount} — check for environment-specific overrides.",
                ]
                recommended_fix = (
                    f"Seed the test database with {req.requiredCount - 1} historical transactions "
                    f"sharing the same card/customer/device identifier, all within {window}, "
                    f"before running this test case. {rule_type} needs {req.requiredCount} total "
                    f"matching transactions to fire."
                )
                confidence = 90

            # UNUSUAL_AMT: needs customer spending baseline
            elif rule_type == "UNUSUAL_AMT":
                hist = req.historicalTransactionCount if req.historicalTransactionCount is not None else None
                hist_str = str(hist) if hist is not None else "unknown"
                summary = (
                    f"The UNUSUAL_AMT test failed because "
                    + ("no historical transactions were found — a spending baseline cannot be computed." if hist == 0
                       else f"the current amount did not deviate from the customer baseline by the configured percentageThreshold ({hist_str} historical transactions available).")
                )
                root_cause = (
                    "UNUSUAL_AMT requires a customer spending baseline. No historical transactions were found — the rule cannot compute a baseline and therefore cannot detect anomalies."
                    if hist == 0 else
                    "The transaction amount does not exceed the computed baseline threshold (baseline_average × (1 + percentageThreshold / 100))."
                )
                possible_reasons = [
                    f"No customer spending history for this card/customer — baseline cannot be established{'.' if hist == 0 else f' (only {hist_str} records found, may be insufficient).'}",
                    "The test transaction amount is not high enough to exceed baseline_average × (1 + percentageThreshold / 100).",
                    "Historical transactions may use a different customerId/card number — baseline lookup returned no results.",
                    "Baseline computation window excludes the historical test transactions (wrong time range or identity key).",
                ]
                debugging_steps = [
                    f"1. Check historicalTransactionCount = {hist_str} — {'seed the test database with baseline transactions for this customer.' if hist == 0 else 'verify these transactions have the same customerId/card as the current transaction.'}",
                    "2. Compute the expected threshold: calculate the average of historical amounts, then multiply by (1 + percentageThreshold / 100). Confirm the test transaction amount exceeds this.",
                    "3. Confirm historical transactions use the exact same customerId or card number as the transaction under test.",
                    "4. Review the baseline computation logic in the rule engine — verify the time range and identity key used.",
                ]
                recommended_fix = (
                    "Seed historical transactions for this customer/card to establish a spending baseline before running UNUSUAL_AMT tests. "
                    "The test transaction must exceed: average_historical_amount × (1 + percentageThreshold / 100)."
                )
                confidence = 80

            # STRUCTURING: multiple small transactions below threshold within window
            elif rule_type == "STRUCTURING":
                txn_count = (req.ruleConfig or {}).get("txnCount") or (req.ruleConfig or {}).get("txnCount") or req.requiredCount or "?"
                txn_amount = (req.ruleConfig or {}).get("txnAmount") or "?"
                freq = (req.ruleConfig or {}).get("frequencyHours") or (req.ruleConfig or {}).get("frequency") or req.frequencyWindow or "?"
                summary = (
                    f"The STRUCTURING test failed — the rule did not detect the expected pattern of "
                    f"{txn_count} small transactions below the threshold within the time window."
                )
                root_cause = (
                    "Structuring detection requires multiple transactions each BELOW the configured txnAmount threshold "
                    "within the frequency window. One or more conditions was not met."
                )
                possible_reasons = [
                    f"Each of the {txn_count} test transactions must be strictly BELOW txnAmount {txn_amount} — amounts equal to the threshold do NOT count.",
                    f"Not all test transactions fall within the {freq}-hour rolling window.",
                    "Test transactions may not share the same customerId/account identifier for the rule's grouping lookup.",
                    f"The rule engine may require exactly {txn_count} qualifying transactions — verify the count is met.",
                ]
                debugging_steps = [
                    f"1. Verify each test transaction amount is strictly LESS THAN the configured txnAmount threshold {txn_amount}.",
                    f"2. Confirm all {txn_count} transactions have timestamps within the {freq}-hour window.",
                    "3. Check that all test transactions share the same customerId/account identifier for grouping.",
                    "4. Review the rule engine comparison: 'less than' (not 'less than or equal to') must be used for txnAmount.",
                ]
                recommended_fix = (
                    f"Ensure all {txn_count} test transactions are: (1) BELOW txnAmount {txn_amount}, "
                    f"(2) within the {freq}-hour window, and (3) share the same customer/account identifier."
                )
                confidence = 80

            # SINGLE_LARGE_TX: pure amount comparison
            elif rule_type == "SINGLE_LARGE_TX":
                max_amount = (req.ruleConfig or {}).get("maxAmount") or "?"
                summary = (
                    f"The SINGLE_LARGE_TX test failed — the transaction amount did not exceed maxAmount {max_amount}."
                    + failure_reason_str
                )
                root_cause = f"Transaction amount is below maxAmount ({max_amount}). SINGLE_LARGE_TX triggers only when amount > maxAmount."
                possible_reasons = [
                    f"Test transaction amount is below the configured maxAmount {max_amount}.",
                    "Amount field may be encoded as a zero-padded string — verify Long.parseLong() parsing is correct.",
                    f"maxAmount in the test environment may be misconfigured (different from expected {max_amount}).",
                    "Rule may be comparing against a stale cached configuration value.",
                ]
                debugging_steps = [
                    f"1. Confirm the test transaction amount is numerically GREATER THAN maxAmount {max_amount}.",
                    "2. Verify the amount field is parsed correctly from its zero-padded string format (e.g. '00000150000' = 150000).",
                    f"3. Check the rule configuration in the test environment — maxAmount should be {max_amount}.",
                    "4. Enable rule trace logging to print the parsed amount and threshold values side by side.",
                ]
                recommended_fix = (
                    f"Set the test transaction amount to a value clearly above maxAmount {max_amount}. "
                    "Verify the amount is parsed correctly from its zero-padded string format before the comparison."
                )
                confidence = 85

            # Generic false-negative for all other rule types
            else:
                summary = (
                    f"The {rule_type} test returned ACCEPT instead of {expected}. "
                    f"Threshold conditions were not satisfied.{failure_reason_str}"
                    + (f" {count_context}" if count_context else "")
                )
                root_cause = (
                    count_context if count_context
                    else f"{rule_type} evaluation conditions were not met by the test input data."
                )
                possible_reasons = [
                    f"The {rule_type} rule threshold conditions were not satisfied by the test input data." + (f" {count_context}" if count_context else ""),
                    "Transaction count or cumulative amount did not reach the configured trigger threshold.",
                    "Time window calculation may have excluded one or more transactions.",
                    "Rule may be inactive or incorrectly configured in the test environment.",
                    "Input data format mismatch — amount or timestamp fields may be incorrectly encoded.",
                    "Rule engine may have encountered a parsing exception that was silently swallowed.",
                ]
                debugging_steps = [
                    "1. Verify the input transaction count equals or exceeds the rule's configured threshold.",
                    "2. Log the parsed amount value and compare it numerically against the rule threshold.",
                    "3. Validate all timestamps fall within the configured time window — check timezone handling.",
                    "4. Confirm the rule is ACTIVE and its action is correctly set in rule configuration.",
                    "5. Add trace logging inside the rule evaluation to print each condition result.",
                    "6. Reproduce with minimal data: simplest possible input that should trigger the rule.",
                    f"7. Review execution logs for clues: {logs[:300] + '...' if len(logs) > 300 else logs or 'No logs provided.'}",
                ]
                recommended_fix = (
                    f"Audit the trigger condition logic in the {rule_type} rule handler. "
                    "Verify: (1) All required conditions are checked with correct operators (>= vs >). "
                    "(2) Amount is parsed from zero-padded string using Long.parseLong() without errors. "
                    "(3) Time window comparison uses the correct unit (hours vs minutes). "
                    "(4) Rule configuration is loaded at startup and not stale from cache."
                )
                confidence = 65

            return {
                "summary": summary,
                "rootCause": root_cause,
                "possibleReasons": possible_reasons,
                "debuggingSteps": debugging_steps,
                "recommendedFix": recommended_fix,
                "riskImpact": (
                    f"If this {rule_type} rule is silently failing in production, fraudulent transactions "
                    f"are being ACCEPTED instead of {expected}. This is a critical gap — suspicious activity "
                    "goes undetected, regulatory reporting obligations are missed, and the institution "
                    "faces financial loss and potential compliance penalties. Prioritize as P1."
                ),
                "confidence": confidence,
            }

        # ----------------------------------------------------------------
        # Branch 2 — False positive: rule fired on data that should pass
        # ----------------------------------------------------------------
        elif expected == "ACCEPT" and actual in ("REJECT", "MONITOR", "BLOCK"):
            return {
                "summary": (
                    f"The {rule_type} test returned {actual} instead of ACCEPT — "
                    "the rule fired on data that should be below the trigger threshold (false positive)."
                ),
                "rootCause": (
                    "Rule threshold is too aggressive, or test input data unintentionally matches "
                    "the rule's trigger conditions."
                ),
                "possibleReasons": [
                    "Rule threshold is too aggressive — triggering on legitimate transaction data.",
                    "Test input data unintentionally matches the rule's trigger conditions.",
                    "Stateful counters or aggregations from previous test runs are polluting this test.",
                    "Amount threshold in test environment is misconfigured to a lower value than intended.",
                    "A different rule (not the one under test) is causing the unexpected action.",
                ],
                "debuggingSteps": [
                    "1. Compare all input values against every rule condition to find which one is triggering.",
                    "2. Clear transaction history/counters between test runs to avoid state contamination.",
                    "3. Verify rule configuration in the test environment matches the intended values.",
                    "4. Enable verbose rule engine logging to see which specific condition evaluates TRUE.",
                    "5. Check if multiple rules are active — another rule may be causing the action.",
                    "6. Confirm the test input amount/count is clearly below the rule's trigger threshold.",
                ],
                "recommendedFix": (
                    "This is a false positive scenario. Steps: (1) Clean all test state between runs. "
                    "(2) Ensure test input values are clearly within safe boundaries (not near thresholds). "
                    "(3) Check test environment rule configuration against production values. "
                    "(4) If the rule is correctly firing on the test data, update the test's expectedResult "
                    "to reflect the actual rule behavior — the test expectation may be wrong."
                ),
                "riskImpact": (
                    "False positives decline legitimate transactions, causing poor customer experience "
                    "and revenue loss. However, this is less critical than false negatives from a fraud "
                    "perspective. Investigate threshold calibration and test data quality."
                ),
                "confidence": 70,
            }

        # ----------------------------------------------------------------
        # Branch 3 — Other unexpected mismatch (e.g. MONITOR vs REJECT)
        # ----------------------------------------------------------------
        else:
            return {
                "summary": (
                    f"The {rule_type} test returned {actual} instead of {expected} — "
                    "unexpected rule action mismatch."
                ),
                "rootCause": (
                    f"Rule engine returned {actual} when {expected} was expected. "
                    "Possible rule logic defect or misconfigured test expectation."
                ),
                "possibleReasons": [
                    f"Expected {expected} but rule engine returned {actual} — unexpected rule behavior.",
                    "Rule configuration may differ from the assumptions made when designing the test.",
                    "Input data may not accurately represent the intended fraud/non-fraud scenario.",
                    "Rule logic may have changed since the test case was originally written.",
                    "Environmental difference between where test was designed and where it was executed.",
                ],
                "debuggingSteps": [
                    "1. Compare current rule configuration against the test case design assumptions.",
                    "2. Add step-by-step trace logging through the rule evaluation path.",
                    "3. Validate every input field against the rule's expected format and type.",
                    "4. Run a git diff on the rule implementation to detect recent changes.",
                    "5. Test with the simplest possible input to isolate the problematic condition.",
                    f"6. Execution logs: {logs[:300] + '...' if len(logs) > 300 else logs or 'No logs provided.'}",
                ],
                "recommendedFix": (
                    f"Investigate the mismatch between expected ({expected}) and actual ({actual}) for "
                    f"the {rule_type} rule. Start by tracing the rule evaluation step by step with the "
                    "provided input data. Identify which condition is not evaluating as expected and "
                    "correct either the rule logic or the test case expectation."
                ),
                "riskImpact": (
                    f"Unexpected rule behavior ({actual} instead of {expected}) indicates either a "
                    "rule implementation defect or incorrect test assumptions. Determine whether this "
                    "is a security gap (fraud not caught) or a false positive (legitimate tx blocked) "
                    "and prioritize accordingly."
                ),
                "confidence": 55,
            }

    # ------------------------------------------------------------------

    # ------------------------------------------------------------------
    # Rule generation helpers
    # ------------------------------------------------------------------

    def _rule_from_requirement(self, requirement: str) -> dict:
        req_lower = requirement.lower().strip()

        # Step 1: detect rule type and confidence level
        rule_type, confidence = self._detect_rule_type_v2(requirement, req_lower)

        # Step 2: detect action (use catalog default for the resolved rule type)
        meta = _RULE_CATALOG.get(rule_type)
        catalog_default_action = meta[1] if meta else "MONITOR"
        action = self._detect_action(req_lower, catalog_default_action)

        # Step 3: extract parameters from the requirement text
        txn_count  = self._extract_count(req_lower)
        raw_amount = self._extract_amount(req_lower)
        frequency  = self._extract_frequency_v2(req_lower)
        percentage = self._extract_percentage(req_lower)

        # Step 4: apply implied frequency defaults for periodic rule types
        if frequency is None and rule_type in _FREQUENCY_DEFAULTS:
            frequency = _FREQUENCY_DEFAULTS[rule_type]

        amount_type = meta[3] if meta else "none"

        # Step 5: build and return the structured rule
        return self._build_universal_rule(
            rule_type, action, txn_count, raw_amount,
            frequency, percentage, amount_type, requirement, confidence, meta,
        )

    # ------------------------------------------------------------------
    # Detection helpers
    # ------------------------------------------------------------------

    def _detect_rule_type_v2(self, original: str, req_lower: str) -> tuple:
        # Priority 1 — explicit uppercase snake-case rule type in the text
        # e.g. MONTHLY_TXN_VOLUME, SINGLE_LARGE_TX (must have at least one underscore)
        explicit = re.findall(r'\b([A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+)\b', original)
        skip = {"FRMS", "SAR", "CTR", "PCI", "DSS", "KYC", "UPI", "ATM", "POS",
                "OTP", "BIN", "MCC", "FIU", "IND", "PMLA", "FEMA"}
        for candidate in explicit:
            if candidate not in skip:
                return candidate, "HIGH"

        # Priority 2 — keyword matching from the ordered catalog map
        for rule_type, keywords in _KEYWORD_MAP:
            if any(kw in req_lower for kw in keywords):
                return rule_type, "MEDIUM"

        # Priority 3 — structural regex patterns
        if re.search(r"(more than|above|over|\d+\s*or more)\s*\d*\s*transactions?\s*(below|under|less than)", req_lower):
            return "STRUCTURING", "MEDIUM"
        if re.search(r"(single|one)\s+transactions?\s+(above|over|exceeding|more than)", req_lower):
            return "SINGLE_LARGE_TX", "MEDIUM"
        if re.search(r"transactions?\s+(above|over|exceeding|more than)\s*(rs\.?|inr|₹)?\s*[\d,]+", req_lower):
            return "SINGLE_LARGE_TX", "MEDIUM"
        # card/device cross patterns — flexible word order
        if re.search(r"multiple\s+cards?\b.{0,30}\bsame\s+device", req_lower):
            return "MULTIPLE_CARD_SAME_DEVICE", "MEDIUM"
        if re.search(r"multiple\s+devices?\b.{0,30}\bsame\s+card", req_lower):
            return "MULTIPLE_DEVICE_SAME_CARD", "MEDIUM"

        # Priority 4 — generate a custom rule type from the meaningful words
        return self._generate_custom_rule_type(req_lower), "LOW"

    def _generate_custom_rule_type(self, req_lower: str) -> str:
        stop = {
            "create", "a", "rule", "to", "for", "that", "the", "an", "and", "or",
            "with", "on", "in", "of", "by", "from", "when", "if", "is", "are", "be",
            "will", "should", "monitor", "flag", "alert", "detect", "check", "reject",
            "block", "accept", "transaction", "transactions", "txn", "more", "than",
            "above", "below", "per", "within", "any", "all", "this", "based", "using",
            "where", "which", "has", "have", "been", "each", "its", "up", "do", "does",
            "at", "as", "my", "our", "new", "into", "out", "over", "under",
        }
        words = re.findall(r'\b[a-z]+\b', req_lower)
        meaningful = list(dict.fromkeys(w for w in words if w not in stop and len(w) > 2))
        return "_".join(meaningful[:4]).upper() if meaningful else "CUSTOM_FRAUD_RULE"

    def _detect_action(self, req_lower: str, default: str) -> str:
        if "reject" in req_lower:
            return "REJECT"
        if "block" in req_lower:
            return "BLOCK"
        if "monitor" in req_lower or "flag" in req_lower or "alert" in req_lower:
            return "MONITOR"
        if "accept" in req_lower or "allow" in req_lower:
            return "ACCEPT"
        return default

    # ------------------------------------------------------------------
    # Extraction helpers
    # ------------------------------------------------------------------

    def _extract_count(self, req_lower: str):
        patterns = [
            r"(?:more than|above|over|exceeds?|greater than|>)\s*(\d+)\s*transactions?",
            r"(\d+)\s*or\s*more\s*transactions?",
            r"(\d+)\s*transactions?\s*(?:or more|and above)",
            r"at least\s*(\d+)\s*transactions?",
            r"(\d+)\+?\s*transactions?\s*(?:within|in|per)",
        ]
        for pat in patterns:
            m = re.search(pat, req_lower)
            if m:
                return int(m.group(1))
        return None

    def _extract_amount(self, req_lower: str):
        # Indian denomination notation (checked first — unambiguous)
        lakh = re.search(r"([\d.]+)\s*lakh", req_lower)
        if lakh:
            return int(float(lakh.group(1)) * 100_000)
        crore = re.search(r"([\d.]+)\s*crore", req_lower)
        if crore:
            return int(float(crore.group(1)) * 10_000_000)

        # "below / under / less than N" — unambiguously an amount threshold
        below = re.search(
            r"(?:below|under|less than)\s*(?:rs\.?|inr|₹)?\s*([\d,]+)",
            req_lower,
        )
        if below:
            return int(below.group(1).replace(",", ""))

        # "above / over / exceeding / limit of / threshold of / value of N"
        above = re.search(
            r"(?:above|over|exceeding|limit of|threshold of|value of)\s*(?:rs\.?|inr|₹)?\s*([\d,]+)",
            req_lower,
        )
        if above:
            return int(above.group(1).replace(",", ""))

        # "more than N" — only treat as amount if N is ≥ 4 digits (avoids matching count "more than 3")
        more = re.search(
            r"more than\s*(?:rs\.?|inr|₹)?\s*(\d{4,})",
            req_lower,
        )
        if more:
            return int(more.group(1).replace(",", ""))

        # Fallback: any standalone 4+ digit number in the text
        nums = re.findall(r"\b(\d{4,})\b", req_lower)
        if nums:
            return int(nums[0].replace(",", ""))

        return None

    def _extract_frequency_v2(self, req_lower: str):
        # Explicit numbers first
        m = re.search(r"(\d+)\s*hours?", req_lower)
        if m:
            return int(m.group(1))
        m = re.search(r"(\d+)\s*days?", req_lower)
        if m:
            return int(m.group(1)) * 24
        m = re.search(r"(\d+)\s*weeks?", req_lower)
        if m:
            return int(m.group(1)) * 168
        m = re.search(r"(\d+)\s*months?", req_lower)
        if m:
            return int(m.group(1)) * 720
        m = re.search(r"(\d+)\s*years?", req_lower)
        if m:
            return int(m.group(1)) * 8760
        # Named periods
        if any(k in req_lower for k in ["annual", "yearly", "per year", "in a year"]):
            return 8760
        if any(k in req_lower for k in ["monthly", "per month", "in a month"]):
            return 720
        if any(k in req_lower for k in ["weekly", "per week", "in a week"]):
            return 168
        if any(k in req_lower for k in ["daily", "per day", "within a day", "in a day"]):
            return 24
        return None

    def _extract_percentage(self, req_lower: str):
        m = re.search(r"(\d+(?:\.\d+)?)\s*(?:%|percent)", req_lower)
        return float(m.group(1)) if m else None

    @staticmethod
    def _pad_amount(amount: int) -> str:
        return str(amount).zfill(12)

    @staticmethod
    def _fmt_freq(frequency: int) -> str:
        """Convert hours to a human-readable period string."""
        if frequency % 8760 == 0:
            n = frequency // 8760
            return f"{n} year{'s' if n > 1 else ''}"
        if frequency % 720 == 0:
            n = frequency // 720
            return f"{n} month{'s' if n > 1 else ''}"
        if frequency % 168 == 0:
            n = frequency // 168
            return f"{n} week{'s' if n > 1 else ''}"
        if frequency % 24 == 0:
            n = frequency // 24
            return f"{n} day{'s' if n > 1 else ''}"
        return f"{frequency} hour{'s' if frequency > 1 else ''}"

    # ------------------------------------------------------------------
    # Universal rule builder
    # ------------------------------------------------------------------

    def _build_universal_rule(
        self, rule_type: str, action: str, txn_count, raw_amount,
        frequency, percentage, amount_type: str, requirement: str,
        confidence: str, meta,
    ) -> dict:
        # ---- display name & domain description ----
        if meta:
            display_name, _, _, _, domain = meta
        else:
            display_name = " ".join(w.capitalize() for w in rule_type.split("_")) + " Rule"
            domain = f"{rule_type.replace('_', ' ').lower()} fraud pattern"

        # ---- amount fields ----
        txn_amount_str = None
        max_amount_val = None
        if amount_type == "txn" and raw_amount is not None:
            txn_amount_str = self._pad_amount(raw_amount)
        elif amount_type == "max" and raw_amount is not None:
            max_amount_val = float(raw_amount)

        # ---- description ----
        ruleDescription = self._build_description(
            rule_type, display_name, action, txn_count,
            raw_amount, frequency, percentage, amount_type,
        )

        # ---- explanation ----
        parts = [f"This rule detects {domain}."]
        if txn_count and frequency:
            parts.append(
                f"It triggers {action} when more than {txn_count} transactions are detected "
                f"within {self._fmt_freq(frequency)}."
            )
        elif txn_count:
            parts.append(f"It triggers {action} when more than {txn_count} transactions are detected.")
        elif frequency and not txn_count:
            parts.append(f"It monitors activity within a rolling {self._fmt_freq(frequency)} window.")
        if raw_amount and amount_type == "max":
            parts.append(f"Maximum allowed transaction amount: ₹{raw_amount:,}.")
        elif raw_amount and amount_type == "txn":
            parts.append(f"Per-transaction amount threshold: ₹{raw_amount:,}.")
        if percentage:
            parts.append(f"Deviation threshold: {percentage:.0f}% above customer baseline.")
        if rule_type == "STRUCTURING":
            parts.append("Structuring is a financial crime under PMLA and must be reported to FIU-IND.")
        elif rule_type in ("ANNUAL_TXN_VALUE", "ANNUAL_TXN_VOLUME"):
            parts.append("Annual limits often align with income-tax reporting thresholds — coordinate with compliance.")
        elif rule_type in ("CHARGEBACK_RISK", "REFUND_ABUSE"):
            parts.append("Coordinate with the merchant risk team for investigation and remediation.")
        explanation = " ".join(parts)

        # ---- risk notes ----
        risk_notes = _RISK_NOTES.get(rule_type)
        if risk_notes is None:
            risk_notes = self._generic_risk_notes(rule_type, action, txn_count, raw_amount, frequency, confidence)

        # ---- missing fields ----
        missing = self._compute_missing_fields(rule_type, txn_count, raw_amount, frequency, percentage, amount_type, meta)

        return {
            "ruleName": display_name,
            "ruleDescription": ruleDescription,
            "ruleType": rule_type,
            "action": action,
            "status": "ACTIVE",
            "txnCount": txn_count,
            "txnAmount": txn_amount_str,
            "frequency": frequency,
            "maxAmount": max_amount_val,
            "percentageThreshold": percentage,
            "explanation": explanation,
            "riskNotes": risk_notes,
            "missingFields": missing,
            "confidence": confidence,
        }

    def _build_description(
        self, rule_type: str, display_name: str, action: str,
        txn_count, raw_amount, frequency, percentage, amount_type: str,
    ) -> str:
        act = action.capitalize()
        freq_str = f"within {self._fmt_freq(frequency)}" if frequency else ""

        if amount_type == "max" and raw_amount:
            return f"{act} single transactions exceeding ₹{raw_amount:,}" + (f" {freq_str}" if freq_str else "")

        if txn_count and raw_amount and amount_type == "txn":
            return f"{act} more than {txn_count} transactions below ₹{raw_amount:,}" + (f" {freq_str}" if freq_str else "")

        if txn_count and freq_str:
            return f"{act} more than {txn_count} transactions {freq_str}"

        if raw_amount and amount_type == "txn":
            return f"{act} cumulative transaction value exceeding ₹{raw_amount:,}" + (f" {freq_str}" if freq_str else "")

        if txn_count:
            return f"{act} more than {txn_count} transactions per period"

        if percentage:
            return f"{act} transactions deviating more than {percentage:.0f}% from customer baseline"

        if freq_str:
            return f"{display_name} — {act.lower()} on activity {freq_str}"

        return f"{display_name} — {act.lower()} when trigger conditions are met"

    def _compute_missing_fields(
        self, rule_type: str, txn_count, raw_amount,
        frequency, percentage, amount_type: str, meta,
    ) -> list:
        if meta is None:
            return []
        relevant = meta[2]
        missing = []
        for field in relevant:
            if field == "txnCount" and txn_count is None:
                missing.append("txnCount")
            elif field == "txnAmount" and raw_amount is None and amount_type == "txn":
                missing.append("txnAmount")
            elif field == "maxAmount" and raw_amount is None and amount_type == "max":
                missing.append("maxAmount")
            elif field == "frequency" and frequency is None:
                missing.append("frequency")
            elif field == "percentageThreshold" and percentage is None:
                missing.append("percentageThreshold")
        return missing

    def _generic_risk_notes(self, rule_type, action, txn_count, raw_amount, frequency, confidence) -> list:
        notes = []
        if confidence == "LOW":
            notes.append(
                f"REVIEW REQUIRED: Rule type '{rule_type}' was auto-generated — verify it matches your intent"
            )
        notes.append("Calibrate all threshold parameters against historical transaction data before activation")
        notes.append("Test in staging with representative samples before deploying to production")
        notes.append("Monitor false positive rate for the first 30 days and tune thresholds accordingly")
        if txn_count:
            notes.append(f"Transaction count threshold of {txn_count} should be validated against normal customer behaviour")
        if raw_amount:
            notes.append(f"Amount threshold of ₹{raw_amount:,} should be reviewed by the fraud risk team before deployment")
        notes.append("Coordinate with the compliance team for SAR/CTR reporting obligations triggered by this rule")
        return notes

    # ------------------------------------------------------------------

    def _transaction(self, req: GenerateTransactionRequest) -> dict:
        rule_type = req.ruleType.upper()
        expected = req.expectedResult.upper()
        currency = (req.currency or "INR").upper()

        amount = req.amount
        if amount is None:
            amount_map = {
                ("SINGLE_LARGE_TX", "REJECT"): 150000,
                ("SINGLE_LARGE_TX", "MONITOR"): 120000,
                ("SINGLE_LARGE_TX", "ACCEPT"): 5000,
                ("STRUCTURING", "MONITOR"): 15000,
                ("VELOCITY", "REJECT"): 500,
                ("VELOCITY", "MONITOR"): 500,
            }
            amount = amount_map.get((rule_type, expected), 10000)

        now = datetime.now()
        rrn = now.strftime("%Y%m%d") + "".join(random.choices(string.digits, k=4))
        stan = "".join(random.choices(string.digits, k=6))
        tid = "TID" + "".join(random.choices(string.digits, k=5))
        mid = "MID" + "".join(random.choices(string.digits + string.ascii_uppercase[:6], k=8))
        customer_ref = "CUST" + "".join(random.choices(string.digits, k=6))

        txn_type = "PURCHASE"
        if rule_type in ("ATM_WITHDRAW", "WITHDRAWAL"):
            txn_type = "WITHDRAWAL"
        elif rule_type in ("TRANSFER", "FUND_TRANSFER"):
            txn_type = "FUND_TRANSFER"

        country = "IN" if currency == "INR" else "US"

        return {
            "transaction": {
                "rrn": rrn,
                "stan": stan,
                "tid": tid,
                "mid": mid,
                "amount": str(int(amount)).zfill(11),
                "currency": currency,
                "transactionType": txn_type,
                "timestamp": now.isoformat(),
                "responseCode": "00" if expected == "ACCEPT" else None,
                "customerRef": customer_ref,
                "channel": "POS",
                "country": country,
            }
        }


# ---------------------------------------------------------------------------
# Groq provider — real LLM calls via OpenAI-compatible client
# ---------------------------------------------------------------------------

class GroqLLMProvider(BaseLLMProvider):
    """
    Calls Groq's LLM via the `openai` SDK pointed at Groq's OpenAI-compatible endpoint.

    Two call modes:
      generate_json_response()  — used by all structured endpoints (test cases, explain,
                                   analyze, transaction, rule generation). Returns parsed dict.
      generate_text_response()  — used only by chat(). Returns plain text string.
                                   Temperature is 0.7 (vs. 0.3 for JSON) for natural replies.

    Fallback behavior:
      AI_FALLBACK_TO_MOCK=true  → on any exception, delegate to MockLLMProvider
      AI_FALLBACK_TO_MOCK=false → raise LLMProviderUnavailableError → 503 via global handler

    Exception: generate_test_cases() ALWAYS falls back to the deterministic mock, ignoring
    AI_FALLBACK_TO_MOCK. This guarantees the frontend never gets a "service unavailable"
    error for this specific feature, which is critical to the test creation workflow.

    SECURITY: GROQ_API_KEY is passed to AsyncOpenAI and never logged or returned.
    """

    def __init__(self):
        settings = get_settings()
        if not settings.GROQ_API_KEY:
            raise ValueError(
                "GROQ_API_KEY is not configured. "
                "Set it in .env or switch AI_PROVIDER=mock."
            )
        from openai import AsyncOpenAI
        # AsyncOpenAI is used here for its async support. Groq exposes an OpenAI-compatible
        # API, so we simply point base_url at Groq's endpoint — no Groq-specific SDK needed.
        self._client = AsyncOpenAI(
            api_key=settings.GROQ_API_KEY,
            base_url=settings.GROQ_BASE_URL,
            timeout=float(settings.GROQ_TIMEOUT_SECONDS),
        )
        self._model = settings.GROQ_MODEL
        self._fallback = settings.AI_FALLBACK_TO_MOCK

    async def generate_json_response(self, system_prompt: str, user_prompt: str) -> dict:
        logger.info(f"[LLM] Calling Groq model: {self._model}")
        response = await self._client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.3,
        )
        raw = response.choices[0].message.content
        logger.info("[LLM] Groq response received")
        return parse_llm_json(raw)

    async def _call_with_fallback(
        self, method_name: str, request, system_prompt: str, user_prompt: str
    ) -> dict:
        """
        Calls Groq and applies AI_FALLBACK_TO_MOCK policy on failure.
        Used by explain_rule, analyze_failure, generate_transaction, generate_rule_from_requirement.
        NOT used by generate_test_cases (which has its own always-fallback logic)
        and NOT used by chat (which uses generate_text_response directly).
        """
        try:
            return await self.generate_json_response(system_prompt, user_prompt)
        except Exception as exc:
            logger.warning(
                f"[LLM] Groq API failed for {method_name}: {type(exc).__name__}"
            )
            if self._fallback:
                logger.warning("[LLM] Falling back to mock provider")
                mock = MockLLMProvider()
                return await getattr(mock, method_name)(request)
            # AI_FALLBACK_TO_MOCK=false → surface the failure as 503 to Spring Boot
            raise LLMProviderUnavailableError(
                "AI provider is currently unavailable"
            ) from exc

    async def generate_test_cases(self, request: GenerateTestCasesRequest) -> dict:
        # Unlike other endpoints, test case generation ALWAYS falls back to the deterministic
        # mock on any Groq failure, regardless of AI_FALLBACK_TO_MOCK. This ensures the
        # frontend never receives a "service unavailable" error during test creation —
        # getting 3 good-enough test cases is always better than a 503.
        rule_type = (request.ruleType or "UNKNOWN").upper()
        logger.info(f"[AI TEST CASES] Provider selected: groq")
        try:
            from app.prompts.rule_test_case_prompt import build_test_case_prompt
            logger.info("[AI TEST CASES] Calling Groq")
            sys_p, usr_p = build_test_case_prompt(request)
            result = await self.generate_json_response(sys_p, usr_p)
            groq_cases = result.get("testCases", [])
            if not isinstance(groq_cases, list) or not groq_cases:
                raise ValueError(f"Groq returned invalid testCases: {type(groq_cases)}")
            # Merge Groq results with deterministic fallbacks to guarantee ACCEPT/MONITOR/REJECT
            normalized = _normalize_to_three_cases(groq_cases, request)
            logger.info(f"[AI TEST CASES] Groq returned {len(groq_cases)} case(s), normalized to 3")
            return {"ruleType": rule_type, "testCases": normalized}
        except Exception as exc:
            logger.warning(
                f"[AI TEST CASES] Groq failed, falling back to mock: {type(exc).__name__}"
            )
            return _build_deterministic_test_cases(request)

    async def explain_rule(self, request: ExplainRuleRequest) -> dict:
        from app.prompts.rule_explanation_prompt import build_explanation_prompt
        sys_p, usr_p = build_explanation_prompt(request)
        return await self._call_with_fallback("explain_rule", request, sys_p, usr_p)

    async def analyze_failure(self, request: AnalyzeFailureRequest) -> dict:
        from app.prompts.failure_analysis_prompt import build_failure_analysis_prompt
        sys_p, usr_p = build_failure_analysis_prompt(request)
        return await self._call_with_fallback("analyze_failure", request, sys_p, usr_p)

    async def generate_transaction(self, request: GenerateTransactionRequest) -> dict:
        from app.prompts.transaction_generation_prompt import build_transaction_prompt
        sys_p, usr_p = build_transaction_prompt(request)
        return await self._call_with_fallback("generate_transaction", request, sys_p, usr_p)

    async def generate_rule_from_requirement(self, request: GenerateRuleRequest) -> dict:
        from app.prompts.rule_generation_prompt import build_rule_generation_prompt
        sys_p, usr_p = build_rule_generation_prompt(request.requirement)
        return await self._call_with_fallback(
            "generate_rule_from_requirement", request, sys_p, usr_p
        )

    async def generate_text_response(self, system_prompt: str, user_prompt: str) -> str:
        logger.info(f"[LLM] Calling Groq model (text): {self._model}")
        response = await self._client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.7,
        )
        raw = response.choices[0].message.content
        logger.info("[LLM] Groq text response received")
        return raw.strip()

    async def chat(self, message: str, context: dict) -> str:
        logger.info("[AI CHAT] provider=groq")
        logger.info(f"[AI CHAT] message={message[:120]}")
        logger.info(f"[AI CHAT] context={context}")
        logger.info("[AI CHAT] calling LLM")
        try:
            from app.prompts.chat_prompt import build_chat_prompt
            sys_p, usr_p = build_chat_prompt(message, context)
            reply = await self.generate_text_response(sys_p, usr_p)
            logger.info("[AI CHAT] response generated")
            return reply
        except Exception as exc:
            logger.warning(f"[AI CHAT] Groq failed: {type(exc).__name__}")
            if self._fallback:
                logger.warning("[AI CHAT] Falling back to mock")
                return MockLLMProvider()._mock_chat_reply(message)
            raise LLMProviderUnavailableError(
                "AI provider is currently unavailable"
            ) from exc


# ---------------------------------------------------------------------------
# OpenAI provider — placeholder
# ---------------------------------------------------------------------------

class OpenAIProvider(BaseLLMProvider):
    """Placeholder for OpenAI integration. Set AI_PROVIDER=openai and provide OPENAI_API_KEY."""

    def __init__(self):
        settings = get_settings()
        if not settings.OPENAI_API_KEY:
            raise ValueError(
                "OPENAI_API_KEY is not configured. "
                "Set it in .env or switch AI_PROVIDER=mock."
            )
        self.api_key = settings.OPENAI_API_KEY
        self.model = settings.OPENAI_MODEL

    async def generate_test_cases(self, request: GenerateTestCasesRequest) -> dict:
        raise NotImplementedError("OpenAI provider is not yet implemented. Use AI_PROVIDER=mock.")

    async def explain_rule(self, request: ExplainRuleRequest) -> dict:
        raise NotImplementedError("OpenAI provider is not yet implemented. Use AI_PROVIDER=mock.")

    async def analyze_failure(self, request: AnalyzeFailureRequest) -> dict:
        raise NotImplementedError("OpenAI provider is not yet implemented. Use AI_PROVIDER=mock.")

    async def generate_transaction(self, request: GenerateTransactionRequest) -> dict:
        raise NotImplementedError("OpenAI provider is not yet implemented. Use AI_PROVIDER=mock.")

    async def generate_rule_from_requirement(self, request: GenerateRuleRequest) -> dict:
        raise NotImplementedError("OpenAI provider is not yet implemented. Use AI_PROVIDER=mock.")

    async def chat(self, message: str, context: dict) -> str:
        raise NotImplementedError("OpenAI provider is not yet implemented. Use AI_PROVIDER=mock.")


# ---------------------------------------------------------------------------
# Anthropic provider — placeholder
# ---------------------------------------------------------------------------

class AnthropicProvider(BaseLLMProvider):
    """Placeholder for Anthropic Claude integration. Set AI_PROVIDER=anthropic and ANTHROPIC_API_KEY."""

    def __init__(self):
        settings = get_settings()
        if not settings.ANTHROPIC_API_KEY:
            raise ValueError(
                "ANTHROPIC_API_KEY is not configured. "
                "Set it in .env or switch AI_PROVIDER=mock."
            )
        self.api_key = settings.ANTHROPIC_API_KEY
        self.model = settings.ANTHROPIC_MODEL

    async def generate_test_cases(self, request: GenerateTestCasesRequest) -> dict:
        raise NotImplementedError("Anthropic provider is not yet implemented. Use AI_PROVIDER=mock.")

    async def explain_rule(self, request: ExplainRuleRequest) -> dict:
        raise NotImplementedError("Anthropic provider is not yet implemented. Use AI_PROVIDER=mock.")

    async def analyze_failure(self, request: AnalyzeFailureRequest) -> dict:
        raise NotImplementedError("Anthropic provider is not yet implemented. Use AI_PROVIDER=mock.")

    async def generate_transaction(self, request: GenerateTransactionRequest) -> dict:
        raise NotImplementedError("Anthropic provider is not yet implemented. Use AI_PROVIDER=mock.")

    async def generate_rule_from_requirement(self, request: GenerateRuleRequest) -> dict:
        raise NotImplementedError("Anthropic provider is not yet implemented. Use AI_PROVIDER=mock.")

    async def chat(self, message: str, context: dict) -> str:
        raise NotImplementedError("Anthropic provider is not yet implemented. Use AI_PROVIDER=mock.")


# ---------------------------------------------------------------------------
# Factory
# ---------------------------------------------------------------------------

def get_llm_provider() -> BaseLLMProvider:
    """
    Returns the configured LLM provider instance.

    Provider selection: reads AI_PROVIDER from settings (mock | groq | openai | anthropic).
    Called once per request — providers are lightweight to instantiate.

    Groq init failure (missing API key) falls back silently to mock so the service
    stays functional even with a misconfigured .env during development. OpenAI and
    Anthropic raise immediately because they are not yet production-supported.

    Unknown AI_PROVIDER values fall back to mock with a warning rather than crashing.
    """
    settings = get_settings()
    provider = settings.AI_PROVIDER.lower().strip()
    logger.info(f"[LLM] Provider selected: {provider}")

    if provider == "mock":
        return MockLLMProvider()
    elif provider == "groq":
        try:
            return GroqLLMProvider()
        except ValueError as exc:
            # Missing GROQ_API_KEY — warn and degrade gracefully to mock
            logger.warning(f"[LLM] Groq provider init failed: {exc}. Falling back to mock.")
            return MockLLMProvider()
    elif provider == "openai":
        return OpenAIProvider()
    elif provider == "anthropic":
        return AnthropicProvider()
    else:
        logger.warning(f"[LLM] Unknown AI_PROVIDER='{provider}', falling back to mock")
        return MockLLMProvider()
