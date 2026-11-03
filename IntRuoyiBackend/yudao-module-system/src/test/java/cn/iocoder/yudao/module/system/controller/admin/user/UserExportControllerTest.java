package cn.iocoder.yudao.module.system.controller.admin.user;

import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class UserExportControllerTest {
    @Test
    void postJsonPreservesIdsAndFiltersWhileRemovingPagination() throws Exception {
        UserController controller = new UserController();
        AdminUserService service = mock(AdminUserService.class);
        DeptService departments = mock(DeptService.class);
        ReflectionTestUtils.setField(controller, "userService", service);
        ReflectionTestUtils.setField(controller, "deptService", departments);
        when(service.getUserPage(any())).thenReturn(new PageResult<>(List.of(), 0L));
        when(departments.getDeptMap(any())).thenReturn(java.util.Map.of());
        var result = MockMvcBuilders.standaloneSetup(controller).build().perform(post("/system/user/export-excel")
                .contentType("application/json")
                .content("""
                        {"ids":[11,22],"username":"operator","nickname":"姓名","deptId":7,"status":0,"pageNo":2,"pageSize":10}
                        """))
                .andReturn().getResponse();
        assertEquals(200, result.getStatus());
        assertTrue(result.getContentAsByteArray().length > 0);
        verify(service).getUserPage(argThat(request -> request.getIds().equals(List.of(11L,22L))
                && request.getUsername().equals("operator") && request.getNickname().equals("姓名")
                && request.getDeptId().equals(7L) && request.getStatus().equals(0)
                && request.getPageSize() == PageParam.PAGE_SIZE_NONE));
    }

    @Test
    void exportRetainsPermissionAndPropagatesWriteFailure() throws Exception {
        var method = UserController.class.getMethod("exportUserListPost", UserPageReqVO.class,
                jakarta.servlet.http.HttpServletResponse.class);
        assertEquals("@ss.hasPermission('system:user:export')", method.getAnnotation(
                org.springframework.security.access.prepost.PreAuthorize.class).value());
        assertNotNull(method.getAnnotation(cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog.class));
        UserController controller = new UserController();
        AdminUserService service = mock(AdminUserService.class);
        DeptService departments = mock(DeptService.class);
        ReflectionTestUtils.setField(controller, "userService", service);
        ReflectionTestUtils.setField(controller, "deptService", departments);
        when(service.getUserPage(any())).thenReturn(new PageResult<>(List.of(), 0L));
        when(departments.getDeptMap(any())).thenReturn(java.util.Map.of());
        try (var excel = mockStatic(ExcelUtils.class)) {
            excel.when(() -> ExcelUtils.write(any(), anyString(), anyString(), any(), anyList()))
                    .thenThrow(new IOException("output failed"));
            assertThrows(IOException.class, () -> controller.exportUserListPost(new UserPageReqVO(), new MockHttpServletResponse()));
        }
    }
}
