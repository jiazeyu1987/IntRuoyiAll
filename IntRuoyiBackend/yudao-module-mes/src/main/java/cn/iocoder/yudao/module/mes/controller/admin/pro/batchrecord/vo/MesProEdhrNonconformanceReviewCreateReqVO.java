package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "管理后台 - eDHR 不合格评审创建 Request VO")
@Data
public class MesProEdhrNonconformanceReviewCreateReqVO {

    @Schema(description = "活跃订单ID；填写后由服务端统一记录为 ACTIVE_ORDER 来源")
    private Long activeOrderId;

    @Schema(description = "兼容旧入口的来源类型：PQC_SUBMISSION/PQC_RELEASE")
    private String sourceType;

    @Schema(description = "来源记录ID")
    private Long sourceId;

    @Schema(description = "eDHR 批次执行ID；PQC生产放行申请尚未建批时可为空")
    private Long batchExecutionId;

    @Schema(description = "不合格原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "不合格原因不能为空")
    private String nonconformanceReason;

    @Schema(description = "兼容旧来源入口的电子签名密码；ACTIVE_ORDER 创建不需要")
    private String signaturePassword;

    @Schema(description = "备注")
    private String remark;
}
