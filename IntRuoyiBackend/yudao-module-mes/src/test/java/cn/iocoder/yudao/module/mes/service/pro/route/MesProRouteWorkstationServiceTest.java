package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.machine.MesMdWorkstationMachineSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
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

    private MesProProcessDO process() {
        return MesProProcessDO.builder().id(8L).name("粗洗").status(0).build();
    }
}
