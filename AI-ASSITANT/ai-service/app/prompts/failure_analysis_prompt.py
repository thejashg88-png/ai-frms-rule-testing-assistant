"""
Prompt builder for POST /api/ai/analyze-failure (Groq provider path).

The actual ruleType from the request is embedded in the user prompt so the LLM
produces rule-specific guidance. Do NOT substitute a default ruleType (e.g. SINGLE_LARGE_TX)
when the real one is unknown — the analysis will be misleading for count-based rules.

Expected JSON output keys: possibleReasons (list), debuggingSteps (list),
recommendedFix (string), riskImpact (string).
"""
from app.models.request_models import AnalyzeFailureRequest
from app.utils.prompt_utils import json_output_instruction


SYSTEM_PROMPT = """You are a senior QA engineer and fraud systems debugger specializing in FRMS rule engines.
Analyze test case failures and provide actionable debugging guidance.
Be specific, practical, and prioritize the most likely root causes.
""" + json_output_instruction()


def build_failure_analysis_prompt(request: AnalyzeFailureRequest) -> tuple[str, str]:
    input_data_str = str(request.inputData) if request.inputData else "Not provided"
    logs_str = request.executionLogs or "Not provided"

    user_prompt = f"""Analyze this test case failure:

Test Case Name: {request.testCaseName}
Rule Type: {request.ruleType}
Expected Result: {request.expectedResult}
Actual Result: {request.actualResult}
Input Data: {input_data_str}
Execution Logs: {logs_str}

Return JSON in exactly this format:
{{
  "possibleReasons": [
    "Reason 1",
    "Reason 2",
    "Reason 3"
  ],
  "debuggingSteps": [
    "Step 1: ...",
    "Step 2: ...",
    "Step 3: ..."
  ],
  "recommendedFix": "Specific actionable fix recommendation",
  "riskImpact": "What happens in production if this bug is not fixed"
}}"""

    return SYSTEM_PROMPT, user_prompt
