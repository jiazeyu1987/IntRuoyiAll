package cn.iocoder.yudao.module.mes.job.schedule;

import cn.iocoder.yudao.module.mes.service.pro.schedule.MesProNightlyReplanResult;
import cn.iocoder.yudao.module.mes.service.pro.schedule.MesProNightlyReplanService;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesProNightlyReplanJobTest {

    @Test
    void execute_shouldDelegateToNightlyReplanServiceAndReturnJobMessage() {
        MesProNightlyReplanService nightlyReplanService = mock(MesProNightlyReplanService.class);
        MesProNightlyReplanResult result = new MesProNightlyReplanResult();
        result.setScheduleOrderCount(1);
        result.setGeneratedTaskCount(2);
        result.setPreservedTaskCount(1);
        when(nightlyReplanService.executeNightlyReplan(any(LocalDateTime.class))).thenReturn(result);
        MesProNightlyReplanJob job = new MesProNightlyReplanJob(nightlyReplanService);

        String message = job.execute("");

        assertEquals("夜间重排完成：排产工单 1，生成任务 2，保护任务 1，阻塞 0，短缺 0", message);
        verify(nightlyReplanService).executeNightlyReplan(any(LocalDateTime.class));
    }

    @Test
    void execute_shouldExposeHandlerBeanNameForInfraJobCronRegistration() {
        Component component = MesProNightlyReplanJob.class.getAnnotation(Component.class);
        assertNotNull(component);
        assertEquals("mesProNightlyReplanJob", component.value());
    }
}
