from loguru import logger

from app.models.request_models import GenerateRuleRequest
from app.services.llm_service import get_llm_provider


class RuleGeneratorService:
    async def generate(self, request: GenerateRuleRequest) -> dict:
        llm = get_llm_provider()
        logger.info(f"RuleGeneratorService: generating rule for requirement='{request.requirement[:80]}'")
        result = await llm.generate_rule_from_requirement(request)
        logger.info(f"RuleGeneratorService: generated ruleType={result.get('ruleType')} action={result.get('action')}")
        return result
