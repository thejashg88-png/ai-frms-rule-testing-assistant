import uvicorn
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config.logging_config import setup_logging
from app.config.settings import get_settings
from app.api.routes import router as main_router
from app.api.test_case_routes import router as test_case_router
from app.api.failure_analysis_routes import router as failure_router
from app.api.rule_explanation_routes import router as rule_router
from app.api.rule_generation_routes import router as rule_generation_router
from app.services.llm_service import LLMProviderUnavailableError

setup_logging()
settings = get_settings()

app = FastAPI(
    title=settings.APP_NAME,
    description=(
        "AI service for the AI-FRMS Rule Testing Assistant. "
        "Generates test cases, explains fraud rules, analyzes test failures, "
        "and produces dummy transaction payloads for FRMS rule validation."
    ),
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
)

@app.exception_handler(LLMProviderUnavailableError)
async def llm_unavailable_handler(request: Request, exc: LLMProviderUnavailableError):
    return JSONResponse(
        status_code=503,
        content={"success": False, "message": str(exc), "data": None},
    )


# CORS — allows Spring Boot backend (port 8080) and any local dev tool to call this service
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Restrict to specific origins in production (e.g., http://localhost:8080)
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routers
app.include_router(main_router)                                  # GET /health, POST /api/ai/generate-transaction
app.include_router(test_case_router, prefix="/api/ai")           # POST /api/ai/generate-test-cases
app.include_router(failure_router, prefix="/api/ai")             # POST /api/ai/analyze-failure
app.include_router(rule_router, prefix="/api/ai")                # POST /api/ai/explain-rule
app.include_router(rule_generation_router, prefix="/api/ai")     # POST /api/ai/generate-rule


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=settings.APP_PORT,
        reload=True,
        log_level=settings.LOG_LEVEL.lower(),
    )
