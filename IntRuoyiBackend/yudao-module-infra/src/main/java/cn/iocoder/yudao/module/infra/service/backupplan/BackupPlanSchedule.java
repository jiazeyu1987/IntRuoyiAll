package cn.iocoder.yudao.module.infra.service.backupplan;

import lombok.Data;

import java.nio.file.Path;

@Data
public class BackupPlanSchedule {

    private String frequency;
    private String time;
    private String weekday;
    private String repositoryEnvironment;
    private Integer maxFreshnessHours;
    private Path repoRoot;
    private Path configPath;
    private Path backupScriptPath;
    private Path registerScriptPath;
}
