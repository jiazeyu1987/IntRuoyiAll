package cn.iocoder.yudao.module.mes.job.workorder;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeFullSyncHandler;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncResult;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component("kingdeeProductionMaterialListSyncJob")
@RequiredArgsConstructor
public class KingdeeProductionMaterialListSyncJob implements JobHandler, ErpKingdeeFullSyncHandler {

    private final MesKingdeeProductionMaterialListSyncService syncService;
    private final ErpKingdeeSyncRuntimeService syncRuntimeService;

    @Override
    @TenantJob
    public String execute(String param) {
        if (ErpKingdeeFullSyncHandler.FULL_SYNC_JOB_PARAM.equals(param)) {
            return executeFullSync();
        }
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<MesKingdeeProductionMaterialListSyncResult> resultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .windowEnd(windowEnd)
                .build(), context -> {
            MesKingdeeProductionMaterialListSyncResult result =
                    syncService.syncModifiedBetween(context.getWindowStart(), context.getWindowEnd());
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(), result.getCreatedCount(),
                    result.getUpdatedCount(), 0, 0);
        });
        MesKingdeeProductionMaterialListSyncResult result = resultReference.get();
        return String.format("ERP production material list sync: created=%d, updated=%d",
                result.getCreatedCount(), result.getUpdatedCount());
    }

    @Override
    public String executeFullSync() {
        LocalDateTime windowEnd = LocalDateTime.now();
        AtomicReference<MesKingdeeProductionMaterialListSyncResult> resultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.FULL)
                .windowEnd(windowEnd)
                .build(), context -> {
            MesKingdeeProductionMaterialListSyncResult result = syncService.syncAllSkipExisting();
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(), result.getCreatedCount(),
                    0, result.getSkippedCount(), 0);
        });
        MesKingdeeProductionMaterialListSyncResult result = resultReference.get();
        return String.format("ERP production material list full sync: created=%d, skipped=%d",
                result.getCreatedCount(), result.getSkippedCount());
    }

}
