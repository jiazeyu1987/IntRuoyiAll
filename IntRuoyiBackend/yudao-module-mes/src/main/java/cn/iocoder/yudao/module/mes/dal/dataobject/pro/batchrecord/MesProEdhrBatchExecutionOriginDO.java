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

@TableName("mes_pro_edhr_batch_execution_origin")
@KeySequence("mes_pro_edhr_batch_execution_origin_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrBatchExecutionOriginDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long batchExecutionId;
    private String entryType;
    private String originKey;
    private Long activeOrderId;
    private Long workOrderId;
    private Long completionTransactionId;
    private Integer completionVersion;
    private Long completionBackfillReceiptId;
    private String completionBackfillReceiptHash;
    private Long pickListBindingId;
    private Long pickListId;
    private Integer pickListBindingVersion;
    private Boolean hasActualLoss;
    private String sourceSnapshotHash;
    private Long batchProvisionReceiptId;
    private String batchProvisionStatus;
    private String sourceCredentialId;
    private String sourceCredentialHash;
    private String sourceBundleHash;
    private String idempotencyKey;
    private String relationStatus;
    private String relationReason;
    private Long capturedBy;
    private LocalDateTime capturedAt;
}
