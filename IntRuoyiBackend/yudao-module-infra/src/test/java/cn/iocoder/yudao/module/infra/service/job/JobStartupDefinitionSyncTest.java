package cn.iocoder.yudao.module.infra.service.job;

import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class JobStartupDefinitionSyncTest {
    private final JobMapper mapper = mock(JobMapper.class);
    private final SchedulerManager scheduler = mock(SchedulerManager.class);
    private final JobServiceImpl service = new JobServiceImpl();
    private final JobDO job = JobDO.builder().id(7L).handlerName("handler").handlerParam("input")
            .cronExpression("0 0 12 * * ?").retryCount(2).retryInterval(3).status(1).build();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "jobMapper", mapper);
        ReflectionTestUtils.setField(service, "schedulerManager", scheduler);
        when(mapper.selectList()).thenReturn(List.of(job));
    }

    @Test
    void identicalDefinitionDoesNotWrite() throws Exception {
        when(scheduler.matchesJob(7L,"handler","input","0 0 12 * * ?",2,3,false)).thenReturn(true);
        service.syncJob();
        verify(scheduler).matchesJob(7L,"handler","input","0 0 12 * * ?",2,3,false);
        verifyNoMoreInteractions(scheduler);
        verify(mapper).selectList();
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void mismatchRetainsOriginalSynchronization() throws Exception {
        service.syncJob();
        verify(scheduler).deleteJob("handler");
        verify(scheduler).addJob(7L,"handler","input","0 0 12 * * ?",2,3);
    }

    @Test
    void stoppedMismatchRetainsPause() throws Exception {
        job.setStatus(2);
        service.syncJob();
        verify(scheduler).deleteJob("handler");
        verify(scheduler).addJob(7L,"handler","input","0 0 12 * * ?",2,3);
        verify(scheduler).pauseJob("handler");
    }

    @Test
    void definitionReadFailurePropagates() throws Exception {
        when(scheduler.matchesJob(7L,"handler","input","0 0 12 * * ?",2,3,false)).thenThrow(new SchedulerException("read failed"));
        assertThrows(SchedulerException.class, service::syncJob);
        verify(scheduler,never()).deleteJob(anyString());
    }
}
