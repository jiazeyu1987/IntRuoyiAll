package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.repair.MesDvRepairDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.repair.MesDvRepairMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.dv.MesDvRepairStatusEnum;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProScheduleResourceAdjustmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteProcessControllerWorkstationViewTest {

    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Mock
    private MesDvMachineryService machineryService;
    @Mock
    private MesDvMachineryProcessService machineryProcessService;
    @Mock
    private MesDvRepairMapper repairMapper;
    @Mock
    private MesProBatchRecordReportMapper batchRecordReportMapper;
    @Mock
    private MesProScheduleResourceAdjustmentService scheduleResourceAdjustmentService;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @InjectMocks
    private MesProRouteProcessController controller;

    @BeforeEach
    void setUp() {
        lenient().when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of());
        lenient().when(scheduleResourceAdjustmentService.getAdjustmentList(any(), any()))
                .thenReturn(List.of());
        lenient().when(routeVersionMapper.selectActiveByRouteId(any())).thenReturn(null);
        lenient().when(routeProcessFlowEdgeMapper.selectListByRouteId(any())).thenReturn(List.of());
    }

    @Test
    void getRouteProcessListByRoute_returnsSinglePredecessorAndMultipleSuccessors() {
        Long routeId = 20L;
        MesProRouteProcessDO processA = routeProcess(21L, routeId, 201L, 1);
        MesProRouteProcessDO processB = routeProcess(22L, routeId, 202L, 2);
        MesProRouteProcessDO processC = routeProcess(23L, routeId, 203L, 3);
        MesProRouteProcessDO processD = routeProcess(24L, routeId, 204L, 4);
        when(routeProcessService.getRouteProcessListByRouteId(routeId))
                .thenReturn(List.of(processA, processB, processC, processD));
        when(processService.getProcessList(any())).thenReturn(List.of(
                process(201L, "A", "工序A"),
                process(202L, "B", "工序B"),
                process(203L, "C", "工序C"),
                process(204L, "D", "工序D")));
        when(routeProcessFlowEdgeMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                flowEdge(routeId, 21L, 22L),
                flowEdge(routeId, 21L, 23L),
                flowEdge(routeId, 23L, 24L)));

        CommonResult<List<MesProRouteProcessRespVO>> response =
                controller.getRouteProcessListByRoute(routeId, null);

        MesProRouteProcessRespVO routeProcessA = response.getData().get(0);
        MesProRouteProcessRespVO routeProcessB = response.getData().get(1);
        MesProRouteProcessRespVO routeProcessC = response.getData().get(2);
        MesProRouteProcessRespVO routeProcessD = response.getData().get(3);
        assertNull(routeProcessA.getPredecessor());
        assertTrue(routeProcessA.getPredecessors().isEmpty());
        assertEquals(List.of("工序B", "工序C"), routeProcessA.getSuccessors().stream()
                .map(item -> item.getProcessName())
                .toList());
        assertEquals("工序A", routeProcessB.getPredecessor().getProcessName());
        assertEquals(List.of("工序A"), routeProcessB.getPredecessors().stream()
                .map(item -> item.getProcessName())
                .toList());
        assertEquals("工序A", routeProcessC.getPredecessor().getProcessName());
        assertEquals("工序C", routeProcessD.getPredecessor().getProcessName());
    }

    @Test
    void getRouteProcessListByRoute_returnsMultiplePredecessorsWithoutSystemException() {
        Long routeId = 30L;
        MesProRouteProcessDO processA = routeProcess(31L, routeId, 301L, 1);
        MesProRouteProcessDO processB = routeProcess(32L, routeId, 302L, 2);
        MesProRouteProcessDO processC = routeProcess(33L, routeId, 303L, 3);
        MesProRouteProcessDO processD = routeProcess(34L, routeId, 304L, 4);
        when(routeProcessService.getRouteProcessListByRouteId(routeId))
                .thenReturn(List.of(processA, processB, processC, processD));
        when(processService.getProcessList(any())).thenReturn(List.of(
                process(301L, "A", "工序A"),
                process(302L, "B", "工序B"),
                process(303L, "C", "工序C"),
                process(304L, "D", "工序D")));
        when(routeProcessFlowEdgeMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                flowEdge(routeId, 31L, 34L),
                flowEdge(routeId, 32L, 34L),
                flowEdge(routeId, 33L, 34L)));

        CommonResult<List<MesProRouteProcessRespVO>> response =
                controller.getRouteProcessListByRoute(routeId, null);

        MesProRouteProcessRespVO routeProcessD = response.getData().get(3);
        assertNull(routeProcessD.getPredecessor());
        assertEquals(List.of("工序A", "工序B", "工序C"), routeProcessD.getPredecessors().stream()
                .map(item -> item.getProcessName())
                .toList());
        assertTrue(routeProcessD.getSuccessors().isEmpty());
    }

    @Test
    void getRouteProcessListByRoute_skipsStaleFlowEdgeAndReturnsCurrentRouteRelations() {
        Long routeId = 31L;
        MesProRouteProcessDO processA = routeProcess(51L, routeId, 501L, 1);
        MesProRouteProcessDO processB = routeProcess(52L, routeId, 502L, 2);
        when(routeProcessService.getRouteProcessListByRouteId(routeId))
                .thenReturn(List.of(processA, processB));
        when(processService.getProcessList(any())).thenReturn(List.of(
                process(501L, "A", "工序A"),
                process(502L, "B", "工序B")));
        when(routeProcessFlowEdgeMapper.selectListByRouteId(routeId)).thenReturn(List.of(
                flowEdge(routeId, 51L, 52L),
                flowEdge(routeId, 51L, 599L),
                flowEdge(routeId, 598L, 52L)));

        CommonResult<List<MesProRouteProcessRespVO>> response =
                controller.getRouteProcessListByRoute(routeId, null);

        MesProRouteProcessRespVO routeProcessA = response.getData().get(0);
        MesProRouteProcessRespVO routeProcessB = response.getData().get(1);
        assertTrue(routeProcessA.getPredecessors().isEmpty());
        assertEquals(List.of("工序B"), routeProcessA.getSuccessors().stream()
                .map(item -> item.getProcessName())
                .toList());
        assertEquals("工序A", routeProcessB.getPredecessor().getProcessName());
        assertEquals(List.of("工序A"), routeProcessB.getPredecessors().stream()
                .map(item -> item.getProcessName())
                .toList());
        assertTrue(routeProcessB.getSuccessors().isEmpty());
    }

    @Test
    void getRouteProcessListByRoute_returnsWorkstationFieldsWhenPresent() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(1L).routeId(10L).processId(101L).workstationId(201L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(10L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder()
                        .id(101L)
                        .productName("球囊扩张导管")
                        .code("P101")
                        .name("吹球囊成型")
                        .attention("更新后的工艺要求")
                        .status(1)
                        .manualShiftCapacity(new BigDecimal("740"))
                        .build()));
        when(workstationService.getWorkstationList(any()))
                .thenReturn(List.of(MesMdWorkstationDO.builder().id(201L).processId(101L)
                        .code("WS-P101").name("吹球囊成型-工位")
                        .shiftHours(new BigDecimal("8")).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(List.of());

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(10L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertEquals(201L, vo.getWorkstationId());
        assertEquals("WS-P101", vo.getWorkstationCode());
        assertEquals("吹球囊成型-工位", vo.getWorkstationName());
        assertEquals("P101", vo.getProcessCode());
        assertEquals("吹球囊成型", vo.getProcessName());
        assertEquals("球囊扩张导管", vo.getProcessProductName());
        assertEquals("更新后的工艺要求", vo.getProcessAttention());
        assertEquals(1, vo.getProcessStatus());
        assertEquals(0, vo.getProcessManualShiftCapacity().compareTo(new BigDecimal("740")));
        assertEquals(0, vo.getMachineryQuantityTotal());
        assertTrue(vo.getMachineryList().isEmpty());
    }

    @Test
    void getRouteProcessListByRoute_acceptsBoundWorkstationProcessAlias() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(11L).routeId(10L).processId(101L).workstationId(201L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(10L)).thenReturn(List.of(routeProcess));
        when(routeProcessService.getProcessIdentityMap(any())).thenReturn(Map.of(101L, 101L, 901L, 101L));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder()
                        .id(101L)
                        .productName("球囊扩张导管")
                        .code("P101")
                        .name("吹球囊成型")
                        .build()));
        when(workstationService.getWorkstationListByProcessIds(any()))
                .thenReturn(List.of(MesMdWorkstationDO.builder().id(201L).processId(901L)
                        .code("WS-P101-OLD").name("吹球囊成型-历史工位")
                        .singleStandardHourlyCapacity(new BigDecimal("10"))
                        .shiftHours(new BigDecimal("8")).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(List.of());

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(10L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertEquals(201L, vo.getWorkstationId());
        assertEquals("WS-P101-OLD", vo.getWorkstationCode());
        assertEquals("WORKER", vo.getCapacitySource());
        assertEquals(0, vo.getProcessHourlyCapacityTotal().compareTo(new BigDecimal("10")));
    }

    @Test
    void getRouteProcessListByRoute_keepsWorkstationFieldsEmptyWhenAbsent() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(2L).routeId(11L).processId(102L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(11L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(102L).code("P102").name("无工位工序").build()));

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(11L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertNull(vo.getWorkstationId());
        assertNull(vo.getWorkstationCode());
        assertNull(vo.getWorkstationName());
        assertEquals("UNCONFIGURED", vo.getCapacitySource());
        assertEquals(0, vo.getWorkerQuantityTotal());
        assertEquals(0, vo.getProcessHourlyCapacityTotal().compareTo(BigDecimal.ZERO));
        assertEquals(0, vo.getProcessShiftCapacityTotal().compareTo(BigDecimal.ZERO));
    }

    @Test
    void getRouteProcessListByRoute_returnsBatchRecordFieldsWhenBound() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(3L).routeId(12L).processId(103L).sort(1)
                .batchRecordReportId("ebr-report-1")
                .build();
        when(routeProcessService.getRouteProcessListByRouteId(12L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(103L).code("P103").name("焊接").build()));
        when(batchRecordReportMapper.selectListByReportIds(any()))
                .thenReturn(List.of(report("ebr-report-1", "EBR_A_T01", "电子批记录[A]-表1")));

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(12L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertEquals("ebr-report-1", vo.getBatchRecordReportId());
        assertEquals("EBR_A_T01", vo.getBatchRecordReportCode());
        assertEquals("电子批记录[A]-表1", vo.getBatchRecordReportName());
    }

    @Test
    void getRouteProcessListByRoute_returnsMachineryQuantityTotalAndMachineryList() {
        MesProRouteProcessDO routeProcessWithMachines = MesProRouteProcessDO.builder()
                .id(4L).routeId(13L).processId(104L).workstationId(201L).sort(1).build();
        MesProRouteProcessDO routeProcessWithoutMachines = MesProRouteProcessDO.builder()
                .id(5L).routeId(13L).processId(105L).workstationId(203L).sort(2).build();
        when(routeProcessService.getRouteProcessListByRouteId(13L))
                .thenReturn(List.of(routeProcessWithMachines, routeProcessWithoutMachines));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(
                        MesProProcessDO.builder().id(104L).code("P104").name("吹球囊成型").build(),
                        MesProProcessDO.builder().id(105L).code("P105").name("无设备工序").build()));
        when(workstationService.getWorkstationList(any()))
                .thenReturn(List.of(
                        MesMdWorkstationDO.builder().id(201L).processId(104L)
                                .code("WS-A").name("吹球囊 A")
                                .singleStandardHourlyCapacity(new BigDecimal("99.00"))
                                .shiftHours(new BigDecimal("10.5")).build(),
                        MesMdWorkstationDO.builder().id(203L).processId(105L)
                                .code("WS-C").name("无设备工位")
                                .singleStandardHourlyCapacity(new BigDecimal("3.00"))
                                .shiftHours(new BigDecimal("8.00")).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(
                        MesMdWorkstationMachineDO.builder().id(302L).workstationId(201L)
                                .machineryId(502L).quantity(3).build(),
                        MesMdWorkstationMachineDO.builder().id(301L).workstationId(201L)
                                .machineryId(501L).quantity(2).build()));
        when(machineryService.getMachineryMap(any())).thenReturn(Map.of(
                501L, MesDvMachineryDO.builder().id(501L).code("M-A").name("设备 A").build(),
                502L, MesDvMachineryDO.builder().id(502L).code("M-B").name("设备 B").build()));
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(any(), any()))
                .thenReturn(List.of(
                        MesDvMachineryProcessDO.builder().machineryId(501L).processId(104L)
                                .standardHourlyCapacity(new BigDecimal("12.50")).build(),
                        MesDvMachineryProcessDO.builder().machineryId(502L).processId(104L)
                                .standardHourlyCapacity(new BigDecimal("8.00")).build()));
        when(repairMapper.selectListByMachineryIdsAndStatuses(any(), any()))
                .thenReturn(List.of(MesDvRepairDO.builder().id(801L).machineryId(502L)
                        .status(MesDvRepairStatusEnum.CONFIRMED.getStatus()).name("设备 B 维修").build()));
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(
                        MesMdWorkstationWorkerDO.builder().id(401L).workstationId(201L).quantity(7).build(),
                        MesMdWorkstationWorkerDO.builder().id(402L).workstationId(203L).quantity(5).build()));

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(13L, null);

        MesProRouteProcessRespVO withMachines = response.getData().get(0);
        assertEquals(5, withMachines.getMachineryQuantityTotal());
        assertEquals(2, withMachines.getMachineryList().size());
        assertEquals(301L, withMachines.getMachineryList().get(0).getWorkstationMachineId());
        assertEquals("WS-A", withMachines.getMachineryList().get(0).getWorkstationCode());
        assertEquals("M-A", withMachines.getMachineryList().get(0).getMachineryCode());
        assertEquals(2, withMachines.getMachineryList().get(0).getQuantity());
        assertEquals(new BigDecimal("12.50"),
                withMachines.getMachineryList().get(0).getMachineryStandardHourlyCapacity());
        assertEquals(new BigDecimal("25.00"),
                withMachines.getMachineryList().get(0).getMachineryHourlyCapacityTotal());
        assertEquals(302L, withMachines.getMachineryList().get(1).getWorkstationMachineId());
        assertEquals(new BigDecimal("24.00"),
                withMachines.getMachineryList().get(1).getMachineryHourlyCapacityTotal());
        assertEquals("MACHINE", withMachines.getCapacitySource());
        assertEquals(0, withMachines.getWorkerQuantityTotal());
        assertEquals(0, withMachines.getProcessHourlyCapacityTotal().compareTo(new BigDecimal("49.00")));
        assertEquals(0, withMachines.getProcessShiftCapacityTotal().compareTo(new BigDecimal("514.500")));
        assertEquals(2, withMachines.getTodayAvailableResourceQuantityTotal());
        assertEquals(0, withMachines.getTodayHourlyCapacityTotal().compareTo(new BigDecimal("25.00")));
        assertEquals(0, withMachines.getTodayShiftCapacityTotal().compareTo(new BigDecimal("262.500")));
        assertEquals("REPAIR", withMachines.getResourceStatus());
        assertTrue(withMachines.getResourceStatusReason().contains("设备 B 维修"));
        assertEquals(2, withMachines.getMachineryList().get(0).getAvailableQuantity());
        assertEquals(0, withMachines.getMachineryList().get(1).getAvailableQuantity());
        assertTrue(withMachines.getMachineryList().get(1).getUnderRepair());

        MesProRouteProcessRespVO withoutMachines = response.getData().get(1);
        assertEquals(0, withoutMachines.getMachineryQuantityTotal());
        assertTrue(withoutMachines.getMachineryList().isEmpty());
        assertEquals("WORKER", withoutMachines.getCapacitySource());
        assertEquals(402L, withoutMachines.getWorkstationWorkerId());
        assertEquals(5, withoutMachines.getWorkerQuantityTotal());
        assertEquals(0, withoutMachines.getProcessHourlyCapacityTotal().compareTo(new BigDecimal("3.00")));
        assertEquals(0, withoutMachines.getShiftHours().compareTo(new BigDecimal("8.00")));
        assertEquals(0, withoutMachines.getProcessShiftCapacityTotal().compareTo(new BigDecimal("24.0000")));
        assertEquals(5, withoutMachines.getTodayAvailableResourceQuantityTotal());
        assertEquals(0, withoutMachines.getTodayShiftCapacityTotal().compareTo(new BigDecimal("24.0000")));
        assertEquals("NORMAL", withoutMachines.getResourceStatus());
    }

    @Test
    void getRouteProcessListByRoute_usesFiniteScheduleConfigAsDisplayedCapacityWithoutChangingResources() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(40L).routeId(20L).processId(140L).workstationId(240L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(20L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(140L).code("P140").name("有限联动工序").build()));
        when(workstationService.getWorkstationList(any()))
                .thenReturn(List.of(MesMdWorkstationDO.builder().id(240L).processId(140L)
                        .code("WS-FINITE").name("有限联动工位")
                        .shiftHours(new BigDecimal("10.5")).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder().id(340L).workstationId(240L)
                        .machineryId(540L).quantity(1).build()));
        when(machineryService.getMachineryMap(any())).thenReturn(Map.of(
                540L, MesDvMachineryDO.builder().id(540L).code("M-FINITE").name("有限联动设备").build()));
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(any(), any()))
                .thenReturn(List.of(MesDvMachineryProcessDO.builder().machineryId(540L).processId(140L)
                        .standardHourlyCapacity(new BigDecimal("12.50")).build()));
        when(repairMapper.selectListByMachineryIdsAndStatuses(any(), any())).thenReturn(List.of());
        when(routeVersionMapper.selectActiveByRouteId(20L)).thenReturn(MesProRouteVersionDO.builder()
                .id(920L).routeId(20L).active(Boolean.TRUE).build());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(920L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .routeVersionId(920L)
                        .routeProcessId(40L)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(new BigDecimal("60"))
                        .build()));

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(20L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertEquals(0, vo.getProcessHourlyCapacityTotal().compareTo(new BigDecimal("60")));
        assertEquals(0, vo.getProcessShiftCapacityTotal().compareTo(new BigDecimal("630.0")));
        assertEquals(0, vo.getMachineryList().get(0).getMachineryHourlyCapacityTotal()
                .compareTo(new BigDecimal("12.50")));
    }

    @Test
    void getRouteProcessListByRoute_recalculatesFiniteScheduleConfigWhenShiftHoursChanges() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(41L).routeId(21L).processId(141L).workstationId(241L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(21L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(141L).code("P141").name("班次重算工序").build()));
        when(workstationService.getWorkstationList(any()))
                .thenReturn(List.of(MesMdWorkstationDO.builder().id(241L).processId(141L)
                        .code("WS-SHIFT").name("班次重算工位")
                        .singleStandardHourlyCapacity(new BigDecimal("9.00"))
                        .shiftHours(new BigDecimal("8")).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(List.of());
        when(routeVersionMapper.selectActiveByRouteId(21L)).thenReturn(MesProRouteVersionDO.builder()
                .id(921L).routeId(21L).active(Boolean.TRUE).build());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(921L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .routeVersionId(921L)
                        .routeProcessId(41L)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(new BigDecimal("60"))
                        .build()));

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(21L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertEquals(0, vo.getShiftHours().compareTo(new BigDecimal("8")));
        assertEquals(0, vo.getProcessHourlyCapacityTotal().compareTo(new BigDecimal("60")));
        assertEquals(0, vo.getProcessShiftCapacityTotal().compareTo(new BigDecimal("480")));
    }

    @Test
    void getRouteProcessListByRoute_doesNotConvertInfiniteFormulaToShiftCapacity() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(42L).routeId(22L).processId(142L).workstationId(242L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(22L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(142L).code("P142").name("无限公式工序").build()));
        when(workstationService.getWorkstationList(any()))
                .thenReturn(List.of(MesMdWorkstationDO.builder().id(242L).processId(142L)
                        .code("WS-INFINITE").name("无限公式工位")
                        .singleStandardHourlyCapacity(new BigDecimal("15"))
                        .shiftHours(new BigDecimal("8")).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(List.of());
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder()
                        .id(442L)
                        .workstationId(242L)
                        .quantity(5)
                        .build()));
        when(routeVersionMapper.selectActiveByRouteId(22L)).thenReturn(MesProRouteVersionDO.builder()
                .id(922L).routeId(22L).active(Boolean.TRUE).build());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(922L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .routeVersionId(922L)
                        .routeProcessId(42L)
                        .capacityMode("INFINITE_FORMULA")
                        .infiniteDurationQuantityFactor(new BigDecimal("60"))
                        .infiniteDurationBaseMinutes(new BigDecimal("0"))
                        .build()));

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(22L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertEquals(0, vo.getProcessHourlyCapacityTotal().compareTo(new BigDecimal("15")));
        assertEquals(0, vo.getProcessShiftCapacityTotal().compareTo(new BigDecimal("120")));
    }

    @Test
    void getRouteProcessListByRoute_usesBoundWorkstationAndExposesMissingShiftHours() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(985L).routeId(23L).processId(922985L).workstationId(800L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(23L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(922985L).code("P985").name("缺班次工序").build()));
        when(workstationService.getWorkstationListByProcessIds(any()))
                .thenReturn(List.of(
                        MesMdWorkstationDO.builder().id(800L).processId(922985L)
                                .code("WS-BOUND").name("绑定工位").build(),
                        MesMdWorkstationDO.builder().id(801L).processId(922985L)
                                .code("WS-OTHER").name("其它同工序工位")
                                .shiftHours(new BigDecimal("7")).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(
                        MesMdWorkstationMachineDO.builder().id(3800L).workstationId(800L)
                                .machineryId(5800L).quantity(1).build(),
                        MesMdWorkstationMachineDO.builder().id(3801L).workstationId(801L)
                                .machineryId(5801L).quantity(1).build()));
        when(machineryService.getMachineryMap(any())).thenReturn(Map.of(
                5800L, MesDvMachineryDO.builder().id(5800L).code("M-BOUND").name("绑定设备").build(),
                5801L, MesDvMachineryDO.builder().id(5801L).code("M-OTHER").name("其它设备").build()));
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(any(), any()))
                .thenReturn(List.of(
                        MesDvMachineryProcessDO.builder().machineryId(5800L).processId(922985L)
                                .standardHourlyCapacity(new BigDecimal("12")).build(),
                        MesDvMachineryProcessDO.builder().machineryId(5801L).processId(922985L)
                                .standardHourlyCapacity(new BigDecimal("99")).build()));
        when(repairMapper.selectListByMachineryIdsAndStatuses(any(), any())).thenReturn(List.of());

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(23L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertNull(vo.getShiftHours());
        assertEquals("CAPACITY_MISSING", vo.getResourceStatus());
        assertEquals("班次小时未配置", vo.getResourceStatusReason());
        assertEquals(1, vo.getMachineryList().size());
        assertEquals(800L, vo.getMachineryList().get(0).getWorkstationId());
        assertNull(vo.getMachineryList().get(0).getAvailableShiftCapacityTotal());
        assertEquals(0, vo.getProcessHourlyCapacityTotal().compareTo(new BigDecimal("12")));
        assertNull(vo.getProcessShiftCapacityTotal());
    }

    @Test
    void getRouteProcessListByRoute_allowsExplicitBoundWorkstationFromDifferentProcess() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(986L).routeId(26L).processId(922986L).workstationId(802L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(26L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(922986L).code("P986").name("复用工位工序").build()));
        when(workstationService.getWorkstationListByProcessIds(any())).thenReturn(List.of());
        when(workstationService.getWorkstationList(any()))
                .thenReturn(List.of(MesMdWorkstationDO.builder()
                        .id(802L)
                        .processId(922111L)
                        .code("WS-SHARED")
                        .name("共享工作站")
                        .singleStandardHourlyCapacity(new BigDecimal("5"))
                        .shiftHours(new BigDecimal("8"))
                        .build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(List.of());

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(26L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertEquals(802L, vo.getWorkstationId());
        assertEquals("WS-SHARED", vo.getWorkstationCode());
        assertEquals(0, vo.getShiftHours().compareTo(new BigDecimal("8")));
        assertEquals(0, vo.getProcessHourlyCapacityTotal().compareTo(new BigDecimal("5")));
        assertEquals(0, vo.getProcessShiftCapacityTotal().compareTo(new BigDecimal("40")));
    }

    @Test
    void getRouteProcessListByRoute_exposesShiftHoursConflictWithoutSystemException() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(917L).routeId(24L).processId(922917L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(24L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(922917L).code("P917").name("班次冲突工序").build()));
        when(workstationService.getWorkstationListByProcessIds(any()))
                .thenReturn(List.of(
                        MesMdWorkstationDO.builder().id(900L).processId(922917L)
                                .code("WS-DAY").name("白班工位")
                                .singleStandardHourlyCapacity(new BigDecimal("10"))
                                .shiftHours(new BigDecimal("7")).build(),
                        MesMdWorkstationDO.builder().id(901L).processId(922917L)
                                .code("WS-NIGHT").name("夜班工位")
                                .singleStandardHourlyCapacity(new BigDecimal("10"))
                                .shiftHours(new BigDecimal("8")).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(List.of());
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(
                        MesMdWorkstationWorkerDO.builder().id(4900L).workstationId(900L).quantity(1).build(),
                        MesMdWorkstationWorkerDO.builder().id(4901L).workstationId(901L).quantity(1).build()));

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(24L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertNull(vo.getShiftHours());
        assertEquals("CAPACITY_MISSING", vo.getResourceStatus());
        assertEquals("班次小时配置不一致", vo.getResourceStatusReason());
        assertEquals(0, vo.getProcessHourlyCapacityTotal().compareTo(new BigDecimal("20")));
        assertNull(vo.getProcessShiftCapacityTotal());
    }

    @Test
    void getRouteProcessListByRoute_usesManualWorkerCapacityWhenWorkerQuantityMissing() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(922467L).routeId(25L).processId(9224670L).workstationId(922707L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(25L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(9224670L).code("P467").name("人员配置工序").build()));
        when(workstationService.getWorkstationListByProcessIds(any()))
                .thenReturn(List.of(MesMdWorkstationDO.builder().id(922707L).processId(9224670L)
                        .code("WS-922707").name("人员配置工位")
                        .singleStandardHourlyCapacity(new BigDecimal("10"))
                        .shiftHours(new BigDecimal("8")).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(List.of());
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder()
                        .id(922807L)
                        .workstationId(922707L)
                        .quantity(null)
                        .build()));

        CommonResult<List<MesProRouteProcessRespVO>> response = controller.getRouteProcessListByRoute(25L, null);

        MesProRouteProcessRespVO vo = response.getData().get(0);
        assertEquals(922467L, vo.getId());
        assertEquals(922707L, vo.getWorkstationId());
        assertEquals("WORKER", vo.getCapacitySource());
        assertEquals(0, vo.getWorkerQuantityTotal());
        assertEquals("NORMAL", vo.getResourceStatus());
        assertEquals("正常", vo.getResourceStatusReason());
        assertEquals(0, vo.getProcessHourlyCapacityTotal().compareTo(new BigDecimal("10")));
        assertEquals(0, vo.getProcessShiftCapacityTotal().compareTo(new BigDecimal("80")));
    }

    @Test
    void getRouteProcessListByRoute_failsFastWhenMachineryMasterMissing() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(6L).routeId(14L).processId(106L).workstationId(204L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(14L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(106L).code("P106").name("缺设备").build()));
        when(workstationService.getWorkstationList(any()))
                .thenReturn(List.of(MesMdWorkstationDO.builder().id(204L).processId(106L)
                        .code("WS-D").name("缺设备工位").build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder().id(304L)
                        .workstationId(204L).machineryId(999L).quantity(1).build()));
        when(machineryService.getMachineryMap(any())).thenReturn(Map.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> controller.getRouteProcessListByRoute(14L, null));
        assertTrue(exception.getMessage().contains("workstationMachineId=304"));
        assertTrue(exception.getMessage().contains("machineryId=999"));
    }

    @Test
    void getRouteProcessListByRoute_failsFastWhenMachineryQuantityMissing() {
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(7L).routeId(15L).processId(107L).workstationId(205L).sort(1).build();
        when(routeProcessService.getRouteProcessListByRouteId(15L)).thenReturn(List.of(routeProcess));
        when(processService.getProcessList(any()))
                .thenReturn(List.of(MesProProcessDO.builder().id(107L).code("P107").name("缺数量").build()));
        when(workstationService.getWorkstationList(any()))
                .thenReturn(List.of(MesMdWorkstationDO.builder().id(205L).processId(107L)
                        .code("WS-E").name("缺数量工位").build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder().id(305L)
                        .workstationId(205L).machineryId(505L).quantity(null).build()));
        when(machineryService.getMachineryMap(any())).thenReturn(Map.of(505L,
                MesDvMachineryDO.builder().id(505L).code("M-E").name("设备 E").build()));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> controller.getRouteProcessListByRoute(15L, null));
        assertTrue(exception.getMessage().contains("workstationMachineId=305"));
        assertTrue(exception.getMessage().contains("machineryId=505"));
    }

    private MesProBatchRecordReportDO report(String reportId, String reportCode, String reportName) {
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setReportId(reportId);
        report.setReportCode(reportCode);
        report.setReportName(reportName);
        return report;
    }

    private MesProRouteProcessDO routeProcess(Long id, Long routeId, Long processId, Integer sort) {
        return MesProRouteProcessDO.builder()
                .id(id)
                .routeId(routeId)
                .processId(processId)
                .sort(sort)
                .build();
    }

    private MesProProcessDO process(Long id, String code, String name) {
        return MesProProcessDO.builder()
                .id(id)
                .code(code)
                .name(name)
                .build();
    }

    private MesProRouteProcessFlowEdgeDO flowEdge(Long routeId, Long source, Long target) {
        return MesProRouteProcessFlowEdgeDO.builder()
                .routeId(routeId)
                .sourceRouteProcessId(source)
                .targetRouteProcessId(target)
                .relationType("NORMAL")
                .build();
    }
}
