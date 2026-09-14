package cn.iocoder.yudao.module.infra.service.backupplan;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BackupPlanSchedulerStatus {

    private Boolean enabled;
    private LocalDateTime nextRunTime;
    private LocalDateTime lastRunTime;
    private Integer lastResultCode;
    private Integer queryExitCode;
    private String taskToRun;
    private String rawStatus;
    private String blockedReason;
}
