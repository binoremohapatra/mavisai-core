"""
Student Life OS – AI Service package

This package acts as the AI Brain for the Spring Boot backend.
It generates safe, supportive, UX-aware responses for students.

Contract:
- Input  : AIGenerateRequest  (from Java AiBrainRequest)
- Output : AIGenerateResponse (mapped to Java AiBrainResponse)
"""

from ai_service.config import Settings, load_settings
from ai_service.llm_client import LLMClient
from ai_service.models import (
    AIGenerateRequest,
    AIGenerateResponse,
    Emotion,
    MascotAction,
    VoiceMeta,
)

__all__ = [
    "Settings",
    "load_settings",
    "LLMClient",
    "AIGenerateRequest",
    "AIGenerateResponse",
    "Emotion",
    "MascotAction",
    "VoiceMeta",
]
