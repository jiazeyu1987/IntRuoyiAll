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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component("kingdeeSaleOrderSyncJob")
@RequiredArgsConstructor
public class KingdeeSaleOrderSyncJob implements JobHandler {

    private final ErpKingdeeSaleOrderSyncService saleOrderSyncService;
    private final ErpKingdeeSyncRuntimeService syncRuntimeService;

    @Override
    @TenantJob
    public String execute(String param) {
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

}
