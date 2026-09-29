package xiaozhi.modules.model.catalog;

import lombok.Data;

/**
 * 模型目录条目（从提供商官方接口拉取的选项，如音色列表、模型列表）
 *
 * @author xiaozhi
 * @since 2026-9-27
 */
@Data
public class CatalogItem {

    /**
     * 选项编码（如音色ID、模型ID），写回配置时使用的值
     */
    private String value;

    /**
     * 展示名称
     */
    private String label;

    /**
     * 补充说明（可为空）
     */
    private String description;

    /**
     * 音色支持的语言（多个语言以逗号分隔，如 "Vietnamese, English"）；无法判定时为空，
     * 空值由前端按“未声明语言”处理（不参与语言过滤）
     */
    private String languages;
}
