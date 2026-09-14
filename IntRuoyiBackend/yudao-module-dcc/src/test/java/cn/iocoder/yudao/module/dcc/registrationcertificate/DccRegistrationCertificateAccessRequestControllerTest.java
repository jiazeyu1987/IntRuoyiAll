package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo.DccRegistrationCertificateAccessReasonReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo.DccRegistrationCertificateAccessRequestStatusRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accessrequest.DccRegistrationCertificateAccessRequestService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateAccessRequestStatus;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.DccRegistrationCertificateAccessRequestController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.grant.DccRegistrationCertificateGrantController;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccRegistrationCertificateAccessRequestControllerTest {

    private static final String CREATE_PERMISSION =
            "dcc:registration-certificate:access-request:create";
    private static final String APPROVE_PERMISSION =
            "dcc:registration-certificate:access-request:approve";

    @Test
    void accessRequestController_exposesFrozenStatusWithdrawAndGrantRevokeRoutes() {
        Class<?> controller = controllerType();
        RequestMapping root = controller.getAnnotation(RequestMapping.class);
        assertNotNull(root);
        assertEquals("/dcc/registration-certificates/access-requests", root.value()[0]);

        Method status = findMethod(controller, "getStatus");
        assertRoute(status, GetMapping.class, "/{requestId}");
        assertPermission(status, CREATE_PERMISSION, APPROVE_PERMISSION);

        Method withdraw = findMethod(controller, "withdraw");
        assertRoute(withdraw, PostMapping.class, "/{requestId}/withdraw");
        assertPermission(withdraw, CREATE_PERMISSION);
        assertPathVariable(withdraw, "requestId");
        assertHasRequestBody(withdraw);

        Class<?> grantController = load("cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.grant"
                + ".DccRegistrationCertificateGrantController");
        RequestMapping grantRoot = grantController.getAnnotation(RequestMapping.class);
        assertNotNull(grantRoot);
        assertEquals("/dcc/registration-certificates/grants", grantRoot.value()[0]);
        Method revoke = findMethod(grantController, "revokeGrant");
        assertRoute(revoke, PostMapping.class, "/{grantId}/revoke");
        assertPermission(revoke, APPROVE_PERMISSION);
        assertPathVariable(revoke, "grantId");
        assertHasRequestBody(revoke);
    }

    @Test
    void statusEndpoint_delegatesTenantActorAndRequestIdAndReturnsGrantStatuses() {
        DccRegistrationCertificateAccessRequestService accessRequestService = mock(
                DccRegistrationCertificateAccessRequestService.class);
        DccRegistrationCertificateApprovalService approvalService = mock(
                DccRegistrationCertificateApprovalService.class);
        DccRegistrationCertificateAccessRequestController controller =
                new DccRegistrationCertificateAccessRequestController(accessRequestService, approvalService);
        DccRegistrationCertificateAccessRequestStatus status = new DccRegistrationCertificateAccessRequestStatus(
                33L, 44L, 55L, 66L, "DOWNLOAD_FILE", "audit", 77L, "APPROVED", "proc-33",
                "APPROVED", null, null, null, null, null, List.of());
        when(approvalService.getStatus(11L, 22L, 33L)).thenReturn(status);

        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(22L);
            CommonResult<DccRegistrationCertificateAccessRequestStatusRespVO> result = controller.getStatus(33L);
            assertEquals(33L, result.getData().getRequestId());
            assertEquals("APPROVED", result.getData().getRequestStatus());
            assertEquals("proc-33", result.getData().getBpmProcessInstanceId());
        } finally {
            TenantContextHolder.clear();
        }
        verify(approvalService).getStatus(11L, 22L, 33L);
    }

    @Test
    void withdrawEndpoint_delegatesTenantActorRequestAndReason() {
        DccRegistrationCertificateAccessRequestService accessRequestService = mock(
                DccRegistrationCertificateAccessRequestService.class);
        DccRegistrationCertificateApprovalService approvalService = mock(
                DccRegistrationCertificateApprovalService.class);
        DccRegistrationCertificateAccessRequestController controller =
                new DccRegistrationCertificateAccessRequestController(accessRequestService, approvalService);
        DccRegistrationCertificateAccessReasonReqVO req = new DccRegistrationCertificateAccessReasonReqVO();
        req.setReason("业务已取消");
        DccRegistrationCertificateApprovalResult expected =
                new DccRegistrationCertificateApprovalResult(33L, "DCC_REG_CERT_ACCESS:33", "proc-33",
                        "WITHDRAWN", List.of());
        when(approvalService.withdraw(11L, 22L, 33L, "业务已取消")).thenReturn(expected);

        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(22L);
            CommonResult<DccRegistrationCertificateApprovalResult> result = controller.withdraw(33L, req);
            assertEquals("WITHDRAWN", result.getData().status());
        } finally {
            TenantContextHolder.clear();
        }
        verify(approvalService).withdraw(11L, 22L, 33L, "业务已取消");
    }

    @Test
    void grantRevokeEndpoint_delegatesTenantActorGrantAndReason() {
        DccRegistrationCertificateApprovalService approvalService = mock(
                DccRegistrationCertificateApprovalService.class);
        DccRegistrationCertificateGrantController controller = new DccRegistrationCertificateGrantController(approvalService);
        DccRegistrationCertificateAccessReasonReqVO req = new DccRegistrationCertificateAccessReasonReqVO();
        req.setReason("权限范围失效");

        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(22L);
            CommonResult<Boolean> result = controller.revokeGrant(88L, req);
            assertTrue(result.getData());
        } finally {
            TenantContextHolder.clear();
        }
        verify(approvalService).revokeGrant(11L, 22L, 88L, "权限范围失效");
    }

    private static Class<?> controllerType() {
        return load("cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest"
                + ".DccRegistrationCertificateAccessRequestController");
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(name + " must exist as a real HTTP controller", exception);
        }
    }

    private static Method findMethod(Class<?> type, String name) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new AssertionError(type.getName() + "." + name + " must exist as a real HTTP entry");
    }

    private static void assertRoute(Method method, Class<? extends Annotation> mappingType, String expected) {
        Annotation mapping = method.getAnnotation(mappingType);
        assertNotNull(mapping, method.getName() + " must declare " + mappingType.getSimpleName());
        String actual;
        if (mapping instanceof GetMapping get) {
            actual = get.value()[0];
        } else if (mapping instanceof PostMapping post) {
            actual = post.value()[0];
        } else {
            throw new AssertionError("unsupported mapping " + mappingType);
        }
        assertEquals(expected, actual, method.getName() + " route");
    }

    private static void assertPermission(Method method, String... permissions) {
        PreAuthorize permission = method.getAnnotation(PreAuthorize.class);
        assertNotNull(permission, method.getName() + " must declare an explicit permission");
        for (String expected : permissions) {
            assertTrue(permission.value().contains("'" + expected + "'"),
                    method.getName() + " must include " + expected);
        }
    }

    private static void assertPathVariable(Method method, String expected) {
        boolean found = false;
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof PathVariable pathVariable
                        && expected.equals(pathVariable.value())) {
                    found = true;
                }
            }
        }
        assertTrue(found, method.getName() + " must bind path variable " + expected);
    }

    private static void assertHasRequestBody(Method method) {
        boolean found = false;
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof RequestBody) {
                    found = true;
                }
            }
        }
        assertTrue(found, method.getName() + " must accept a reason request body");
    }
}
