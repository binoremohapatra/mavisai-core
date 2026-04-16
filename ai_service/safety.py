import re
from typing import Iterable

PROHIBITED_PHRASES = (
    "click",
    "tap",
    "open",
    "go to",
    "navigate",
    "press",
    "install",
    "download",
    "log in",
    "login",
    "sign in",
    "signup",
    "register",
    "visit",
    "browse",
)

PROHIBITED_SYSTEM_TERMS = (
    "system",
    "policy",
    "rules",
    "backend",
    "java",
    "orchestrator",
)

DEVICE_WORDS = (
    "laptop",
    "phone",
    "tablet",
    "device",
    "desktop",
    "pc",
)

TRANSFER_WORDS = (
    "send",
    "transfer",
    "share",
    "move",
    "sync",
    "push",
)


def sanitize_reply(text: str, max_sentences: int) -> str:
    if not text:
        return ""
    cleaned = text.replace("\r", " ").replace("\n", " ")
    cleaned = _strip_markdown(cleaned)
    cleaned = cleaned.encode("ascii", "ignore").decode("ascii")
    cleaned = _normalize_space(cleaned)
    cleaned = _limit_sentences(cleaned, max_sentences)
    return cleaned.strip()


def has_prohibited_phrase(text: str) -> bool:
    if not text:
        return True
    lowered = text.lower()
    for phrase in _iter_phrases(PROHIBITED_PHRASES):
        if phrase in lowered:
            return True
    for term in _iter_phrases(PROHIBITED_SYSTEM_TERMS):
        if term in lowered:
            return True
    return False


def wants_cross_device_support(user_message: str) -> bool:
    lowered = (user_message or "").lower()
    return any(word in lowered for word in DEVICE_WORDS) and any(
        word in lowered for word in TRANSFER_WORDS
    )


def _strip_markdown(text: str) -> str:
    text = text.replace("```", " ").replace("`", " ")
    text = text.replace("*", " ").replace("_", " ").replace("#", " ")
    text = re.sub(r"(?m)^\s*[-*]\s+", "", text)
    return text


def _normalize_space(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def _split_sentences(text: str) -> list[str]:
    parts = re.split(r"(?<=[.!?])\s+", text)
    return [part.strip() for part in parts if part.strip()]


def _limit_sentences(text: str, max_sentences: int) -> str:
    parts = _split_sentences(text)
    if not parts:
        return ""
    return " ".join(parts[: max(1, max_sentences)])


def _iter_phrases(phrases: Iterable[str]) -> Iterable[str]:
    for phrase in phrases:
        yield phrase.strip().lower()
