package cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 保存备份计划 Request VO")
@Data
public class BackupPlanScheduleSaveReqVO {

    @Schema(description = "频率：DAILY 每天，WEEKLY 每周", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "备份频率不能为空")
    private String frequency;

    @Schema(description = "备份时间，HH:mm", requiredMode = Schema.RequiredMode.REQUIRED, example = "01:30")
    @NotBlank(message = "备份时间不能为空")
    private String time;

    @Schema(description = "每周星期：MON/TUE/WED/THU/FRI/SAT/SUN，仅 WEEKLY 必填", example = "SUN")
    private String weekday;
}
