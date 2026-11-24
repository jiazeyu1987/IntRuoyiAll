package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductUnitService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeOrderAmounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpPurchaseOrderKingdeeSnapshotTest {
    @Mock private ErpPurchaseOrderMapper orderMapper;
    @Mock private ErpPurchaseOrderItemMapper itemMapper;
    @Mock private ErpNoRedisDAO noRedisDAO;
    @Mock private ErpProductService productService;
    @Mock private ErpProductUnitService unitService;
    @Mock private ErpSupplierService supplierService;
    private ErpPurchaseOrderServiceImpl service;

    @BeforeEach void setUp() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(
                        new com.baomidou.mybatisplus.core.MybatisConfiguration(), "purchase-snapshot-test"),
                ErpPurchaseOrderDO.class);
        service = new ErpPurchaseOrderServiceImpl();
        ReflectionTestUtils.setField(service, "purchaseOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "purchaseOrderItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "noRedisDAO", noRedisDAO);
        ReflectionTestUtils.setField(service, "productService", productService);
        ReflectionTestUtils.setField(service, "productUnitService", unitService);
        ReflectionTestUtils.setField(service, "supplierService", supplierService);
    }

    private ErpPurchaseOrderSaveReqVO request() {
        var req = new ErpPurchaseOrderSaveReqVO();
        req.setSupplierId(5L);
        req.setDepositPrice(new BigDecimal("3.00"));
        var item = new ErpPurchaseOrderSaveReqVO.Item();
        item.setSourceEntryId("entry-1");
        item.setProductId(10L);
        item.setProductUnitId(20L); // Transaction unit differs from the product master unit 99.
        item.setProductPrice(new BigDecimal("100"));
        item.setCount(new BigDecimal("2"));
        item.setTaxPercent(new BigDecimal("13"));
        item.setRemark("ERP entry remark");
        req.setItems(List.of(item));
        return req;
    }

    private void stubCreate(boolean kingdee) {
        when(unitService.getProductUnitMap(Set.of(20L))).thenReturn(Map.of(20L, new ErpProductUnitDO().setId(20L)));
        if (kingdee) {
            when(productService.getProduct(10L)).thenReturn(new ErpProductDO().setId(10L).setUnitId(99L));
        } else {
            when(productService.validProductList(Set.of(10L))).thenReturn(List.of(new ErpProductDO().setId(10L).setUnitId(99L)));
        }
        when(noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_ORDER_NO_PREFIX)).thenReturn("test-order");
        when(orderMapper.insert(any(ErpPurchaseOrderDO.class))).thenAnswer(call -> {
            call.getArgument(0, ErpPurchaseOrderDO.class).setId(100L); return 1;
        });
    }

    @Test void kingdeeSnapshotPreservesTransactionUnitAndActualDiscountedAmounts() {
        stubCreate(true);
        var req = request();
        var snapshot = new ErpKingdeeOrderAmounts();
        snapshot.setProductAmount(new BigDecimal("8.85"));
        snapshot.setTaxAmount(new BigDecimal("1.15"));
        snapshot.setTotalAmount(new BigDecimal("10.00"));
        snapshot.setDiscountAmount(new BigDecimal("2.00"));
        var amount = new ErpKingdeeOrderAmounts.Line();
        amount.setNetAmount(new BigDecimal("8.85"));
        amount.setTaxAmount(new BigDecimal("1.15"));
        amount.setTotalAmount(new BigDecimal("10.00"));
        snapshot.setLines(List.of(amount));
        req.setKingdeeAmounts(snapshot);
        assertEquals(100L, service.createPurchaseOrder(req));
        var header = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(orderMapper).insert(header.capture());
        assertEquals(new BigDecimal("10.00"), header.getValue().getTotalPrice());
        assertEquals(new BigDecimal("8.85"), header.getValue().getTotalProductPrice());
        assertEquals(new BigDecimal("1.15"), header.getValue().getTotalTaxPrice());
        assertEquals(new BigDecimal("2.00"), header.getValue().getDiscountPrice());
        assertEquals(new BigDecimal("3.00"), header.getValue().getDepositPrice());
        assertEquals(new BigDecimal("16.666667"), header.getValue().getDiscountPercent());
        assertEquals(BigDecimal.ZERO, header.getValue().getInCount());
        assertEquals(BigDecimal.ZERO, header.getValue().getReturnCount());
        verify(itemMapper).insertBatch(org.mockito.ArgumentMatchers.<java.util.Collection<ErpPurchaseOrderItemDO>>argThat(items -> {
            var line = items.iterator().next();
            return line.getProductUnitId().equals(20L) && line.getTotalPrice().compareTo(new BigDecimal("8.85")) == 0
                && line.getTaxPrice().compareTo(new BigDecimal("1.15")) == 0 && line.getRemark().equals("ERP entry remark")
                && BigDecimal.ZERO.equals(line.getInCount()) && BigDecimal.ZERO.equals(line.getReturnCount());
        }));
    }

    @Test void manualOrderStillCalculatesAmountsFromPriceAndQuantity() {
        stubCreate(false);
        var req = request();
        req.setDiscountPercent(BigDecimal.TEN);
        service.createPurchaseOrder(req);
        var header = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(orderMapper).insert(header.capture());
        assertEquals(0, new BigDecimal("203.40").compareTo(header.getValue().getTotalPrice()));
        assertEquals(0, new BigDecimal("22.60").compareTo(header.getValue().getDiscountPrice()));
        assertEquals(BigDecimal.ZERO, header.getValue().getInCount());
        assertEquals(BigDecimal.ZERO, header.getValue().getReturnCount());
        verify(itemMapper).insertBatch(org.mockito.ArgumentMatchers.<java.util.Collection<ErpPurchaseOrderItemDO>>argThat(items ->
            items.iterator().next().getProductUnitId().equals(20L)
                    && BigDecimal.ZERO.equals(items.iterator().next().getInCount())
                    && BigDecimal.ZERO.equals(items.iterator().next().getReturnCount())));
    }

    @Test void discrepantSourceAmountsArePreservedOnCreateAndUpdate() {
        stubCreate(true);
        var req = request();
        var source = new ErpKingdeeOrderAmounts();
        source.setProductAmount(BigDecimal.ZERO);
        source.setTaxAmount(BigDecimal.ZERO);
        source.setTotalAmount(new BigDecimal("448.25"));
        source.setDiscountAmount(BigDecimal.ZERO);
        var line = new ErpKingdeeOrderAmounts.Line();
        line.setNetAmount(BigDecimal.ZERO);
        line.setTaxAmount(BigDecimal.ZERO);
        line.setTotalAmount(new BigDecimal("448.25"));
        source.setLines(List.of(line));
        req.setKingdeeAmounts(source);
        assertEquals(100L, service.createPurchaseOrder(req));
        var inserted = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(orderMapper).insert(inserted.capture());
        when(orderMapper.selectById(100L)).thenReturn(new ErpPurchaseOrderDO().setId(100L).setStatus(10));
        when(itemMapper.selectListByOrderId(100L)).thenReturn(List.of());
        req.setId(100L);
        service.updatePurchaseOrder(req);
        var updated = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(orderMapper).updateById(updated.capture());
        for (var header : List.of(inserted.getValue(), updated.getValue())) {
            assertEquals(BigDecimal.ZERO, header.getTotalProductPrice());
            assertEquals(BigDecimal.ZERO, header.getTotalTaxPrice());
            assertEquals(new BigDecimal("448.25"), header.getTotalPrice());
            assertEquals(BigDecimal.ZERO, header.getDiscountPrice());
        }
        verify(itemMapper, times(2)).insertBatch(org.mockito.ArgumentMatchers.<java.util.Collection<ErpPurchaseOrderItemDO>>argThat(items ->
                items.size() == 1 && items.iterator().next().getTotalPrice().compareTo(BigDecimal.ZERO) == 0
                        && items.iterator().next().getTaxPrice().compareTo(BigDecimal.ZERO) == 0));
        assertEquals(new BigDecimal("448.25"), source.getLines().get(0).getTotalAmount());
    }

    @Test void adminJsonCannotSupplyTrustedKingdeeAmounts() {
        var req = JsonUtils.parseObject("{\"kingdeeAmounts\":{\"totalAmount\":1},\"remark\":\"manual\"}", ErpPurchaseOrderSaveReqVO.class);
        assertNull(req.getKingdeeAmounts());
        assertEquals("manual", req.getRemark());
        req.setKingdeeAmounts(new ErpKingdeeOrderAmounts());
        assertFalse(JsonUtils.toJsonString(req).contains("kingdeeAmounts"));
    }

    @Test void trustedSourceDraftPreservesNullReferencesOnCreateAndUpdate() {
        var req = request();
        req.setSupplierId(null);
        req.getItems().get(0).setProductId(null);
        req.getItems().get(0).setProductUnitId(null);
        req.getItems().get(0).setProductPrice(BigDecimal.ZERO);
        req.getItems().get(0).setCount(BigDecimal.ZERO);
        var source = new ErpKingdeeOrderAmounts();
        source.setProductAmount(BigDecimal.ZERO);
        source.setTaxAmount(BigDecimal.ZERO);
        source.setTotalAmount(BigDecimal.ZERO);
        source.setDiscountAmount(BigDecimal.ZERO);
        var line = new ErpKingdeeOrderAmounts.Line();
        line.setNetAmount(BigDecimal.ZERO);
        line.setTaxAmount(BigDecimal.ZERO);
        line.setTotalAmount(BigDecimal.ZERO);
        source.setLines(List.of(line));
        req.setKingdeeAmounts(source);
        when(noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_ORDER_NO_PREFIX)).thenReturn("draft-order");
        when(orderMapper.insert(any(ErpPurchaseOrderDO.class))).thenAnswer(call -> {
            call.getArgument(0, ErpPurchaseOrderDO.class).setId(100L); return 1;
        });
        assertEquals(100L, service.createPurchaseOrder(req));
        when(orderMapper.selectById(100L)).thenReturn(new ErpPurchaseOrderDO().setId(100L).setStatus(10));
        when(itemMapper.selectListByOrderId(100L)).thenReturn(List.of());
        req.setId(100L);
        service.updatePurchaseOrder(req);
        verifyNoInteractions(supplierService, productService, unitService);
        verify(itemMapper, times(2)).insertBatch(org.mockito.ArgumentMatchers.<java.util.Collection<ErpPurchaseOrderItemDO>>argThat(items -> {
            var item = items.iterator().next();
            return items.size() == 1 && item.getProductId() == null && item.getProductUnitId() == null
                    && item.getCount().signum() == 0 && item.getTotalPrice().signum() == 0;
        }));
        var clear = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(orderMapper).update(org.mockito.ArgumentMatchers.<ErpPurchaseOrderDO>isNull(), clear.capture());
        var wrapper = (com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<?>) clear.getValue();
        assertTrue(wrapper.getSqlSet().contains("supplier_id="));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(null));
    }

    @Test void manualOrderStillRejectsMissingUnit() {
        var req = request();
        req.getItems().get(0).setProductUnitId(null);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.createPurchaseOrder(req));
        verifyNoInteractions(orderMapper, itemMapper, unitService);
    }

    @Test void editingInitializesNewLinesWithoutOverwritingExistingFulfillmentCounts() {
        var req = request();
        req.setId(100L);
        req.getItems().get(0).setId(201L);
        var added = new ErpPurchaseOrderSaveReqVO.Item();
        added.setProductId(10L);
        added.setProductUnitId(20L);
        added.setProductPrice(BigDecimal.ONE);
        added.setCount(BigDecimal.TEN);
        req.setItems(List.of(req.getItems().get(0), added));
        when(unitService.getProductUnitMap(Set.of(20L))).thenReturn(Map.of(20L, new ErpProductUnitDO().setId(20L)));
        when(orderMapper.selectById(100L)).thenReturn(new ErpPurchaseOrderDO().setId(100L).setStatus(10));
        when(itemMapper.selectListByOrderId(100L)).thenReturn(List.of(new ErpPurchaseOrderItemDO()
                .setId(201L).setOrderId(100L).setInCount(BigDecimal.ONE).setReturnCount(BigDecimal.ZERO)));

        service.updatePurchaseOrder(req);

        verify(itemMapper).insertBatch(org.mockito.ArgumentMatchers.<java.util.Collection<ErpPurchaseOrderItemDO>>argThat(items -> {
            var line = items.iterator().next();
            return items.size() == 1 && line.getOrderId().equals(100L)
                    && BigDecimal.ZERO.equals(line.getInCount()) && BigDecimal.ZERO.equals(line.getReturnCount());
        }));
        verify(itemMapper).updateBatch(org.mockito.ArgumentMatchers.<java.util.Collection<ErpPurchaseOrderItemDO>>argThat(items -> {
            var line = items.iterator().next();
            return items.size() == 1 && line.getId().equals(201L)
                    && line.getInCount() == null && line.getReturnCount() == null;
        }));
    }

    @Test void calculatedInboundAndReturnCountsCanReplaceLegacyNullItemTotals() {
        var item = new ErpPurchaseOrderItemDO().setId(701L).setOrderId(501L).setCount(BigDecimal.TEN);
        when(itemMapper.selectListByOrderId(501L)).thenReturn(List.of(item));

        service.updatePurchaseOrderInCount(501L, Map.of(701L, BigDecimal.ONE));
        item.setInCount(BigDecimal.ONE);
        service.updatePurchaseOrderReturnCount(501L, Map.of(701L, BigDecimal.ONE));

        verify(itemMapper).updateById(org.mockito.ArgumentMatchers.<ErpPurchaseOrderItemDO>argThat(value -> value.getId().equals(701L)
                && BigDecimal.ONE.equals(value.getInCount())));
        verify(itemMapper).updateById(org.mockito.ArgumentMatchers.<ErpPurchaseOrderItemDO>argThat(value -> value.getId().equals(701L)
                && BigDecimal.ONE.equals(value.getReturnCount())));
        verify(orderMapper).updateById(org.mockito.ArgumentMatchers.<ErpPurchaseOrderDO>argThat(value -> BigDecimal.ONE.equals(value.getInCount())));
        verify(orderMapper).updateById(org.mockito.ArgumentMatchers.<ErpPurchaseOrderDO>argThat(value -> BigDecimal.ONE.equals(value.getReturnCount())));
    }

    @Test void methodValidationAllowsNullSourceSupplierButRejectsManualNullSupplier() throws Exception {
        var req = request();
        req.setSupplierId(null);
        req.setOrderTime(java.time.LocalDateTime.of(2023, 2, 13, 0, 0));
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator().forExecutables();
            for (String name : List.of("createPurchaseOrder", "updatePurchaseOrder")) {
                var method = ErpPurchaseOrderService.class.getMethod(name, ErpPurchaseOrderSaveReqVO.class);
                req.setKingdeeAmounts(null);
                var manual = validator.validateParameters(service, method, new Object[]{req});
                assertEquals(1, manual.size());
                assertEquals("供应商编号不能为空", manual.iterator().next().getMessage());
                req.setKingdeeAmounts(new ErpKingdeeOrderAmounts());
                assertTrue(validator.validateParameters(service, method, new Object[]{req}).isEmpty());
            }
        }
        assertFalse(JsonUtils.toJsonString(req).contains("supplierSpecifiedOrTrustedSource"));
    }
}
