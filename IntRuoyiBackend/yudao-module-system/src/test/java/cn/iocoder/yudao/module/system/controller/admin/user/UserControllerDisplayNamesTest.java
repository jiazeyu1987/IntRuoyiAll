package cn.iocoder.yudao.module.system.controller.admin.user;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerDisplayNamesTest {
    @Mock AdminUserService userService;
    @Mock DeptService deptService;
    @Mock PostService postService;
    @Mock RoleService roleService;
    @Mock PermissionService permissionService;
    @InjectMocks UserController controller;

    @Test
    void pageIncludesAssignedNamesOnlyForDataScopedUsersAndKeepsDisabledAssignments() throws Exception {
        UserPageReqVO request = new UserPageReqVO();
        AdminUserDO user = new AdminUserDO().setId(11L).setUsername("scoped").setDeptId(7L)
                .setPostIds(Set.of(31L));
        when(userService.getUserPage(request)).thenReturn(new PageResult<>(List.of(user), 1L));
        when(deptService.getDeptMap(anyCollection())).thenReturn(Map.of());
        when(permissionService.getUserRoleIdListByUserId(11L)).thenReturn(Set.of(41L));
        when(postService.getPostList(Set.of(31L))).thenReturn(List.of(
                new PostDO().setId(31L).setName("既有停用岗位").setStatus(1)));
        when(roleService.getRoleList(Set.of(41L))).thenReturn(List.of(
                new RoleDO().setId(41L).setName("既有停用角色").setStatus(1)));

        var result = controller.getUserPage(request).getData();
        assertEquals(1L, result.getTotal());
        assertEquals(List.of("既有停用岗位"), result.getList().get(0).getPostNames());
        assertEquals(List.of("既有停用角色"), result.getList().get(0).getRoleNames());
        verify(permissionService).getUserRoleIdListByUserId(11L);
        verifyNoMoreInteractions(permissionService);
        assertEquals("@ss.hasPermission('system:user:query')", UserController.class
                .getMethod("getUserPage", UserPageReqVO.class).getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void emptyScopedPageDoesNotQueryOtherUsersOrRoleCatalogs() {
        UserPageReqVO request = new UserPageReqVO();
        when(userService.getUserPage(request)).thenReturn(new PageResult<>(List.of(), 0L));
        assertTrue(controller.getUserPage(request).getData().getList().isEmpty());
        verifyNoInteractions(deptService, permissionService, postService, roleService);
    }

    @Test
    void failedNameLookupRemainsAnErrorInsteadOfAnEmptySuccessfulList() {
        UserPageReqVO request = new UserPageReqVO();
        when(userService.getUserPage(request)).thenReturn(new PageResult<>(
                List.of(new AdminUserDO().setId(11L).setDeptId(7L)), 1L));
        when(deptService.getDeptMap(anyCollection())).thenReturn(Map.of());
        when(permissionService.getUserRoleIdListByUserId(11L))
                .thenThrow(new IllegalStateException("lookup unavailable"));
        assertThrows(IllegalStateException.class, () -> controller.getUserPage(request));
    }
}
