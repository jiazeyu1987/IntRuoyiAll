package cn.iocoder.yudao.module.erp.job.sale;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sale.sync.ErpKingdeeSaleOrderSyncResult;
import cn.iocoder.yudao.module.erp.service.sale.sync.ErpKingdeeSaleOrderSyncService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeFullSyncHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component("kingdeeSaleOrderSyncJob")
@RequiredArgsConstructor
public class KingdeeSaleOrderSyncJob implements JobHandler, ErpKingdeeFullSyncHandler {

    private final ErpKingdeeSaleOrderSyncService saleOrderSyncService;
    private final ErpKingdeeSyncRuntimeService syncRuntimeService;

    @Override
    @TenantJob
    public String execute(String param) {
        if (param != null && param.startsWith(WINDOW_SYNC_JOB_PREFIX)) {
            return executeWindowSync(param);
        }
        if (ErpKingdeeFullSyncHandler.FULL_SYNC_JOB_PARAM.equals(param)) {
            return executeFullSync();
        }
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<ErpKingdeeSaleOrderSyncResult> resultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.SALE_ORDER)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeeSaleOrderSyncResult result = saleOrderSyncService.syncSaleOrdersModifiedBetween(
                    context.getWindowStart(), context.getWindowEnd());
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(),
                    result.getCreatedCount(), result.getUpdatedCount(), result.getSkippedCount(), 0);
        });
        ErpKingdeeSaleOrderSyncResult result = resultReference.get();
        return String.format("ERP sale order sync: created=%d, updated=%d, skipped=%d",
                result.getCreatedCount(), result.getUpdatedCount(), result.getSkippedCount());
    }

    @Override
    public String executeFullSync() {
        LocalDateTime windowEnd = LocalDateTime.now();
        return executeFullSync(null, windowEnd);
    }

    @Override
    public String executeFullSync(LocalDateTime windowStart, LocalDateTime windowEnd) {
        AtomicReference<ErpKingdeeSaleOrderSyncResult> resultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.SALE_ORDER)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.FULL)
                .forceInitialWindowStart(windowStart != null)
                .initialWindowStart(windowStart)
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeeSaleOrderSyncResult result = windowStart == null ? saleOrderSyncService.syncSaleOrdersFullSkipExisting()
                    : saleOrderSyncService.syncSaleOrdersFullSkipExisting(windowStart, windowEnd);
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(windowStart == null ? context.getWindowEnd() : null,
                    result.getCreatedCount(), result.getUpdatedCount(), result.getSkippedCount(), 0);
        });
        ErpKingdeeSaleOrderSyncResult result = resultReference.get();
        return String.format("ERP sale order full sync: created=%d, skipped=%d",
                result.getCreatedCount(), result.getSkippedCount());
    }

}
