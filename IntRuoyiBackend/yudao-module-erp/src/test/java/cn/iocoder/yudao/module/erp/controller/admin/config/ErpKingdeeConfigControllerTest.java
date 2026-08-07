package cn.iocoder.yudao.module.erp.controller.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErpKingdeeConfigControllerTest {

    private static final String PROFILE_CONFIG_PERMISSION =
            "@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')";

    @Test
    void externalWritePermissionEndpointsRequireSuperAdminRole() throws Exception {
        assertEquals("@ss.hasRole('super_admin')",
                ErpKingdeeConfigController.class.getMethod("getExternalWritePermission")
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasRole('super_admin')",
                ErpKingdeeConfigController.class.getMethod("updateExternalWritePermission",
                                Class.forName("cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeExternalWritePermissionSaveReqVO"))
                        .getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void activeConnectionEndpointsReuseProfileConfigurationPermission() throws Exception {
        assertEquals(PROFILE_CONFIG_PERMISSION,
                ErpKingdeeConfigController.class.getMethod("getActiveConnection")
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals(PROFILE_CONFIG_PERMISSION,
                ErpKingdeeConfigController.class.getMethod("updateActiveConnection",
                                Class.forName("cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeActiveConnectionSaveReqVO"))
                        .getAnnotation(PreAuthorize.class).value());
    }

}
