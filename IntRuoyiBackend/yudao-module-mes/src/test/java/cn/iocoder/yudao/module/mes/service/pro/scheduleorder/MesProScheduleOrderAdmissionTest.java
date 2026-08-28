package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderCreateFromWorkOrderReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationWorkerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderRouteStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleDefaultCompatibilityPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_SCHEDULE_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_RESOURCE_CAPACITY_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProScheduleOrderAdmissionTest {

    @InjectMocks
    private MesProScheduleOrderServiceImpl scheduleOrderService;

    @Spy
    private ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy = new ScheduleDefaultCompatibilityPolicy();

    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Mock
    private MesMdWorkstationWorkerMapper workstationWorkerMapper;
    @Mock
    private MesDvMachineryMapper machineryMapper;
    @Mock
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;

    @org.junit.jupiter.api.BeforeEach
    void setUpProcessIdentity() {
        org.mockito.Mockito.lenient().when(routeProcessService.getProcessIdentityMap(
                        org.mockito.ArgumentMatchers.anyCollection()))
                .thenAnswer(invocation -> identityMap(invocation.getArgument(0)));
        org.mockito.Mockito.lenient().when(routeProcessService.resolveFrozenRouteProcess(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .build());
        org.mockito.Mockito.lenient().when(syncRecordMapper.selectByWorkOrderId(
                        org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> {
                    Long workOrderId = invocation.getArgument(0);
                    return MesKingdeeProductionOrderSyncRecordDO.builder()
                            .workOrderId(workOrderId)
                            .sourceFid("FID-" + workOrderId)
                            .sourceBillNo("ERP-MO-" + workOrderId)
                            .build();
                });
    }

    private Map<Long, Long> identityMap(java.util.Collection<Long> processIds) {
        Map<Long, Long> result = new java.util.LinkedHashMap<>();
        processIds.stream().filter(java.util.Objects::nonNull).forEach(id -> result.put(id, id));
        return result;
    }

    @Test
    void createFromWorkOrder_shouldRejectMissingRouteWithFailFast() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(100L)
                .code("ERP-MO-001")
                .productId(20L)
                .quantity(new BigDecimal("120.000000"))
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(100L);
        reqVO.setPromiseDate(LocalDate.of(2026, 6, 30));

        when(workOrderMapper.selectById(100L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(20L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_REQUIRED.getCode(),
                ex.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrder_shouldRejectRouteWithoutProcessesWithFailFast() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(101L)
                .code("ERP-MO-002")
                .productId(21L)
                .quantity(BigDecimal.TEN)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().id(201L).routeId(31L).itemId(21L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(31L).code("ROUTE-B").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(101L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 1));

        when(workOrderMapper.selectById(101L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(101L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(21L)).thenReturn(routeProduct);
        when(routeMapper.selectById(31L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(31L)).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULE_ORDER_ROUTE_PROCESS_REQUIRED.getCode(),
                ex.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
        verify(scheduleOrderProcessMapper, never()).insert(any(MesProScheduleOrderProcessDO.class));
    }

    @Test
    void createFromWorkOrder_shouldMarkReadyRouteAsAutoSchedulable() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(102L)
                .code("ERP-MO-003")
                .productId(22L)
                .quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().id(202L).routeId(32L).itemId(22L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(32L).code("ROUTE-C").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder().id(302L).routeId(32L).processId(42L).sort(1).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(102L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 2));

        when(workOrderMapper.selectById(102L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(102L))).thenReturn(List.of());
        when(scheduleOrderMapper.selectMaxCodeByPrefix(anyString())).thenReturn(null);
        when(routeProductMapper.selectByItemId(22L)).thenReturn(routeProduct);
        when(routeMapper.selectById(32L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(32L)).thenReturn(List.of(routeProcess));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                32L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(32L)
                        .routeId(32L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build());
        when(routeVersionMapper.selectActiveByRouteId(32L)).thenReturn(MesProRouteVersionDO.builder()
                .id(702L)
                .routeId(32L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .build());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(32L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(32L)
                        .routeId(32L)
                        .routeProcessId(302L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .productionQuantityFactor(BigDecimal.ONE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(702L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(802L)
                        .routeVersionId(702L)
                        .routeProcessId(302L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("8.000000"))
                        .build()));
        when(processMapper.selectBatchIds(List.of(42L))).thenReturn(Collections.emptyList());
        when(workstationMapper.selectListByProcessIds(
                List.of(42L), CommonStatusEnum.ENABLE.getStatus())).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            MesProScheduleOrderDO scheduleOrder = invocation.getArgument(0);
            scheduleOrder.setId(902L);
            return 1;
        }).when(scheduleOrderMapper).insert(any(MesProScheduleOrderDO.class));

        scheduleOrderService.createFromWorkOrder(reqVO);

        ArgumentCaptor<MesProScheduleOrderDO> orderCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper).insert(orderCaptor.capture());
        assertEquals(MesProScheduleOrderRouteStatusEnum.READY.getStatus(), orderCaptor.getValue().getRouteStatus());
        assertEquals(Boolean.TRUE, orderCaptor.getValue().getAutoSchedulable());
    }

    @Test
    void createFromWorkOrder_shouldRejectMissingScheduleUseConfig() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(103L)
                .code("ERP-MO-004")
                .productId(23L)
                .quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(33L).itemId(23L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(33L).code("ROUTE-D").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder().id(303L).routeId(33L).processId(43L).sort(1).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(103L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 3));

        when(workOrderMapper.selectById(103L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(103L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(23L)).thenReturn(routeProduct);
        when(routeMapper.selectById(33L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(33L)).thenReturn(List.of(routeProcess));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_REQUIRED.getCode(), ex.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
    }

    @Test
    void createFromWorkOrder_shouldRejectMissingScheduleConfig() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(104L)
                .code("ERP-MO-005")
                .productId(24L)
                .quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(34L).itemId(24L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(34L).code("ROUTE-E").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder().id(304L).routeId(34L).processId(44L).sort(1).build();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder().id(704L).routeId(34L).versionNo("V1").active(Boolean.TRUE).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(104L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 4));

        when(workOrderMapper.selectById(104L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(104L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(24L)).thenReturn(routeProduct);
        when(routeMapper.selectById(34L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(34L)).thenReturn(List.of(routeProcess));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                34L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(34L)
                        .routeId(34L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build());
        when(routeVersionMapper.selectActiveByRouteId(34L)).thenReturn(routeVersion);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(34L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(34L)
                        .routeId(34L)
                        .routeProcessId(304L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(704L)).thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_ROUTE_SCHEDULE_CONFIG_REQUIRED.getCode(), ex.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
    }

    @Test
    void createFromWorkOrder_shouldRejectResourceCalculatedWhenResourceCapacityMissing() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(106L)
                .code("ERP-MO-007")
                .productId(26L)
                .quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(36L).itemId(26L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(36L).code("ROUTE-G").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder().id(306L).routeId(36L).processId(46L).sort(1).build();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder().id(706L).routeId(36L).versionNo("V1").active(Boolean.TRUE).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(106L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 6));

        when(workOrderMapper.selectById(106L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(106L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(26L)).thenReturn(routeProduct);
        when(routeMapper.selectById(36L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(36L)).thenReturn(List.of(routeProcess));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                36L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(36L)
                        .routeId(36L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build());
        when(routeVersionMapper.selectActiveByRouteId(36L)).thenReturn(routeVersion);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(36L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(36L)
                        .routeId(36L)
                        .routeProcessId(306L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(706L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(806L)
                        .routeVersionId(706L)
                        .routeProcessId(306L)
                        .capacityMode(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode())
                        .build()));
        when(workstationMapper.selectListByProcessIds(
                List.of(46L), CommonStatusEnum.ENABLE.getStatus())).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> scheduleOrderService.createFromWorkOrder(reqVO));

        assertEquals(PRO_SCHEDULE_ORDER_RESOURCE_CAPACITY_REQUIRED.getCode(), ex.getCode());
        verify(scheduleOrderMapper, never()).insert(any(MesProScheduleOrderDO.class));
    }

    @Test
    void getAdmissionDiff_shouldExposeResourceCalculatedCapacityBlocker() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(107L)
                .code("ERP-MO-008")
                .productId(27L)
                .quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(37L).itemId(27L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(37L).code("ROUTE-H").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder().id(307L).routeId(37L).processId(47L).sort(1).build();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder().id(707L).routeId(37L).versionNo("V1").active(Boolean.TRUE).build();
        MesProScheduleOrderAdmissionDiffPageReqVO reqVO = new MesProScheduleOrderAdmissionDiffPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        when(workOrderMapper.selectPage(any())).thenReturn(new PageResult<>(List.of(workOrder), 1L));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(107L))).thenReturn(List.of());
        when(routeProductMapper.selectListByItemId(27L)).thenReturn(List.of(routeProduct));
        when(routeMapper.selectById(37L)).thenReturn(route);
        when(routeVersionMapper.selectActiveByRouteId(37L)).thenReturn(routeVersion);
        when(routeProcessMapper.selectListByRouteId(37L)).thenReturn(List.of(routeProcess));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                37L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(37L)
                        .routeId(37L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(37L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(37L)
                        .routeId(37L)
                        .routeProcessId(307L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(707L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(807L)
                        .routeVersionId(707L)
                        .routeProcessId(307L)
                        .capacityMode(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode())
                        .build()));
        when(workstationMapper.selectListByProcessIds(
                List.of(47L), CommonStatusEnum.ENABLE.getStatus())).thenReturn(Collections.emptyList());

        MesProScheduleOrderAdmissionDiffPageRespVO result = scheduleOrderService.getAdmissionDiff(reqVO);

        assertEquals(1, result.getList().size());
        assertEquals("BLOCKED_RESOURCE_CAPACITY_MISSING", result.getList().get(0).getReasonCode());
        assertEquals("BLOCKED", result.getList().get(0).getAdmissionStatus());
        assertEquals(Boolean.FALSE, result.getList().get(0).getSelectable());
    }

    @Test
    void createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(105L)
                .code("ERP-MO-006")
                .productId(25L)
                .quantity(BigDecimal.ONE)
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder().routeId(35L).itemId(25L).build();
        MesProRouteDO route = MesProRouteDO.builder().id(35L).code("ROUTE-F").status(CommonStatusEnum.ENABLE.getStatus()).build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder().id(305L).routeId(35L).processId(45L).sort(1).build();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder().id(705L).routeId(35L).versionNo("V1").active(Boolean.TRUE).build();
        MesProScheduleOrderCreateFromWorkOrderReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrderReqVO();
        reqVO.setWorkOrderId(105L);
        reqVO.setPromiseDate(LocalDate.of(2026, 7, 5));

        when(workOrderMapper.selectById(105L)).thenReturn(workOrder);
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(105L))).thenReturn(List.of());
        when(routeProductMapper.selectByItemId(25L)).thenReturn(routeProduct);
        when(routeMapper.selectById(35L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(35L)).thenReturn(List.of(routeProcess));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                35L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(35L)
                        .routeId(35L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build());
        when(routeVersionMapper.selectActiveByRouteId(35L)).thenReturn(routeVersion);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(35L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(35L)
                        .routeId(35L)
                        .routeProcessId(305L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .productionQuantityFactor(BigDecimal.ONE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(705L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(805L)
                        .routeVersionId(705L)
                        .routeProcessId(305L)
                        .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                        .hourlyCapacity(new BigDecimal("12.000000"))
                        .build()));
        when(workstationMapper.selectListByProcessIds(
                List.of(45L), CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of(
                cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO.builder()
                        .id(505L)
                        .processId(45L)
                        .code("WS-505")
                        .name("设备工位")
                        .singleStandardHourlyCapacity(new BigDecimal("12.000000"))
                        .build()));
        when(workstationMachineMapper.selectListByWorkstationIds(List.of(505L))).thenReturn(List.of());
        when(workstationWorkerMapper.selectListByWorkstationIds(List.of(505L))).thenReturn(List.of(
                cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO.builder()
                        .id(605L)
                        .workstationId(505L)
                        .quantity(1)
                        .build()));
        when(machineryProcessMapper.selectListByMachineryIds(java.util.Set.of())).thenReturn(List.of());
        when(routeProcessFlowEdgeMapper.selectListByRouteId(35L)).thenReturn(List.of());
        when(processMapper.selectBatchIds(List.of(45L))).thenReturn(List.of(
                MesProProcessDO.builder().id(45L).code("B045").name("设备工序").build()));
        doAnswer(invocation -> {
            MesProScheduleOrderDO scheduleOrder = invocation.getArgument(0);
            scheduleOrder.setId(905L);
            return 1;
        }).when(scheduleOrderMapper).insert(any(MesProScheduleOrderDO.class));

        Long scheduleOrderId = scheduleOrderService.createFromWorkOrder(reqVO);

        assertEquals(905L, scheduleOrderId);
        ArgumentCaptor<MesProScheduleOrderProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderProcessDO.class);
        verify(scheduleOrderProcessMapper).insert(processCaptor.capture());
        MesProScheduleOrderProcessDO snapshot = processCaptor.getValue();
        assertEquals(0, snapshot.getShiftHours().compareTo(new BigDecimal("10.5")));
        assertEquals(0, snapshot.getHourlyCapacityTotal().compareTo(new BigDecimal("12.000000")));
        assertEquals(0, snapshot.getShiftCapacityTotal().compareTo(new BigDecimal("126.0000000")));
    }

}
