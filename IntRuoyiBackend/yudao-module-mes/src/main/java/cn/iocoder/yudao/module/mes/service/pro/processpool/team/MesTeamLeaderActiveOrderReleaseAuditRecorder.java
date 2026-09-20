package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditCommand;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditEventType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MesTeamLeaderActiveOrderReleaseAuditRecorder implements MesReleaseFlowAuditRecorder {

    private static final String OBJECT_TYPE = "PRODUCTION_RELEASE_APPLICATION";
    private static final String APPLY_PERMISSION_CODE = "mes:pro-process-pool-team-leader:release-apply";
    private static final String PQC_PERMISSION_CODE = "mes:pro-production-release:pqc-approve";

    private final MesProEdhrOperationAuditService auditService;
    private final AdminUserService adminUserService;

    public MesTeamLeaderActiveOrderReleaseAuditRecorder(
            MesProEdhrOperationAuditService auditService,
            AdminUserService adminUserService) {
        this.auditService = auditService;
        this.adminUserService = adminUserService;
    }

    @Override
    public void record(MesReleaseFlowAuditCommand command) {
        AdminUserDO actor = adminUserService.getUser(command.getActorUserId());
        if (actor == null || actor.getId() == null || actor.getNickname() == null || actor.getNickname().isBlank()) {
            throw new IllegalStateException("RELEASE_FLOW_AUDIT_ACTOR_NAME_MISSING");
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("stage", command.getStage());
        metadata.put("idempotencyKey", command.getIdempotencyKey());
        metadata.put("tenantId", command.getTenantId());
        metadata.put("applicationId", command.getApplicationId());
        metadata.put("activeOrderId", command.getActiveOrderId());
        metadata.put("batchExecutionId", command.getBatchExecutionId());
        metadata.put("signatureId", command.getSignatureId());
        metadata.put("fromStatus", command.getFromStatus());
        metadata.put("toStatus", command.getToStatus());
        metadata.put("version", command.getVersion());
        metadata.put("sourceSnapshotHash", command.getSourceSnapshotHash());
        auditService.recordInCallerTransaction(new MesProEdhrOperationAuditCommand()
                .setRequestId(command.getRequestId())
                .setObjectType(OBJECT_TYPE)
                .setObjectId(String.valueOf(command.getApplicationId()))
                .setBatchExecutionId(command.getBatchExecutionId())
                .setWorkTaskId(command.getWorkTaskId())
                .setOperationType(command.getEventType())
                .setActionName(actionName(command))
                .setActorUserId(command.getActorUserId())
                .setActorUsername(actor.getNickname())
                .setPermissionCode(permissionCode(command))
                .setPermissionDecision("ALLOW")
                .setResultStatus(command.getResultStatus())
                .setBeforeSummaryHash(command.getFromStatus())
                .setAfterSummaryHash(auditSummaryHash(command.getSourceSnapshotHash()))
                .setMetadataJson(JsonUtils.toJsonString(metadata))
                .setOccurredAt(command.getOccurredAt()));
    }

    private String auditSummaryHash(String sourceSnapshotHash) {
        if (StrUtil.isBlank(sourceSnapshotHash) || sourceSnapshotHash.length() <= 64) {
            return sourceSnapshotHash;
        }
        return DigestUtil.sha256Hex(sourceSnapshotHash);
    }

    private String permissionCode(MesReleaseFlowAuditCommand command) {
        return "SP_2".equals(command.getStage()) ? PQC_PERMISSION_CODE : APPLY_PERMISSION_CODE;
    }

    private String actionName(MesReleaseFlowAuditCommand command) {
        return switch (command.getEventType()) {
            case MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPLIED -> "P3推送PQC生产放行";
            case MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPROVED -> "PQC生产放行";
            case MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_REJECTED -> "PQC生产放行退回";
            case MesReleaseFlowAuditEventType.BATCH_EXECUTION_CREATED_FROM_RELEASE -> "生产放行批次执行创建";
            case MesReleaseFlowAuditEventType.RELEASE_REPORT_NODE_COMPLETED -> "生产放行报告节点完成";
            case MesReleaseFlowAuditEventType.RELEASE_REPORT_UPLOAD_COMPLETED -> "生产放行报告上传完成";
            case MesReleaseFlowAuditEventType.MANAGER_RELEASE_TASK_CREATED -> "管理者代表放行任务创建";
            case MesReleaseFlowAuditEventType.BATCH_RECORD_RELEASE_APPROVED -> "批记录上市放行";
            default -> command.getStage();
        };
    }
}
