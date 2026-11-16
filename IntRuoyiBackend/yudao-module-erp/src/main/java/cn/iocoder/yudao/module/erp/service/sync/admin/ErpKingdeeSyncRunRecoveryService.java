package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.enums.kingdeeautosync.ErpKingdeeTableAutoSyncTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.autoconfigure.quartz.JobStoreType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Explicit recovery of an interrupted run; never infer failure from elapsed time or zero counts. */
@Service
@RequiredArgsConstructor
public class ErpKingdeeSyncRunRecoveryService {

    private final ErpKingdeeSyncRunMapper runMapper;
    private final JdbcTemplate jdbcTemplate;
    private final QuartzProperties quartzProperties;
    private final org.redisson.api.RedissonClient redisson;

    public ErpKingdeeSyncRunDO getRun(Long runId) {
        TenantContextHolder.getRequiredTenantId();
        return runMapper.selectById(runId);
    }

    /** False means stop was requested, but execution has not exited yet. Never delete early. */
    @Transactional(rollbackFor = Exception.class)
    public boolean stopAndDelete(Long runId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpKingdeeSyncRunDO run = runMapper.selectById(runId);
        if (run == null) return true; // An earlier acknowledged delete may already have completed.
        String prefix = quartzTablePrefix();
        String handler = ErpKingdeeTableAutoSyncTypeEnum.requiredOf(run.getSyncType()).getHandlerName();
        // Durable stop requests are observed on the node that owns the executing thread.
        jdbcTemplate.update("UPDATE erp_kingdee_sync_run SET status = 40, failure_message = ?, update_time = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND tenant_id = ? AND status = 10 AND deleted = 0",
                "正在停止同步任务，停止完成后删除运行记录", runId, tenantId);
        if (redisson.getLock(cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCancellation
                .lockKey(tenantId, run.getSyncType())).isLocked()) return false;
        return jdbcTemplate.update("UPDATE erp_kingdee_sync_run SET deleted = 1, update_time = CURRENT_TIMESTAMP, "
                        + "ended_at = COALESCE(ended_at, CURRENT_TIMESTAMP), "
                        + "failure_message = CASE WHEN status = 40 THEN '任务已停止并按用户请求删除运行记录' ELSE failure_message END, "
                        + "status = CASE WHEN status = 40 THEN 30 ELSE status END "
                        + "WHERE id = ? AND tenant_id = ? AND status <> 10 AND deleted = 0 "
                        + "AND NOT EXISTS (SELECT 1 FROM " + prefix + "FIRED_TRIGGERS WHERE JOB_NAME = ?)",
                runId, tenantId, handler) == 1;
    }

    private String quartzTablePrefix() {
        if (quartzProperties.getJobStoreType() != JobStoreType.JDBC) {
            throw new IllegalStateException("无法核验集群执行状态：Quartz 必须使用 JDBC 存储");
        }
        String prefix = quartzProperties.getProperties().getOrDefault("org.quartz.jobStore.tablePrefix", "QRTZ_");
        if (!prefix.matches("[A-Za-z0-9_]+")) throw new IllegalStateException("无法核验集群执行状态：不支持的 Quartz 表前缀");
        return prefix;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recover(Long runId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpKingdeeSyncRunDO run = runMapper.selectById(runId);
        if (run == null || !Integer.valueOf(10).equals(run.getStatus())) {
            throw new IllegalStateException("记录不存在或已结束，请刷新列表");
        }
        String prefix = quartzTablePrefix();
        String handler = ErpKingdeeTableAutoSyncTypeEnum.requiredOf(run.getSyncType()).getHandlerName();
        if (redisson.getLock(cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCancellation
                .lockKey(tenantId, run.getSyncType())).isLocked()) throw new IllegalStateException("任务仍有执行实例，不能结束记录");
        // Cross-cluster check and tenant-scoped state change are one SQL statement. Keep every counter.
        int changed = jdbcTemplate.update("UPDATE erp_kingdee_sync_run SET status = 30, ended_at = CURRENT_TIMESTAMP, "
                        + "update_time = CURRENT_TIMESTAMP, failure_message = ? "
                        + "WHERE id = ? AND tenant_id = ? AND status = 10 AND deleted = 0 "
                        + "AND NOT EXISTS (SELECT 1 FROM " + prefix + "FIRED_TRIGGERS WHERE JOB_NAME = ?)",
                "同步已中断：管理员核验 Quartz 集群无执行实例后结束记录；已提交计数保留，可重新全量同步", runId, tenantId, handler);
        if (changed != 1) {
            throw new IllegalStateException("任务仍有调度执行实例，或记录状态已变化；未结束记录，请刷新后检查");
        }
    }
}
