-- Translate system catalog display labels to English for the management console.
-- Scope: ai_model_provider names/form labels, ai_model_config display names,
-- ai_tts_voice names/languages, agent template names, mobile area dictionary,
-- human-readable remarks, and tts_language sync so voice-language filtering keeps working.
-- Runtime values (API keys, URLs, model IDs, voice codes, prompts, lang_code) are unchanged.

-- Widen display columns first: translated labels are longer than the
-- original Chinese ones and ai_tts_voice.name is only VARCHAR(20).
ALTER TABLE `ai_tts_voice`
    MODIFY COLUMN `name` VARCHAR(100) COMMENT 'Voice name',
    MODIFY COLUMN `languages` VARCHAR(100) COMMENT 'Languages';

ALTER TABLE `sys_params`
    MODIFY COLUMN `remark` VARCHAR(500) COMMENT 'Remark';

UPDATE `ai_model_provider`
SET
    `name` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`name`, '仅上报聊天记录', 'Report Chat History Only'), '本地短记忆', 'Local Short-Term Memory'), '语音活动检测', 'Voice Activity Detection'), '语音识别', 'Speech Recognition'), '语音合成', 'Text-to-Speech'), '函数调用', 'Function Calling'), '意图识别', 'Intent Recognition'), '大模型', 'Large Language Model'), '小模型', 'Small Model'), '通义千问', 'Qwen'), '通义百炼', 'DashScope'), '火山引擎', 'Volcengine'), '阿里云', 'Alibaba Cloud'), '腾讯', 'Tencent'), '讯飞', 'iFlytek'), '豆包', 'Doubao'), '硅基流动', 'SiliconFlow'), '无记忆', 'No Memory'), '记忆', 'Memory'),
    `fields` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`fields`, '是否开启链接复用', 'Enable WebSocket Reuse'), '是否使用内存缓存', 'Use Memory Cache'), '是否使用本地prompt', 'Use Local Prompt'), '是否不使用本地prompt', 'Disable Local Prompt'), '是否使用SR', 'Use SR'), '是否口语化', 'Colloquial'), '是否流式模式', 'Streaming Mode'), 'WebSocket地址（含Workspace ID）', 'WebSocket URL (including Workspace ID)'), '辅助参考音频路径', 'Aux Reference Audio Paths'), 'WebSocket地址', 'WebSocket URL'), '最小静音时长', 'Minimum Silence Duration'), '是否使用SSL', 'Use SSL'), '是否标准化', 'Normalize'), '是否并行推理', 'Parallel Inference'), '是否返回片段', 'Return Fragment'), '是否分桶', 'Split Bucket'), '是否流式', 'Streaming'), '检测阈值', 'Detection Threshold'), '文本分割方法', 'Text Split Method'), '批处理阈值', 'Batch Threshold'), '批处理大小', 'Batch Size'), '最大新令牌数', 'Max New Tokens'), '最大令牌数', 'Max Tokens'), '重复惩罚', 'Repetition Penalty'), '频率惩罚', 'Frequency Penalty'), '个人访问令牌', 'Personal Access Token'), '访问密钥密码', 'Access Key Secret'), '访问密钥ID', 'Access Key ID'), '目标语言', 'Target Language'), '文本语言', 'Text Language'), '提示语言', 'Prompt Language'), '提示文本', 'Prompt Text'), '对话模式', 'Conversation Mode'), '服务类型', 'Service Type'), '响应格式', 'Response Format'), '音频格式', 'Audio Format'), '默认音色', 'Default Voice'), '模型名称', 'Model Name'), '模型目录', 'Model Directory'), '输出目录', 'Output Directory'), 'API服务地址', 'API Service URL'), '服务地址', 'Service URL'), '基础URL', 'Base URL'), '应用密钥', 'App Key'), '访问令牌', 'Access Token'), 'API密钥', 'API Key'), '机器人ID', 'Bot ID'), '用户ID', 'User ID'), '参考音频', 'Reference Audio'), '参考文本', 'Reference Text'), '参考ID', 'Reference ID'), '端口号', 'Port'), '资源ID', 'Resource ID'), '组ID', 'Group ID'), '音色ID', 'Voice ID'), '应用ID', 'App ID'), '记忆ID', 'Memory ID'), '速度因子', 'Speed Factor'), '音调因子', 'Pitch Factor'), '音量变化', 'Volume Change'), '切分标点', 'Punctuation Split'), '采样率', 'Sample Rate'), '语速', 'Speech Rate'), '音调', 'Pitch'), '音量', 'Volume'), '温度', 'Temperature'), '速度', 'Speed'), '模型', 'Model'), '音色', 'Voice'), '情感', 'Emotion'), '格式', 'Format'), '区域', 'Region'), '集群', 'Cluster'), '授权', 'Authorization'), '通道数', 'Channels'), '块长度', 'Chunk Length'), '种子', 'Seed'), '变量', 'Variables'), '函数列表', 'Function List'), '请求参数', 'Request Parameters'), '请求头', 'Request Headers'), '密钥', 'Key'), '令牌', 'Token'), '默认', 'Default'), '语言', 'Language'), '模式', 'Mode'), '应用', 'Application'), '类型', 'Type'), '列表', 'List'), '参数', 'Parameter'), '是否', 'Use')
