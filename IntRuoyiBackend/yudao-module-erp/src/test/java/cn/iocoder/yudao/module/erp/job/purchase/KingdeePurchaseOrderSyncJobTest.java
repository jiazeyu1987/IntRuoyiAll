package cn.iocoder.yudao.module.erp.job.purchase;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeePurchaseOrderSyncResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeePurchaseOrderSyncService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KingdeePurchaseOrderSyncJobTest {

    @Test
    void execute_shouldSyncPurchaseOrdersByRuntimeWindow() {
        ErpKingdeePurchaseOrderSyncService purchaseOrderSyncService = mock(ErpKingdeePurchaseOrderSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeePurchaseOrderSyncResult syncResult = new ErpKingdeePurchaseOrderSyncResult();
        syncResult.addCreated(1001L);
        syncResult.addSkipped("FID-OLD");
        when(purchaseOrderSyncService.syncPurchaseOrdersModifiedBetween(windowStart, windowEnd)).thenReturn(syncResult);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    return task.run(ErpKingdeeSyncContext.builder()
                            .syncType(ErpKingdeeSyncTypeEnum.PURCHASE_ORDER)
                            .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                            .windowStart(windowStart)
                            .windowEnd(windowEnd)
                            .build());
                });
        KingdeePurchaseOrderSyncJob job = new KingdeePurchaseOrderSyncJob(purchaseOrderSyncService, runtimeService);

        String output = job.execute("");

        assertTrue(output.contains("created=1"));
        assertTrue(output.contains("skipped=1"));
        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        verify(runtimeService).executeSync(commandCaptor.capture(), any(ErpKingdeeSyncTask.class));
        assertEquals(ErpKingdeeSyncTypeEnum.PURCHASE_ORDER, commandCaptor.getValue().getSyncType());
        assertEquals(ErpKingdeeSyncTriggerTypeEnum.AUTO, commandCaptor.getValue().getTriggerType());
        assertNotNull(commandCaptor.getValue().getWindowEnd());
        verify(purchaseOrderSyncService).syncPurchaseOrdersModifiedBetween(windowStart, windowEnd);
    }

    @Test
    void execute_shouldUseInitializedIncrementalWindowWhenRuntimeWatermarkIsMissing() {
        ErpKingdeePurchaseOrderSyncService purchaseOrderSyncService = mock(ErpKingdeePurchaseOrderSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeePurchaseOrderSyncResult syncResult = new ErpKingdeePurchaseOrderSyncResult();
        syncResult.addCreated(1001L);
        when(purchaseOrderSyncService.syncPurchaseOrdersModifiedBetween(windowEnd, windowEnd)).thenReturn(syncResult);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    return task.run(ErpKingdeeSyncContext.builder()
                            .syncType(ErpKingdeeSyncTypeEnum.PURCHASE_ORDER)
                            .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                            .windowStart(windowEnd)
                            .windowEnd(windowEnd)
                            .build());
                });
        KingdeePurchaseOrderSyncJob job = new KingdeePurchaseOrderSyncJob(purchaseOrderSyncService, runtimeService);

        String output = job.execute("");

        assertTrue(output.contains("created=1"));
        verify(purchaseOrderSyncService).syncPurchaseOrdersModifiedBetween(windowEnd, windowEnd);
        verify(purchaseOrderSyncService, never()).syncPurchaseOrders();
    }

    @Test
    void execute_shouldExposeRuntimeFailure() {
        ErpKingdeePurchaseOrderSyncService purchaseOrderSyncService = mock(ErpKingdeePurchaseOrderSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenThrow(new IllegalStateException("purchase runtime failed"));
        KingdeePurchaseOrderSyncJob job = new KingdeePurchaseOrderSyncJob(purchaseOrderSyncService, runtimeService);

        Assertions.assertThrows(IllegalStateException.class, () -> job.execute(""));
    }

    @Test
    void boundedFullSync_usesClickWindowSkipExistingAndDoesNotAdvanceWatermark() {
        ErpKingdeePurchaseOrderSyncService service = mock(ErpKingdeePurchaseOrderSyncService.class);
        ErpKingdeeSyncRuntimeService runtime = mock(ErpKingdeeSyncRuntimeService.class);
        LocalDateTime end = LocalDateTime.of(2026, 10, 30, 15, 32);
        LocalDateTime start = end.minusDays(7);
        ErpKingdeePurchaseOrderSyncResult result = new ErpKingdeePurchaseOrderSyncResult();
        result.addCreated(1L);
        result.addSkipped("existing");
        when(service.syncPurchaseOrdersFullSkipExisting(start, end)).thenReturn(result);
        when(runtime.executeSync(any(), any())).thenAnswer(invocation -> {
            ErpKingdeeSyncCommand command = invocation.getArgument(0);
            assertEquals(start, command.getInitialWindowStart());
            assertEquals(end, command.getWindowEnd());
            assertTrue(command.isForceInitialWindowStart());
            ErpKingdeeSyncTask task = invocation.getArgument(1);
            var completed = task.run(ErpKingdeeSyncContext.builder().windowStart(start).windowEnd(end).build());
            Assertions.assertNull(completed.getWatermarkTime());
            assertEquals(1, completed.getSkippedCount());
            return completed;
        });
        new KingdeePurchaseOrderSyncJob(service, runtime).execute(
                cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeFullSyncHandler.windowParam(start, end));
        verify(service).syncPurchaseOrdersFullSkipExisting(start, end);
        verify(service, never()).syncPurchaseOrdersModifiedBetween(any(), any());
    }

}
