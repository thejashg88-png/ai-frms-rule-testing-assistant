from app.models.request_models import ExplainRuleRequest
from app.utils.prompt_utils import build_rule_context, json_output_instruction


SYSTEM_PROMPT = """You are a senior fraud risk analyst and compliance expert specializing in payment systems.
Explain fraud detection rules in clear, practical language covering both business and technical perspectives.
Your audience includes developers, QA testers, compliance officers, and business analysts.
""" + json_output_instruction()


def build_explanation_prompt(request: ExplainRuleRequest) -> tuple[str, str]:
    rule_context = build_rule_context(request.model_dump(exclude_none=True))

    user_prompt = f"""Explain this fraud detection rule in detail:

{rule_context}

Return JSON in exactly this format:
{{
  "summary": "One-paragraph summary of what the rule does",
  "businessMeaning": "Why this rule exists from a business/compliance perspective",
  "technicalMeaning": "How the rule works technically — what fields are evaluated, how",
  "exampleScenario": "A realistic step-by-step example of the rule triggering",
  "riskNotes": [
    "Risk note 1",
    "Risk note 2",
    "Risk note 3"
  ]
}}"""

    return SYSTEM_PROMPT, user_prompt
