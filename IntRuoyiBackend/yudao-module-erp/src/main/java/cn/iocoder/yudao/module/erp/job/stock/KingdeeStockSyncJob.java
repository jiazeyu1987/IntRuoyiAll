package cn.iocoder.yudao.module.erp.job.stock;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockSyncResult;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockSyncService;
import cn.iocoder.yudao.module.erp.service.stock.kingdee.ErpKingdeeInventoryListService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component("kingdeeStockSyncJob")
@RequiredArgsConstructor
public class KingdeeStockSyncJob implements JobHandler {

    private final ErpKingdeeStockSyncService stockSyncService;
    private final ErpKingdeeInventoryListService inventoryListService;
    private final ErpKingdeeSyncRuntimeService syncRuntimeService;

    @Override
    @TenantJob
    public String execute(String param) {
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<ErpKingdeeStockSyncResult> stockResultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.STOCK)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeeStockSyncResult stockResult = stockSyncService.syncStocksModifiedBetween(
                    context.getWindowStart(), context.getWindowEnd());
            int syncedInventoryRows = inventoryListService.syncModifiedBetween(
                    context.getWindowStart(), context.getWindowEnd());
            stockResultReference.set(stockResult);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(),
                    stockResult.getSyncedCount() + syncedInventoryRows, 0, 0, 0);
        });
        ErpKingdeeStockSyncResult stockResult = stockResultReference.get();
        return String.format("ERP stock sync: synced=%d", stockResult.getSyncedCount());
    }

}
