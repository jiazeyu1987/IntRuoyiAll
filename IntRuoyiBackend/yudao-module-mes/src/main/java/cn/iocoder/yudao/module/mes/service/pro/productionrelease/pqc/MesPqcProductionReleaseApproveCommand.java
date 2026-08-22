package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesCompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceipt;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesPqcProductionReleaseApproveCommand {

    private Long applicationId;
    private Long pqcReleaseWorkTaskId;
    private Integer expectedVersion;
    private String idempotencyKey;
    private String approvalOpinion;

    private String entryType;
    private String entryBusinessId;
    private String sourceCredentialType;
    private String sourceCredentialId;
    private String sourceRelationId;
    private String sourceContextHash;
    private Long tenantId;
    private Long activeOrderId;
    private String workOrderCode;
    private Long pickListBindingId;
    private Long pickListId;
    private Long bindingVersion;
    private Long batchPickListRelationId;
    private String sourceSnapshotHash;
    private String expectedSourceVersion;
    private String payloadHash;
    private String completionTransactionId;
    private Long expectedActiveOrderVersion;
    private Long completionVersion;
    private String sourceVersion;
    private String sourceBundleHash;
    private String completionBackfillReceiptId;
    private String completionBackfillReceiptHash;
    private String pickListHeaderSnapshotHash;
    private String pickListLineSnapshotHash;
    private List<MesBatchExecutionSourceEvidence> sourceEvidence;
    private MesCompletionBackfillReceipt completionBackfillReceipt;
    private MesIndependentBatchPrerequisiteReceipt independentReceipt;
}
