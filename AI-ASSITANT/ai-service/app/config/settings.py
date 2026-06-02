from functools import lru_cache
from typing import Optional

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
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

    # Groq settings
    GROQ_API_KEY: Optional[str] = None
    GROQ_BASE_URL: str = "https://api.groq.com/openai/v1"
    GROQ_MODEL: str = "llama-3.3-70b-versatile"
    GROQ_TIMEOUT_SECONDS: int = 60

    # Fallback to mock if the real provider fails
    AI_FALLBACK_TO_MOCK: bool = True

    # OpenAI settings
    OPENAI_API_KEY: Optional[str] = None
    OPENAI_MODEL: str = "gpt-4o"

    # Anthropic settings
    ANTHROPIC_API_KEY: Optional[str] = None
    ANTHROPIC_MODEL: str = "claude-sonnet-4-6"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
