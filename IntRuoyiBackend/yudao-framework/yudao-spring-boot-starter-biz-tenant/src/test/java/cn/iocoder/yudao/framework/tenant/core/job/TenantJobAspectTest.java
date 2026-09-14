package cn.iocoder.yudao.framework.tenant.core.job;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
