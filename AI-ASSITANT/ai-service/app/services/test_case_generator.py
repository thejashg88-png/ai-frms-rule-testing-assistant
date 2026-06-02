from loguru import logger

from app.models.request_models import GenerateTestCasesRequest
from app.services.llm_service import _build_deterministic_test_cases, get_llm_provider

_REQUIRED_RESULTS = {"ACCEPT", "MONITOR", "REJECT"}


class TestCaseGeneratorService:
    def __init__(self):
        self._llm = get_llm_provider()

    async def generate(self, request: GenerateTestCasesRequest) -> dict:
        rule_type = (request.ruleType or "UNKNOWN").upper()
        logger.info(
            f"[AI TEST CASES] Generating test cases | rule={request.ruleName} | "
            f"type={rule_type} | action={request.action}"
        )

        result = await self._llm.generate_test_cases(request)

        # Safety validation — guarantee exactly 3 cases with ACCEPT/MONITOR/REJECT
        test_cases = result.get("testCases") if isinstance(result, dict) else None
        if not isinstance(test_cases, list) or len(test_cases) != 3:
            logger.warning(
                f"[AI TEST CASES] Unexpected case count ({len(test_cases) if isinstance(test_cases, list) else 'none'}), "
                "rebuilding with deterministic fallback"
            )
            result = _build_deterministic_test_cases(request)
            test_cases = result["testCases"]

        actual_results = {c.get("expectedResult") for c in test_cases}
        if not _REQUIRED_RESULTS.issubset(actual_results):
            logger.warning(
                f"[AI TEST CASES] Missing required outcomes {_REQUIRED_RESULTS - actual_results}, "
                "rebuilding with deterministic fallback"
            )
            result = _build_deterministic_test_cases(request)

        logger.info(f"[AI TEST CASES] Returning {len(result.get('testCases', []))} test cases")
        return result
