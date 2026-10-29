package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - eDHR 批次执行驳回 Request VO")
@Data
@Accessors(chain = true)
public class MesProEdhrBatchExecutionRejectReqVO {

    @jakarta.validation.constraints.NotNull(message = "请选择QA评审责任人")
    @jakarta.validation.constraints.Positive(message = "QA评审责任人编号必须为正数")
    @Schema(description = "发起人明确选择的QA评审责任人", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long qaReviewerUserId;

    @Schema(description = "批次执行 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "批次执行 ID 不能为空")
    private Long batchExecutionId;

    @Schema(description = "不合格原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "不合格原因不能为空")
    private String nonconformanceReason;

    @Schema(description = "电子签名密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "电子签名密码不能为空")
    private String signaturePassword;
}
