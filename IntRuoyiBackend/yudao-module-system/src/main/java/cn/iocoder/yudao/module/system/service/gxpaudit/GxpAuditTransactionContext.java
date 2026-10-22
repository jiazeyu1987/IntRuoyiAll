package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

final class GxpAuditTransactionContext {

    private static final Object RESOURCE_KEY = GxpAuditTransactionContext.class;

    private GxpAuditTransactionContext() {
    }

    static String requireTransactionId() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("GxP audit append requires an active transaction");
        }
        Object existing = TransactionSynchronizationManager.getResource(RESOURCE_KEY);
        if (existing instanceof String transactionId) {
            return transactionId;
        }
        String transactionId = UUID.randomUUID().toString();
        TransactionSynchronizationManager.bindResource(RESOURCE_KEY, transactionId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void suspend() {
                if (!transactionId.equals(TransactionSynchronizationManager.getResource(RESOURCE_KEY))) {
                    throw new IllegalStateException("GxP transaction context does not belong to the suspended transaction");
                }
                TransactionSynchronizationManager.unbindResource(RESOURCE_KEY);
            }

            @Override
            public void resume() {
                // Spring suspends JDBC resources and synchronizations for REQUIRES_NEW separately.
                // This resource must follow its owning physical transaction as well.
                if (TransactionSynchronizationManager.hasResource(RESOURCE_KEY)) {
                    throw new IllegalStateException("GxP transaction context is occupied while resuming its owner");
                }
                TransactionSynchronizationManager.bindResource(RESOURCE_KEY, transactionId);
            }

            @Override
            public void afterCompletion(int status) {
                if (transactionId.equals(TransactionSynchronizationManager.getResource(RESOURCE_KEY))) {
                    TransactionSynchronizationManager.unbindResource(RESOURCE_KEY);
                }
            }
        });
        return transactionId;
    }
}
