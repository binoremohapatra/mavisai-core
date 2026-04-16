from enum import Enum
from typing import Any, Dict, Optional, List
from pydantic import BaseModel, ConfigDict, Field

# ✅ UPDATED EMOTIONS (To match Frontend & Java)
class Emotion(str, Enum):
    # Basic
    FRIENDLY = "FRIENDLY"
    HAPPY = "HAPPY"
    SAD = "SAD"
    ANGRY = "ANGRY"
    CONFUSED = "CONFUSED"
    THINKING = "THINKING"
    SURPRISED = "SURPRISED"
    
    # New Contextual Emotions
    EXCITED = "EXCITED"
    CALM = "CALM"
    SERIOUS = "SERIOUS"
    SUPPORTIVE = "SUPPORTIVE"
    CELEBRATORY = "CELEBRATORY"
    HELPFUL = "HELPFUL"

# ✅ UPDATED ACTIONS (Mapped to your FBX files)
class MascotAction(str, Enum):
    # Base States
    IDLE = "IDLE"
    THINKING = "THINKING"
    SPEAKING = "SPEAKING"
    BREATHING = "BREATHING"
    
    # New Animations (Direct mappings)
    WAVE = "WAVE"           # wave.fbx
    VICTORY = "VICTORY"     # victory.fbx
    SAD = "SAD"             # sadidle.fbx
    ANGRY = "ANGRY"         # angrypoint.fbx
    DEFEAT = "DEFEAT"       # defeat.fbx
    THANKFUL = "THANKFUL"   # thankful.fbx
    WALKING = "WALKING"     # walking.fbx
    
    # Aliases (For backward compatibility / logical grouping)
    WAVE_HELLO = "WAVE"
    CELEBRATE = "VICTORY"
    ERROR_STATE = "DEFEAT"
    SERIOUS_WARNING = "ANGRY"
    BREATHING_ANIMATION = "BREATHING"
    POINT_TO_BUTTON = "WAVE"

# === Code Editor Data Structure ===
class CodePayload(BaseModel):
    """
    Carries code content and control signals for the frontend editor.
    """
    code: Optional[str] = None
    language: str = "python"
    # Action tells the frontend what to do with the code
    action: str = Field(..., description="OVERWRITE | HIGHLIGHT | APPEND | NONE")
    highlightLines: List[int] = Field(default_factory=list, description="List of line numbers to highlight (1-based)")

class VoiceMeta(BaseModel):
    speakable: bool = True
    tone: str = Field(..., description="calm | friendly | serious | celebratory | supportive")
    speechRate: float = Field(..., ge=0.8, le=1.1)
    interruptible: bool = True
    model_config = ConfigDict(extra="ignore")

class AIGenerateRequest(BaseModel):
    intent: str
    facts: Dict[str, Any] = Field(default_factory=dict)
    userMessage: str
    desiredTone: Optional[str] = "friendly"
    language: Optional[str] = "en"
    model_config = ConfigDict(extra="ignore")

class AIGenerateResponse(BaseModel):
    replyText: str
    emotion: Emotion
    mascotAction: MascotAction
    voiceMeta: VoiceMeta
    
    # Optional Code Data Field
    codeData: Optional[CodePayload] = None
    
    model_config = ConfigDict(extra="ignore")
