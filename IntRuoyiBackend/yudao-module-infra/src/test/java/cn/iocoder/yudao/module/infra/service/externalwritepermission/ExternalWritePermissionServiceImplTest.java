package cn.iocoder.yudao.module.infra.service.externalwritepermission;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.config.ConfigMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import({ExternalWritePermissionServiceImpl.class, ConfigServiceImpl.class})
class ExternalWritePermissionServiceImplTest extends BaseDbUnitTest {

    @Resource
    private ExternalWritePermissionService externalWritePermissionService;
    @Resource
    private ConfigMapper configMapper;

    @Test
    void erpExternalWritePermissionDefaultsToDisabled() {
        assertFalse(externalWritePermissionService.isErpExternalWriteEnabled());
    }

    @Test
    void updateErpExternalWritePermissionPersistsSharedConfigKey() {
        externalWritePermissionService.updateErpExternalWriteEnabled(true);

        ConfigDO enabledConfig = configMapper.selectByKey(ExternalWritePermissionService.ERP_EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        assertNotNull(enabledConfig);
        assertEquals("true", enabledConfig.getValue());
        assertFalse(enabledConfig.getVisible());
        assertTrue(externalWritePermissionService.isErpExternalWriteEnabled());

        externalWritePermissionService.updateErpExternalWriteEnabled(false);

        ConfigDO disabledConfig = configMapper.selectByKey(ExternalWritePermissionService.ERP_EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        assertEquals(enabledConfig.getId(), disabledConfig.getId());
        assertEquals("false", disabledConfig.getValue());
        assertFalse(externalWritePermissionService.isErpExternalWriteEnabled());
    }

}
