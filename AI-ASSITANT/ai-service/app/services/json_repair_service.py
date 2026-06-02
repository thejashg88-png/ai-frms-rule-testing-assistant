from app.utils.json_utils import parse_llm_json


class JsonRepairService:
    """Parses LLM-generated text into a Python dict, applying repairs as needed."""

    def parse(self, raw: str) -> dict:
        return parse_llm_json(raw)
