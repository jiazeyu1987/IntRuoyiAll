package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcSubmitRespVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcContextService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmitResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlinePqcSubmitReceiptControllerTest {

    @Test
    void actualMethodSecurityRequiresPqcBusinessRoleAndOriginalPermissionForEveryPqcEntry() throws Exception {
        var heldPermission = new java.util.concurrent.atomic.AtomicReference<String>();
        var heldRole = new java.util.concurrent.atomic.AtomicReference<String>("production_permission");
        var permissionApi = org.mockito.Mockito.mock(
                cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi.class, invocation -> {
                    if (!Long.valueOf(9001L).equals(invocation.getArgument(0))) return false;
                    var requested = java.util.Arrays.asList((String[]) invocation.getRawArguments()[1]);
                    return switch (invocation.getMethod().getName()) {
                        case "hasAnyPermissions" -> requested.contains(heldPermission.get());
                        case "hasAnyRoles" -> requested.contains(heldRole.get());
                        default -> throw new AssertionError("Unexpected permission API call");
                    };
                });
        var beans = new org.springframework.context.support.GenericApplicationContext();
        beans.registerBean("ss", cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkServiceImpl.class,
                () -> new cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkServiceImpl(permissionApi));
        beans.refresh();
        var previousContext = org.springframework.security.core.context.SecurityContextHolder.getContext();
        var authenticationContext = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        authenticationContext.setAuthentication(new org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken("pqc-test-user", null, java.util.List.of()));
        org.springframework.security.core.context.SecurityContextHolder.setContext(authenticationContext);
        try (beans; MockedStatic<SecurityFrameworkUtils> login = mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            var handler = new org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler();
            handler.setApplicationContext(beans);
            var manager = new org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager();
            manager.setExpressionHandler(handler);
            var targets = java.util.List.of(new MesFrontlineDeviceAccountController(),
                    new cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.MesProFrontlineReturnCorrectionController(
                            null, null, null),
                    new cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.MesProProcessPoolEventRevisionController(
                            null, null, null, null));
            int protectedMethods = 0;
            for (Object target : targets) {
                var factory = new org.springframework.aop.framework.ProxyFactory(target);
                factory.setProxyTargetClass(true);
                factory.addAdvisor(org.springframework.security.authorization.method
                        .AuthorizationManagerBeforeMethodInterceptor.preAuthorize(manager));
                // Sentinel after the real authorization advisor distinguishes allowed invocation from denial.
                factory.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation -> {
                    throw new AllowedInvocation();
                });
                Object proxy = factory.getProxy();
                for (Method method : target.getClass().getDeclaredMethods()) {
                    PreAuthorize guard = method.getAnnotation(PreAuthorize.class);
                    if (guard == null || !guard.value().contains("pqc_permission")) continue;
                    protectedMethods++;
                    Object[] args = new Object[method.getParameterCount()];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = method.getParameterTypes()[i] == String.class ? "PQC"
                                : method.getParameterTypes()[i] == Long.class ? 6101L : null;
                    }
                    String permission = guard.value().contains("event-revision:update")
                            ? "mes:pro-process-pool:event-revision:update"
                            : guard.value().contains("feedback:create") ? "mes:pro-feedback:create"
                            : "mes:pro-feedback:query";
                    heldPermission.set(permission);
                    heldRole.set("production_permission");
                    assertDenied(method, proxy, args);
                    for (String role : java.util.List.of("pqc_permission", "pqc_leader_permission")) {
                        heldRole.set(role);
                        assertAllowed(method, proxy, args);
                        heldPermission.set(null);
                        assertDenied(method, proxy, args);
                        heldPermission.set(permission);
                    }
                }
            }
            assertEquals(10, protectedMethods);
            // The shared return endpoints remain available to production for their own production returns.
            var returnFactory = new org.springframework.aop.framework.ProxyFactory(targets.get(1));
            returnFactory.setProxyTargetClass(true);
            returnFactory.addAdvisor(org.springframework.security.authorization.method
                    .AuthorizationManagerBeforeMethodInterceptor.preAuthorize(manager));
            returnFactory.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation -> { throw new AllowedInvocation(); });
            heldRole.set("production_permission");
            heldPermission.set("mes:pro-feedback:query");
            assertAllowed(targets.get(1).getClass().getDeclaredMethod("list", String.class),
                    returnFactory.getProxy(), new Object[]{"PRODUCTION"});
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.setContext(previousContext);
        }
    }

    private static void assertDenied(Method method, Object proxy, Object[] args) {
        var error = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> method.invoke(proxy, args));
        org.junit.jupiter.api.Assertions.assertInstanceOf(org.springframework.security.access.AccessDeniedException.class,
                error.getCause(), method.getName());
    }

    private static void assertAllowed(Method method, Object proxy, Object[] args) {
        var error = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> method.invoke(proxy, args));
        org.junit.jupiter.api.Assertions.assertInstanceOf(AllowedInvocation.class, error.getCause(), method.getName());
    }

    private static final class AllowedInvocation extends RuntimeException { }

    @Mock
    private MesFrontlinePqcContextService pqcContextService;

    @InjectMocks
    private MesFrontlineDeviceAccountController controller;

    @Test
    void submitReceiptEndpoint_isReadOnlyGetRouteWithQueryPermission() throws Exception {
        Method method = MesFrontlineDeviceAccountController.class.getDeclaredMethod(
                "getPqcSubmitReceipt", Long.class);

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
        assertArrayEquals(new String[]{"/pqc/submit-receipt"}, getMapping.value());
        assertEquals("@ss.hasPermission('mes:pro-feedback:query') and @ss.hasAnyRoles('pqc_permission', 'pqc_leader_permission')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void getPqcSubmitReceipt_usesLoginUserAndMapsSubmittedReceipt() {
        LocalDateTime serverSubmitTime = LocalDateTime.of(2026, 8, 8, 9, 30);
        when(pqcContextService.getSubmittedPqcInspection(9001L, 6101L))
                .thenReturn(Optional.of(new MesFrontlinePqcSubmitResult(
                        6101L, 7101L, 7101L, "payload-hash", 8101L, 9101L, "QUALIFIED", serverSubmitTime)));

        CommonResult<MesFrontlinePqcSubmitRespVO> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            response = controller.getPqcSubmitReceipt(6101L);
        }

        verify(pqcContextService).getSubmittedPqcInspection(9001L, 6101L);
        MesFrontlinePqcSubmitRespVO data = response.getData();
        assertEquals(6101L, data.getPqcTaskId());
        assertEquals(7101L, data.getPqcEventId());
        assertEquals(7101L, data.getSourceRevision());
        assertEquals("payload-hash", data.getPayloadHash());
        assertEquals(8101L, data.getPqcRecordId());
        assertEquals(9101L, data.getSignatureId());
        assertEquals("QUALIFIED", data.getInspectionResult());
        assertEquals(serverSubmitTime, data.getServerSubmitTime());
    }

    @Test
    void getPqcSubmitReceipt_returnsNullWhenTaskHasNoSubmittedReceipt() {
        when(pqcContextService.getSubmittedPqcInspection(9001L, 6102L)).thenReturn(Optional.empty());

        CommonResult<MesFrontlinePqcSubmitRespVO> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            response = controller.getPqcSubmitReceipt(6102L);
        }

        verify(pqcContextService).getSubmittedPqcInspection(9001L, 6102L);
        assertNull(response.getData());
    }
}
