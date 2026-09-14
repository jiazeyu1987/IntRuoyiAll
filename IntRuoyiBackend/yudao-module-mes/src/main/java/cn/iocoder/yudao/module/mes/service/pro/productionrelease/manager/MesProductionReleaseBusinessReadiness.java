package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesOrderReleaseCompletenessCheck;

import java.util.List;

public record MesProductionReleaseBusinessReadiness(
        String dhrStatus,
        String inspectionStatus,
        String deviationStatus,
        String reworkStatus,
        String scrapStatus,
        String inventoryStatus,
        int requiredCheckCount,
        int failedCheckCount,
        int blockingCheckCount,
        String snapshotJson,
        String snapshotHash,
        List<MesOrderReleaseCompletenessCheck> checks) {

    public boolean hasBlockingChecks() {
        return failedCheckCount > 0 || blockingCheckCount > 0;
    }
}
