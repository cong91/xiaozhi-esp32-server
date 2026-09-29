package xiaozhi.modules.model.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import cn.hutool.json.JSONObject;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.modules.model.catalog.CatalogItem;
import xiaozhi.modules.model.catalog.ProviderCatalogFetcher;
import xiaozhi.modules.model.dao.ModelConfigDao;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.CatalogService;
import xiaozhi.modules.timbre.dao.TimbreDao;
import xiaozhi.modules.timbre.entity.TimbreEntity;

/**
 * 模型目录服务的实现。
 * 按 config_json 的 "type" 值路由到对应的 {@link ProviderCatalogFetcher}；
 * 同步音色时直接操作 TimbreDao，只新增/更新，绝不删除已有行。
 *
 * @author xiaozhi
 * @since 2026-9-27
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    private static final String KIND_VOICES = "voices";
    private static final String KIND_MODELS = "models";

    private final ModelConfigDao modelConfigDao;
    private final TimbreDao timbreDao;
    private final RedisUtils redisUtils;
    private final Map<String, ProviderCatalogFetcher> fetchersByCode;

    public CatalogServiceImpl(List<ProviderCatalogFetcher> fetchers, ModelConfigDao modelConfigDao,
            TimbreDao timbreDao, RedisUtils redisUtils) {
        this.modelConfigDao = modelConfigDao;
        this.timbreDao = timbreDao;
        this.redisUtils = redisUtils;
        // 按 providerCode 建立路由表，key 对应 config_json 的 "type" 值
        Map<String, ProviderCatalogFetcher> map = new HashMap<>();
        for (ProviderCatalogFetcher fetcher : fetchers) {
            map.put(fetcher.providerCode(), fetcher);
        }
        this.fetchersByCode = map;
    }

    @Override
    public List<CatalogItem> getCatalog(String modelConfigId, String kind) {
        boolean wantVoices = KIND_VOICES.equals(kind);
        if (!wantVoices && !KIND_MODELS.equals(kind)) {
            throw new RenException("无效的目录类型：" + kind + "，仅支持 voices 或 models");
        }

        ModelConfigEntity entity = modelConfigDao.selectById(modelConfigId);
        if (entity == null) {
            throw new RenException("模型配置不存在");
        }

        Map<String, Object> config = toConfigMap(entity.getConfigJson());
        String providerCode = config.get("type") == null ? "" : String.valueOf(config.get("type"));
        ProviderCatalogFetcher fetcher = fetchersByCode.get(providerCode);
        if (fetcher == null) {
            throw new RenException("该提供商暂不支持自动获取此目录");
        }

        List<CatalogItem> items = wantVoices ? fetcher.fetchVoices(config) : fetcher.fetchModels(config);
        if (items == null) {
            throw new RenException("该提供商暂不支持自动获取此目录");
        }
        return items;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncVoices(String modelConfigId) {
        List<CatalogItem> items = getCatalog(modelConfigId, KIND_VOICES);

        int count = 0;
        for (int index = 0; index < items.size(); index++) {
            CatalogItem item = items.get(index);
            // 同一 (tts_model_id, tts_voice) 可能存在多行历史数据，selectList 取 sort 最小的一行更新，
            // 避免 selectOne 命中多行时抛 TooManyResultsException
            List<TimbreEntity> existingRows = timbreDao.selectList(new QueryWrapper<TimbreEntity>()
                    .eq("tts_model_id", modelConfigId)
                    .eq("tts_voice", item.getValue())
                    .orderByAsc("sort")
                    .orderByAsc("id"));
            if (!existingRows.isEmpty()) {
                // 更新已有行：刷新名称与备注；语言仅在原值为空时补填，不覆盖人工维护的语言
                TimbreEntity existing = existingRows.get(0);
                existing.setName(item.getLabel());
                existing.setRemark(item.getDescription());
                if (StringUtils.isBlank(existing.getLanguages())
                        && StringUtils.isNotBlank(item.getLanguages())) {
                    existing.setLanguages(item.getLanguages());
                }
                timbreDao.updateById(existing);
                // 名称缓存（timbre:name:，默认24小时过期）与详情缓存都要失效，
                // 否则 agent 列表最长 24 小时内仍显示旧名称
                redisUtils.delete(RedisKeys.getTimbreNameById(existing.getId()));
                redisUtils.delete(RedisKeys.getTimbreDetailsKey(existing.getId()));
            } else {
                TimbreEntity entity = new TimbreEntity();
                // 32位随机十六进制UUID作为主键
                entity.setId(UUID.randomUUID().toString().replace("-", ""));
                entity.setTtsModelId(modelConfigId);
                entity.setTtsVoice(item.getValue());
                entity.setName(item.getLabel());
                entity.setRemark(item.getDescription());
                entity.setLanguages(item.getLanguages());
                entity.setSort((long) index);
                timbreDao.insert(entity);
            }
            count++;
        }
        return count;
    }

    /**
     * 将 config_json 转为普通 Map（值为 null 时返回空 Map，交由后续校验报错）
     */
    private Map<String, Object> toConfigMap(JSONObject configJson) {
        Map<String, Object> config = new HashMap<>();
        if (configJson != null) {
            config.putAll(configJson);
        }
        return config;
    }
}
