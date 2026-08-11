package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSourceMigrationReadiness;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Admin - DCC formal source ownership migration readiness")
@Data
@Builder
public class DccControlledFileSourceMigrationReadinessRespVO {

    private Long totalSourceRecordCount;
    private Long ownedSourceRecordCount;
    private Long unownedSourceRecordCount;
    private Long sharedSourceGroupCount;
    private Long sharedSourceRecordCount;
    private Long failedMigrationCount;

    public static DccControlledFileSourceMigrationReadinessRespVO from(
            DccControlledFileSourceMigrationReadiness readiness) {
        return DccControlledFileSourceMigrationReadinessRespVO.builder()
                .totalSourceRecordCount(readiness.totalSourceRecordCount())
                .ownedSourceRecordCount(readiness.ownedSourceRecordCount())
                .unownedSourceRecordCount(readiness.unownedSourceRecordCount())
                .sharedSourceGroupCount(readiness.sharedSourceGroupCount())
                .sharedSourceRecordCount(readiness.sharedSourceRecordCount())
                .failedMigrationCount(readiness.failedMigrationCount())
                .build();
    }
}
