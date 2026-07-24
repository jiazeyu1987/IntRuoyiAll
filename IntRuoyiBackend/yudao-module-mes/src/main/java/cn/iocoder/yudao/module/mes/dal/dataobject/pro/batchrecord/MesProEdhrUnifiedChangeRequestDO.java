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

@TableName("mes_pro_edhr_unified_change_request")
@KeySequence("mes_pro_edhr_unified_change_request_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrUnifiedChangeRequestDO extends BaseDO {

    @TableId
    private Long id;

    private String changeCode;

    private String controlledObjectType;

    private String controlledObjectId;

    private String controlledObjectCode;

    private String currentVersion;

    private String targetVersion;

    private String changeType;

    private String changeStatus;

    private String riskLevel;

    private String reasonCategory;

    private String reason;

    private String diffSnapshotJson;

    private String impactSummaryJson;

    private LocalDateTime impactRecalculatedAt;

    private String impactRecalculationHash;

    private Long requestedBy;

    private LocalDateTime requestedAt;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private String approvalOpinion;

    private String approvalSignoffEvidenceHash;

    private Long effectRequestedBy;

    private LocalDateTime effectRequestedAt;

    private String effectSignoffEvidenceHash;

    private String idempotencyKey;

    private String evidenceHash;
}
