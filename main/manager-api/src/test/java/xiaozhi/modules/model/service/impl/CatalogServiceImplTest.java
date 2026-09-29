package xiaozhi.modules.model.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import cn.hutool.json.JSONObject;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.modules.model.catalog.CatalogItem;
import xiaozhi.modules.model.catalog.ProviderCatalogFetcher;
import xiaozhi.modules.model.dao.ModelConfigDao;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.timbre.dao.TimbreDao;
import xiaozhi.modules.timbre.entity.TimbreEntity;

class CatalogServiceImplTest {

    private final ModelConfigDao modelConfigDao = mock(ModelConfigDao.class);
    private final TimbreDao timbreDao = mock(TimbreDao.class);
    private final RedisUtils redisUtils = mock(RedisUtils.class);

    private CatalogItem item(String value, String label, String description) {
        CatalogItem item = new CatalogItem();
        item.setValue(value);
        item.setLabel(label);
        item.setDescription(description);
        return item;
    }

    private CatalogItem item(String value, String label, String description, String languages) {
        CatalogItem item = item(value, label, description);
        item.setLanguages(languages);
        return item;
    }

    private ModelConfigEntity entityWithType(String type) {
        ModelConfigEntity entity = new ModelConfigEntity();
        entity.setId("model-config-1");
        entity.setModelType("TTS");
        entity.setConfigJson(new JSONObject().set("type", type).set("api_key", "sk-test"));
        return entity;
    }

    private CatalogServiceImpl serviceWith(ProviderCatalogFetcher... fetchers) {
        return new CatalogServiceImpl(List.of(fetchers), modelConfigDao, timbreDao, redisUtils);
    }

    @Test
    void getCatalogRoutesByConfigTypeValue() {
        ProviderCatalogFetcher minimaxFetcher = mock(ProviderCatalogFetcher.class);
        when(minimaxFetcher.providerCode()).thenReturn("minimax_httpstream");
        List<CatalogItem> voices = List.of(item("male-qn-qingse", "青涩青年音色", null));
        when(minimaxFetcher.fetchVoices(any())).thenReturn(voices);

        ProviderCatalogFetcher openaiFetcher = mock(ProviderCatalogFetcher.class);
        when(openaiFetcher.providerCode()).thenReturn("openai");

        CatalogServiceImpl service = serviceWith(openaiFetcher, minimaxFetcher);
        when(modelConfigDao.selectById("model-config-1")).thenReturn(entityWithType("minimax_httpstream"));

        List<CatalogItem> result = service.getCatalog("model-config-1", "voices");

        assertSame(voices, result);
        // 路由按 config_json 的 "type" 值，openai 拉取器不应被调用
        verify(openaiFetcher, never()).fetchVoices(any());
        verify(openaiFetcher, never()).fetchModels(any());
    }

    @Test
    void getCatalogRejectsUnknownKind() {
        CatalogServiceImpl service = serviceWith();

        RenException ex = assertThrows(RenException.class,
                () -> service.getCatalog("model-config-1", "bogus"));

        assertTrue(ex.getMessage().contains("voices"));
        verify(modelConfigDao, never()).selectById(any());
    }

    @Test
    void getCatalogRejectsMissingEntity() {
        CatalogServiceImpl service = serviceWith();
        when(modelConfigDao.selectById("missing")).thenReturn(null);

        RenException ex = assertThrows(RenException.class,
                () -> service.getCatalog("missing", "voices"));

        assertEquals("模型配置不存在", ex.getMessage());
    }

    @Test
    void getCatalogRejectsUnsupportedProviderType() {
        CatalogServiceImpl service = serviceWith();
        when(modelConfigDao.selectById("model-config-1")).thenReturn(entityWithType("no-such-provider"));

        RenException ex = assertThrows(RenException.class,
                () -> service.getCatalog("model-config-1", "voices"));

        assertEquals("该提供商暂不支持自动获取此目录", ex.getMessage());
    }

