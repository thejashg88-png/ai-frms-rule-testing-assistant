"""
Prompt builder for POST /api/ai/analyze-failure (Groq provider path).

The actual ruleType from the request is embedded in the user prompt so the LLM
produces rule-specific guidance. Do NOT substitute a default ruleType (e.g. SINGLE_LARGE_TX)
when the real one is unknown — the analysis will be misleading for count-based rules.

When Spring Boot sends enriched context fields (matchedCount, requiredCount,
historicalTransactionCount, frequencyWindow, ruleConfig, executionTrace), those are
embedded directly in the user prompt so the AI gives specific, data-driven analysis
instead of a generic response.

Expected JSON output keys:
    summary (string), rootCause (string), possibleReasons (list),
    debuggingSteps (list), recommendedFix (string), riskImpact (string),
    confidence (int 0-100).
"""
import json

from app.models.request_models import AnalyzeFailureRequest
from app.utils.prompt_utils import json_output_instruction

SYSTEM_PROMPT = (
    """You are a senior QA engineer and fraud systems debugger specializing in FRMS rule engines.
Analyze test case failures and provide actionable, specific debugging guidance.

STRICT ANALYSIS RULES:
- Analyze ONLY the ruleType provided. Do NOT assume SINGLE_LARGE_TX unless ruleType IS SINGLE_LARGE_TX.
- Do NOT invent missing database records, transaction IDs, or historical data.
- If historicalTransactionCount is 0, explicitly state that no matching transactions were found
  for this card/customer/device in the time window — do not speculate about other causes first.
- If matchedCount < requiredCount, your primary explanation must be that the threshold was not reached.
- Use ONLY the context provided. Do not fabricate specific values that were not given.

RULE-TYPE-SPECIFIC GUIDANCE:

HIGH_FREQ_TXN / TXN_VELOCITY / SEQUENTIAL_TXN (count-based rules):
  - These rules count transactions, NOT amounts. Focus on txnCount threshold and transaction count.
  - matchedCount = total transactions evaluated (historical + current combined).
  - requiredCount = configured txnCount threshold the rule needs to fire.
  - historicalTransactionCount = prior transactions already in the database for this card/customer/device.
  - Root cause of ACCEPT failure: matching history is insufficient (historicalTransactionCount too low).
  - If historicalTransactionCount = 0: the rule engine found no prior transactions in the time window
    for this card/customer/device identity — the test database needs to be seeded with history.
  - The matching key is typically customerId, track2_data, or serialNumber — all test transactions
    must share the same value for the key the rule uses.

UNUSUAL_AMT:
  - Focus on: customer spending baseline average, percentageThreshold, current amount vs threshold.
  - If historicalTransactionCount = 0: the rule cannot compute a baseline → it cannot trigger.
  - Baseline must be established before UNUSUAL_AMT tests can work.

STRUCTURING:
  - Focus on: multiple transactions each BELOW the configured txnAmount within the frequencyWindow.
  - Each transaction must be strictly below the threshold to count — amounts equal to the threshold do NOT count.
  - All transactions must share the same customer/account identifier.

SINGLE_LARGE_TX:
  - Focus purely on: transaction amount vs maxAmount. No historical data is needed.
  - If amount < maxAmount, the rule will not trigger regardless of other factors.

DAILY_TXN_VALUE / ANNUAL_TXN_VALUE (cumulative amount rules):
  - Focus on cumulative transaction value within the rolling window vs configured txnAmount threshold.
  - historicalTransactionCount represents prior transactions contributing to the cumulative total.

DAILY_TXN_VOLUME / MONTHLY_TXN_VOLUME / ANNUAL_TXN_VOLUME (cumulative count rules):
  - Focus on cumulative transaction count within the rolling window vs configured txnCount threshold.

Be specific and practical. When count/threshold data is provided, lead with those numbers.
"""
    + json_output_instruction()
)


