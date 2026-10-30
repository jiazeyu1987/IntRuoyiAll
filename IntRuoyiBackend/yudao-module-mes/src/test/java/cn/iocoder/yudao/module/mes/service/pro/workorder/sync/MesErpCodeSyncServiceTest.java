package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.service.product.sync.ErpKingdeeProductSyncResult;
import cn.iocoder.yudao.module.erp.service.product.sync.ErpKingdeeProductSyncService;
import cn.iocoder.yudao.module.erp.service.production.kingdee.ErpKingdeeProductionPickListService;
import cn.iocoder.yudao.module.erp.service.production.kingdee.ErpKingdeeProductionReplenishmentListService;
import cn.iocoder.yudao.module.erp.service.stock.kingdee.ErpKingdeeInventoryListService;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockSyncService;
import cn.iocoder.yudao.module.mes.service.md.item.kingdee.MesKingdeeBomListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesErpCodeSyncServiceTest {
    @Mock private ErpKingdeeProductSyncService products;
    @Mock private ErpKingdeeStockSyncService stocks;
    @Mock private ErpKingdeeInventoryListService inventory;
    @Mock private MesKingdeeBomListService bom;
    @Mock private MesKingdeeProductionMaterialListSyncService materials;
    @Mock private ErpKingdeeProductionPickListService picks;
    @Mock private ErpKingdeeProductionReplenishmentListService replenishments;
    @Mock private MesKingdeeProductionOrderSyncService orders;
    private MesErpCodeSyncService service;

    @BeforeEach
    void setUp() {
        service = new MesErpCodeSyncService(products, stocks, inventory, bom, materials,
                picks, replenishments, orders);
    }

    @Test
    void successfulSyncReturnsTrimmedCodeAndOnlyInvokesChosenBusiness() {
        when(materials.syncByCode("PPBOM-001")).thenReturn(3);
        var result = service.sync("PRODUCTION_MATERIAL_LIST", " PPBOM-001 ");
        assertEquals("PPBOM-001", result.code());
        assertEquals(3, result.syncedCount());
        verify(materials).syncByCode("PPBOM-001");
        verifyNoInteractions(products, stocks, inventory, bom, picks, replenishments, orders);
    }

    @Test
    void unchangedProductVerifiedInErpIsSuccessful() {
        var result = new ErpKingdeeProductSyncResult();
        result.addSkipped("MAT-001");
        when(products.syncProductsByNumbers(List.of("MAT-001"))).thenReturn(result);
        assertEquals(1, service.sync("PRODUCT", "MAT-001").syncedCount());
    }

    @Test
    void missingCodeFailsInsteadOfReportingSuccess() {
        when(picks.syncByCode("MISSING")).thenReturn(0);
        var error = assertThrows(ServiceException.class,
                () -> service.sync("PRODUCTION_PICK_LIST", "MISSING"));
        assertTrue(error.getMessage().contains("未找到"));
    }

    @Test
    void erpFailurePropagatesWithOriginalReason() {
        var failure = new IllegalStateException("ERP连接失败");
        when(stocks.syncByCode("MAT-001")).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.sync("STOCK", "MAT-001")));
    }

    @Test
    void unsupportedTypeAndInvalidCodeDoNotInvokeErp() {
        assertThrows(ServiceException.class, () -> service.sync("UNKNOWN", "A"));
        assertThrows(ServiceException.class, () -> service.sync("BOM", "   "));
        assertThrows(ServiceException.class, () -> service.sync("BOM", "A\nB"));
        verifyNoInteractions(products, stocks, inventory, bom, materials, picks, replenishments, orders);
    }
}
