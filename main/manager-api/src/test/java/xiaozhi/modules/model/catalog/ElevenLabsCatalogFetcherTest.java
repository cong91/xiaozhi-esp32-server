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

class ElevenLabsCatalogFetcherTest {

    private final AtomicReference<String> capturedUrl = new AtomicReference<>();
    private final AtomicReference<String> capturedApiKey = new AtomicReference<>();

    private ElevenLabsCatalogFetcher fetcherReturning(String rawResponse) {
        return new ElevenLabsCatalogFetcher() {
            @Override
            protected String requestCatalog(String url, String apiKey) {
                capturedUrl.set(url);
                capturedApiKey.set(apiKey);
                return rawResponse;
            }
        };
    }

    @Test
    void fetchVoicesMapsVoiceEntriesOnHappyPath() {
        ElevenLabsCatalogFetcher fetcher = fetcherReturning("""
                {"voices":[
                    {"voice_id":"21m00Tcm4TlvDq8ikWAM","name":"Rachel",
                     "description":"Calm American woman","labels":{"language":"en","gender":"female"}},
                    {"voice_id":"pNInz6obpgDQGcFmaJgB","name":"Adam"}
                ]}""");

        List<CatalogItem> items = fetcher.fetchVoices(Map.<String, Object>of("api_key", "key-123"));

        assertEquals(2, items.size());
        assertEquals("21m00Tcm4TlvDq8ikWAM", items.get(0).getValue());
        assertEquals("Rachel", items.get(0).getLabel());
        assertEquals("Calm American woman", items.get(0).getDescription());
        assertEquals("en", items.get(0).getLanguages());
        assertEquals("pNInz6obpgDQGcFmaJgB", items.get(1).getValue());
        assertEquals("Adam", items.get(1).getLabel());
        assertNull(items.get(1).getDescription());
        assertNull(items.get(1).getLanguages());

        // 未配置 base_url 时使用官方默认域名，凭据通过 xi-api-key 请求头传递
        assertEquals("https://api.elevenlabs.io/v1/voices", capturedUrl.get());
        assertEquals("key-123", capturedApiKey.get());
    }

    @Test
    void fetchVoicesFallsBackToVoiceIdWhenNameMissing() {
        ElevenLabsCatalogFetcher fetcher = fetcherReturning(
                "{\"voices\":[{\"voice_id\":\"voice-abc\"}]}");

        List<CatalogItem> items = fetcher.fetchVoices(Map.<String, Object>of("api_key", "key-123"));

        assertEquals(1, items.size());
        assertEquals("voice-abc", items.get(0).getValue());
        assertEquals("voice-abc", items.get(0).getLabel());
        assertNull(items.get(0).getLanguages());
    }

    @Test
    void fetchVoicesResolvesBaseUrlFromSpeechToTextConfig() {
        ElevenLabsCatalogFetcher fetcher = fetcherReturning("{\"voices\":[]}");

        fetcher.fetchVoices(Map.<String, Object>of("api_key", "key-123",
                "base_url", "https://api.elevenlabs.io/v1/speech-to-text"));

        assertEquals("https://api.elevenlabs.io/v1/voices", capturedUrl.get());
    }

    @Test
    void fetchVoicesResolvesBaseUrlFromTtsApiUrlConfig() {
        ElevenLabsCatalogFetcher fetcher = fetcherReturning("{\"voices\":[]}");

        fetcher.fetchVoices(Map.<String, Object>of("api_key", "key-123",
                "api_url", "https://api.elevenlabs.io/v1/text-to-speech"));

        assertEquals("https://api.elevenlabs.io/v1/voices", capturedUrl.get());
    }

    @Test
    void fetchModelsParsesBareArrayResponse() {
        ElevenLabsCatalogFetcher fetcher = fetcherReturning("""
                [
                    {"model_id":"eleven_flash_v2_5","name":"Eleven Flash v2.5",
                     "description":"Fast model supporting 32 languages"},
                    {"model_id":"scribe_v2","name":"Scribe v2"}
                ]""");

        List<CatalogItem> items = fetcher.fetchModels(Map.<String, Object>of("api_key", "key-123"));

        assertEquals(2, items.size());
        assertEquals("eleven_flash_v2_5", items.get(0).getValue());
        assertEquals("Eleven Flash v2.5", items.get(0).getLabel());
        assertEquals("Fast model supporting 32 languages", items.get(0).getDescription());
        assertEquals("scribe_v2", items.get(1).getValue());
        assertEquals("Scribe v2", items.get(1).getLabel());

        assertEquals("https://api.elevenlabs.io/v1/models", capturedUrl.get());
    }

    @Test
    void requireSuccessThrowsWithStatusAndTruncatedBody() {
        RenException ex = assertThrows(RenException.class,
                () -> ElevenLabsCatalogFetcher.requireSuccess(401, "{\"detail\":{\"status\":\"invalid_api_key\"}}"));

        assertTrue(ex.getMessage().contains("401"));
        assertTrue(ex.getMessage().contains("invalid_api_key"));
    }
}
