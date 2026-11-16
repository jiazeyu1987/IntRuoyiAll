package cn.iocoder.yudao.module.mes.productionrelease.core;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesReleaseFlowFailureRespVO {

    private String stage;
    private String currentStatus;
    private List<MesReleaseFlowBlocker> blockers;
    private RejectedDecision rejectedDecision;

    /** The decision transaction was NOT_STARTED or completed rollback (ROLLED_BACK).
     * Authentication attempt auditing is independent of the decision transaction. */
    @Data
    @Accessors(chain = true)
    public static class RejectedDecision {
        private String outcome;
        private String applicationId;
        private String workTaskId;
        private String actorId;
        private Integer expectedVersion;
        private String idempotencyKey;
        private String payloadHash;
    }
}
