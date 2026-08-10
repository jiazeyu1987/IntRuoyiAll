package cn.iocoder.yudao.module.erp.job.stock;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.stock.kingdee.ErpKingdeeStockMoveListService;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockMoveSyncResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component("kingdeeStockMoveSyncJob")
@RequiredArgsConstructor
public class KingdeeStockMoveSyncJob implements JobHandler {

    private final ErpKingdeeStockMoveListService stockMoveListService;
    private final ErpKingdeeSyncRuntimeService syncRuntimeService;

    @Override
    @TenantJob
    public String execute(String param) {
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<ErpKingdeeStockMoveSyncResult> resultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.STOCK_MOVE)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .windowEnd(windowEnd)
                .build(), context -> {
            ErpKingdeeStockMoveSyncResult result = stockMoveListService.syncModifiedBetween(
                    context.getWindowStart(), context.getWindowEnd());
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(),
                    result.getCreatedCount(), result.getUpdatedCount(), 0, 0);
        });
        ErpKingdeeStockMoveSyncResult result = resultReference.get();
        return String.format("ERP Kingdee stock move sync: created=%d, updated=%d",
                result.getCreatedCount(), result.getUpdatedCount());
    }

}
