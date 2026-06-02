"""
Route: POST /api/ai/explain-rule

Called by Spring Boot when the user requests an explanation for a configured fraud rule.
Delegates to RuleExplainerService which selects the appropriate LLM provider.
Response data fields: summary, businessMeaning, technicalMeaning, exampleScenario, riskNotes.
"""
from fastapi import APIRouter, HTTPException
from loguru import logger

from app.models.request_models import ExplainRuleRequest
from app.models.response_models import ApiResponse
from app.services.rule_explainer import RuleExplainerService

router = APIRouter(tags=["Rule Explanation"])


@router.post(
    "/explain-rule",
    response_model=ApiResponse,
    summary="Explain Fraud Rule",
)
async def explain_rule(request: ExplainRuleRequest):
    try:
        service = RuleExplainerService()
        data = await service.explain(request)
        return ApiResponse(
            success=True,
            message="Rule explanation generated successfully",
            data=data,
        )
    except NotImplementedError as exc:
        logger.warning(f"Provider not implemented: {exc}")
        raise HTTPException(status_code=501, detail=str(exc))
    except Exception as exc:
        logger.exception(f"Unexpected error in explain_rule: {exc}")
        raise HTTPException(status_code=500, detail="Internal server error while explaining rule.")
