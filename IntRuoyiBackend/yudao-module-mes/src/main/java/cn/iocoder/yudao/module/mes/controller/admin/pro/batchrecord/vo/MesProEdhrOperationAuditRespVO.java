package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrOperationAuditRespVO {

    private Long id;

    private String requestId;

    private String objectType;

    private String objectId;

    private Long batchExecutionId;

    private Long executionId;

    private Long workTaskId;

    private Long routeId;

    private Long routeProcessId;

    private String reportId;

    private String recordCategory;

    private String operationType;

    private String actionName;

    private Long actorUserId;

    private String actorUsername;

    private String permissionCode;

    private String permissionDecision;

    private String matchedRuleIds;

    private String resultStatus;

    private String failureCode;

    private String failureMessage;

    private String beforeSummaryHash;

    private String afterSummaryHash;

    private String metadataJson;

    private LocalDateTime occurredAt;

    private String previousAuditHash;

    private String auditHash;
}
