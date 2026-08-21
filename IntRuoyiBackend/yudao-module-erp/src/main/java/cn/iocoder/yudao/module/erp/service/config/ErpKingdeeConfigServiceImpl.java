package cn.iocoder.yudao.module.erp.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeActiveConnectionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeActiveConnectionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConnectionOptionRespVO;
import cn.iocoder.yudao.module.erp.enums.ErpKingdeeConnectionTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_ACTIVE_CONNECTION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_CONNECTION_CONFIG_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_CONNECTION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_EXTERNAL_WRITE_DISABLED;

@Service
@Validated
public class ErpKingdeeConfigServiceImpl implements ErpKingdeeConfigService {

    public static final String CONFIG_KEY = "yudao.erp.kingdee.config";
    public static final String PRODUCTION_CONNECTION_CONFIG_KEY = "yudao.erp.kingdee.connection.production";
    public static final String ACTIVE_CONNECTION_CONFIG_KEY = "yudao.erp.kingdee.connection.active";
    public static final String EXTERNAL_WRITE_ENABLED_CONFIG_KEY = "yudao.erp.kingdee.external-write-enabled";
    private static final String CONFIG_CATEGORY = "erp";
    private static final String CONFIG_NAME = "ERP金蝶连接配置";
    private static final String CONFIG_REMARK = "ERP Kingdee config JSON";
    private static final String PRODUCTION_CONNECTION_CONFIG_NAME = "ERP金蝶正式账套连接";
    private static final String PRODUCTION_CONNECTION_CONFIG_REMARK = "ERP Kingdee production connection JSON";
    private static final String ACTIVE_CONNECTION_CONFIG_NAME = "ERP金蝶当前连接";
    private static final String ACTIVE_CONNECTION_CONFIG_REMARK = "ERP Kingdee active connection type";
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
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(ErpKingdeeConfigSaveReqVO saveReqVO) {
        ErpKingdeeProperties effectiveProperties = cloneDefaultProperties();
        applyOverrides(effectiveProperties, saveReqVO);
        effectiveProperties.validateProductSyncConfig();
        effectiveProperties.validateBomSyncConfig();
        effectiveProperties.validateProductionOrderCreateConfig();
        effectiveProperties.validatePurchaseOrderSyncConfig();
        effectiveProperties.validateSaleOrderSyncConfig();

        ErpKingdeeConnectionTypeEnum activeType = resolveActiveConnectionType();
        if (activeType == ErpKingdeeConnectionTypeEnum.TEST) {
            saveHiddenConfig(CONFIG_KEY, CONFIG_NAME, CONFIG_REMARK, JsonUtils.toJsonString(saveReqVO));
            return;
        }

        ErpKingdeeConfigSaveReqVO testConfig = toSaveReqVO(getTestProperties());
        copySharedSyncConfig(testConfig, saveReqVO);
        saveHiddenConfig(CONFIG_KEY, CONFIG_NAME, CONFIG_REMARK, JsonUtils.toJsonString(testConfig));
        saveHiddenConfig(PRODUCTION_CONNECTION_CONFIG_KEY, PRODUCTION_CONNECTION_CONFIG_NAME,
                PRODUCTION_CONNECTION_CONFIG_REMARK,
                JsonUtils.toJsonString(ErpKingdeeConnectionConfig.from(saveReqVO)));
    }

    @Override
    public ErpKingdeeProperties getEffectiveProperties() {
        return resolveProperties(resolveActiveConnectionType());
    }

