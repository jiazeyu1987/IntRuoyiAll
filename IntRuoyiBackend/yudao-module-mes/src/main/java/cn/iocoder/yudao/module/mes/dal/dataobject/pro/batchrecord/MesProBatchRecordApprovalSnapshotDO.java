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

@TableName("mes_pro_batch_record_approval_snapshot")
@KeySequence("mes_pro_batch_record_approval_snapshot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordApprovalSnapshotDO extends BaseDO {

    @TableId
    private Long id;

    private Long executionId;

    private String processDefinitionKey;

    private String processDefinitionId;

    private String processInstanceId;

    private String approvalStatus;

    private String snapshotJson;

    private String snapshotHash;

    private String currentBpmTaskId;

    private String currentTaskDefinitionKey;

    private Long submitSignatureId;

    private Long approveSignatureId;

    private Long rejectSignatureId;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private Long rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectReason;

    private LocalDateTime closedAt;
}
