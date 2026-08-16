package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditCommand;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MesTeamLeaderActiveOrderReleaseAuditRecorder implements MesReleaseFlowAuditRecorder {

    private static final String OBJECT_TYPE = "PRODUCTION_RELEASE_APPLICATION";
    private static final String APPLY_PERMISSION_CODE = "mes:pro-process-pool-team-leader:release-apply";
    private static final String PQC_PERMISSION_CODE = "mes:pro-production-release:pqc-approve";

    private final MesProEdhrOperationAuditService auditService;

    public MesTeamLeaderActiveOrderReleaseAuditRecorder(MesProEdhrOperationAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void record(MesReleaseFlowAuditCommand command) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("stage", command.getStage());
        metadata.put("idempotencyKey", command.getIdempotencyKey());
        metadata.put("tenantId", command.getTenantId());
        metadata.put("applicationId", command.getApplicationId());
        metadata.put("fromStatus", command.getFromStatus());
        metadata.put("toStatus", command.getToStatus());
        metadata.put("version", command.getVersion());
        auditService.recordInCallerTransaction(new MesProEdhrOperationAuditCommand()
                .setRequestId(command.getRequestId())
                .setObjectType(OBJECT_TYPE)
                .setObjectId(String.valueOf(command.getApplicationId()))
                .setWorkTaskId(command.getWorkTaskId())
                .setOperationType(command.getEventType())
                .setActionName(command.getStage())
                .setActorUserId(command.getActorUserId())
                .setPermissionCode(permissionCode(command))
                .setPermissionDecision("ALLOW")
                .setResultStatus(command.getResultStatus())
                .setBeforeSummaryHash(command.getFromStatus())
                .setAfterSummaryHash(command.getSourceSnapshotHash())
                .setMetadataJson(JsonUtils.toJsonString(metadata))
                .setOccurredAt(command.getOccurredAt()));
    }

    private String permissionCode(MesReleaseFlowAuditCommand command) {
        return "SP_2".equals(command.getStage()) ? PQC_PERMISSION_CODE : APPLY_PERMISSION_CODE;
    }
}
