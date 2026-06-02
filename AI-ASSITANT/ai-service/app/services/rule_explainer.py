from loguru import logger

from app.models.request_models import ExplainRuleRequest
from app.services.llm_service import get_llm_provider


class RuleExplainerService:
    def __init__(self):
        self._llm = get_llm_provider()

    async def explain(self, request: ExplainRuleRequest) -> dict:
        logger.info(
            f"Explaining rule | name={request.ruleName} | type={request.ruleType} | action={request.action}"
        )
        result = await self._llm.explain_rule(request)
        logger.info("Rule explanation generated successfully")
        return result
