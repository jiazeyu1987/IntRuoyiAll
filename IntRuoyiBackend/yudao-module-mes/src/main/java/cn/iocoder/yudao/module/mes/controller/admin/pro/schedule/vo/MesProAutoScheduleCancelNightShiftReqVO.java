package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "???? - MES ?????? Request VO")
@Data
public class MesProAutoScheduleCancelNightShiftReqVO {

    @Schema(description = "??????", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "??????????")
    private Long taskId;

    @Schema(description = "????", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "????????")
    private String reason;
}
