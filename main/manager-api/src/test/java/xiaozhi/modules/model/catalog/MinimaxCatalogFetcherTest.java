package xiaozhi.modules.model.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import xiaozhi.common.exception.RenException;

class MinimaxCatalogFetcherTest {

    private final AtomicReference<String> capturedUrl = new AtomicReference<>();
    private final AtomicReference<String> capturedApiKey = new AtomicReference<>();
    private final AtomicReference<String> capturedBody = new AtomicReference<>();

    private MinimaxCatalogFetcher fetcherReturning(String rawResponse) {
        return new MinimaxCatalogFetcher() {
            @Override
            protected String requestVoiceCatalog(String url, String apiKey, String requestBody) {
                capturedUrl.set(url);
                capturedApiKey.set(apiKey);
                capturedBody.set(requestBody);
                return rawResponse;
            }
        };
    }

    @Test
    void fetchVoicesMapsSystemVoiceEntriesOnHappyPath() {
        MinimaxCatalogFetcher fetcher = fetcherReturning("""
                {"base_resp":{"status_code":0,"status_msg":"success"},
                 "system_voice":[
                    {"voice_id":"male-qn-qingse","voice_name":"青涩青年音色","description":["年轻男声","普通话"]},
                    {"voice_id":"female-shaonv","voice_name":"少女音色"}
                 ]}""");

        List<CatalogItem> items = fetcher.fetchVoices(Map.<String, Object>of("api_key", "key-123"));

        assertEquals(2, items.size());
        assertEquals("male-qn-qingse", items.get(0).getValue());
        assertEquals("青涩青年音色", items.get(0).getLabel());
        assertEquals("年轻男声", items.get(0).getDescription());
        assertEquals("female-shaonv", items.get(1).getValue());
        assertEquals("少女音色", items.get(1).getLabel());
        assertNull(items.get(1).getDescription());

        // 未配置 host 时使用默认域名，请求头带 Bearer 凭据，请求体固定为系统音色
        assertEquals("https://api.minimaxi.com/v1/get_voice", capturedUrl.get());
        assertEquals("key-123", capturedApiKey.get());
        assertEquals("{\"voice_type\":\"system\"}", capturedBody.get());
    }

    @Test
    void fetchVoicesThrowsWithStatusMsgWhenBaseRespStatusIsNotZero() {
        MinimaxCatalogFetcher fetcher = fetcherReturning(
                "{\"base_resp\":{\"status_code\":1004,\"status_msg\":\"invalid api key\"}}");

        RenException ex = assertThrows(RenException.class,
                () -> fetcher.fetchVoices(Map.<String, Object>of("api_key", "bad-key")));

        assertTrue(ex.getMessage().contains("1004"));
        assertTrue(ex.getMessage().contains("invalid api key"));
    }

    @Test
    void fetchVoicesFallsBackToVoiceIdWhenVoiceNameMissing() {
        MinimaxCatalogFetcher fetcher = fetcherReturning(
                "{\"base_resp\":{\"status_code\":0},\"system_voice\":[{\"voice_id\":\"system-boy\"}]}");

        List<CatalogItem> items = fetcher.fetchVoices(Map.<String, Object>of("api_key", "key-123"));

        assertEquals(1, items.size());
        assertEquals("system-boy", items.get(0).getValue());
        assertEquals("system-boy", items.get(0).getLabel());
        assertNull(items.get(0).getDescription());
    }

    @Test
    void fetchVoicesUsesConfiguredHostWhenPresent() {
        MinimaxCatalogFetcher fetcher = fetcherReturning(
                "{\"base_resp\":{\"status_code\":0},\"system_voice\":[]}");

        fetcher.fetchVoices(Map.<String, Object>of("api_key", "key-123", "host", "api.minimax.com"));

        assertEquals("https://api.minimax.com/v1/get_voice", capturedUrl.get());
    }

    @Test
    void fetchVoicesFallsBackToDefaultHostWhenConfiguredHostIsBlank() {
        MinimaxCatalogFetcher fetcher = fetcherReturning(
                "{\"base_resp\":{\"status_code\":0},\"system_voice\":[]}");

        fetcher.fetchVoices(Map.<String, Object>of("api_key", "key-123", "host", ""));

        assertEquals("https://api.minimaxi.com/v1/get_voice", capturedUrl.get());
    }

    @Test
    void fetchVoicesDerivesLanguagesFromVoiceIdPrefix() {
        MinimaxCatalogFetcher fetcher = fetcherReturning("""
                {"base_resp":{"status_code":0},"system_voice":[
                    {"voice_id":"Vietnamese_kindhearted_girl","voice_name":"Kind-hearted girl"},
                    {"voice_id":"Chinese (Mandarin)_Reliable_Executive","voice_name":"Reliable Executive"},
                    {"voice_id":"Cantonese_ProfessionalHost（F)","voice_name":"Professional Female Host"},
                    {"voice_id":"greek_male_1a_v1","voice_name":"Thoughtful Mentor"},
                    {"voice_id":"Arrogant_Miss","voice_name":"Arrogant Miss"},
                    {"voice_id":"female-shaonv","voice_name":"Shaonv"}
                ]}""");

        List<CatalogItem> items = fetcher.fetchVoices(Map.<String, Object>of("api_key", "key-123"));

        assertEquals("Vietnamese", items.get(0).getLanguages());
        // 带括号与空格的前缀映射为仓库统一的语言名
        assertEquals("Mandarin", items.get(1).getLanguages());
        assertEquals("Cantonese", items.get(2).getLanguages());
        // 小写前缀同样能匹配
        assertEquals("Greek", items.get(3).getLanguages());
        // 风格前缀按官方描述归入 Mandarin；旧版无前缀音色不声明语言
        assertEquals("Mandarin", items.get(4).getLanguages());
        assertNull(items.get(5).getLanguages());
    }

    @Test
    void deriveLanguagesReturnsNullForUnknownOrMissingPrefix() {
        assertNull(MinimaxCatalogFetcher.deriveLanguages("female-shaonv"));
        assertNull(MinimaxCatalogFetcher.deriveLanguages(null));
        assertNull(MinimaxCatalogFetcher.deriveLanguages("_leading"));
        assertNull(MinimaxCatalogFetcher.deriveLanguages("Wizard_style_voice"));
    }
}
