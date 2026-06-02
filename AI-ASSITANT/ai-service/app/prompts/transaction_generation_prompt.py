from app.models.request_models import GenerateTransactionRequest
from app.utils.prompt_utils import json_output_instruction


SYSTEM_PROMPT = """You are a payment systems engineer generating realistic test transaction payloads for fraud rule testing.
Generate ISO 8583-style transaction data appropriate for the given fraud rule type and expected outcome.
Amount must be a zero-padded 11-digit string. RRN is 12 digits. STAN is 6 digits.
""" + json_output_instruction()


def build_transaction_prompt(request: GenerateTransactionRequest) -> tuple[str, str]:
    amount_hint = f"approximately {request.amount}" if request.amount else "appropriate for the scenario"

    user_prompt = f"""Generate a realistic test transaction payload for:

Rule Type: {request.ruleType}
Expected Result: {request.expectedResult}
Amount: {amount_hint}
Currency: {request.currency}

The transaction amount should make sense for a scenario where result is {request.expectedResult}.

Return JSON in exactly this format:
{{
  "transaction": {{
    "rrn": "12-digit retrieval reference number",
    "stan": "6-digit system trace audit number",
    "tid": "terminal ID",
    "mid": "merchant ID",
    "amount": "zero-padded 11-digit amount string",
    "currency": "{request.currency}",
    "transactionType": "PURCHASE | WITHDRAWAL | FUND_TRANSFER | REFUND"
  }}
}}"""

    return SYSTEM_PROMPT, user_prompt
