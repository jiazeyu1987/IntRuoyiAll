package cn.iocoder.yudao.module.mes.job.workorder;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncTask;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionOrderSyncResult;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionOrderSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KingdeeProductionOrderSyncJobTest {

    @Test
    void execute_shouldDelegateToKingdeeProductionOrderSyncService() {
        MesKingdeeProductionOrderSyncService syncService = mock(MesKingdeeProductionOrderSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        MesKingdeeProductionOrderSyncResult result = new MesKingdeeProductionOrderSyncResult();
        result.addCreated(100L);
        result.addUpdated(101L);
        result.addFinished(102L);
        result.addCanceled(103L);
        when(syncService.syncWorkOrders(any(ErpKingdeeSyncContext.class))).thenReturn(result);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    return task.run(ErpKingdeeSyncContext.builder()
                            .initialSync(false)
                            .windowStart(java.time.LocalDateTime.of(2026, 6, 23, 10, 0))
                            .windowEnd(java.time.LocalDateTime.of(2026, 6, 24, 10, 0))
                            .build());
                });
        KingdeeProductionOrderSyncJob job = new KingdeeProductionOrderSyncJob(syncService, runtimeService);

        String output = job.execute("");

        assertTrue(output.contains("created=1"));
        assertTrue(output.contains("updated=1"));
        assertTrue(output.contains("finished=1"));
        assertTrue(output.contains("canceled=1"));
        assertTrue(output.contains("skipped=0"));
        verify(syncService).syncWorkOrders(any(ErpKingdeeSyncContext.class));
        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        ArgumentCaptor<ErpKingdeeSyncTask> taskCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncTask.class);
        verify(runtimeService).executeSync(commandCaptor.capture(), taskCaptor.capture());
        assertEquals(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER, commandCaptor.getValue().getSyncType());
        assertEquals(ErpKingdeeSyncTriggerTypeEnum.AUTO, commandCaptor.getValue().getTriggerType());
        assertNotNull(commandCaptor.getValue().getInitialWindowStart());
        assertNotNull(commandCaptor.getValue().getWindowEnd());
    }

    @Test
    void execute_shouldExposeRuntimeFailure() {
        MesKingdeeProductionOrderSyncService syncService = mock(MesKingdeeProductionOrderSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenThrow(new IllegalStateException("kingdee runtime failed"));
        KingdeeProductionOrderSyncJob job = new KingdeeProductionOrderSyncJob(syncService, runtimeService);

        Assertions.assertThrows(IllegalStateException.class, () -> job.execute(""));
    }

}
