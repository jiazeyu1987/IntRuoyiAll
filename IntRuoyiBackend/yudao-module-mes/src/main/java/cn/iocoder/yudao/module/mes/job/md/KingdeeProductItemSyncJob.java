package cn.iocoder.yudao.module.mes.job.md;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.sync.ErpKingdeeProductSyncResult;
import cn.iocoder.yudao.module.erp.service.product.sync.ErpKingdeeProductSyncService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeFullSyncHandler;
import cn.iocoder.yudao.module.mes.service.md.item.sync.MesKingdeeItemSyncResult;
import cn.iocoder.yudao.module.mes.service.md.item.sync.MesKingdeeItemSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component("kingdeeProductItemSyncJob")
@RequiredArgsConstructor
public class KingdeeProductItemSyncJob implements JobHandler, ErpKingdeeFullSyncHandler {

    private final ErpKingdeeProductSyncService productSyncService;
    private final MesKingdeeItemSyncService itemSyncService;
    private final ErpKingdeeSyncRuntimeService syncRuntimeService;

    @Override
    @TenantJob
    public String execute(String param) {
        if (ErpKingdeeFullSyncHandler.FULL_SYNC_JOB_PARAM.equals(param)) {
            return executeFullSync();
        }
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<ErpKingdeeProductSyncResult> productResultReference = new AtomicReference<>();
        AtomicReference<MesKingdeeItemSyncResult> itemResultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCT)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeeProductSyncResult productResult = productSyncService.syncProductsModifiedBetween(
                    context.getWindowStart(), context.getWindowEnd());
            productResultReference.set(productResult);
            List<String> changedProductCodes = changedProductCodes(productResult);
            MesKingdeeItemSyncResult itemResult = itemSyncService.syncItemsByProductCodes(changedProductCodes);
            itemResultReference.set(itemResult);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(),
                    productResult.getCreatedCount() + itemResult.getCreatedCount(),
                    productResult.getUpdatedCount() + itemResult.getUpdatedCount() + itemResult.getDisabledCount(),
                    productResult.getSkippedCount() + itemResult.getSkippedCount(),
                    0);
        });
        ErpKingdeeProductSyncResult productResult = productResultReference.get();
        MesKingdeeItemSyncResult itemResult = itemResultReference.get();
        return String.format("ERP product item sync: productsCreated=%d, productsUpdated=%d, productsSkipped=%d, "
                        + "itemsCreated=%d, itemsUpdated=%d, itemsDisabled=%d, itemsSkipped=%d",
                productResult.getCreatedCount(), productResult.getUpdatedCount(), productResult.getSkippedCount(),
                itemResult.getCreatedCount(), itemResult.getUpdatedCount(), itemResult.getDisabledCount(),
                itemResult.getSkippedCount());
    }

    @Override
    public String executeFullSync() {
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<ErpKingdeeProductSyncResult> productResultReference = new AtomicReference<>();
        AtomicReference<MesKingdeeItemSyncResult> itemResultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCT)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.FULL)
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeeProductSyncResult productResult = productSyncService.syncProductsFullSkipExisting();
            MesKingdeeItemSyncResult itemResult = itemSyncService.syncItemsByProductCodes(
                    productResult.getCreatedProductCodes());
            productResultReference.set(productResult);
            itemResultReference.set(itemResult);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(),
                    productResult.getCreatedCount() + itemResult.getCreatedCount(),
                    itemResult.getUpdatedCount() + itemResult.getDisabledCount(),
                    productResult.getSkippedCount() + itemResult.getSkippedCount(), 0);
        });
        ErpKingdeeProductSyncResult productResult = productResultReference.get();
        MesKingdeeItemSyncResult itemResult = itemResultReference.get();
        return String.format("ERP product full sync: productsCreated=%d, productsSkipped=%d, itemsCreated=%d",
                productResult.getCreatedCount(), productResult.getSkippedCount(), itemResult.getCreatedCount());
    }

    private List<String> changedProductCodes(ErpKingdeeProductSyncResult productResult) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        codes.addAll(productResult.getCreatedProductCodes());
        codes.addAll(productResult.getUpdatedProductCodes());
        return new ArrayList<>(codes);
    }

}
