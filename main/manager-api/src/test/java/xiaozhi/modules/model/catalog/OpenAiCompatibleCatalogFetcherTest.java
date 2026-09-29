package xiaozhi.modules.model.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import xiaozhi.common.exception.RenException;

class OpenAiCompatibleCatalogFetcherTest {

    private final AtomicReference<String> capturedUrl = new AtomicReference<>();
    private final AtomicReference<String> capturedApiKey = new AtomicReference<>();

    private OpenAiCompatibleCatalogFetcher fetcherReturning(String rawResponse) {
        return new OpenAiCompatibleCatalogFetcher() {
            @Override
            protected String requestModelList(String url, String apiKey) {
                capturedUrl.set(url);
                capturedApiKey.set(apiKey);
                return rawResponse;
            }
        };
    }

    @Test
    void fetchModelsStripsChatCompletionsSuffixFromBaseUrl() {
        OpenAiCompatibleCatalogFetcher fetcher = fetcherReturning(
                "{\"data\":[{\"id\":\"gpt-4o-mini\"},{\"id\":\"whisper-1\"}]}");

        List<CatalogItem> items = fetcher.fetchModels(Map.<String, Object>of("api_key", "sk-test",
                "base_url", "https://api.example.com/v1/chat/completions"));

        assertEquals(2, items.size());
        assertEquals("gpt-4o-mini", items.get(0).getValue());
        assertEquals("gpt-4o-mini", items.get(0).getLabel());
        assertNull(items.get(0).getDescription());
        assertEquals("whisper-1", items.get(1).getValue());
        assertEquals("https://api.example.com/v1/models", capturedUrl.get());
        assertEquals("sk-test", capturedApiKey.get());
    }

    @Test
    void fetchModelsStripsAudioTranscriptionsSuffixAndTrailingSlash() {
        OpenAiCompatibleCatalogFetcher fetcher = fetcherReturning("{\"data\":[]}");

        fetcher.fetchModels(Map.<String, Object>of("api_key", "sk-test",
                "base_url", "https://api.example.com/v1/audio/transcriptions/"));

        assertEquals("https://api.example.com/v1/models", capturedUrl.get());
    }

    @Test
    void fetchModelsKeepsPlainBaseUrlAndHandlesEmptyData() {
        OpenAiCompatibleCatalogFetcher fetcher = fetcherReturning("{\"data\":[]}");

        List<CatalogItem> items = fetcher.fetchModels(Map.<String, Object>of(
                "api_key", "sk-test", "base_url", "https://api.example.com/v1/"));

        assertEquals(0, items.size());
        assertEquals("https://api.example.com/v1/models", capturedUrl.get());
    }

    @Test
    void fetchModelsThrowsWhenBodyIsProviderErrorInsteadOfReturningEmptyList() {
        OpenAiCompatibleCatalogFetcher fetcher = fetcherReturning(
                "{\"error\":{\"message\":\"Invalid API key\",\"type\":\"invalid_request_error\"}}");

        RenException ex = assertThrows(RenException.class,
                () -> fetcher.fetchModels(Map.<String, Object>of("api_key", "sk-bad",
                        "base_url", "https://api.example.com/v1")));

        assertEquals("模型列表接口返回错误：Invalid API key", ex.getMessage());
    }

    @Test
    void requireSuccessThrowsWithStatusAndBodyOnNon2xx() {
        RenException ex = assertThrows(RenException.class,
                () -> OpenAiCompatibleCatalogFetcher.requireSuccess(401, "{\"error\":{\"message\":\"bad key\"}}"));

        assertEquals("请求模型列表失败，HTTP状态码 401：{\"error\":{\"message\":\"bad key\"}}", ex.getMessage());

        // 2xx 状态码不抛异常
        assertDoesNotThrow(() -> OpenAiCompatibleCatalogFetcher.requireSuccess(200, "{\"data\":[]}"));
    }

    @Test
    void fetchVoicesReturnsStaticOpenAiVoicesWithoutNetwork() {
        // 使用真实实现：fetchVoices 不发起任何网络请求
        OpenAiCompatibleCatalogFetcher fetcher = new OpenAiCompatibleCatalogFetcher();

        List<CatalogItem> items = fetcher.fetchVoices(Map.<String, Object>of("api_key", "sk-test"));

        assertEquals(6, items.size());
        for (int i = 0; i < items.size(); i++) {
            assertEquals(items.get(i).getValue(), items.get(i).getLabel());
            assertNull(items.get(i).getDescription());
        }
        assertEquals("alloy", items.get(0).getValue());
        assertEquals("shimmer", items.get(5).getValue());
    }
}
