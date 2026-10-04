package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccRevisionRollbackSynchronizationTest {
    private final DccControlledFileSourceOwnershipService ownership=mock(DccControlledFileSourceOwnershipService.class);
    private final DccControlledFilePreparedSource frozen=new DccControlledFilePreparedSource(300L,100L,"hash",true);
    private final Runnable cleanup=mock(Runnable.class);
    @BeforeEach void setup() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        when(ownership.rollbackCleanup(frozen)).thenReturn(cleanup);
        DccControlledFileVersionSourceRollback.enlist(ownership,frozen);
    }
    @AfterEach void clear() { TransactionSynchronizationManager.clear(); }
    @Test void unknownCommitOutcomeCannotDeletePossiblyCommittedBody() {
        var callback=TransactionSynchronizationManager.getSynchronizations().get(0);
        assertThrows(IllegalStateException.class,() -> callback.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN));
        verify(cleanup,never()).run();
    }
    @Test void confirmedRollbackCleansTheIsolatedBody() {
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(cleanup).run();
    }
    @Test void committedBodyRemainsOwned() {
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        verify(cleanup,never()).run();
    }
}
