"""MinimaxTTSHTTPStream 配置解析测试（离线，不发网络请求）"""

import pytest

from core.providers.tts.minimax_httpstream import TTSProvider


def make_provider(config):
    return TTSProvider(config, delete_audio_file=True)


def test_default_host_is_domestic_and_no_group_id():
    provider = make_provider({"api_key": "test", "model": "speech-02-turbo"})
    assert provider.api_url == "https://api.minimaxi.com/v1/t2a_v2"


def test_group_id_appended_when_configured():
    provider = make_provider(
        {"api_key": "test", "group_id": "181000000000000"}
    )
    assert (
        provider.api_url
        == "https://api.minimaxi.com/v1/t2a_v2?GroupId=181000000000000"
    )


def test_international_host_override():
    provider = make_provider(
        {"api_key": "test", "host": "api.minimax.io", "group_id": ""}
    )
    assert provider.api_url == "https://api.minimax.io/v1/t2a_v2"


def test_language_boost_read_from_config():
    provider = make_provider(
        {"api_key": "test", "language_boost": "auto"}
    )
    assert provider.language_boost == "auto"


def test_legacy_string_nested_settings_are_ignored():
    provider = make_provider(
        {
            "api_key": "test",
            "voice_setting": "",
            "pronunciation_dict": "",
            "audio_setting": "",
        }
    )
    assert provider.voice_setting["voice_id"] == "female-shaonv"
    assert provider.pronunciation_dict["tone"]
    assert provider.audio_setting["format"] == "pcm"


def test_mapping_nested_settings_override_defaults():
    provider = make_provider(
        {
            "api_key": "test",
            "voice_setting": {"emotion": "calm"},
            "pronunciation_dict": {"tone": ["xin chao"]},
            "audio_setting": {"format": "mp3"},
        }
    )
    assert provider.voice_setting["emotion"] == "calm"
    assert provider.pronunciation_dict["tone"] == ["xin chao"]
    assert provider.audio_setting["format"] == "mp3"

