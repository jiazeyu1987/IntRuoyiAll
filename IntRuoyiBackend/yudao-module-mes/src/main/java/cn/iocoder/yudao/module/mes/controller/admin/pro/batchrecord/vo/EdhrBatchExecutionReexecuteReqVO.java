package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesCompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceipt;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionReexecuteReqVO {

    @NotNull(message = "来源拒收批次执行ID不能为空")
    private Long sourceRejectedBatchExecutionId;

    @NotBlank(message = "同批号重做原因不能为空")
    private String reason;

    private String remark;

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
