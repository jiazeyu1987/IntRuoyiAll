package cn.iocoder.yudao.module.erp.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_EXTERNAL_WRITE_DISABLED;

@Service
@Validated
public class ErpKingdeeConfigServiceImpl implements ErpKingdeeConfigService {

    public static final String CONFIG_KEY = "yudao.erp.kingdee.config";
    public static final String EXTERNAL_WRITE_ENABLED_CONFIG_KEY = "yudao.erp.kingdee.external-write-enabled";
    private static final String CONFIG_CATEGORY = "erp";
    private static final String CONFIG_NAME = "ERP金蝶连接配置";
    private static final String CONFIG_REMARK = "ERP Kingdee config JSON";
    private static final String EXTERNAL_WRITE_ENABLED_CONFIG_NAME = "ERP外部写入权限开关";
    private static final String EXTERNAL_WRITE_ENABLED_CONFIG_REMARK = "ERP external write permission switch";

    @Resource
    private ErpKingdeeProperties defaultKingdeeProperties;
    @Resource
    private ConfigService configService;

    @Override
    public ErpKingdeeConfigRespVO getConfig() {
        return JsonUtils.parseObject(JsonUtils.toJsonString(getEffectiveProperties()), ErpKingdeeConfigRespVO.class);
    }

    @Override
    public void saveConfig(ErpKingdeeConfigSaveReqVO saveReqVO) {
        ErpKingdeeProperties effectiveProperties = cloneDefaultProperties();
        applyOverrides(effectiveProperties, saveReqVO);
        effectiveProperties.validateProductSyncConfig();
        effectiveProperties.validateBomSyncConfig();
        effectiveProperties.validateProductionOrderCreateConfig();
        effectiveProperties.validatePurchaseOrderSyncConfig();
        effectiveProperties.validateSaleOrderSyncConfig();

        ConfigDO existingConfig = configService.getConfigByKey(CONFIG_KEY);
        ConfigSaveReqVO configSaveReqVO = new ConfigSaveReqVO();
        configSaveReqVO.setId(existingConfig != null ? existingConfig.getId() : null);
        configSaveReqVO.setCategory(CONFIG_CATEGORY);
        configSaveReqVO.setName(CONFIG_NAME);
        configSaveReqVO.setKey(CONFIG_KEY);
        configSaveReqVO.setValue(JsonUtils.toJsonString(saveReqVO));
        configSaveReqVO.setVisible(Boolean.FALSE);
        configSaveReqVO.setRemark(CONFIG_REMARK);
        if (existingConfig == null) {
            configService.createConfig(configSaveReqVO);
        } else {
            configService.updateConfig(configSaveReqVO);
        }
    }

    @Override
    public ErpKingdeeProperties getEffectiveProperties() {
        ErpKingdeeProperties properties = cloneDefaultProperties();
        ConfigDO config = configService.getConfigByKey(CONFIG_KEY);
        if (config == null || StrUtil.isBlank(config.getValue())) {
            return properties;
        }
        ErpKingdeeConfigSaveReqVO saveReqVO =
                JsonUtils.parseObjectQuietly(config.getValue(), ErpKingdeeConfigSaveReqVO.class);
        if (saveReqVO == null) {
            return properties;
        }
        applyOverrides(properties, saveReqVO);
        return properties;
    }

    @Override
    public boolean isExternalWriteEnabled() {
        ConfigDO config = configService.getConfigByKey(EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        return config != null && Boolean.parseBoolean(StrUtil.trim(config.getValue()));
    }

    @Override
    public void updateExternalWriteEnabled(Boolean enabled) {
        ConfigDO existingConfig = configService.getConfigByKey(EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        ConfigSaveReqVO configSaveReqVO = new ConfigSaveReqVO();
        configSaveReqVO.setId(existingConfig != null ? existingConfig.getId() : null);
        configSaveReqVO.setCategory(CONFIG_CATEGORY);
        configSaveReqVO.setName(EXTERNAL_WRITE_ENABLED_CONFIG_NAME);
        configSaveReqVO.setKey(EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        configSaveReqVO.setValue(String.valueOf(Boolean.TRUE.equals(enabled)));
        configSaveReqVO.setVisible(Boolean.FALSE);
        configSaveReqVO.setRemark(EXTERNAL_WRITE_ENABLED_CONFIG_REMARK);
        if (existingConfig == null) {
            configService.createConfig(configSaveReqVO);
        } else {
            configService.updateConfig(configSaveReqVO);
        }
    }

    @Override
    public void assertExternalWriteEnabled() {
        if (!isExternalWriteEnabled()) {
            throw exception(KINGDEE_EXTERNAL_WRITE_DISABLED);
        }
    }

    private ErpKingdeeProperties cloneDefaultProperties() {
        return JsonUtils.parseObject(JsonUtils.toJsonString(defaultKingdeeProperties), ErpKingdeeProperties.class);
    }

    private void applyOverrides(ErpKingdeeProperties target, ErpKingdeeConfigSaveReqVO source) {
        target.setBaseUrl(source.getBaseUrl());
        target.setAcctId(source.getAcctId());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setLcid(source.getLcid());
        if (source.getProduct() != null) {
            target.getProduct().setQueryLimit(source.getProduct().getQueryLimit());
        }
        if (source.getProductionOrder() != null) {
            target.getProductionOrder().setQueryLimit(source.getProductionOrder().getQueryLimit());
            target.getProductionOrder().setTemplateBillNo(source.getProductionOrder().getTemplateBillNo());
        }
        if (source.getBom() != null) {
            target.getBom().setQueryLimit(source.getBom().getQueryLimit());
        }
        if (source.getPurchaseOrder() != null) {
            target.getPurchaseOrder().setPurchaseOrgNumber(source.getPurchaseOrder().getPurchaseOrgNumber());
            target.getPurchaseOrder().setQueryDays(source.getPurchaseOrder().getQueryDays());
            target.getPurchaseOrder().setQueryLimit(source.getPurchaseOrder().getQueryLimit());
        }
        if (source.getSaleOrder() != null) {
            target.getSaleOrder().setQueryDays(source.getSaleOrder().getQueryDays());
            target.getSaleOrder().setQueryLimit(source.getSaleOrder().getQueryLimit());
        }
    }

}
