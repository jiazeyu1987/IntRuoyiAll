package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import cn.iocoder.yudao.module.mes.productionrelease.core.IndependentBatchPrerequisiteReceipt;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseOrigin;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrReleaseApproveReqVO {

    @NotNull(message = "放行事务不能为空")
    private Long releaseTransactionId;

    private Long workTaskId;

    private Integer expectedVersion;

    @NotBlank(message = "批准幂等键不能为空")
    private String idempotencyKey;

    @NotBlank(message = "签核证据不能为空")
    private String signoffEvidenceHash;

    private String approvalOpinion;

    /** Release provenance and gate receipts are required by finalizeRelease. */
    private Long releaseApplicationId;

    private Long batchExecutionId;

    private Long workOrderId;

    private Long pickListId;

    private String independentPrerequisiteReceiptId;

    private String materialGateReceiptId;

    private String materialGateManifestHash;

    private String materialGateSourceSnapshotHash;

    private MesReleaseOrigin origin;

    private String entryType;

    private Long activeOrderId;

    private Integer activeOrderExpectedVersion;

    private String pickListBindingId;

    private String completionEventId;

    private String completionBackfillReceiptId;

    private Boolean dualProgressCompleted;

    private Boolean threeBackfillsSucceeded;

    private String sourceRelation;

    private String sourceSnapshotHash;

    /** Receipt payloads are never accepted from HTTP; only IDs/hashes are forwarded to the owner port. */
    @JsonIgnore
    private IndependentBatchPrerequisiteReceipt independentPrerequisiteReceipt;

    @JsonIgnore
    private MesReleaseMaterialGateReceipt materialGateReceipt;

    public MesProEdhrReleaseApproveReqVO setReleaseTransactionId(Long releaseTransactionId) {
        this.releaseTransactionId = releaseTransactionId;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setWorkTaskId(Long workTaskId) {
        this.workTaskId = workTaskId;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setExpectedVersion(Integer expectedVersion) {
        this.expectedVersion = expectedVersion;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setSignoffEvidenceHash(String signoffEvidenceHash) {
        this.signoffEvidenceHash = signoffEvidenceHash;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setApprovalOpinion(String approvalOpinion) {
        this.approvalOpinion = approvalOpinion;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setActiveOrderExpectedVersion(Integer activeOrderExpectedVersion) {
        this.activeOrderExpectedVersion = activeOrderExpectedVersion;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setPickListId(Long pickListId) {
        this.pickListId = pickListId;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setIndependentPrerequisiteReceiptId(String independentPrerequisiteReceiptId) {
        this.independentPrerequisiteReceiptId = independentPrerequisiteReceiptId;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setMaterialGateReceiptId(String materialGateReceiptId) {
        this.materialGateReceiptId = materialGateReceiptId;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setMaterialGateManifestHash(String materialGateManifestHash) {
        this.materialGateManifestHash = materialGateManifestHash;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setMaterialGateSourceSnapshotHash(String materialGateSourceSnapshotHash) {
        this.materialGateSourceSnapshotHash = materialGateSourceSnapshotHash;
        return this;
    }
}
