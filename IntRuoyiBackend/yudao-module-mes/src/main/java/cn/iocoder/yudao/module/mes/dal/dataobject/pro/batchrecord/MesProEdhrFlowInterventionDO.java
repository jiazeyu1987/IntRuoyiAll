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

@TableName("mes_pro_edhr_flow_intervention")
@KeySequence("mes_pro_edhr_flow_intervention_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrFlowInterventionDO extends BaseDO {

    @TableId
    private Long id;

    private String interventionCode;

    private String businessObjectType;

    private String businessObjectId;

    private String businessObjectCode;

    private String flowInstanceId;

    private String interventionAction;

    private String interventionStatus;

    private String fromStatus;

    private String toStatus;

    private String sourceTaskId;

    private String targetTaskId;

    private String nodeKey;

    private Long targetUserId;

    private Long requestedBy;

    private LocalDateTime requestedAt;

    private String reasonCategory;

    private String reason;

    private String authorizationBasis;

    private String signoffEvidenceHash;

    private String idempotencyKey;

    private String integrityCheckResult;

    private String integrityCheckSnapshotJson;

    private String evidenceHash;
}
