import asyncio
import os
import time
from typing import Optional, Tuple, List

import requests

from config.logger import setup_logging
from core.providers.asr.dto.dto import InterfaceType
from core.providers.asr.base import ASRProviderBase
from core.providers.asr.utils import normalize_language

TAG = __name__
logger = setup_logging()


class ASRProvider(ASRProviderBase):
    """ElevenLabs Scribe 非流式ASR：multipart POST /v1/speech-to-text"""

    def __init__(self, config: dict, delete_audio_file: bool):
        self.interface_type = InterfaceType.NON_STREAM
        self.api_key = config.get("api_key")
        self.api_url = config.get(
            "base_url", "https://api.elevenlabs.io/v1/speech-to-text"
        )
        # scribe_v2 支持 90+ 种语言（含越南语），为当前文档中的主力模型
        self.model = config.get("model_name", "scribe_v2")
        self.output_dir = config.get("output_dir")
        # 识别语言（ISO-639-1 码）；不设置时 API 自动检测
        self.language = config.get("language")
        self.delete_audio_file = delete_audio_file

        os.makedirs(self.output_dir, exist_ok=True)

    def requires_file(self) -> bool:
        return True

    async def speech_to_text(
        self, opus_data: List[bytes], session_id: str, artifacts=None
    ) -> Tuple[Optional[str], Optional[str]]:
        if artifacts is None:
            return "", None
        file_path = artifacts.file_path
        try:
            logger.bind(tag=TAG).info(f"file path: {file_path}")
            headers = {
                "xi-api-key": self.api_key,
            }

            data = {"model_id": self.model}
            # 归一化失败则不传语言参数，回退到API自动检测
            language = normalize_language(self.language)
            if language:
                data["language_code"] = language

            with open(file_path, "rb") as audio_file:
                files = {"file": audio_file}

                start_time = time.time()
                # requests 是阻塞调用，放线程池避免卡住事件循环
                response = await asyncio.to_thread(
                    requests.post,
                    self.api_url,
                    files=files,
                    data=data,
                    headers=headers,
                    timeout=30,
                )
                logger.bind(tag=TAG).debug(
                    f"语音识别耗时: {time.time() - start_time:.3f}s | 结果: {response.text}"
                )

            if response.status_code == 200:
                text = response.json().get("text", "")
                return text, file_path
            else:
                raise Exception(
                    f"ElevenLabs ASR请求失败: {response.status_code} - {response.text}"
                )

        except Exception as e:
            logger.bind(tag=TAG).error(f"语音识别失败: {e}")
            return "", None
