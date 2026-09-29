package xiaozhi.modules.model.service;

import java.util.List;

import xiaozhi.modules.model.catalog.CatalogItem;

/**
 * 模型目录服务：按模型配置的凭据调用提供商官方接口，返回可选项目录并同步音色
 *
 * @author xiaozhi
 * @since 2026-9-27
 */
public interface CatalogService {

    /**
     * 获取模型目录
     *
     * @param modelConfigId 模型配置ID
     * @param kind          目录类型：voices（音色）或 models（模型）
     * @return 目录条目列表
     */
    List<CatalogItem> getCatalog(String modelConfigId, String kind);

    /**
     * 从提供商同步音色到 ai_tts_voice 表（仅新增与更新，不删除已有行）。
     * 新增行携带目录推导的语言；已有行刷新名称与备注，语言仅在原值为空时补填，
     * 不覆盖人工维护的语言与既有语言声明。
     *
     * @param modelConfigId 模型配置ID
     * @return 新增与更新的音色总条数
     */
    int syncVoices(String modelConfigId);
}
