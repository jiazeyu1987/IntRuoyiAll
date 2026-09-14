package cn.iocoder.yudao.module.erp.service.sync.runtime;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ErpKingdeeSyncRunResult {

    LocalDateTime watermarkTime;
    int createdCount;
    int updatedCount;
    int skippedCount;
    int failedCount;

    public static ErpKingdeeSyncRunResult success(LocalDateTime watermarkTime, int createdCount, int updatedCount,
                                                  int skippedCount, int failedCount) {
        return ErpKingdeeSyncRunResult.builder()
                .watermarkTime(watermarkTime)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .build();
    }

}
