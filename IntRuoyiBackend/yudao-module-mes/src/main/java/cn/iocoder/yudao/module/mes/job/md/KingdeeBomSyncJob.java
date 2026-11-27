package cn.iocoder.yudao.module.mes.job.md;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeFullSyncHandler;
import cn.iocoder.yudao.module.mes.service.md.item.kingdee.MesKingdeeBomListService;
import cn.iocoder.yudao.module.mes.service.md.item.kingdee.MesKingdeeBomListSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component("kingdeeBomSyncJob")
@RequiredArgsConstructor
public class KingdeeBomSyncJob implements JobHandler, ErpKingdeeFullSyncHandler {

    private final MesKingdeeBomListService bomListService;
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
        AtomicReference<Integer> resultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.BOM)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .windowEnd(windowEnd)
                .build(), context -> {
            // Preserve every ERP BOM version. Applying a version to MES product BOMs is a separate operation.
            int syncedBomListCount = bomListService.syncModifiedBetween(context.getWindowStart(), context.getWindowEnd());
            resultReference.set(syncedBomListCount);
            return ErpKingdeeSyncRunResult.success(context.getWindowEnd(), 0, syncedBomListCount, 0, 0);
        });
        return String.format("ERP BOM sync: bomLines=%d", resultReference.get());
    }

    @Override
    public String executeFullSync() {
        LocalDateTime windowEnd = LocalDateTime.now();
        return executeFullSync(null, windowEnd);
    }

    @Override
    public String executeFullSync(LocalDateTime windowStart, LocalDateTime windowEnd) {
        AtomicReference<MesKingdeeBomListSyncResult> resultReference = new AtomicReference<>();
        syncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.BOM)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.FULL)
                .forceInitialWindowStart(windowStart != null)
                .initialWindowStart(windowStart)
                .windowEnd(windowEnd)
                .build(), context -> {
            MesKingdeeBomListSyncResult result = windowStart == null ? bomListService.syncAllWithUpdates()
                    : bomListService.syncAllSkipExisting(windowStart, windowEnd);
            resultReference.set(result);
            return ErpKingdeeSyncRunResult.success(windowStart == null ? context.getWindowEnd() : null, result.getCreatedCount(),
                    result.getUpdatedCount(), result.getSkippedCount(), 0);
        });
        MesKingdeeBomListSyncResult result = resultReference.get();
        return String.format("ERP BOM full sync: created=%d, skipped=%d",
                result.getCreatedCount(), result.getSkippedCount());
    }

}
