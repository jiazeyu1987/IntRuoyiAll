package cn.iocoder.yudao.module.mes.job.md;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.sync.ErpKingdeeProductSyncResult;
import cn.iocoder.yudao.module.erp.service.product.sync.ErpKingdeeProductSyncService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncTask;
import cn.iocoder.yudao.module.mes.service.md.item.sync.MesKingdeeItemSyncResult;
import cn.iocoder.yudao.module.mes.service.md.item.sync.MesKingdeeItemSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KingdeeProductItemSyncJobTest {

    @Test
    void execute_shouldSyncProductsByRuntimeWindowAndDeriveChangedMesItems() {
        ErpKingdeeProductSyncService productSyncService = mock(ErpKingdeeProductSyncService.class);
        MesKingdeeItemSyncService itemSyncService = mock(MesKingdeeItemSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeeProductSyncResult productResult = new ErpKingdeeProductSyncResult();
        productResult.addCreated("MAT-NEW");
        productResult.addUpdated("MAT-UPD");
        productResult.addSkipped("MAT-SAME");
        MesKingdeeItemSyncResult itemResult = new MesKingdeeItemSyncResult();
        itemResult.addCreated();
        itemResult.addUpdated();
        when(productSyncService.syncProductsModifiedBetween(windowStart, windowEnd)).thenReturn(productResult);
        when(itemSyncService.syncItemsByProductCodes(any(Collection.class))).thenReturn(itemResult);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    return task.run(ErpKingdeeSyncContext.builder()
                            .syncType(ErpKingdeeSyncTypeEnum.PRODUCT)
                            .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                            .windowStart(windowStart)
                            .windowEnd(windowEnd)
                            .build());
                });
        KingdeeProductItemSyncJob job = new KingdeeProductItemSyncJob(
                productSyncService, itemSyncService, runtimeService);

        String output = job.execute("");

        assertTrue(output.contains("productsCreated=1"));
        assertTrue(output.contains("productsUpdated=1"));
        assertTrue(output.contains("productsSkipped=1"));
        assertTrue(output.contains("itemsCreated=1"));
        assertTrue(output.contains("itemsUpdated=1"));
        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        ArgumentCaptor<ErpKingdeeSyncTask> taskCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncTask.class);
        verify(runtimeService).executeSync(commandCaptor.capture(), taskCaptor.capture());
        assertEquals(ErpKingdeeSyncTypeEnum.PRODUCT, commandCaptor.getValue().getSyncType());
        assertEquals(ErpKingdeeSyncTriggerTypeEnum.AUTO, commandCaptor.getValue().getTriggerType());
        assertNotNull(commandCaptor.getValue().getWindowEnd());
        verify(productSyncService).syncProductsModifiedBetween(windowStart, windowEnd);
        ArgumentCaptor<Collection<String>> codesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(itemSyncService).syncItemsByProductCodes(codesCaptor.capture());
        assertEquals(List.of("MAT-NEW", "MAT-UPD"), List.copyOf(codesCaptor.getValue()));
    }

    @Test
    void execute_shouldUseInitializedIncrementalWindowWhenRuntimeWatermarkIsMissing() {
        ErpKingdeeProductSyncService productSyncService = mock(ErpKingdeeProductSyncService.class);
        MesKingdeeItemSyncService itemSyncService = mock(MesKingdeeItemSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeeProductSyncResult productResult = new ErpKingdeeProductSyncResult();
        productResult.addCreated("MAT-NEW");
        MesKingdeeItemSyncResult itemResult = new MesKingdeeItemSyncResult();
        itemResult.addCreated();
        when(productSyncService.syncProductsModifiedBetween(windowEnd, windowEnd)).thenReturn(productResult);
        when(itemSyncService.syncItemsByProductCodes(any(Collection.class))).thenReturn(itemResult);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    return task.run(ErpKingdeeSyncContext.builder()
                            .syncType(ErpKingdeeSyncTypeEnum.PRODUCT)
                            .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                            .windowStart(windowEnd)
                            .windowEnd(windowEnd)
                            .build());
                });
        KingdeeProductItemSyncJob job = new KingdeeProductItemSyncJob(
                productSyncService, itemSyncService, runtimeService);

        String output = job.execute("");

        assertTrue(output.contains("productsCreated=1"));
        verify(productSyncService).syncProductsModifiedBetween(windowEnd, windowEnd);
        verify(productSyncService, never()).syncProducts();
        ArgumentCaptor<Collection<String>> codesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(itemSyncService).syncItemsByProductCodes(codesCaptor.capture());
        assertEquals(List.of("MAT-NEW"), List.copyOf(codesCaptor.getValue()));
    }

    @Test
    void execute_shouldExposeRuntimeFailure() {
        ErpKingdeeProductSyncService productSyncService = mock(ErpKingdeeProductSyncService.class);
        MesKingdeeItemSyncService itemSyncService = mock(MesKingdeeItemSyncService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenThrow(new IllegalStateException("kingdee runtime failed"));
        KingdeeProductItemSyncJob job = new KingdeeProductItemSyncJob(
                productSyncService, itemSyncService, runtimeService);

        Assertions.assertThrows(IllegalStateException.class, () -> job.execute(""));
    }

}
