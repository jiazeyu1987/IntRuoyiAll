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

@TableName("mes_pro_edhr_release_decision")
@KeySequence("mes_pro_edhr_release_decision_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrReleaseDecisionDO extends BaseDO {

    @TableId
    private Long id;

    private Long releaseTransactionId;
    private Long releaseApplicationId;
    private Long batchExecutionId;
    private Long workOrderId;
    private Long activeOrderId;
    private String pickListBindingId;
    private String completionEventId;
    private String completionBackfillReceiptId;
    private String origin;
    private String entryType;
    private String sourceRelation;
    private String sourceSnapshotHash;
    private String materialGateReceiptId;
    private String materialGateSnapshotHash;
    private Integer materialGateVersion;
    private String decisionStatus;
    private String idempotencyKey;
    private String payloadHash;
    private Long actorUserId;
    private String signoffEvidenceHash;
    private String approvalOpinion;
    private String decisionReason;
    private String auditSnapshotJson;
    private LocalDateTime decidedAt;
    private Integer version;
}
