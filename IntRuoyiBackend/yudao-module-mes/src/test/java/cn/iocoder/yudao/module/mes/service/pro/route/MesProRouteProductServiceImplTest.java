package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductCopyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteProductServiceImplTest {

    @InjectMocks
    private MesProRouteProductServiceImpl routeProductService;

    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteProductBomService routeProductBomService;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteCandidateConfigService routeCandidateConfigService;

    @Test
    void createRouteProduct_shouldInsertRouteProductWhenRouteEditableAndItemUnique() {
        MesProRouteProductSaveReqVO reqVO = new MesProRouteProductSaveReqVO();
        reqVO.setRouteId(200L);
        reqVO.setItemId(301L);
        reqVO.setQuantity(5);
        reqVO.setProductionTime(new BigDecimal("2.50"));
        reqVO.setTimeUnitType("HOUR");
        reqVO.setRemark("new product");
        when(routeProductMapper.selectByItemId(301L)).thenReturn(null);
        doAnswer(invocation -> {
            MesProRouteProductDO data = invocation.getArgument(0);
            data.setId(101L);
            return 1;
        }).when(routeProductMapper).insert(any(MesProRouteProductDO.class));

        Long createdId = routeProductService.createRouteProduct(reqVO);

        assertEquals(101L, createdId);
        verify(routeService).validateRouteNotEnable(200L);
        ArgumentCaptor<MesProRouteProductDO> productCaptor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper).insert(productCaptor.capture());
        assertEquals(200L, productCaptor.getValue().getRouteId());
        assertEquals(301L, productCaptor.getValue().getItemId());
        assertEquals(5, productCaptor.getValue().getQuantity());
        assertEquals(new BigDecimal("2.50"), productCaptor.getValue().getProductionTime());
        assertEquals("HOUR", productCaptor.getValue().getTimeUnitType());
        assertEquals("new product", productCaptor.getValue().getRemark());
    }

    @Test
    void createRouteProduct_shouldWriteDraftCandidateProductSnapshotWithoutInsertingActiveBinding() {
        MesProRouteProductSaveReqVO reqVO = new MesProRouteProductSaveReqVO();
        reqVO.setRouteId(200L);
        reqVO.setRouteVersionId(2002L);
        reqVO.setItemId(301L);
        reqVO.setQuantity(5);
        reqVO.setProductionTime(new BigDecimal("2.50"));
        reqVO.setTimeUnitType("HOUR");
        reqVO.setRemark("new product");
        when(routeVersionMapper.selectById(2002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(200L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .build());

        Long result = routeProductService.createRouteProduct(reqVO);

        assertEquals(2002L, result);
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(2002L), eq("products"),
                argThat(snapshot -> snapshot.toString().contains("301")));
        verify(routeService, never()).validateRouteNotEnable(200L);
        verify(routeProductMapper, never()).insert(any(MesProRouteProductDO.class));
    }

    @Test
    void updateRouteProduct_shouldUpdateRouteProductWhenRouteEditableAndItemUnique() {
        MesProRouteProductSaveReqVO reqVO = new MesProRouteProductSaveReqVO();
        reqVO.setId(100L);
        reqVO.setRouteId(200L);
        reqVO.setItemId(301L);
        reqVO.setQuantity(8);
        reqVO.setProductionTime(new BigDecimal("3.00"));
        reqVO.setTimeUnitType("MINUTE");
        reqVO.setRemark("updated product");
        when(routeProductMapper.selectById(100L)).thenReturn(MesProRouteProductDO.builder()
                .id(100L)
                .routeId(200L)
                .itemId(300L)
                .build());
        when(routeProductMapper.selectByItemId(301L)).thenReturn(null);

        routeProductService.updateRouteProduct(reqVO);

        verify(routeService).validateRouteNotEnable(200L);
        ArgumentCaptor<MesProRouteProductDO> productCaptor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper).updateById(productCaptor.capture());
        assertEquals(100L, productCaptor.getValue().getId());
        assertEquals(200L, productCaptor.getValue().getRouteId());
        assertEquals(301L, productCaptor.getValue().getItemId());
        assertEquals(8, productCaptor.getValue().getQuantity());
        assertEquals(new BigDecimal("3.00"), productCaptor.getValue().getProductionTime());
        assertEquals("MINUTE", productCaptor.getValue().getTimeUnitType());
        assertEquals("updated product", productCaptor.getValue().getRemark());
    }

    @Test
    void copyRouteProduct_shouldCloneProductAndBomBindingsToTargetProduct() {
        Long sourceRouteProductId = 100L;
        Long routeId = 200L;
        Long sourceProductId = 300L;
        Long targetProductId = 301L;
        MesProRouteProductDO sourceProduct = MesProRouteProductDO.builder()
                .id(sourceRouteProductId)
                .routeId(routeId)
                .itemId(sourceProductId)
                .quantity(5)
                .productionTime(new BigDecimal("2.50"))
                .timeUnitType("HOUR")
                .remark("source product")
                .build();
        MesProRouteProductBomDO sourceBom = MesProRouteProductBomDO.builder()
                .id(400L)
                .routeId(routeId)
                .processId(500L)
                .productId(sourceProductId)
                .itemId(600L)
                .quantity(new BigDecimal("1.25"))
                .remark("source bom")
                .build();
        MesProRouteProductCopyReqVO reqVO = new MesProRouteProductCopyReqVO();
        reqVO.setSourceRouteProductId(sourceRouteProductId);
        reqVO.setTargetItemId(targetProductId);
        reqVO.setRemark("copied product");

        when(routeProductMapper.selectById(sourceRouteProductId)).thenReturn(sourceProduct);
        when(routeProductMapper.selectByItemId(targetProductId)).thenReturn(null);
        when(routeProductBomMapper.selectListByRouteIdAndProductId(routeId, sourceProductId))
                .thenReturn(List.of(sourceBom));
        doAnswer(invocation -> {
            MesProRouteProductDO data = invocation.getArgument(0);
            data.setId(101L);
            return 1;
        }).when(routeProductMapper).insert(any(MesProRouteProductDO.class));

        Long copiedId = routeProductService.copyRouteProduct(reqVO);

        assertEquals(101L, copiedId);
        verify(routeService).validateRouteNotEnable(routeId);
        ArgumentCaptor<MesProRouteProductDO> productCaptor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper).insert(productCaptor.capture());
        MesProRouteProductDO copiedProduct = productCaptor.getValue();
        assertNotEquals(sourceRouteProductId, copiedProduct.getId());
        assertEquals(routeId, copiedProduct.getRouteId());
        assertEquals(targetProductId, copiedProduct.getItemId());
        assertEquals(5, copiedProduct.getQuantity());
        assertEquals(new BigDecimal("2.50"), copiedProduct.getProductionTime());
        assertEquals("HOUR", copiedProduct.getTimeUnitType());
        assertEquals("copied product", copiedProduct.getRemark());

        ArgumentCaptor<MesProRouteProductBomDO> bomCaptor = ArgumentCaptor.forClass(MesProRouteProductBomDO.class);
        verify(routeProductBomMapper).insert(bomCaptor.capture());
        MesProRouteProductBomDO copiedBom = bomCaptor.getValue();
        assertNotEquals(400L, copiedBom.getId());
        assertEquals(routeId, copiedBom.getRouteId());
        assertEquals(500L, copiedBom.getProcessId());
        assertEquals(targetProductId, copiedBom.getProductId());
        assertEquals(600L, copiedBom.getItemId());
        assertEquals(new BigDecimal("1.25"), copiedBom.getQuantity());
        assertEquals("source bom", copiedBom.getRemark());
    }

    @Test
    void copyRouteProduct_shouldRejectMissingSourceProduct() {
        MesProRouteProductCopyReqVO reqVO = new MesProRouteProductCopyReqVO();
        reqVO.setSourceRouteProductId(100L);
        reqVO.setTargetItemId(301L);
        when(routeProductMapper.selectById(100L)).thenReturn(null);

        AssertUtils.assertServiceException(
                () -> routeProductService.copyRouteProduct(reqVO),
                ErrorCodeConstants.PRO_ROUTE_PRODUCT_NOT_EXISTS
        );

        verify(routeProductMapper, never()).insert(any(MesProRouteProductDO.class));
        verify(routeProductBomMapper, never()).insert(any(MesProRouteProductBomDO.class));
    }

    @Test
    void copyRouteProduct_shouldRejectDuplicatedTargetProduct() {
        Long sourceRouteProductId = 100L;
        Long targetProductId = 301L;
        MesProRouteProductCopyReqVO reqVO = new MesProRouteProductCopyReqVO();
        reqVO.setSourceRouteProductId(sourceRouteProductId);
        reqVO.setTargetItemId(targetProductId);
        when(routeProductMapper.selectById(sourceRouteProductId)).thenReturn(MesProRouteProductDO.builder()
                .id(sourceRouteProductId)
                .routeId(200L)
                .itemId(300L)
                .build());
        when(routeProductMapper.selectByItemId(targetProductId)).thenReturn(MesProRouteProductDO.builder()
                .id(102L)
                .routeId(201L)
                .itemId(targetProductId)
                .build());

        AssertUtils.assertServiceException(
                () -> routeProductService.copyRouteProduct(reqVO),
                ErrorCodeConstants.PRO_ROUTE_PRODUCT_ITEM_DUPLICATE
        );

        verify(routeProductMapper, never()).insert(any(MesProRouteProductDO.class));
        verify(routeProductBomMapper, never()).insert(any(MesProRouteProductBomDO.class));
    }

    @Test
    void copyRouteProduct_shouldRejectEnabledRoute() {
        Long sourceRouteProductId = 100L;
        MesProRouteProductCopyReqVO reqVO = new MesProRouteProductCopyReqVO();
        reqVO.setSourceRouteProductId(sourceRouteProductId);
        reqVO.setTargetItemId(301L);
        when(routeProductMapper.selectById(sourceRouteProductId)).thenReturn(MesProRouteProductDO.builder()
                .id(sourceRouteProductId)
                .routeId(200L)
                .itemId(300L)
                .build());
        org.mockito.Mockito.doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                ErrorCodeConstants.PRO_ROUTE_IS_ENABLE
        )).when(routeService).validateRouteNotEnable(200L);

        AssertUtils.assertServiceException(
                () -> routeProductService.copyRouteProduct(reqVO),
                ErrorCodeConstants.PRO_ROUTE_IS_ENABLE
        );

        verify(routeProductMapper, never()).insert(any(MesProRouteProductDO.class));
        verify(routeProductBomMapper, never()).insert(any(MesProRouteProductBomDO.class));
    }
}
