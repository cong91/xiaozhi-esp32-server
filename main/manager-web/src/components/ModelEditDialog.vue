<template>
  <CustomDialog
    :title="$t('modelConfigDialog.editModel')"
    :visible.sync="dialogVisible"
    width="57%"
    class="model-edit-dialog"
    :confirmLoading="saving"
    @confirm="handleSave"
    @close="handleClose"
    @open="handleOpen"
  >
    <div class="dialog-scroll-body">
    <div class="header-row">
      <div class="section-title">{{ $t("modelConfigDialog.modelInfo") }}</div>
      <div class="switch-group">
        <div class="switch-item">
          <span class="switch-label">{{ $t("modelConfigDialog.enable") }}</span>
          <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" class="custom-switch"></el-switch>
        </div>
        <div class="switch-item hidden">
          <span class="switch-label">{{ $t("modelConfigDialog.setDefault") }}</span>
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" class="custom-switch"></el-switch>
        </div>
      </div>
    </div>

    <div class="section-divider"></div>

    <el-form :model="form" ref="form" label-width="auto" label-position="left">
      <div class="form-row">
        <el-form-item :label="$t('modelConfigDialog.modelName')" prop="name" style="flex: 1">
          <el-input v-model="form.modelName" :placeholder="$t('modelConfigDialog.enterModelName')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('modelConfigDialog.modelCode')" prop="code" style="flex: 1">
          <el-input v-model="form.modelCode" :placeholder="$t('modelConfigDialog.enterModelCode')"></el-input>
        </el-form-item>
      </div>

      <div class="form-row">
        <el-form-item :label="$t('modelConfigDialog.supplier')" prop="supplier" style="flex: 1">
          <el-select v-model="form.configJson.type" :placeholder="$t('modelConfigDialog.selectSupplier')"
            style="width: 100%" @focus="loadProviders" filterable>
            <el-option v-for="item in providers" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('modelConfigDialog.sortOrder')" prop="sort" style="flex: 1">
          <el-input v-model.number="form.sort" type="number" :placeholder="$t('modelConfigDialog.enterSortOrder')"></el-input>
        </el-form-item>
      </div>

      <el-form-item :label="$t('modelConfigDialog.docLink')" prop="docUrl" style="margin-bottom: 27px">
        <el-input v-model="form.docLink" :placeholder="$t('modelConfigDialog.enterDocLink')"></el-input>
      </el-form-item>

      <el-form-item :label="$t('modelConfigDialog.remark')" prop="remark" class="prop-remark">
        <el-input v-model="form.remark" type="textarea" :rows="3" :autosize="{ minRows: 3, maxRows: 5 }" :placeholder="$t('modelConfigDialog.enterRemark')"></el-input>
      </el-form-item>
    </el-form>

    <teleport v-if="chunkedCallInfoFields.length">
      <div class="section-title">{{ $t("modelConfigDialog.callInfo") }}</div>
      <div class="section-divider"></div>
    </teleport>

    <el-form :model="form.configJson" ref="callInfoForm" label-width="auto" label-position="left">
      <template>
        <div v-for="(row, rowIndex) in chunkedCallInfoFields" :key="rowIndex" class="form-row">
          <el-form-item v-for="field in row" :key="field.prop" :label="field.label" :prop="field.prop"
            style="flex: 1">
            <template v-if="field.type === 'json-textarea'">
              <el-input v-model="fieldJsonMap[field.prop]" type="textarea" :rows="3"
                :placeholder="$t('modelConfigDialog.enterJsonExample')"
                @change="(val) => handleJsonChange(field.prop, val)" @focus="
                  isSensitiveField(field.prop)
                    ? handleJsonInputFocus(field.prop, fieldJsonMap[field.prop])
                    : undefined
                  " @blur="
                  isSensitiveField(field.prop)
                    ? handleJsonInputBlur(field.prop)
                    : undefined
                  "></el-input>
            </template>

            <div v-else-if="isSelectField(field)" class="select-field-row">
              <el-select v-model="form.configJson[field.prop]" class="select-field-control" filterable
                :disabled="isSelectDisabled(field)" :placeholder="selectPlaceholder(field)">
                <el-option v-for="opt in selectOptions(field)" :key="opt.value" :label="opt.label"
                  :value="opt.value" />
              </el-select>
              <el-button v-if="field.dynamic === 'voices' && form.id" class="sync-voices-btn" size="mini"
                icon="el-icon-refresh" :loading="syncingVoices" :title="$t('modelConfigDialog.syncVoices')"
                @click="handleSyncVoices(field)"></el-button>
            </div>

            <el-input v-else v-model="form.configJson[field.prop]" :placeholder="field.placeholder" :type="field.type"
              :show-password="field.type === 'password'" @focus="
                isSensitiveField(field.prop)
                  ? handleInputFocus(field.prop, form.configJson[field.prop])
                  : undefined
                " @blur="
                isSensitiveField(field.prop) ? handleInputBlur(field.prop) : undefined
                "></el-input>
          </el-form-item>
        </div>
      </template>
    </el-form>
    </div>
  </CustomDialog>
