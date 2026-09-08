package cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 备份计划状态 Response VO")
@Data
public class BackupPlanStatusRespVO {

    @Schema(description = "计划状态：已开启/已关闭")
    private String planStatus;

    @Schema(description = "健康状态：正常/已关闭/上次失败/配置异常")
    private String healthStatus;

    @Schema(description = "每周全量备份计划，格式 WEEKDAY HH:mm")
    private String fullSchedule;

    @Schema(description = "每日增量备份时间，格式 HH:mm")
    private String incrementalSchedule;

    @Schema(description = "备份仓库环境：test/backup")
    private String repositoryEnvironment;

    @Schema(description = "最近成功备份点最大新鲜度小时数")
    private Integer maxFreshnessHours;

    @Schema(description = "下次运行时间")
    private LocalDateTime nextRunTime;

    @Schema(description = "上次运行时间")
    private LocalDateTime lastRunTime;

    @Schema(description = "上次运行结果代码")
    private Integer lastResultCode;

    @Schema(description = "配置异常说明")
    private String blockedReason;

    @Schema(description = "最近备份包")
    private RuntimeControlBackupPointRespVO latestBackupPoint;
}
