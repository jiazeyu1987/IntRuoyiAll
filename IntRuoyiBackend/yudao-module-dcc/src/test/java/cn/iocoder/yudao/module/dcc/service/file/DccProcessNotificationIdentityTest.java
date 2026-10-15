package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccProcessNotificationIdentityTest {
    @Test void actualApprovalStarterHasAnExplicitNumberAndVersionAndNoUnfilledTemplateToken() {
        var workflow=new DccControlledFileWorkflowServiceImpl();
        var api=mock(BpmProcessInstanceApi.class);
        var resolver=mock(DccControlledFileApprovalRouteAssigneeResolver.class);
        ReflectionTestUtils.setField(workflow,"bpmProcessInstanceApi",api);
        ReflectionTestUtils.setField(workflow,"approvalRouteAssigneeResolver",resolver);
        when(api.createProcessInstance(anyLong(),any())).thenReturn("actual-round");
        var file=DccControlledFileDO.builder().id(10L).fileNumber("SOP-测试").versionNo("A/2").needTraining(false).build();
        var route=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(null,List.of());
        ReflectionTestUtils.invokeMethod(workflow,"createApprovalProcess",99L,file,route,DccControlledFileProcessDefinitionKeys.REVISION);
        var sent=ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(api).createProcessInstance(eq(99L),sent.capture());
        assertEquals("DCC受控文件升版审批 SOP-测试 / A/2",sent.getValue().getName());
        assertEquals("SOP-测试",sent.getValue().getVariables().get("fileCode"));
        assertEquals("10",sent.getValue().getBusinessKey());
    }
}
