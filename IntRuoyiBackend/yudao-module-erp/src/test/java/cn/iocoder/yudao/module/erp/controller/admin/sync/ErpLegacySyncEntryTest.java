package cn.iocoder.yudao.module.erp.controller.admin.sync;

import cn.iocoder.yudao.module.erp.controller.admin.product.ErpProductController;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpKingdeeProductCodeSyncReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.ErpStockController;
import cn.iocoder.yudao.module.erp.service.product.sync.*;
import cn.iocoder.yudao.module.erp.service.stock.sync.*;
import cn.iocoder.yudao.module.erp.service.sync.runtime.*;
import cn.iocoder.yudao.module.erp.enums.sync.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpLegacySyncEntryTest {
    private ErpKingdeeSyncRuntimeService runtime(ErpKingdeeSyncTypeEnum type) {
        ErpKingdeeSyncRuntimeService runtime = mock(ErpKingdeeSyncRuntimeService.class);
        when(runtime.executeSync(any(), any())).thenAnswer(invocation -> {
            ErpKingdeeSyncCommand command = invocation.getArgument(0);
            assertEquals(type, command.getSyncType());
            assertEquals(ErpKingdeeSyncTriggerTypeEnum.MANUAL, command.getTriggerType());
            ErpKingdeeSyncTask task = invocation.getArgument(1);
            ErpKingdeeSyncRunResult result = task.run(null);
            assertNull(result.getWatermarkTime());
            assertEquals(2, result.getCreatedCount());
            assertEquals(3, result.getUpdatedCount());
            assertEquals(4, result.getSkippedCount());
            return result;
        });
        return runtime;
    }

    @Test
    void productEntriesKeepScopeAndCounts() {
        ErpProductController controller = new ErpProductController();
        ErpKingdeeProductSyncService service = mock(ErpKingdeeProductSyncService.class);
        ErpKingdeeProductSyncResult result = new ErpKingdeeProductSyncResult();
        for (int i = 0; i < 2; i++) result.addCreated("C" + i);
        for (int i = 0; i < 3; i++) result.addUpdated("U" + i);
        for (int i = 0; i < 4; i++) result.addSkipped("S" + i);
        when(service.syncProducts()).thenReturn(result);
        when(service.syncProductsByNumbers(List.of("P001"))).thenReturn(result);
        ReflectionTestUtils.setField(controller, "kingdeeProductSyncService", service);
        ErpKingdeeSyncRuntimeService runtime = runtime(ErpKingdeeSyncTypeEnum.PRODUCT);
        ReflectionTestUtils.setField(controller, "syncRuntimeService", runtime);
        assertEquals(2, controller.syncKingdeeProducts().getData().getCreatedCount());
        ErpKingdeeProductCodeSyncReqVO request = new ErpKingdeeProductCodeSyncReqVO();
        request.setProductCodes(List.of("P001"));
        assertEquals(3, controller.syncKingdeeProductsByCodes(request).getData().getUpdatedCount());
        verify(service).syncProducts();
        verify(service).syncProductsByNumbers(List.of("P001"));
        verify(runtime, times(2)).executeSync(any(), any());
    }

    @Test
    void stockEntryKeepsScopeAndCounts() {
        ErpStockController controller = new ErpStockController();
        ErpKingdeeStockSyncService service = mock(ErpKingdeeStockSyncService.class);
        ErpKingdeeStockSyncResult result = new ErpKingdeeStockSyncResult();
        result.setCreatedCount(2); result.setUpdatedCount(3); result.setSkippedCount(4); result.setSyncedCount(5);
        when(service.syncStocks()).thenReturn(result);
        ReflectionTestUtils.setField(controller, "kingdeeStockSyncService", service);
        ReflectionTestUtils.setField(controller, "syncRuntimeService", runtime(ErpKingdeeSyncTypeEnum.STOCK));
        assertEquals(5, controller.syncKingdeeStocks().getData().getSyncedCount());
        verify(service).syncStocks();
    }
}
