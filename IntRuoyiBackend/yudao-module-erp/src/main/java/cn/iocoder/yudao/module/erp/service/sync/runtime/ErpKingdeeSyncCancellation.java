package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CancellationException;

/** Each node observes durable stop requests for only its own executing threads. */
@Component
@RequiredArgsConstructor
public class ErpKingdeeSyncCancellation {
    private final ErpKingdeeSyncRunMapper runMapper;
    private final org.redisson.api.RedissonClient redisson;

    public static String lockKey(Long tenantId, String syncType) {
        return "erp:kingdee:execution:" + tenantId + ":" + syncType;
    }

    public void lock(String syncType) {
        if (!redisson.getLock(lockKey(TenantContextHolder.getRequiredTenantId(), syncType)).tryLock()) {
            throw new IllegalStateException("该同步任务仍在执行或停止中");
        }
    }

    public void unlock(String syncType) {
        redisson.getLock(lockKey(TenantContextHolder.getRequiredTenantId(), syncType)).unlock();
    }
    private final ConcurrentHashMap<Long, Execution> executions = new ConcurrentHashMap<>();

    public void register(Long runId) {
        executions.put(runId, new Execution(TenantContextHolder.getRequiredTenantId(), Thread.currentThread()));
    }

    public boolean isRequested(Long runId) {
        Execution execution = executions.get(runId);
        if (execution != null && execution.requested) return true;
        ErpKingdeeSyncRunDO run = runMapper.selectById(runId);
        return run != null && Integer.valueOf(40).equals(run.getStatus());
    }

    public void check(Long runId) {
        if (isRequested(runId)) throw new CancellationException("同步任务已按删除请求停止");
    }

    public void unregister(Long runId) {
        Execution execution = executions.get(runId);
        if (execution != null) synchronized (execution) {
            executions.remove(runId, execution);
            // Do not leak our cancellation signal into the Quartz pooled thread's next job.
            if (execution.requested) Thread.interrupted();
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void interruptRequestedExecutions() {
        executions.forEach((id, execution) -> TenantUtils.execute(execution.tenantId, () -> {
            ErpKingdeeSyncRunDO run = runMapper.selectById(id);
            synchronized (execution) {
                if (executions.get(id) == execution && run != null && Integer.valueOf(40).equals(run.getStatus()) && !execution.requested) {
                    execution.requested = true;
                    execution.thread.interrupt();
                }
            }
        }));
    }

    private static final class Execution {
        private final Long tenantId;
        private final Thread thread;
        private volatile boolean requested;
        private Execution(Long tenantId, Thread thread) { this.tenantId = tenantId; this.thread = thread; }
    }
}
