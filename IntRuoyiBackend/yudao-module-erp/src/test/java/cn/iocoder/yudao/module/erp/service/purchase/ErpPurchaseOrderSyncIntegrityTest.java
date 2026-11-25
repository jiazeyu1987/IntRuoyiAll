package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.*;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.*;
import cn.iocoder.yudao.module.erp.service.product.*;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeOrderAmounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpPurchaseOrderSyncIntegrityTest {
    @Mock private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Mock private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock private ErpPurchaseInMapper purchaseInMapper;
    @Mock private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Mock private ErpProductService productService;
    @Mock private ErpProductUnitService productUnitService;
    @Mock private ErpSupplierService supplierService;
    @InjectMocks private ErpPurchaseOrderServiceImpl service;
    private ErpPurchaseOrderDO order;
    private ErpPurchaseOrderItemDO stored;
    private ErpPurchaseOrderSaveReqVO req;

    @BeforeEach void prepare() {
        order = new ErpPurchaseOrderDO().setId(100L).setNo("ORDER-1").setStatus(20).setSupplierId(5L);
        stored = new ErpPurchaseOrderItemDO().setId(101L).setOrderId(100L).setSourceEntryId("e1")
                .setProductId(10L).setProductUnitId(20L).setProductPrice(BigDecimal.ONE).setCount(BigDecimal.TEN);
        req = new ErpPurchaseOrderSaveReqVO();
        req.setId(100L);
        req.setSupplierId(5L);
        var item = new ErpPurchaseOrderSaveReqVO.Item();
        item.setSourceEntryId("e1"); item.setProductId(10L); item.setProductUnitId(20L);
        item.setProductPrice(BigDecimal.ONE); item.setCount(BigDecimal.TEN);
        req.setItems(List.of(item));
        var amounts = new ErpKingdeeOrderAmounts();
        amounts.setProductAmount(BigDecimal.TEN); amounts.setTaxAmount(BigDecimal.ZERO);
        amounts.setTotalAmount(BigDecimal.TEN); amounts.setDiscountAmount(BigDecimal.ZERO);
        var line = new ErpKingdeeOrderAmounts.Line();
        line.setNetAmount(BigDecimal.TEN); line.setTaxAmount(BigDecimal.ZERO); line.setTotalAmount(BigDecimal.TEN);
        amounts.setLines(List.of(line)); req.setKingdeeAmounts(amounts);
        lenient().when(purchaseOrderItemMapper.updateSourceItem(any(), eq(100L))).thenReturn(1);
        lenient().when(purchaseOrderMapper.selectById(100L)).thenReturn(order);
        lenient().when(purchaseOrderItemMapper.selectListByOrderId(100L)).thenReturn(List.of(stored));
        lenient().when(productUnitService.getProductUnitMap(Set.of(20L))).thenReturn(Map.of(20L, new ErpProductUnitDO().setId(20L)));
        lenient().when(productService.getProduct(anyLong())).thenAnswer(call -> new ErpProductDO().setId(call.getArgument(0)));
        lenient().when(purchaseOrderMapper.updateByIdAndStatus(eq(100L), eq(20), any())).thenReturn(1);
    }

    private void received() {
        when(purchaseInMapper.selectListByOrderId(100L)).thenReturn(List.of(
                new ErpPurchaseInDO().setId(200L).setNo("FULFILL-1").setTotalCount(new BigDecimal("3"))));
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(List.of(200L)))
                .thenReturn(Map.of(101L, new BigDecimal("3")));
    }

    private void update() { service.updatePurchaseOrderFromKingdee(req, 20, List.of()); }

    @Test void approvedOrderWithActualFulfillmentUpdatesWithoutReversingOrReplacingLines() {
        received();
        when(purchaseReturnMapper.selectListByOrderId(100L)).thenReturn(List.of(
                new ErpPurchaseReturnDO().setId(201L).setNo("RETURN-1").setTotalCount(BigDecimal.ONE)));
        when(purchaseReturnItemMapper.selectOrderItemCountSumMapByReturnIds(List.of(201L))).thenReturn(Map.of(101L, BigDecimal.ONE));
        update();
        var header = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).updateByIdAndStatus(eq(100L), eq(20), header.capture());
        assertEquals(20, header.getValue().getStatus());
        assertEquals(new BigDecimal("3"), header.getValue().getInCount());
        assertEquals(BigDecimal.ONE, header.getValue().getReturnCount());
        verify(purchaseOrderItemMapper).updateSourceItem(ArgumentMatchers.<ErpPurchaseOrderItemDO>argThat(row ->
                row.getId().equals(101L) && row.getInCount().compareTo(new BigDecimal("3")) == 0), eq(100L));
        verify(purchaseOrderItemMapper, never()).deleteByIds(any());
        verify(purchaseOrderItemMapper, never()).insertBatch(ArgumentMatchers.<Collection<ErpPurchaseOrderItemDO>>any());
    }

    @Test void legacyIdentityAdoptedOnlyFromPreviousSnapshotEvenWhenSourceQuantityChanges() {
        stored.setSourceEntryId(null);
        var previous = new ErpPurchaseOrderSaveReqVO.Item();
        previous.setSourceEntryId("e1"); previous.setProductId(10L); previous.setProductUnitId(20L);
        previous.setProductPrice(BigDecimal.ONE); previous.setCount(new BigDecimal("10.000"));
        req.getItems().get(0).setCount(new BigDecimal("12"));
        service.updatePurchaseOrderFromKingdee(req, 20, List.of(previous));
        verify(purchaseOrderItemMapper).updateSourceItem(ArgumentMatchers.<ErpPurchaseOrderItemDO>argThat(row ->
                row.getId().equals(101L) && row.getSourceEntryId().equals("e1")), eq(100L));
    }

    @Test void missingHistoricalSnapshotFailsBeforeWriting() {
        stored.setSourceEntryId(null);
        assertTrue(assertThrows(IllegalStateException.class, this::update).getMessage().contains("无法唯一绑定"));
        verify(purchaseOrderMapper, never()).updateByIdAndStatus(any(), any(), any());
        verify(purchaseOrderItemMapper, never()).deleteByIds(any());
    }

    @Test void referencedEntryCannotBeDeletedOrReplacedByNewEntryId() {
        received(); req.getItems().get(0).setSourceEntryId("e2");
        assertTrue(assertThrows(IllegalStateException.class, this::update).getMessage().contains("不能删除"));
        verify(purchaseOrderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test void referencedProductCannotChange() {
        received(); req.getItems().get(0).setProductId(11L);
        assertThrows(IllegalStateException.class, this::update);
        verify(purchaseOrderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test void quantityCannotFallBelowFulfilledQuantity() {
        received(); req.getItems().get(0).setCount(BigDecimal.ONE);
        assertTrue(assertThrows(IllegalStateException.class, this::update).getMessage().contains("小于已出入库"));
    }

    @Test void linkedOrderCannotReverseSourceApproval() {
        received();
        assertThrows(IllegalStateException.class, () -> service.updatePurchaseOrderFromKingdee(req, 10, List.of()));
    }

    @Test void linkedOrderCannotChangeCounterparty() {
        received(); req.setSupplierId(6L);
        assertThrows(IllegalStateException.class, this::update);
    }

    @Test void contradictoryLocalQuantityFailsInsteadOfResettingIt() {
        stored.setInCount(BigDecimal.ONE);
        assertTrue(assertThrows(IllegalStateException.class, this::update).getMessage().contains("数量不一致"));
    }

    @Test void danglingFulfillmentReferenceFailsInsteadOfDroppingIt() {
        received();
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(List.of(200L))).thenReturn(Map.of(999L, new BigDecimal("3")));
        assertTrue(assertThrows(IllegalStateException.class, this::update).getMessage().contains("不存在的订单明细"));
    }

    @Test void missingFulfillmentLinesFailAgainstDocumentTotal() {
        received();
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(List.of(200L))).thenReturn(Map.of());
        assertTrue(assertThrows(IllegalStateException.class, this::update).getMessage().contains("汇总与明细"));
    }

    @Test void sourceUpdateRequiresTrustedSnapshot() {
        req.setKingdeeAmounts(null);
        assertThrows(IllegalArgumentException.class, this::update);
        verifyNoInteractions(purchaseOrderMapper);
    }

    @Test void manualEditStillRejectsApprovedOrder() {
        assertThrows(RuntimeException.class, () -> service.updatePurchaseOrder(req));
        verify(purchaseOrderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test void unreferencedSourceDraftCanClearProductAndUnitWithoutReplacingIdentity() {
        req.getItems().get(0).setRemark(null);
        req.getItems().get(0).setProductId(null); req.getItems().get(0).setProductUnitId(null);
        update();
        verify(purchaseOrderItemMapper).updateSourceItem(ArgumentMatchers.<ErpPurchaseOrderItemDO>argThat(row ->
                row.getId().equals(101L) && row.getRemark() == null
                && row.getProductId() == null && row.getProductUnitId() == null), eq(100L));
    }

    @Test void concurrentSourceLineRemovalFailsRatherThanReportingSuccess() {
        when(purchaseOrderItemMapper.updateSourceItem(any(), eq(100L))).thenReturn(0);
        assertTrue(assertThrows(IllegalStateException.class, this::update).getMessage().contains("ERP明细更新期间已变化"));
    }

    @Test void nullItemQuantityCanBeRecomputedWithoutNullPointer() {
        service.updatePurchaseOrderInCount(100L, Map.of(101L, BigDecimal.ONE));
        verify(purchaseOrderItemMapper).updateById(ArgumentMatchers.<ErpPurchaseOrderItemDO>argThat(row ->
                row.getId().equals(101L) && row.getInCount().equals(BigDecimal.ONE)));
    }
}
