package cn.iocoder.yudao.module.mes.controller.admin.md.workstation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityMetrics;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesMdWorkstationControllerCapacityViewTest {

    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesMdWorkshopService workshopService;
    @Mock
    private MesMdProductionLineService productionLineService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdWorkstationCapacityService workstationCapacityService;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesDvMachineryService machineryService;
    @InjectMocks
    private MesMdWorkstationController controller;

    @Test
    void getWorkstation_returnsSingleStandardHourlyCapacity() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(1L)
                .code("WS-01")
                .name("工位1")
                .singleStandardHourlyCapacity(new BigDecimal("10.5"))
                .build();
        when(workstationService.getWorkstation(1L)).thenReturn(workstation);

        CommonResult<MesMdWorkstationRespVO> response = controller.getWorkstation(1L);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals(0, response.getData().getSingleStandardHourlyCapacity().compareTo(new BigDecimal("10.5")));
    }

    @Test
    void getWorkstationPage_returnsCapacityMetricsUsingShiftHoursSetting() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(2L)
                .code("WS-02")
                .name("工位2")
                .workshopId(11L)
                .processId(22L)
                .shiftHours(new BigDecimal("7.5"))
                .singleStandardHourlyCapacity(new BigDecimal("8"))
                .build();
        MesMdWorkstationPageReqVO reqVO = new MesMdWorkstationPageReqVO();

        when(workstationService.getWorkstationPage(reqVO))
                .thenReturn(new PageResult<>(List.of(workstation), 1L));
        when(workshopService.getWorkshopMap(any()))
                .thenReturn(Map.of(11L, MesMdWorkshopDO.builder().id(11L).name("车间1").build()));
        when(productionLineService.getProductionLineMap(any()))
                .thenReturn(Map.of());
        when(processService.getProcessMap(any()))
                .thenReturn(Map.of(22L, MesProProcessDO.builder().id(22L).name("工序A").build()));
        when(workstationCapacityService.getCapacityMetricsUsingShiftHours(any()))
                .thenReturn(Map.of(2L, MesMdWorkstationCapacityMetrics.builder()
                        .configuredWorkerCount(3)
                        .currentWorkerCount(2)
                        .machineryStandardHourlyCapacity(new BigDecimal("100"))
                        .todayCapacity(new BigDecimal("750"))
                        .build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(
                        MesMdWorkstationMachineDO.builder()
                                .id(301L)
                                .workstationId(2L)
                                .machineryId(401L)
                                .quantity(2)
                                .build(),
                        MesMdWorkstationMachineDO.builder()
                                .id(302L)
                                .workstationId(2L)
                                .machineryId(402L)
                                .quantity(1)
                                .build()));
        when(machineryService.getMachineryMap(any()))
                .thenReturn(Map.of(401L, MesDvMachineryDO.builder()
                        .id(401L)
                        .code("EQ-01")
                        .name("裁切机")
                        .build(),
                        402L, MesDvMachineryDO.builder()
                                .id(402L)
                                .code("EQ-02")
                                .name("封口机")
                                .build()));

        CommonResult<PageResult<MesMdWorkstationRespVO>> response = controller.getWorkstationPage(reqVO);

        assertEquals(0, response.getCode());
        MesMdWorkstationRespVO vo = response.getData().getList().get(0);
        assertEquals("车间1", vo.getWorkshopName());
        assertEquals("工序A", vo.getProcessName());
        assertEquals(3, vo.getConfiguredWorkerCount());
        assertEquals(2, vo.getCurrentWorkerCount());
        assertEquals(2, vo.getMachineryCount());
        assertEquals("EQ-01 / 裁切机 ×2；EQ-02 / 封口机 ×1", vo.getMachinerySummary());
        assertEquals(0, vo.getMachineryStandardHourlyCapacity().compareTo(new BigDecimal("100")));
        assertEquals(0, vo.getTodayCapacity().compareTo(new BigDecimal("750")));
        verify(workstationCapacityService).getCapacityMetricsUsingShiftHours(any());
    }
}
