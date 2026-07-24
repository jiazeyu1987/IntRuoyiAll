package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.repair.MesDvRepairMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProScheduleResourceAdjustmentService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.MesProRouteProcessController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteProcessControllerTest {

    @InjectMocks
    private MesProRouteProcessController controller;

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

    @Test
    void getRouteProcessListByRoute_shouldReturnBusinessErrorWhenFiniteScheduleCapacityIsZero() {
        Long routeId = 10L;
        Long routeProcessId = 200L;
        Long processId = 300L;
        when(routeProcessService.getRouteProcessListByRouteId(routeId)).thenReturn(List.of(
                MesProRouteProcessDO.builder()
                        .id(routeProcessId)
                        .routeId(routeId)
                        .processId(processId)
                        .sort(1)
                        .build()));
        when(processService.getProcessList(anyList())).thenReturn(List.of(
                MesProProcessDO.builder().id(processId).code("B010").name("吹球囊成型").build()));
        when(workstationService.getWorkstationListByProcessIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(batchRecordReportMapper.selectListByReportIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(scheduleResourceAdjustmentService.getAdjustmentList(org.mockito.ArgumentMatchers.eq(routeId),
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(
                MesProRouteVersionDO.builder().id(100L).routeId(routeId).active(Boolean.TRUE).build());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(100L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder()
                        .id(700L)
                        .routeVersionId(100L)
                        .routeProcessId(routeProcessId)
                        .capacityMode("FINITE_HOURLY")
                        .hourlyCapacity(BigDecimal.ZERO)
                        .build()));
        when(routeProcessFlowEdgeMapper.selectListByRouteId(routeId)).thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> controller.getRouteProcessListByRoute(routeId, null));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_SCHEDULE_HOURLY_CAPACITY_REQUIRED.getCode(), ex.getCode());
    }
}
