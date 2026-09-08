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

    @Schema(description = "保存期限来源说明，例如质量批准的记录保存期限矩阵", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "质量批准的记录保存期限矩阵 QA-RET-2026-01")
    @NotBlank(message = "保存期限来源不能为空")
    private String retentionSource;

    @Schema(description = "质量批准引用编号", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "QA-SIGN-20260907")
    @NotBlank(message = "质量批准引用不能为空")
    private String qualityApprovalRef;
}
