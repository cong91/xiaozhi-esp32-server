package xiaozhi.modules.model.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import xiaozhi.common.exception.RenException;

/**
 * MiniMax 音色目录拉取器。
 * 调用官方 /v1/get_voice 接口获取系统音色列表，凭据取自模型配置的 config_json。
 * 语言从 voice_id 前缀推导（如 "Vietnamese_kindhearted_girl" → "Vietnamese"）。
 *
 * @author xiaozhi
 * @since 2026-9-27
 */
@Component
public class MinimaxCatalogFetcher implements ProviderCatalogFetcher {

    private static final String DEFAULT_HOST = "api.minimaxi.com";
    private static final int TIMEOUT_MILLIS = 15000;

    /**
     * voice_id 语言前缀到展示语言的允许列表（按 2026-09 国际站音色目录实测整理，
     * 键一律小写做忽略大小写匹配）。不在表中的前缀视为非语言前缀返回 null，
     * 避免把风格前缀（如未来的 Wizard_）当成语言污染语言下拉框。
     */
    private static final Map<String, String> LANGUAGE_PREFIX_MAP = Map.ofEntries(
            Map.entry("chinese (mandarin)", "Mandarin"),
            // 这两个是 Mandarin 的风格音色（官方描述为 Standard Mandarin），前缀非语言
            Map.entry("arrogant", "Mandarin"),
            Map.entry("robot", "Mandarin"),
            Map.entry("english", "English"),
            Map.entry("japanese", "Japanese"),
            Map.entry("cantonese", "Cantonese"),
            Map.entry("korean", "Korean"),
            Map.entry("spanish", "Spanish"),
            Map.entry("portuguese", "Portuguese"),
            Map.entry("french", "French"),
            Map.entry("indonesian", "Indonesian"),
            Map.entry("german", "German"),
            Map.entry("russian", "Russian"),
            Map.entry("italian", "Italian"),
            Map.entry("dutch", "Dutch"),
            Map.entry("vietnamese", "Vietnamese"),
            Map.entry("arabic", "Arabic"),
            Map.entry("turkish", "Turkish"),
            Map.entry("ukrainian", "Ukrainian"),
            Map.entry("thai", "Thai"),
            Map.entry("polish", "Polish"),
            Map.entry("romanian", "Romanian"),
            Map.entry("greek", "Greek"),
            Map.entry("czech", "Czech"),
            Map.entry("finnish", "Finnish"),
            Map.entry("hindi", "Hindi"));

    @Override
    public String providerCode() {
        return "minimax_httpstream";
    }

    @Override
    public List<CatalogItem> fetchVoices(Map<String, Object> config) {
        String host = StringUtils.defaultIfBlank(str(config.get("host")), DEFAULT_HOST);
        String apiKey = str(config.get("api_key"));
        String url = "https://" + host + "/v1/get_voice";
        String raw = requestVoiceCatalog(url, apiKey, "{\"voice_type\":\"system\"}");
        return parseVoiceCatalog(raw);
    }

    /**
     * 从 voice_id 首个下划线前的语言前缀推导语言（"Vietnamese_x" → "Vietnamese"，
     * "Chinese (Mandarin)_x" → "Mandarin"）；无下划线或前缀不在允许列表时返回 null，
     * 由前端按未声明语言处理（不参与语言过滤）。
     */
    static String deriveLanguages(String voiceId) {
        if (voiceId == null) {
            return null;
        }
        int underscore = voiceId.indexOf('_');
        if (underscore <= 0) {
            return null;
        }
        return LANGUAGE_PREFIX_MAP.get(voiceId.substring(0, underscore).toLowerCase());
    }

    /**
     * 解析 /v1/get_voice 响应：校验 base_resp.status_code 为 0，映射 system_voice 数组
     */
    private List<CatalogItem> parseVoiceCatalog(String raw) {
        JSONObject json;
        try {
            json = JSONUtil.parseObj(raw);
        } catch (Exception e) {
            throw new RenException("解析MiniMax音色列表响应失败", e);
        }

        JSONObject baseResp = json.getJSONObject("base_resp");
        int statusCode = baseResp == null ? -1 : baseResp.getInt("status_code", -1);
        if (statusCode != 0) {
            String statusMsg = baseResp == null ? "响应缺少base_resp字段"
                    : StringUtils.defaultIfBlank(baseResp.getStr("status_msg"), "未知错误");
            throw new RenException("MiniMax接口返回错误，状态码 " + statusCode + "：" + statusMsg);
        }

        JSONArray systemVoice = json.getJSONArray("system_voice");
        List<CatalogItem> items = new ArrayList<>();
        if (systemVoice == null) {
            return items;
        }
        for (Object obj : systemVoice) {
            JSONObject voice = (JSONObject) obj;
            CatalogItem item = new CatalogItem();
            String voiceId = voice.getStr("voice_id");
            item.setValue(voiceId);
            // voice_name 缺失时回退为 voice_id
            item.setDescription(firstDescription(voice));
            item.setLabel(StringUtils.defaultIfBlank(voice.getStr("voice_name"), voiceId));
            item.setLanguages(deriveLanguages(voiceId));
            items.add(item);
        }
        return items;
    }

    /**
     * description 为数组时取第一个元素
     */
    private String firstDescription(JSONObject voice) {
        JSONArray description = voice.getJSONArray("description");
        if (description == null || description.isEmpty()) {
            return null;
        }
        return description.getStr(0);
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 发起 HTTP 请求，返回原始响应体。独立成 protected 方法便于单元测试桩替换。
     */
    protected String requestVoiceCatalog(String url, String apiKey, String requestBody) {
        try (HttpResponse response = HttpRequest.post(url)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header(Header.CONTENT_TYPE, "application/json")
                .body(requestBody)
                .timeout(TIMEOUT_MILLIS)
                .execute()) {
            return response.body();
        } catch (RenException e) {
            throw e;
        } catch (Exception e) {
            throw new RenException("请求MiniMax音色列表失败", e);
        }
    }
}
