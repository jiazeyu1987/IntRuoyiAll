package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReleaseWorkflowRecoverySchedulerTest {

    @Test
    void scheduledRecoveryInvokesWorkflowReconciliation() {
        ReleaseWorkflowOrchestrator orchestrator = mock(ReleaseWorkflowOrchestrator.class);
        when(orchestrator.reconcileActiveWorkflows(any(Instant.class))).thenReturn(List.of());
        when(orchestrator.recoverStaleWorkflows(any(Instant.class))).thenReturn(List.of());
        ReleaseWorkflowRecoveryScheduler scheduler = new ReleaseWorkflowRecoveryScheduler(orchestrator);

        scheduler.recoverStaleWorkflows();

        verify(orchestrator).reconcileActiveWorkflows(any(Instant.class));
        verify(orchestrator).recoverStaleWorkflows(any(Instant.class));
    }

    @Test
    void recoveryMethodIsScheduledWithDeterministicCadence() throws NoSuchMethodException {
        Method method = ReleaseWorkflowRecoveryScheduler.class.getDeclaredMethod("recoverStaleWorkflows");

        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals(60_000L, scheduled.initialDelay());
        assertEquals(30_000L, scheduled.fixedDelay());
    }
}
