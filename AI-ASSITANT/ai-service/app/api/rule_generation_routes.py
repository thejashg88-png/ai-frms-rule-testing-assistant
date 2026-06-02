"""
Route: POST /api/ai/generate-rule

Converts a plain-English business requirement into a structured FRMS rule configuration.
Returns ruleName, ruleType, action, threshold fields, explanation, riskNotes, and confidence.
Used by the "Generate Rule from Requirement" feature in the rule creation UI.
"""
from fastapi import APIRouter, HTTPException
from loguru import logger

from app.models.request_models import GenerateRuleRequest
from app.models.response_models import ApiResponse
from app.services.rule_generator import RuleGeneratorService

router = APIRouter(tags=["Rule Generation"])


@router.post(
    "/generate-rule",
    response_model=ApiResponse,
    summary="Generate Rule from Natural Language Requirement",
    description=(
        "Accepts a plain-English business requirement and returns a structured FRMS rule configuration. "
        "Supports rule types: STRUCTURING, SINGLE_LARGE_TX, DAILY_TXN_VALUE, HIGH_FREQ_TXN, "
        "UNUSUAL_AMT, ABNORMAL_HOUR, ROUND_AMT_TXN."
    ),
)
async def generate_rule(request: GenerateRuleRequest):
    try:
        service = RuleGeneratorService()
        data = await service.generate(request)
        return ApiResponse(
            success=True,
            message="Rule suggestion generated successfully",
            data=data,
        )
    except NotImplementedError as exc:
        logger.warning(f"Provider not implemented: {exc}")
        raise HTTPException(status_code=501, detail=str(exc))
    except Exception as exc:
        logger.exception(f"Unexpected error in generate_rule: {exc}")
        raise HTTPException(status_code=500, detail="Internal server error while generating rule suggestion.")
