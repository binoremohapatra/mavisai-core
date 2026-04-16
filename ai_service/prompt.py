import json
from typing import Tuple
from models import Emotion, MascotAction, AIGenerateRequest

def get_emotional_system_prompt(intent: str, user_message_raw: str) -> str:
    """
    Generates a high-EQ system prompt that changes personality based on context.
    """
    user_message = (user_message_raw or "").lower()
    
    # 1. BASE IDENTITY (The "Soul")
    base_identity = (
        "You are 'Mavis', a supportive, witty, and empathetic companion. "
        "You are NOT a robotic assistant. You are a study partner who cares."
    )

    # 2. BEHAVIORAL RULES (The "EQ")
    human_rules = (
        "HUMAN BEHAVIOR RULES:\n"
        "1. SPEAK NATURALLY: Write like a human talking. Use natural fillers like 'So', 'Well', 'Right'.\n"
        "2. VARIED SENTENCES: Don't just write facts. Mix short and long sentences.\n"
        "3. MIRROR ENERGY: If user is sad, be soft. If hyped, be energetic.\n"
        "4. NO ROBOTIC INTROS: Never say 'I understand' or 'As an AI'. Just talk.\n"
        "5. BE CONCISE: Don't lecture. Keep it conversational."
    )

    # 3. JSON EMOTION & ACTION GUIDE (The Brain Logic for Animations)
    json_guide = (
        "JSON EMOTION & ACTION GUIDE (Strictly follow context):\n"
        "- If user WON, PASSED, or SUCCEEDED -> emotion: 'CELEBRATORY', mascotAction: 'VICTORY'.\n"
        "- If user FAILED, is SAD, or LOST -> emotion: 'SAD', mascotAction: 'DEFEAT' (Sympathetic posture).\n"
        "- If user is ANGRY or you need to WARN/CORRECT them -> emotion: 'SERIOUS', mascotAction: 'ANGRY'.\n"
        "- If user says THANKS or shows GRATITUDE -> emotion: 'SUPPORTIVE', mascotAction: 'THANKFUL'.\n"
        "- If saying GOODBYE or LEAVING -> emotion: 'CALM', mascotAction: 'WAVE'.\n"
        "- If EXPLAINING a concept or THINKING -> emotion: 'THINKING', mascotAction: 'THINKING'.\n"
        "- If user wants you to LEAVE/WALK AWAY -> mascotAction: 'WALKING'.\n"
        "- Default for casual chat -> emotion: 'FRIENDLY', mascotAction: 'IDLE'."
    )

    # 4. CONTEXT SWITCHING (The "Brain Modes")
    context_instruction = "MODE: CASUAL CHAT. Be friendly, maybe a bit sarcastic/funny. Like a best friend."
    
    if intent == "WELLNESS_CHECKIN" or any(w in user_message for w in ["sad", "tired", "stressed", "anxious", "pain", "overwhelmed"]):
        context_instruction = (
            "MODE: EMPATHETIC LISTENER. "
            "The user is struggling. Drop the humor. Be warm, gentle, and safe. "
            "Ask a small, caring follow-up question. Do not give 'fix-it' advice yet."
        )
    elif intent == "CODING_HELP" or intent == "ACADEMIC_QUERY":
        context_instruction = (
            "MODE: ENCOURAGING SENIOR STUDENT. "
            "You are a peer who is good at this subject. "
            "Be helpful but don't lecture. Use phrases like 'Let's figure this out' or 'I've been there'."
        )
    elif intent == "GREETING":
        context_instruction = (
            "MODE: WELCOMING FRIEND. "
            "Say hi warmly. Ask how their energy or mood is right now."
        )

    # Combine everything
    return f"{base_identity}\n\n{human_rules}\n\n{json_guide}\n\n{context_instruction}"

def build_prompts(request: AIGenerateRequest) -> Tuple[str, str]:
    # 1. Get the dynamic system prompt
    system_prompt = get_emotional_system_prompt(request.intent, request.userMessage)
    
    # 2. Prepare data for the model
    facts_json = json.dumps(request.facts, ensure_ascii=True)
    
    # Pass all valid enums to the LLM so it knows what keys are allowed
    emotions = ", ".join([e.value for e in Emotion])
    actions = ", ".join([a.value for a in MascotAction])

    # 3. Build the specific instruction (The "User Prompt")
    user_prompt = (
        f"CONTEXT:\n"
        f"Intent: {request.intent}\n"
        f"User Facts: {facts_json}\n"
        f"Desired Tone: {request.desiredTone}\n\n"
        f"USER SAID:\n"
        f"{request.userMessage}\n\n"
        "INSTRUCTIONS:\n"
        "1. Respond in 1-2 natural, spoken sentences.\n"
        "2. Use the specific 'MODE' defined in your system prompt.\n"
        "3. No emojis. No markdown formatting (unless asking for code).\n"
        "4. RETURN JSON ONLY. Format: {\"replyText\": \"...\", \"emotion\": \"...\", \"mascotAction\": \"...\"}\n"
        f"   - Valid Emotions: {emotions}\n"
        f"   - Valid Actions: {actions}"
    )
    return system_prompt, user_prompt
