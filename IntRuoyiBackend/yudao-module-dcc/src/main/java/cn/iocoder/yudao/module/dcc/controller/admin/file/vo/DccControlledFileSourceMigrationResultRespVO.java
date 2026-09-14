package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSourceMigrationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Admin - DCC formal source ownership migration batch result")
@Data
@Builder
public class DccControlledFileSourceMigrationResultRespVO {

    private Integer processedCount;
    private Long remainingCount;

    public static DccControlledFileSourceMigrationResultRespVO from(
            DccControlledFileSourceMigrationResult result) {
        return DccControlledFileSourceMigrationResultRespVO.builder()
                .processedCount(result.processedCount())
                .remainingCount(result.remainingCount())
                .build();
    }
}
