package cn.iocoder.yudao.module.infra.service.job;

import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobStartupSyncRunnerTest {

    @Test
    void run_syncsJobsToQuartzOnStartup() throws Exception {
        JobService jobService = mock(JobService.class);
        SchedulerManager schedulerManager = mock(SchedulerManager.class);
        when(schedulerManager.isEnabled()).thenReturn(true);
        JobStartupSyncRunner runner = new JobStartupSyncRunner(jobService, schedulerManager);

        runner.run(null);

        verify(jobService).syncJob();
    }

    @Test
    void run_skipsSyncWhenQuartzDisabled() throws Exception {
        JobService jobService = mock(JobService.class);
        SchedulerManager schedulerManager = mock(SchedulerManager.class);
        when(schedulerManager.isEnabled()).thenReturn(false);
        JobStartupSyncRunner runner = new JobStartupSyncRunner(jobService, schedulerManager);

        runner.run(null);

        verify(jobService, never()).syncJob();
    }

    @Test
    void run_propagatesSyncFailure() throws Exception {
        JobService jobService = mock(JobService.class);
        SchedulerManager schedulerManager = mock(SchedulerManager.class);
        when(schedulerManager.isEnabled()).thenReturn(true);
        SchedulerException failure = new SchedulerException("quartz unavailable");
        doThrow(failure).when(jobService).syncJob();
        JobStartupSyncRunner runner = new JobStartupSyncRunner(jobService, schedulerManager);

        SchedulerException thrown = assertThrows(SchedulerException.class, () -> runner.run(null));

        assertSame(failure, thrown);
    }

    @Test
    void run_skipsSyncWhenQuartzIsDisabled() throws Exception {
        JobService jobService = mock(JobService.class);
        SchedulerManager schedulerManager = mock(SchedulerManager.class);
        when(schedulerManager.isEnabled()).thenReturn(false);
        JobStartupSyncRunner runner = new JobStartupSyncRunner(jobService, schedulerManager);

        runner.run(null);

        verify(jobService, never()).syncJob();
    }

}
