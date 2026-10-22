package cn.iocoder.yudao.module.system.service.signature;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.HashSet;
import java.util.Set;

/** Proof registered only after a real SYSTEM identity FOR UPDATE in this transaction. */
public final class SignatureIdentityLockContext {
    private static final Object RESOURCE = new Object();
    private record Identity(Long tenantId, Long actorId) {}
    private SignatureIdentityLockContext() {}

    public static void registerSystemIdentityLock(Long tenantId, Long actorId) {
        requireScope(tenantId, actorId);
        Set<Identity> locked = existing();
        if (locked == null) {
            Set<Identity> owned = new HashSet<>();
            TransactionSynchronizationManager.bindResource(RESOURCE, owned);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void suspend() {
                    if (TransactionSynchronizationManager.getResource(RESOURCE) != owned)
                        throw new IllegalStateException("SIGNATURE_IDENTITY_LOCK_OWNER_MISMATCH");
                    TransactionSynchronizationManager.unbindResource(RESOURCE);
                }
                @Override public void resume() {
                    if (TransactionSynchronizationManager.hasResource(RESOURCE))
                        throw new IllegalStateException("SIGNATURE_IDENTITY_LOCK_RESOURCE_OCCUPIED");
                    TransactionSynchronizationManager.bindResource(RESOURCE, owned);
                }
                @Override public void afterCompletion(int status) {
                    if (TransactionSynchronizationManager.getResource(RESOURCE) == owned)
                        TransactionSynchronizationManager.unbindResource(RESOURCE);
                }
            });
            locked = owned;
        }
        locked.add(new Identity(tenantId, actorId));
    }

    public static void requireSystemIdentityLock(Long actorId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        requireScope(tenantId, actorId);
        Set<Identity> locked = existing();
        if (locked == null || !locked.contains(new Identity(tenantId, actorId)))
            throw new IllegalStateException("SIGNATURE_IDENTITY_LOCK_REQUIRED");
    }

    private static void requireScope(Long tenantId, Long actorId) {
        if (tenantId == null || tenantId <= 0 || actorId == null || actorId <= 0
                || !tenantId.equals(TenantContextHolder.getRequiredTenantId()))
            throw new IllegalArgumentException("SIGNATURE_IDENTITY_LOCK_SCOPE_MISMATCH");
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive())
            throw new IllegalStateException("SIGNATURE_IDENTITY_TRANSACTION_REQUIRED");
    }

    @SuppressWarnings("unchecked")
    private static Set<Identity> existing() {
        return (Set<Identity>) TransactionSynchronizationManager.getResource(RESOURCE);
    }
}
