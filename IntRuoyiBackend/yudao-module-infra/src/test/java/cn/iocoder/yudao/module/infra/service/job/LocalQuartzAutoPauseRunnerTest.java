package cn.iocoder.yudao.module.infra.service.job;

import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobMapper;
import cn.iocoder.yudao.module.infra.enums.job.JobStatusEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalQuartzAutoPauseRunnerTest {

    @Test
    void run_pausesNormalQuartzJobsOutsideAllowlist() throws Exception {
        JobMapper jobMapper = mock(JobMapper.class);
        SchedulerManager schedulerManager = mock(SchedulerManager.class);
        LocalJobControlProperties properties = new LocalJobControlProperties();
        properties.setQuartzAutoRunHandlerWhitelist(List.of("kingdeeProductionOrderSyncJob"));
        when(schedulerManager.isEnabled()).thenReturn(true);
        when(jobMapper.selectList()).thenReturn(List.of(
                job(5600L, "kingdeeProductionOrderSyncJob", JobStatusEnum.NORMAL.getStatus(), "0 0 2 * * ?"),
                job(5616L, "mesProNightlyReplanJob", JobStatusEnum.NORMAL.getStatus(), "0 0 2 * * ?"),
                job(5611L, "runtimeNightlyReleaseJob", JobStatusEnum.STOP.getStatus(), "0 0 2 * * ?")
        ));
        LocalQuartzAutoPauseRunner runner = new LocalQuartzAutoPauseRunner(jobMapper, schedulerManager, properties);

        runner.run(null);

        verify(schedulerManager).pauseJob("mesProNightlyReplanJob");
        verify(schedulerManager, never()).pauseJob("kingdeeProductionOrderSyncJob");
        verify(schedulerManager, never()).pauseJob("runtimeNightlyReleaseJob");
    }

    @Test
    void run_skipsContainmentWhenDisabled() throws Exception {
        JobMapper jobMapper = mock(JobMapper.class);
        SchedulerManager schedulerManager = mock(SchedulerManager.class);
        LocalJobControlProperties properties = new LocalJobControlProperties();
        properties.setEnabled(false);
        when(schedulerManager.isEnabled()).thenReturn(true);
        LocalQuartzAutoPauseRunner runner = new LocalQuartzAutoPauseRunner(jobMapper, schedulerManager, properties);

        runner.run(null);

        verify(jobMapper, never()).selectList();
    }

    @Test
    void run_skipsContainmentWhenQuartzDisabled() throws Exception {
        JobMapper jobMapper = mock(JobMapper.class);
        SchedulerManager schedulerManager = mock(SchedulerManager.class);
        LocalJobControlProperties properties = new LocalJobControlProperties();
        when(schedulerManager.isEnabled()).thenReturn(false);
        LocalQuartzAutoPauseRunner runner = new LocalQuartzAutoPauseRunner(jobMapper, schedulerManager, properties);

        runner.run(null);

        verify(jobMapper, never()).selectList();
    }

    private static JobDO job(Long id, String handlerName, Integer status, String cronExpression) {
        return JobDO.builder()
                .id(id)
                .handlerName(handlerName)
                .status(status)
                .cronExpression(cronExpression)
                .build();
    }
}
