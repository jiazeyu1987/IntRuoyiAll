package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
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
class ErpSaleOrderKingdeeSnapshotTest {
    @Mock private ErpSaleOrderMapper orderMapper;
    @Mock private ErpSaleOrderItemMapper itemMapper;
    @Mock private ErpNoRedisDAO noRedisDAO;
    @Mock private ErpProductService productService;
    @Mock private ErpProductUnitService unitService;
    @Mock private ErpCustomerService customerService;
    private ErpSaleOrderServiceImpl service;

    @BeforeEach void setUp() {
        service = new ErpSaleOrderServiceImpl();
        ReflectionTestUtils.setField(service, "saleOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "saleOrderItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "noRedisDAO", noRedisDAO);
        ReflectionTestUtils.setField(service, "productService", productService);
        ReflectionTestUtils.setField(service, "productUnitService", unitService);
        ReflectionTestUtils.setField(service, "customerService", customerService);
    }

    private ErpSaleOrderSaveReqVO request() {
        var req = new ErpSaleOrderSaveReqVO();
        req.setCustomerId(5L);
        req.setDepositPrice(new BigDecimal("3.00"));
        var item = new ErpSaleOrderSaveReqVO.Item();
        item.setProductId(10L);
        item.setProductUnitId(20L); // Transaction unit differs from the product master unit 99.
        item.setProductPrice(new BigDecimal("100"));
        item.setCount(new BigDecimal("2"));
        item.setTaxPercent(new BigDecimal("13"));
        item.setRemark("ERP entry remark");
        req.setItems(List.of(item));
        return req;
    }

    private void stubCreate() {
        when(unitService.getProductUnitMap(Set.of(20L))).thenReturn(Map.of(20L, new ErpProductUnitDO().setId(20L)));
        when(productService.validProductList(Set.of(10L))).thenReturn(List.of(new ErpProductDO().setId(10L).setUnitId(99L)));
        when(noRedisDAO.generate(ErpNoRedisDAO.SALE_ORDER_NO_PREFIX)).thenReturn("test-order");
        when(orderMapper.insert(any(ErpSaleOrderDO.class))).thenAnswer(call -> {
            call.getArgument(0, ErpSaleOrderDO.class).setId(100L); return 1;
        });
    }

    @Test void kingdeeSnapshotPreservesTransactionUnitAndActualDiscountedAmounts() {
        stubCreate();
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
        assertEquals(100L, service.createSaleOrder(req));
        var header = ArgumentCaptor.forClass(ErpSaleOrderDO.class);
        verify(orderMapper).insert(header.capture());
        assertEquals(new BigDecimal("10.00"), header.getValue().getTotalPrice());
        assertEquals(new BigDecimal("8.85"), header.getValue().getTotalProductPrice());
        assertEquals(new BigDecimal("1.15"), header.getValue().getTotalTaxPrice());
        assertEquals(new BigDecimal("2.00"), header.getValue().getDiscountPrice());
        assertEquals(new BigDecimal("3.00"), header.getValue().getDepositPrice());
        assertEquals(new BigDecimal("16.666667"), header.getValue().getDiscountPercent());
        verify(itemMapper).insertBatch(org.mockito.ArgumentMatchers.<java.util.Collection<ErpSaleOrderItemDO>>argThat(items -> {
            var line = items.iterator().next();
            return line.getProductUnitId().equals(20L) && line.getTotalPrice().compareTo(new BigDecimal("8.85")) == 0
                && line.getTaxPrice().compareTo(new BigDecimal("1.15")) == 0 && line.getRemark().equals("ERP entry remark");
        }));
    }

    @Test void manualOrderStillCalculatesAmountsFromPriceAndQuantity() {
        stubCreate();
        var req = request();
        req.setDiscountPercent(BigDecimal.TEN);
        service.createSaleOrder(req);
        var header = ArgumentCaptor.forClass(ErpSaleOrderDO.class);
        verify(orderMapper).insert(header.capture());
        assertEquals(0, new BigDecimal("203.40").compareTo(header.getValue().getTotalPrice()));
        assertEquals(0, new BigDecimal("22.60").compareTo(header.getValue().getDiscountPrice()));
        verify(itemMapper).insertBatch(org.mockito.ArgumentMatchers.<java.util.Collection<ErpSaleOrderItemDO>>argThat(items ->
            items.iterator().next().getProductUnitId().equals(20L)));
    }

    @Test void adminJsonCannotSupplyTrustedKingdeeAmounts() {
        var req = JsonUtils.parseObject("{\"kingdeeAmounts\":{\"totalAmount\":1},\"remark\":\"manual\"}", ErpSaleOrderSaveReqVO.class);
        assertNull(req.getKingdeeAmounts());
        assertEquals("manual", req.getRemark());
        req.setKingdeeAmounts(new ErpKingdeeOrderAmounts());
        assertFalse(JsonUtils.toJsonString(req).contains("kingdeeAmounts"));
    }
}
