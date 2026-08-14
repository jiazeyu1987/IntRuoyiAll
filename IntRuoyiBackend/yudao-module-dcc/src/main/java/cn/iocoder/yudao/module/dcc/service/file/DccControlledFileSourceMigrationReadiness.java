package cn.iocoder.yudao.module.dcc.service.file;

public record DccControlledFileSourceMigrationReadiness(
        long totalSourceRecordCount,
        long ownedSourceRecordCount,
        long unownedSourceRecordCount,
        long sharedSourceGroupCount,
        long sharedSourceRecordCount,
        long failedMigrationCount) {
}
