package cn.iocoder.yudao.module.dcc.service.file;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Keeps isolated source copies owned by the transaction that allocates the version. */
final class DccControlledFileVersionSourceRollback {
    private DccControlledFileVersionSourceRollback() {}

    static void enlist(DccControlledFileSourceOwnershipService ownership, DccControlledFilePreparedSource... sources) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("version source ownership requires an active transaction synchronization");
        }
        var cleanups=java.util.Arrays.stream(sources).filter(source->source!=null && source.isolatedCopy())
                .map(ownership::rollbackCleanup).toList();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) return;
                if (status != STATUS_ROLLED_BACK) {
                    throw new IllegalStateException("transaction completion is unknown; frozen source must be retained for reconciliation");
                }
                RuntimeException cleanupFailure = null;
                for (var cleanup : cleanups) {
                    try { cleanup.run(); }
                    catch (RuntimeException failure) {
                        if (cleanupFailure == null) cleanupFailure = failure;
                        else cleanupFailure.addSuppressed(failure);
                    }
                }
                if (cleanupFailure != null) throw cleanupFailure;
            }
        });
    }
}
