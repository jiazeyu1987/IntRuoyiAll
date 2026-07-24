package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrUnifiedChangeRespVO {

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