    @Test
    void getCatalogRejectsNullFetcherResult() {
        ProviderCatalogFetcher fetcher = mock(ProviderCatalogFetcher.class);
        when(fetcher.providerCode()).thenReturn("minimax_httpstream");
        when(fetcher.fetchVoices(any())).thenReturn(null);

        CatalogServiceImpl service = serviceWith(fetcher);
        when(modelConfigDao.selectById("model-config-1")).thenReturn(entityWithType("minimax_httpstream"));

        RenException ex = assertThrows(RenException.class,
                () -> service.getCatalog("model-config-1", "voices"));

        assertEquals("该提供商暂不支持自动获取此目录", ex.getMessage());
    }

    @Test
    void syncVoicesInsertsNewRowsWhenNothingMatches() {
        ProviderCatalogFetcher fetcher = mock(ProviderCatalogFetcher.class);
        when(fetcher.providerCode()).thenReturn("minimax_httpstream");
        when(fetcher.fetchVoices(any())).thenReturn(List.of(
                item("male-qn-qingse", "青涩青年音色", "年轻男声"),
                item("Vietnamese_kindhearted_girl", "Kind-hearted girl", "温暖女声", "Vietnamese"),
                item("Robot_Armor", "Robot Armor", null)));

        CatalogServiceImpl service = serviceWith(fetcher);
        when(modelConfigDao.selectById("model-config-1")).thenReturn(entityWithType("minimax_httpstream"));
        // selectList 默认返回空列表：数据库中无匹配行

        int count = service.syncVoices("model-config-1");

        assertEquals(3, count);
        ArgumentCaptor<TimbreEntity> captor = ArgumentCaptor.forClass(TimbreEntity.class);
        verify(timbreDao, never()).updateById(any(TimbreEntity.class));
        verify(timbreDao, times(3)).insert(captor.capture());
        List<TimbreEntity> inserted = captor.getAllValues();
        TimbreEntity first = inserted.get(0);
        assertEquals("model-config-1", first.getTtsModelId());
        assertEquals("male-qn-qingse", first.getTtsVoice());
        assertEquals("青涩青年音色", first.getName());
        assertEquals("年轻男声", first.getRemark());
        // 无语言前缀的目录条目落库为 NULL，由前端按未声明语言处理
        assertNull(first.getLanguages());
        assertEquals(0L, first.getSort());
        assertTrue(first.getId() != null && first.getId().matches("[0-9a-f]{32}"));
        // 目录条目自带的语言写入新行
        assertEquals("Vietnamese", inserted.get(1).getLanguages());
        assertNull(inserted.get(2).getLanguages());
        assertEquals(2L, inserted.get(2).getSort());
        // 从不删除已有行
        verify(timbreDao, never()).deleteById(any(String.class));
    }

    @Test
    void syncVoicesUpdatesExistingRowWithoutDeletingOrDuplicating() {
        ProviderCatalogFetcher fetcher = mock(ProviderCatalogFetcher.class);
        when(fetcher.providerCode()).thenReturn("minimax_httpstream");
        when(fetcher.fetchVoices(any()))
                .thenReturn(List.of(item("male-qn-qingse", "青涩青年（新）", "更新后的描述", "Mandarin")));

        TimbreEntity existing = new TimbreEntity();
        existing.setId("existing-1");
        existing.setTtsModelId("model-config-1");
        existing.setTtsVoice("male-qn-qingse");
        existing.setName("青涩青年音色");
        existing.setLanguages("中文");
        existing.setSort(3L);

        CatalogServiceImpl service = serviceWith(fetcher);
        when(modelConfigDao.selectById("model-config-1")).thenReturn(entityWithType("minimax_httpstream"));
        when(timbreDao.selectList(any())).thenReturn(List.of(existing));

        int count = service.syncVoices("model-config-1");

        assertEquals(1, count);
        verify(timbreDao).updateById(argThat((TimbreEntity entity) ->
                "existing-1".equals(entity.getId())
                        && "青涩青年（新）".equals(entity.getName())
                        && "更新后的描述".equals(entity.getRemark())
                        // 人工维护的语言不被目录覆盖
                        && "中文".equals(entity.getLanguages())
                        && entity.getSort() == 3L));
        verify(timbreDao, never()).insert(any(TimbreEntity.class));
        // 名称缓存与详情缓存都要失效
        verify(redisUtils).delete("timbre:name:existing-1");
        verify(redisUtils).delete("timbre:details:existing-1");
        verify(timbreDao, never()).deleteById(any(String.class));
    }

