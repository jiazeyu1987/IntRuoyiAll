package cn.iocoder.yudao.module.mes.productionrelease.core;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesReleaseFinalizationCommand {

    private Long releaseTransactionId;
    private MesReleaseFinalizationAction action;
    /** Optional association created after flow 6 batch provision; never a batch-creation prerequisite. */
    private Long releaseApplicationId;
    private Long batchExecutionId;
    private Long workOrderId;
    private MesReleaseOrigin origin;
    private String entryType;
    private Long activeOrderId;
    private Integer activeOrderExpectedVersion;
    private String pickListBindingId;
    private String completionEventId;
    private String completionBackfillReceiptId;
    private String independentPrerequisiteReceiptId;
    private String materialGateReceiptId;
    private String materialGateManifestHash;
    private String materialGateSourceSnapshotHash;
    private Long pickListId;
    private Boolean dualProgressCompleted;
    private Boolean threeBackfillsSucceeded;
    private String sourceRelation;
    private String sourceSnapshotHash;
    private String idempotencyKey;
    private Long actorUserId;
    private Long workTaskId;
    private Integer expectedVersion;
    private String signoffEvidenceHash;
    private String approvalOpinion;
    private String decisionReason;
    private IndependentBatchPrerequisiteReceipt independentPrerequisiteReceipt;
    private MesReleaseMaterialGateReceipt materialGateReceipt;

    public boolean isIndependentOrigin() {
        return origin == MesReleaseOrigin.MANUAL
                || origin == MesReleaseOrigin.SCHEDULED
                || origin == MesReleaseOrigin.PQC_INDEPENDENT;
    }
}
