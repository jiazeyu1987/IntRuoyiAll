package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrBatchTraceCaptureCommand {

    private Long batchExecutionId;
    private String entryType;
    private String originKey;
    private Long activeOrderId;
    private Long workOrderId;
    private Long completionTransactionId;
    private Integer completionVersion;
    private Long completionBackfillReceiptId;
    private String completionBackfillReceiptHash;
    private Long pickListBindingId;
    private Long pickListId;
    private Integer pickListBindingVersion;
    private String sourceSnapshotHash;
    private Long batchProvisionReceiptId;
    private String batchProvisionStatus;
    private String sourceBundleHash;
    private String idempotencyKey;
    private Long sourceCredentialId;
    private String sourceCredentialHash;
    private Long releaseApplicationId;
    private Boolean hasActualLoss;
    private Long capturedBy;
    private List<MesProEdhrBatchTraceSource> sources;
}