    @Test
    void syncVoicesFillsLanguagesOnlyWhenExistingValueIsBlank() {
        ProviderCatalogFetcher fetcher = mock(ProviderCatalogFetcher.class);
        when(fetcher.providerCode()).thenReturn("minimax_httpstream");
        when(fetcher.fetchVoices(any()))
                .thenReturn(List.of(item("male-qn-qingse", "青涩青年音色", null, "Mandarin")));

        TimbreEntity existing = new TimbreEntity();
        existing.setId("existing-1");
        existing.setTtsVoice("male-qn-qingse");
        existing.setName("青涩青年音色");
        existing.setLanguages(null);

        CatalogServiceImpl service = serviceWith(fetcher);
        when(modelConfigDao.selectById("model-config-1")).thenReturn(entityWithType("minimax_httpstream"));
        when(timbreDao.selectList(any())).thenReturn(List.of(existing));

        service.syncVoices("model-config-1");

        // 原语言为空时按目录补填
        verify(timbreDao).updateById(argThat((TimbreEntity entity) ->
                "Mandarin".equals(entity.getLanguages())));
    }

    @Test
    void syncVoicesUpdatesFirstRowWhenDuplicateVoiceRowsExist() {
        ProviderCatalogFetcher fetcher = mock(ProviderCatalogFetcher.class);
        when(fetcher.providerCode()).thenReturn("minimax_httpstream");
        when(fetcher.fetchVoices(any()))
                .thenReturn(List.of(item("female-shaonv", "Shaonv", null, "Mandarin")));

        TimbreEntity curated = new TimbreEntity();
        curated.setId("curated-1");
        curated.setTtsVoice("female-shaonv");
        curated.setName("Minimax Vietnamese Female - Shaonv");
        curated.setLanguages("Vietnamese");
        curated.setSort(1L);
        TimbreEntity seeded = new TimbreEntity();
        seeded.setId("seeded-2");
        seeded.setTtsVoice("female-shaonv");
        seeded.setName("Girl Voice");
        seeded.setLanguages("Mandarin");
        seeded.setSort(2L);

        CatalogServiceImpl service = serviceWith(fetcher);
        when(modelConfigDao.selectById("model-config-1")).thenReturn(entityWithType("minimax_httpstream"));
        // 与查询的 orderBy(sort, id) 一致：curated-1 排在前
        when(timbreDao.selectList(any())).thenReturn(List.of(curated, seeded));

        int count = service.syncVoices("model-config-1");

        // 不因多行匹配抛 TooManyResultsException；只更新 sort 最小的一行
        assertEquals(1, count);
        verify(timbreDao).updateById(argThat((TimbreEntity entity) ->
                "curated-1".equals(entity.getId())));
        verify(timbreDao, never()).updateById(argThat((TimbreEntity entity) ->
                "seeded-2".equals(entity.getId())));
        verify(timbreDao, never()).insert(any(TimbreEntity.class));
    }

    @Test
    void syncVoicesReturnsInsertPlusUpdateCountForMixedCatalog() {
        ProviderCatalogFetcher fetcher = mock(ProviderCatalogFetcher.class);
        when(fetcher.providerCode()).thenReturn("minimax_httpstream");
        when(fetcher.fetchVoices(any())).thenReturn(List.of(
                item("male-qn-qingse", "青涩青年音色", null),
                item("female-shaonv", "少女音色", null)));

        TimbreEntity existing = new TimbreEntity();
        existing.setId("existing-1");
        existing.setTtsVoice("male-qn-qingse");

        CatalogServiceImpl service = serviceWith(fetcher);
        when(modelConfigDao.selectById("model-config-1")).thenReturn(entityWithType("minimax_httpstream"));
        when(timbreDao.selectList(any())).thenReturn(List.of(existing)).thenReturn(List.of());

        int count = service.syncVoices("model-config-1");

        assertEquals(2, count);
        verify(timbreDao).updateById(any(TimbreEntity.class));
        verify(timbreDao).insert(any(TimbreEntity.class));
    }
}
