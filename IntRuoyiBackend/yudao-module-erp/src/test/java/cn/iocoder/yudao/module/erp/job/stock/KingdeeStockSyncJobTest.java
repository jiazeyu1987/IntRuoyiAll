package cn.iocoder.yudao.module.erp.job.stock;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockSyncResult;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockSyncService;
import cn.iocoder.yudao.module.erp.service.stock.kingdee.ErpKingdeeInventoryListService;
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

class KingdeeStockSyncJobTest {

    @Test
    void execute_shouldSyncStocksByRuntimeWindow() {
        ErpKingdeeStockSyncService stockSyncService = mock(ErpKingdeeStockSyncService.class);
        ErpKingdeeInventoryListService inventoryListService = mock(ErpKingdeeInventoryListService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeeStockSyncResult stockResult = new ErpKingdeeStockSyncResult();
        stockResult.setSyncedCount(3);
        when(stockSyncService.syncStocksModifiedBetween(windowStart, windowEnd)).thenReturn(stockResult);
        when(inventoryListService.syncModifiedBetween(windowStart, windowEnd)).thenReturn(3);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    return task.run(ErpKingdeeSyncContext.builder()
                            .syncType(ErpKingdeeSyncTypeEnum.STOCK)
                            .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                            .windowStart(windowStart)
                            .windowEnd(windowEnd)
                            .build());
                });
        KingdeeStockSyncJob job = new KingdeeStockSyncJob(stockSyncService, inventoryListService, runtimeService);

        String output = job.execute("");

        assertTrue(output.contains("synced=3"));
        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        verify(runtimeService).executeSync(commandCaptor.capture(), any(ErpKingdeeSyncTask.class));
        assertEquals(ErpKingdeeSyncTypeEnum.STOCK, commandCaptor.getValue().getSyncType());
        assertEquals(ErpKingdeeSyncTriggerTypeEnum.AUTO, commandCaptor.getValue().getTriggerType());
        assertNotNull(commandCaptor.getValue().getWindowEnd());
        verify(stockSyncService).syncStocksModifiedBetween(windowStart, windowEnd);
        verify(inventoryListService).syncModifiedBetween(windowStart, windowEnd);
    }

    @Test
    void execute_shouldUseInitializedIncrementalWindowWhenRuntimeWatermarkIsMissing() {
        ErpKingdeeStockSyncService stockSyncService = mock(ErpKingdeeStockSyncService.class);
        ErpKingdeeInventoryListService inventoryListService = mock(ErpKingdeeInventoryListService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeeStockSyncResult stockResult = new ErpKingdeeStockSyncResult();
        stockResult.setSyncedCount(5);
        when(stockSyncService.syncStocksModifiedBetween(windowEnd, windowEnd)).thenReturn(stockResult);
        when(inventoryListService.syncModifiedBetween(windowEnd, windowEnd)).thenReturn(5);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    return task.run(ErpKingdeeSyncContext.builder()
                            .syncType(ErpKingdeeSyncTypeEnum.STOCK)
                            .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                            .windowStart(windowEnd)
                            .windowEnd(windowEnd)
                            .build());
                });
        KingdeeStockSyncJob job = new KingdeeStockSyncJob(stockSyncService, inventoryListService, runtimeService);

        String output = job.execute("");

        assertTrue(output.contains("synced=5"));
        verify(stockSyncService).syncStocksModifiedBetween(windowEnd, windowEnd);
        verify(inventoryListService).syncModifiedBetween(windowEnd, windowEnd);
        verify(stockSyncService, never()).syncStocks();
    }

    @Test
    void execute_shouldExposeRuntimeFailure() {
        ErpKingdeeStockSyncService stockSyncService = mock(ErpKingdeeStockSyncService.class);
        ErpKingdeeInventoryListService inventoryListService = mock(ErpKingdeeInventoryListService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenThrow(new IllegalStateException("stock runtime failed"));
        KingdeeStockSyncJob job = new KingdeeStockSyncJob(stockSyncService, inventoryListService, runtimeService);

        Assertions.assertThrows(IllegalStateException.class, () -> job.execute(""));
    }

}
