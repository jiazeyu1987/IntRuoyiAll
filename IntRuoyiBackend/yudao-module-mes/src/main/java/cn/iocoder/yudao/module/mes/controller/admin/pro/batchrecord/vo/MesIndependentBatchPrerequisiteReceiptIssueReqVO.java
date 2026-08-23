package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesIndependentBatchPrerequisiteReceiptIssueReqVO {

    @NotBlank private String entryType;
    @NotNull private Long workOrderId;
    @NotBlank private String workOrderCode;
    @NotNull private Long routeId;
    @NotNull private Long routeVersionId;
    @NotBlank private String routeVersion;
    @NotBlank private String batchCode;
    @NotBlank private String sourceRelationId;
    @NotBlank private String sourceRelationVersion;
    @NotBlank private String sourceRelationSnapshotHash;
    @NotBlank private String sourceObjectType;
    @NotBlank private String sourceObjectId;
    @NotBlank private String materialSourceType;
    @NotBlank private String materialSourceId;
    @NotBlank private String sourceContextHash;
    @NotBlank private String sourceSnapshotHash;
    @NotBlank private String businessReason;
    @NotBlank private String idempotencyKey;
    @Valid @NotNull private List<MesBatchExecutionSourceEvidence> sourceEvidence;
}
