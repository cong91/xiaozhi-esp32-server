package xiaozhi.modules.model.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import xiaozhi.common.exception.RenException;

/**
 * ElevenLabs 目录拉取器。
 * 音色列表来自官方 GET /v1/voices，模型列表来自 GET /v1/models，
 * 凭据取自模型配置的 config_json，通过 xi-api-key 请求头传递。
 * 音色语言从 labels.language 读取（共享音色库的音色才带该字段）；
 * 官方预设音色本身是多语言音色，无该字段时不声明语言。
 *
 * @author xiaozhi
 * @since 2026-9-29
 */
@Component
public class ElevenLabsCatalogFetcher implements ProviderCatalogFetcher {

    private static final String DEFAULT_BASE_URL = "https://api.elevenlabs.io";

    /**
     * config_json 里若存的是具体接口地址（TTS 存 api_url、ASR 存 base_url），
     * 去掉末尾的接口路径得到 API 根地址
     */
    private static final List<String> API_PATH_SUFFIXES = List.of("/v1/text-to-speech", "/v1/speech-to-text");

    private static final int TIMEOUT_MILLIS = 15000;

    @Override
    public String providerCode() {
        return "elevenlabs";
    }

    @Override
    public List<CatalogItem> fetchModels(Map<String, Object> config) {
        String raw = requestCatalog(resolveBaseUrl(config) + "/v1/models", str(config.get("api_key")));
        return parseModelCatalog(raw);
    }

    @Override
    public List<CatalogItem> fetchVoices(Map<String, Object> config) {
        String raw = requestCatalog(resolveBaseUrl(config) + "/v1/voices", str(config.get("api_key")));
        return parseVoiceCatalog(raw);
    }

    /**
     * 去掉 base_url（ASR）/ api_url（TTS）末尾的接口路径与斜杠得到 API 根地址；
     * 未配置时使用官方默认域名
     */
    private String resolveBaseUrl(Map<String, Object> config) {
        String baseUrl = StringUtils.defaultIfBlank(str(config.get("base_url")), str(config.get("api_url")));
        if (StringUtils.isBlank(baseUrl)) {
            return DEFAULT_BASE_URL;
        }
        baseUrl = baseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        for (String suffix : API_PATH_SUFFIXES) {
            if (baseUrl.endsWith(suffix)) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - suffix.length());
                break;
            }
        }
        return baseUrl;
    }

    /**
     * 解析 GET /v1/voices 响应：映射 voices 数组，
     * name 缺失时回退为 voice_id，语言取 labels.language
     */
    private List<CatalogItem> parseVoiceCatalog(String raw) {
        JSONObject json;
        try {
            json = JSONUtil.parseObj(raw);
        } catch (Exception e) {
            throw new RenException("解析ElevenLabs音色列表响应失败", e);
        }

        JSONArray voices = json.getJSONArray("voices");
        List<CatalogItem> items = new ArrayList<>();
        if (voices == null) {
            return items;
        }
        for (Object obj : voices) {
            JSONObject voice = (JSONObject) obj;
            CatalogItem item = new CatalogItem();
            String voiceId = voice.getStr("voice_id");
            item.setValue(voiceId);
            item.setLabel(StringUtils.defaultIfBlank(voice.getStr("name"), voiceId));
            item.setDescription(voice.getStr("description"));
            item.setLanguages(voiceLanguage(voice));
            items.add(item);
        }
        return items;
    }

    /**
     * 语言取 labels.language（如 "vietnamese"）；预设音色无该字段时返回 null，
     * 由前端按未声明语言处理
     */
    private String voiceLanguage(JSONObject voice) {
        JSONObject labels = voice.getJSONObject("labels");
        if (labels == null) {
            return null;
        }
        String language = labels.getStr("language");
        return StringUtils.isBlank(language) ? null : language;
    }

    /**
     * 解析 GET /v1/models 响应：响应体是裸数组，逐项映射 model_id / name / description。
     * 不按 can_do_text_to_speech 过滤，同一 type 也服务于 ASR（scribe 系列）配置
     */
    private List<CatalogItem> parseModelCatalog(String raw) {
        JSONArray models;
        try {
            models = JSONUtil.parseArray(raw);
        } catch (Exception e) {
            throw new RenException("解析ElevenLabs模型列表响应失败", e);
        }

        List<CatalogItem> items = new ArrayList<>();
        for (Object obj : models) {
            JSONObject model = (JSONObject) obj;
            CatalogItem item = new CatalogItem();
            String modelId = model.getStr("model_id");
            item.setValue(modelId);
            item.setLabel(StringUtils.defaultIfBlank(model.getStr("name"), modelId));
            item.setDescription(model.getStr("description"));
            items.add(item);
        }
        return items;
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 校验响应状态：非 2xx 视为提供商返回错误（key 错误会在此暴露），
     * 不能把错误页静默解析成空目录。响应体截断到 200 字符避免刷屏。
     */
    static void requireSuccess(int status, String body) {
        if (status < 200 || status >= 300) {
            throw new RenException("请求ElevenLabs目录失败，HTTP状态码 " + status + "："
                    + StringUtils.abbreviate(StringUtils.trimToEmpty(body), 200));
        }
    }

    /**
     * 发起 GET 请求，返回原始响应体。独立成 protected 方法便于单元测试桩替换。
     */
    protected String requestCatalog(String url, String apiKey) {
        try (HttpResponse response = HttpRequest.get(url)
                .header("xi-api-key", apiKey)
                .timeout(TIMEOUT_MILLIS)
                .execute()) {
            String body = response.body();
            requireSuccess(response.getStatus(), body);
            return body;
        } catch (RenException e) {
            throw e;
        } catch (Exception e) {
            throw new RenException("请求ElevenLabs目录失败", e);
        }
    }
}
