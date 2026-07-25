package cn.iocoder.yudao.module.erp.service.sync.runtime;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErpKingdeeSyncRunResult {

    private final LocalDateTime watermarkTime;
    private final Integer createdCount;
    private final Integer updatedCount;
    private final Integer skippedCount;
    private final Integer failedCount;

    public static ErpKingdeeSyncRunResult success(LocalDateTime watermarkTime, Integer createdCount,
                                                  Integer updatedCount, Integer skippedCount, Integer failedCount) {
        return ErpKingdeeSyncRunResult.builder()
                .watermarkTime(watermarkTime)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .build();
    }

}
