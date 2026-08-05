package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.MesProRouteProductController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductBindFromWorkOrdersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductBindFromWorkOrdersRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductByItemSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProductService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProductServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteProductBindFromWorkOrdersTest {

    @InjectMocks
    private MesProRouteProductServiceImpl routeProductService;

    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProRouteService routeService;

    @Mock
    private MesProRouteProductService controllerRouteProductService;
    @InjectMocks
    private MesProRouteProductController controller;

    @Test
    void previewBindFromWorkOrders_shouldCountCreatableExistingAndConflictProductsWithoutInsert() {
        Long routeId = 100L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).name("球囊扩张压力泵").build();
        MesMdItemDO itemA = MesMdItemDO.builder().id(10L).code("AW.107.02.01.2036").name("球囊扩张压力泵").build();
        MesMdItemDO itemB = MesMdItemDO.builder().id(11L).code("AW.107.02.01.1009").name("球囊扩张压力泵").build();
        MesMdItemDO itemExisting = MesMdItemDO.builder().id(12L).code("AW.107.02.01.2010").name("球囊扩张压力泵").build();
        MesMdItemDO itemConflict = MesMdItemDO.builder().id(13L).code("AW.107.02.01.2011").name("球囊扩张压力泵").build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(workOrderMapper.selectListAll()).thenReturn(List.of(
                MesProWorkOrderDO.builder().id(1L).productId(10L).build(),
                MesProWorkOrderDO.builder().id(2L).productId(11L).build(),
                MesProWorkOrderDO.builder().id(3L).productId(11L).build(),
                MesProWorkOrderDO.builder().id(4L).productId(12L).build(),
                MesProWorkOrderDO.builder().id(5L).productId(13L).build()
        ));
        when(itemMapper.selectListByIds(List.of(10L, 11L, 12L, 13L)))
                .thenReturn(List.of(itemA, itemB, itemExisting, itemConflict));
        when(routeProductMapper.selectListByItemIds(argThat(ids -> ids.containsAll(List.of(10L, 11L, 12L, 13L)))))
                .thenReturn(List.of(
                MesProRouteProductDO.builder().id(200L).routeId(routeId).itemId(12L).build(),
                MesProRouteProductDO.builder().id(201L).routeId(999L).itemId(13L).build()
        ));

        MesProRouteProductBindFromWorkOrdersRespVO result =
                routeProductService.previewBindFromWorkOrders(routeId, null);

        assertEquals(routeId, result.getRouteId());
        assertEquals("球囊扩张压力泵", result.getRouteName());
        assertEquals(4, result.getMatchedCount());
        assertEquals(1, result.getExistingCount());
        assertEquals(2, result.getCreatedCount());
        assertEquals(1, result.getConflictCount());
        assertIterableEquals(List.of("AW.107.02.01.2036", "AW.107.02.01.1009", "AW.107.02.01.2010",
                        "AW.107.02.01.2011"),
                result.getItemCodes());
        assertIterableEquals(List.of("AW.107.02.01.2036", "AW.107.02.01.1009"),
                result.getCreatableItemCodes());
        assertIterableEquals(List.of("AW.107.02.01.2010"), result.getExistingItemCodes());
        assertIterableEquals(List.of("AW.107.02.01.2011"), result.getConflictItemCodes());
        verify(routeProductMapper, never()).insert(any(MesProRouteProductDO.class));
    }

    @Test
    void bindFromWorkOrders_shouldInsertMissingProductsAndSkipConflictsByExactRouteName() {
        Long routeId = 100L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).name("球囊扩张压力泵").build();
        MesMdItemDO itemA = MesMdItemDO.builder().id(10L).code("AW.107.02.01.2036").name("球囊扩张压力泵").build();
        MesMdItemDO itemB = MesMdItemDO.builder().id(11L).code("AW.107.02.01.1009").name("球囊扩张压力泵").build();
        MesMdItemDO itemExisting = MesMdItemDO.builder().id(12L).code("AW.107.02.01.2010").name("球囊扩张压力泵").build();
        MesMdItemDO itemConflict = MesMdItemDO.builder().id(13L).code("AW.107.02.01.2011").name("球囊扩张压力泵").build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(workOrderMapper.selectListAll()).thenReturn(List.of(
                MesProWorkOrderDO.builder().id(1L).productId(10L).build(),
                MesProWorkOrderDO.builder().id(2L).productId(11L).build(),
                MesProWorkOrderDO.builder().id(3L).productId(11L).build(),
                MesProWorkOrderDO.builder().id(4L).productId(12L).build(),
                MesProWorkOrderDO.builder().id(5L).productId(13L).build()
        ));
        when(itemMapper.selectListByIds(List.of(10L, 11L, 12L, 13L)))
                .thenReturn(List.of(itemA, itemB, itemExisting, itemConflict));
        when(routeProductMapper.selectListByItemIds(argThat(ids -> ids.containsAll(List.of(10L, 11L, 12L, 13L)))))
                .thenReturn(List.of(
                        MesProRouteProductDO.builder().id(200L).routeId(routeId).itemId(12L).build(),
                        MesProRouteProductDO.builder().id(201L).routeId(999L).itemId(13L).build()
                ));

        MesProRouteProductBindFromWorkOrdersRespVO result =
                routeProductService.bindFromWorkOrders(routeId, null);

        assertEquals(routeId, result.getRouteId());
        assertEquals("球囊扩张压力泵", result.getRouteName());
        assertEquals(4, result.getMatchedCount());
        assertEquals(1, result.getExistingCount());
        assertEquals(2, result.getCreatedCount());
        assertEquals(1, result.getConflictCount());
        assertIterableEquals(List.of("AW.107.02.01.2036", "AW.107.02.01.1009", "AW.107.02.01.2010",
                        "AW.107.02.01.2011"),
                result.getItemCodes());
        assertIterableEquals(List.of("AW.107.02.01.2011"), result.getConflictItemCodes());

        ArgumentCaptor<MesProRouteProductDO> captor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper, times(2)).insert(captor.capture());
        List<MesProRouteProductDO> inserted = captor.getAllValues();
        assertEquals(List.of(10L, 11L), inserted.stream().map(MesProRouteProductDO::getItemId).toList());
        for (MesProRouteProductDO row : inserted) {
            assertEquals(routeId, row.getRouteId());
            assertEquals(1, row.getQuantity());
            assertEquals(new BigDecimal("1"), row.getProductionTime());
            assertEquals("MINUTE", row.getTimeUnitType());
            assertEquals("生产订单自动补齐", row.getRemark());
        }
    }

    @Test
    void bindFromWorkOrders_shouldFailWhenNoMatchedWorkOrderProduct() {
        Long routeId = 101L;
        when(routeMapper.selectById(routeId)).thenReturn(
                MesProRouteDO.builder().id(routeId).name("无生产订单产品路线").build());
        when(workOrderMapper.selectListAll()).thenReturn(List.of(
                MesProWorkOrderDO.builder().id(20L).productId(20L).build()
        ));
        when(itemMapper.selectListByIds(List.of(20L))).thenReturn(List.of(
                MesMdItemDO.builder().id(20L).code("NO-WO").name("其它产品").build()
        ));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> routeProductService.bindFromWorkOrders(routeId, null));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_PRODUCT_WORK_ORDER_MATCH_EMPTY.getCode(), exception.getCode());
        verify(routeProductMapper, never()).insert(any(MesProRouteProductDO.class));
    }

    @Test
    void bindFromWorkOrders_shouldSkipConflictOnlyProductsWithoutInsert() {
        Long routeId = 102L;
        MesMdItemDO conflict = MesMdItemDO.builder().id(31L).code("CONFLICT-001").name("冲突路线").build();
        when(routeMapper.selectById(routeId)).thenReturn(MesProRouteDO.builder().id(routeId).name("冲突路线").build());
        when(workOrderMapper.selectListAll()).thenReturn(List.of(
                MesProWorkOrderDO.builder().id(11L).productId(31L).build()
        ));
        when(itemMapper.selectListByIds(List.of(31L))).thenReturn(List.of(conflict));
        when(routeProductMapper.selectListByItemIds(argThat(ids -> ids.containsAll(List.of(31L)))))
                .thenReturn(List.of(
                MesProRouteProductDO.builder().id(300L).routeId(999L).itemId(31L).build()
        ));

        MesProRouteProductBindFromWorkOrdersRespVO result = routeProductService.bindFromWorkOrders(routeId, null);

        assertEquals(1, result.getMatchedCount());
        assertEquals(0, result.getCreatedCount());
        assertEquals(0, result.getExistingCount());
        assertEquals(1, result.getConflictCount());
        assertIterableEquals(List.of("CONFLICT-001"), result.getConflictItemCodes());
        verify(routeProductMapper, never()).insert(any(MesProRouteProductDO.class));
    }

    @Test
    void controller_shouldExposeBindFromWorkOrdersEndpointContract() {
        MesProRouteProductBindFromWorkOrdersReqVO reqVO = new MesProRouteProductBindFromWorkOrdersReqVO();
        reqVO.setRouteId(103L);
        reqVO.setRouteVersionId(1003L);
        MesProRouteProductBindFromWorkOrdersRespVO expected =
                MesProRouteProductBindFromWorkOrdersRespVO.builder()
                        .routeId(103L)
                        .routeName("球囊扩张压力泵")
                        .matchedCount(1)
                        .createdCount(1)
                        .existingCount(0)
                        .conflictCount(0)
                        .itemCodes(List.of("AW.107.02.01.2036"))
                        .creatableItemCodes(List.of("AW.107.02.01.2036"))
                        .existingItemCodes(List.of())
                        .conflictItemCodes(List.of())
                        .build();
        when(controllerRouteProductService.bindFromWorkOrders(103L, 1003L)).thenReturn(expected);

        assertEquals(expected, controller.bindFromWorkOrders(reqVO).getData());
    }

    @Test
    void controller_shouldExposePreviewBindFromWorkOrdersEndpointContract() {
        MesProRouteProductBindFromWorkOrdersReqVO reqVO = new MesProRouteProductBindFromWorkOrdersReqVO();
        reqVO.setRouteId(104L);
        reqVO.setRouteVersionId(1004L);
        MesProRouteProductBindFromWorkOrdersRespVO expected =
                MesProRouteProductBindFromWorkOrdersRespVO.builder()
                        .routeId(104L)
                        .routeName("球囊扩张压力泵")
                        .matchedCount(2)
                        .createdCount(1)
                        .existingCount(1)
                        .conflictCount(0)
                        .itemCodes(List.of("AW.107.02.01.2036", "AW.107.02.01.2010"))
                        .creatableItemCodes(List.of("AW.107.02.01.2036"))
                        .existingItemCodes(List.of("AW.107.02.01.2010"))
                        .conflictItemCodes(List.of())
                        .build();
        when(controllerRouteProductService.previewBindFromWorkOrders(104L, 1004L)).thenReturn(expected);

        assertEquals(expected, controller.previewBindFromWorkOrders(reqVO).getData());
    }

    @Test
    void controller_shouldExposeQaRegulationRouteProductBindingEndpointContract() {
        MesProRouteProductByItemSaveReqVO reqVO = new MesProRouteProductByItemSaveReqVO();
        reqVO.setItemId(301L);
        reqVO.setRouteId(200L);
        when(controllerRouteProductService.saveQaRegulationRouteProductByItem(301L, 200L)).thenReturn(100L);

        assertEquals(100L, controller.saveQaRegulationRouteProductByItem(reqVO).getData());
    }
}
