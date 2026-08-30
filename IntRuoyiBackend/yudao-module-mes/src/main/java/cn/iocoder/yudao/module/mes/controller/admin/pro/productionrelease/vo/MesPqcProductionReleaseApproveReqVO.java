package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesCompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceipt;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES PQC 生产放行批准 Request VO")
@Data
public class MesPqcProductionReleaseApproveReqVO {

    @NotNull
    private Long applicationId;

    @NotNull
    private Long pqcReleaseWorkTaskId;

    @NotNull
    @Min(1)
    private Integer expectedVersion;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[\\x21-\\x7E]+")
    private String idempotencyKey;

    @Size(max = 500)
    private String approvalOpinion;

    @NotBlank(message = "电子签名密码不能为空")
    private String signaturePassword;

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
