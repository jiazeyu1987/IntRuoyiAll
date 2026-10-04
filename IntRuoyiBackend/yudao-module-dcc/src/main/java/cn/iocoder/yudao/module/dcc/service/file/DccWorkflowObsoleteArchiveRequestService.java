package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;

/** Physical archival is a separate, recoverable operation after the obsolete transaction commits. */
@Service
public class DccWorkflowObsoleteArchiveRequestService {
    private final JdbcTemplate jdbc;
    public DccWorkflowObsoleteArchiveRequestService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void request(Long fileId, LocalDateTime obsoleteTime) {
        if(!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("作废归档请求必须与作废事实同事务保存");
        int rows=jdbc.update("""
                INSERT INTO dcc_workflow_obsolete_archive(tenant_id,controlled_file_id,obsolete_time,status)
                VALUES(?,?,?,'PENDING')
                """,TenantContextHolder.getRequiredTenantId(),fileId,obsoleteTime);
        if(rows!=1) throw new IllegalStateException("作废归档请求保存失败");
    }
}
