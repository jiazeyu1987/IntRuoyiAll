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
    void supervisorWithoutHomeDepartmentRoundTripsMultipleResponsiblePaths() throws Exception {
        var controller = new UserController();
        var service = mock(AdminUserService.class);
        var departments = mock(DeptService.class);
        ReflectionTestUtils.setField(controller, "userService", service);
        ReflectionTestUtils.setField(controller, "deptService", departments);
        var user = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
        user.setId(900L);
        user.setUsername("xujianhai");
        user.setNickname("徐建海");
        when(service.getUserPage(any())).thenReturn(new PageResult<>(List.of(user), 1L));
        when(departments.getDeptMap(any())).thenReturn(java.util.Map.of());
        when(departments.getDeptList(any(cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO.class)))
                .thenReturn(List.of(
                        new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO()
                                .setId(1L).setName("瑛泰医疗").setParentId(0L),
                        new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO()
                                .setId(7L).setName("质量体系中心").setParentId(1L).setLeaderUserId(900L),
                        new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO()
                                .setId(8L).setName("QA").setParentId(7L).setLeaderUserId(900L)));
        var response = new MockHttpServletResponse();
        controller.exportUserListPost(new UserPageReqVO(), response);
        var file = new org.springframework.mock.web.MockMultipartFile("file", response.getContentAsByteArray());
        var preview = controller.previewImport(file).getData();
        assertTrue(preview.getOrganizations().contains(
                "部门负责人：瑛泰医疗 / 质量体系中心 → xujianhai（该账号成功导入后恢复）"));
        controller.importExcel(file, true);
        verify(service).importUserList(argThat(rows -> rows.size() == 1
                && rows.get(0).getDeptId() == null && rows.get(0).getDeptPath() == null
                && cn.iocoder.yudao.module.system.service.user.UserDepartmentPath
                        .parseLeaderPaths(rows.get(0).getLeaderDeptPaths()).equals(List.of(
                                "瑛泰医疗 / 质量体系中心", "瑛泰医疗 / 质量体系中心 / QA"))), eq(true));
    }

    @Test
    void previewDoesNotResolveOrWriteTargetDepartments() throws Exception {
        var controller = new UserController();
        var service = mock(AdminUserService.class);
        var departments = mock(DeptService.class);
        ReflectionTestUtils.setField(controller, "userService", service);
        ReflectionTestUtils.setField(controller, "deptService", departments);
        var output = new java.io.ByteArrayOutputStream();
        cn.idev.excel.FastExcelFactory.write(output).sheet("数据").doWrite(List.of(
                List.of("用户名称", "用户昵称", "部门名称", "部门路径"),
                List.of("xujianhai", "徐建海", "质量体系中心", "瑛泰医疗 / 质量体系中心")));
        var file = new org.springframework.mock.web.MockMultipartFile("file", output.toByteArray());
        assertEquals(List.of("部门路径：瑛泰医疗 / 质量体系中心（按层级匹配或创建）"),
                controller.previewImport(file).getData().getOrganizations());
        verifyNoInteractions(service, departments);
    }

    @Test
    void exported1081UsersCanBeImportedUnchangedWithoutUpdatingExistingUsers() throws Exception {
        UserController controller = new UserController();
        AdminUserService service = mock(AdminUserService.class);
        DeptService departments = mock(DeptService.class);
        ReflectionTestUtils.setField(controller, "userService", service);
        ReflectionTestUtils.setField(controller, "deptService", departments);
        var users = java.util.stream.IntStream.range(0, 1081).mapToObj(index -> {
            var user = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
            user.setId((long) index + 1);
            user.setDeptId(7L);
            user.setUsername(index == 0 ? "yingtaigroupadmin" : index == 2 ? "xujianhai" : "operator" + index);
            user.setNickname("姓名" + index);
            user.setEmail("operator" + index + "@example.com");
            user.setSex(1);
            user.setStatus(index % 2);
            return user;
        }).toList();
        when(service.getUserPage(any())).thenReturn(new PageResult<>(users, 1081L));
        var sourceDept = new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO();
        sourceDept.setId(7L);
        sourceDept.setName("生产部");
        sourceDept.setParentId(1L);
        when(departments.getDept(1L)).thenReturn(new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO()
                .setId(1L).setName("瑛泰医疗").setParentId(0L));
        when(departments.getDeptMap(any())).thenReturn(java.util.Map.of(7L, sourceDept));
        try (var dict = mockStatic(cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils.class)) {
            dict.when(() -> cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils.parseDictDataLabel("system_user_sex", "1")).thenReturn("男");
            dict.when(() -> cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils.parseDictDataLabel("common_status", "0")).thenReturn("开启");
            dict.when(() -> cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils.parseDictDataLabel("common_status", "1")).thenReturn("关闭");
            dict.when(() -> cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils.parseDictDataValue("system_user_sex", "男")).thenReturn("1");
            dict.when(() -> cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils.parseDictDataValue("common_status", "开启")).thenReturn("0");
            dict.when(() -> cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils.parseDictDataValue("common_status", "关闭")).thenReturn("1");
            var response = new MockHttpServletResponse();
            controller.exportUserListPost(new UserPageReqVO(), response);
            var file = new org.springframework.mock.web.MockMultipartFile("file", "用户数据.xlsx",
                    "application/octet-stream", response.getContentAsByteArray());
            var preview = controller.previewImport(file).getData();
            assertTrue(preview.getPersonnel().contains("xujianhai / 姓名2"));
            assertEquals(List.of("部门路径：瑛泰医疗 / 生产部（按层级匹配或创建）"), preview.getOrganizations());
            assertEquals(1081, preview.getRowCount());
            assertEquals("yingtaigroupadmin / 姓名0", preview.getPersonnel().get(0));
            controller.importExcel(file, false);
            verify(service).importUserList(argThat(rows -> rows.size() == 1081
                    && java.util.stream.IntStream.range(0, rows.size()).allMatch(index -> {
                        var row = rows.get(index);
                        var user = users.get(index);
                        return user.getUsername().equals(row.getUsername())
                                && user.getNickname().equals(row.getNickname())
                                && user.getEmail().equals(row.getEmail())
                                && user.getSex().equals(row.getSex())
                                && user.getStatus().equals(row.getStatus())
                                && row.getDeptId() == null
                                && "瑛泰医疗 / 生产部".equals(row.getDeptPath());
                    })), eq(false));
        }
    }

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
