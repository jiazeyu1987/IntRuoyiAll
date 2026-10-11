package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Recorded only after the real ledger mapper has obtained its row lock. */
final class GxpAuditLedgerLockContext {
    private static final Object KEY = GxpAuditLedgerLockContext.class;
    private record Reservation(Long tenantId, String operationId, String subjectId, String locator, String requestKey) { }
    private record Locks(String transactionId, Set<Long> tenants, Map<Long, Reservation> reservations) { }
    private GxpAuditLedgerLockContext() { }

    static void acquired(Long tenantId) {
        String transactionId = GxpAuditTransactionContext.requireTransactionId();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("GXP_LEDGER_LOCK_TRANSACTION_REQUIRED");
        }
        Object existing = TransactionSynchronizationManager.getResource(KEY);
        if (existing != null) {
            if (!(existing instanceof Locks locks) || !Objects.equals(locks.transactionId(), transactionId)) {
                throw new IllegalStateException("GXP_LEDGER_LOCK_OWNER_MISMATCH");
            }
            locks.tenants().add(tenantId);
            return;
        }
        Locks owned = new Locks(transactionId, new HashSet<>(Set.of(tenantId)), new HashMap<>());
        TransactionSynchronizationManager.bindResource(KEY, owned);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void suspend() {
                if (TransactionSynchronizationManager.getResource(KEY) != owned) {
                    throw new IllegalStateException("GXP_LEDGER_LOCK_OWNER_MISMATCH");
                }
                TransactionSynchronizationManager.unbindResource(KEY);
            }
            @Override public void resume() {
                if (TransactionSynchronizationManager.hasResource(KEY)) {
                    throw new IllegalStateException("GXP_LEDGER_LOCK_RESOURCE_OCCUPIED");
                }
                TransactionSynchronizationManager.bindResource(KEY, owned);
            }
            @Override public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.getResource(KEY) == owned) {
                    TransactionSynchronizationManager.unbindResource(KEY);
                }
            }
        });
    }

    static void requireHeld(Long tenantId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("GXP_LEDGER_LOCK_TRANSACTION_REQUIRED");
        }
        Object existing = TransactionSynchronizationManager.getResource(KEY);
        Object currentTransaction = TransactionSynchronizationManager.getResource(GxpAuditTransactionContext.class);
        if (!(existing instanceof Locks locks) || !Objects.equals(locks.transactionId(), currentTransaction)
                || !locks.tenants().contains(tenantId)) {
            throw new IllegalStateException("GXP_LEDGER_LOCK_REQUIRED");
        }
    }

    static Long reserve(Long tenantId, String operationId, String subjectId, String locator, String requestKey) {
        requireHeld(tenantId);
        if (operationId == null || operationId.isBlank() || subjectId == null || subjectId.isBlank()
                || locator == null || locator.isBlank() || requestKey == null || requestKey.isBlank()) {
            throw new IllegalArgumentException("GXP_EVENT_RESERVATION_SCOPE_REQUIRED");
        }
        Long id = com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
        if (id <= 0) throw new IllegalStateException("GXP_EVENT_RESERVATION_ID_INVALID");
        Locks locks = (Locks) TransactionSynchronizationManager.getResource(KEY);
        if (locks.reservations().putIfAbsent(id,
                new Reservation(tenantId, operationId, subjectId, locator, requestKey)) != null) {
            throw new IllegalStateException("GXP_EVENT_RESERVATION_COLLISION");
        }
        return id;
    }

    static void consume(Long tenantId, GxpAuditCommand command) {
        if (command.getReservedEventId() == null) return;
        requireHeld(tenantId);
        Locks locks = (Locks) TransactionSynchronizationManager.getResource(KEY);
        Reservation actual = locks.reservations().get(command.getReservedEventId());
        Reservation expected = new Reservation(tenantId, command.getOperationId(), command.getSubjectId(),
                command.getSourceLocator(), command.getIdempotencyKey());
        if (!expected.equals(actual)) throw new IllegalStateException("GXP_EVENT_RESERVATION_SCOPE_MISMATCH");
        locks.reservations().remove(command.getReservedEventId());
    }
}
