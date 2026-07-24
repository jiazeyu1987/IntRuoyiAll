package cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 生产任务锁定 Request VO")
@Data
public class MesProTaskLockReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "任务编号不能为空")
    private Long taskId;

    @Schema(description = "锁定原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "planner-lock")
    @NotBlank(message = "锁定原因不能为空")
    private String lockedReason;
}