WHERE `id` LIKE 'SYSTEM_%';

UPDATE `ai_model_config`
SET `model_name` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`model_name`, '仅上报聊天记录', 'Report Chat History Only'), '本地短期记忆', 'Local Short-Term Memory'), '本地短记忆', 'Local Short-Term Memory'), '语音活动检测', 'Voice Activity Detection'), '语音识别', 'Speech Recognition'), '语音合成', 'Text-to-Speech'), '函数调用', 'Function Calling'), '意图识别', 'Intent Recognition'), '大模型', 'Large Language Model'), '小模型', 'Small Model'), '实时', 'Real-Time'), '流式', 'Streaming'), '离线', 'Offline'), '通义千问', 'Qwen'), '通义百炼', 'DashScope'), '火山引擎', 'Volcengine'), '阿里云', 'Alibaba Cloud'), '腾讯', 'Tencent'), '讯飞', 'iFlytek'), '豆包', 'Doubao'), '谷歌', 'Google'), '硅基流动', 'SiliconFlow'), '无记忆', 'No Memory'), '记忆', 'Memory'), '模型', 'Model'), '服务', 'Service'), '智谱AI', 'Zhipu AI')
WHERE `id` REGEXP '^(VAD|ASR|LLM|VLLM|TTS|Memory|Intent|RAG)_';

UPDATE `ai_tts_voice`
SET
    `name` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`name`, '越南女声', 'Vietnamese Female'), '越南男声', 'Vietnamese Male'), '女声', 'Female'), '女生', 'Female'), '男声', 'Male'), '男生', 'Male'), '童声', 'Child Voice'), '少女音', 'Girl Voice'), '萌娃', 'Child Voice'), '通用', 'General'), '普通话', 'Mandarin'), '越南', 'Vietnamese'), '中文', 'Chinese'), '英文', 'English'),
    `languages` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`languages`, '越南语', 'Vietnamese'), '西班牙语', 'Spanish'), '普通话', 'Mandarin'), '中文', 'Chinese'), '英文', 'English'), '英语', 'English'), '日语', 'Japanese'), '韩语', 'Korean'), '粤语', 'Cantonese'), '越南', 'Vietnamese'), '西语', 'Spanish'), '辽宁', 'Liaoning Accent'), '陕西', 'Shaanxi Accent'), '、', ', ')
WHERE `id` LIKE 'TTS_%';

UPDATE `ai_agent`
SET `tts_language` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`tts_language`, '越南语', 'Vietnamese'), '西班牙语', 'Spanish'), '普通话', 'Mandarin'), '中文', 'Chinese'), '英文', 'English'), '英语', 'English'), '日语', 'Japanese'), '韩语', 'Korean'), '粤语', 'Cantonese'), '越南', 'Vietnamese'), '西语', 'Spanish'), '、', ', ')
WHERE `tts_language` IS NOT NULL;

UPDATE `ai_agent_template`
SET `tts_language` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`tts_language`, '越南语', 'Vietnamese'), '西班牙语', 'Spanish'), '普通话', 'Mandarin'), '中文', 'Chinese'), '英文', 'English'), '英语', 'English'), '日语', 'Japanese'), '韩语', 'Korean'), '粤语', 'Cantonese'), '越南', 'Vietnamese'), '西语', 'Spanish'), '、', ', ')
WHERE `tts_language` IS NOT NULL;

