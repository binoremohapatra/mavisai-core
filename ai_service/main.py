import logging
import traceback
import os
import httpx
import tempfile
import base64
import edge_tts
import json
import random # ✅ Added for Key Shuffling
from collections import deque
from contextlib import asynccontextmanager
from typing import Dict, Any, Deque, AsyncIterator
import time
import re

from fastapi import FastAPI, UploadFile, File, Response
from fastapi.responses import JSONResponse, FileResponse
from fastapi.middleware.cors import CORSMiddleware
from groq import Groq 
from pydantic import BaseModel 

# Import your custom modules
from config import load_settings, Settings
from models import AIGenerateRequest, AIGenerateResponse, Emotion, MascotAction
from response_builder import build_response, parse_llm_payload

# Initialize Logger
settings = load_settings()
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("main")

# 🌍 GLOBAL CACHE: To prevent hammer-hitting the backend
CONTEXT_CACHE = {} 
CACHE_TTL = 60 # 60 seconds

# 🤫 Silence noisy third-party libraries
logging.getLogger("httpx").setLevel(logging.WARNING)
logging.getLogger("httpcore").setLevel(logging.WARNING)
logging.getLogger("hpack").setLevel(logging.WARNING)
logging.getLogger("uvicorn.access").setLevel(logging.WARNING) # 🚀 Silence request logs

# ==========================================
# 🔑 HELPER: KEY ROTATION LOGIC
# ==========================================
def get_key_list(env_var_name: str, fallback_single_key: str = None) -> list:
    """
    Loads keys from comma-separated env var (e.g., 'key1,key2').
    If empty, falls back to the single config key.
    Returns a SHUFFLED list of keys.
    """
    keys_str = os.getenv(env_var_name, "")
    keys = [k.strip() for k in keys_str.split(",") if k.strip()]
    
    # If no list found in secrets, use the single key from settings/config
    if not keys and fallback_single_key:
        keys = [fallback_single_key]
    
    if keys:
        random.shuffle(keys) # 🎲 Shuffle to distribute load
        # 🔥 DEBUG: Log key status (masked)
        masked_keys = [f"{k[:4]}...{k[-4:]}" for k in keys]
        logger.info(f"🔑 Initialized '{env_var_name}': {len(keys)} keys found. {masked_keys}")
    else:
        logger.warning(f"⚠️ No keys found for '{env_var_name}'. Falling back to config.")
        
    return keys

# ==========================================
# 📡 CORE BACKEND SYNC
# ==========================================
async def fetch_user_context(http_client: httpx.AsyncClient, core_api_url: str, user_id: str) -> Dict[str, Any]:
    """Fetch live data from the core backend if needed (with 60s cache)."""
    now = time.time()
    if user_id in CONTEXT_CACHE:
        data, ts = CONTEXT_CACHE[user_id]
        if now - ts < CACHE_TTL:
            logger.debug(f"⚡ Context Cache Hit for user {user_id}")
            return data

    facts = {}
    try:
        # ✅ SAFE FETCH: Fetching attendance doesn't call back to AI
        resp = await http_client.get(f"{core_api_url}/api/v1/attendance/status?userId={user_id}")
        if resp.status_code == 200:
            data = resp.json()
            facts["attendance"] = data.get("payload", {})
            # Update Cache
            CONTEXT_CACHE[user_id] = (facts, now)
    except Exception as e:
        logger.debug(f"⚠️ Context Fetch Failed: {e}") 
    return facts


# (Logic already implemented in prompt.py, but used here in the snippets)
from prompt import build_prompts, get_emotional_system_prompt

# ==========================================
# 🧠 1. MEMORY SYSTEM
# ==========================================
class ShortTermMemory:
    def __init__(self, limit: int = 12): # 📉 Reduced from 30 to save tokens
        self.history: Deque[Dict[str, str]] = deque(maxlen=limit)

    def add_interaction(self, user_text: str, ai_text: str):
        self.history.append({"role": "user", "content": user_text})
        self.history.append({"role": "assistant", "content": ai_text})

    def get_context_string(self) -> str:
        if not self.history:
            return ""
        context_str = "PREVIOUS CONVERSATION:\n"
        for turn in self.history:
            context_str += f"{turn['role'].upper()}: {turn['content']}\n"
        return context_str + "\n"

