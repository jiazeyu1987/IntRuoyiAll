package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - MES 从生产工单生成排产工单 Request VO")
@Data
public class MesProScheduleOrderCreateFromWorkOrderReqVO {

    @Schema(description = "生产工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "生产工单编号不能为空")
    private Long workOrderId;

    @Schema(description = "承诺交期")
    private LocalDate promiseDate;

    @Schema(description = "优先级排序", example = "10")
    private Integer priorityNo;

    @Schema(description = "备注", example = "排产员备注")
    private String remark;

}
