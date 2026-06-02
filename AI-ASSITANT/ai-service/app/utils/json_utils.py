import json
import re
from typing import Any

from loguru import logger


def parse_llm_json(raw: str) -> Any:
    """Parse JSON from LLM response, stripping markdown code fences if present."""
    cleaned = _strip_markdown_fences(raw.strip())

    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        pass

    repaired = _repair_json(cleaned)
    try:
        return json.loads(repaired)
    except json.JSONDecodeError as exc:
        logger.error(f"JSON parse failed after repair attempt: {exc}")
        logger.debug(f"Raw LLM output (first 500 chars): {raw[:500]}")
        raise ValueError(f"LLM response is not valid JSON: {exc}") from exc


def _strip_markdown_fences(text: str) -> str:
    text = re.sub(r"^```(?:json)?\s*\n?", "", text, flags=re.MULTILINE)
    text = re.sub(r"\n?```\s*$", "", text, flags=re.MULTILINE)
    return text.strip()


def _repair_json(text: str) -> str:
    # Remove trailing commas before closing braces/brackets
    text = re.sub(r",\s*([}\]])", r"\1", text)
    return text
