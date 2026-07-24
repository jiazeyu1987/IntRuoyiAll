package cn.iocoder.yudao.module.erp.service.stock.sync;

import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpKingdeeWarehouseSyncRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductCategoryMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpKingdeeWarehouseSyncRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeInventoryClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeInventoryRow;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpKingdeeStockSyncServiceImplTest {

    @Mock
    private ErpKingdeeInventoryClient inventoryClient;
    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpWarehouseMapper warehouseMapper;
    @Mock
    private ErpKingdeeWarehouseSyncRecordMapper warehouseSyncRecordMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpProductCategoryMapper productCategoryMapper;
    @Mock
    private ErpProductUnitMapper productUnitMapper;

    private ErpKingdeeProperties kingdeeProperties;
    private ErpKingdeeStockSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        kingdeeProperties = new ErpKingdeeProperties();
        kingdeeProperties.setBaseUrl("https://k3.example.com");
        kingdeeProperties.setAcctId("acct");
        kingdeeProperties.setUsername("user");
        kingdeeProperties.setPassword("password");
        kingdeeProperties.setLcid(2052);
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        syncService = new ErpKingdeeStockSyncServiceImpl(
                inventoryClient, kingdeeConfigService, stockMapper, warehouseMapper,
                warehouseSyncRecordMapper, productMapper, productCategoryMapper, productUnitMapper);
    }

    @Test
    void syncStocks_aggregatesRowsAndUpsertsWarehousesAndProducts() {
        ErpKingdeeInventoryRow row1 = buildInventoryRow("LOT001", new BigDecimal("5"));
        ErpKingdeeInventoryRow row2 = buildInventoryRow("LOT002", new BigDecimal("3"));
        when(inventoryClient.fetchInventoryRows(kingdeeProperties)).thenReturn(List.of(row1, row2));
        when(warehouseSyncRecordMapper.selectBySourceKey("892", "CK001")).thenReturn(null);
        when(warehouseMapper.selectByName("Org A / Warehouse A")).thenReturn(null);
        when(productMapper.selectListByBarCodes(any(Collection.class)))
                .thenReturn(List.of(), List.of(new ErpProductDO().setId(20L).setBarCode("MAT-001")));
        when(productCategoryMapper.selectByCode("KINGDEE_STOCK_PRODUCT")).thenReturn(null);
        when(productUnitMapper.selectByName("PCS")).thenReturn(null);
        doAnswer(invocation -> {
            ErpWarehouseDO warehouse = invocation.getArgument(0);
            warehouse.setId(30L);
            return 1;
        }).when(warehouseMapper).insert(any(ErpWarehouseDO.class));
        doAnswer(invocation -> {
            ErpProductCategoryDO category = invocation.getArgument(0);
            category.setId(40L);
            return 1;
        }).when(productCategoryMapper).insert(any(ErpProductCategoryDO.class));
        doAnswer(invocation -> {
            ErpProductUnitDO unit = invocation.getArgument(0);
            unit.setId(50L);
            return 1;
        }).when(productUnitMapper).insert(any(ErpProductUnitDO.class));
        when(productMapper.insertBatch(any(Collection.class), eq(1000))).thenReturn(true);

        ErpKingdeeStockSyncResult result = syncService.syncStocks();

        assertEquals(1, result.getSyncedCount());
        ArgumentCaptor<List<ErpStockDO>> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockMapper).insertBatch(stockCaptor.capture());
        assertEquals(new BigDecimal("8"), stockCaptor.getValue().get(0).getCount());
        verify(stockMapper, never()).delete(any());
        verify(warehouseSyncRecordMapper).insert(any(ErpKingdeeWarehouseSyncRecordDO.class));
    }

    @Test
    void syncStocksModifiedBetween_upsertsChangedKeysAndPreservesZeroQuantityWithoutDeletingAllStocks() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeeInventoryRow zeroRow = buildInventoryRow("LOT001", BigDecimal.ZERO);
        when(inventoryClient.fetchInventoryRowsModifiedBetween(kingdeeProperties, windowStart, windowEnd))
                .thenReturn(List.of(zeroRow));
        when(warehouseSyncRecordMapper.selectBySourceKey("892", "CK001"))
                .thenReturn(new ErpKingdeeWarehouseSyncRecordDO().setWarehouseId(30L));
        when(productMapper.selectListByBarCodes(any(Collection.class)))
                .thenReturn(List.of(new ErpProductDO().setId(20L).setBarCode("MAT-001")));
        when(stockMapper.selectByProductIdAndWarehouseId(20L, 30L))
                .thenReturn(new ErpStockDO().setId(99L).setProductId(20L).setWarehouseId(30L)
                        .setCount(new BigDecimal("5")));

        ErpKingdeeStockSyncResult result = syncService.syncStocksModifiedBetween(windowStart, windowEnd);

        assertEquals(1, result.getSyncedCount());
        ArgumentCaptor<ErpStockDO> stockCaptor = ArgumentCaptor.forClass(ErpStockDO.class);
        verify(stockMapper).updateById(stockCaptor.capture());
        assertEquals(99L, stockCaptor.getValue().getId());
        assertEquals(BigDecimal.ZERO, stockCaptor.getValue().getCount());
        verify(stockMapper, never()).delete(any());
        verify(stockMapper, never()).insertBatch(any(Collection.class));
    }

    private static ErpKingdeeInventoryRow buildInventoryRow(String lotNumber, BigDecimal quantity) {
        ErpKingdeeInventoryRow row = new ErpKingdeeInventoryRow();
        row.setMaterialNumber("MAT-001");
        row.setMaterialName("Material A");
        row.setMaterialSpecification("12ml");
        row.setQuantity(quantity);
        row.setWarehouseNumber("CK001");
        row.setWarehouseName("Warehouse A");
        row.setStockOrgNumber("892");
        row.setStockOrgName("Org A");
        row.setUnitName("PCS");
        row.setLotNumber(lotNumber);
        return row;
    }
}
