"""
Pydantic request models for all AI service endpoints.

Field naming follows the FRMS backend convention (camelCase) so Spring Boot
can deserialize directly without field mapping.

Amount encoding:
    txnAmount  — zero-padded 11/12-digit string, e.g. "00000050000" = ₹50,000
    maxAmount  — plain float, e.g. 100000.0 = ₹1,00,000

ruleType should come from the FRMS backend's rule configuration, not be guessed
by the frontend. The AI service uses ruleType to select the correct logic path.
"""
from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class GenerateTestCasesRequest(BaseModel):
    """
    Generates exactly 3 test cases (ACCEPT / MONITOR / REJECT) for a fraud rule.
    Provide as many threshold fields as are configured for the rule — the more context,
    the more realistic the generated test data (amounts, counts, time windows).
    """

    ruleName: str = Field(..., description="Name of the fraud rule")
    ruleType: str = Field(..., description="Type: STRUCTURING, SINGLE_LARGE_TX, VELOCITY, etc.")
    action: str = Field(..., description="Rule action: MONITOR, REJECT, ACCEPT, BLOCK")
    txnCount: Optional[int] = Field(None, description="Number of transactions threshold")
    txnAmount: Optional[str] = Field(None, description="Amount threshold (zero-padded string)")
    frequency: Optional[int] = Field(None, description="Time window in hours")
    maxAmount: Optional[float] = Field(None, description="Maximum single transaction amount")
    percentageThreshold: Optional[float] = Field(None, description="Percentage threshold for ratio-based rules")
    description: Optional[str] = Field(None, description="Human-readable rule description")

    model_config = {
        "json_schema_extra": {
            "example": {
                "ruleName": "Structuring Transaction Monitor Rule",
                "ruleType": "STRUCTURING",
                "action": "MONITOR",
                "txnCount": 2,
                "txnAmount": "000000050000",
                "frequency": 24,
                "description": "Detect multiple small transactions within 24 hours"
            }
        }
    }


class ExplainRuleRequest(BaseModel):
    """
    Returns a human-readable explanation of a fraud rule covering:
    summary, businessMeaning, technicalMeaning, exampleScenario, riskNotes.
    Used by Spring Boot when the user clicks "Explain Rule" in the frontend.
    """

    ruleName: str = Field(..., description="Name of the fraud rule")
    ruleType: str = Field(..., description="Type: STRUCTURING, SINGLE_LARGE_TX, VELOCITY, etc.")
    action: str = Field(..., description="Rule action: MONITOR, REJECT, ACCEPT, BLOCK")
    txnCount: Optional[int] = Field(None, description="Transaction count threshold")
    txnAmount: Optional[str] = Field(None, description="Amount threshold (zero-padded string)")
    frequency: Optional[int] = Field(None, description="Time window in hours")
    maxAmount: Optional[float] = Field(None, description="Maximum single transaction amount")
    percentageThreshold: Optional[float] = Field(None, description="Percentage threshold")
    description: Optional[str] = Field(None, description="Human-readable rule description")

    model_config = {
        "json_schema_extra": {
            "example": {
                "ruleName": "Single Large Transaction Reject Rule",
                "ruleType": "SINGLE_LARGE_TX",
                "action": "REJECT",
                "maxAmount": 100000,
                "description": "Reject transaction above max amount"
            }
        }
    }