# ==========================================
# 💻 2. CODING ASSISTANT
# ==========================================
class CodingAssistant:
    def __init__(self, settings: Settings):
        self.settings = settings

    def _build_coding_system_prompt(self, language: str, assistance_type: str) -> str:
        base_prompt = f"""You are a Code Editor Controller API. You are NOT a chatbot.
Your job is to generate a valid JSON response to control a code editor for {language}.
### STRICT OUTPUT FORMAT:
You must return a raw JSON object. 
- Do NOT wrap the JSON in markdown code blocks.
- The JSON must follow this exact schema:
{{
    "replyText": "Short spoken explanation of what you changed.",
    "emotion": "HELPFUL", 
    "mascotAction": "THINKING",
    "codeData": {{
        "code": "THE_FULL_CODE_STRING_HERE",
        "language": "{language}",
        "action": "ACTION_TYPE", 
        "highlightLines": [1, 2] 
    }}
}}
### VALID ACTIONS ("action" field):
- "OVERWRITE": Replaces the user's editor content. Use for new code or fixes.
- "HIGHLIGHT": Adds visual markers to lines. Use for code reviews/bug finding.
- "APPEND": Adds code to the end.
- "NONE": No editor changes."""
        
        type_specific = {
            'CODE_REVIEW': "Analyze for bugs. Return action='HIGHLIGHT' with the line numbers of errors.",
            'BUG_FIX': "Return action='OVERWRITE' with the fully corrected code.",
            'CODE_GENERATION': "Return action='OVERWRITE' with the complete implementation.",
            'EXPLANATION': "Return action='NONE' and explain the concept in 'replyText'.",
            'REFACTORING': "Return action='OVERWRITE' with the refactored code."
        }
        return base_prompt + "\n\nTASK: " + type_specific.get(assistance_type, "Provide coding help.")

    def _build_coding_user_prompt(self, code_context: str, language: str,
                                   assistance_type: str, voice_command: str) -> str:
        prompt_parts = [f"Assistance Type: {assistance_type}"]
        
        if voice_command:
            prompt_parts.append(f"User Request: {voice_command}")
        
        if code_context:
            prompt_parts.append(f"Current Editor Content:\n{code_context}")
            
        prompt_parts.append("IMPORTANT: Return ONLY raw JSON. No markdown.")
        return "\n\n".join(prompt_parts)

