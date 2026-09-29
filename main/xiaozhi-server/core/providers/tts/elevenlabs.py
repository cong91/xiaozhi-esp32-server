import asyncio

import requests

from core.utils.util import check_model_key
from core.providers.tts.base import TTSProviderBase
from core.providers.asr.utils import normalize_language
from config.logger import setup_logging

TAG = __name__
logger = setup_logging()


class TTSProvider(TTSProviderBase):
    """ElevenLabs 非流式TTS：POST /v1/text-to-speech/{voice_id}，返回音频字节"""

    # ElevenLabs 语速仅接受 0.7~1.2，控制台 -100~100 百分比映射到该范围
    TTS_PARAM_CONFIG = [
        ("ttsRate", "speed", 0.7, 1.2, 1, lambda v: round(float(v), 2)),
    ]

    # format 配置项映射到 API 的 output_format（编码_采样率_码率）
    OUTPUT_FORMATS = {
        "mp3": "mp3_44100_128",
        "wav": "wav_44100",
        "pcm": "pcm_24000",
        "ulaw": "ulaw_8000",
    }

    def __init__(self, config, delete_audio_file):
        super().__init__(config, delete_audio_file)
        self.api_key = config.get("api_key")
        self.api_url = config.get(
            "api_url", "https://api.elevenlabs.io/v1/text-to-speech"
        )
        # eleven_flash_v2_5 支持 32 种语言（含越南语）且延迟最低；eleven_v3 更有情感
        self.model = config.get("model", "eleven_flash_v2_5")
        if config.get("private_voice"):
            self.voice = config.get("private_voice")
        else:
            self.voice = config.get("voice")
        self.audio_file_type = config.get("format", "mp3")
        self.output_format = self.OUTPUT_FORMATS.get(
            self.audio_file_type, "mp3_44100_128"
        )
        # 指定语种可提升发音准确度；multilingual_v2 不支持该参数，填了会 422。
        # 归一化成 ISO-639-1 码，识别不了的值不下发，避免 400
        self.language_code = normalize_language(config.get("language"))

        self.voice_settings = {
            "stability": float(config.get("stability", 0.5)),
            "similarity_boost": float(config.get("similarity_boost", 0.75)),
        }
        # 处理空字符串的情况，并夹取到 ElevenLabs 允许的 0.7~1.2 范围
        speed = config.get("speed", "1.0")
        self.speed = float(speed) if speed else 1.0
        self.speed = max(0.7, min(1.2, self.speed))
        self.voice_settings["speed"] = self.speed
        # 应用百分比调整（如果存在），覆盖上面的默认语速
        self._apply_percentage_params(config)
        self.voice_settings["speed"] = self.speed

        model_key_msg = check_model_key("TTS", self.api_key)
        if model_key_msg:
            logger.bind(tag=TAG).error(model_key_msg)
        if not self.voice:
            logger.bind(tag=TAG).error(
                "ElevenLabs TTS缺少voice配置，请填入voice_id（GET /v1/voices 可查询）"
            )

    async def text_to_speak(self, text, output_file):
        if not self.voice:
            raise ValueError("ElevenLabs TTS缺少voice配置")

        headers = {
            "xi-api-key": self.api_key,
            "Content-Type": "application/json",
        }
        data = {
            "text": text,
            "model_id": self.model,
            "voice_settings": self.voice_settings,
        }
        if self.language_code:
            data["language_code"] = self.language_code

        url = f"{self.api_url}/{self.voice}?output_format={self.output_format}"
        # requests 是阻塞调用，放线程池避免卡住事件循环
        response = await asyncio.to_thread(
            requests.post, url, json=data, headers=headers, timeout=self.tts_timeout
        )
        if response.status_code != 200:
            raise Exception(
                f"ElevenLabs TTS请求失败: {response.status_code} - {response.text}"
            )

        if output_file:
            with open(output_file, "wb") as audio_file:
                audio_file.write(response.content)
        else:
            return response.content
