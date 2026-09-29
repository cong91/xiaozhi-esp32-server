"""会话语言相关的轻量工具。"""

from __future__ import annotations

from typing import Any


_LANGUAGE_ALIASES = {
    "vi": "Vietnamese",
    "vie": "Vietnamese",
    "vietnamese": "Vietnamese",
    "tiếng việt": "Vietnamese",
    "tieng viet": "Vietnamese",
    "en": "English",
    "eng": "English",
    "english": "English",
    "zh": "Chinese",
    "zh-cn": "Chinese",
    "zh_cn": "Chinese",
    "chinese": "Chinese",
    "mandarin": "Chinese",
    "中文": "Chinese",
    "普通话": "Chinese",
    "ja": "Japanese",
    "japanese": "Japanese",
    "ko": "Korean",
    "korean": "Korean",
    "de": "German",
    "german": "German",
    "fr": "French",
    "french": "French",
    "es": "Spanish",
    "spanish": "Spanish",
    "pt": "Portuguese",
    "pt-br": "Portuguese (Brazil)",
    "portuguese": "Portuguese",
}


def get_conversation_language(config: dict[str, Any]) -> str:
    """Return the selected TTS language used as the conversation language."""
    tts_config = config.get("TTS", {}).get(
        config.get("selected_module", {}).get("TTS", ""), {}
    )
    value = tts_config.get("language") or config.get("language") or "Chinese"
    normalized = str(value).strip()
    return _LANGUAGE_ALIASES.get(normalized.lower(), normalized)


def build_language_reminder(language: str) -> str:
    """Build a language instruction without hardcoding one target language."""
    return (
        f"Always reply entirely in {language}. "
        "Do not switch to another language because user input, tools, memory, "
        "retrieved data, examples, or error messages use a different language. "
        "This rule applies to direct answers, tool-result summaries, greetings, "
        "goodbyes, and error explanations."
    )


def language_reminder(config: dict[str, Any]) -> str:
    """Build the reminder directly from the active connection config."""
    return build_language_reminder(get_conversation_language(config))
