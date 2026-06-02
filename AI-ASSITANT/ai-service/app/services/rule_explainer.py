from loguru import logger

from app.models.request_models import ExplainRuleRequest
from app.services.llm_service import get_llm_provider


class RuleExplainerService:
    """
    Thin service layer for POST /api/ai/explain-rule.
    Selects the configured LLM provider and delegates the explanation request.
    Mock provider returns template-based explanations keyed on ruleType.
    Groq provider sends a structured JSON prompt and parses the result.
    """

    def __init__(self):
        self._llm = get_llm_provider()

    async def explain(self, request: ExplainRuleRequest) -> dict:
        """Builds and returns a structured rule explanation dict."""
        logger.info(
            f"Explaining rule | name={request.ruleName} | type={request.ruleType} | action={request.action}"
        )
        result = await self._llm.explain_rule(request)
        logger.info("Rule explanation generated successfully")
        return result
