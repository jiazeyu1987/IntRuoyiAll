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

@TableName("mes_pro_process_pool_active_order_release_application")
@KeySequence("mes_pro_process_pool_active_order_release_application_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolActiveOrderReleaseApplicationDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private Long routeId;
    private Long routeVersionId;
    private Long productId;
    private String batchCode;
    private Long batchExecutionId;
    private Long releaseTransactionId;
    private Long releaseApprovalWorkTaskId;
    private Long pqcReleaseWorkTaskId;
    private String pqcDecision;
    private Long pqcDecidedBy;
    private LocalDateTime pqcDecidedAt;
    private String pqcRejectReason;
    private String applicationStatus;
    private String sourceSnapshotHash;
    private String reportSnapshotHash;
    private Integer version;
    private String requestIdempotencyKey;
    private String businessIdempotencyKey;
    private String blockerSnapshotJson;
    private String dossierSummaryJson;
    private Long appliedBy;
    private LocalDateTime appliedAt;
    private LocalDateTime lastPrecheckAt;
    private String remark;
}
