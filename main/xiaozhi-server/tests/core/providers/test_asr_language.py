"""ASR 语言参数修复测试（离线，不发网络请求）：

- normalize_language 归一化各来源的语言配置值
- openai ASR 提供方把 language 放进 multipart 表单
- initialize_asr 在 ASR 未配置语言时继承 TTS 语言
"""

import asyncio
from unittest.mock import MagicMock, patch

from core.providers.asr.utils import normalize_language
from core.providers.asr.openai import ASRProvider as OpenAIASRProvider
from core.utils.modules_initialize import initialize_asr


def test_normalize_language_full_names():
    assert normalize_language("Vietnamese") == "vi"
    assert normalize_language("vietnamese") == "vi"
    assert normalize_language("tiếng việt") == "vi"
    assert normalize_language("Chinese") == "zh"
    assert normalize_language("普通话") == "zh"
    assert normalize_language("English") == "en"


def test_normalize_language_codes():
    assert normalize_language("zh_CN") == "zh"
    assert normalize_language("vi-VN") == "vi"
    assert normalize_language("vi") == "vi"
    assert normalize_language("zh") == "zh"


def test_normalize_language_unrecognized_returns_none():
    # 无法识别就不发送语言参数，回退 API 自动检测
    assert normalize_language("auto") is None
    assert normalize_language("") is None
    assert normalize_language(None) is None
    assert normalize_language("klingon") is None


def make_openai_asr(language=None):
    import tempfile

    config = {
        "api_key": "test",
        "base_url": "https://example.invalid/v1/audio/transcriptions",
        "model_name": "whisper-large-v3-turbo",
        "output_dir": tempfile.gettempdir(),
    }
    if language is not None:
        config["language"] = language
    return OpenAIASRProvider(config, delete_audio_file=True)


def test_openai_asr_sends_normalized_language():
    provider = make_openai_asr("Vietnamese")
    assert provider.language == "Vietnamese"

    fake_resp = MagicMock(status_code=200, text="{}")
    fake_resp.json.return_value = {"text": "xin chào"}
    with patch("core.providers.asr.openai.requests.post", return_value=fake_resp) as mock_post:
        artifacts = OpenAIASRProvider.AudioArtifacts(
            pcm_frames=[], pcm_bytes=b"", file_path=__file__, temp_path=__file__
        )
        text, _ = asyncio.run(provider.speech_to_text([], "session", artifacts))

    assert text == "xin chào"
    data = mock_post.call_args.kwargs["data"]
    assert data["language"] == "vi"


def test_openai_asr_omits_language_when_unset_or_unrecognized():
    fake_resp = MagicMock(status_code=200, text="{}")
    fake_resp.json.return_value = {"text": "hello"}
    with patch("core.providers.asr.openai.requests.post", return_value=fake_resp) as mock_post:
        artifacts = OpenAIASRProvider.AudioArtifacts(
            pcm_frames=[], pcm_bytes=b"", file_path=__file__, temp_path=__file__
        )
        asyncio.run(make_openai_asr().speech_to_text([], "session", artifacts))
        data = mock_post.call_args.kwargs["data"]
        assert "language" not in data

        asyncio.run(make_openai_asr("auto").speech_to_text([], "session", artifacts))
        data = mock_post.call_args.kwargs["data"]
        assert "language" not in data


def make_config(tmp_asr_section, tts_language):
    return {
        "selected_module": {"ASR": "ASR_Test", "TTS": "TTS_Test"},
        "ASR": {"ASR_Test": tmp_asr_section},
        "TTS": {"TTS_Test": {"type": "openai", "api_key": "k", "language": tts_language}},
        "delete_audio": "true",
    }


def test_initialize_asr_inherits_tts_language(tmp_path):
    asr_section = {
        "type": "openai",
        "api_key": "test",
        "base_url": "https://example.invalid/v1/audio/transcriptions",
        "model_name": "whisper-large-v3-turbo",
        "output_dir": str(tmp_path),
    }
    provider = initialize_asr(make_config(asr_section, "Vietnamese"))
    assert provider.language == "vi"


def test_initialize_asr_explicit_language_wins(tmp_path):
    asr_section = {
        "type": "openai",
        "api_key": "test",
        "base_url": "https://example.invalid/v1/audio/transcriptions",
        "model_name": "whisper-large-v3-turbo",
        "output_dir": str(tmp_path),
        "language": "en",
    }
    provider = initialize_asr(make_config(asr_section, "Vietnamese"))
    assert provider.language == "en"


def test_initialize_asr_auto_keeps_default(tmp_path):
    asr_section = {
        "type": "openai",
        "api_key": "test",
        "base_url": "https://example.invalid/v1/audio/transcriptions",
        "model_name": "whisper-large-v3-turbo",
        "output_dir": str(tmp_path),
        "language": "auto",
    }
    provider = initialize_asr(make_config(asr_section, "Vietnamese"))
    assert provider.language == "auto"
