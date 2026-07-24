package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccBrowserExtensionBlacklistSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileBrowserSettingsServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private ConfigService configService;

    @InjectMocks
    private DccControlledFileBrowserSettingsServiceImpl service;

    @Test
    void saveExtensionBlacklist_normalizesWildcardExtensionsAndCreatesMissingConfig() {
        DccBrowserExtensionBlacklistSaveReqVO reqVO = new DccBrowserExtensionBlacklistSaveReqVO();
        reqVO.setExtensionPatterns(List.of("*.DB", ".pyc", "*.db"));
        when(configService.getConfigByKey(DccControlledFileBrowserSettingsServiceImpl.EXTENSION_BLACKLIST_CONFIG_KEY))
                .thenReturn(null);

        service.saveExtensionBlacklist(reqVO);

        ArgumentCaptor<ConfigSaveReqVO> captor = ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService).createConfig(captor.capture());
        assertEquals("dcc", captor.getValue().getCategory());
        assertEquals(DccControlledFileBrowserSettingsServiceImpl.EXTENSION_BLACKLIST_CONFIG_KEY,
                captor.getValue().getKey());
        assertEquals("[\"*.db\",\"*.pyc\"]", captor.getValue().getValue());
    }

    @Test
    void getBlacklistedExtensionPatterns_readsStoredConfig() {
        ConfigDO config = new ConfigDO();
        config.setValue("[\"*.db\",\"*.pyc\"]");
        when(configService.getConfigByKey(DccControlledFileBrowserSettingsServiceImpl.EXTENSION_BLACKLIST_CONFIG_KEY))
                .thenReturn(config);

        List<String> result = service.getBlacklistedExtensionPatterns();

        assertEquals(List.of("*.db", "*.pyc"), result);
    }
}
