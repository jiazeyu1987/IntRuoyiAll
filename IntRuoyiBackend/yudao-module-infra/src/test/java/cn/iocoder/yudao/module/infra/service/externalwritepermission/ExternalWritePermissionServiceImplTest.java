package cn.iocoder.yudao.module.infra.service.externalwritepermission;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.TenantConfigService;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import({ExternalWritePermissionServiceImpl.class, TenantConfigService.class})
class ExternalWritePermissionServiceImplTest extends BaseDbUnitTest {

    @Resource
    private ExternalWritePermissionService externalWritePermissionService;
    @Resource
    private TenantConfigService tenantConfigService;

    @Test
    void erpExternalWritePermissionDefaultsToDisabled() {
        assertFalse(externalWritePermissionService.isErpExternalWriteEnabled());
    }

    @Test
    void updateErpExternalWritePermissionPersistsTenantConfigKey() {
        externalWritePermissionService.updateErpExternalWriteEnabled(true);

        ConfigDO enabledConfig = tenantConfigService.getConfigByKey(ExternalWritePermissionService.ERP_EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        assertNotNull(enabledConfig);
        assertEquals("true", enabledConfig.getValue());
        assertFalse(enabledConfig.getVisible());
        assertTrue(externalWritePermissionService.isErpExternalWriteEnabled());

        externalWritePermissionService.updateErpExternalWriteEnabled(false);

        ConfigDO disabledConfig = tenantConfigService.getConfigByKey(ExternalWritePermissionService.ERP_EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        assertEquals(enabledConfig.getId(), disabledConfig.getId());
        assertEquals("false", disabledConfig.getValue());
        assertFalse(externalWritePermissionService.isErpExternalWriteEnabled());
    }

}
