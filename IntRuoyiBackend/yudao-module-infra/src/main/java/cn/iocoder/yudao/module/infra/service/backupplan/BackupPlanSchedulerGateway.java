package cn.iocoder.yudao.module.infra.service.backupplan;

public interface BackupPlanSchedulerGateway {

    BackupPlanSchedulerStatus getStatus();

    void registerOrUpdate(BackupPlanSchedule schedule);

    void enable();

    void disable();
}
