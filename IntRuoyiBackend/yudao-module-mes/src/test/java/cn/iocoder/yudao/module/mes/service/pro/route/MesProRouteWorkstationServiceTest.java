package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.machine.MesMdWorkstationMachineSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationToolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProRouteWorkstationServiceTest {
    @InjectMocks private MesProRouteWorkstationService service;
    @Mock private MesMdWorkshopService workshopService;
    @Mock private MesMdWorkstationService workstationService;
    @Mock private MesMdWorkstationMachineService machineService;
    @Mock private MesMdWorkstationWorkerService workerService;
    @Mock private MesMdWorkstationToolService toolService;
    @Mock private MesDvMachineryProcessMapper machineryProcessMapper;

    @Test void missingUnknownOrDisabledWorkshopPreventsCreation() {
        assertThrows(ServiceException.class, () -> service.requireWorkshop(null));
        assertThrows(ServiceException.class, () -> service.requireWorkshop(70L));
        when(workshopService.getWorkshop(70L)).thenReturn(MesMdWorkshopDO.builder().id(70L).status(1).build());
        assertThrows(ServiceException.class, () -> service.create(500L, 1, process(), 70L));
        verifyNoInteractions(workstationService, machineService, machineryProcessMapper);
    }

    @Test void eachRouteProcessGetsDistinctStationInSelectedWorkshopWithDeduplicatedLedgerDevices() {
        when(workshopService.getWorkshop(70L)).thenReturn(MesMdWorkshopDO.builder().id(70L).status(0).build());
        when(workstationService.createWorkstation(any())).thenReturn(900L, 901L);
        when(machineryProcessMapper.selectListByProcessId(8L)).thenReturn(List.of(
                MesDvMachineryProcessDO.builder().machineryId(91L).build(),
                MesDvMachineryProcessDO.builder().machineryId(91L).build(),
                MesDvMachineryProcessDO.builder().machineryId(92L).build()));
        assertEquals(900L, service.create(500L, 1, process(), 70L));
        assertEquals(901L, service.create(501L, 1, process(), 70L));
        var stations = ArgumentCaptor.forClass(MesMdWorkstationSaveReqVO.class);
        verify(workstationService, times(2)).createWorkstation(stations.capture());
        assertNotEquals(stations.getAllValues().get(0).getCode(), stations.getAllValues().get(1).getCode());
        assertNotEquals(stations.getAllValues().get(0).getName(), stations.getAllValues().get(1).getName());
        stations.getAllValues().forEach(station -> {
            assertEquals(70L, station.getWorkshopId());
            assertEquals(8L, station.getProcessId());
            assertEquals(0, station.getStatus());
            assertTrue(station.getName().contains("粗洗"));
        });
        var machines = ArgumentCaptor.forClass(MesMdWorkstationMachineSaveReqVO.class);
        verify(machineService, times(4)).createWorkstationMachine(machines.capture());
        assertEquals(List.of(91L, 92L, 91L, 92L), machines.getAllValues().stream()
                .map(MesMdWorkstationMachineSaveReqVO::getMachineryId).toList());
        assertEquals(List.of(900L, 900L, 901L, 901L), machines.getAllValues().stream()
                .map(MesMdWorkstationMachineSaveReqVO::getWorkstationId).toList());
    }

    @Test void noDeviceProcessStillCreatesStationAndExistingAssociationIsNotDuplicated() {
        when(workshopService.getWorkshop(70L)).thenReturn(MesMdWorkshopDO.builder().id(70L).status(0).build());
        when(workstationService.createWorkstation(any())).thenReturn(900L);
        assertEquals(900L, service.create(500L, 1, process(), 70L));
        verify(machineService, never()).createWorkstationMachine(any());
        when(machineService.getWorkstationMachineListByWorkstationId(900L)).thenReturn(List.of(
                MesMdWorkstationMachineDO.builder().machineryId(91L).build()));
        service.bindMachinery(900L, List.of(91L, 92L, 92L));
        verify(machineService).createWorkstationMachine(argThat(req -> req.getMachineryId().equals(92L)));
    }

    @Test void malformedAssociationFailsAndResourceCreationRequiresOuterTransaction() throws Exception {
        assertThrows(ServiceException.class, () -> service.bindMachinery(900L, Arrays.asList((Long) null)));
        assertThrows(ServiceException.class, () -> service.bindMachinery(null, List.of()));
        assertEquals(Propagation.MANDATORY, MesProRouteWorkstationService.class
                .getMethod("create", Long.class, int.class, MesProProcessDO.class, Long.class)
                .getAnnotation(Transactional.class).propagation());
        assertEquals(Propagation.MANDATORY, MesProRouteWorkstationService.class
                .getMethod("bindMachinery", Long.class, Collection.class)
                .getAnnotation(Transactional.class).propagation());
        verify(machineService, never()).createWorkstationMachine(any());
    }

    @Test void unchangedProcessKeepsOriginalStationWithoutCopyingResources() {
        when(workstationService.validateWorkstationExists(900L)).thenReturn(
                MesMdWorkstationDO.builder().id(900L).processId(8L).build());
        assertEquals(900L, service.forPublishedProcess(501L, 1, 900L, 8L, "粗洗"));
        verify(workstationService, never()).createWorkstation(any());
        verifyNoInteractions(workshopService, machineService, workerService, toolService);
    }

    @Test void replacedProcessGetsSeparateStationWithOriginalWorkshopAndResources() {
        var original = MesMdWorkstationDO.builder().id(900L).code("WS-500-1").name("粗洗工作站")
                .processId(8L).workshopId(70L).productionLineId(71L).address("一楼")
                .warehouseId(72L).locationId(73L).areaId(74L).status(0).remark("原站")
                .singleStandardHourlyCapacity(new java.math.BigDecimal("12.5"))
                .shiftHours(new java.math.BigDecimal("8")).build();
        when(workstationService.validateWorkstationExists(900L)).thenReturn(original);
        when(workshopService.getWorkshop(70L)).thenReturn(MesMdWorkshopDO.builder().id(70L).status(0).build());
        when(workstationService.createWorkstation(any())).thenReturn(901L);
        when(machineService.getWorkstationMachineListByWorkstationId(900L)).thenReturn(List.of(
                MesMdWorkstationMachineDO.builder().id(11L).workstationId(900L).machineryId(91L).quantity(2).build()));
        when(workerService.getWorkstationWorkerListByWorkstationId(900L)).thenReturn(List.of(
                MesMdWorkstationWorkerDO.builder().id(12L).workstationId(900L).postId(92L).quantity(3).remark("岗位配置").build()));
        when(toolService.getWorkstationToolListByWorkstationId(900L)).thenReturn(List.of(
                MesMdWorkstationToolDO.builder().id(13L).workstationId(900L).toolTypeId(93L).quantity(4).build()));

        assertEquals(901L, service.forPublishedProcess(501L, 1, 900L, 9L, "精洗"));

        var request = ArgumentCaptor.forClass(MesMdWorkstationSaveReqVO.class);
        verify(workstationService).createWorkstation(request.capture());
        var station = request.getValue();
        assertNull(station.getId());
        assertEquals("WSV-501-1", station.getCode());
        assertEquals(9L, station.getProcessId());
        assertEquals(70L, station.getWorkshopId());
        assertEquals(71L, station.getProductionLineId());
        assertEquals("一楼", station.getAddress());
        assertEquals(72L, station.getWarehouseId());
        assertEquals(73L, station.getLocationId());
        assertEquals(74L, station.getAreaId());
        assertEquals(original.getSingleStandardHourlyCapacity(), station.getSingleStandardHourlyCapacity());
        assertEquals(original.getShiftHours(), station.getShiftHours());
        verify(machineService).createWorkstationMachine(argThat(r -> r.getId() == null
                && r.getWorkstationId().equals(901L) && r.getMachineryId().equals(91L) && r.getQuantity() == 2));
        verify(workerService).createWorkstationWorker(argThat(r -> r.getId() == null
                && r.getWorkstationId().equals(901L) && r.getPostId().equals(92L) && r.getQuantity() == 3
                && "岗位配置".equals(r.getRemark())));
        verify(toolService).createWorkstationTool(argThat(r -> r.getId() == null
                && r.getWorkstationId().equals(901L) && r.getToolTypeId().equals(93L) && r.getQuantity() == 4));
        assertEquals(8L, original.getProcessId());
        assertEquals("WS-500-1", original.getCode());
        verify(workstationService, never()).updateWorkstation(any());
    }

    @Test void replacementResourceFailurePropagatesToPublishingTransaction() throws Exception {
        when(workstationService.validateWorkstationExists(900L)).thenReturn(
                MesMdWorkstationDO.builder().id(900L).processId(8L).workshopId(70L).build());
        when(workshopService.getWorkshop(70L)).thenReturn(MesMdWorkshopDO.builder().id(70L).status(0).build());
        when(workstationService.createWorkstation(any())).thenReturn(901L);
        when(machineService.getWorkstationMachineListByWorkstationId(900L)).thenReturn(List.of(
                MesMdWorkstationMachineDO.builder().machineryId(91L).quantity(2).build()));
        var failure = new IllegalStateException("资源保存失败");
        when(machineService.createWorkstationMachine(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.forPublishedProcess(501L, 1, 900L, 9L, "精洗")));
        var transaction = MesProRouteWorkstationService.class
                .getMethod("forPublishedProcess", Long.class, int.class, Long.class, Long.class, String.class)
                .getAnnotation(Transactional.class);
        assertEquals(Propagation.MANDATORY, transaction.propagation());
        assertTrue(Arrays.asList(transaction.rollbackFor()).contains(Exception.class));
        verifyNoInteractions(workerService, toolService);
    }

    private MesProProcessDO process() {
        return MesProProcessDO.builder().id(8L).name("粗洗").status(0).build();
    }
}
