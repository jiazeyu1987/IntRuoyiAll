package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesFixedTestOrderDownstreamCleanupMapper;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesFixedTestOrderBpmCancellationServiceTest {
    private final BpmProcessInstanceService bpm = mock(BpmProcessInstanceService.class);

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void cancelsRunningProcessThroughFormalAdminServiceAndVerifiesItEnded() throws Exception {
        ProcessInstance running = mock(ProcessInstance.class);
        when(running.getTenantId()).thenReturn("1");
        when(bpm.getProcessInstance("process-1")).thenReturn(running, null);
        cancel(service(List.of("process-1")));
        verify(bpm).cancelProcessInstanceByAdmin(eq(91L), argThat(req -> req.getId().equals("process-1")
                && req.getReason().contains("固定测试订单重置")));
        verify(bpm, times(2)).getProcessInstance("process-1");
    }

    @Test
    void alreadyEndedProcessIsRetainedWithoutCancellation() throws Exception {
        cancel(service(List.of("process-1")));
        verify(bpm, never()).cancelProcessInstanceByAdmin(any(), any());
    }

    @Test
    void crossTenantRunningProcessRejectsBeforeCancellation() throws Exception {
        ProcessInstance running = mock(ProcessInstance.class);
        when(running.getTenantId()).thenReturn("2");
        when(bpm.getProcessInstance("process-1")).thenReturn(running);
        assertThrows(IllegalStateException.class, () -> cancel(service(List.of("process-1"))));
        verify(bpm, never()).cancelProcessInstanceByAdmin(any(), any());
    }

    @Test
    void processStillRunningAfterCancelFailsWholeReset() throws Exception {
        ProcessInstance running = mock(ProcessInstance.class);
        when(running.getTenantId()).thenReturn("1");
        when(bpm.getProcessInstance("process-1")).thenReturn(running);
        assertThrows(IllegalStateException.class, () -> cancel(service(List.of("process-1"))));
    }

    private Object service(List<String> processIds) throws Exception {
        TenantContextHolder.setTenantId(1L);
        var mapper = mock(MesFixedTestOrderDownstreamCleanupMapper.class, call -> {
            if (call.getMethod().getName().equals("selectProcessInstanceIds")) return processIds;
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
        });
        return Class.forName("cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderBpmCancellationService")
                .getConstructor(MesFixedTestOrderDownstreamCleanupMapper.class, BpmProcessInstanceService.class)
                .newInstance(mapper, bpm);
    }

    private void cancel(Object service) throws Exception {
        try {
            service.getClass().getMethod("cancel", Long.class, List.class, List.class, Long.class)
                    .invoke(service, 1L, List.of(301L), List.of(401L), 91L);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }
}
