package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.*;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProEdhrDeviationReadPermissionTest {

    @Test
    void fullTraceCanBeReadByLoggedInUserWithoutBusinessPermissions() throws Exception {
        var methods = List.of(
                MesProEdhrDeviationController.class.getMethod("getBatchOptionsByActiveOrder", Long.class),
                MesProEdhrDeviationController.class.getMethod("getPage", MesProEdhrDeviationPageReqVO.class),
                MesProEdhrDeviationController.class.getMethod("get", Long.class),
                MesProEdhrDeviationHandlingController.class.getMethod("get", Long.class),
                MesProEdhrNonconformanceReviewController.class.getMethod("get", Long.class));
        var loggedIn = new UsernamePasswordAuthenticationToken("ordinary-user", "", List.of());
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        var handler = new DefaultMethodSecurityExpressionHandler();
        for (Method method : methods) {
            var rule = method.getAnnotation(PreAuthorize.class);
            assertNotNull(rule, method.toString());
            assertEquals("isAuthenticated()", rule.value(), method.toString());
            var invocation = mock(MethodInvocation.class);
            when(invocation.getMethod()).thenReturn(method);
            when(invocation.getThis()).thenReturn(mock(method.getDeclaringClass()));
            var expression = handler.getExpressionParser().parseExpression(rule.value());
            assertEquals(true, expression.getValue(handler.createEvaluationContext(loggedIn, invocation)));
            assertEquals(false, expression.getValue(handler.createEvaluationContext(anonymous, invocation)));
        }
    }

    @Test
    void traceReadAccessDoesNotGrantCreateOrHandlingRights() throws Exception {
        assertEquals("@ss.hasPermission('mes:pro-edhr-deviation:create')",
                MesProEdhrDeviationController.class.getMethod("create", MesProEdhrDeviationCreateReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-deviation:handle')",
                MesProEdhrDeviationHandlingController.class
                        .getMethod("save", Long.class, MesProEdhrDeviationHandlingSaveReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
        for (var method : MesProEdhrDeviationHandlingController.class.getDeclaredMethods()) {
            if (List.of("sign", "close").contains(method.getName())) {
                assertTrue(method.getAnnotation(PreAuthorize.class).value().contains("@ss.hasAnyPermissions"));
            }
        }
    }
}