# ==========================================
# 🔀 3. AUTO-SWITCHING CLIENT (ROTATION ENABLED)
# ==========================================
class LLMClient:
    def __init__(self, settings: Settings):
        self.settings = settings

    async def generate(self, system_prompt: str, user_prompt: str) -> str:
        """
        Attempts Primary Provider (with Key Rotation). 
        If ALL keys fail, switches to Fallback Provider (with Key Rotation).
        """
        primary = self.settings.llm_provider
        fallback = self.settings.llm_fallback_provider
        
        try:
            # 1. Try Primary
            return await self._call_provider(primary, system_prompt, user_prompt)
        except Exception as e:
            logger.debug(f"🔄 Primary LLM '{primary}' exhausted. Switching to Fallback '{fallback}'...")
            
            try:
                # 2. Try Fallback
                return await self._call_provider(fallback, system_prompt, user_prompt)
            except Exception as ex:
                logger.debug(f"❌ Fallback LLM '{fallback}' also failed: {ex}")
                return "" 

    async def _call_provider(self, provider: str, system_prompt: str, user_prompt: str) -> str:
        if provider == "groq":
            return await self._call_groq_rotated(system_prompt, user_prompt)
        elif provider == "gemini":
            return await self._call_gemini_rotated(system_prompt, user_prompt)
        elif provider == "openai":
            return await self._call_openai(system_prompt, user_prompt)
        else:
            raise ValueError(f"Unknown provider: {provider}")

    # --- GROQ HANDLER (ROTATION) ---
    async def _call_groq_rotated(self, system_prompt: str, user_prompt: str) -> str:
        # Load keys list (from secrets GROQ_API_KEYS="key1,key2" OR config)
        keys = get_key_list("GROQ_API_KEYS", self.settings.groq_api_key)
        
        if not keys:
            raise RuntimeError("No Groq API Keys found.")

        url = "https://api.groq.com/openai/v1/chat/completions"
        payload = {
            "model": self.settings.groq_model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "temperature": 0.6,
            "max_tokens": 4096,
        }

        # 🔄 Try keys one by one
        last_error = None
        for key in keys:
            headers = {
                "Authorization": f"Bearer {key}",
                "Content-Type": "application/json",
            }
            try:
                async with httpx.AsyncClient(timeout=self.settings.request_timeout_seconds) as client:
                    response = await client.post(url, json=payload, headers=headers)
                    
                    if response.status_code == 429:
                        error_detail = response.text[:200]
                        logger.debug(f"Rotating Groq key (429 hit). Detail: {error_detail}")
                        last_error = f"Rate Limit (429): {error_detail}"
                        continue 
                    
                    response.raise_for_status()
                    return response.json()["choices"][0]["message"]["content"]
            
            except Exception as e:
                logger.warning(f"⚠️ Groq Key Failed: {str(e)[:100]}...")
                last_error = e
                continue 
        
        # If loop finishes, all keys failed
        logger.error(f"❌ ALL GROQ KEYS EXHAUSTED: {last_error}")
        raise Exception(f"All Groq Keys Exhausted. Last Error: {last_error}")

    # --- GEMINI HANDLER (ROTATION) ---
    async def _call_gemini_rotated(self, system_prompt: str, user_prompt: str) -> str:
        keys = get_key_list("GEMINI_API_KEYS", self.settings.gemini_api_key)
        
        if not keys:
            raise RuntimeError("No Gemini API Keys found.")

        full_prompt = f"{system_prompt}\n\nUSER REQUEST: {user_prompt}"
        
        # 🔄 Try keys one by one
        last_error = None
        for key in keys:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.settings.gemini_model}:generateContent?key={key}"
            
            payload = {
                "contents": [{"parts": [{"text": full_prompt}]}],
                "generationConfig": {
                    "temperature": 0.6,
                    "maxOutputTokens": 4096
                }
            }
            
            try:
                async with httpx.AsyncClient(timeout=self.settings.request_timeout_seconds) as client:
                    response = await client.post(url, json=payload)
                    
                    if response.status_code == 429 or response.status_code == 403:
                        logger.debug(f"Rotating Gemini key ({response.status_code} hit).")
                        continue 
                    
                    response.raise_for_status()
                    data = response.json()
                    
                    try:
                        return data["candidates"][0]["content"]["parts"][0]["text"]
                    except (KeyError, IndexError):
                        raise Exception("Invalid Gemini Response structure")
            
            except Exception as e:
                logger.warning(f"⚠️ Gemini Key Failed: {str(e)[:100]}...")
                last_error = e
                continue 

        logger.error(f"❌ ALL GEMINI KEYS EXHAUSTED: {last_error}")
        raise last_error or Exception("All Gemini Keys Exhausted")

    # --- OPENAI HANDLER (Single Key usually) ---
    async def _call_openai(self, system_prompt: str, user_prompt: str) -> str:
        if not self.settings.openai_api_key:
            raise RuntimeError("OPENAI_API_KEY is missing")
        url = "https://api.openai.com/v1/chat/completions"
        payload = {
            "model": self.settings.openai_model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "max_tokens": 500,
        }
        headers = {
            "Authorization": f"Bearer {self.settings.openai_api_key}",
            "Content-Type": "application/json",
        }
        async with httpx.AsyncClient(timeout=self.settings.request_timeout_seconds) as client:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            return response.json()["choices"][0]["message"]["content"]

