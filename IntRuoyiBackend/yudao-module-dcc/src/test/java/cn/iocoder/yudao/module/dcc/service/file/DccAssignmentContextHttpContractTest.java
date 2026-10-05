package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccWorkflowLifecycleController;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DccAssignmentContextHttpContractTest {
    @Test void actualTaskIdentityAndExactLongObligationDepartmentReachTheFormalService() throws Exception {
        var assignments=mock(DccWorkflowSignoffAssignmentService.class);
        var controller=new DccWorkflowLifecycleController(null,null,null,null,null);
        ReflectionTestUtils.setField(controller,"signoffAssignments",assignments);
        when(assignments.assignmentContext(99L,9007199254740993L,"exact-task")).thenReturn(new DccSignoffAssignmentContext(
                9007199254740993L,"real-round","exact-task","frozen-obligation",DccControlledFileProcessDefinitionKeys.UPLOAD,9007199254740995L,"Quality",99L,false,true,
                List.of(new DccSignoffAssignmentContext.AssigneeOption(9007199254740997L,"Signer"))));
        try(var auth=mockStatic(SecurityFrameworkUtils.class)){
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build()
                    .perform(get("/dcc/controlled-file/workflow-lifecycle/9007199254740993/signoff-assignment-context").param("taskId","exact-task"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.controlledFileId").value("9007199254740993"))
                    .andExpect(jsonPath("$.data.departmentId").value("9007199254740995"))
                    .andExpect(jsonPath("$.data.obligationId").value("frozen-obligation"))
                    .andExpect(jsonPath("$.data.processDefinitionKey").value(DccControlledFileProcessDefinitionKeys.UPLOAD))
                    .andExpect(jsonPath("$.data.assigneeOptions[0].id").value("9007199254740997"));
        }
    }
}
