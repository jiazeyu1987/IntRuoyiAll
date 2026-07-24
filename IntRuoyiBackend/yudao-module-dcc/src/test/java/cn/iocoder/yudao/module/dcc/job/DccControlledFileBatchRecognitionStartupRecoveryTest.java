package cn.iocoder.yudao.module.dcc.job;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBatchRecognitionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileBatchRecognitionStartupRecoveryTest extends BaseMockitoUnitTest {

    @Mock
    private TenantFrameworkService tenantFrameworkService;
    @Mock
    private DccControlledFileBatchRecognitionService batchRecognitionService;

    @InjectMocks
    private DccControlledFileBatchRecognitionStartupRecovery startupRecovery;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void recoverInterruptedTasksOnStartupRunsForEveryTenantWithoutSchedulerCondition() {
        when(tenantFrameworkService.getTenantIds()).thenReturn(List.of(1L, 122L));
        List<Long> tenantIdsAtRecovery = new ArrayList<>();
        doAnswer(invocation -> {
            tenantIdsAtRecovery.add(TenantContextHolder.getTenantId());
            return null;
        }).when(batchRecognitionService).recoverInterruptedTasksOnStartup();

        startupRecovery.recoverInterruptedTasksOnStartup();

        verify(batchRecognitionService, times(2)).recoverInterruptedTasksOnStartup();
        assertEquals(List.of(1L, 122L), tenantIdsAtRecovery);
        assertNull(TenantContextHolder.getTenantId());
    }
}
