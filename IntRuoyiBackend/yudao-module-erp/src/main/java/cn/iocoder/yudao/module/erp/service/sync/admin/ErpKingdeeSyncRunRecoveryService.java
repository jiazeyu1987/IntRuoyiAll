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

    @Transactional(rollbackFor = Exception.class)
    public void recover(Long runId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpKingdeeSyncRunDO run = runMapper.selectById(runId);
        if (run == null || !Integer.valueOf(10).equals(run.getStatus())) {
            throw new IllegalStateException("记录不存在或已结束，请刷新列表");
        }
        if (quartzProperties.getJobStoreType() != JobStoreType.JDBC) {
            throw new IllegalStateException("无法核验集群执行状态：Quartz 必须使用 JDBC 存储");
        }
        String prefix = quartzProperties.getProperties()
                .getOrDefault("org.quartz.jobStore.tablePrefix", "QRTZ_");
        if (!prefix.matches("[A-Za-z0-9_]+")) {
            throw new IllegalStateException("无法核验集群执行状态：不支持的 Quartz 表前缀");
        }
        String handler = ErpKingdeeTableAutoSyncTypeEnum.requiredOf(run.getSyncType()).getHandlerName();
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
