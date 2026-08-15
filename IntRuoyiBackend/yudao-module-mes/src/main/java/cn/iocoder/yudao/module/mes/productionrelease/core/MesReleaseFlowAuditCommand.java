package cn.iocoder.yudao.module.mes.productionrelease.core;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesReleaseFlowAuditCommand {

    private String eventType;
    private String stage;
    private String requestId;
    private String idempotencyKey;
    private Long tenantId;
    private Long applicationId;
    private Long workTaskId;
    private Long batchExecutionId;
    private Long releaseTransactionId;
    private String fromStatus;
    private String toStatus;
    private Integer version;
    private Long actorUserId;
    private LocalDateTime occurredAt;
    private String sourceSnapshotHash;
    private String resultStatus;
    private MesReleaseFlowBlockerType blockerType;
}
