package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductUnitService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderServiceImpl;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpKingdeeHistoricalOrderProductTest {
    private final ErpProductService products = mock(ErpProductService.class);
    private final ErpProductUnitService units = mock(ErpProductUnitService.class);

    private Object request(boolean sale, boolean historical) {
        ErpKingdeeOrderAmounts source = new ErpKingdeeOrderAmounts();
        source.setProductAmount(BigDecimal.TEN);
        source.setTaxAmount(BigDecimal.ZERO);
        source.setTotalAmount(BigDecimal.TEN);
        source.setDiscountAmount(BigDecimal.ZERO);
        var line = new ErpKingdeeOrderAmounts.Line();
        line.setNetAmount(BigDecimal.TEN); line.setTaxAmount(BigDecimal.ZERO); line.setTotalAmount(BigDecimal.TEN);
        source.setLines(List.of(line));
        if (sale) {
            var req = new ErpSaleOrderSaveReqVO();
            var item = new ErpSaleOrderSaveReqVO.Item();
            item.setProductId(7L); item.setProductUnitId(8L);
            item.setProductPrice(BigDecimal.TEN); item.setCount(BigDecimal.ONE);
            req.setItems(List.of(item)); req.setKingdeeAmounts(historical ? source : null);
            return req;
        }
        var req = new ErpPurchaseOrderSaveReqVO();
        var item = new ErpPurchaseOrderSaveReqVO.Item();
        item.setProductId(7L); item.setProductUnitId(8L);
        item.setProductPrice(BigDecimal.TEN); item.setCount(BigDecimal.ONE);
        req.setItems(List.of(item)); req.setKingdeeAmounts(historical ? source : null);
        return req;
    }

    private List<?> validate(boolean sale, Object request) {
        Object service = sale ? new ErpSaleOrderServiceImpl() : new ErpPurchaseOrderServiceImpl();
        ReflectionTestUtils.setField(service, "productService", products);
        ReflectionTestUtils.setField(service, "productUnitService", units);
        when(units.getProductUnitMap(Set.of(8L))).thenReturn(Map.of(8L, new ErpProductUnitDO().setId(8L)));
        return ReflectionTestUtils.invokeMethod(service, sale ? "validateSaleOrderItems" : "validatePurchaseOrderItems", request);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void historicalImportKeepsDisabledProductWithoutEnablingIt(boolean sale) {
        var product = new ErpProductDO().setId(7L).setStatus(1);
        when(products.getProduct(7L)).thenReturn(product);
        assertEquals(1, validate(sale, request(sale, true)).size());
        assertEquals(1, product.getStatus());
        verify(products).getProduct(7L);
        verifyNoMoreInteractions(products);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void historicalImportStillRejectsMissingProduct(boolean sale) {
        var error = assertThrows(ServiceException.class, () -> validate(sale, request(sale, true)));
        assertEquals(PRODUCT_NOT_EXISTS.getCode(), error.getCode());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void manualOrderStillRequiresEnabledProducts(boolean sale) {
        when(products.validProductList(Set.of(7L))).thenThrow(exception(PRODUCT_NOT_ENABLE, "disabled"));
        var error = assertThrows(ServiceException.class, () -> validate(sale, request(sale, false)));
        assertEquals(PRODUCT_NOT_ENABLE.getCode(), error.getCode());
        verify(products, never()).getProduct(anyLong());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void externalRequestCannotSetTrustedErpSnapshot(boolean sale) throws Exception {
        Class<?> requestType = sale ? ErpSaleOrderSaveReqVO.class : ErpPurchaseOrderSaveReqVO.class;
        Object req = new ObjectMapper().readValue("{\"kingdeeAmounts\":{\"totalAmount\":10}}", requestType);
        assertNull(ReflectionTestUtils.getField(req, "kingdeeAmounts"));
    }
}
