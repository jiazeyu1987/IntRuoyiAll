package cn.iocoder.yudao.module.mes.job.workorder;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionOrderSyncResult;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionOrderSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component("kingdeeProductionOrderSyncJob")
@RequiredArgsConstructor
public class KingdeeProductionOrderSyncJob implements JobHandler {

    private final MesKingdeeProductionOrderSyncService syncService;
    private final ErpKingdeeSyncRuntimeService syncRuntimeService;

    @Override
    @TenantJob
    public String execute(String param) {
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<MesKingdeeProductionOrderSyncResult> resultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .initialWindowStart(windowEnd.toLocalDate().minusYears(1).atStartOfDay())
                .windowEnd(windowEnd)
                .build(), context -> {
            MesKingdeeProductionOrderSyncResult result = syncService.syncWorkOrders(context);
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(windowEnd, result.getCreatedCount(),
                    result.getUpdatedCount() + result.getFinishedCount() + result.getCanceledCount(),
                    result.getSkippedCount(), 0);
        });
        MesKingdeeProductionOrderSyncResult result = resultReference.get();
        return String.format("ERP production order sync: created=%d, updated=%d, finished=%d, canceled=%d, skipped=%d",
                result.getCreatedCount(), result.getUpdatedCount(), result.getFinishedCount(),
                result.getCanceledCount(), result.getSkippedCount());
    }

}
