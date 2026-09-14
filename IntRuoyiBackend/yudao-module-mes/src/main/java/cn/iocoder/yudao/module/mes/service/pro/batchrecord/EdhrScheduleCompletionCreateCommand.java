package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 排产工单完成排产后创建 eDHR 执行批次的正式业务命令。
 */
@Data
@Accessors(chain = true)
public class EdhrScheduleCompletionCreateCommand {

    private Long scheduleOrderId;

    private String scheduleOrderCode;

    private Long workOrderId;
    private String workOrderCode;

    private Long tenantId;

    private String batchCode;

    private Long productId;

    private Long routeId;

    private String remark;

    private String entryType;
    private String entryBusinessId;
    private String sourceCredentialType;
    private String sourceCredentialId;
    private String sourceRelationId;
    private String sourceContextHash;
    private Long activeOrderId;
    private Long pickListBindingId;
    private Long pickListId;
    private List<MesBatchExecutionPickListSource> pickListSources;
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
