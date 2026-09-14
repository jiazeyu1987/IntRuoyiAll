package cn.iocoder.yudao.module.erp.job.purchase;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeePurchaseOrderSyncResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeePurchaseOrderSyncService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeFullSyncHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component("kingdeePurchaseOrderSyncJob")
@RequiredArgsConstructor
public class KingdeePurchaseOrderSyncJob implements JobHandler, ErpKingdeeFullSyncHandler {

    private final ErpKingdeePurchaseOrderSyncService purchaseOrderSyncService;
    private final ErpKingdeeSyncRuntimeService syncRuntimeService;

    @Override
    @TenantJob
    public String execute(String param) {
        if (ErpKingdeeFullSyncHandler.FULL_SYNC_JOB_PARAM.equals(param)) {
            return executeFullSync();
        }
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<ErpKingdeePurchaseOrderSyncResult> resultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PURCHASE_ORDER)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeePurchaseOrderSyncResult result = purchaseOrderSyncService.syncPurchaseOrdersModifiedBetween(
                    context.getWindowStart(), context.getWindowEnd());
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(),
                    result.getCreatedCount(), result.getUpdatedCount(), result.getSkippedCount(), 0);
        });
        ErpKingdeePurchaseOrderSyncResult result = resultReference.get();
        return String.format("ERP purchase order sync: created=%d, updated=%d, skipped=%d",
                result.getCreatedCount(), result.getUpdatedCount(), result.getSkippedCount());
    }

    @Override
    public String executeFullSync() {
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<ErpKingdeePurchaseOrderSyncResult> resultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PURCHASE_ORDER)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.FULL)
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeePurchaseOrderSyncResult result = purchaseOrderSyncService.syncPurchaseOrdersFullSkipExisting();
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(),
                    result.getCreatedCount(), result.getUpdatedCount(), result.getSkippedCount(), 0);
        });
        ErpKingdeePurchaseOrderSyncResult result = resultReference.get();
        return String.format("ERP purchase order full sync: created=%d, skipped=%d",
                result.getCreatedCount(), result.getSkippedCount());
    }

}