class AnalyzeFailureRequest(BaseModel):
    """
    Analyzes why a test case returned an unexpected result.
    The analysis is ruleType-specific — always pass the exact ruleType from the rule
    configuration. Do NOT default to SINGLE_LARGE_TX for unrecognized types; the
    analysis reasoning will be wrong for count-based rules like HIGH_FREQ_TXN.

    Enriched fields (optional, sent by Spring Boot when available):
        ruleConfig              — full rule configuration dict (txnCount, frequencyHours, action, etc.)
        testCaseInput           — the test case input data sent to the rule engine
        failureReason           — short reason string from the rule engine (e.g. "Threshold not reached")
        matchedCount            — total transactions evaluated by the rule (historical + current)
        requiredCount           — configured txnCount threshold the rule needs to trigger
        historicalTransactionCount — prior transactions found for the same card/customer/device
        currentCount            — the current transaction being evaluated
        frequencyWindow         — human-readable window string (e.g. "last 1 hour")
        ruleExplanation         — structured explanation of what the rule does
        executionTrace          — ordered list of rule evaluation steps from the engine

    All enriched fields are Optional and default to None — missing fields do not break
    existing behavior; the prompt builder includes only fields that are present.
    """

    # Core fields (always required)
    testCaseName: str = Field(..., description="Name of the failed test case")
    ruleType: str = Field(..., description="Type of the fraud rule being tested")
    expectedResult: str = Field(..., description="Expected outcome: MONITOR, REJECT, ACCEPT")
    actualResult: str = Field(..., description="Actual outcome returned by the rule engine")
    inputData: Optional[Dict[str, Any]] = Field(None, description="Transaction input data used in the test")
    executionLogs: Optional[str] = Field(None, description="Rule engine execution logs")

    # Enriched context fields (optional — sent by Spring Boot when available)
    ruleConfig: Optional[Dict[str, Any]] = Field(None, description="Full rule configuration (txnCount, frequencyHours, action, etc.)")
    testCaseInput: Optional[Dict[str, Any]] = Field(None, description="Test case input sent to the rule engine")
    failureReason: Optional[str] = Field(None, description="Short failure reason from the rule engine")
    matchedCount: Optional[int] = Field(None, description="Total transactions evaluated (historical + current)")
    requiredCount: Optional[int] = Field(None, description="Configured txnCount threshold required to trigger the rule")
    historicalTransactionCount: Optional[int] = Field(None, description="Prior transactions found for the same card/customer/device")
    currentCount: Optional[int] = Field(None, description="Current transaction count in this evaluation")
    frequencyWindow: Optional[str] = Field(None, description="Human-readable frequency window (e.g. 'last 1 hour')")
    ruleExplanation: Optional[Dict[str, Any]] = Field(None, description="Structured rule explanation")
    executionTrace: Optional[list] = Field(None, description="Ordered rule evaluation steps from the engine")

    model_config = {
        "json_schema_extra": {
            "example": {
                "testCaseName": "High Frequency Monitor Case",
                "ruleType": "HIGH_FREQ_TXN",
                "ruleConfig": {"txnCount": 5, "frequencyHours": 1, "action": "MONITOR"},
                "testCaseInput": {"amount": 150000, "cardNumber": "****3333"},
                "expectedResult": "MONITOR",
                "actualResult": "ACCEPT",
                "failureReason": "Threshold not reached",
                "matchedCount": 1,
                "historicalTransactionCount": 0,
                "currentCount": 1,
                "requiredCount": 5,
                "frequencyWindow": "last 1 hour"
            }
        }
    }


class GenerateTransactionRequest(BaseModel):
    """
    Generates a dummy ISO 8583-style transaction payload suitable for testing a specific rule.
    Amount is auto-selected based on ruleType + expectedResult if not provided.
    Used by the "Generate Transaction" feature in the test execution UI.
    """

    ruleType: str = Field(..., description="Rule type to tailor the transaction for")
    expectedResult: str = Field(..., description="Desired outcome: MONITOR, REJECT, ACCEPT")
    amount: Optional[float] = Field(None, description="Transaction amount (auto-generated if not provided)")
    currency: str = Field("INR", description="ISO 4217 currency code")

    model_config = {
        "json_schema_extra": {
            "example": {
                "ruleType": "SINGLE_LARGE_TX",
                "expectedResult": "REJECT",
                "amount": 150000,
                "currency": "INR"
            }
        }
    }


class GenerateRuleRequest(BaseModel):
    """
    Converts a plain-English business requirement into a structured FRMS rule configuration.
    The AI service infers ruleType, action, and threshold fields from the text.
    Confidence is HIGH if an explicit rule type appears, MEDIUM if inferred, LOW if generated.
    """

    requirement: str = Field(
        ...,
        description="Natural language business requirement describing the fraud rule to generate",
        min_length=10,
    )

    model_config = {
        "json_schema_extra": {
            "example": {
                "requirement": "Create a rule to monitor more than 3 transactions below 50000 within 24 hours"
            }
        }
    }


class AiChatRequest(BaseModel):
    """
    Free-form chat with the FRMS AI assistant.
    Used by the Spring Boot POST /api/ai/chat endpoint which proxies to this service.
    context is optional — pass rule configuration or test result details to get
    more specific answers. message must be non-empty (min_length=1 enforced).
    """

    message: str = Field(..., description="User's chat message", min_length=1)
    context: dict = Field(default_factory=dict, description="Optional context for the conversation")

    model_config = {
        "json_schema_extra": {
            "example": {
                "message": "Why did my High Frequency Transaction test fail?",
                "context": {}
            }
        }
    }
