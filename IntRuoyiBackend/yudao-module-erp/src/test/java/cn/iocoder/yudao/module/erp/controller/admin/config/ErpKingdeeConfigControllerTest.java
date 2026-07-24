package cn.iocoder.yudao.module.erp.controller.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErpKingdeeConfigControllerTest {

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

}