    @Override
    public ErpKingdeeActiveConnectionRespVO getActiveConnection() {
        ErpKingdeeConnectionTypeEnum activeType = resolveActiveConnectionType();
        resolveProperties(activeType).validateBaseConfig();
        return buildActiveConnectionResponse(activeType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeActiveConnectionRespVO updateActiveConnection(
            ErpKingdeeActiveConnectionSaveReqVO saveReqVO) {
        ErpKingdeeConnectionTypeEnum targetType =
                ErpKingdeeConnectionTypeEnum.requiredOf(StrUtil.trim(saveReqVO.getConnectionType()));
        resolveProperties(targetType).validateBaseConfig();
        saveHiddenConfig(ACTIVE_CONNECTION_CONFIG_KEY, ACTIVE_CONNECTION_CONFIG_NAME,
                ACTIVE_CONNECTION_CONFIG_REMARK, targetType.getType());
        return buildActiveConnectionResponse(targetType);
    }

    private ErpKingdeeProperties resolveProperties(ErpKingdeeConnectionTypeEnum connectionType) {
        ErpKingdeeProperties properties = getTestProperties();
        if (connectionType == ErpKingdeeConnectionTypeEnum.TEST) {
            return properties;
        }
        applyConnection(properties, getProductionConnection());
        return properties;
    }

    private ErpKingdeeProperties getTestProperties() {
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

    private ErpKingdeeConnectionTypeEnum resolveActiveConnectionType() {
        ConfigDO config = configService.getConfigByKey(ACTIVE_CONNECTION_CONFIG_KEY);
        if (config == null || StrUtil.isBlank(config.getValue())) {
            throw exception(KINGDEE_ACTIVE_CONNECTION_CONFIG_MISSING);
        }
        return ErpKingdeeConnectionTypeEnum.requiredOf(StrUtil.trim(config.getValue()));
    }

    private ErpKingdeeConnectionConfig getProductionConnection() {
        ConfigDO config = configService.getConfigByKey(PRODUCTION_CONNECTION_CONFIG_KEY);
        if (config == null || StrUtil.isBlank(config.getValue())) {
            throw exception(KINGDEE_CONNECTION_CONFIG_MISSING,
                    ErpKingdeeConnectionTypeEnum.PRODUCTION.getName());
        }
        ErpKingdeeConnectionConfig connectionConfig;
        try {
            connectionConfig = JsonUtils.parseObject(config.getValue(), ErpKingdeeConnectionConfig.class);
        } catch (RuntimeException ex) {
            throw exception(KINGDEE_CONNECTION_CONFIG_INVALID,
                    ErpKingdeeConnectionTypeEnum.PRODUCTION.getName(), "JSON 格式错误");
        }
        validateProductionConnection(connectionConfig);
        return connectionConfig;
    }

    private void validateProductionConnection(ErpKingdeeConnectionConfig connectionConfig) {
        if (connectionConfig == null) {
            throw exception(KINGDEE_CONNECTION_CONFIG_INVALID,
                    ErpKingdeeConnectionTypeEnum.PRODUCTION.getName(), "配置内容为空");
        }
        if (StrUtil.isBlank(connectionConfig.getBaseUrl())) {
            throw productionConnectionInvalid("基础地址为空");
        }
        if (StrUtil.isBlank(connectionConfig.getAcctId())) {
            throw productionConnectionInvalid("账套 ID 为空");
        }
        if (StrUtil.isBlank(connectionConfig.getUsername())) {
            throw productionConnectionInvalid("用户名为空");
        }
        if (StrUtil.isBlank(connectionConfig.getPassword())) {
            throw productionConnectionInvalid("密码为空");
        }
        if (connectionConfig.getLcid() == null) {
            throw productionConnectionInvalid("语言 LCID 为空");
        }
    }

    private RuntimeException productionConnectionInvalid(String reason) {
        return exception(KINGDEE_CONNECTION_CONFIG_INVALID,
                ErpKingdeeConnectionTypeEnum.PRODUCTION.getName(), reason);
    }

    private ErpKingdeeActiveConnectionRespVO buildActiveConnectionResponse(
            ErpKingdeeConnectionTypeEnum activeType) {
        ErpKingdeeActiveConnectionRespVO response = new ErpKingdeeActiveConnectionRespVO();
        response.setActiveConnectionType(activeType.getType());
        response.setActiveConnectionName(activeType.getName());
        response.setOptions(Arrays.stream(ErpKingdeeConnectionTypeEnum.values())
                .map(item -> new ErpKingdeeConnectionOptionRespVO(item.getType(), item.getName()))
                .collect(Collectors.toList()));
        return response;
    }

    private void saveHiddenConfig(String key, String name, String remark, String value) {
        ConfigDO existingConfig = configService.getConfigByKey(key);
        ConfigSaveReqVO configSaveReqVO = new ConfigSaveReqVO();
        configSaveReqVO.setId(existingConfig != null ? existingConfig.getId() : null);
        configSaveReqVO.setCategory(CONFIG_CATEGORY);
        configSaveReqVO.setName(name);
        configSaveReqVO.setKey(key);
        configSaveReqVO.setValue(value);
        configSaveReqVO.setVisible(Boolean.FALSE);
        configSaveReqVO.setRemark(remark);
        if (existingConfig == null) {
            configService.createConfig(configSaveReqVO);
        } else {
            configService.updateConfig(configSaveReqVO);
        }
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

    private ErpKingdeeConfigSaveReqVO toSaveReqVO(ErpKingdeeProperties properties) {
        return JsonUtils.parseObject(JsonUtils.toJsonString(properties), ErpKingdeeConfigSaveReqVO.class);
    }

    private void copySharedSyncConfig(ErpKingdeeConfigSaveReqVO target, ErpKingdeeConfigSaveReqVO source) {
        target.setProduct(source.getProduct());
        target.setBom(source.getBom());
        target.setProductionOrder(source.getProductionOrder());
        target.setPurchaseOrder(source.getPurchaseOrder());
        target.setSaleOrder(source.getSaleOrder());
    }

    private void applyConnection(ErpKingdeeProperties target, ErpKingdeeConnectionConfig source) {
        target.setBaseUrl(source.getBaseUrl());
        target.setAcctId(source.getAcctId());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setLcid(source.getLcid());
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

    @Data
    private static class ErpKingdeeConnectionConfig {

        private String baseUrl;
        private String acctId;
        private String username;
        private String password;
        private Integer lcid;

        private static ErpKingdeeConnectionConfig from(ErpKingdeeConfigSaveReqVO source) {
            ErpKingdeeConnectionConfig connectionConfig = new ErpKingdeeConnectionConfig();
            connectionConfig.setBaseUrl(source.getBaseUrl());
            connectionConfig.setAcctId(source.getAcctId());
            connectionConfig.setUsername(source.getUsername());
            connectionConfig.setPassword(source.getPassword());
            connectionConfig.setLcid(source.getLcid());
            return connectionConfig;
        }

    }

}
