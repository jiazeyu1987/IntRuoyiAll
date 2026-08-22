package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/** Flow 9 normalized command passed to the Flow 6 batch provisioner. */
@Data
@Accessors(chain = true)
public class MesBatchExecutionProvisionCommand {

    private String entryType;
    private String entryBusinessId;
    private String sourceCredentialType;
    private String sourceCredentialId;
    private String sourceRelationId;
    private String sourceContextHash;
    private Long tenantId;
    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private String batchCode;
    private Long routeId;
    private Long pickListBindingId;
    private Long pickListId;
    private Long bindingVersion;
    private Long batchPickListRelationId;
    private String sourceSnapshotHash;
    private Long routeVersionId;
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
    private String idempotencyKey;
    private String expectedSourceVersion;
    private String payloadHash;
    private MesCompletionBackfillReceipt completionBackfillReceipt;
    private MesIndependentBatchPrerequisiteReceipt independentReceipt;
}
