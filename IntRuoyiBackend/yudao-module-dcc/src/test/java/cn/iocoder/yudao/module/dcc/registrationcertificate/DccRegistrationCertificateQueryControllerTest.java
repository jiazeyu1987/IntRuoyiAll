package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.history.DccRegistrationCertificateHistoryItem;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.history.DccRegistrationCertificateHistoryService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccRegistrationCertificateQueryControllerTest {

    private static final String CONTROLLER =
            "cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.query."
                    + "DccRegistrationCertificateQueryController";

    @Test
    void queryController_exposesScopedPageDetailAndHistoryRoutes() throws Exception {
        Class<?> controller = loadController();
        RequestMapping root = controller.getAnnotation(RequestMapping.class);
        assertNotNull(root, "query controller must declare a stable root route");
        assertEquals("/dcc/registration-certificates", root.value()[0]);

        assertRoute(controller, "getPage", "/page");
        assertRoute(controller, "getOldIndexPage", "/old-index/page");
        assertRoute(controller, "getDetail", "/{id}");
        assertRoute(controller, "getHistory", "/{id}/history");
    }

    @Test
    void queryController_usesTheReadPermissionOnEveryRoute() throws Exception {
        Class<?> controller = loadController();
        for (String methodName : new String[]{"getPage", "getOldIndexPage", "getDetail", "getHistory"}) {
            Method method = findMethod(controller, methodName);
            PreAuthorize permission = method.getAnnotation(PreAuthorize.class);
            assertNotNull(permission, methodName + " must declare read permission");
            assertTrue(permission.value().contains("'dcc:registration-certificate:query-current'"),
                    methodName + " must use the registration-certificate read permission");
        }
    }

    @Test
    void historyRoute_checksScopedDetailBeforeReadingHistory() throws Exception {
        Class<?> controllerType = loadController();
        DccRegistrationCertificateQueryService queryService = mock(DccRegistrationCertificateQueryService.class);
        DccRegistrationCertificateHistoryService historyService = mock(DccRegistrationCertificateHistoryService.class);
        Object controller = controllerType
                .getConstructor(DccRegistrationCertificateQueryService.class,
                        DccRegistrationCertificateHistoryService.class)
                .newInstance(queryService, historyService);
        HttpServletRequest request = auditRequest();
        List<DccRegistrationCertificateHistoryItem> expected = List.of(
                new DccRegistrationCertificateHistoryItem("FORMALIZED", "FORMALIZED", null, "{}", 22L,
                        null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null));
        when(historyService.listHistory(11L, 33L)).thenReturn(expected);

        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(22L);
            CommonResult<?> result = (CommonResult<?>) controllerType
                    .getDeclaredMethod("getHistory", Long.class, HttpServletRequest.class)
                    .invoke(controller, 33L, request);
            assertEquals(expected, result.getData());
        } finally {
            TenantContextHolder.clear();
        }

        InOrder order = inOrder(queryService, historyService);
        order.verify(queryService).getDetail(eq(11L), eq(22L), eq(33L), eq(null), any(DccRequestAuditContext.class));
        order.verify(historyService).listHistory(11L, 33L);
    }

    @Test
    void historyRoute_doesNotReadHistoryWhenScopedDetailFails() throws Exception {
        Class<?> controllerType = loadController();
        DccRegistrationCertificateQueryService queryService = mock(DccRegistrationCertificateQueryService.class);
        DccRegistrationCertificateHistoryService historyService = mock(DccRegistrationCertificateHistoryService.class);
        Object controller = controllerType
                .getConstructor(DccRegistrationCertificateQueryService.class,
                        DccRegistrationCertificateHistoryService.class)
                .newInstance(queryService, historyService);
        HttpServletRequest request = auditRequest();
        IllegalStateException denied = new IllegalStateException("scope denied");
        when(queryService.getDetail(eq(11L), eq(22L), eq(33L), eq(null), any(DccRequestAuditContext.class)))
                .thenThrow(denied);

        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(22L);
            InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                    () -> controllerType.getDeclaredMethod("getHistory", Long.class, HttpServletRequest.class)
                            .invoke(controller, 33L, request));
            assertSame(denied, thrown.getCause());
        } finally {
            TenantContextHolder.clear();
        }
        verifyNoInteractions(historyService);
    }

    private static Class<?> loadController() throws ClassNotFoundException {
        try {
            return Class.forName(CONTROLLER);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("registration certificate query controller must exist", exception);
        }
    }

    private static void assertRoute(Class<?> controller, String methodName, String expectedPath) {
        Method method = findMethod(controller, methodName);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping, methodName + " must declare GET mapping");
        assertEquals(expectedPath, mapping.value()[0], methodName + " route");
    }

    private static Method findMethod(Class<?> controller, String methodName) {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError(controller.getName() + "." + methodName + " must exist");
    }

    private static HttpServletRequest auditRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getHeader(DccRequestAuditContext.REQUEST_ID_HEADER)).thenReturn("REQ-QUERY-CONTROLLER");
        return request;
    }
}
