"""ElevenLabs TTS 配置解析与请求构造测试（离线，不发网络请求）"""

import pytest

from core.providers.tts.elevenlabs import TTSProvider


def make_provider(config):
    return TTSProvider(config, delete_audio_file=True)


def test_default_url_model_and_output_format():
    provider = make_provider({"api_key": "test", "voice": "voice-1"})
    assert provider.api_url == "https://api.elevenlabs.io/v1/text-to-speech"
    assert provider.model == "eleven_flash_v2_5"
    assert provider.output_format == "mp3_44100_128"
    assert provider.audio_file_type == "mp3"


def test_format_mapping():
    provider = make_provider({"api_key": "test", "voice": "v", "format": "wav"})
    assert provider.output_format == "wav_44100"
    provider = make_provider({"api_key": "test", "voice": "v", "format": "pcm"})
    assert provider.output_format == "pcm_24000"


def test_private_voice_wins_over_voice():
    provider = make_provider(
        {"api_key": "test", "voice": "public", "private_voice": "private"}
    )
    assert provider.voice == "private"


def test_speed_clamped_to_elevenlabs_range():
    provider = make_provider({"api_key": "test", "voice": "v", "speed": 2.0})
    assert provider.voice_settings["speed"] == 1.2
    provider = make_provider({"api_key": "test", "voice": "v", "speed": 0.1})
    assert provider.voice_settings["speed"] == 0.7


def test_voice_defaults_read_from_config():
    provider = make_provider(
        {
            "api_key": "test",
            "voice": "v",
            "stability": 0.3,
            "similarity_boost": 0.9,
        }
    )
    assert provider.voice_settings["stability"] == 0.3
    assert provider.voice_settings["similarity_boost"] == 0.9


@pytest.mark.asyncio
async def test_text_to_speak_builds_request_and_writes_file(tmp_path, monkeypatch):
    captured = {}

    class FakeResponse:
        status_code = 200
        content = b"fake-mp3-bytes"

        def __init__(self):
            pass

    def fake_post(url, json=None, headers=None, timeout=None):
        captured["url"] = url
        captured["json"] = json
        captured["headers"] = headers
        return FakeResponse()

    monkeypatch.setattr(
        "core.providers.tts.elevenlabs.requests.post", fake_post
    )

    provider = make_provider(
        {
            "api_key": "sk-test",
            "voice": "voice-abc",
            "language": "Vietnamese",
            "output_dir": str(tmp_path),
        }
    )
    out_file = tmp_path / "out.mp3"
    await provider.text_to_speak("Xin chào", str(out_file))

    assert captured["url"] == (
        "https://api.elevenlabs.io/v1/text-to-speech/voice-abc"
        "?output_format=mp3_44100_128"
    )
    assert captured["headers"]["xi-api-key"] == "sk-test"
    assert captured["json"]["text"] == "Xin chào"
    assert captured["json"]["model_id"] == "eleven_flash_v2_5"
    assert captured["json"]["language_code"] == "vi"
    assert out_file.read_bytes() == b"fake-mp3-bytes"


@pytest.mark.asyncio
async def test_text_to_speak_raises_on_api_error(monkeypatch):
    class FakeResponse:
        status_code = 401
        content = b""
        text = "invalid_api_key"

    def fake_post(url, json=None, headers=None, timeout=None):
        return FakeResponse()

    monkeypatch.setattr(
        "core.providers.tts.elevenlabs.requests.post", fake_post
    )

    provider = make_provider({"api_key": "bad", "voice": "v"})
    with pytest.raises(Exception, match="ElevenLabs TTS请求失败"):
        await provider.text_to_speak("hello", None)


@pytest.mark.asyncio
async def test_text_to_speak_without_voice_raises():
    provider = make_provider({"api_key": "test"})
    with pytest.raises(ValueError, match="voice"):
        await provider.text_to_speak("hello", None)
