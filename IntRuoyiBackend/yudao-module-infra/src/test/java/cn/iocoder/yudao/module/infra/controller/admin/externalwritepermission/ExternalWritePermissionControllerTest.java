package cn.iocoder.yudao.module.infra.controller.admin.externalwritepermission;

import cn.iocoder.yudao.module.infra.controller.admin.externalwritepermission.vo.ExternalWritePermissionSaveReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExternalWritePermissionControllerTest {

    @Test
    void erpExternalWritePermissionEndpointMustLiveOutsideErpModulePath() throws Exception {
        assertEquals("/infra/external-write-permission",
                ExternalWritePermissionController.class.getAnnotation(RequestMapping.class).value()[0]);
        assertEquals("@ss.hasRole('super_admin')",
                ExternalWritePermissionController.class.getMethod("getErpExternalWritePermission")
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasRole('super_admin')",
                ExternalWritePermissionController.class.getMethod("updateErpExternalWritePermission",
                                ExternalWritePermissionSaveReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
    }

}
