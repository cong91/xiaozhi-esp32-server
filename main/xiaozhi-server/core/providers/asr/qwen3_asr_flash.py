import os
import base64
import time
from typing import Optional, Tuple, List
import dashscope
from config.logger import setup_logging
from core.providers.asr.base import ASRProviderBase
from core.providers.asr.dto.dto import InterfaceType
from core.providers.asr.utils import normalize_language

tag = __name__
logger = setup_logging()


class ASRProvider(ASRProviderBase):
    def __init__(self, config: dict, delete_audio_file: bool):
        super().__init__()
        # 音频文件上传类型，流式文本识别输出
        self.interface_type = InterfaceType.NON_STREAM
        """Qwen3-ASR-Flash ASR初始化"""
        
        # 配置参数
        self.api_key = config.get("api_key")
        if not self.api_key:
            raise ValueError("Qwen3-ASR-Flash 需要配置 api_key")

        # 可选：自定义 DashScope 服务地址（如新加坡 workspace 专用域名）
        # 不配置时使用 SDK 默认值 https://dashscope.aliyuncs.com/api/v1
        self.base_url = config.get("base_url")
        if self.base_url:
            dashscope.base_http_api_url = self.base_url
            
        self.model_name = config.get("model_name", "qwen3-asr-flash")
        self.output_dir = config.get("output_dir", "./audio_output")
        self.delete_audio_file = delete_audio_file
        
        # ASR选项配置
        self.enable_lid = config.get("enable_lid", True)  # 自动语种检测
        self.enable_itn = config.get("enable_itn", True)  # 逆文本归一化
        self.language = config.get("language", None)  # 指定语种，默认自动检测
        self.context = config.get("context", "")  # 上下文信息，用于提高识别准确率
        
        # 确保输出目录存在
        os.makedirs(self.output_dir, exist_ok=True)

    def prefers_temp_file(self) -> bool:
        return True

    def requires_file(self) -> bool:
        return True

    async def speech_to_text(
        self, opus_data: List[bytes], session_id: str, artifacts=None
    ) -> Tuple[Optional[str], Optional[str]]:
        """将语音数据转换为文本"""
        file_path = None
        try:
            if artifacts is None:
                return "", None
            file_path = artifacts.file_path
            temp_file_path = artifacts.temp_path
            if not temp_file_path:
                return "", file_path

            # 以 base64 data URI 内联音频：传本地路径会触发 SDK 的 OSS 上传
            # （先向 /uploads 取证书，workspace 域名下偶发
            # "Get upload certificate failed"），内联后无此依赖也少一次上传往返
            with open(temp_file_path, "rb") as audio_file:
                audio_b64 = base64.b64encode(audio_file.read()).decode()
            messages = [
                {
                    "role": "user",
                    "content": [
                        {"audio": f"data:audio/wav;base64,{audio_b64}"}
                    ]
                }
            ]

            # 如果有上下文信息，添加system消息
            if self.context:
                messages.insert(0, {
                    "role": "system",
                    "content": [
                        {"text": self.context}
                    ]
                })

            # 准备ASR选项
            asr_options = {
                "enable_lid": self.enable_lid,
                "enable_itn": self.enable_itn
            }

            # 归一化失败则不传语种，保持自动检测（enable_lid）
            language = normalize_language(self.language)
            if language:
                asr_options["language"] = language

            # 设置API密钥
            dashscope.api_key = self.api_key

            # 发送流式请求，网关偶发抖动时重试一次
            full_text = ""
            for attempt in range(2):
                full_text = ""
                try:
                    response = dashscope.MultiModalConversation.call(
                        model=self.model_name,
                        messages=messages,
                        result_format="message",
                        asr_options=asr_options,
                        stream=True
                    )

                    # 处理流式响应，取最后一个完整文本
                    for chunk in response:
                        try:
                            text = chunk["output"]["choices"][0]["message"].content[0]["text"]
                            full_text = text.strip()
                        except Exception:
                            pass
                    return full_text, file_path
                except Exception as e:
                    logger.bind(tag=tag).warning(
                        f"语音识别失败(第{attempt + 1}次尝试): {e}"
                    )
                    if attempt == 0:
                        time.sleep(0.5)
                    else:
                        raise
            return full_text, file_path

        except Exception as e:
            logger.bind(tag=tag).error(f"语音识别失败: {e}")
            return "", file_path
