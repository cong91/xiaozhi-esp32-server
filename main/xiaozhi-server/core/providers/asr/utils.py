import re
from config.logger import setup_logging

TAG = __name__
logger = setup_logging()

EMOTION_EMOJI_MAP = {
    "HAPPY": "🙂",
    "SAD": "😔",
    "ANGRY": "😡",
    "NEUTRAL": "😶",
    "FEARFUL": "😰",
    "DISGUSTED": "🤢",
    "SURPRISED": "😲",
    "EMO_UNKNOWN": "😶",  # 未知情绪默认用中性表情
}
# EVENT_EMOJI_MAP = {
#     "<|BGM|>": "🎼",
#     "<|Speech|>": "",
#     "<|Applause|>": "👏",
#     "<|Laughter|>": "😀",
#     "<|Cry|>": "😭",
#     "<|Sneeze|>": "🤧",
#     "<|Breath|>": "",
#     "<|Cough|>": "🤧",
# }

# 各API通用的语言别名表：TTS配置里的语言名（Vietnamese/普通话/zh_CN…）
# 统一归一化成 ISO-639-1 码，供需要语言参数的 ASR 提供方使用
LANGUAGE_ALIASES = {
    "vi": "vi", "vie": "vi", "vietnamese": "vi", "tiếng việt": "vi", "tieng viet": "vi",
    "zh": "zh", "zh-cn": "zh", "zh_cn": "zh", "cmn": "zh", "zh-hans": "zh", "zh-hant": "zh",
    "chinese": "zh", "mandarin": "zh", "普通话": "zh", "中文": "zh",
    "yue": "yue", "cantonese": "yue", "粤语": "yue",
    "en": "en", "eng": "en", "english": "en", "tiếng anh": "en",
    "ja": "ja", "japanese": "ja", "日语": "ja",
    "ko": "ko", "korean": "ko", "韩语": "ko",
    "fr": "fr", "french": "fr",
    "de": "de", "german": "de",
    "es": "es", "spanish": "es",
    "ru": "ru", "russian": "ru",
    "th": "th", "thai": "th",
    "id": "id", "indonesian": "id",
    "ms": "ms", "malay": "ms",
    "pt": "pt", "portuguese": "pt",
    "hi": "hi", "hindi": "hi",
    "ar": "ar", "arabic": "ar",
}


def normalize_language(value) -> str | None:
    """
    把语言配置值归一化为 ISO-639-1 码，无法识别时返回 None。

    无法识别就不发送语言参数，让 API 回退到自动检测，避免发出非法值导致整个请求 400。
    >>> normalize_language("Vietnamese")
    'vi'
    >>> normalize_language("zh_CN")
    'zh'
    >>> normalize_language("auto") is None
    True
    """
    if not value:
        return None
    v = str(value).strip().lower().replace("_", "-")
    if v in LANGUAGE_ALIASES:
        return LANGUAGE_ALIASES[v]
    base = v.split("-")[0]
    if len(base) in (2, 3) and base.isalpha():
        return base
    return None

def lang_tag_filter(text: str) -> dict:
    """
    解析 FunASR 识别结果，按顺序提取标签和纯文本内容

    Args:
        text: ASR 识别的原始文本，可能包含多种标签

    Returns:
        dict: {"language": "zh", "emotion": "SAD", "emoji": "😔", "content": "你好"} 如果有标签，
              {"content": "纯文本"} 如果没有标签

    Examples:
        FunASR 输出格式：<|语种|><|情绪|><|事件|><|其他选项|>原文
        >>> lang_tag_filter("<|zh|><|SAD|><|Speech|><|withitn|>你好啊，测试测试。")
        {"language": "zh", "emotion": "SAD", "emoji": "😔", "content": "你好啊，测试测试。"}
        >>> lang_tag_filter("<|en|><|HAPPY|><|Speech|><|withitn|>Hello hello.")
        {"language": "en", "emotion": "HAPPY", "emoji": "🙂", "content": "Hello hello."}
        >>> lang_tag_filter("plain text")
        {"content": "plain text"}
    """
    # 提取所有标签（按顺序）
    tag_pattern = r"<\|([^|]+)\|>"
    all_tags = re.findall(tag_pattern, text)

    # 移除所有 <|...|> 格式的标签，获取纯文本
    clean_text = re.sub(tag_pattern, "", text).strip()

    # 保持返回结构一致，避免调用方把纯文本误当成字典访问。
    if not all_tags:
        return {"content": clean_text}

    # 按照 FunASR 的固定顺序提取标签，返回 dict
    language = all_tags[0] if len(all_tags) > 0 else "zh"
    emotion = all_tags[1] if len(all_tags) > 1 else "NEUTRAL"
    # event = all_tags[2] if len(all_tags) > 2 else "Speech"  # 事件标签暂不使用

    result = {
        "content": clean_text,
        "language": language,
        "emotion": emotion,
        # "event": event,
    }

    # 添加 emoji 映射
    if emotion in EMOTION_EMOJI_MAP:
        result["emotion"] = EMOTION_EMOJI_MAP[emotion]
    # 事件标签暂不使用
    # if event in EVENT_EMOJI_MAP:
    #     result["event"] = EVENT_EMOJI_MAP[event]

    return result
