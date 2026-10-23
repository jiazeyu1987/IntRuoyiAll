package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessRespVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DccRouteDepartmentPreviewHttpTest {
    @Test void selectedLongDepartmentsReachTheFormalPreviewInsteadOfBeingDiscarded() throws Exception {
        var arguments=new AtomicReference<Object[]>();
        var workflow=mock(DccControlledFileWorkflowService.class,invocation->{
            if (invocation.getMethod().getName().equals("previewRoute")) {
                arguments.set(invocation.getArguments());
                return DccControlledFileRouteReadinessRespVO.builder().ready(true).nodes(List.of()).blockers(List.of()).build();
            }
            return null;
        });
        var controller=new DccControlledFileController();
        ReflectionTestUtils.setField(controller,"workflowService",workflow);
        try(var auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(post("/dcc/controlled-files/route-preview")
                    .contentType("application/json").content("{\"categoryId\":10,\"actionType\":\"NEW\",\"selectedSignoffDepartmentIds\":[\"9007199254740993\"]}"))
                    .andExpect(status().isOk());
        }
        assertNotNull(arguments.get());
        assertEquals(5,arguments.get().length,"preview must carry both distinct user and department fields");
        assertEquals(99L,arguments.get()[0]);
        assertEquals(List.of(9007199254740993L),arguments.get()[3]);
        assertEquals("NEW",arguments.get()[4]);
    }
}
