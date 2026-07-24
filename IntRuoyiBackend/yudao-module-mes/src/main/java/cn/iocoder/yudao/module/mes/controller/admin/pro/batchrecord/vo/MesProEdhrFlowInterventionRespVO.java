package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrFlowInterventionRespVO {

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
