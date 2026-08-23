package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/** Formal order-level Tx-A materialization before Flow-6 creates a batch execution. */
@TableName("mes_pro_process_pool_active_order_completion_backfill")
@KeySequence("mes_pro_process_pool_active_order_completion_backfill_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProcessPoolActiveOrderCompletionBackfillDO extends TenantBaseDO {

    public static final String TYPE_BATCH_RECORD = "BATCH_RECORD";
    public static final String TYPE_PROCESS_INSPECTION = "PROCESS_INSPECTION";
    public static final String TYPE_LOSS_REPORT = "LOSS_REPORT";

    @TableId
    private Long id;
    private Long activeOrderId;
    private Long workOrderId;
    private String backfillType;
    private String status;
    private String sourceIdsJson;
    private String sourceSnapshotHash;
    private String payloadJson;
    private LocalDateTime materializedAt;
    private Long materializedBy;
}
