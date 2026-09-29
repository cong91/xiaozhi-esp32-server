package xiaozhi.modules.model.catalog;

import java.util.List;
import java.util.Map;

/**
 * 提供商目录拉取器接口。
 * 每个支持的提供商实现一个类（Spring @Component），通过 {@link #providerCode()} 与
 * 模型配置 config_json 中的 "type" 值匹配；新增提供商只需新增一个实现类，无需注册表。
 *
 * @author xiaozhi
 * @since 2026-9-27
 */
public interface ProviderCatalogFetcher {

    /**
     * 提供商编码，对应模型配置 config_json 中的 "type" 值
     */
    String providerCode();

    /**
     * 拉取可用的模型列表
     *
     * @param config 模型配置的 config_json（含密钥等凭据）
     * @return 目录条目列表；不支持时返回 null
     */
    default List<CatalogItem> fetchModels(Map<String, Object> config) {
        return null;
    }

    /**
     * 拉取可用的音色列表
     *
     * @param config 模型配置的 config_json（含密钥等凭据）
     * @return 目录条目列表；不支持时返回 null
     */
    default List<CatalogItem> fetchVoices(Map<String, Object> config) {
        return null;
    }
}
