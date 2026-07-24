package cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 创建付款执行单 Request VO")
@Data
public class SrmPaymentExecutionCreateReqVO {

    @Schema(description = "对账单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "对账单编号不能为空")
    private Long reconciliationId;

    @Schema(description = "采购合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "采购合同编号不能为空")
    private Long contractId;

    @Schema(description = "付款说明", example = "测试租户受控模拟链路")
    private String paymentRemark;
}
