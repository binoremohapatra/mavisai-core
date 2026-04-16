import json
import logging
import re
from typing import Any, Dict, Tuple

from config import Settings
from models import (
    AIGenerateRequest,
    AIGenerateResponse,
    Emotion,
    MascotAction,
    VoiceMeta,
    CodePayload,
)

logger = logging.getLogger(__name__)

# =========================
# VOICE + EMOTION MAPS
# =========================
VOICE_META_BY_TONE = {
    "calm": (0.95, True),
    "friendly": (1.0, True),
    "serious": (0.9, True),
    "celebratory": (1.05, True),
    "supportive": (0.95, True), 
}

EMOTION_TONE_MAP = {
    Emotion.CALM: "calm",
    Emotion.SERIOUS: "serious",
    Emotion.CELEBRATORY: "celebratory",
    Emotion.HAPPY: "friendly",
    Emotion.SUPPORTIVE: "supportive",
    Emotion.SAD: "calm",
    Emotion.CONFUSED: "friendly",
    Emotion.ANGRY: "serious",
}

# =========================
# LLM PAYLOAD PARSER
# =========================
def parse_llm_payload(raw: str) -> Dict[str, Any]:
    if not raw:
        return {}
    
    raw = raw.strip()
    
    # Clean up markdown code blocks if AI added them
    if raw.startswith("```json"):
        raw = raw.replace("```json", "").replace("```", "").strip()

    # Try direct JSON parsing
    try:
        parsed = json.loads(raw)
        
        # Logic Fix: If AI returned the structured plan directly without 'replyText'
        if isinstance(parsed, dict) and "replyText" not in parsed:
            # Check for common structured plan keys
            if any(key in parsed for key in ["weekly", "immediate", "todayPlan"]):
                # Create a speaker-friendly snippet for the mascot
                voice_snippet = parsed.get("immediate") or parsed.get("title") or "I've updated your academic strategy."
                return {
                    "replyText": raw, # Keep raw for Java parsing
                    "speech": voice_snippet, # Helper for TTS/History
                    "emotion": "HELPFUL",
                    "mascotAction": "THINKING"
                }
        return parsed
    except json.JSONDecodeError:
        pass
    
    # Try extracting JSON block via regex if direct parse failed
    match = re.search(r"\{.*\}", raw, re.DOTALL)
    if match:
        extracted = match.group(0)
        try:
            parsed = json.loads(extracted)
            if isinstance(parsed, dict) and "replyText" not in parsed:
                if any(key in parsed for key in ["weekly", "immediate", "todayPlan"]):
                    return {
                        "replyText": extracted,
                        "emotion": "HELPFUL",
                        "mascotAction": "THINKING"
                    }
            return parsed
        except json.JSONDecodeError:
            pass
            
    # Treat as plain text fallback
    return {
        "replyText": raw,
        "emotion": "HAPPY",
        "mascotAction": "IDLE",
    }

# =========================
# TEXT SANITIZER (FIXED)
# =========================
def sanitize_reply(text: Any, max_sentences: int) -> str:
    # ✅ FIX: Ensure text is actually a string before processing to prevent 'dict' errors
    if not text or not isinstance(text, str):
        return str(text) if text else ""
    
    # Basic cleanup
    text = text.replace("\r", " ").replace("\n", " ")
    text = re.sub(r"\s+", " ", text).strip()
    
    # Split sentences roughly
    sentences = re.split(r"(?<=[.!?])\s+", text)
    
    # Rejoin only the allowed number of sentences
    return " ".join(sentences[: max(1, max_sentences)])

# =========================
# RESPONSE BUILDER (UPDATED)
# =========================
def build_response(
    request: AIGenerateRequest,
    payload: Dict[str, Any],
    settings: Settings,
) -> AIGenerateResponse:
    intent = (request.intent or "").upper()
    
    # 1. Get Fallbacks
    fallback_reply, fallback_emotion, fallback_action = build_fallback_response(request)
    
    # 2. Extract Data (with fallbacks)
    reply = payload.get("replyText") or fallback_reply
    emotion = _coerce_emotion(payload.get("emotion"), fallback_emotion)
    action = _coerce_action(payload.get("mascotAction"), fallback_action)
    
    # 3. Capture Code Data
    code_payload = None
    raw_code_data = payload.get("codeData")
    if raw_code_data:
        try:
            code_payload = CodePayload(**raw_code_data)
        except Exception as e:
            logger.error(f"Failed to parse codeData: {e}")

    # 4. ✅ LOGIC UPDATE: Conditional Sanitization
    # Skip sentence limiting for Academic/Study plans or Coding help
    is_academic = intent in ["STUDY_PLANNING", "ACADEMIC_QUERY", "EXAM_QUERY"]
    
    if not is_academic and intent != "CODING_HELP":
        reply = sanitize_reply(reply, settings.max_reply_sentences)
    elif isinstance(reply, dict):
        # If the reply is accidentally the whole JSON dict, stringify it safely
        reply = json.dumps(reply)
        
    # 5. Resolve Voice Tone
    voice_meta = resolve_voice_meta(request.desiredTone, emotion)
    
    return AIGenerateResponse(
        replyText=reply,
        emotion=emotion,
        mascotAction=action,
        voiceMeta=voice_meta,
        codeData=code_payload
    )

# =========================
# FALLBACKS
# =========================
def build_fallback_response(
    request: AIGenerateRequest,
) -> Tuple[str, Emotion, MascotAction]:
    intent = (request.intent or "").upper()
    
    if intent == "GREETING":
        return "Hi. I am here with you.", Emotion.HAPPY, MascotAction.WAVE
        
    if intent == "CODING_HELP":
        return "Here is the complete working code.", Emotion.HELPFUL, MascotAction.THINKING
        
    if intent == "ATTENDANCE_QUERY":
        return (
            "Your attendance looks okay for now.",
            Emotion.SUPPORTIVE,
            MascotAction.VICTORY,
        )
        
    if intent == "WELLNESS_CHECKIN":
        return (
            "I hear you. A slow breath might help.",
            Emotion.CALM,
            MascotAction.BREATHING,
        )
        
    return (
        "I am here to help. If you share a bit more, I can assist.",
        Emotion.CONFUSED,
        MascotAction.THINKING,
    )

# =========================
# HELPERS
# =========================
def resolve_voice_meta(desired_tone: str | None, emotion: Emotion) -> VoiceMeta:
    tone = (desired_tone or "").lower()
    
    if tone not in VOICE_META_BY_TONE:
        tone = EMOTION_TONE_MAP.get(emotion, "friendly")
        
    speech_rate, interruptible = VOICE_META_BY_TONE.get(tone, (1.0, True))
    
    return VoiceMeta(
        speakable=True,
        tone=tone,
        speechRate=speech_rate,
        interruptible=interruptible,
    )

def _coerce_emotion(value: Any, fallback: Emotion) -> Emotion:
    try:
        if not value: return fallback
        return Emotion(str(value).upper())
    except Exception:
        return fallback

def _coerce_action(value: Any, fallback: MascotAction) -> MascotAction:
    try:
        if not value: return fallback
        return MascotAction(str(value).upper())
    except Exception:
        return fallback
