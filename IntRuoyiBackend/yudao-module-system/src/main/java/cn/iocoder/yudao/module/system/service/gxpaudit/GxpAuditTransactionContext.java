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
            public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.hasResource(RESOURCE_KEY)) {
                    TransactionSynchronizationManager.unbindResourceIfPossible(RESOURCE_KEY);
                }
            }
        });
        return transactionId;
    }
}
