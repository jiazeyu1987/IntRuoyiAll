package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class DccPublicationNotificationPostCommitSchedulerTest {

    @AfterEach
    void cleanupSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void scheduleAfterCommit_withoutPublicationTransactionFailsFast() {
        DccPublicationNotificationPostCommitScheduler scheduler = scheduler(mock(
                DccPublicationNotificationDispatchOrchestrator.class));

        assertThrows(IllegalStateException.class, () -> scheduler.scheduleAfterCommit(1L, 7L));
    }

    @Test
    void afterCommit_dispatchInfrastructureFailureDoesNotPropagateBackToPublicationCaller() {
        DccPublicationNotificationDispatchOrchestrator orchestrator = mock(
                DccPublicationNotificationDispatchOrchestrator.class);
        doThrow(new IllegalStateException("injected batch query failure"))
                .when(orchestrator).dispatchBatch(1L, 7L);
        DccPublicationNotificationPostCommitScheduler scheduler = scheduler(orchestrator);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        scheduler.scheduleAfterCommit(1L, 7L);
        TransactionSynchronization callback = TransactionSynchronizationManager.getSynchronizations().get(0);

        assertDoesNotThrow(callback::afterCommit);
    }

    private DccPublicationNotificationPostCommitScheduler scheduler(
            DccPublicationNotificationDispatchOrchestrator orchestrator) {
        DccPublicationNotificationPostCommitScheduler scheduler =
                new DccPublicationNotificationPostCommitScheduler();
        ReflectionTestUtils.setField(scheduler, "orchestrator", orchestrator);
        return scheduler;
    }
}
