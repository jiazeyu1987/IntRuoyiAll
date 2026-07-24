package cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 创建委外执行单 Request VO")
@Data
public class SrmOutsourceExecutionCreateReqVO {

    @Schema(description = "采购订单协同单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "采购订单协同单编号不能为空")
    private Long purchaseOrderId;

    @Schema(description = "模拟链路说明", example = "测试租户受控模拟链路")
    private String simulationRemark;
}
