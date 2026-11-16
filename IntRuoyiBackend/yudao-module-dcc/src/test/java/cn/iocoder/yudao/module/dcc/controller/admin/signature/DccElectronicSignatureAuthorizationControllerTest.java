package cn.iocoder.yudao.module.dcc.controller.admin.signature;

import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationUnlockReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationUpdateReqVO;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DccElectronicSignatureAuthorizationControllerTest {

    private static final String SIGNATURE_MANAGE_PERMISSION =
            "@ss.hasPermission('dcc:controlled-file:signature:manage')";
    private static final String ELECTRONIC_SIGNATURE_ADMIN_ROLE =
            "@ss.hasRole('electronic_signature_admin')";

    @ParameterizedTest
    @CsvSource({
            "tenant_admin, true, true",
            "electronic_signature_admin, true, true",
            "tenant_admin, false, false",
            "electronic_signature_admin, false, false",
            "audit_admin, true, false",
            "operator, true, false",
            "operator, false, false"
    })
    void authorizationEndpointsEnforceRoleAndPermission(String role, boolean hasPermission,
                                                        boolean expected) throws NoSuchMethodException {
        SecurityFrameworkService security = mock(SecurityFrameworkService.class);
        when(security.hasPermission("dcc:controlled-file:signature:manage")).thenReturn(hasPermission);
        when(security.hasRole(anyString())).thenAnswer(invocation -> role.equals(invocation.getArgument(0)));
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver((evaluationContext, beanName) -> {
            assertEquals("ss", beanName);
            return security;
        });
        SpelExpressionParser parser = new SpelExpressionParser();
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
            assertEquals(expected, parser.parseExpression(preAuthorize.value()).getValue(context, Boolean.class),
                    endpoint.getName() + " role=" + role + " permission=" + hasPermission);
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
