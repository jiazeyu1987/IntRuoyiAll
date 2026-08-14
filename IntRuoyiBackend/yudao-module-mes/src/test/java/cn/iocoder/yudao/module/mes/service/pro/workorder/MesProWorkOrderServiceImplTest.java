package cn.iocoder.yudao.module.mes.service.pro.workorder;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderTemporaryFreezeStatusRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskDependencyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemBatchConfigService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationOrderChangeService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProWorkOrderServiceImplTest {

    @InjectMocks
    private MesProWorkOrderServiceImpl workOrderService;

    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProWorkOrderBomService workOrderBomService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesMdItemBatchConfigService itemBatchConfigService;
    @Mock
    private MesWmBarcodeService barcodeService;
    @Mock
    private MesProTaskService taskService;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Mock
    private MesProTaskMapper taskMapper;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProTaskDependencyMapper taskDependencyMapper;
    @Mock
    private MesProScheduleIssueMapper scheduleIssueMapper;
    @Mock
    private MesReportAllocationOrderChangeService reportAllocationOrderChangeService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void updateTemporaryFreeze_shouldFreezeNonWhitelistWorkOrdersAndClearOpenTasks() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(1L)
                .skipStatutoryHolidays(false)
                .weekendRestMode("DOUBLE")
                .dateShiftModeByDateJson("{}")
                .temporaryFreezeEnabled(false)
                .build();
        MesProWorkOrderDO keepOpen = MesProWorkOrderDO.builder()
                .id(100L)
                .productId(10L)
                .temporaryFrozen(true)
                .build();
        MesProWorkOrderDO shouldFreeze = MesProWorkOrderDO.builder()
                .id(200L)
                .productId(20L)
                .temporaryFrozen(false)
                .build();
        MesProTaskDO openTask = MesProTaskDO.builder()
                .id(300L)
                .workOrderId(200L)
                .status(MesProTaskStatusEnum.PREPARE.getStatus())
                .quantity(new BigDecimal("5"))
                .build();
        MesProTaskDO finishedTask = MesProTaskDO.builder()
                .id(301L)
                .workOrderId(200L)
                .status(MesProTaskStatusEnum.FINISHED.getStatus())
                .quantity(new BigDecimal("5"))
                .build();

        when(scheduleCalendarRuleMapper.selectByTenantId(any())).thenReturn(rule);
        when(workOrderMapper.selectList()).thenReturn(List.of(keepOpen, shouldFreeze));
        when(routeMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus()))
                .thenReturn(List.of(MesProRouteDO.builder().id(1L).build()));
        when(routeProductMapper.selectListByRouteIds(List.of(1L)))
                .thenReturn(List.of(MesProRouteProductDO.builder().id(11L).routeId(1L).itemId(10L).build()));
        when(taskMapper.selectListByWorkOrderIds(List.of(200L))).thenReturn(List.of(openTask, finishedTask));

        MesProWorkOrderTemporaryFreezeStatusRespVO response = workOrderService.updateTemporaryFreeze(true);

        assertTrue(response.getEnabled());
        assertEquals(2, response.getTotalWorkOrderCount());
        assertEquals(1, response.getFrozenWorkOrderCount());
        assertEquals(1, response.getUnfrozenWorkOrderCount());
        assertEquals(1, response.getClearedTaskCount());
        verify(workOrderMapper).updateTemporaryFrozenByIds(List.of(100L), false);
        verify(workOrderMapper).updateTemporaryFrozenByIds(List.of(200L), true);
        verify(taskDependencyMapper).deleteByTaskIds(List.of(300L));
        verify(taskScheduleExtMapper).deleteByTaskIds(List.of(300L));
        verify(scheduleIssueMapper).deleteByTaskIds(List.of(300L));
        verify(scheduleIssueMapper).deleteByWorkOrderIds(List.of(200L));
        verify(taskMapper).deleteById(300L);
        verify(taskMapper, never()).deleteById(301L);
        verify(workOrderMapper).updateQuantityScheduled(200L, BigDecimal.ZERO);
        ArgumentCaptor<MesProScheduleCalendarRuleDO> updateCaptor =
                ArgumentCaptor.forClass(MesProScheduleCalendarRuleDO.class);
        verify(scheduleCalendarRuleMapper).updateById(updateCaptor.capture());
        assertTrue(updateCaptor.getValue().getTemporaryFreezeEnabled());
    }

    @Test
    void updateTemporaryFreeze_shouldFreezeAllWhenNoEnabledRouteExists() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(2L)
                .skipStatutoryHolidays(false)
                .weekendRestMode("DOUBLE")
                .dateShiftModeByDateJson("{}")
                .temporaryFreezeEnabled(false)
                .build();
        MesProWorkOrderDO first = MesProWorkOrderDO.builder().id(1L).productId(10L).temporaryFrozen(false).build();
        MesProWorkOrderDO second = MesProWorkOrderDO.builder().id(2L).productId(20L).temporaryFrozen(false).build();
        MesProWorkOrderDO firstFrozen = MesProWorkOrderDO.builder().id(1L).productId(10L).temporaryFrozen(true).build();
        MesProWorkOrderDO secondFrozen = MesProWorkOrderDO.builder().id(2L).productId(20L).temporaryFrozen(true).build();

        when(scheduleCalendarRuleMapper.selectByTenantId(any())).thenReturn(rule);
        when(workOrderMapper.selectList()).thenReturn(List.of(first, second), List.of(firstFrozen, secondFrozen));
        when(routeMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of());
        when(taskMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(List.of());

        MesProWorkOrderTemporaryFreezeStatusRespVO response = workOrderService.updateTemporaryFreeze(true);

        assertTrue(response.getEnabled());
        assertEquals(2, response.getFrozenWorkOrderCount());
        verify(workOrderMapper).updateTemporaryFrozenByIds(List.of(1L, 2L), true);
    }

    @Test
    void updateTemporaryFreeze_shouldDisableAndUnfreezeAllWorkOrders() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(3L)
                .skipStatutoryHolidays(false)
                .weekendRestMode("DOUBLE")
                .dateShiftModeByDateJson("{}")
                .temporaryFreezeEnabled(true)
                .build();
        MesProWorkOrderDO first = MesProWorkOrderDO.builder().id(1L).productId(10L).temporaryFrozen(true).build();
        MesProWorkOrderDO second = MesProWorkOrderDO.builder().id(2L).productId(20L).temporaryFrozen(true).build();
        MesProWorkOrderDO firstUnfrozen = MesProWorkOrderDO.builder().id(1L).productId(10L).temporaryFrozen(false).build();
        MesProWorkOrderDO secondUnfrozen = MesProWorkOrderDO.builder().id(2L).productId(20L).temporaryFrozen(false).build();

        when(scheduleCalendarRuleMapper.selectByTenantId(any())).thenReturn(rule);
        when(workOrderMapper.selectList()).thenReturn(List.of(firstUnfrozen, secondUnfrozen));

        MesProWorkOrderTemporaryFreezeStatusRespVO response = workOrderService.updateTemporaryFreeze(false);

        assertFalse(response.getEnabled());
        assertEquals(0, response.getFrozenWorkOrderCount());
        assertEquals(2, response.getUnfrozenWorkOrderCount());
        verify(workOrderMapper).updateTemporaryFrozenAll(false);
        verify(taskMapper, never()).selectListByWorkOrderIds(anyCollection());
    }

    @Test
    void updateWorkOrderTemporaryFrozen_shouldFreezeOneWorkOrderAndClearOpenTasks() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(200L)
                .productId(20L)
                .temporaryFrozen(false)
                .build();
        MesProTaskDO openTask = MesProTaskDO.builder()
                .id(300L)
                .workOrderId(200L)
                .status(MesProTaskStatusEnum.PREPARE.getStatus())
                .quantity(new BigDecimal("5"))
                .build();
        MesProTaskDO finishedTask = MesProTaskDO.builder()
                .id(301L)
                .workOrderId(200L)
                .status(MesProTaskStatusEnum.FINISHED.getStatus())
                .quantity(new BigDecimal("5"))
                .build();
        when(workOrderMapper.selectById(200L)).thenReturn(workOrder);
        when(taskMapper.selectListByWorkOrderIds(List.of(200L))).thenReturn(List.of(openTask, finishedTask));

        workOrderService.updateWorkOrderTemporaryFrozen(200L, true);

        verify(reportAllocationOrderChangeService)
                .invalidateWorkOrder(200L, null, "工单冻结暂停");
        verify(workOrderMapper).updateTemporaryFrozenByIds(List.of(200L), true);
        verify(taskDependencyMapper).deleteByTaskIds(List.of(300L));
        verify(taskScheduleExtMapper).deleteByTaskIds(List.of(300L));
        verify(scheduleIssueMapper).deleteByTaskIds(List.of(300L));
        verify(scheduleIssueMapper).deleteByWorkOrderIds(List.of(200L));
        verify(taskMapper).deleteById(300L);
        verify(taskMapper, never()).deleteById(301L);
        verify(workOrderMapper).updateQuantityScheduled(200L, BigDecimal.ZERO);
    }

    @Test
    void cancelWorkOrder_shouldReturnUnreleasedAllocationsBeforeStatusChange() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder().id(202L)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()).build();
        when(workOrderMapper.selectById(202L)).thenReturn(workOrder);

        workOrderService.cancelWorkOrder(202L);

        verify(reportAllocationOrderChangeService)
                .invalidateWorkOrder(eq(202L), isNull(), eq("工单取消"));
        verify(taskService).cancelTaskByOrderId(202L);
        verify(workOrderMapper).updateById(org.mockito.ArgumentMatchers.argThat((MesProWorkOrderDO row) ->
                row.getId().equals(202L)
                        && row.getStatus().equals(MesProWorkOrderStatusEnum.CANCELED.getStatus())));
    }

    @Test
    void updateWorkOrder_shouldReturnOnlyQuantityExcessBeforeDecrease() {
        MesProWorkOrderDO old = MesProWorkOrderDO.builder().id(203L).code("WO-203").productId(20L)
                .quantity(new BigDecimal("100")).status(MesProWorkOrderStatusEnum.PREPARE.getStatus()).build();
        MesProWorkOrderSaveReqVO request = new MesProWorkOrderSaveReqVO();
        request.setId(203L);
        request.setCode("WO-203");
        request.setProductId(20L);
        request.setQuantity(new BigDecimal("60"));
        when(workOrderMapper.selectById(203L)).thenReturn(old);
        when(workOrderMapper.selectByCode("WO-203")).thenReturn(old);

        workOrderService.updateWorkOrder(request);

        verify(reportAllocationOrderChangeService).reduceWorkOrderAllocations(
                eq(203L), eq(new BigDecimal("60")), isNull(), eq("工单数量减少"));
        verify(workOrderBomService).generateWorkOrderBom(203L, request, true);
        verify(workOrderMapper).updateById(org.mockito.ArgumentMatchers.any(MesProWorkOrderDO.class));
    }

    @Test
    void updateWorkOrderTemporaryFrozen_shouldUnfreezeOneWorkOrderWithoutClearingTasks() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(201L)
                .productId(21L)
                .temporaryFrozen(true)
                .build();
        when(workOrderMapper.selectById(201L)).thenReturn(workOrder);

        workOrderService.updateWorkOrderTemporaryFrozen(201L, false);

        verify(workOrderMapper).updateTemporaryFrozenByIds(List.of(201L), false);
        verify(taskMapper, never()).selectListByWorkOrderIds(anyCollection());
    }

    @Test
    void updateWorkOrderTemporaryFrozen_shouldFailFastWhenWorkOrderMissing() {
        when(workOrderMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> workOrderService.updateWorkOrderTemporaryFrozen(999L, true));
    }

    @Test
    void getWorkOrderPage_shouldExpandSelectedProductToProductsContainingSameName() {
        MesProWorkOrderPageReqVO reqVO = new MesProWorkOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setProductId(1001L);
        PageResult<MesProWorkOrderDO> expected = new PageResult<>(List.of(), 0L);
        when(itemMapper.selectById(1001L)).thenReturn(MesMdItemDO.builder()
                .id(1001L)
                .name("球囊扩张压力泵")
                .build());
        when(itemMapper.selectListByNameLike("球囊扩张压力泵")).thenReturn(List.of(
                MesMdItemDO.builder().id(1001L).name("球囊扩张压力泵").build(),
                MesMdItemDO.builder().id(1002L).name("数显球囊扩张压力泵").build(),
                MesMdItemDO.builder().id(1003L).name("按压式球囊扩张压力泵").build()
        ));
        when(workOrderMapper.selectPageByProductIds(reqVO, List.of(1001L, 1002L, 1003L))).thenReturn(expected);

        PageResult<MesProWorkOrderDO> result = workOrderService.getWorkOrderPage(reqVO);

        assertEquals(expected, result);
        verify(workOrderMapper).selectPageByProductIds(reqVO, List.of(1001L, 1002L, 1003L));
    }

    @Test
    void getWorkOrderPage_shouldUseProductNameAndCodeCandidateFilterIds() {
        MesProWorkOrderPageReqVO reqVO = new MesProWorkOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setProductNameFilterId(2001L);
        reqVO.setProductCodeFilterId(2001L);
        PageResult<MesProWorkOrderDO> expected = new PageResult<>(List.of(), 0L);
        when(workOrderMapper.selectPageByProductIds(reqVO, List.of(2001L))).thenReturn(expected);

        PageResult<MesProWorkOrderDO> result = workOrderService.getWorkOrderPage(reqVO);

        assertEquals(expected, result);
        verify(workOrderMapper).selectPageByProductIds(reqVO, List.of(2001L));
    }

    @Test
    void getWorkOrderPage_shouldUseProductNameKeywordWhenCandidateNotSelected() {
        MesProWorkOrderPageReqVO reqVO = new MesProWorkOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setProductNameKeyword("球囊扩张压力泵");
        PageResult<MesProWorkOrderDO> expected = new PageResult<>(List.of(), 0L);
        when(itemMapper.selectListByNameLike("球囊扩张压力泵")).thenReturn(List.of(
                MesMdItemDO.builder().id(3001L).name("球囊扩张压力泵").build(),
                MesMdItemDO.builder().id(3002L).name("数显球囊扩张压力泵").build()
        ));
        when(workOrderMapper.selectPageByProductIds(reqVO, List.of(3001L, 3002L))).thenReturn(expected);

        PageResult<MesProWorkOrderDO> result = workOrderService.getWorkOrderPage(reqVO);

        assertEquals(expected, result);
        verify(workOrderMapper).selectPageByProductIds(reqVO, List.of(3001L, 3002L));
    }

    @Test
    void getWorkOrderPage_shouldReturnEmptyWhenProductCandidateFiltersConflict() {
        MesProWorkOrderPageReqVO reqVO = new MesProWorkOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setProductNameFilterId(2001L);
        reqVO.setProductCodeFilterId(2002L);
        PageResult<MesProWorkOrderDO> expected = new PageResult<>(List.of(), 0L);
        when(workOrderMapper.selectPageByProductIds(reqVO, List.of(-1L))).thenReturn(expected);

        PageResult<MesProWorkOrderDO> result = workOrderService.getWorkOrderPage(reqVO);

        assertEquals(expected, result);
        verify(workOrderMapper).selectPageByProductIds(reqVO, List.of(-1L));
    }
}
