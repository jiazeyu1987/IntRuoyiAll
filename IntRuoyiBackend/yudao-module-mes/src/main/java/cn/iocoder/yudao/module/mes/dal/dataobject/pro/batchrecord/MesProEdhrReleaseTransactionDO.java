package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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

@TableName("mes_pro_edhr_release_transaction")
@KeySequence("mes_pro_edhr_release_transaction_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrReleaseTransactionDO extends BaseDO {

    @TableId
    private Long id;

    private String releaseCode;

    private Long batchExecutionId;

    private String batchExecutionCode;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private Long productId;

    private String productCode;

    private String productName;

    private Long routeId;

    private String routeCode;

    private String routeName;

    private String dhrStatus;

    private String inspectionStatus;

    private String deviationStatus;

    private String reworkStatus;

    private String scrapStatus;

    private String inventoryStatus;

    private String releaseStatus;

    private Integer requiredCheckCount;

    private Integer failedCheckCount;

    private Integer blockingCheckCount;

    private LocalDateTime lastPrecheckAt;

    private String precheckSnapshotJson;

    private String submitIdempotencyKey;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private String approvalIdempotencyKey;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private String approvalSignoffEvidenceHash;

    private String approvalOpinion;

    private Long rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectReason;

    private Long withdrawnBy;

    private LocalDateTime withdrawnAt;

    private String withdrawReason;

    private Integer version;

    private String remark;
}
