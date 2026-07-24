package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourcePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource.MesProRouteResourceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationWorkerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteResourceServiceImplTest {

    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdItemService itemService;
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
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Mock
    private MesMdWorkstationWorkerMapper workstationWorkerMapper;
    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @InjectMocks
    private MesProRouteResourceServiceImpl routeResourceService;

    @Test
    void getResourcePage_buildsMachineAndWorkerRowsFromExistingTables() {
        MesProRouteResourcePageReqVO reqVO = new MesProRouteResourcePageReqVO();
        reqVO.setPageSize(20);
        when(routeProductMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                MesProRouteProductDO.builder().id(1001L).routeId(1L).itemId(11L).build()));
        when(routeService.getRouteMap(List.of(1L))).thenReturn(Map.of(1L,
                MesProRouteDO.builder().id(1L).code("R-001").name("球囊扩张导管").build()));
        when(itemService.getItemMap(List.of(11L))).thenReturn(Map.of(11L,
                MesMdItemDO.builder().id(11L).code("P-001").name("PTCA球囊扩张导管").build()));
        when(routeProcessService.getRouteProcessListByRouteIds(List.of(1L))).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(201L).routeId(1L).processId(101L).sort(1).build(),
                MesProRouteProcessDO.builder().id(202L).routeId(1L).processId(102L).sort(2).build()));
        when(processService.getProcessMap(List.of(101L, 102L))).thenReturn(Map.of(
                101L, MesProProcessDO.builder().id(101L).code("P101").name("吹球囊成型").build(),
                102L, MesProProcessDO.builder().id(102L).code("P102").name("穿显影环").build()));
        when(workstationService.getWorkstationListByProcessIds(List.of(101L, 102L))).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(301L).code("WS-1").name("吹球囊成型").processId(101L)
                        .shiftHours(new BigDecimal("8")).build(),
                MesMdWorkstationDO.builder().id(302L).code("WS-2").name("穿显影环").processId(102L)
                        .singleStandardHourlyCapacity(new BigDecimal("70.48"))
                        .shiftHours(new BigDecimal("7.50")).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(List.of(301L, 302L))).thenReturn(List.of(
                MesMdWorkstationMachineDO.builder().id(401L).workstationId(301L).machineryId(501L).quantity(2).build()));
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(List.of(301L, 302L))).thenReturn(List.of(
                MesMdWorkstationWorkerDO.builder().id(601L).workstationId(302L).quantity(5).build()));
        when(machineryService.getMachineryMap(List.of(501L))).thenReturn(Map.of(501L,
                MesDvMachineryDO.builder().id(501L).code("M-1").name("设备一").build()));
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                List.of(501L), List.of(101L, 102L))).thenReturn(List.of(
                MesDvMachineryProcessDO.builder().id(701L).machineryId(501L).processId(101L)
                        .standardHourlyCapacity(new BigDecimal("20")).build()));

        PageResult<MesProRouteResourceRespVO> page = routeResourceService.getResourcePage(reqVO);

        assertEquals(2L, page.getTotal());
        MesProRouteResourceRespVO machineRow = page.getList().get(0);
        assertEquals("MACHINE", machineRow.getResourceType());
        assertEquals("P-001", machineRow.getProductCode());
        assertEquals("R-001", machineRow.getRouteCode());
        assertEquals("P101", machineRow.getProcessCode());
        assertEquals("M-1", machineRow.getMachineryCode());
        assertEquals(2, machineRow.getMachineryQuantity());
        assertEquals(0, machineRow.getMachineryStandardHourlyCapacity().compareTo(new BigDecimal("20")));
        assertEquals(0, machineRow.getBudgetHourlyCapacity().compareTo(new BigDecimal("40")));
        assertEquals(0, machineRow.getBudgetDailyCapacity().compareTo(new BigDecimal("320")));

        MesProRouteResourceRespVO workerRow = page.getList().get(1);
        assertEquals("WORKER", workerRow.getResourceType());
        assertEquals("P102", workerRow.getProcessCode());
        assertEquals(5, workerRow.getWorkerQuantity());
        assertEquals(0, workerRow.getSingleStandardHourlyCapacity().compareTo(new BigDecimal("70.48")));
        assertEquals(0, workerRow.getBudgetHourlyCapacity().compareTo(new BigDecimal("70.48")));
        assertEquals(0, workerRow.getBudgetDailyCapacity().compareTo(new BigDecimal("528.600")));
    }

    @Test
    void getResourcePage_doesNotFallbackToMachineryCapacityWhenMachineProcessMissing() {
        MesProRouteResourcePageReqVO reqVO = new MesProRouteResourcePageReqVO();
        reqVO.setPageSize(20);
        when(routeProductMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                MesProRouteProductDO.builder().id(1001L).routeId(1L).itemId(11L).build()));
        when(routeService.getRouteMap(List.of(1L))).thenReturn(Map.of(1L,
                MesProRouteDO.builder().id(1L).code("R-001").name("球囊扩张导管").build()));
        when(itemService.getItemMap(List.of(11L))).thenReturn(Map.of(11L,
                MesMdItemDO.builder().id(11L).code("P-001").name("PTCA球囊扩张导管").build()));
        when(routeProcessService.getRouteProcessListByRouteIds(List.of(1L))).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(201L).routeId(1L).processId(101L).sort(1).build()));
        when(processService.getProcessMap(List.of(101L))).thenReturn(Map.of(101L,
                MesProProcessDO.builder().id(101L).code("P101").name("吹球囊成型").build()));
        when(workstationService.getWorkstationListByProcessIds(List.of(101L))).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(301L).code("WS-1").name("吹球囊成型").processId(101L).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(List.of(301L))).thenReturn(List.of(
                MesMdWorkstationMachineDO.builder().id(401L).workstationId(301L).machineryId(501L).quantity(2).build()));
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(List.of(301L))).thenReturn(List.of());
        when(machineryService.getMachineryMap(List.of(501L))).thenReturn(Map.of(501L,
                MesDvMachineryDO.builder().id(501L).code("M-1").name("设备一")
                        .standardHourlyCapacity(new BigDecimal("99")).build()));
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                List.of(501L), List.of(101L))).thenReturn(List.of());

        PageResult<MesProRouteResourceRespVO> page = routeResourceService.getResourcePage(reqVO);

        MesProRouteResourceRespVO row = page.getList().get(0);
        assertEquals("MACHINE", row.getResourceType());
        assertNull(row.getMachineryStandardHourlyCapacity());
        assertEquals(0, row.getBudgetHourlyCapacity().compareTo(BigDecimal.ZERO));
        assertNull(row.getBudgetDailyCapacity());
        assertEquals("设备工序产能缺失", row.getCapacitySource());
    }
}
