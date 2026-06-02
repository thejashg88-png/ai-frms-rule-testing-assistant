from fastapi import APIRouter, HTTPException
from loguru import logger

from app.models.request_models import AnalyzeFailureRequest
from app.models.response_models import ApiResponse
from app.services.failure_analyzer import FailureAnalyzerService

router = APIRouter(tags=["Failure Analysis"])


@router.post(
    "/analyze-failure",
    response_model=ApiResponse,
    summary="Analyze Test Case Failure",
)
async def analyze_failure(request: AnalyzeFailureRequest):
    try:
        service = FailureAnalyzerService()
        data = await service.analyze(request)
        return ApiResponse(
            success=True,
            message="Failure analysis completed successfully",
            data=data,
        )
    except NotImplementedError as exc:
        logger.warning(f"Provider not implemented: {exc}")
        raise HTTPException(status_code=501, detail=str(exc))
    except Exception as exc:
        logger.exception(f"Unexpected error in analyze_failure: {exc}")
        raise HTTPException(status_code=500, detail="Internal server error while analyzing failure.")
