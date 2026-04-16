from dataclasses import dataclass
import os
from dotenv import load_dotenv

# Load .env file (if running locally)
load_dotenv()

@dataclass(frozen=True)
class Settings:
    app_name: str
    
    # Provider Settings
    llm_provider: str          # Primary (e.g., 'groq')
    llm_fallback_provider: str # Backup (e.g., 'gemini')
    
    # API Keys
    openai_api_key: str
    gemini_api_key: str
    groq_api_key: str
    
    # Models
    openai_model: str
    gemini_model: str
    groq_model: str
    ollama_model: str
    
    # URLs
    ollama_base_url: str
    core_api_url: str          # ✅ Added for Render Backend integration
    
    # Tuning
    request_timeout_seconds: float
    max_reply_sentences: int
    log_level: str

def _get_env(name: str, default: str) -> str:
    return os.getenv(name, default)

def load_settings() -> Settings:
    return Settings(
        app_name=_get_env("APP_NAME", "Mavis"),
        
        # Default Logic: Try Groq -> Fail -> Switch to Gemini
        llm_provider=_get_env("LLM_PROVIDER", "groq").lower(),
        llm_fallback_provider=_get_env("LLM_FALLBACK_PROVIDER", "gemini").lower(),
        
        openai_api_key=_get_env("OPENAI_API_KEY", ""),
        openai_model=_get_env("OPENAI_MODEL", "gpt-4o-mini"),
        
        # Gemini Settings
        gemini_api_key=_get_env("GEMINI_API_KEY", ""),
        gemini_model=_get_env("GEMINI_MODEL", "gemini-2.0-flash"), 
        
        # Groq Settings
        groq_api_key=_get_env("GROQ_API_KEY", ""),
        groq_model=_get_env("GROQ_MODEL", "llama-3.1-8b-instant"),
        
        # Ollama Settings
        ollama_base_url=_get_env("OLLAMA_BASE_URL", "http://localhost:11434"),
        ollama_model=_get_env("OLLAMA_MODEL", "llama3.1"),
        
        # ✅ Added Core API Backend Link
        core_api_url=_get_env("CORE_API_URL", "https://mavisai-core.onrender.com"),
        
        request_timeout_seconds=float(_get_env("REQUEST_TIMEOUT_SECONDS", "60")), 
        max_reply_sentences=int(_get_env("MAX_REPLY_SENTENCES", "3")),
        log_level=_get_env("LOG_LEVEL", "INFO"),
    )
