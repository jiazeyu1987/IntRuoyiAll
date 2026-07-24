package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 排产工单单条动作 Request VO")
@Data
public class MesProScheduleOrderActionReqVO {

    @Schema(description = "排产工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "排产工单不能为空")
    private Long id;

    @Schema(description = "操作原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作原因不能为空")
    private String reason;

}
