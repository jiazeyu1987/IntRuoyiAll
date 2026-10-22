package cn.iocoder.yudao.framework.tenant.core.job;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantJobAspectTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void scopedParameter_executesOnlyTheRequestedTenantAndRestoresHandlerParameter() throws Throwable {
        TenantFrameworkService tenantService = mock(TenantFrameworkService.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        String scopedParam = TenantJobParam.forTenant(1L, "FULL_SYNC");
        when(joinPoint.getArgs()).thenReturn(new Object[]{scopedParam});
        when(joinPoint.proceed(any(Object[].class))).thenAnswer(invocation -> {
            Object[] args = invocation.getArgument(0);
            assertEquals("FULL_SYNC", args[0]);
            assertEquals(1L, TenantContextHolder.getRequiredTenantId());
            return "ok";
        });

        String result = new TenantJobAspect(tenantService).around(joinPoint, mock(TenantJob.class));

        assertTrue(result.contains("1"));
        assertFalse(result.contains("2"));
        verify(tenantService).validTenant(1L);
        verify(tenantService, never()).getTenantIds();
    }

    @Test
    void unscopedParameter_keepsAllEnabledTenantExecution() throws Throwable {
        TenantFrameworkService tenantService = mock(TenantFrameworkService.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(tenantService.getTenantIds()).thenReturn(List.of(1L, 2L));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"SCHEDULED"});
        when(joinPoint.proceed()).thenReturn("ok");

        String result = new TenantJobAspect(tenantService).around(joinPoint, mock(TenantJob.class));

        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        verify(tenantService).getTenantIds();
    }

    @Test
    void strictFailureCollectsEveryTenantAndPropagatesRuntimeAndError() {
        TenantFrameworkService tenantService = mock(TenantFrameworkService.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(tenantService.getTenantIds()).thenReturn(List.of(1L, 2L, 3L));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"SCHEDULED"});
        Set<Long> attempted = ConcurrentHashMap.newKeySet();
        try {
            when(joinPoint.proceed()).thenAnswer(invocation -> {
                long tenantId = TenantContextHolder.getRequiredTenantId();
                attempted.add(tenantId);
                if (tenantId == 1L) throw new IllegalStateException("DCC_FAILED_IDS=41");
                if (tenantId == 2L) throw new AssertionError("DCC_ERROR");
                return "ok";
            });
        } catch (Throwable throwable) {
            throw new AssertionError(throwable);
        }
        TenantJob strict = strictAnnotation();

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> new TenantJobAspect(tenantService).around(joinPoint, strict));

        assertEquals(Set.of(1L, 2L, 3L), attempted);
        assertTrue(failure.getMessage().contains("1"));
        assertTrue(failure.getMessage().contains("2"));
        assertTrue(failure.getMessage().contains("DCC_FAILED_IDS=41"));
        assertEquals(2, failure.getSuppressed().length);
        assertNull(failure.getCause());
    }

    @Test
    void strictOptInRemainsFalseByDefault() throws Exception {
        assertEquals(Boolean.FALSE, TenantJob.class.getMethod("failOnError").getDefaultValue());
    }

    @Test
    void ordinaryTenantFailureKeepsLegacyResultBehavior() throws Throwable {
        TenantFrameworkService tenantService = mock(TenantFrameworkService.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(tenantService.getTenantIds()).thenReturn(List.of(7L));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"SCHEDULED"});
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("legacy tenant failure"));

        String result = new TenantJobAspect(tenantService).around(joinPoint, mock(TenantJob.class));

        assertTrue(result.contains("7"));
        assertTrue(result.contains("legacy tenant failure"));
    }

    private TenantJob strictAnnotation() {
        return (TenantJob) Proxy.newProxyInstance(TenantJob.class.getClassLoader(), new Class<?>[]{TenantJob.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "failOnError" -> true;
                    case "annotationType" -> TenantJob.class;
                    case "toString" -> "@TenantJob(failOnError=true)";
                    case "hashCode" -> 1;
                    case "equals" -> proxy == args[0];
                    default -> method.getDefaultValue();
                });
    }

    @Test
    void malformedScopedParameter_failsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> TenantJobParam.parse("__TENANT_JOB_SCOPE__:broken"));
    }

    @Test
    void scopedParameter_preservesNullHandlerParameter() {
        TenantJobParam parameter = TenantJobParam.parse(TenantJobParam.forTenant(1L, null));

        assertEquals(1L, parameter.tenantId());
        assertNull(parameter.handlerParam());
    }

}
