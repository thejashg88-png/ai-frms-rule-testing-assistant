from app.models.request_models import GenerateTestCasesRequest
from app.utils.prompt_utils import build_rule_context, json_output_instruction


SYSTEM_PROMPT = """You are an expert fraud detection engineer and QA specialist for a Financial Risk Management System (FRMS).
Your job is to generate comprehensive, realistic test cases for fraud detection rules used in payment processing systems.

Each test case must include:
- A meaningful name describing the scenario
- A clear description of what is being tested
- Realistic transaction input data (amount as zero-padded 11-digit string, ISO timestamps)
- The expected rule outcome

Always generate at least 3 test cases: one that triggers the rule, one boundary case, and one that should NOT trigger.
""" + json_output_instruction()


def build_test_case_prompt(request: GenerateTestCasesRequest) -> tuple[str, str]:
    rule_context = build_rule_context(request.model_dump(exclude_none=True))

    user_prompt = f"""Generate test cases for this fraud detection rule:

{rule_context}

Return JSON in exactly this format:
{{
  "ruleType": "{request.ruleType}",
  "testCases": [
    {{
      "testCaseName": "...",
      "description": "...",
      "inputData": {{ ... }},
      "expectedResult": "{request.action}"
    }}
  ]
}}

Generate 3 test cases covering: trigger scenario, boundary/edge case, and negative (should NOT trigger) scenario."""

    return SYSTEM_PROMPT, user_prompt
