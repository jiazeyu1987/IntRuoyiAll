package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeActiveConnectionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeActiveConnectionSaveReqVO;
import cn.iocoder.yudao.module.erp.enums.ErpKingdeeConnectionTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpKingdeeConfigServiceImplTest {

    @Mock
    private ConfigService configService;

    @InjectMocks
    private ErpKingdeeConfigServiceImpl kingdeeConfigService;

    @BeforeEach
    void setUp() {
        ErpKingdeeProperties defaultProperties = new ErpKingdeeProperties();
        defaultProperties.setBaseUrl("http://172.30.30.8/K3Cloud");
        defaultProperties.setAcctId("6977227150362f");
        defaultProperties.setUsername("贾泽宇");
        defaultProperties.setPassword("default-password");
        defaultProperties.setAppId("default-test-app-id");
        defaultProperties.setAppSecret("default-test-app-secret");
        defaultProperties.setLcid(2052);
        defaultProperties.getProduct().setQueryLimit(5000);
        defaultProperties.getBom().setQueryLimit(1000);
        defaultProperties.getProductionOrder().setQueryLimit(1000);
        defaultProperties.getProductionOrder().setTemplateBillNo("881MO090756");
        defaultProperties.getPurchaseOrder().setPurchaseOrgNumber("881");
        defaultProperties.getPurchaseOrder().setQueryDays(365);
        defaultProperties.getPurchaseOrder().setQueryLimit(1000);
        defaultProperties.getSaleOrder().setQueryDays(365);
        defaultProperties.getSaleOrder().setQueryLimit(1000);
        ReflectionTestUtils.setField(kingdeeConfigService, "defaultKingdeeProperties", defaultProperties);
    }

    @Test
    void getConfig_returnsRuntimeDefaultsWhenNoSavedConfigExists() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(config(2L, ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY,
                        ErpKingdeeConnectionTypeEnum.TEST.getType()));
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(null);

        ErpKingdeeConfigRespVO config = kingdeeConfigService.getConfig();

        assertEquals("http://172.30.30.8/K3Cloud", config.getBaseUrl());
        assertEquals("6977227150362f", config.getAcctId());
        assertEquals("贾泽宇", config.getUsername());
        assertEquals("default-test-app-id", config.getAppId());
        assertEquals("default-test-app-secret", config.getAppSecret());
        assertEquals(5000, config.getProduct().getQueryLimit());
        assertEquals(1000, config.getBom().getQueryLimit());
        assertEquals(1000, config.getProductionOrder().getQueryLimit());
        assertEquals("881", config.getPurchaseOrder().getPurchaseOrgNumber());
    }

    @Test
    void getConfig_preservesChineseUsername() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(config(2L, ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY,
                        ErpKingdeeConnectionTypeEnum.TEST.getType()));
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(null);

        ErpKingdeeConfigRespVO config = kingdeeConfigService.getConfig();

        assertEquals("贾泽宇", config.getUsername());
    }

    @Test
    void saveConfig_createsDedicatedInfraConfigWhenMissing() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(config(2L, ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY,
                        ErpKingdeeConnectionTypeEnum.TEST.getType()));
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(null);
        ErpKingdeeConfigSaveReqVO saveReqVO = buildSaveReqVO();

        kingdeeConfigService.saveConfig(saveReqVO);

        org.mockito.ArgumentCaptor<ConfigSaveReqVO> captor = org.mockito.ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService).createConfig(captor.capture());
        verify(configService, never()).updateConfig(any());
        assertEquals(ErpKingdeeConfigServiceImpl.CONFIG_KEY, captor.getValue().getKey());
        assertEquals(Boolean.FALSE, captor.getValue().getVisible());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getValue().contains("\"bom\":{\"queryLimit\":1000}"));
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getValue().contains("\"appId\":\"test-app-id\""));
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getValue().contains("\"appSecret\":\"test-app-secret\""));
    }

    @Test
    void getEffectiveProperties_overridesDefaultsWithSavedConfig() {
        ConfigDO configDO = new ConfigDO();
        configDO.setId(1L);
        configDO.setConfigKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY);
        configDO.setValue("{\"baseUrl\":\"http://custom/K3Cloud\",\"acctId\":\"acct-new\",\"username\":\"sync-user\",\"password\":\"sync-pass\",\"lcid\":2052,\"product\":{\"queryLimit\":9000},\"bom\":{\"queryLimit\":800},\"productionOrder\":{\"queryLimit\":1500},\"purchaseOrder\":{\"purchaseOrgNumber\":\"990\",\"queryDays\":30,\"queryLimit\":1500},\"saleOrder\":{\"queryDays\":45,\"queryLimit\":1800}}");
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(config(2L, ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY,
                        ErpKingdeeConnectionTypeEnum.TEST.getType()));
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(configDO);

        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();

        assertEquals("http://custom/K3Cloud", properties.getBaseUrl());
        assertEquals("acct-new", properties.getAcctId());
        assertEquals("sync-user", properties.getUsername());
        assertEquals("default-test-app-id", properties.getAppId());
        assertEquals("default-test-app-secret", properties.getAppSecret());
        assertEquals(9000, properties.getProduct().getQueryLimit());
        assertEquals(800, properties.getBom().getQueryLimit());
        assertEquals(1500, properties.getProductionOrder().getQueryLimit());
        assertEquals("990", properties.getPurchaseOrder().getPurchaseOrgNumber());
        assertEquals(45, properties.getSaleOrder().getQueryDays());
    }

    @Test
    void getActiveConnection_failsFastWhenSelectionHasNeverBeenSaved() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> kingdeeConfigService.getActiveConnection());

        assertTrue(exception.getMessage().contains("当前连接选择配置缺失"));
    }

    @Test
    void getEffectiveProperties_failsFastWhenSelectionIsBlank() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(config(2L, ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY, "  "));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> kingdeeConfigService.getEffectiveProperties());

        assertTrue(exception.getMessage().contains("当前连接选择配置缺失"));
    }

    @Test
    void updateActiveConnection_savesProductionOnlyAfterProductionConfigIsValidated() {
        ConfigDO productionConfig = config(2L, ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY,
                "{\"baseUrl\":\"http://prod/K3Cloud\",\"acctId\":\"prod-acct\","
                        + "\"username\":\"prod-user\",\"password\":\"prod-password\","
                        + "\"appId\":\"prod-app-id\",\"appSecret\":\"prod-app-secret\",\"lcid\":2052}");
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(null);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(null);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY))
                .thenReturn(productionConfig);
        ErpKingdeeActiveConnectionSaveReqVO request = new ErpKingdeeActiveConnectionSaveReqVO();
        request.setConnectionType(ErpKingdeeConnectionTypeEnum.PRODUCTION.getType());

        ErpKingdeeActiveConnectionRespVO response = kingdeeConfigService.updateActiveConnection(request);

        org.mockito.ArgumentCaptor<ConfigSaveReqVO> captor = org.mockito.ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService).createConfig(captor.capture());
        assertEquals(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY, captor.getValue().getKey());
        assertEquals(ErpKingdeeConnectionTypeEnum.PRODUCTION.getType(), captor.getValue().getValue());
        assertEquals(Boolean.FALSE, captor.getValue().getVisible());
        assertEquals("正式账套", response.getActiveConnectionName());
    }

    @Test
    void updateActiveConnection_doesNotSaveWhenProductionConfigIsMissing() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(null);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY))
                .thenReturn(null);
        ErpKingdeeActiveConnectionSaveReqVO request = new ErpKingdeeActiveConnectionSaveReqVO();
        request.setConnectionType(ErpKingdeeConnectionTypeEnum.PRODUCTION.getType());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> kingdeeConfigService.updateActiveConnection(request));

        assertTrue(exception.getMessage().contains("正式账套连接配置缺失"));
        verify(configService, never()).createConfig(any());
        verify(configService, never()).updateConfig(any());
    }

    @Test
    void getEffectiveProperties_usesProductionConnectionAndPreservesSharedSyncSettings() {
        ConfigDO testConfig = config(1L, ErpKingdeeConfigServiceImpl.CONFIG_KEY,
                "{\"baseUrl\":\"http://test/K3Cloud\",\"acctId\":\"test-acct\","
                        + "\"username\":\"test-user\",\"password\":\"test-password\",\"lcid\":2052,"
                        + "\"product\":{\"queryLimit\":9000},\"bom\":{\"queryLimit\":800},"
                        + "\"productionOrder\":{\"queryLimit\":1500,\"templateBillNo\":\"TEST-MO\"},"
                        + "\"purchaseOrder\":{\"purchaseOrgNumber\":\"990\",\"queryDays\":30,\"queryLimit\":1500},"
                        + "\"saleOrder\":{\"queryDays\":45,\"queryLimit\":1800}}");
        ConfigDO activeConfig = config(2L, ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY,
                ErpKingdeeConnectionTypeEnum.PRODUCTION.getType());
        ConfigDO productionConfig = config(3L, ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY,
                "{\"baseUrl\":\"http://prod/K3Cloud\",\"acctId\":\"prod-acct\","
                        + "\"username\":\"prod-user\",\"password\":\"prod-password\","
                        + "\"appId\":\"prod-app-id\",\"appSecret\":\"prod-app-secret\",\"lcid\":2052}");
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(testConfig);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(activeConfig);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY))
                .thenReturn(productionConfig);

        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();

        assertEquals("http://prod/K3Cloud", properties.getBaseUrl());
        assertEquals("prod-acct", properties.getAcctId());
        assertEquals("prod-user", properties.getUsername());
        assertEquals("prod-app-id", properties.getAppId());
        assertEquals("prod-app-secret", properties.getAppSecret());
        assertEquals(9000, properties.getProduct().getQueryLimit());
        assertEquals("TEST-MO", properties.getProductionOrder().getTemplateBillNo());
        assertEquals("990", properties.getPurchaseOrder().getPurchaseOrgNumber());
    }

    @Test
    void getEffectiveProperties_usesDefaultApplicationCredentialsWhenSavedProductionConfigPredatesThem() {
        ConfigDO testConfig = config(1L, ErpKingdeeConfigServiceImpl.CONFIG_KEY,
                "{\"baseUrl\":\"http://test/K3Cloud\",\"acctId\":\"test-acct\","
                        + "\"username\":\"test-user\",\"password\":\"test-password\",\"lcid\":2052,"
                        + "\"product\":{\"queryLimit\":9000},\"bom\":{\"queryLimit\":800},"
                        + "\"productionOrder\":{\"queryLimit\":1500,\"templateBillNo\":\"TEST-MO\"},"
                        + "\"purchaseOrder\":{\"purchaseOrgNumber\":\"990\",\"queryDays\":30,\"queryLimit\":1500},"
                        + "\"saleOrder\":{\"queryDays\":45,\"queryLimit\":1800}}");
        ConfigDO activeConfig = config(2L, ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY,
                ErpKingdeeConnectionTypeEnum.PRODUCTION.getType());
        ConfigDO productionConfig = config(3L, ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY,
                "{\"baseUrl\":\"http://prod/K3Cloud\",\"acctId\":\"prod-acct\","
                        + "\"username\":\"prod-user\",\"password\":\"prod-password\",\"lcid\":2052}");
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(testConfig);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(activeConfig);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY))
                .thenReturn(productionConfig);

        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();

        assertEquals("http://prod/K3Cloud", properties.getBaseUrl());
        assertEquals("prod-acct", properties.getAcctId());
        assertEquals("prod-user", properties.getUsername());
        assertEquals("default-test-app-id", properties.getAppId());
        assertEquals("default-test-app-secret", properties.getAppSecret());
        assertEquals(9000, properties.getProduct().getQueryLimit());
    }

    @Test
    void getEffectiveProperties_failsFastWhenActiveProductionConfigIsMissing() {
        ConfigDO activeConfig = config(2L, ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY,
                ErpKingdeeConnectionTypeEnum.PRODUCTION.getType());
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(null);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(activeConfig);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY))
                .thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> kingdeeConfigService.getEffectiveProperties());

        assertTrue(exception.getMessage().contains("正式账套连接配置缺失"));
    }

    @Test
    void saveConfig_whenProductionIsActiveKeepsTestConnectionAndUpdatesProductionConnection() {
        ConfigDO activeConfig = config(2L, ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY,
                ErpKingdeeConnectionTypeEnum.PRODUCTION.getType());
        ConfigDO testConfig = config(1L, ErpKingdeeConfigServiceImpl.CONFIG_KEY,
                JsonUtils.toJsonString(buildSaveReqVO()));
        ConfigDO productionConfig = config(3L, ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY,
                "{\"baseUrl\":\"http://old-prod/K3Cloud\",\"acctId\":\"old-prod-acct\","
                        + "\"username\":\"old-prod-user\",\"password\":\"old-prod-password\","
                        + "\"appId\":\"old-prod-app-id\",\"appSecret\":\"old-prod-app-secret\",\"lcid\":2052}");
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(activeConfig);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(testConfig);
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY))
                .thenReturn(productionConfig);
        ErpKingdeeConfigSaveReqVO request = buildSaveReqVO();
        request.setBaseUrl("http://new-prod/K3Cloud");
        request.setAcctId("new-prod-acct");
        request.setUsername("new-prod-user");
        request.setPassword("new-prod-password");
        request.setAppId("new-prod-app-id");
        request.setAppSecret("new-prod-app-secret");
        request.getProduct().setQueryLimit(7777);

        kingdeeConfigService.saveConfig(request);

        org.mockito.ArgumentCaptor<ConfigSaveReqVO> captor = org.mockito.ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService, times(2)).updateConfig(captor.capture());
        List<ConfigSaveReqVO> savedConfigs = captor.getAllValues();
        ConfigSaveReqVO savedTestConfig = savedConfigs.stream()
                .filter(item -> ErpKingdeeConfigServiceImpl.CONFIG_KEY.equals(item.getKey()))
                .findFirst().orElseThrow();
        ConfigSaveReqVO savedProductionConfig = savedConfigs.stream()
                .filter(item -> ErpKingdeeConfigServiceImpl.PRODUCTION_CONNECTION_CONFIG_KEY.equals(item.getKey()))
                .findFirst().orElseThrow();
        ErpKingdeeConfigSaveReqVO savedTest = JsonUtils.parseObject(savedTestConfig.getValue(),
                ErpKingdeeConfigSaveReqVO.class);
        assertEquals("6977227150362f", savedTest.getAcctId());
        assertEquals("test-app-id", savedTest.getAppId());
        assertEquals("test-app-secret", savedTest.getAppSecret());
        assertEquals(7777, savedTest.getProduct().getQueryLimit());
        assertTrue(savedProductionConfig.getValue().contains("new-prod-acct"));
        assertTrue(savedProductionConfig.getValue().contains("new-prod-app-id"));
        assertTrue(savedProductionConfig.getValue().contains("new-prod-app-secret"));
        assertFalse(savedProductionConfig.getValue().contains("old-prod-acct"));
    }

    @Test
    void getActiveConnection_failsFastWhenSavedSelectionIsInvalid() {
        ConfigDO activeConfig = config(2L, ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY, "UNKNOWN");
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.ACTIVE_CONNECTION_CONFIG_KEY))
                .thenReturn(activeConfig);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> kingdeeConfigService.getActiveConnection());

        assertTrue(exception.getMessage().contains("不支持的 ERP 连接类型"));
    }

    @Test
    void isExternalWriteEnabled_returnsFalseWhenConfigMissing() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.EXTERNAL_WRITE_ENABLED_CONFIG_KEY))
                .thenReturn(null);

        assertFalse(kingdeeConfigService.isExternalWriteEnabled());
    }

    @Test
    void updateExternalWriteEnabled_createsDedicatedHiddenConfigWhenMissing() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.EXTERNAL_WRITE_ENABLED_CONFIG_KEY))
                .thenReturn(null);

        kingdeeConfigService.updateExternalWriteEnabled(Boolean.TRUE);

        org.mockito.ArgumentCaptor<ConfigSaveReqVO> captor = org.mockito.ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService).createConfig(captor.capture());
        assertEquals(ErpKingdeeConfigServiceImpl.EXTERNAL_WRITE_ENABLED_CONFIG_KEY, captor.getValue().getKey());
        assertEquals("true", captor.getValue().getValue());
        assertEquals(Boolean.FALSE, captor.getValue().getVisible());
    }

    @Test
    void assertExternalWriteEnabled_failsFastWhenSwitchClosed() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.EXTERNAL_WRITE_ENABLED_CONFIG_KEY))
                .thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> kingdeeConfigService.assertExternalWriteEnabled());

        assertTrue(exception.getMessage().contains("ERP写权限已关闭"));
    }

    @Test
    void assertExternalWriteEnabled_passesWhenSwitchOpen() {
        ConfigDO configDO = new ConfigDO();
        configDO.setConfigKey(ErpKingdeeConfigServiceImpl.EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        configDO.setValue("true");
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.EXTERNAL_WRITE_ENABLED_CONFIG_KEY))
                .thenReturn(configDO);

        assertDoesNotThrow(() -> kingdeeConfigService.assertExternalWriteEnabled());
    }

    private static ErpKingdeeConfigSaveReqVO buildSaveReqVO() {
        ErpKingdeeConfigSaveReqVO reqVO = new ErpKingdeeConfigSaveReqVO();
        reqVO.setBaseUrl("http://172.30.30.8/K3Cloud");
        reqVO.setAcctId("6977227150362f");
        reqVO.setUsername("kingdee-user");
        reqVO.setPassword("kingdee-password");
        reqVO.setAppId("test-app-id");
        reqVO.setAppSecret("test-app-secret");
        reqVO.setLcid(2052);

        ErpKingdeeConfigSaveReqVO.ProductConfig productConfig = new ErpKingdeeConfigSaveReqVO.ProductConfig();
        productConfig.setQueryLimit(5000);
        reqVO.setProduct(productConfig);

        ErpKingdeeConfigSaveReqVO.BomConfig bomConfig = new ErpKingdeeConfigSaveReqVO.BomConfig();
        bomConfig.setQueryLimit(1000);
        reqVO.setBom(bomConfig);

        ErpKingdeeConfigSaveReqVO.ProductionOrderConfig productionOrderConfig =
                new ErpKingdeeConfigSaveReqVO.ProductionOrderConfig();
        productionOrderConfig.setQueryLimit(1000);
        productionOrderConfig.setTemplateBillNo("881MO090756");
        reqVO.setProductionOrder(productionOrderConfig);

        ErpKingdeeConfigSaveReqVO.PurchaseOrderConfig purchaseOrderConfig =
                new ErpKingdeeConfigSaveReqVO.PurchaseOrderConfig();
        purchaseOrderConfig.setPurchaseOrgNumber("881");
        purchaseOrderConfig.setQueryDays(365);
        purchaseOrderConfig.setQueryLimit(1000);
        reqVO.setPurchaseOrder(purchaseOrderConfig);

        ErpKingdeeConfigSaveReqVO.SaleOrderConfig saleOrderConfig =
                new ErpKingdeeConfigSaveReqVO.SaleOrderConfig();
        saleOrderConfig.setQueryDays(365);
        saleOrderConfig.setQueryLimit(1000);
        reqVO.setSaleOrder(saleOrderConfig);
        return reqVO;
    }

    private static ConfigDO config(Long id, String key, String value) {
        ConfigDO configDO = new ConfigDO();
        configDO.setId(id);
        configDO.setConfigKey(key);
        configDO.setValue(value);
        return configDO;
    }

}
