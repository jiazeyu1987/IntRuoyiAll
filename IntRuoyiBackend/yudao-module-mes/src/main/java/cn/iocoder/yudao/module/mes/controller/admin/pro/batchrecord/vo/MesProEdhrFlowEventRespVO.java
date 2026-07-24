package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrFlowEventRespVO {

    private Long id;

    private String businessObjectType;

    private String businessObjectId;

    private String businessObjectCode;

    private Long interventionId;

    private String flowInstanceId;

    private String taskId;

    private String nodeKey;

    private String eventType;

    private String fromStatus;

    private String toStatus;

    private Long actorUserId;

    private Long targetUserId;

    private String permissionCode;

    private String permissionDecision;

    private String reason;

    private String signoffEvidenceHash;

    private String integrityCheckResult;

    private String integrityCheckSnapshotJson;

    private String eventSnapshotJson;

    private String evidenceHash;

    private LocalDateTime occurredAt;
}
