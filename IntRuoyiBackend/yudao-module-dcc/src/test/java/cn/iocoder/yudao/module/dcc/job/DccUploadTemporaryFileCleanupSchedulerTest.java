package cn.iocoder.yudao.module.dcc.job;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccUploadTemporaryFileCleanupSchedulerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccUploadTemporaryFileCleanupScheduler scheduler;

    @Mock
    private TenantFrameworkService tenantFrameworkService;
    @Mock
    private DccUploadTicketService uploadTicketService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void schedule_cleansExpiredTemporaryFilesForEachTenant() throws Exception {
        when(tenantFrameworkService.getTenantIds()).thenReturn(List.of(122L, 123L));

        scheduler.schedule();

        verify(uploadTicketService, times(2)).cleanupExpiredTemporaryFiles(
                any(LocalDateTime.class), eq(DccUploadTemporaryFileCleanupScheduler.CLEANUP_BATCH_SIZE));
    }

    @Test
    void schedule_failsFastWhenOneTenantCleanupFails() throws Exception {
        when(tenantFrameworkService.getTenantIds()).thenReturn(List.of(122L, 123L));
        doThrow(new Exception("delete failed")).when(uploadTicketService).cleanupExpiredTemporaryFiles(
                any(LocalDateTime.class), eq(DccUploadTemporaryFileCleanupScheduler.CLEANUP_BATCH_SIZE));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> scheduler.schedule());

        assertTrue(ex.getMessage().contains("DCC upload temporary cleanup failed"));
        verify(uploadTicketService, times(1)).cleanupExpiredTemporaryFiles(
                any(LocalDateTime.class), eq(DccUploadTemporaryFileCleanupScheduler.CLEANUP_BATCH_SIZE));
    }

}
