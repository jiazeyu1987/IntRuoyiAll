package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workrecord.MesProWorkRecordDO;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.pro.workrecord.MesProWorkRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_EFFECTIVE_HOURS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_WORKSTATION_SHIFT_HOURS_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesMdWorkstationCapacityServiceTest {

    @Mock
    private MesMdWorkstationWorkerService workstationWorkerService;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesDvMachineryProcessService machineryProcessService;
    @Mock
    private MesDvMachineryService machineryService;
    @Mock
    private MesProWorkRecordService workRecordService;
    @InjectMocks
    private MesMdWorkstationCapacityServiceImpl capacityService;

    @Test
    void getCapacityMetrics_withMachineryBindings_usesMachineryFormulaOnly() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(1L).processId(101L).singleStandardHourlyCapacity(new BigDecimal("9.5")).build();
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder().workstationId(1L).quantity(3).build()));
        when(workRecordService.getClockInWorkRecordListByWorkstationIds(any()))
                .thenReturn(List.of(
                        MesProWorkRecordDO.builder().workstationId(1L).build(),
                        MesProWorkRecordDO.builder().workstationId(1L).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder().workstationId(1L).machineryId(11L).quantity(2).build()));
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(any(), any()))
                .thenReturn(List.of(MesDvMachineryProcessDO.builder()
                        .machineryId(11L).processId(101L)
                        .standardHourlyCapacity(new BigDecimal("20")).build()));
        when(machineryService.getMachineryMap(any()))
                .thenReturn(Map.of(11L, MesDvMachineryDO.builder().id(11L)
                        .standardHourlyCapacity(new BigDecimal("99")).build()));

        MesMdWorkstationCapacityMetrics metrics = capacityService
                .getCapacityMetrics(List.of(workstation), new BigDecimal("8")).get(1L);

        assertEquals(3, metrics.getConfiguredWorkerCount());
        assertEquals(2, metrics.getCurrentWorkerCount());
        assertEquals(0, metrics.getMachineryStandardHourlyCapacity().compareTo(new BigDecimal("40")));
        assertEquals(0, metrics.getTodayCapacity().compareTo(new BigDecimal("320")));
    }

    @Test
    void getCapacityMetrics_withoutMachineryBindings_usesManualWorkstationCapacity() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(2L).singleStandardHourlyCapacity(new BigDecimal("12.5")).build();
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder().workstationId(2L).quantity(4).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of());
        when(workRecordService.getClockInWorkRecordListByWorkstationIds(any()))
                .thenReturn(List.of(
                        MesProWorkRecordDO.builder().workstationId(2L).build(),
                        MesProWorkRecordDO.builder().workstationId(2L).build(),
                        MesProWorkRecordDO.builder().workstationId(2L).build()));
        when(machineryService.getMachineryMap(any())).thenReturn(Map.of());

        MesMdWorkstationCapacityMetrics metrics = capacityService
                .getCapacityMetrics(List.of(workstation), new BigDecimal("6")).get(2L);

        assertEquals(4, metrics.getConfiguredWorkerCount());
        assertEquals(3, metrics.getCurrentWorkerCount());
        assertEquals(0, metrics.getMachineryStandardHourlyCapacity().compareTo(BigDecimal.ZERO));
        assertEquals(0, metrics.getTodayCapacity().compareTo(new BigDecimal("75.0")));
    }

    @Test
    void getCapacityMetrics_withMachineryBindingsButNoCapacity_doesNotFallbackToWorkerCapacity() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(3L).singleStandardHourlyCapacity(new BigDecimal("15")).build();
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder().workstationId(3L).quantity(2).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder().workstationId(3L).machineryId(31L).quantity(1).build()));
        when(workRecordService.getClockInWorkRecordListByWorkstationIds(any()))
                .thenReturn(List.of(MesProWorkRecordDO.builder().workstationId(3L).build()));
        when(machineryService.getMachineryMap(any()))
                .thenReturn(Map.of(31L, MesDvMachineryDO.builder().id(31L).standardHourlyCapacity(null).build()));

        MesMdWorkstationCapacityMetrics metrics = capacityService
                .getCapacityMetrics(List.of(workstation), new BigDecimal("8")).get(3L);

        assertEquals(1, metrics.getCurrentWorkerCount());
        assertEquals(0, metrics.getMachineryStandardHourlyCapacity().compareTo(BigDecimal.ZERO));
        assertEquals(0, metrics.getTodayCapacity().compareTo(BigDecimal.ZERO));
    }

    @Test
    void getCapacityMetrics_withMachineryBindingsButNoProcessCapacity_doesNotFallbackToWorkerCapacity() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(6L).processId(106L).singleStandardHourlyCapacity(new BigDecimal("15")).build();
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder().workstationId(6L).quantity(2).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder().workstationId(6L).machineryId(61L).quantity(3).build()));
        when(workRecordService.getClockInWorkRecordListByWorkstationIds(any())).thenReturn(List.of());
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(any(), any()))
                .thenReturn(List.of());
        when(machineryService.getMachineryMap(any()))
                .thenReturn(Map.of(61L, MesDvMachineryDO.builder().id(61L)
                        .standardHourlyCapacity(new BigDecimal("77")).build()));

        MesMdWorkstationCapacityMetrics metrics = capacityService
                .getCapacityMetrics(List.of(workstation), new BigDecimal("8")).get(6L);

        assertEquals(0, metrics.getMachineryStandardHourlyCapacity().compareTo(BigDecimal.ZERO));
        assertEquals(0, metrics.getTodayCapacity().compareTo(BigDecimal.ZERO));
    }

    @Test
    void getCapacityMetrics_currentWorkerCount_onlyCountsClockInSnapshots() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(4L).singleStandardHourlyCapacity(new BigDecimal("10")).build();
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any())).thenReturn(List.of());
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(List.of());
        when(workRecordService.getClockInWorkRecordListByWorkstationIds(any()))
                .thenReturn(List.of(
                        MesProWorkRecordDO.builder().workstationId(4L).build(),
                        MesProWorkRecordDO.builder().workstationId(4L).build()));
        when(machineryService.getMachineryMap(any())).thenReturn(Map.of());

        MesMdWorkstationCapacityMetrics metrics = capacityService
                .getCapacityMetrics(List.of(workstation), new BigDecimal("8")).get(4L);

        assertEquals(2, metrics.getCurrentWorkerCount());
        assertEquals(0, metrics.getTodayCapacity().compareTo(new BigDecimal("80")));
    }

    @Test
    void getCapacityMetrics_withoutClockInUsesConfiguredWorkersForWorkerBudget() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(5L).singleStandardHourlyCapacity(new BigDecimal("10")).build();
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder().workstationId(5L).quantity(5).build()));
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any())).thenReturn(List.of());
        when(workRecordService.getClockInWorkRecordListByWorkstationIds(any())).thenReturn(List.of());
        when(machineryService.getMachineryMap(any())).thenReturn(Map.of());

        MesMdWorkstationCapacityMetrics metrics = capacityService
                .getCapacityMetrics(List.of(workstation), new BigDecimal("8")).get(5L);

        assertEquals(5, metrics.getConfiguredWorkerCount());
        assertEquals(0, metrics.getCurrentWorkerCount());
        assertEquals(0, metrics.getTodayCapacity().compareTo(new BigDecimal("80")));
    }

    @Test
    void getCapacityMetrics_withoutEffectiveHours_failsFastInsteadOfDefaultingEightHours() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(7L).singleStandardHourlyCapacity(new BigDecimal("10")).build();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> capacityService.getCapacityMetrics(List.of(workstation), null));

        assertEquals(MD_WORKSTATION_EFFECTIVE_HOURS_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void getCapacityMetricsUsingShiftHours_withMachineryBindings_usesWorkbenchShiftHours() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(8L)
                .processId(108L)
                .shiftHours(new BigDecimal("7.5"))
                .singleStandardHourlyCapacity(new BigDecimal("9.5"))
                .build();
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationWorkerDO.builder().workstationId(8L).quantity(3).build()));
        when(workRecordService.getClockInWorkRecordListByWorkstationIds(any()))
                .thenReturn(List.of());
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder().workstationId(8L).machineryId(81L).quantity(2).build()));
        when(machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(any(), any()))
                .thenReturn(List.of(MesDvMachineryProcessDO.builder()
                        .machineryId(81L).processId(108L)
                        .standardHourlyCapacity(new BigDecimal("20")).build()));
        when(machineryService.getMachineryMap(any()))
                .thenReturn(Map.of(81L, MesDvMachineryDO.builder().id(81L)
                        .standardHourlyCapacity(new BigDecimal("99")).build()));

        MesMdWorkstationCapacityMetrics metrics = capacityService
                .getCapacityMetricsUsingShiftHours(List.of(workstation)).get(8L);

        assertEquals(0, metrics.getMachineryStandardHourlyCapacity().compareTo(new BigDecimal("40")));
        assertEquals(0, metrics.getTodayCapacity().compareTo(new BigDecimal("300.0")));
    }

    @Test
    void getCapacityMetricsUsingShiftHours_withZeroProcessIdDoesNotFallbackToWorkerCapacity() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(10L)
                .processId(0L)
                .shiftHours(new BigDecimal("7.5"))
                .singleStandardHourlyCapacity(new BigDecimal("9.5"))
                .build();
        when(workstationWorkerService.getWorkstationWorkerListByWorkstationIds(any()))
                .thenReturn(List.of());
        when(workRecordService.getClockInWorkRecordListByWorkstationIds(any()))
                .thenReturn(List.of());
        when(workstationMachineService.getWorkstationMachineListByWorkstationIds(any()))
                .thenReturn(List.of(MesMdWorkstationMachineDO.builder()
                        .workstationId(10L).machineryId(101L).quantity(1).build()));
        when(machineryService.getMachineryMap(any()))
                .thenReturn(Map.of(101L, MesDvMachineryDO.builder().id(101L).build()));

        MesMdWorkstationCapacityMetrics metrics = capacityService
                .getCapacityMetricsUsingShiftHours(List.of(workstation)).get(10L);

        assertEquals(0, metrics.getMachineryStandardHourlyCapacity().compareTo(BigDecimal.ZERO));
        assertEquals(0, metrics.getTodayCapacity().compareTo(BigDecimal.ZERO));
        verifyNoInteractions(machineryProcessService);
    }

    @Test
    void getCapacityMetricsUsingShiftHours_withoutWorkbenchShiftHours_failsFast() {
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(9L)
                .singleStandardHourlyCapacity(new BigDecimal("10"))
                .build();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> capacityService.getCapacityMetricsUsingShiftHours(List.of(workstation)));

        assertEquals(MD_WORKSTATION_SHIFT_HOURS_REQUIRED.getCode(), exception.getCode());
    }
}