# ==========================================
# 🧠 4. THE BRAIN (Orchestrator)
# ==========================================
class AIBrain:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.llm_client = LLMClient(settings)
        self.coding_assistant = CodingAssistant(settings)
        self.memory = ShortTermMemory(limit=30)
        
        # Groq client for Whisper Audio (Using the first available key for simplicity)
        # Note: Audio requests usually have different rate limits than chat
        groq_keys = get_key_list("GROQ_API_KEYS", settings.groq_api_key)
        self.audio_client = Groq(api_key=groq_keys[0]) if groq_keys else None

    async def think(self, request: AIGenerateRequest) -> str:
        user_text = request.userMessage
        intent = request.intent
        
        # ✅ Context Fetching: Call core backend if userId is present
        user_id = request.facts.get("userId")
        if user_id:
            async with httpx.AsyncClient(timeout=10.0) as client:
                live_facts = await fetch_user_context(client, self.settings.core_api_url, user_id)
                request.facts.update(live_facts)

        is_coding = self._is_coding_task(request)
        
        system_prompt = ""
        user_prompt = ""

        if is_coding:
            logger.debug("Brain Mode: LOGICAL (Coding)")
            lang = request.facts.get("language", "python")
            assistance_type = request.facts.get("assistanceType", "CODE_GENERATION")
            code_context = request.facts.get("codeContext", "")
            
            system_prompt = self.coding_assistant._build_coding_system_prompt(lang, assistance_type)
            user_prompt = self.coding_assistant._build_coding_user_prompt(
                code_context, lang, assistance_type, user_text
            )
        else:
            logger.debug("Brain Mode: EMOTIONAL (Chat)")
            system_prompt, strict_user_instruction = build_prompts(request)
            history_context = self.memory.get_context_string()
            
            # 🛡️ PROMPT GUARD: If history is too large, prune it to save the API key
            combined_prompt_len = len(system_prompt) + len(history_context) + len(strict_user_instruction)
            if combined_prompt_len > 3500: # Safe threshold for free 8B model limits
                logger.warning(f"🛡️ Prompt Guard: Prompt too large ({combined_prompt_len} chars). Pruning history.")
                self.memory.history.clear() # Emergency wipe
                history_context = ""
                
            user_prompt = f"{history_context}\n\n{strict_user_instruction}"

        # 🚀 CALL LLM (Auto-Switches & Rotates Keys)
        raw_output = await self.llm_client.generate(system_prompt, user_prompt)
        
        # Memory Update (Prune JSON bloat to save tokens)
        if not is_coding and raw_output:
            parsed_preview = parse_llm_payload(raw_output)
            
            # Use voice-friendly 'speech' if available, otherwise use 'replyText'
            ai_reply = parsed_preview.get("speech") or parsed_preview.get("replyText", "...")
            
            # 🔥 ROBUST ANTI-BLOAT: Remove ANY JSON blocks { ... } or Code blocks from history
            # This ensures massive data payloads never enter conversation history.
            if isinstance(ai_reply, str):
                # Remove Markdown code blocks first
                ai_reply = re.sub(r'```.*?```', '[Code Block]', ai_reply, flags=re.DOTALL)
                # Remove large JSON clusters { ... }
                if len(ai_reply) > 100:
                    ai_reply = re.sub(r'\{.*?\}', '[Structured Data Block]', ai_reply, flags=re.DOTALL)
                
            self.memory.add_interaction(user_text, ai_reply)

        return raw_output

    def _is_coding_task(self, request: AIGenerateRequest) -> bool:
        user_text = (request.userMessage or "").lower()
        return (
            request.intent == "CODING_HELP"
            or "code" in user_text
            or "def " in user_text
            or "function" in user_text
            or "debug" in user_text
        )

