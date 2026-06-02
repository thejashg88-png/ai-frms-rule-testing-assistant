"""
Main router — health, transaction generation, and AI chat endpoints.

Endpoint overview:
    GET  /health                    — liveness probe (no auth, no LLM call)
    GET  /api/ai/provider-health    — shows which provider is configured (never exposes API key)
    POST /api/ai/generate-transaction — generates dummy ISO 8583 transaction payload
    POST /api/ai/chat               — FRMS AI assistant; proxied from Spring Boot POST /api/ai/chat

Note: LLMProviderUnavailableError is NOT caught here. It propagates to the global handler
in main.py which returns a 503 JSON response to Spring Boot.
"""
from fastapi import APIRouter, HTTPException
from loguru import logger

from app.config.settings import get_settings
from app.models.request_models import AiChatRequest, GenerateTransactionRequest
from app.models.response_models import AiChatData, ApiResponse, HealthResponse
from app.services.llm_service import get_llm_provider

router = APIRouter()


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Health Check",
    tags=["Health"],
)
async def health_check():
    return HealthResponse(status="UP", service="AI-FRMS AI Service")


@router.get(
    "/api/ai/provider-health",
    summary="AI Provider Health",
    tags=["Health"],
)
async def provider_health():
    # Returns which provider is active and whether its API key is set.
    # IMPORTANT: only the boolean presence is returned — the key itself is never exposed.
    settings = get_settings()
    provider = settings.AI_PROVIDER.lower().strip()
    if provider == "groq":
        return {
            "provider": "groq",
            "model": settings.GROQ_MODEL,
            "configured": bool(settings.GROQ_API_KEY),
        }
    if provider == "openai":
        return {
            "provider": "openai",
            "model": settings.OPENAI_MODEL,
            "configured": bool(settings.OPENAI_API_KEY),
        }
    if provider == "anthropic":
        return {
            "provider": "anthropic",
            "model": settings.ANTHROPIC_MODEL,
            "configured": bool(settings.ANTHROPIC_API_KEY),
        }
    return {
        "provider": "mock",
        "model": "built-in",
        "configured": True,
    }


@router.post(
    "/api/ai/generate-transaction",
    response_model=ApiResponse,
    summary="Generate Dummy Transaction Payload",
    tags=["Transaction Generation"],
)
async def generate_transaction(request: GenerateTransactionRequest):
    try:
        llm = get_llm_provider()
        data = await llm.generate_transaction(request)
        return ApiResponse(
            success=True,
            message="Transaction generated successfully",
            data=data,
        )
    except NotImplementedError as exc:
        logger.warning(f"Provider not implemented: {exc}")
        raise HTTPException(status_code=501, detail=str(exc))
    except Exception as exc:
        logger.exception(f"Unexpected error in generate_transaction: {exc}")
        raise HTTPException(status_code=500, detail="Internal server error while generating transaction.")


@router.post(
    "/api/ai/chat",
    response_model=ApiResponse,
    summary="AI Chat",
    tags=["AI Chat"],
    description=(
        "Ask the AI assistant questions about fraud rules, test cases, "
        "test failures, rule configuration, and FRMS behavior."
    ),
)
async def ai_chat(request: AiChatRequest):
    llm = get_llm_provider()
    logger.info(f"[AI CHAT] provider={type(llm).__name__}")
    logger.info(f"[AI CHAT] message={request.message[:120]}")
    logger.info(f"[AI CHAT] context={request.context}")
    reply = await llm.chat(request.message, request.context)
    logger.info("[AI CHAT] response generated")
    return ApiResponse(
        success=True,
        message="AI chat response returned",
        data=AiChatData(reply=reply, context=request.context),
    )
