package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_pro_process_pool_active_order_version_upgrade_request")
@KeySequence("mes_pro_process_pool_active_order_version_upgrade_request_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolActiveOrderVersionUpgradeRequestDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long sourceActiveOrderId;
    private Long sourceWorkOrderId;
    private Long sourceBatchExecutionId;
    private Long targetActiveOrderId;
    private Long targetBatchExecutionId;
    private String requestCode;
    private String idempotencyKey;
    private String requestStatus;
    private String approvalStatus;
    private String freezeStatus;
    private String approvalProcessInstanceId;
    private String upgradeReason;
    private String currentSnapshotJson;
    private String targetSnapshotJson;
    private String snapshotHash;
    private Long requestedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime appliedAt;
    private LocalDateTime cancelledAt;
    private String resultMessage;
}
