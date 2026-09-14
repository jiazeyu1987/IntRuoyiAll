package cn.iocoder.yudao.module.erp.job.production;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.production.kingdee.ErpKingdeeProductionReplenishmentListService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionReplenishmentListSyncResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeFullSyncHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component("kingdeeProductionReplenishmentListSyncJob")
@RequiredArgsConstructor
public class KingdeeProductionReplenishmentListSyncJob implements JobHandler, ErpKingdeeFullSyncHandler {

    private final ErpKingdeeProductionReplenishmentListService productionReplenishmentListService;
    private final ErpKingdeeSyncRuntimeService syncRuntimeService;

    @Override
    @TenantJob
    public String execute(String param) {
        if (ErpKingdeeFullSyncHandler.FULL_SYNC_JOB_PARAM.equals(param)) {
            return executeFullSync();
        }
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<ErpKingdeeProductionReplenishmentListSyncResult> resultReference =
                new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_REPLENISHMENT_LIST)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .initialWindowStart(windowEnd.toLocalDate().minusMonths(6).atStartOfDay())
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeeProductionReplenishmentListSyncResult result =
                    productionReplenishmentListService.syncModifiedBetween(
                            context.getWindowStart(), context.getWindowEnd());
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(),
                    result.getCreatedCount(), result.getUpdatedCount(), 0, 0);
        });
        ErpKingdeeProductionReplenishmentListSyncResult result = resultReference.get();
        return String.format("ERP Kingdee production replenishment list sync: created=%d, updated=%d",
                result.getCreatedCount(), result.getUpdatedCount());
    }

    @Override
    public String executeFullSync() {
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.toLocalDate().minusDays(365).atStartOfDay();
        AtomicReference<ErpKingdeeProductionReplenishmentListSyncResult> resultReference =
                new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_REPLENISHMENT_LIST)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.FULL)
                .forceInitialWindowStart(true)
                .initialWindowStart(windowStart)
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeeProductionReplenishmentListSyncResult result = productionReplenishmentListService.syncAllSkipExisting(
                    context.getWindowStart(), context.getWindowEnd());
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(),
                    result.getCreatedCount(), result.getUpdatedCount(), result.getSkippedCount(), 0);
        });
        ErpKingdeeProductionReplenishmentListSyncResult result = resultReference.get();
        return String.format("ERP Kingdee production replenishment list full sync: created=%d, skipped=%d",
                result.getCreatedCount(), result.getSkippedCount());
    }

}
