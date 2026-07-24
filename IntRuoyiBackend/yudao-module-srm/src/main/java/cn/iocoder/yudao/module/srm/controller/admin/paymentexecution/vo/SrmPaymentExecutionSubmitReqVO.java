package cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 提交付款申请 Request VO")
@Data
public class SrmPaymentExecutionSubmitReqVO {

    @Schema(description = "付款执行单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "付款执行单编号不能为空")
    private Long id;

    @Schema(description = "提交说明", example = "提交付款申请")
    private String submitRemark;
}
