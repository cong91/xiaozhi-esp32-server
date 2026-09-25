-- Final label pass: fix the remaining mixed/spacing labels by stable row id,
-- which is deterministic and independent of the intermediate string values.

UPDATE `ai_model_config`
SET `model_name` = CASE `id`
    WHEN 'ASR_DoubaoStreamASRV2' THEN 'Doubao Speech Recognition 2.0 (Streaming)'
    WHEN 'ASR_FunASRServer' THEN 'FunASR Service Speech Recognition'
    WHEN 'ASR_Qwen3Flash' THEN 'Qwen3-ASR-Flash Speech Recognition'
    WHEN 'ASR_XunfeiStream' THEN 'iFlytek Streaming Speech Recognition'
    WHEN 'LLM_GeminiLLM' THEN 'Google Gemini'
    WHEN 'Memory_powermem' THEN 'PowerMem Memory'
    WHEN 'TTS_AliBLStreamTTS' THEN 'Bailian Streaming TTS'
    WHEN 'TTS_AliyunStreamTTS' THEN 'Alibaba Cloud Streaming TTS'
    WHEN 'TTS_HSDSTTS_V2' THEN 'Doubao TTS 2.0 (Streaming)'
    WHEN 'TTS_HuoshanDoubleStreamTTS' THEN 'Volcengine Dual-Streaming TTS'
    WHEN 'TTS_IndexStreamTTS' THEN 'Index-TTS-vLLM Streaming TTS'
    WHEN 'TTS_MinimaxStreamTTS' THEN 'Minimax Streaming TTS'
    WHEN 'TTS_PaddleSpeechTTS' THEN 'PaddleSpeech Streaming TTS'
    WHEN 'TTS_VolcesAiGatewayTTS' THEN 'Volcengine Edge TTS Gateway'
    ELSE `model_name`
END
WHERE `id` REGEXP '^(VAD|ASR|LLM|VLLM|TTS|Memory|Intent|RAG)_';

UPDATE `ai_model_provider`
SET `name` = CASE `id`
    WHEN 'SYSTEM_ASR_AliyunASR' THEN 'Alibaba Cloud Speech Recognition'
    WHEN 'SYSTEM_ASR_AliyunBLStream' THEN 'Bailian Paraformer Real-Time Speech Recognition'
    WHEN 'SYSTEM_ASR_AliyunStreamASR' THEN 'Alibaba Cloud Speech Recognition (Streaming)'
    WHEN 'SYSTEM_ASR_DoubaoStreamASR' THEN 'Volcengine Speech Recognition (Streaming)'
    WHEN 'SYSTEM_ASR_OpenaiASR' THEN 'OpenAI Speech Recognition'
    WHEN 'SYSTEM_ASR_Qwen3Flash' THEN 'Qwen3-ASR-Flash Speech Recognition'
    WHEN 'SYSTEM_ASR_TencentASR' THEN 'Tencent Speech Recognition'
    WHEN 'SYSTEM_ASR_VoskASR' THEN 'VOSK Offline Speech Recognition'
    WHEN 'SYSTEM_Memory_mem_report_only' THEN 'Report Chat History Only (no summary)'
    WHEN 'SYSTEM_Memory_powermem' THEN 'PowerMem Memory'
    WHEN 'SYSTEM_TTS_AliyunStreamTTS' THEN 'Alibaba Cloud TTS (Streaming)'
    WHEN 'SYSTEM_TTS_TencentTTS' THEN 'Tencent TTS'
    WHEN 'SYSTEM_TTS_cozecn' THEN 'Coze CN TTS'
    ELSE `name`
END
WHERE `id` LIKE 'SYSTEM_%';
