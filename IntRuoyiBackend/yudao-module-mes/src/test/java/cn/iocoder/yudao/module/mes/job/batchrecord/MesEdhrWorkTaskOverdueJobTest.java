package cn.iocoder.yudao.module.mes.job.batchrecord;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskOverdueProcessResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesEdhrWorkTaskOverdueJobTest {

    @Test
    void execute_shouldDelegateToWorkTaskServiceWithDefaultLimit() throws Exception {
        MesProEdhrWorkTaskService workTaskService = mock(MesProEdhrWorkTaskService.class);
        when(workTaskService.processOverdueTasksWithSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(200)))
                .thenReturn(new MesProEdhrWorkTaskOverdueProcessResult(5, 3, 2, "CONCURRENT_STATUS_CHANGED"));
        MesEdhrWorkTaskOverdueJob job = new MesEdhrWorkTaskOverdueJob(workTaskService);

        String output = job.execute("");

        assertTrue(output.contains("scanned=5"));
        assertTrue(output.contains("overdue=3"));
        assertTrue(output.contains("skipped=2"));
        assertTrue(output.contains("skippedReason=CONCURRENT_STATUS_CHANGED"));
        verify(workTaskService).processOverdueTasksWithSummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(200));
    }

    @Test
    void execute_shouldFailFastWhenLimitParamInvalid() {
        MesProEdhrWorkTaskService workTaskService = mock(MesProEdhrWorkTaskService.class);
        MesEdhrWorkTaskOverdueJob job = new MesEdhrWorkTaskOverdueJob(workTaskService);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> job.execute("{\"limit\":0}"));

        assertTrue(exception.getMessage().contains("limit"));
    }
}