UPDATE `ai_agent_template`
SET `agent_name` = CASE `id`
    WHEN '9406648b5cc5fde1b8aa335b6f8b4f76' THEN 'Wanwan Xiaohe'
    WHEN '0ca32eb728c949e58b1000b2e401f90c' THEN 'Cosmic Traveler'
    WHEN '6c7d8e9f0a1b2c3d4e5f6a7b8c9d0s24' THEN 'English Teacher'
    WHEN 'e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b1' THEN 'Curious Boy'
    WHEN 'a45b6c7d8e9f0a1b2c3d4e5f6a7b8c92' THEN 'Woof Captain'
    ELSE `agent_name`
END
WHERE `id` IN (
    '9406648b5cc5fde1b8aa335b6f8b4f76',
    '0ca32eb728c949e58b1000b2e401f90c',
    '6c7d8e9f0a1b2c3d4e5f6a7b8c9d0s24',
    'e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b1',
    'a45b6c7d8e9f0a1b2c3d4e5f6a7b8c92'
);

UPDATE `ai_model_config`
SET `remark` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`remark`, '配置说明', 'Configuration Notes'), '申请步骤', 'Application Steps'), '当前使用', 'Current model'), '网络连接', 'Internet access'), '输出文件', 'Output file'), '保存在', 'is saved in'), '语音识别', 'Speech Recognition'), '语音合成', 'Text-to-Speech'), '多语言', 'multilingual'), '函数调用', 'function calling'), '登录', 'Log in'), '访问', 'Open'), '获取', 'Get'), '填入', 'Enter'), '开通', 'Enable'), '购买', 'Purchase'), '步骤', 'Steps'), '支持', 'Supports'), '需要', 'Requires'), '说明', 'Notes'), '模型', 'Model'), '语音', 'Speech'), '识别', 'Recognition'), '服务', 'Service'), '配置', 'Configuration'), '密钥', 'Key'), '应用', 'Application'), '目录', 'directory'), '参数', 'Parameters'), '默认', 'Default'), '注意', 'Note'), '详细', 'Detailed'), '文档', 'documentation'), '本地', 'local'), '云端', 'cloud'), '中文', 'Chinese'), '英文', 'English'), '日语', 'Japanese'), '韩语', 'Korean'), '粤语', 'Cantonese'), '流式', 'streaming'), '实时', 'real-time'), '大模型', 'large language model'), '记忆', 'memory')
WHERE `id` REGEXP '^(VAD|ASR|LLM|VLLM|TTS|Memory|Intent|RAG)_'
  AND `remark` IS NOT NULL;

UPDATE `sys_params`
SET `remark` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`remark`, '结束提示音', 'Stop Notification Sound'), '开场回复', 'Greeting'), '无语音输入', 'No voice input'), '断开连接', 'Disconnect'), '唤醒词', 'Wake Word'), '加速', 'Acceleration'), '服务器', 'Server'), '监听', 'Listen'), '控制台', 'Console'), '文件名', 'File Name'), '管理员', 'Administrator'), '删除', 'Delete'), '使用后的', 'after use'), '音频', 'Audio'), '日志', 'Log'), '格式', 'Format'), '目录', 'Directory'), '数据', 'Data'), '时间', 'Time'), '请求', 'Request'), '超时', 'Timeout'), '秒', 'seconds'), '开启', 'Enable'), '路径', 'Path'), '列表', 'List'), '类型', 'Type'), '默认', 'Default'), '城市', 'City'), '插件', 'Plugin'), '分类', 'Category'), '设备', 'Device'), '参数', 'Parameter'), '允许', 'Allow'), '注册', 'Registration'), '语音', 'Speech'), '识别', 'Recognition'), '说明', 'Notes'), '配置', 'Configuration'), '访问', 'Access'), '令牌', 'Token'), '区域字典', 'Area Dictionary'), '区域', 'Region'), '手机', 'Mobile'), '地址', 'Address'), '密钥', 'Key'), '是否', 'Enable')
WHERE `remark` IS NOT NULL;

