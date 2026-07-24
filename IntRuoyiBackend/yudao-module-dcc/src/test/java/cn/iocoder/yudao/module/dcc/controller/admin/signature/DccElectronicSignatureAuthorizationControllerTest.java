package cn.iocoder.yudao.module.dcc.controller.admin.signature;

import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationUnlockReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationUpdateReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccElectronicSignatureAuthorizationControllerTest {

    private static final String SIGNATURE_MANAGE_PERMISSION =
            "@ss.hasPermission('dcc:controlled-file:signature:manage')";
    private static final String ELECTRONIC_SIGNATURE_ADMIN_ROLE =
            "@ss.hasRole('electronic_signature_admin')";

    @Test
    void authorizationAdministrationEndpointsRequireElectronicSignatureAdminRole() throws NoSuchMethodException {
        List<Method> adminEndpoints = List.of(
                DccElectronicSignatureAuthorizationController.class.getMethod("getAuthorizationPage",
                        DccElectronicSignatureAuthorizationPageReqVO.class),
                DccElectronicSignatureAuthorizationController.class.getMethod("updateAuthorization",
                        Long.class, DccElectronicSignatureAuthorizationUpdateReqVO.class),
                DccElectronicSignatureAuthorizationController.class.getMethod("getAuthorizationAuditPage",
                        Long.class, DccElectronicSignatureAuthorizationAuditPageReqVO.class),
                DccElectronicSignatureAuthorizationController.class.getMethod("unlockAuthorization",
                        Long.class, DccElectronicSignatureAuthorizationUnlockReqVO.class)
        );

        for (Method endpoint : adminEndpoints) {
            PreAuthorize preAuthorize = endpoint.getAnnotation(PreAuthorize.class);
            assertNotNull(preAuthorize, "Missing @PreAuthorize on " + endpoint.getName());
            assertTrue(preAuthorize.value().contains(SIGNATURE_MANAGE_PERMISSION),
                    endpoint.getName() + " must retain the DCC signature manage permission");
            assertTrue(preAuthorize.value().contains(ELECTRONIC_SIGNATURE_ADMIN_ROLE),
                    endpoint.getName() + " must require the electronic signature admin role");
        }
    }

    @Test
    void mySignatureImageEndpointsStayPersonalAndDoNotRequireAdminRole() throws NoSuchMethodException {
        List<Method> personalEndpoints = List.of(
                DccElectronicSignatureAuthorizationController.class.getMethod("getMySignatureImage"),
                DccElectronicSignatureAuthorizationController.class.getMethod("uploadMySignatureImage",
                        MultipartFile.class, String.class),
                DccElectronicSignatureAuthorizationController.class.getMethod("enableMySignatureImage",
                        Long.class, String.class),
                DccElectronicSignatureAuthorizationController.class.getMethod("disableMySignatureImage",
                        String.class)
        );

        for (Method endpoint : personalEndpoints) {
            PreAuthorize preAuthorize = endpoint.getAnnotation(PreAuthorize.class);
            if (preAuthorize == null) {
                assertNull(preAuthorize);
                continue;
            }
            assertTrue(!preAuthorize.value().contains(ELECTRONIC_SIGNATURE_ADMIN_ROLE),
                    endpoint.getName() + " must remain available to the current user, not just admins");
        }
    }
}
