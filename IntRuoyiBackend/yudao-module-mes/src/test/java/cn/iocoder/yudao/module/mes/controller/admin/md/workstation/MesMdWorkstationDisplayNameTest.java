package cn.iocoder.yudao.module.mes.controller.admin.md.workstation;

import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesMdWorkstationDisplayNameTest {

    @Test
    void displaysBusinessNameOnlyForTheExactGeneratedNameAndRealAssociation() {
        var workstation = generatedWorkstation();
        assertEquals("中包装2号工序工作站", display(workstation, binding()));
        assertEquals("中包装2号工序工作站-2121453182031056991-14", workstation.getName());
    }

    @Test
    void preservesCustomNamesIncludingTheirLegalNumericSuffix() {
        var workstation = generatedWorkstation();
        workstation.setName("包装工作站-2026-14");
        assertEquals("包装工作站-2026-14", display(workstation, binding()));
    }

    @Test
    void doesNotInferFromANameOrCodeWithoutMatchingStructuredAssociation() {
        var workstation = generatedWorkstation();
        assertEquals(workstation.getName(), MesMdWorkstationController.resolveDisplayName(workstation, List.of()));
        var wrongProcess = binding();
        wrongProcess.setProcessId(999L);
        assertEquals(workstation.getName(), display(workstation, wrongProcess));
        var wrongWorkstation = binding();
        wrongWorkstation.setWorkstationId(999L);
        assertEquals(workstation.getName(), display(workstation, wrongWorkstation));
        var wrongRoute = binding();
        wrongRoute.setRouteId(999L);
        assertEquals(workstation.getName(), display(workstation, wrongRoute));
    }

    @Test
    void preservesTheNameWhenTheBusinessNameCannotBeResolvedOrWasEdited() {
        var workstation = generatedWorkstation();
        workstation.setProcessName(null);
        assertEquals(workstation.getName(), display(workstation, binding()));
        workstation.setProcessName("包装工序（已改名）");
        assertEquals(workstation.getName(), display(workstation, binding()));
    }

    @Test
    void doesNotTreatAVersionNumberAsARouteAssociation() {
        var workstation = generatedWorkstation();
        workstation.setCode("WSV-2121453182031056991-14");
        assertEquals(workstation.getName(), display(workstation, binding()));
    }

    private static String display(MesMdWorkstationRespVO workstation, MesProRouteProcessDO binding) {
        return MesMdWorkstationController.resolveDisplayName(workstation, List.of(binding));
    }

    private static MesMdWorkstationRespVO generatedWorkstation() {
        var workstation = new MesMdWorkstationRespVO();
        workstation.setId(101L);
        workstation.setProcessId(201L);
        workstation.setProcessName("中包装2号工序");
        workstation.setCode("WS-2121453182031056991-14");
        workstation.setName("中包装2号工序工作站-2121453182031056991-14");
        return workstation;
    }

    private static MesProRouteProcessDO binding() {
        return MesProRouteProcessDO.builder().workstationId(101L).processId(201L)
                .routeId(2121453182031056991L).sort(14).build();
    }
}
