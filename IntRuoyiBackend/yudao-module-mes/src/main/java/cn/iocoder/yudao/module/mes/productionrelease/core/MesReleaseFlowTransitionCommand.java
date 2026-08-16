package cn.iocoder.yudao.module.mes.productionrelease.core;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesReleaseFlowTransitionCommand {

    private Long applicationId;
    private Integer expectedVersion;
    private String expectedStatus;
    private String targetStatus;
    private String stage;
    private String auditEventType;
    private String requestId;
    private String idempotencyKey;
    private Long actorUserId;
    private Long workTaskId;
    private Long releaseTransactionId;
}
