package cn.iocoder.yudao.module.bpm.convert.task;

import cn.iocoder.yudao.module.bpm.controller.admin.base.user.UserSimpleBaseVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.process.BpmProcessDefinitionRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessPrintDataRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BpmProcessInstanceConvertTest {

    @Test
    public void testCopyTo_preservesRuntimeDefinitionFieldsAndMapsMetaInfo() {
        BpmProcessDefinitionInfoDO from = BpmProcessDefinitionInfoDO.builder()
                .id(10L)
                .modelType(21)
                .icon("icon")
                .description("desc")
                .formType(31)
                .formId(41L)
                .formConf("conf")
                .formFields(List.of("field"))
                .visible(false)
                .sort(51L)
                .build();
        BpmProcessDefinitionRespVO to = new BpmProcessDefinitionRespVO();
        to.setId("process-id");
        to.setVersion(3);
        to.setName("name");
        to.setKey("key");
        to.setCategoryName("category");
        to.setFormName("form");
        to.setSuspensionState(2);
        to.setDeploymentTime(LocalDateTime.of(2025, 1, 1, 0, 0));
        to.setBpmnXml("<xml />");

        BpmProcessInstanceConvert.INSTANCE.copyTo(from, to);

        assertEquals("process-id", to.getId());
        assertEquals(21, to.getType());
        assertEquals(21, to.getModelType());
        assertEquals("icon", to.getIcon());
        assertEquals("desc", to.getDescription());
        assertEquals(31, to.getFormType());
        assertEquals(41L, to.getFormId());
        assertEquals("conf", to.getFormConf());
        assertEquals(List.of("field"), to.getFormFields());
        assertFalse(to.getVisible());
        assertEquals(51L, to.getSort());
        assertEquals(3, to.getVersion());
        assertEquals("name", to.getName());
        assertEquals("key", to.getKey());
        assertEquals("category", to.getCategoryName());
        assertEquals("form", to.getFormName());
        assertEquals(2, to.getSuspensionState());
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), to.getDeploymentTime());
        assertEquals("<xml />", to.getBpmnXml());
    }

    @Test
    public void buildProcessInstancePrintData_marksMissingAssigneeUser() {
        HistoricProcessInstance historicProcessInstance = mock(HistoricProcessInstance.class);
        when(historicProcessInstance.getId()).thenReturn("proc-1");
        when(historicProcessInstance.getName()).thenReturn("流程打印");
        when(historicProcessInstance.getBusinessKey()).thenReturn("BUSINESS-1");
        when(historicProcessInstance.getStartTime()).thenReturn(new Date(1704067200000L));
        when(historicProcessInstance.getEndTime()).thenReturn(new Date(1704070800000L));
        when(historicProcessInstance.getProcessVariables()).thenReturn(Map.of());
        BpmProcessDefinitionInfoDO processDefinitionInfo = BpmProcessDefinitionInfoDO.builder().build();
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getTaskLocalVariables()).thenReturn(Map.of(
                BpmnVariableConstants.TASK_VARIABLE_STATUS, BpmTaskStatusEnum.APPROVE.getStatus(),
                BpmnVariableConstants.TASK_VARIABLE_REASON, "同意",
                BpmnVariableConstants.TASK_SIGN_PIC_URL, "sign.png"));
        when(task.getName()).thenReturn("审核");
        when(task.getId()).thenReturn("task-1");
        when(task.getAssignee()).thenReturn("113");
        when(task.getEndTime()).thenReturn(new Date(1704070800000L));

        BpmProcessPrintDataRespVO result = BpmProcessInstanceConvert.INSTANCE.buildProcessInstancePrintData(
                historicProcessInstance, processDefinitionInfo, List.of(task), Map.of(),
                new UserSimpleBaseVO().setNickname("提交人"));

        assertEquals(1, result.getTasks().size());
        assertTrue(result.getTasks().get(0).getDescription().contains("用户不存在(113) / 审核 /"));
        assertTrue(result.getTasks().get(0).getDescription().contains("审批通过"));
    }

}
