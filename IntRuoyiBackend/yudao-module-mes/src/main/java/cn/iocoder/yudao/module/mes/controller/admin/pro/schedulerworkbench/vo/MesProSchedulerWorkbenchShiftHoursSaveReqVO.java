package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 排产员工作台班次小时保存 Request VO")
@Data
public class MesProSchedulerWorkbenchShiftHoursSaveReqVO {

    @Schema(description = "班次小时数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.50")
    @NotNull(message = "班次小时不能为空")
    @DecimalMin(value = "0.01", message = "班次小时必须大于 0")
    private BigDecimal shiftHours;

}
