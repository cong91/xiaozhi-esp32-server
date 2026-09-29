"""Language reminder and direct-response regression tests."""

from core.utils.conversation_language import (
    build_language_reminder,
    get_conversation_language,
)


def test_conversation_language_comes_from_selected_tts_config():
    config = {
        "selected_module": {"TTS": "TTS_Test"},
        "TTS": {"TTS_Test": {"language": "Vietnamese"}},
    }
    assert get_conversation_language(config) == "Vietnamese"
    assert "Vietnamese" in build_language_reminder("Vietnamese")


def test_language_aliases_are_normalized_without_vietnamese_hardcoding():
    config = {
        "selected_module": {"TTS": "TTS_Test"},
        "TTS": {"TTS_Test": {"language": "en-US"}},
    }
    assert get_conversation_language(config) == "en-US"
    reminder = build_language_reminder(get_conversation_language(config))
    assert "en-US" in reminder
    assert "Vietnamese" not in reminder


def test_missing_language_uses_generic_configurable_fallback():
    config = {"selected_module": {"TTS": "TTS_Test"}, "TTS": {"TTS_Test": {}}}
    assert get_conversation_language(config) == "Chinese"
