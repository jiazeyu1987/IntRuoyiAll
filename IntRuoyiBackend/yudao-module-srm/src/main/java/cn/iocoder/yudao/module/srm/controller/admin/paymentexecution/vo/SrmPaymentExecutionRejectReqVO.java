package cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 付款状态处理 Request VO")
@Data
public class SrmPaymentExecutionRejectReqVO {

    @Schema(description = "付款执行单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "付款执行单编号不能为空")
    private Long id;

    @Schema(description = "驳回原因", example = "单据不完整")
    private String rejectRemark;

    @Schema(description = "财务推送是否成功", example = "false")
    private Boolean pushSuccess;

    @Schema(description = "财务推送说明", example = "模拟财务回执")
    private String pushRemark;
}
