package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigSaveReqVO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(null);

        ErpKingdeeConfigRespVO config = kingdeeConfigService.getConfig();

        assertEquals("http://172.30.30.8/K3Cloud", config.getBaseUrl());
        assertEquals("6977227150362f", config.getAcctId());
        assertEquals("贾泽宇", config.getUsername());
        assertEquals(5000, config.getProduct().getQueryLimit());
        assertEquals(1000, config.getBom().getQueryLimit());
        assertEquals(1000, config.getProductionOrder().getQueryLimit());
        assertEquals("881", config.getPurchaseOrder().getPurchaseOrgNumber());
    }

    @Test
    void getConfig_preservesChineseUsername() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(null);

        ErpKingdeeConfigRespVO config = kingdeeConfigService.getConfig();

        assertEquals("贾泽宇", config.getUsername());
    }

    @Test
    void saveConfig_createsDedicatedInfraConfigWhenMissing() {
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(null);
        ErpKingdeeConfigSaveReqVO saveReqVO = buildSaveReqVO();

        kingdeeConfigService.saveConfig(saveReqVO);

        org.mockito.ArgumentCaptor<ConfigSaveReqVO> captor = org.mockito.ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService).createConfig(captor.capture());
        verify(configService, never()).updateConfig(any());
        assertEquals(ErpKingdeeConfigServiceImpl.CONFIG_KEY, captor.getValue().getKey());
        assertEquals(Boolean.FALSE, captor.getValue().getVisible());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getValue().contains("\"bom\":{\"queryLimit\":1000}"));
    }

    @Test
    void getEffectiveProperties_overridesDefaultsWithSavedConfig() {
        ConfigDO configDO = new ConfigDO();
        configDO.setId(1L);
        configDO.setConfigKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY);
        configDO.setValue("{\"baseUrl\":\"http://custom/K3Cloud\",\"acctId\":\"acct-new\",\"username\":\"sync-user\",\"password\":\"sync-pass\",\"lcid\":2052,\"product\":{\"queryLimit\":9000},\"bom\":{\"queryLimit\":800},\"productionOrder\":{\"queryLimit\":1500},\"purchaseOrder\":{\"purchaseOrgNumber\":\"990\",\"queryDays\":30,\"queryLimit\":1500},\"saleOrder\":{\"queryDays\":45,\"queryLimit\":1800}}");
        when(configService.getConfigByKey(ErpKingdeeConfigServiceImpl.CONFIG_KEY)).thenReturn(configDO);

        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();

        assertEquals("http://custom/K3Cloud", properties.getBaseUrl());
        assertEquals("acct-new", properties.getAcctId());
        assertEquals("sync-user", properties.getUsername());
        assertEquals(9000, properties.getProduct().getQueryLimit());
        assertEquals(800, properties.getBom().getQueryLimit());
        assertEquals(1500, properties.getProductionOrder().getQueryLimit());
        assertEquals("990", properties.getPurchaseOrder().getPurchaseOrgNumber());
        assertEquals(45, properties.getSaleOrder().getQueryDays());
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

}
