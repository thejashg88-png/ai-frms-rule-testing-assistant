from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class ApiResponse(BaseModel):
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
    summary: str
    businessMeaning: str
    technicalMeaning: str
    exampleScenario: str
    riskNotes: List[str]


class FailureAnalysisData(BaseModel):
    possibleReasons: List[str]
    debuggingSteps: List[str]
    recommendedFix: str
    riskImpact: str


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
    reply: str
    context: dict = Field(default_factory=dict)
