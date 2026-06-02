from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class GenerateTestCasesRequest(BaseModel):
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
    testCaseName: str = Field(..., description="Name of the failed test case")
    ruleType: str = Field(..., description="Type of the fraud rule being tested")
    expectedResult: str = Field(..., description="Expected outcome: MONITOR, REJECT, ACCEPT")
    actualResult: str = Field(..., description="Actual outcome returned by the rule engine")
    inputData: Optional[Dict[str, Any]] = Field(None, description="Transaction input data used in the test")
    executionLogs: Optional[str] = Field(None, description="Rule engine execution logs")

    model_config = {
        "json_schema_extra": {
            "example": {
                "testCaseName": "Structuring monitor case",
                "ruleType": "STRUCTURING",
                "expectedResult": "MONITOR",
                "actualResult": "ACCEPT",
                "inputData": {},
                "executionLogs": "Rule did not trigger"
            }
        }
    }


class GenerateTransactionRequest(BaseModel):
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