# ==========================================
# ⚡ 5. FASTAPI APP
# ==========================================
@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    settings = load_settings()
    app.state.settings = settings
    app.state.brain = AIBrain(settings)
    logger.info("🚀 Mavis Brain is Online")
    yield

app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==========================================
# 📡 6. ROUTES
# ==========================================
@app.get("/")
async def read_root():
    return FileResponse("index.html")

@app.post("/ai/generate", response_model=AIGenerateResponse)
async def generate(request: AIGenerateRequest) -> AIGenerateResponse:
    settings = app.state.settings
    brain = app.state.brain
    try:
        raw_output = await brain.think(request)
        payload = parse_llm_payload(raw_output)
        
        if not payload:
            payload = {
                "replyText": raw_output if raw_output else "I'm having trouble thinking.",
                "emotion": "HAPPY",
                "mascotAction": "THINKING",
            }
        
        return build_response(request, payload, settings)

    except Exception as e:
        error_msg = f"CRASH:\n{str(e)}\n\n{traceback.format_exc()}"
        logger.error(error_msg)
        return build_response(
            request,
            {
                "replyText": "I'm experiencing a neural disconnect. Please check my connections.",
                "emotion": "SAD",
                "mascotAction": "DEFEAT", 
            },
            settings,
        )

class TextToSpeechRequest(BaseModel):
    text: str
    voice: str = "en-US-EmmaNeural"
    rate: str = "+0%"
    pitch: str = "-2Hz"

@app.post("/ai/speak")
async def speak_text(request: TextToSpeechRequest):
    try:
        communicate = edge_tts.Communicate(
            request.text, 
            request.voice, 
            pitch=request.pitch,
            rate=request.rate
        )
        audio_data = b""
        async for chunk in communicate.stream():
            if chunk["type"] == "audio":
                audio_data += chunk["data"]
        return Response(content=audio_data, media_type="audio/mpeg")
    except Exception as e:
        logger.error(f"❌ TTS Error: {str(e)}")
        return JSONResponse({"error": str(e)}, status_code=500)

@app.post("/ai/voice-chat")
async def voice_chat(file: UploadFile = File(...)):
    settings = app.state.settings
    brain = app.state.brain

    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".webm") as temp_audio:
            temp_audio.write(await file.read())
            temp_audio_path = temp_audio.name
        
        # Audio client logic
        if not brain.audio_client:
             raise Exception("Groq Client not initialized (Check API Keys)")

        with open(temp_audio_path, "rb") as file_obj:
            transcription = brain.audio_client.audio.transcriptions.create(
                file=(temp_audio_path, file_obj.read()),
                model="whisper-large-v3",
                response_format="text",
                language="en"
            )
        
        user_text = str(transcription)
        logger.debug(f"🎤 Heard: {user_text}")
        
        os.remove(temp_audio_path)

        ai_request = AIGenerateRequest(
            userMessage=user_text,
            intent="CHAT",
            facts={}
        )
        
        raw_output = await brain.think(ai_request)
        payload = parse_llm_payload(raw_output)
        
        if not payload:
            payload = {"replyText": raw_output, "emotion": "HAPPY"}
            
        ai_reply_text = payload.get("replyText", "I am listening.")

        communicate = edge_tts.Communicate(
            ai_reply_text, 
            "en-US-EmmaNeural", 
            pitch="-2Hz",
            rate="+0%"
        )
        audio_data = b""
        async for chunk in communicate.stream():
            if chunk["type"] == "audio":
                audio_data += chunk["data"]
        
        audio_base64 = base64.b64encode(audio_data).decode('utf-8')

        return JSONResponse({
            "user_transcription": user_text,
            "ai_response": ai_reply_text,
            "ai_emotion": payload.get("emotion", "HAPPY"),
            "audio_base64": audio_base64
        })

    except Exception as e:
        logger.error(f"❌ Voice Error: {str(e)}")
        return JSONResponse({"error": str(e)}, status_code=500)

@app.get("/health")
async def health():
    return {"status": "ok", "cors": "enabled"}
