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
 * OpenAI 兼容接口的目录拉取器。
 * 模型列表来自官方 GET /models 接口；音色列表为 OpenAI TTS 的六个固定内置音色，无需网络请求。
 *
 * @author xiaozhi
 * @since 2026-9-27
 */
@Component
public class OpenAiCompatibleCatalogFetcher implements ProviderCatalogFetcher {

    /**
     * config_json 的 base_url 若填写的是具体接口地址，去掉末尾的接口路径得到 API 根地址
     */
    private static final List<String> API_PATH_SUFFIXES = List.of("/audio/transcriptions", "/chat/completions",
            "/embeddings");
    private static final int TIMEOUT_MILLIS = 15000;

    /**
     * OpenAI TTS 固定内置音色
     */
    private static final List<String> OPENAI_TTS_VOICES = List.of("alloy", "echo", "fable", "onyx", "nova", "shimmer");

    @Override
    public String providerCode() {
        return "openai";
    }

    @Override
    public List<CatalogItem> fetchModels(Map<String, Object> config) {
        String baseUrl = resolveBaseUrl(config.get("base_url"));
        String apiKey = str(config.get("api_key"));
        String raw = requestModelList(baseUrl + "/models", apiKey);
        return parseModelList(raw);
    }

    @Override
    public List<CatalogItem> fetchVoices(Map<String, Object> config) {
        List<CatalogItem> items = new ArrayList<>();
        for (String voice : OPENAI_TTS_VOICES) {
            CatalogItem item = new CatalogItem();
            item.setValue(voice);
            item.setLabel(voice);
            items.add(item);
        }
        return items;
    }

    /**
     * 去掉 base_url 末尾的具体接口路径与斜杠，得到 API 根地址
     */
    private String resolveBaseUrl(Object baseUrlObj) {
        String baseUrl = str(baseUrlObj);
        if (StringUtils.isBlank(baseUrl)) {
            throw new RenException("配置缺少base_url，无法获取模型列表");
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
     * 解析 GET /models 响应：映射 data 数组，条目 id 同时作为 value 与 label
     */
    private List<CatalogItem> parseModelList(String raw) {
        JSONObject json;
        try {
            json = JSONUtil.parseObj(raw);
        } catch (Exception e) {
            throw new RenException("解析模型列表响应失败", e);
        }

        // 个别网关会以 200 状态返回错误体，同样不能静默解析成空列表
        Object error = json.get("error");
        if (error != null) {
            String detail;
            if (error instanceof JSONObject err) {
                detail = StringUtils.defaultIfBlank(err.getStr("message"), err.toString());
            } else {
                detail = String.valueOf(error);
            }
            throw new RenException("模型列表接口返回错误：" + detail);
        }

        JSONArray data = json.getJSONArray("data");
        List<CatalogItem> items = new ArrayList<>();
        if (data == null) {
            return items;
        }
        for (Object obj : data) {
            JSONObject model = (JSONObject) obj;
            CatalogItem item = new CatalogItem();
            String id = model.getStr("id");
            item.setValue(id);
            item.setLabel(id);
            items.add(item);
        }
        return items;
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 校验 GET /models 响应状态：非 2xx 视为提供商返回错误（key 或 base_url 错误会在此暴露），
     * 不能把错误页静默解析成空目录。响应体截断到 200 字符避免刷屏。
     */
    static void requireSuccess(int status, String body) {
        if (status < 200 || status >= 300) {
            throw new RenException("请求模型列表失败，HTTP状态码 " + status + "："
                    + StringUtils.abbreviate(StringUtils.trimToEmpty(body), 200));
        }
    }

    /**
     * 发起 HTTP 请求，返回原始响应体。独立成 protected 方法便于单元测试桩替换。
     */
    protected String requestModelList(String url, String apiKey) {
        try (HttpResponse response = HttpRequest.get(url)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .timeout(TIMEOUT_MILLIS)
                .execute()) {
            String body = response.body();
            requireSuccess(response.getStatus(), body);
            return body;
        } catch (RenException e) {
            throw e;
        } catch (Exception e) {
            throw new RenException("请求模型列表失败", e);
        }
    }
}
