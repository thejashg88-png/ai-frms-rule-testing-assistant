from functools import lru_cache
from typing import Optional

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    Application configuration loaded from the .env file.

    AI provider selection:
        AI_PROVIDER=mock    → no API key needed, uses built-in deterministic responses
        AI_PROVIDER=groq    → calls Groq via OpenAI-compatible API; requires GROQ_API_KEY
        AI_PROVIDER=openai  → placeholder, not yet implemented
        AI_PROVIDER=anthropic → placeholder, not yet implemented

    Fallback behavior:
        AI_FALLBACK_TO_MOCK=true   → if Groq/real provider fails, silently return mock response
        AI_FALLBACK_TO_MOCK=false  → raise LLMProviderUnavailableError → Spring Boot receives 503

    SECURITY: API key fields (GROQ_API_KEY, OPENAI_API_KEY, ANTHROPIC_API_KEY) must never
    be logged, printed, included in responses, or exposed via /api/ai/provider-health.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    APP_NAME: str = "AI-FRMS AI Service"
    APP_PORT: int = 8000
    LOG_LEVEL: str = "INFO"

    # AI Provider: mock | groq | openai | anthropic
    AI_PROVIDER: str = "mock"

    # Groq settings — GROQ_API_KEY must never be logged or returned in responses
    GROQ_API_KEY: Optional[str] = None
    # OpenAI-compatible endpoint provided by Groq (allows using the openai SDK directly)
    GROQ_BASE_URL: str = "https://api.groq.com/openai/v1"
    GROQ_MODEL: str = "llama-3.3-70b-versatile"
    GROQ_TIMEOUT_SECONDS: int = 60

    # When true, a failed real LLM call falls back to mock instead of returning 503
    AI_FALLBACK_TO_MOCK: bool = True

    # OpenAI settings (placeholder — not yet implemented)
    OPENAI_API_KEY: Optional[str] = None
    OPENAI_MODEL: str = "gpt-4o"

    # Anthropic settings (placeholder — not yet implemented)
    ANTHROPIC_API_KEY: Optional[str] = None
    ANTHROPIC_MODEL: str = "claude-sonnet-4-6"


@lru_cache()
def get_settings() -> Settings:
    # Cached singleton — settings are read once at startup and reused across all requests
    return Settings()
