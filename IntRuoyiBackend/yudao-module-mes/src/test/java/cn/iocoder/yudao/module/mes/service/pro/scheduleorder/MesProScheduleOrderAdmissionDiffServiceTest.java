package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationWorkerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProScheduleOrderAdmissionDiffServiceTest {

    @InjectMocks
    private MesProScheduleOrderServiceImpl scheduleOrderService;

    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
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

    @org.junit.jupiter.api.BeforeEach
    void setUpProcessIdentity() {
        org.mockito.Mockito.lenient().when(routeProcessService.getProcessIdentityMap(
                        org.mockito.ArgumentMatchers.anyCollection()))
                .thenAnswer(invocation -> identityMap(invocation.getArgument(0)));
        org.mockito.Mockito.lenient().when(routeProcessService.resolveCurrentRouteProcess(
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
        org.mockito.Mockito.lenient().when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO
                        .builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(0))
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build());
    }

    private Map<Long, Long> identityMap(java.util.Collection<Long> processIds) {
        Map<Long, Long> result = new java.util.LinkedHashMap<>();
        processIds.stream().filter(java.util.Objects::nonNull).forEach(id -> result.put(id, id));
        return result;
    }

    @Test
    void getAdmissionDiff_shouldClassifyReadyAlreadyAdmittedMissingRouteAndFrozenRows() {
        MesProWorkOrderDO ready = buildWorkOrder(100L, "MO-READY", 10L, Boolean.FALSE,
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus());
        MesProWorkOrderDO admitted = buildWorkOrder(101L, "MO-ADMITTED", 11L, Boolean.FALSE,
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus());
        MesProWorkOrderDO missingRoute = buildWorkOrder(102L, "MO-MISSING-ROUTE", 12L, Boolean.FALSE,
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus());
        MesProWorkOrderDO frozen = buildWorkOrder(103L, "MO-FROZEN", 13L, Boolean.TRUE,
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus());

        when(workOrderMapper.selectPage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageResult<>(List.of(ready, admitted, missingRoute, frozen), 4L));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L, 101L, 102L, 103L)))
                .thenReturn(List.of(MesProScheduleOrderDO.builder()
                        .id(901L)
                        .workOrderId(101L)
                        .status(MesProScheduleOrderStatusEnum.PREPARE.getStatus())
                        .build()));
        when(routeProductMapper.selectListByItemId(10L)).thenReturn(List.of(MesProRouteProductDO.builder()
                .routeId(201L)
                .itemId(10L)
                .build()));
        when(routeMapper.selectById(201L)).thenReturn(MesProRouteDO.builder()
                .id(201L)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
        when(routeVersionMapper.selectActiveByRouteId(201L)).thenReturn(MesProRouteVersionDO.builder()
                .id(301L)
                .routeId(201L)
                .active(Boolean.TRUE)
                .versionNo("V1")
                .build());
        when(routeProcessMapper.selectListByRouteId(201L)).thenReturn(List.of(MesProRouteProcessDO.builder()
                .id(401L)
                .routeId(201L)
                .processId(501L)
                .build()));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(201L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(201L)
                        .routeId(201L)
                        .routeProcessId(401L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(301L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(601L)
                        .routeVersionId(301L)
                        .routeProcessId(401L)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(new BigDecimal("10.000000"))
                        .build()));
        when(workstationMapper.selectListByProcessIds(List.of(501L))).thenReturn(List.of());

        MesProScheduleOrderAdmissionDiffPageReqVO reqVO = new MesProScheduleOrderAdmissionDiffPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        MesProScheduleOrderAdmissionDiffPageRespVO result = scheduleOrderService.getAdmissionDiff(reqVO);

        assertEquals(4L, result.getTotal());
        assertEquals(1, result.getSummary().getReadyCount());
        assertEquals(1, result.getSummary().getAlreadyAdmittedCount());
        assertEquals(2, result.getSummary().getBlockedCount());
        assertEquals("READY_TO_ADMIT", findByCode(result.getList(), "MO-READY").getAdmissionStatus());
        assertTrue(findByCode(result.getList(), "MO-READY").getSelectable());
        assertEquals("ALREADY_ADMITTED", findByCode(result.getList(), "MO-ADMITTED").getReasonCode());
        assertFalse(findByCode(result.getList(), "MO-ADMITTED").getSelectable());
        assertEquals("BLOCKED_MISSING_ROUTE", findByCode(result.getList(), "MO-MISSING-ROUTE").getReasonCode());
        assertEquals("BLOCKED_WORK_ORDER_FROZEN", findByCode(result.getList(), "MO-FROZEN").getReasonCode());
    }

    @Test
    void getAdmissionDiff_shouldFilterRowsByReasonCode() {
        MesProWorkOrderDO missingRoute = buildWorkOrder(102L, "MO-MISSING-ROUTE", 12L, Boolean.FALSE,
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus());
        MesProWorkOrderDO frozen = buildWorkOrder(103L, "MO-FROZEN", 13L, Boolean.TRUE,
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus());

        when(workOrderMapper.selectPage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageResult<>(List.of(missingRoute, frozen), 2L));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(102L, 103L)))
                .thenReturn(List.of());

        MesProScheduleOrderAdmissionDiffPageReqVO reqVO = new MesProScheduleOrderAdmissionDiffPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setReasonCode("BLOCKED_MISSING_ROUTE");

        MesProScheduleOrderAdmissionDiffPageRespVO result = scheduleOrderService.getAdmissionDiff(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getSummary().getBlockedCount());
        assertEquals("MO-MISSING-ROUTE", result.getList().get(0).getWorkOrderCode());
        assertEquals("BLOCKED_MISSING_ROUTE", result.getList().get(0).getReasonCode());
    }

    @Test
    void getAdmissionDiff_shouldWarnInsteadOfBlockingForDefaultScheduleConfig() {
        MesProWorkOrderDO workOrder = buildWorkOrder(104L, "MO-DEFAULT-SCHEDULE", 14L, Boolean.FALSE,
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus());

        when(workOrderMapper.selectPage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageResult<>(List.of(workOrder), 1L));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(104L))).thenReturn(List.of());
        when(routeProductMapper.selectListByItemId(14L)).thenReturn(List.of(MesProRouteProductDO.builder()
                .routeId(204L)
                .itemId(14L)
                .build()));
        when(routeMapper.selectById(204L)).thenReturn(MesProRouteDO.builder()
                .id(204L)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
        when(routeVersionMapper.selectActiveByRouteId(204L)).thenReturn(MesProRouteVersionDO.builder()
                .id(304L)
                .routeId(204L)
                .active(Boolean.TRUE)
                .versionNo("V2")
                .build());
        when(routeProcessMapper.selectListByRouteId(204L)).thenReturn(List.of(MesProRouteProcessDO.builder()
                .id(404L)
                .routeId(204L)
                .processId(504L)
                .build()));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(204L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(204L)
                        .routeId(204L)
                        .routeProcessId(404L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(304L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(604L)
                        .routeVersionId(304L)
                        .routeProcessId(404L)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(new BigDecimal("1.000000"))
                        .configVersion(MesProRouteServiceImpl.DEFAULT_SCHEDULE_CONFIG_VERSION)
                        .build()));

        MesProScheduleOrderAdmissionDiffPageReqVO reqVO = new MesProScheduleOrderAdmissionDiffPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        MesProScheduleOrderAdmissionDiffPageRespVO result = scheduleOrderService.getAdmissionDiff(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(0, result.getSummary().getBlockedCount());
        assertEquals(1, result.getSummary().getWarnCount());
        MesProScheduleOrderAdmissionDiffRespVO row = result.getList().get(0);
        assertEquals("READY_TO_ADMIT", row.getAdmissionStatus());
        assertEquals("WARN", row.getSeverity());
        assertEquals("WARN", row.getSchedulableStatus());
        assertEquals("WARN_DEFAULT_ROUTE_SCHEDULE_CONFIG", row.getReasonCode());
        assertTrue(row.getSelectable());
    }

    @Test
    void getAdmissionDiff_shouldBlockDuplicateRouteProductMappingsWithoutSystemException() {
        MesProWorkOrderDO workOrder = buildWorkOrder(106L, "MO-DUPLICATE-ROUTE", 16L, Boolean.FALSE,
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus());

        when(workOrderMapper.selectPage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageResult<>(List.of(workOrder), 1L));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(106L))).thenReturn(List.of());
        when(routeProductMapper.selectListByItemId(16L)).thenReturn(List.of(
                MesProRouteProductDO.builder().id(701L).routeId(207L).itemId(16L).build(),
                MesProRouteProductDO.builder().id(702L).routeId(208L).itemId(16L).build()
        ));

        MesProScheduleOrderAdmissionDiffPageReqVO reqVO = new MesProScheduleOrderAdmissionDiffPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        MesProScheduleOrderAdmissionDiffPageRespVO result = scheduleOrderService.getAdmissionDiff(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getSummary().getBlockedCount());
        MesProScheduleOrderAdmissionDiffRespVO row = result.getList().get(0);
        assertEquals("MO-DUPLICATE-ROUTE", row.getWorkOrderCode());
        assertEquals("BLOCKED_ROUTE_PRODUCT_AMBIGUOUS", row.getReasonCode());
        assertEquals("BLOCKED", row.getAdmissionStatus());
        assertFalse(row.getSelectable());
        verify(routeMapper, never()).selectById(207L);
    }

    @Test
    void getAdmissionDiff_shouldBlockMissingWorkerQuantityBeforeSelection() {
        MesProWorkOrderDO workOrder = buildWorkOrder(105L, "MO-MISSING-WORKER-QUANTITY", 15L, Boolean.FALSE,
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus());

        when(workOrderMapper.selectPage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageResult<>(List.of(workOrder), 1L));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(105L))).thenReturn(List.of());
        when(routeProductMapper.selectListByItemId(15L)).thenReturn(List.of(MesProRouteProductDO.builder()
                .routeId(205L)
                .itemId(15L)
                .build()));
        when(routeMapper.selectById(205L)).thenReturn(MesProRouteDO.builder()
                .id(205L)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
        when(routeVersionMapper.selectActiveByRouteId(205L)).thenReturn(MesProRouteVersionDO.builder()
                .id(305L)
                .routeId(205L)
                .active(Boolean.TRUE)
                .versionNo("V1")
                .build());
        when(routeProcessMapper.selectListByRouteId(205L)).thenReturn(List.of(MesProRouteProcessDO.builder()
                .id(405L)
                .routeId(205L)
                .processId(505L)
                .build()));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(205L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(205L)
                        .routeId(205L)
                        .routeProcessId(405L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(305L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(605L)
                        .routeVersionId(305L)
                        .routeProcessId(405L)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(new BigDecimal("1.000000"))
                        .build()));
        when(workstationMapper.selectListByProcessIds(List.of(505L))).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(705L).code("WS-705").name("人工工位")
                        .processId(505L).shiftHours(new BigDecimal("8.0"))
                        .singleStandardHourlyCapacity(new BigDecimal("2.0")).build()
        ));
        when(workstationMachineMapper.selectListByWorkstationIds(List.of(705L))).thenReturn(List.of());
        when(workstationWorkerMapper.selectListByWorkstationIds(List.of(705L))).thenReturn(List.of(
                MesMdWorkstationWorkerDO.builder().id(805L).workstationId(705L).quantity(null).build()
        ));
        when(machineryProcessMapper.selectListByMachineryIds(Set.of())).thenReturn(List.of());

        MesProScheduleOrderAdmissionDiffPageReqVO reqVO = new MesProScheduleOrderAdmissionDiffPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        MesProScheduleOrderAdmissionDiffPageRespVO result = scheduleOrderService.getAdmissionDiff(reqVO);

        MesProScheduleOrderAdmissionDiffRespVO row = result.getList().get(0);
        assertEquals("BLOCKED", row.getAdmissionStatus());
        assertEquals("BLOCKED_WORKER_QUANTITY_REQUIRED", row.getReasonCode());
        assertFalse(row.getSelectable());
        assertEquals(1, result.getSummary().getBlockedCount());
        assertEquals(0, result.getSummary().getReadyCount());
    }

    @Test
    void getAdmissionDiff_shouldNotLoadAllWorkOrdersForReadyFilterPage() {
        MesProWorkOrderDO ready = buildWorkOrder(100L, "MO-READY", 10L, Boolean.FALSE,
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus());
        when(workOrderMapper.selectPage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageResult<>(List.of(ready), 1L))
                .thenReturn(new PageResult<>(List.of(), 1L));
        when(scheduleOrderMapper.selectListByWorkOrderIds(List.of(100L))).thenReturn(List.of());
        mockReadyRoute(10L, 201L, 301L, 401L, 501L);

        MesProScheduleOrderAdmissionDiffPageReqVO reqVO = new MesProScheduleOrderAdmissionDiffPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setAdmissionStatus("READY_TO_ADMIT");

        MesProScheduleOrderAdmissionDiffPageRespVO result = scheduleOrderService.getAdmissionDiff(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("MO-READY", result.getList().get(0).getWorkOrderCode());
        verify(workOrderMapper, never()).selectPage(argThat(req -> PageParam.PAGE_SIZE_NONE.equals(req.getPageSize())));
    }

    private void mockReadyRoute(Long productId, Long routeId, Long routeVersionId, Long routeProcessId, Long processId) {
        when(routeProductMapper.selectListByItemId(productId)).thenReturn(List.of(MesProRouteProductDO.builder()
                .routeId(routeId)
                .itemId(productId)
                .build()));
        when(routeMapper.selectById(routeId)).thenReturn(MesProRouteDO.builder()
                .id(routeId)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(MesProRouteVersionDO.builder()
                .id(routeVersionId)
                .routeId(routeId)
                .active(Boolean.TRUE)
                .versionNo("V1")
                .build());
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .routeId(routeId)
                .processId(processId)
                .build()));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .routeFlowConfigId(routeId)
                        .routeId(routeId)
                        .routeProcessId(routeProcessId)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                        .enabled(Boolean.TRUE)
                        .build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(601L)
                        .routeVersionId(routeVersionId)
                        .routeProcessId(routeProcessId)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(new BigDecimal("10.000000"))
                        .build()));
        when(workstationMapper.selectListByProcessIds(List.of(processId))).thenReturn(List.of());
    }

    private MesProWorkOrderDO buildWorkOrder(Long id, String code, Long productId, Boolean frozen, Integer status) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code(code)
                .productId(productId)
                .quantity(new BigDecimal("100.000000"))
                .temporaryFrozen(frozen)
                .status(status)
                .build();
    }

    private MesProScheduleOrderAdmissionDiffRespVO findByCode(List<MesProScheduleOrderAdmissionDiffRespVO> rows,
                                                              String code) {
        return rows.stream()
                .filter(row -> code.equals(row.getWorkOrderCode()))
                .findFirst()
                .orElseThrow();
    }

}
