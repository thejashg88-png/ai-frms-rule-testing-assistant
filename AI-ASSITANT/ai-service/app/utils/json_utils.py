"""
Utilities for parsing JSON from LLM responses.

LLMs frequently wrap JSON in markdown code fences (```json ... ```) even when
instructed not to, and sometimes produce trailing commas that are invalid JSON.
This module strips those artefacts before parsing, and raises ValueError (not
silently returns None) when the output is genuinely unparsable — the caller
(GroqLLMProvider._call_with_fallback) will then trigger the fallback path.
"""
import json
import re
from typing import Any

from loguru import logger


def parse_llm_json(raw: str) -> Any:
    """
    Parse JSON from an LLM response.

    Attempt order:
    1. Strip markdown fences, parse directly.
    2. Apply light JSON repair (trailing commas), parse again.
    3. Raise ValueError — caller decides whether to fall back to mock.
    """
    cleaned = _strip_markdown_fences(raw.strip())

    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        pass

    repaired = _repair_json(cleaned)
    try:
        return json.loads(repaired)
    except json.JSONDecodeError as exc:
        # Log at error level so ops can see when the LLM is consistently misbehaving,
        # but never log the raw content at INFO level (it may contain PII from the prompt).
        logger.error(f"JSON parse failed after repair attempt: {exc}")
        logger.debug(f"Raw LLM output (first 500 chars): {raw[:500]}")
        raise ValueError(f"LLM response is not valid JSON: {exc}") from exc


def _strip_markdown_fences(text: str) -> str:
    # Remove leading ```json or ``` and trailing ```
    text = re.sub(r"^```(?:json)?\s*\n?", "", text, flags=re.MULTILINE)
    text = re.sub(r"\n?```\s*$", "", text, flags=re.MULTILINE)
    return text.strip()


def _repair_json(text: str) -> str:
    # Remove trailing commas before closing braces/brackets (common LLM output error)
    text = re.sub(r",\s*([}\]])", r"\1", text)
    return text
