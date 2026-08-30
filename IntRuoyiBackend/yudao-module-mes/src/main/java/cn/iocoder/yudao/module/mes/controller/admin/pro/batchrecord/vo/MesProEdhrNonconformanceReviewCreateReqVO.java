package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 不合格评审创建 Request VO")
@Data
public class MesProEdhrNonconformanceReviewCreateReqVO {

    @Schema(description = "来源类型：PQC_SUBMISSION/PQC_RELEASE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @Schema(description = "来源记录ID")
    private Long sourceId;

    @Schema(description = "eDHR 批次执行ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "批次执行不能为空")
    private Long batchExecutionId;

    @Schema(description = "不合格原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "不合格原因不能为空")
    private String nonconformanceReason;

    @Schema(description = "备注")
    private String remark;
}
