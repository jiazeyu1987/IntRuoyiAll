package cn.iocoder.yudao.module.erp.service.sync.runtime;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ErpKingdeeSyncRunResult {

    private final LocalDateTime watermarkTime;
    private final int createdCount;
    private final int updatedCount;
    private final int skippedCount;
    private final int failedCount;

    private ErpKingdeeSyncRunResult(LocalDateTime watermarkTime, int createdCount, int updatedCount,
            int skippedCount, int failedCount) {
        this.watermarkTime = watermarkTime;
        this.createdCount = createdCount;
        this.updatedCount = updatedCount;
        this.skippedCount = skippedCount;
        this.failedCount = failedCount;
    }

    public static ErpKingdeeSyncRunResult success(LocalDateTime watermarkTime, int createdCount, int updatedCount,
            int skippedCount, int failedCount) {
        return new ErpKingdeeSyncRunResult(watermarkTime, createdCount, updatedCount, skippedCount, failedCount);
    }

}
