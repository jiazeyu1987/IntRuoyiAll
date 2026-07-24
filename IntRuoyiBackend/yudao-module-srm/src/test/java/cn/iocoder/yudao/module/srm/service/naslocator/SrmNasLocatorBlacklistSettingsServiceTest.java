package cn.iocoder.yudao.module.srm.service.naslocator;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_BLACKLIST_CONFIG_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SrmNasLocatorBlacklistSettingsServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private SrmNasLocatorBlacklistSettingsServiceImpl settingsService;

    @Mock
    private ConfigService configService;

    @Test
    void getPatterns_shouldReturnNormalizedPatterns() {
        when(configService.getConfigByKey(eq(SrmNasLocatorBlacklistSettingsServiceImpl.KEY_BLACKLIST_PATTERNS)))
                .thenReturn(config(SrmNasLocatorBlacklistSettingsServiceImpl.KEY_BLACKLIST_PATTERNS,
                        "[\"  *.pyc  \",\"*MO13*.pdf\",\"*.PYC\"]", 1L));

        assertEquals(List.of("*.pyc", "*MO13*.pdf"), settingsService.getPatterns());
    }

    @Test
    void getPatterns_shouldFailFastWhenConfigIsInvalidJson() {
        when(configService.getConfigByKey(eq(SrmNasLocatorBlacklistSettingsServiceImpl.KEY_BLACKLIST_PATTERNS)))
                .thenReturn(config(SrmNasLocatorBlacklistSettingsServiceImpl.KEY_BLACKLIST_PATTERNS, "{bad-json}", 1L));

        AssertUtils.assertServiceException(settingsService::getPatterns, NAS_LOCATOR_BLACKLIST_CONFIG_INVALID);
    }

    @Test
    void savePatterns_shouldNormalizeDeduplicateAndPersistJsonArray() {
        when(configService.getConfigByKey(eq(SrmNasLocatorBlacklistSettingsServiceImpl.KEY_BLACKLIST_PATTERNS)))
                .thenReturn(null);

        settingsService.savePatterns(List.of("  *.pyc  ", "", "*MO13*.pdf", "*.PYC"));

        ArgumentCaptor<ConfigSaveReqVO> captor = ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService).createConfig(captor.capture());
        ConfigSaveReqVO saved = captor.getValue();
        assertEquals(SrmNasLocatorBlacklistSettingsServiceImpl.KEY_BLACKLIST_PATTERNS, saved.getKey());
        assertEquals("[\"*.pyc\",\"*MO13*.pdf\"]", saved.getValue());
    }

    @Test
    void savePatterns_shouldDeleteConfigWhenPatternsBecomeEmpty() {
        when(configService.getConfigByKey(eq(SrmNasLocatorBlacklistSettingsServiceImpl.KEY_BLACKLIST_PATTERNS)))
                .thenReturn(config(SrmNasLocatorBlacklistSettingsServiceImpl.KEY_BLACKLIST_PATTERNS,
                        "[\"*.pyc\"]", 12L));

        settingsService.savePatterns(List.of(" ", ""));

        verify(configService).deleteConfig(12L);
        verify(configService, never()).createConfig(org.mockito.ArgumentMatchers.any());
        verify(configService, never()).updateConfig(org.mockito.ArgumentMatchers.any());
    }

    private static ConfigDO config(String key, String value, Long id) {
        ConfigDO config = new ConfigDO();
        config.setId(id == null ? randomLongId() : id);
        config.setConfigKey(key);
        config.setValue(value);
        return config;
    }
}
