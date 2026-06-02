from fastapi import APIRouter, HTTPException
from loguru import logger

from app.models.request_models import GenerateTestCasesRequest
from app.models.response_models import ApiResponse
from app.services.test_case_generator import TestCaseGeneratorService

router = APIRouter(tags=["Test Case Generation"])


@router.post(
    "/generate-test-cases",
    response_model=ApiResponse,
    summary="Generate Rule Test Cases",
)
async def generate_test_cases(request: GenerateTestCasesRequest):
    try:
        service = TestCaseGeneratorService()
        data = await service.generate(request)
        return ApiResponse(
            success=True,
            message="Test cases generated successfully",
            data=data,
        )
    except NotImplementedError as exc:
        logger.warning(f"Provider not implemented: {exc}")
        raise HTTPException(status_code=501, detail=str(exc))
    except Exception as exc:
        logger.exception(f"Unexpected error in generate_test_cases: {exc}")
        raise HTTPException(status_code=500, detail="Internal server error while generating test cases.")
