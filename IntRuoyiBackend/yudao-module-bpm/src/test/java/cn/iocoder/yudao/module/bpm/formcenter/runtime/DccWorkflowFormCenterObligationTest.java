package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DccWorkflowFormCenterObligationTest {
    @Test
    void obsoletePreservesTwoDepartmentObligationsForTheSameLeader() {
        var request = request("[51,52]");
        assertEquals(Map.of("MATRIX_REVIEW",List.of("form-7:MATRIX_REVIEW:51","form-7:MATRIX_REVIEW:52")),
                request.getVariables().get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_DCC_TASK_OBLIGATION_IDS));
        assertEquals(List.of(99L,99L),request.getStartUserSelectAssignees().get("MATRIX_REVIEW"));
    }

    @Test
    void invalidDepartmentRosterCannotStartAnObsoleteWorkflow() {
        assertThrows(RuntimeException.class, () -> request("[51,51]"));
        assertThrows(RuntimeException.class, () -> request("[51]"));
    }

    private BpmProcessInstanceCreateReqDTO request(String ids) {
        var instance = new FormActionInstanceDO();
        instance.setId(7L);
        instance.setInstanceCode("F-7");
        instance.setObjectId("900");instance.setObjectVersion("A/2");
        instance.setBusinessContextJson("{\"systemCode\":\"DCC\",\"objectType\":\"CONTROLLED_FILE\",\"actionCode\":\"OBSOLETE\",\"tenantId\":1}");
        instance.setFormDataJson("{\"controlledFileId\":900,\"fileCode\":\"SOP-1\",\"fileVersion\":\"A/2\",\"dccSignoffDepartmentIds\":"+ids+"}");
        var policy = FormActionPolicy.builder().bpmProcessKey("dcc-controlled-file-obsolete").build();
        return ReflectionTestUtils.invokeMethod(new FormCenterRuntimeServiceImpl(),"buildBpmRequest",
                instance,policy,Map.of("MATRIX_REVIEW",List.of(99L,99L)),Map.of("MATRIX_APPROVAL",List.of(100L)));
    }
    @Test void obsoleteNotificationIdentityComesFromFrozenFileFacts() {
        var result=request("[51,52]");
        assertEquals("DCC受控文件作废审批 SOP-1 / A/2",result.getName());
        assertEquals("SOP-1",result.getVariables().get("fileCode"));
        assertEquals("A/2",result.getVariables().get("fileVersion"));
    }
    @Test void obsoleteApprovalContentGetsExactFrozenControlledFileIdentity() {
        var result = request("[51,52]");
        assertEquals(900L, result.getVariables().get("controlledFileId"));
        assertEquals("900", result.getVariables().get("objectId"));
        assertEquals("A/2", result.getVariables().get("objectVersion"));
    }
    @Test void incompleteOrChangedFrozenIdentityCannotStartAnObsoleteWorkflow() {
        for (String facts : List.of("{\"controlledFileId\":900,\"fileVersion\":\"A/2\"}",
                "{\"controlledFileId\":901,\"fileCode\":\"SOP-1\",\"fileVersion\":\"A/2\"}",
                "{\"controlledFileId\":900,\"fileCode\":\"SOP-1\",\"fileVersion\":\"B/1\"}")) {
            var instance = new FormActionInstanceDO();instance.setId(7L);instance.setInstanceCode("F-7");
            instance.setObjectId("900");instance.setObjectVersion("A/2");
            instance.setBusinessContextJson("{\"systemCode\":\"DCC\",\"tenantId\":1}");
            instance.setFormDataJson(facts);
            assertThrows(IllegalArgumentException.class,()->ReflectionTestUtils.invokeMethod(new FormCenterRuntimeServiceImpl(),
                    "buildBpmRequest",instance,FormActionPolicy.builder().bpmProcessKey("dcc-controlled-file-obsolete").build(),
                    Map.of("MATRIX_REVIEW",List.of(99L)),Map.of("MATRIX_APPROVAL",List.of(100L))));
        }
    }
}
