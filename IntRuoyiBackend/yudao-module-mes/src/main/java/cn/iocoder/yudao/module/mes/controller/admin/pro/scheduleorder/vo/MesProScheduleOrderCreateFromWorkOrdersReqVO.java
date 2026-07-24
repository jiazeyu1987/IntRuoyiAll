package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 从生产工单批量生成排产工单 Request VO")
@Data
public class MesProScheduleOrderCreateFromWorkOrdersReqVO {

    @Schema(description = "生产工单编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "生产工单不能为空")
    private List<Long> workOrderIds;

    @Schema(description = "承诺交期")
    private LocalDate promiseDate;

}
