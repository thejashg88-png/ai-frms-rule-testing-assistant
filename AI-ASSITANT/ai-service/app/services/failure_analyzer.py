from loguru import logger

from app.models.request_models import AnalyzeFailureRequest
from app.services.llm_service import get_llm_provider


class FailureAnalyzerService:
    """
    Thin service layer for POST /api/ai/analyze-failure.
    Analysis is rule-type-aware — the ruleType from the request drives which
    debugging guidance is returned (count-based rules vs. amount-based rules
    have fundamentally different failure modes).
    """

    def __init__(self):
        self._llm = get_llm_provider()

    async def analyze(self, request: AnalyzeFailureRequest) -> dict:
        """Returns summary, rootCause, possibleReasons, debuggingSteps, recommendedFix, riskImpact, confidence."""
        logger.info(
            f"[FAILURE ANALYSIS] ruleType={request.ruleType} "
            f"| case={request.testCaseName} "
            f"| expected={request.expectedResult} | actual={request.actualResult}"
        )
        if request.matchedCount is not None:
            logger.info(f"[FAILURE ANALYSIS] matchedCount={request.matchedCount}")
        if request.historicalTransactionCount is not None:
            logger.info(f"[FAILURE ANALYSIS] historicalCount={request.historicalTransactionCount}")
        if request.requiredCount is not None:
            logger.info(f"[FAILURE ANALYSIS] requiredCount={request.requiredCount}")
        if request.frequencyWindow:
            logger.info(f"[FAILURE ANALYSIS] frequencyWindow={request.frequencyWindow}")

        has_enriched = any([
            request.ruleConfig,
            request.matchedCount is not None,
            request.requiredCount is not None,
            request.historicalTransactionCount is not None,
            request.executionTrace,
            request.failureReason,
        ])
        logger.info(f"[FAILURE ANALYSIS] prompt uses enriched context={has_enriched}")

        result = await self._llm.analyze_failure(request)
        logger.info(f"Failure analysis complete | reasons={len(result.get('possibleReasons', []))}")
        return result
