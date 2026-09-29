"""ElevenLabs Scribe ASR 配置解析与请求构造测试（离线，不发网络请求）"""

import pytest

from core.providers.asr.elevenlabs import ASRProvider
from core.providers.asr.base import ASRProviderBase
from core.providers.asr.dto.dto import InterfaceType


def make_provider(config):
    return ASRProvider(config, delete_audio_file=True)


def test_default_url_and_model(tmp_path):
    provider = make_provider({"output_dir": str(tmp_path)})
    assert provider.api_url == "https://api.elevenlabs.io/v1/speech-to-text"
    assert provider.model == "scribe_v2"
    assert provider.interface_type == InterfaceType.NON_STREAM


def test_is_non_stream_with_file_input(tmp_path):
    provider = make_provider({"output_dir": str(tmp_path)})
    assert isinstance(provider, ASRProviderBase)
    assert provider.requires_file() is True


def test_model_and_language_from_config(tmp_path):
    provider = make_provider(
        {
            "output_dir": str(tmp_path),
            "model_name": "scribe_v2",
            "language": "Vietnamese",
        }
    )
    assert provider.model == "scribe_v2"
    assert provider.language == "Vietnamese"


@pytest.mark.asyncio
async def test_speech_to_text_builds_multipart_request(tmp_path, monkeypatch):
    captured = {}

    class FakeResponse:
        status_code = 200
        text = '{"language_code": "vi", "language_probability": 0.98, "text": "Xin chào thế giới"}'

        def json(self):
            import json

            return json.loads(self.text)

    def fake_post(url, files=None, data=None, headers=None, timeout=None):
        captured["url"] = url
        captured["files"] = files
        captured["data"] = data
        captured["headers"] = headers
        return FakeResponse()

    monkeypatch.setattr(
        "core.providers.asr.elevenlabs.requests.post", fake_post
    )

    wav_file = tmp_path / "asr_test.wav"
    wav_file.write_bytes(b"fake-wav-bytes")
    artifacts = ASRProviderBase.AudioArtifacts(
        pcm_frames=[b"\x00\x00"],
        pcm_bytes=b"\x00\x00",
        file_path=str(wav_file),
        temp_path=None,
    )

    provider = make_provider(
        {
            "output_dir": str(tmp_path),
            "api_key": "sk-test",
            "model_name": "scribe_v2",
            "language": "Vietnamese",
        }
    )
    text, file_path = await provider.speech_to_text(
        [b"\x00\x00"], "session-1", artifacts
    )

    assert captured["url"] == "https://api.elevenlabs.io/v1/speech-to-text"
    assert captured["headers"]["xi-api-key"] == "sk-test"
    assert captured["data"]["model_id"] == "scribe_v2"
    assert captured["data"]["language_code"] == "vi"
    assert "file" in captured["files"]
    assert text == "Xin chào thế giới"
    assert file_path == str(wav_file)


@pytest.mark.asyncio
async def test_speech_to_text_without_language_omits_param(tmp_path, monkeypatch):
    captured = {}

    class FakeResponse:
        status_code = 200
        text = '{"text": "hello"}'

        def json(self):
            import json

            return json.loads(self.text)

    def fake_post(url, files=None, data=None, headers=None, timeout=None):
        captured["data"] = data
        return FakeResponse()

    monkeypatch.setattr(
        "core.providers.asr.elevenlabs.requests.post", fake_post
    )

    wav_file = tmp_path / "asr_test.wav"
    wav_file.write_bytes(b"fake-wav-bytes")
    artifacts = ASRProviderBase.AudioArtifacts(
        pcm_frames=[b"\x00\x00"],
        pcm_bytes=b"\x00\x00",
        file_path=str(wav_file),
        temp_path=None,
    )

    provider = make_provider({"output_dir": str(tmp_path), "api_key": "sk-test"})
    text, _ = await provider.speech_to_text([b"\x00\x00"], "s", artifacts)

    assert "language_code" not in captured["data"]
    assert text == "hello"


@pytest.mark.asyncio
async def test_speech_to_text_api_error_returns_empty(tmp_path, monkeypatch):
    class FakeResponse:
        status_code = 401
        text = '{"detail": {"message": "invalid_api_key"}}'

        def json(self):
            import json

            return json.loads(self.text)

    def fake_post(url, files=None, data=None, headers=None, timeout=None):
        return FakeResponse()

    monkeypatch.setattr(
        "core.providers.asr.elevenlabs.requests.post", fake_post
    )

    wav_file = tmp_path / "asr_test.wav"
    wav_file.write_bytes(b"fake-wav-bytes")
    artifacts = ASRProviderBase.AudioArtifacts(
        pcm_frames=[b"\x00\x00"],
        pcm_bytes=b"\x00\x00",
        file_path=str(wav_file),
        temp_path=None,
    )

    provider = make_provider({"output_dir": str(tmp_path), "api_key": "bad"})
    text, file_path = await provider.speech_to_text([b"\x00\x00"], "s", artifacts)
    assert text == ""


@pytest.mark.asyncio
async def test_speech_to_text_without_artifacts_returns_empty(tmp_path):
    provider = make_provider({"output_dir": str(tmp_path), "api_key": "sk-test"})
    text, file_path = await provider.speech_to_text([b"\x00\x00"], "s", None)
    assert text == ""
