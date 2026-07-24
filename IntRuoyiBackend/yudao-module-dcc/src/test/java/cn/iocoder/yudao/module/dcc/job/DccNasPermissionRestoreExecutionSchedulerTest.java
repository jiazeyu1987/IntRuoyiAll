package cn.iocoder.yudao.module.dcc.job;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionRestoreExecutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccNasPermissionRestoreExecutionSchedulerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccNasPermissionRestoreExecutionScheduler scheduler;

    @Mock
    private TenantFrameworkService tenantFrameworkService;
    @Mock
    private DccNasPermissionRestoreExecutionService restoreExecutionService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void schedule_processesWaitingRestorePlansForEachTenant() {
        when(tenantFrameworkService.getTenantIds()).thenReturn(List.of(122L, 123L));

        scheduler.schedule();

        verify(restoreExecutionService, times(2)).processWaitingRestorePlans();
    }

    @Test
    void schedule_failsFastWhenOneTenantFails() {
        when(tenantFrameworkService.getTenantIds()).thenReturn(List.of(122L, 123L));
        doThrow(new IllegalStateException("restore failed"))
                .when(restoreExecutionService).processWaitingRestorePlans();

        assertThrows(IllegalStateException.class, () -> scheduler.schedule());

        verify(restoreExecutionService, times(1)).processWaitingRestorePlans();
    }

}
