package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 排产工单创建 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleOrderCreateReqVO {

    @Schema(description = "来源生产工单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "来源生产工单不能为空")
    private Long workOrderId;

    @Schema(description = "承诺交期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "承诺交期不能为空")
    private LocalDateTime promisedDeliveryDate;

    @Schema(description = "优先级，数值越小越优先")
    private Integer priority;

    @Schema(description = "备注")
    private String remark;

}