</template>

<script>
import CustomDialog from './CustomDialog.vue';
import Api from "@/apis/api";

export default {
  name: "ModelEditDialog",
  components: { CustomDialog },
  props: {
    visible: { type: Boolean, default: false },
    modelData: {
      type: Object,
      default: () => ({}),
      validator: (value) => typeof value === "object" && !Array.isArray(value),
    },
    modelType: { type: String, required: true },
  },
  data() {
    return {
      dialogVisible: this.visible,
      providers: [],
      providersLoaded: false,
      saving: false,
      allProvidersData: null,
      pendingProviderType: null,
      pendingModelData: null,
      dynamicCallInfoFields: [],
      fieldJsonMap: {}, // 用于存储JSON字段的字符串形式
      dynamicOptions: {}, // 动态目录选项，按字段prop存储（如音色、模型列表）
      dynamicLoadFailed: {}, // 动态目录加载失败的字段prop集合
      syncingVoices: false, // 音色目录同步进行中
      sensitive_keys: [
        "api_key",
        "personal_access_token",
        "access_token",
        "token",
        "secret",
        "access_key_secret",
        "secret_key",
      ],
      originalValues: {}, // 存储原始值，用于失焦时恢复
      form: {
        id: "",
        modelType: "",
        modelCode: "",
        modelName: "",
        isDefault: false,
        isEnabled: false,
        docLink: "",
        remark: "",
        sort: 0,
        configJson: {},
      },
    };
  },
  computed: {
    title() {
      return this.modelData.duplicateMode
        ? this.$t("modelConfigDialog.duplicateModel")
        : this.$t("modelConfigDialog.editModel");
    },
    chunkedCallInfoFields() {
      const chunkSize = 2;
      const result = [];
      for (let i = 0; i < this.dynamicCallInfoFields.length; i += chunkSize) {
        result.push(this.dynamicCallInfoFields.slice(i, i + chunkSize));
      }
      return result;
    },
  },
  watch: {
    modelType() {
      this.resetProviders();
      this.loadProviders();
    },
    visible(val) {
      this.dialogVisible = val;
    },
    dialogVisible(val) {
      this.$emit("update:visible", val);
    },
    "form.configJson.type"(newVal) {
      if (newVal && this.providersLoaded) {
        this.loadProviderFields(newVal);
      }
    },
  },
  methods: {
    handleOpen() {
      this.dynamicOptions = {};
      this.dynamicLoadFailed = {};
      this.loadProviders();
      if (this.modelData.id) {
        this.loadModelData();
      }
    },
    handleClose() {
      this.saving = false;
      // 处理关闭弹窗闪动问题
      setTimeout(() => {
        this.resetForm();
      }, 200)
    },
    resetForm() {
      this.form = {
        id: "",
        modelType: "",
        modelCode: "",
        modelName: "",
        isDefault: false,
        isEnabled: false,
        docLink: "",
        remark: "",
        sort: 0,
        configJson: {},
      };
      this.fieldJsonMap = {};
      this.dynamicOptions = {};
      this.dynamicLoadFailed = {};
      this.syncingVoices = false;
    },
    resetProviders() {
      this.providers = [];
      this.providersLoaded = false;
    },
    loadModelData() {
      if (this.modelData.id) {
        Api.model.getModelConfig(this.modelData.id, ({ data }) => {
          if (data.code === 0 && data.data) {
            let model = data.data;

            if (this.modelData.duplicateMode) {
              model.modelName =
                this.modelData.modelName + this.$t("modelConfigDialog.copySuffix");
              model.modelCode =
                this.modelData.modelCode + this.$t("modelConfigDialog.copySuffix");

              // 处理敏感字段
              if (model.configJson) {
                Object.keys(model.configJson).forEach((key) => {
                  if (this.isSensitiveField(key) && model.configJson[key]) {
                    const sensitiveName = this.getSensitiveFieldName(key);
                    model.configJson[key] = `你的${sensitiveName}`;
                  }
                });
              }
            }
            this.pendingProviderType = model.configJson.type;
            this.pendingModelData = model;

            if (this.providersLoaded) {
              this.loadProviderFields(model.configJson.type);
            } else {
              this.loadProviders();
            }
          }
        });
      }
    },
    handleSave() {
      this.saving = true; // 开始保存加载

      // 处理所有JSON字段
      Object.keys(this.fieldJsonMap).forEach((key) => {
        const parsed = this.validateJson(this.fieldJsonMap[key]);
        if (parsed !== null) {
          this.form.configJson[key] = parsed;
        }
      });

      const formData = {
        id: this.modelData.id,
        modelCode: this.form.modelCode,
        modelName: this.form.modelName,
        isDefault: this.form.isDefault ? 1 : 0,
        isEnabled: this.form.isEnabled ? 1 : 0,
        docLink: this.form.docLink,
        remark: this.form.remark,
        sort: this.form.sort || 0,
        configJson: { ...this.form.configJson },
      };

      this.$emit("save", {
        provideCode: this.form.configJson.type,
        formData,
        done: () => {
          this.saving = false; // 保存完成后回调
        },
      });

      // 如果父组件不处理done回调，3秒后自动关闭加载状态
      setTimeout(() => {
        this.saving = false;
      }, 3000);
    },
    loadProviders() {
      if (this.providersLoaded) return;

      Api.model.getModelProviders(this.modelType, (data) => {
        this.providers = data.map((item) => ({
          label: item.name,
          value: String(item.providerCode),
        }));
        this.providersLoaded = true;
        this.allProvidersData = data;

        if (this.pendingProviderType) {
          this.loadProviderFields(this.pendingProviderType);
        }
      });
    },
    loadProviderFields(providerCode) {
      if (this.allProvidersData) {
        const provider = this.allProvidersData.find(
          (p) => p.providerCode === providerCode
        );
        if (provider) {
          this.dynamicCallInfoFields = JSON.parse(provider.fields || "[]").map((f) => ({
            label: f.label,
            prop: f.key,
            type:
              f.type === "dict"
                ? "json-textarea"
                : f.type === "password"
                  ? "password"
                  : "text",
            placeholder: `请输入${f.key}`,
            options: f.options,
            dynamic: f.dynamic || null,
          }));

          // 字段集变更后旧目录选项失效，清空并按需重新加载
          this.dynamicOptions = {};
          this.dynamicLoadFailed = {};

          if (this.pendingModelData && this.pendingProviderType === providerCode) {
            this.processModelData(this.pendingModelData);
            this.pendingModelData = null;
            this.pendingProviderType = null;
          }

          this.loadDynamicCatalogs();
        }
      }
    },
    // 字段是否渲染为下拉框：携带静态options或dynamic目录
    isSelectField(field) {
      return Array.isArray(field.options) || !!field.dynamic;
    },
    // 下拉框选项：静态options直接使用，dynamic从后端目录读取
    selectOptions(field) {
      if (Array.isArray(field.options)) {
        return field.options;
      }
      if (field.dynamic) {
        return this.dynamicOptions[field.prop] || [];
      }
      return [];
    },
    // 动态下拉框禁用条件：新建模型无ID无法加载目录，或目录加载失败
    isSelectDisabled(field) {
      if (!field.dynamic) {
        return false;
      }
      return !this.form.id || !!this.dynamicLoadFailed[field.prop];
    },
    // 新建模型的动态下拉框提示先保存
    selectPlaceholder(field) {
      if (field.dynamic && !this.form.id) {
        return this.$t('modelConfigDialog.saveFirstToLoadOptions');
      }
      return field.placeholder;
    },
    loadDynamicCatalogs() {
      this.dynamicCallInfoFields.forEach((field) => {
        if (field.dynamic && this.form.id) {
          this.loadFieldCatalog(field);
        }
      });
    },
    loadFieldCatalog(field) {
      Api.model.getModelCatalog(
        this.form.id,
        field.dynamic,
        ({ data }) => {
          // 字段集已变更（切换供应器或重新打开），丢弃过期响应
          if (!this.dynamicCallInfoFields.includes(field)) {
            return;
          }
          if (data.code === 0 && Array.isArray(data.data)) {
            this.$set(this.dynamicOptions, field.prop, data.data);
            this.$delete(this.dynamicLoadFailed, field.prop);
          } else {
            this.$set(this.dynamicLoadFailed, field.prop, true);
            this.$message.error(this.$t('modelConfigDialog.catalogLoadFailed'));
          }
        },
        () => {
          if (!this.dynamicCallInfoFields.includes(field)) {
            return;
          }
          this.$set(this.dynamicLoadFailed, field.prop, true);
          this.$message.error(this.$t('modelConfigDialog.catalogLoadFailed'));
        }
      );
    },
    // 同步音色目录后重新加载该字段选项
    handleSyncVoices(field) {
      if (this.syncingVoices || !this.form.id) {
        return;
      }
      this.syncingVoices = true;
      Api.model.syncModelVoices(
        this.form.id,
        () => {
          this.syncingVoices = false;
          this.$message.success(this.$t('modelConfigDialog.syncVoicesSuccess'));
          this.loadFieldCatalog(field);
        },
        (err) => {
          this.syncingVoices = false;
          this.$message.error(
            (err && err.data && err.data.msg) || this.$t('modelConfigDialog.syncVoicesFailed')
          );
        }
      );
    },
    processModelData(model) {
      let configJson = model.configJson || {};
      this.dynamicCallInfoFields.forEach((field) => {
        if (!configJson.hasOwnProperty(field.prop)) {
          configJson[field.prop] = "";
        } else if (field.type === "json-textarea") {
          this.$set(
            this.fieldJsonMap,
            field.prop,
            this.formatJson(configJson[field.prop])
          );
          configJson[field.prop] = this.ensureObject(configJson[field.prop]);
        } else if (typeof configJson[field.prop] !== "string") {
          configJson[field.prop] = String(configJson[field.prop]);
        }
      });

      this.form = {
        id: model.id,
        modelType: model.modelType,
        modelCode: model.modelCode,
        modelName: model.modelName,
        isDefault: model.isDefault,
        isEnabled: model.isEnabled,
        docLink: model.docLink,
        remark: model.remark,
        sort: Number(model.sort) || 0,
        configJson: { ...configJson },
      };
    },
    handleJsonChange(field, value) {
      const parsed = this.validateJson(value);
      if (parsed !== null) {
        this.form.configJson[field] = parsed;
      }
    },
    validateJson(value) {
      try {
        const parsed = JSON.parse(value);
        if (typeof parsed === "object" && parsed !== null && !Array.isArray(parsed)) {
          return parsed;
        }
        this.$message.error({
          message: '必须输入字典格式（如 {"key":"value"}），保存则使用原数据',
          showClose: true,
        });
        return null;
      } catch (e) {
        this.$message.error({
          message: 'JSON格式错误（如 {"key":"value"}），保存则使用原数据',
          showClose: true,
        });
        return null;
      }
    },
    formatJson(obj) {
      try {
        return JSON.stringify(obj, null, 2);
      } catch {
        return "";
      }
    },
    ensureObject(value) {
      return typeof value === "object" ? value : {};
    },

    // 检测字段是否为敏感字段
    isSensitiveField(fieldName) {
      // 将字段名转换为小写进行比较
      const lowerFieldName = fieldName.toLowerCase();
      // 精确匹配keyMap中定义的7个敏感词
      return this.sensitive_keys.includes(lowerFieldName);
    },

    // 获取敏感字段对应的中文名称
    getSensitiveFieldName(fieldName) {
      const keyMap = {
        api_key: "API密钥",
        personal_access_token: "个人访问令牌",
        access_token: "访问令牌",
        token: "令牌",
        secret: "密钥",
        access_key_secret: "访问密钥",
        secret_key: "密钥",
      };

      for (const [key, value] of Object.entries(keyMap)) {
        if (fieldName.toLowerCase().includes(key)) {
          return value;
        }
      }
      return "敏感信息";
    },

    // 处理input聚焦事件
    handleInputFocus(field, value) {
      // 如果值包含星号，清空显示
      if (value && value.includes("*")) {
        // 存储原始值，用于失焦时恢复
        this.$set(this.originalValues, field, this.form.configJson[field]);
        this.$set(this.form.configJson, field, "");
      }
    },

    // 处理input失焦事件
    handleInputBlur(field) {
      // 检查是否为敏感字段
      if (this.isSensitiveField(field)) {
        // 如果值为空，恢复掩码值
        if (!this.form.configJson[field] || this.form.configJson[field].trim() === "") {
          // 如果有原始值，则恢复原始值；否则设置为掩码提示
          if (this.originalValues[field]) {
            this.$set(this.form.configJson, field, this.originalValues[field]);
          } else {
            const sensitiveName = this.getSensitiveFieldName(field);
            this.$set(this.form.configJson, field, `你的${sensitiveName}`);
          }
          // 清除临时存储的原始值
          this.$delete(this.originalValues, field);
        }
      }
    },

    // 处理JSON字段的聚焦事件
    handleJsonInputFocus(field, value) {
      if (value && value.includes("*")) {
        this.$set(this.fieldJsonMap, field, "");
      }
    },

    // 处理JSON字段的失焦事件
    handleJsonInputBlur(field) {
      // JSON字段不做特殊处理，因为它们通常不包含简单的敏感信息
    },
  },
};
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';

::v-deep .el-dialog {
  margin-top: 6vh !important;
}
::v-deep .el-dialog__body {
  max-height: 60vh;
  overflow-y: auto;
  @include scrollbar-style;
}
.model-edit-dialog {
  .header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .section-title {
    margin-bottom: 10px;
    font-size: 20px;
    font-weight: bold;
    color: #3d4566;
    text-align: left;
  }

  .switch-group {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  .switch-item {
    display: flex;
    align-items: center;

    &.hidden {
      display: none;
    }

    .switch-label {
      margin-right: 8px;
    }
  }

  .section-divider {
    height: 1px;
    background: #e9e9e9;
    margin-bottom: 16px;
  }

  .form-row {
    display: flex;
    gap: 20px;
    margin-bottom: 0;
  }

  .select-field-row {
    display: flex;
    align-items: center;
    gap: 8px;

    .select-field-control {
      flex: 1;
    }

    .sync-voices-btn {
      flex-shrink: 0;
      padding: 7px 8px;
    }
  }

  ::v-deep .el-input__inner {
    height: 32px;
  }
  ::v-deep .el-form-item {
    margin-bottom: 10px;
  }
}
</style>
