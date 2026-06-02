from loguru import logger

from app.models.request_models import AnalyzeFailureRequest
from app.services.llm_service import get_llm_provider


class FailureAnalyzerService:
    def __init__(self):
        self._llm = get_llm_provider()

    async def analyze(self, request: AnalyzeFailureRequest) -> dict:
        logger.info(
            f"Analyzing failure | case={request.testCaseName} "
            f"| expected={request.expectedResult} | actual={request.actualResult}"
        )
        result = await self._llm.analyze_failure(request)
        logger.info(f"Failure analysis complete | reasons={len(result.get('possibleReasons', []))}")
        return result
