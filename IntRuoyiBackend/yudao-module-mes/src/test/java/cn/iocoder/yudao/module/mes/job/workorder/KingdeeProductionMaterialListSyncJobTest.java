package cn.iocoder.yudao.module.mes.job.workorder;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeFullSyncHandler;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncTask;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncResult;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KingdeeProductionMaterialListSyncJobTest {

    @Test
    void execute_shouldSyncProductionMaterialListsByRuntimeWindow() {
        MesKingdeeProductionMaterialListSyncService syncService =
                mock(MesKingdeeProductionMaterialListSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        MesKingdeeProductionMaterialListSyncResult result = new MesKingdeeProductionMaterialListSyncResult();
        result.addCreated(100L);
        result.addUpdated(101L);
        when(syncService.syncModifiedBetween(windowStart, windowEnd)).thenReturn(result);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> invocation.<ErpKingdeeSyncTask>getArgument(1).run(
                        ErpKingdeeSyncContext.builder()
                                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST)
                                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                                .windowStart(windowStart)
                                .windowEnd(windowEnd)
                                .build()));
        KingdeeProductionMaterialListSyncJob job = new KingdeeProductionMaterialListSyncJob(syncService, runtimeService);

        String output = job.execute("");

        assertTrue(output.contains("created=1"));
        assertTrue(output.contains("updated=1"));
        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        verify(runtimeService).executeSync(commandCaptor.capture(), any(ErpKingdeeSyncTask.class));
        assertEquals(ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST, commandCaptor.getValue().getSyncType());
        verify(syncService).syncModifiedBetween(windowStart, windowEnd);
    }

    @Test
    void execute_shouldExposeRuntimeFailure() {
        MesKingdeeProductionMaterialListSyncService syncService =
                mock(MesKingdeeProductionMaterialListSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenThrow(new IllegalStateException("material runtime failed"));
        KingdeeProductionMaterialListSyncJob job = new KingdeeProductionMaterialListSyncJob(syncService, runtimeService);

        Assertions.assertThrows(IllegalStateException.class, () -> job.execute(""));
    }

    @Test
    void execute_shouldDispatchExplicitFullParameterToFullSync() {
        MesKingdeeProductionMaterialListSyncService syncService =
                mock(MesKingdeeProductionMaterialListSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        MesKingdeeProductionMaterialListSyncResult result = new MesKingdeeProductionMaterialListSyncResult();
        result.addCreated(100L);
        result.addSkipped("BILL-101");
        when(syncService.syncAllSkipExisting()).thenReturn(result);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> invocation.<ErpKingdeeSyncTask>getArgument(1).run(
                        ErpKingdeeSyncContext.builder()
                                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST)
                                .triggerType(ErpKingdeeSyncTriggerTypeEnum.FULL)
                                .windowEnd(LocalDateTime.of(2026, 6, 12, 9, 0))
                                .build()));
        KingdeeProductionMaterialListSyncJob job = new KingdeeProductionMaterialListSyncJob(syncService, runtimeService);

        String output = job.execute(ErpKingdeeFullSyncHandler.FULL_SYNC_JOB_PARAM);

        assertTrue(output.contains("full sync"));
        verify(syncService).syncAllSkipExisting();
        verify(syncService, never()).syncModifiedBetween(any(), any());
    }

}
