package cn.iocoder.yudao.module.dcc.job;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBatchRecognitionService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DccWorktreeBackgroundConditionTest {
    @Test void explicitlyDisabledBatchRecoveryDoesNotRegisterOrTouchAnyTenant() {
        var tenants = mock(TenantFrameworkService.class);
        when(tenants.getTenantIds()).thenReturn(List.of(1L, 2L));
        var service = mock(DccControlledFileBatchRecognitionService.class);
        batchContext(tenants, service).withPropertyValues("yudao.local-job-control.dcc-batch-recognition-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(DccControlledFileBatchRecognitionStartupRecovery.class);
                    verifyNoInteractions(service);
                    verify(tenants, never()).getTenantIds();
                });
    }

    @Test void enabledAndUnspecifiedBatchRecoveryKeepExistingPerTenantStartupBehavior() {
        for (boolean explicit : List.of(false, true)) {
            var tenants = mock(TenantFrameworkService.class);
            when(tenants.getTenantIds()).thenReturn(List.of(1L, 2L));
            var service = mock(DccControlledFileBatchRecognitionService.class);
            var runner = batchContext(tenants, service);
            if (explicit) runner = runner.withPropertyValues("yudao.local-job-control.dcc-batch-recognition-enabled=true");
            runner.run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(DccControlledFileBatchRecognitionStartupRecovery.class);
                verify(service, times(2)).recoverInterruptedTasksOnStartup();
                verify(tenants, times(1)).getTenantIds();
            });
        }
    }

    @Test void explicitlyDisabledTemporaryCleanupDoesNotRegisterOrTouchAnyTenant() {
        var tenants = mock(TenantFrameworkService.class);
        when(tenants.getTenantIds()).thenReturn(List.of(1L, 2L));
        var tickets = mock(DccUploadTicketService.class);
        cleanupContext(tenants, tickets).withPropertyValues("yudao.local-job-control.dcc-upload-temporary-cleanup-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(DccUploadTemporaryFileCleanupScheduler.class);
                    verifyNoInteractions(tickets);
                    verify(tenants, never()).getTenantIds();
                });
    }

    @Test void enabledAndUnspecifiedTemporaryCleanupPreserveActualExistingCleanupCall() {
        for (boolean explicit : List.of(false, true)) {
            var tenants = mock(TenantFrameworkService.class);
            when(tenants.getTenantIds()).thenReturn(List.of(1L, 2L));
            var tickets = mock(DccUploadTicketService.class);
            var runner = cleanupContext(tenants, tickets);
            if (explicit) runner = runner.withPropertyValues("yudao.local-job-control.dcc-upload-temporary-cleanup-enabled=true");
            runner.run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(DccUploadTemporaryFileCleanupScheduler.class);
                context.getBean(DccUploadTemporaryFileCleanupScheduler.class).schedule();
                verify(tickets, times(2)).cleanupExpiredTemporaryFiles(any(LocalDateTime.class), eq(DccUploadTemporaryFileCleanupScheduler.CLEANUP_BATCH_SIZE));
            });
        }
    }

    private ApplicationContextRunner batchContext(TenantFrameworkService tenants, DccControlledFileBatchRecognitionService service) {
        return new ApplicationContextRunner().withUserConfiguration(DccControlledFileBatchRecognitionStartupRecovery.class)
                .withBean(TenantFrameworkService.class, () -> tenants)
                .withBean(DccControlledFileBatchRecognitionService.class, () -> service);
    }

    private ApplicationContextRunner cleanupContext(TenantFrameworkService tenants, DccUploadTicketService tickets) {
        return new ApplicationContextRunner().withUserConfiguration(DccUploadTemporaryFileCleanupScheduler.class)
                .withBean(TenantFrameworkService.class, () -> tenants)
                .withBean(DccUploadTicketService.class, () -> tickets);
    }
}
