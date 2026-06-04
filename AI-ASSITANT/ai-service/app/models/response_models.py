"""
Pydantic response models for all AI service endpoints.

All endpoints return ApiResponse as the outer wrapper:
    { "success": true, "message": "...", "data": { ... } }

Spring Boot deserializes the `data` field into its own DTO. The frontend
reads specific keys from `data` (e.g. summary, testCases, reply) — changing
key names here will break the Spring Boot ↔ FastAPI contract.
"""
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class ApiResponse(BaseModel):
    """
    Standard envelope returned by every endpoint.
    On failure, success=False, data=None, and message describes the error.
    On success, data contains the endpoint-specific payload model.
    """

    success: bool
    message: str
    data: Optional[Any] = None


class HealthResponse(BaseModel):
    status: str
    service: str


class TestCase(BaseModel):
    testCaseName: str
    description: str
    inputData: Dict[str, Any]
    expectedResult: str


class TestCasesData(BaseModel):
    ruleType: str
    testCases: List[TestCase]


class RuleExplanationData(BaseModel):
    """
    Structured explanation of a fraud rule.
    All five fields are required — Spring Boot maps them to its AiRuleExplanationResponse DTO.
    """

    summary: str
    businessMeaning: str
    technicalMeaning: str
    exampleScenario: str
    riskNotes: List[str]


class FailureAnalysisData(BaseModel):
    """
    Failure analysis result for a test case that returned an unexpected outcome.

    New fields (added when Spring Boot sends enriched context):
        summary    — one-sentence description of what happened and why
        rootCause  — single most likely root cause
        confidence — 0-100 integer; how confident the AI is given the context provided

    Legacy fields (always present for backward compatibility):
        possibleReasons and debuggingSteps are always non-empty lists.
        riskImpact communicates the production consequence of leaving the bug unfixed.
    """

    summary: Optional[str] = None
    rootCause: Optional[str] = None
    possibleReasons: List[str]
    debuggingSteps: List[str]
    recommendedFix: str
    riskImpact: str
    confidence: Optional[int] = None


class TransactionPayload(BaseModel):
    rrn: str
    stan: str
    tid: str
    mid: str
    amount: str
    currency: str
    transactionType: str
    timestamp: Optional[str] = None
    responseCode: Optional[str] = None
    customerRef: Optional[str] = None
    channel: Optional[str] = None
    country: Optional[str] = None


class TransactionData(BaseModel):
    transaction: TransactionPayload


class RuleGenerationData(BaseModel):
    ruleName: str
    ruleDescription: str
    ruleType: str
    action: str
    status: str
    txnCount: Optional[int] = None
    txnAmount: Optional[str] = None
    frequency: Optional[int] = None
    maxAmount: Optional[float] = None
    percentageThreshold: Optional[float] = None
    explanation: str
    riskNotes: List[str]
    missingFields: List[str] = []
    confidence: str = "MEDIUM"


class AiChatData(BaseModel):
    """
    Response for POST /api/ai/chat.
    reply is the AI-generated plain text answer (not JSON).
    context echoes back the request context — Spring Boot passes it through to the frontend.
    """

    reply: str
    context: dict = Field(default_factory=dict)
