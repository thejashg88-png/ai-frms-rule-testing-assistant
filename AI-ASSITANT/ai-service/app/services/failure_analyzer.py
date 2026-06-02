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
        """Returns possibleReasons, debuggingSteps, recommendedFix, and riskImpact."""
        logger.info(
            f"Analyzing failure | case={request.testCaseName} "
            f"| expected={request.expectedResult} | actual={request.actualResult}"
        )
        result = await self._llm.analyze_failure(request)
        logger.info(f"Failure analysis complete | reasons={len(result.get('possibleReasons', []))}")
        return result
