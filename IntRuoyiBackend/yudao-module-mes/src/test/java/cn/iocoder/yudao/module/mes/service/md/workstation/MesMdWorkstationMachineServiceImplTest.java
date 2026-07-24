package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.machine.MesMdWorkstationMachineSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Import(MesMdWorkstationMachineServiceImpl.class)
class MesMdWorkstationMachineServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesMdWorkstationMachineServiceImpl workstationMachineService;

    @MockitoBean
    private MesMdWorkstationService workstationService;
    @MockitoBean
    private cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService machineryService;

    @Test
    void createWorkstationMachine_allowsSameMachineryAcrossDifferentWorkstations() {
        doNothing().when(machineryService).validateMachineryExists(9001L);
        when(workstationService.validateWorkstationExists(1001L))
                .thenReturn(MesMdWorkstationDO.builder().id(1001L).name("WS-1001").build());
        when(workstationService.validateWorkstationExists(1002L))
                .thenReturn(MesMdWorkstationDO.builder().id(1002L).name("WS-1002").build());

        MesMdWorkstationMachineSaveReqVO first = new MesMdWorkstationMachineSaveReqVO();
        first.setWorkstationId(1001L);
        first.setMachineryId(9001L);
        first.setQuantity(1);
        workstationMachineService.createWorkstationMachine(first);

        MesMdWorkstationMachineSaveReqVO second = new MesMdWorkstationMachineSaveReqVO();
        second.setWorkstationId(1002L);
        second.setMachineryId(9001L);
        second.setQuantity(1);

        assertDoesNotThrow(() -> workstationMachineService.createWorkstationMachine(second));
    }

    @Test
    void createWorkstationMachine_rejectsDuplicateWorkstationAndMachinery() {
        doNothing().when(machineryService).validateMachineryExists(9002L);
        when(workstationService.validateWorkstationExists(1003L))
                .thenReturn(MesMdWorkstationDO.builder().id(1003L).name("WS-1003").build());

        MesMdWorkstationMachineSaveReqVO reqVO = new MesMdWorkstationMachineSaveReqVO();
        reqVO.setWorkstationId(1003L);
        reqVO.setMachineryId(9002L);
        reqVO.setQuantity(1);
        workstationMachineService.createWorkstationMachine(reqVO);

        assertThrows(RuntimeException.class, () -> workstationMachineService.createWorkstationMachine(reqVO));
    }
}
