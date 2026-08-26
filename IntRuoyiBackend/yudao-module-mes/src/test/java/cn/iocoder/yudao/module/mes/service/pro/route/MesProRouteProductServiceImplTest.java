package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductCopyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private MesProRouteMapper routeMapper;
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
    void createRouteProduct_shouldConvertLegacyProductNameSnapshotToFormalProductIdSnapshot() {
        MesProRouteProductSaveReqVO reqVO = new MesProRouteProductSaveReqVO();
        reqVO.setRouteId(980098L);
        reqVO.setRouteVersionId(636L);
        reqVO.setItemId(912954L);
        when(routeVersionMapper.selectById(636L)).thenReturn(MesProRouteVersionDO.builder()
                .id(636L)
                .routeId(980098L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 980098,
                          "configSnapshots": {
                              "products": [
                                {
                                  "routeId": 980098,
                                  "itemId": 900001,
                                  "remark": "draft-value"
                                },
                                "按压式球囊扩张压力泵"
                              ]
                          }
                        }
                        """)
                .build());
        when(routeProductMapper.selectListByRouteId(980098L)).thenReturn(List.of(
                MesProRouteProductDO.builder()
                        .routeId(980098L)
                        .itemId(900001L)
                        .quantity(1)
                        .productionTime(BigDecimal.ONE)
                        .timeUnitType("MINUTE")
                          .remark("formal-value")
                        .build()));

        Long result = routeProductService.createRouteProduct(reqVO);

        assertEquals(636L, result);
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(636L), eq("products"),
                argThat(snapshot -> {
                    if (!(snapshot instanceof Map<?, ?> products)) {
                        return false;
                    }
                    Object existing = products.get("900001");
                    Object created = products.get("912954");
                    return existing instanceof Map<?, ?> existingProduct
                              && created instanceof Map<?, ?> createdProduct
                              && existingProduct.get("itemId") instanceof Number existingItemId
                              && existingItemId.longValue() == 900001L
                              && "draft-value".equals(existingProduct.get("remark"))
                              && createdProduct.get("itemId") instanceof Number createdItemId
                              && createdItemId.longValue() == 912954L
                            && products.values().stream().noneMatch("按压式球囊扩张压力泵"::equals);
                }));
        verify(routeService, never()).validateRouteNotEnable(980098L);
        verify(routeProductMapper, never()).insert(any(MesProRouteProductDO.class));
    }

    @Test
    void createRouteProduct_shouldNotRestoreFormalProductsAfterDraftSnapshotWasExplicitlyCleared() {
        MesProRouteProductSaveReqVO reqVO = new MesProRouteProductSaveReqVO();
        reqVO.setRouteId(200L);
        reqVO.setRouteVersionId(2002L);
        reqVO.setItemId(302L);
        when(routeVersionMapper.selectById(2002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(200L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {"configSnapshots": {"products": {}}}
                        """)
                .build());

        routeProductService.createRouteProduct(reqVO);

        verify(routeCandidateConfigService).saveConfigSnapshot(eq(2002L), eq("products"),
                argThat(snapshot -> snapshot instanceof Map<?, ?> products
                        && products.size() == 1 && products.containsKey("302")));
        verify(routeProductMapper, never()).selectListByRouteId(200L);
    }

    @Test
    void getRouteProductListByRouteId_shouldReadDraftCandidateSnapshotInsteadOfActiveBindings() {
        when(routeVersionMapper.selectById(2002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(200L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "configSnapshots": {
                            "products": {
                              "301": {"routeId": 200, "itemId": 301, "remark": "active-derived"},
                              "302": {"routeId": 200, "itemId": 302, "remark": "candidate-only"}
                            }
                          }
                        }
                        """)
                .build());
        when(routeProductMapper.selectListByRouteId(200L)).thenReturn(List.of(
                MesProRouteProductDO.builder().id(101L).routeId(200L).itemId(301L).build()));

        List<MesProRouteProductDO> result = routeProductService.getRouteProductListByRouteId(200L, 2002L);

        assertEquals(2, result.size());
        assertEquals(List.of(301L, 302L), result.stream().map(MesProRouteProductDO::getItemId).toList());
        assertEquals(101L, result.get(0).getId());
        assertNull(result.get(1).getId());
        assertEquals("candidate-only", result.get(1).getRemark());
    }

    @Test
    void getRouteProductListByRouteId_shouldKeepExplicitlyEmptyDraftSnapshotEmpty() {
        when(routeVersionMapper.selectById(2002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(200L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {"configSnapshots": {"products": {}}}
                        """)
                .build());

        List<MesProRouteProductDO> result = routeProductService.getRouteProductListByRouteId(200L, 2002L);

        assertEquals(List.of(), result);
        verify(routeProductMapper, never()).selectListByRouteId(200L);
    }

    @Test
    void deleteCandidateRouteProduct_shouldRemoveCandidateOnlyItemByFormalItemIdentity() {
        when(routeVersionMapper.selectById(2002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(200L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "configSnapshots": {
                            "products": {
                              "301": {"routeId": 200, "itemId": 301},
                              "302": {"routeId": 200, "itemId": 302}
                            },
                            "productBoms": {}
                          }
                        }
                        """)
                .build());

        routeProductService.deleteCandidateRouteProduct(200L, 302L, 2002L);

        verify(routeCandidateConfigService).saveConfigSnapshot(eq(2002L), eq("products"),
                argThat(snapshot -> snapshot instanceof Map<?, ?> products
                        && products.containsKey("301") && !products.containsKey("302")));
        verify(routeProductMapper, never()).selectById(any());
    }

    @Test
    void copyCandidateRouteProduct_shouldCopyCandidateOnlySourceByFormalItemIdentity() {
        when(routeVersionMapper.selectById(2002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(200L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "configSnapshots": {
                            "products": {
                              "302": {
                                "routeId": 200,
                                "itemId": 302,
                                "quantity": 7,
                                "productionTime": 2.5,
                                "timeUnitType": "HOUR",
                                "remark": "candidate-only"
                              }
                            },
                            "productBoms": {}
                          }
                        }
                        """)
                .build());

        Long result = routeProductService.copyCandidateRouteProduct(200L, 2002L, 302L, 303L);

        assertEquals(2002L, result);
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(2002L), eq("products"),
                argThat(snapshot -> snapshot instanceof Map<?, ?> products
                        && products.get("303") instanceof Map<?, ?> copied
                        && copied.get("itemId") instanceof Number itemId
                        && itemId.longValue() == 303L
                        && "candidate-only".equals(copied.get("remark"))));
        verify(routeProductMapper, never()).selectById(any());
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
    void saveRouteProductByItem_shouldCreateBindingForEnabledRouteWhenProductHasNoRoute() {
        when(routeProductMapper.selectByItemId(301L)).thenReturn(null);
        doAnswer(invocation -> {
            MesProRouteProductDO data = invocation.getArgument(0);
            data.setId(101L);
            return 1;
        }).when(routeProductMapper).insert(any(MesProRouteProductDO.class));

        Long result = routeProductService.saveRouteProductByItem(301L, 200L);

        assertEquals(101L, result);
        verify(routeService, never()).validateRouteNotEnable(any());
        ArgumentCaptor<MesProRouteProductDO> productCaptor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper).insert(productCaptor.capture());
        MesProRouteProductDO inserted = productCaptor.getValue();
        assertEquals(200L, inserted.getRouteId());
        assertEquals(301L, inserted.getItemId());
        assertEquals(1, inserted.getQuantity());
        assertEquals(BigDecimal.ONE, inserted.getProductionTime());
        assertEquals("MINUTE", inserted.getTimeUnitType());
    }

    @Test
    void saveRouteProductByItem_shouldMoveExistingBindingAndPreserveRouteSideParameters() {
        MesProRouteProductDO existing = MesProRouteProductDO.builder()
                .id(100L)
                .routeId(199L)
                .itemId(301L)
                .quantity(8)
                .productionTime(new BigDecimal("3.00"))
                .timeUnitType("HOUR")
                .remark("keep route side parameters")
                .build();
        when(routeProductMapper.selectByItemId(301L)).thenReturn(existing);

        Long result = routeProductService.saveRouteProductByItem(301L, 200L);

        assertEquals(100L, result);
        verify(routeService).validateRouteNotEnable(199L);
        verify(routeService).validateRouteNotEnable(200L);
        verify(routeProductBomService).deleteRouteProductBomByRouteIdAndProductId(199L, 301L);
        ArgumentCaptor<MesProRouteProductDO> productCaptor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper).updateById(productCaptor.capture());
        MesProRouteProductDO updated = productCaptor.getValue();
        assertEquals(100L, updated.getId());
        assertEquals(200L, updated.getRouteId());
        assertEquals(301L, updated.getItemId());
        assertEquals(8, updated.getQuantity());
        assertEquals(new BigDecimal("3.00"), updated.getProductionTime());
        assertEquals("HOUR", updated.getTimeUnitType());
        assertEquals("keep route side parameters", updated.getRemark());
    }

    @Test
    void saveQaRegulationRouteProductByItem_shouldCreateBindingForPublishedRouteWithoutEditableGuard() {
        when(routeMapper.selectById(200L)).thenReturn(MesProRouteDO.builder().id(200L).build());
        when(routeVersionMapper.selectActiveByRouteId(200L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2001L)
                .routeId(200L)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionMapper.STATUS_ACTIVE)
                .build());
        when(routeProductMapper.selectByItemId(301L)).thenReturn(null);
        doAnswer(invocation -> {
            MesProRouteProductDO data = invocation.getArgument(0);
            data.setId(101L);
            return 1;
        }).when(routeProductMapper).insert(any(MesProRouteProductDO.class));

        Long result = routeProductService.saveQaRegulationRouteProductByItem(301L, 200L);

        assertEquals(101L, result);
        verify(routeService, never()).validateRouteNotEnable(any());
        ArgumentCaptor<MesProRouteProductDO> productCaptor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper).insert(productCaptor.capture());
        MesProRouteProductDO inserted = productCaptor.getValue();
        assertEquals(200L, inserted.getRouteId());
        assertEquals(301L, inserted.getItemId());
        assertEquals(1, inserted.getQuantity());
        assertEquals(BigDecimal.ONE, inserted.getProductionTime());
        assertEquals("MINUTE", inserted.getTimeUnitType());
        assertEquals("QA 规程手动绑定", inserted.getRemark());
    }

    @Test
    void saveQaRegulationRouteProductByItem_shouldMoveExistingBindingWithoutEditableGuard() {
        MesProRouteProductDO existing = MesProRouteProductDO.builder()
                .id(100L)
                .routeId(199L)
                .itemId(301L)
                .quantity(8)
                .productionTime(new BigDecimal("3.00"))
                .timeUnitType("HOUR")
                .remark("keep route side parameters")
                .build();
        when(routeMapper.selectById(200L)).thenReturn(MesProRouteDO.builder().id(200L).build());
        when(routeVersionMapper.selectActiveByRouteId(200L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2001L)
                .routeId(200L)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionMapper.STATUS_ACTIVE)
                .build());
        when(routeProductMapper.selectByItemId(301L)).thenReturn(existing);

        Long result = routeProductService.saveQaRegulationRouteProductByItem(301L, 200L);

        assertEquals(100L, result);
        verify(routeService, never()).validateRouteNotEnable(any());
        verify(routeProductBomService).deleteRouteProductBomByRouteIdAndProductId(199L, 301L);
        ArgumentCaptor<MesProRouteProductDO> productCaptor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper).updateById(productCaptor.capture());
        MesProRouteProductDO updated = productCaptor.getValue();
        assertEquals(100L, updated.getId());
        assertEquals(200L, updated.getRouteId());
        assertEquals(301L, updated.getItemId());
        assertEquals(8, updated.getQuantity());
        assertEquals(new BigDecimal("3.00"), updated.getProductionTime());
        assertEquals("HOUR", updated.getTimeUnitType());
        assertEquals("keep route side parameters", updated.getRemark());
    }

    @Test
    void saveQaRegulationRouteProductByItem_shouldFailWhenRouteHasNoActiveVersion() {
        when(routeMapper.selectById(200L)).thenReturn(MesProRouteDO.builder().id(200L).build());
        when(routeVersionMapper.selectActiveByRouteId(200L)).thenReturn(null);

        AssertUtils.assertServiceException(
                () -> routeProductService.saveQaRegulationRouteProductByItem(301L, 200L),
                ErrorCodeConstants.PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS,
                200L
        );

        verify(routeProductMapper, never()).insert(any(MesProRouteProductDO.class));
        verify(routeProductMapper, never()).updateById(any(MesProRouteProductDO.class));
        verify(routeService, never()).validateRouteNotEnable(any());
    }

    @Test
    void saveRouteProductByItem_shouldDeleteExistingBindingWhenRouteCleared() {
        MesProRouteProductDO existing = MesProRouteProductDO.builder()
                .id(100L)
                .routeId(199L)
                .itemId(301L)
                .build();
        when(routeProductMapper.selectByItemId(301L)).thenReturn(existing);

        Long result = routeProductService.saveRouteProductByItem(301L, null);

        assertNull(result);
        verify(routeService).validateRouteNotEnable(199L);
        verify(routeProductBomService).deleteRouteProductBomByRouteIdAndProductId(199L, 301L);
        verify(routeProductMapper).deleteById(100L);
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
