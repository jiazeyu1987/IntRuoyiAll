package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.ApprovalAuthorityRevisionDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.ApprovalAuthorityRevisionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class ApprovalAuthorityService implements ApprovalAuthorityApi {
    private static final Object LOCKS_KEY = new Object();
    private final ApprovalAuthorityRevisionMapper mapper;
    private final Clock clock;

    public ApprovalAuthorityService(ApprovalAuthorityRevisionMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public ApprovalAuthorityRevisionDTO lockTenantApprovalAuthority(Long tenantId) {
        requireTenant(tenantId);
        ApprovalAuthorityRevisionDO row = mapper.selectForUpdate(tenantId);
        if (row == null || !Objects.equals(tenantId, row.getTenantId())) {
            throw new IllegalStateException("APPROVAL_AUTHORITY_NOT_INITIALIZED");
        }
        var revision = new ApprovalAuthorityRevisionDTO(tenantId, row.getRevision());
        locks().put(tenantId, revision.revision());
        return revision;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void incrementLockedAuthorityRevision(Long tenantId, Long expectedRevision) {
        requireTenant(tenantId);
        var revision = new ApprovalAuthorityRevisionDTO(tenantId, expectedRevision);
        Map<Long, Long> locks = existingLocks();
        if (locks == null || !Objects.equals(locks.get(tenantId), expectedRevision)) {
            throw new IllegalStateException("APPROVAL_AUTHORITY_LOCK_REQUIRED");
        }
        long next = Math.addExact(revision.revision(), 1L);
        var now = LocalDateTime.ofInstant(clock.instant().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC);
        if (mapper.increment(tenantId, expectedRevision, now) != 1) {
            throw new IllegalStateException("APPROVAL_AUTHORITY_REVISION_CONFLICT");
        }
        locks.put(tenantId, next);
    }

    private static void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0
                || !Objects.equals(tenantId, TenantContextHolder.getRequiredTenantId())) {
            throw new IllegalArgumentException("APPROVAL_AUTHORITY_TENANT_MISMATCH");
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("APPROVAL_AUTHORITY_TRANSACTION_REQUIRED");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, Long> existingLocks() {
        return (Map<Long, Long>) TransactionSynchronizationManager.getResource(LOCKS_KEY);
    }

    private static Map<Long, Long> locks() {
        Map<Long, Long> existing = existingLocks();
        if (existing != null) return existing;
        Map<Long, Long> owned = new HashMap<>();
        TransactionSynchronizationManager.bindResource(LOCKS_KEY, owned);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void suspend() {
                if (TransactionSynchronizationManager.getResource(LOCKS_KEY) != owned) {
                    throw new IllegalStateException("APPROVAL_AUTHORITY_TRANSACTION_OWNER_MISMATCH");
                }
                TransactionSynchronizationManager.unbindResource(LOCKS_KEY);
            }
            @Override public void resume() {
                if (TransactionSynchronizationManager.hasResource(LOCKS_KEY)) {
                    throw new IllegalStateException("APPROVAL_AUTHORITY_TRANSACTION_RESOURCE_OCCUPIED");
                }
                TransactionSynchronizationManager.bindResource(LOCKS_KEY, owned);
            }
            @Override public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.getResource(LOCKS_KEY) == owned) {
                    TransactionSynchronizationManager.unbindResource(LOCKS_KEY);
                }
            }
        });
        return owned;
    }
}
