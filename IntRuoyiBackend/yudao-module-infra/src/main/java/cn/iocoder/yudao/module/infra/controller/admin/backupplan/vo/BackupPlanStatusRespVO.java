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

    @Schema(description = "频率：DAILY/WEEKLY")
    private String frequency;

    @Schema(description = "备份时间，HH:mm")
    private String time;

    @Schema(description = "每周星期")
    private String weekday;

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
