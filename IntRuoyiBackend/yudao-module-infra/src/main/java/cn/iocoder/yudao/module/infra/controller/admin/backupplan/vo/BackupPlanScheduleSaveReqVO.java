package cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 保存备份计划 Request VO")
@Data
public class BackupPlanScheduleSaveReqVO {

    @Schema(description = "每周全量备份计划，格式 WEEKDAY HH:mm", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "SUN 01:00")
    @NotBlank(message = "全量备份计划不能为空")
    private String fullSchedule;

    @Schema(description = "每日增量备份时间，格式 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "02:00")
    @NotBlank(message = "增量备份计划不能为空")
    private String incrementalSchedule;
}