UPDATE `sys_dict_type`
SET `dict_name` = 'Mobile Area', `remark` = 'Mobile Area Dictionary'
WHERE `id` = 102;

UPDATE `sys_dict_data`
SET
    `dict_label` = CASE `id`
        WHEN 102001 THEN 'Mainland China'
        WHEN 102002 THEN 'Hong Kong, China'
        WHEN 102003 THEN 'Macau, China'
        WHEN 102004 THEN 'Taiwan, China'
        WHEN 102005 THEN 'United States / Canada'
        WHEN 102006 THEN 'United Kingdom'
        WHEN 102007 THEN 'France'
        WHEN 102008 THEN 'Italy'
        WHEN 102009 THEN 'Germany'
        WHEN 102010 THEN 'Poland'
        WHEN 102011 THEN 'Switzerland'
        WHEN 102012 THEN 'Spain'
        WHEN 102013 THEN 'Denmark'
        WHEN 102014 THEN 'Malaysia'
        WHEN 102015 THEN 'Australia'
        WHEN 102016 THEN 'Indonesia'
        WHEN 102017 THEN 'Philippines'
        WHEN 102018 THEN 'New Zealand'
        WHEN 102019 THEN 'Singapore'
        WHEN 102020 THEN 'Thailand'
        WHEN 102021 THEN 'Japan'
        WHEN 102022 THEN 'South Korea'
        WHEN 102023 THEN 'Vietnam'
        WHEN 102024 THEN 'India'
        WHEN 102025 THEN 'Pakistan'
        WHEN 102026 THEN 'Nigeria'
        WHEN 102027 THEN 'Bangladesh'
        WHEN 102028 THEN 'Saudi Arabia'
        WHEN 102029 THEN 'United Arab Emirates'
        WHEN 102030 THEN 'Brazil'
        WHEN 102031 THEN 'Mexico'
        WHEN 102032 THEN 'Chile'
        WHEN 102033 THEN 'Argentina'
        WHEN 102034 THEN 'Egypt'
        WHEN 102035 THEN 'South Africa'
        WHEN 102036 THEN 'Kenya'
        WHEN 102037 THEN 'Tanzania'
        WHEN 102038 THEN 'Kazakhstan'
        ELSE `dict_label`
    END,
    `remark` = CASE `id`
        WHEN 102001 THEN 'Mainland China'
        WHEN 102002 THEN 'Hong Kong, China'
        WHEN 102003 THEN 'Macau, China'
        WHEN 102004 THEN 'Taiwan, China'
        WHEN 102005 THEN 'United States / Canada'
        WHEN 102006 THEN 'United Kingdom'
        WHEN 102007 THEN 'France'
        WHEN 102008 THEN 'Italy'
        WHEN 102009 THEN 'Germany'
        WHEN 102010 THEN 'Poland'
        WHEN 102011 THEN 'Switzerland'
        WHEN 102012 THEN 'Spain'
        WHEN 102013 THEN 'Denmark'
        WHEN 102014 THEN 'Malaysia'
        WHEN 102015 THEN 'Australia'
        WHEN 102016 THEN 'Indonesia'
        WHEN 102017 THEN 'Philippines'
        WHEN 102018 THEN 'New Zealand'
        WHEN 102019 THEN 'Singapore'
        WHEN 102020 THEN 'Thailand'
        WHEN 102021 THEN 'Japan'
        WHEN 102022 THEN 'South Korea'
        WHEN 102023 THEN 'Vietnam'
        WHEN 102024 THEN 'India'
        WHEN 102025 THEN 'Pakistan'
        WHEN 102026 THEN 'Nigeria'
        WHEN 102027 THEN 'Bangladesh'
        WHEN 102028 THEN 'Saudi Arabia'
        WHEN 102029 THEN 'United Arab Emirates'
        WHEN 102030 THEN 'Brazil'
        WHEN 102031 THEN 'Mexico'
        WHEN 102032 THEN 'Chile'
        WHEN 102033 THEN 'Argentina'
        WHEN 102034 THEN 'Egypt'
        WHEN 102035 THEN 'South Africa'
        WHEN 102036 THEN 'Kenya'
        WHEN 102037 THEN 'Tanzania'
        WHEN 102038 THEN 'Kazakhstan'
        ELSE `remark`
    END
WHERE `dict_type_id` = 102;