def build_failure_analysis_prompt(request: AnalyzeFailureRequest) -> tuple[str, str]:
    """
    Builds (system_prompt, user_prompt) for the Groq failure analysis call.

    When enriched context fields are present on the request (matchedCount,
    requiredCount, historicalTransactionCount, ruleConfig, etc.), they are all
    included in the user prompt. The AI is instructed to prioritize those concrete
    numbers over generic reasoning.

    Fields included if not None: ruleConfig, testCaseInput, inputData,
    failureReason, matchedCount, requiredCount, historicalTransactionCount,
    currentCount, frequencyWindow, ruleExplanation, executionTrace, executionLogs.
    """
    lines = []

    # Core failure context
    lines.append(f"Test Case Name: {request.testCaseName}")
    lines.append(f"Rule Type: {request.ruleType}")
    lines.append(f"Expected Result: {request.expectedResult}")
    lines.append(f"Actual Result: {request.actualResult}")

    # Rule configuration
    if request.ruleConfig:
        lines.append(f"\nRule Configuration:\n{json.dumps(request.ruleConfig, indent=2)}")

    # Test input (prefer testCaseInput over inputData if both present)
    if request.testCaseInput:
        lines.append(f"\nTest Case Input:\n{json.dumps(request.testCaseInput, indent=2)}")
    elif request.inputData:
        lines.append(f"\nInput Data:\n{json.dumps(request.inputData, indent=2)}")

    # Failure reason from the rule engine
    if request.failureReason:
        lines.append(f"\nFailure Reason (from rule engine): {request.failureReason}")

    # Count/threshold evidence — critical for diagnosing count-based rule failures
    if any(v is not None for v in [
        request.matchedCount, request.requiredCount,
        request.historicalTransactionCount, request.currentCount,
    ]):
        lines.append("\nCount Evidence:")
        if request.matchedCount is not None:
            lines.append(f"  matchedCount (transactions evaluated): {request.matchedCount}")
        if request.requiredCount is not None:
            lines.append(f"  requiredCount (threshold to trigger rule): {request.requiredCount}")
        if request.historicalTransactionCount is not None:
            lines.append(f"  historicalTransactionCount (prior transactions in DB): {request.historicalTransactionCount}")
        if request.currentCount is not None:
            lines.append(f"  currentCount (current transaction count): {request.currentCount}")
    elif request.historicalTransactionCount is not None:
        lines.append(f"\nHistorical Transaction Count: {request.historicalTransactionCount}")

    if request.frequencyWindow:
        lines.append(f"\nFrequency Window: {request.frequencyWindow}")

    # Rule explanation context
    if request.ruleExplanation:
        lines.append(f"\nRule Explanation:\n{json.dumps(request.ruleExplanation, indent=2)}")

    # Execution trace (prefer over plain logs if both present)
    if request.executionTrace:
        lines.append(f"\nExecution Trace:\n{json.dumps(request.executionTrace, indent=2)}")
    elif request.executionLogs:
        lines.append(f"\nExecution Logs: {request.executionLogs}")

    # Signal to the AI whether it has enough data to be precise
    has_enriched = any([
        request.ruleConfig,
        request.matchedCount is not None,
        request.requiredCount is not None,
        request.historicalTransactionCount is not None,
        request.executionTrace,
        request.failureReason,
    ])
    context_note = (
        " (enriched context provided — base your analysis on the specific numbers given)"
        if has_enriched
        else " (limited context — reason from ruleType and expected vs actual result)"
    )

    user_prompt = f"""Analyze this test case failure{context_note}:

{chr(10).join(lines)}

Return JSON in exactly this format:
{{
  "summary": "<one sentence describing what happened and why, using specific numbers if available>",
  "rootCause": "<the single most likely root cause>",
  "possibleReasons": [
    "Reason 1 — specific to {request.ruleType}",
    "Reason 2",
    "Reason 3"
  ],
  "debuggingSteps": [
    "Step 1: ...",
    "Step 2: ...",
    "Step 3: ..."
  ],
  "recommendedFix": "Specific actionable fix recommendation",
  "riskImpact": "What happens in production if this issue is not fixed",
  "confidence": <integer 0-100 — how confident you are given the context provided>
}}"""

    return SYSTEM_PROMPT, user_prompt
