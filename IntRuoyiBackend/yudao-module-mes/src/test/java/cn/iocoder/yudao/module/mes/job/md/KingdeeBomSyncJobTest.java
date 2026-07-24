package cn.iocoder.yudao.module.mes.job.md;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncTask;
import cn.iocoder.yudao.module.mes.service.md.item.sync.MesKingdeeProductBomSyncResult;
import cn.iocoder.yudao.module.mes.service.md.item.sync.MesKingdeeProductBomSyncService;
import cn.iocoder.yudao.module.mes.service.md.item.kingdee.MesKingdeeBomListService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KingdeeBomSyncJobTest {

    @Test
    void execute_shouldDelegateToProductBomIncrementalSync() {
        MesKingdeeProductBomSyncService syncService = mock(MesKingdeeProductBomSyncService.class);
        MesKingdeeBomListService bomListService = mock(MesKingdeeBomListService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        MesKingdeeProductBomSyncResult result = new MesKingdeeProductBomSyncResult();
        result.setSyncedParentCount(2);
        result.setSyncedBomCount(5);
        result.setRecalculatedWorkOrderCount(3);
        when(syncService.syncBomLinesModifiedBetween(windowStart, windowEnd)).thenReturn(result);
        when(bomListService.syncModifiedBetween(windowStart, windowEnd)).thenReturn(5);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    return task.run(ErpKingdeeSyncContext.builder()
                            .syncType(ErpKingdeeSyncTypeEnum.BOM)
                            .windowStart(windowStart)
                            .windowEnd(windowEnd)
                            .build());
                });
        KingdeeBomSyncJob job = new KingdeeBomSyncJob(syncService, bomListService, runtimeService);

        String output = job.execute("");

        assertTrue(output.contains("parents=2"));
        assertTrue(output.contains("bomLines=5"));
        assertTrue(output.contains("workOrders=3"));
        verify(syncService).syncBomLinesModifiedBetween(windowStart, windowEnd);
        verify(bomListService).syncModifiedBetween(windowStart, windowEnd);
        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        verify(runtimeService).executeSync(commandCaptor.capture(), any(ErpKingdeeSyncTask.class));
        assertEquals(ErpKingdeeSyncTypeEnum.BOM, commandCaptor.getValue().getSyncType());
        assertEquals(ErpKingdeeSyncTriggerTypeEnum.AUTO, commandCaptor.getValue().getTriggerType());
        assertNotNull(commandCaptor.getValue().getWindowEnd());
    }

    @Test
    void execute_shouldExposeRuntimeFailure() {
        MesKingdeeProductBomSyncService syncService = mock(MesKingdeeProductBomSyncService.class);
        MesKingdeeBomListService bomListService = mock(MesKingdeeBomListService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenThrow(new IllegalStateException("kingdee runtime failed"));
        KingdeeBomSyncJob job = new KingdeeBomSyncJob(syncService, bomListService, runtimeService);

        Assertions.assertThrows(IllegalStateException.class, () -> job.execute(""));
    }

}
