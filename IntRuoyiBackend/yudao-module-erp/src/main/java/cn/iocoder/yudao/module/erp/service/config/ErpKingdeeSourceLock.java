package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Serializes source changes against all synchronization work across nodes. */
@Component
@RequiredArgsConstructor
public class ErpKingdeeSourceLock {
    private final RedissonClient redisson;
    public RLock acquire() {
        RLock lock = redisson.getLock("erp:kingdee:source:" + TenantContextHolder.getRequiredTenantId());
        if (!lock.tryLock()) throw new IllegalStateException("金蝶同步或连接修改正在进行，请完成后重试");
        return lock;
    }
    public void releaseAfterTransaction(RLock lock) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { lock.unlock(); }
            });
        } else lock.unlock();
    }
}
