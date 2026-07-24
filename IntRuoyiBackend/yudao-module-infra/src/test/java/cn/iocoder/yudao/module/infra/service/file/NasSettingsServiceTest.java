package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_CONFIG_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NasSettingsServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private NasSettingsServiceImpl nasSettingsService;

    @Mock
    private ConfigService configService;

    @Test
    void testGetNasConfig() {
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SERVER))).thenReturn(config(NasSettingsServiceImpl.KEY_SERVER, "172.30.30.4", 1L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PORT))).thenReturn(config(NasSettingsServiceImpl.KEY_PORT, "1445", 5L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SHARE))).thenReturn(config(NasSettingsServiceImpl.KEY_SHARE, "it共享", 2L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_DOMAIN))).thenReturn(config(NasSettingsServiceImpl.KEY_DOMAIN, "WORKGROUP", 6L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_USERNAME))).thenReturn(config(NasSettingsServiceImpl.KEY_USERNAME, "int", 3L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PASSWORD))).thenReturn(config(NasSettingsServiceImpl.KEY_PASSWORD, "Kdlyx123", 4L));

        FileNasConfigRespVO result = nasSettingsService.getNasConfig();

        assertEquals("172.30.30.4", result.getServer());
        assertEquals(1445, result.getPort());
        assertEquals("it共享", result.getShare());
        assertEquals("WORKGROUP", result.getDomain());
        assertEquals("int", result.getUsername());
        assertEquals("Kdlyx123", result.getPassword());
    }

    @Test
    void testGetNasConfig_withoutOptionalValues() {
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SERVER))).thenReturn(config(NasSettingsServiceImpl.KEY_SERVER, "172.30.30.4", 1L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PORT))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SHARE))).thenReturn(config(NasSettingsServiceImpl.KEY_SHARE, "it共享", 2L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_DOMAIN))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_USERNAME))).thenReturn(config(NasSettingsServiceImpl.KEY_USERNAME, "int", 3L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PASSWORD))).thenReturn(config(NasSettingsServiceImpl.KEY_PASSWORD, "Kdlyx123", 4L));

        FileNasConfigRespVO result = nasSettingsService.getNasConfig();

        assertEquals("172.30.30.4", result.getServer());
        assertNull(result.getPort());
        assertEquals("it共享", result.getShare());
        assertEquals("", result.getDomain());
        assertEquals("int", result.getUsername());
        assertEquals("Kdlyx123", result.getPassword());
    }

    @Test
    void testSaveNasConfig_createsAndUpdates() {
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SERVER))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PORT))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SHARE))).thenReturn(config(NasSettingsServiceImpl.KEY_SHARE, "old-share", 2L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_DOMAIN))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_USERNAME))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PASSWORD))).thenReturn(config(NasSettingsServiceImpl.KEY_PASSWORD, "old-pass", 4L));

        FileNasConfigSaveReqVO reqVO = new FileNasConfigSaveReqVO();
        reqVO.setServer("172.30.30.4");
        reqVO.setPort(1445);
        reqVO.setShare("it共享");
        reqVO.setDomain("WORKGROUP");
        reqVO.setUsername("int");
        reqVO.setPassword("Kdlyx123");

        nasSettingsService.saveNasConfig(reqVO);

        verify(configService, times(4)).createConfig(any(ConfigSaveReqVO.class));
        verify(configService, times(2)).updateConfig(any(ConfigSaveReqVO.class));
        ArgumentCaptor<ConfigSaveReqVO> createCaptor = ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService, atLeastOnce()).createConfig(createCaptor.capture());
        assertEquals(NasSettingsServiceImpl.KEY_SERVER, createCaptor.getAllValues().get(0).getKey());
    }

    @Test
    void testSaveNasConfig_removesEmptyOptionalValues() {
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SERVER))).thenReturn(config(NasSettingsServiceImpl.KEY_SERVER, "172.30.30.4", 1L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PORT))).thenReturn(config(NasSettingsServiceImpl.KEY_PORT, "1445", 5L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SHARE))).thenReturn(config(NasSettingsServiceImpl.KEY_SHARE, "it共享", 2L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_DOMAIN))).thenReturn(config(NasSettingsServiceImpl.KEY_DOMAIN, "WORKGROUP", 6L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_USERNAME))).thenReturn(config(NasSettingsServiceImpl.KEY_USERNAME, "int", 3L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PASSWORD))).thenReturn(config(NasSettingsServiceImpl.KEY_PASSWORD, "Kdlyx123", 4L));

        FileNasConfigSaveReqVO reqVO = new FileNasConfigSaveReqVO();
        reqVO.setServer("172.30.30.4");
        reqVO.setPort(null);
        reqVO.setShare("it共享");
        reqVO.setDomain("   ");
        reqVO.setUsername("int");
        reqVO.setPassword("Kdlyx123");

        nasSettingsService.saveNasConfig(reqVO);

        verify(configService).deleteConfig(5L);
        verify(configService).deleteConfig(6L);
    }

    @Test
    void testGetRequiredNasConfig_usesDefaultPortWhenOptionalPortMissing() {
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SERVER))).thenReturn(config(NasSettingsServiceImpl.KEY_SERVER, "172.30.30.4", 1L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PORT))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SHARE))).thenReturn(config(NasSettingsServiceImpl.KEY_SHARE, "it共享", 2L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_DOMAIN))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_USERNAME))).thenReturn(config(NasSettingsServiceImpl.KEY_USERNAME, "int", 3L));
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PASSWORD))).thenReturn(config(NasSettingsServiceImpl.KEY_PASSWORD, "Kdlyx123", 4L));

        NasConnectionConfig result = nasSettingsService.getRequiredNasConfig();

        assertEquals("172.30.30.4", result.server());
        assertEquals(445, result.port());
        assertEquals("it共享", result.share());
        assertEquals("", result.domain());
        assertEquals("int", result.username());
        assertEquals("Kdlyx123", result.password());
    }

    @Test
    void testGetRequiredNasConfig_missing() {
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SERVER))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PORT))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_SHARE))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_DOMAIN))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_USERNAME))).thenReturn(null);
        when(configService.getConfigByKey(eq(NasSettingsServiceImpl.KEY_PASSWORD))).thenReturn(null);

        AssertUtils.assertServiceException(() -> nasSettingsService.getRequiredNasConfig(), FILE_NAS_CONFIG_MISSING);
    }

    private static ConfigDO config(String key, String value, Long id) {
        ConfigDO config = new ConfigDO();
        config.setId(id == null ? randomLongId() : id);
        config.setConfigKey(key);
        config.setValue(value);
        return config;
    }
}
