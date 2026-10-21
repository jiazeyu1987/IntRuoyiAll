package cn.iocoder.yudao.module.system.controller.admin.user;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.user.UserDisplayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerDisplayNamesTest {
    @Mock AdminUserService userService;
    @Mock DeptService deptService;
    @Mock UserDisplayService userDisplayService;
    @InjectMocks UserController controller;

    @Test
    void pageIncludesAssignedNamesOnlyForDataScopedUsersAndKeepsDisabledAssignments() throws Exception {
        UserPageReqVO request = new UserPageReqVO();
        AdminUserDO user = new AdminUserDO().setId(11L).setUsername("scoped").setDeptId(7L)
                .setPostIds(Set.of(31L));
        when(userService.getUserPage(request)).thenReturn(new PageResult<>(List.of(user), 1L));
        UserRespVO described = new UserRespVO();
        described.setId(user.getId());
        described.setPostNames(List.of("既有停用岗位（已停用）"));
        described.setRoleNames(List.of("既有停用角色（已停用）"));
        when(userDisplayService.describe(List.of(user))).thenReturn(List.of(described));

        var result = controller.getUserPage(request).getData();
        assertEquals(1L, result.getTotal());
        assertEquals(List.of("既有停用岗位（已停用）"), result.getList().get(0).getPostNames());
        assertEquals(List.of("既有停用角色（已停用）"), result.getList().get(0).getRoleNames());
        verify(userDisplayService).describe(List.of(user));
        verifyNoMoreInteractions(userDisplayService);
        verifyNoInteractions(deptService);
        assertEquals("@ss.hasPermission('system:user:query')", UserController.class
                .getMethod("getUserPage", UserPageReqVO.class).getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void emptyScopedPageDoesNotQueryOtherUsersOrRoleCatalogs() {
        UserPageReqVO request = new UserPageReqVO();
        when(userService.getUserPage(request)).thenReturn(new PageResult<>(List.of(), 0L));
        assertTrue(controller.getUserPage(request).getData().getList().isEmpty());
        verifyNoInteractions(deptService, userDisplayService);
    }

    @Test
    void failedNameLookupRemainsAnErrorInsteadOfAnEmptySuccessfulList() {
        UserPageReqVO request = new UserPageReqVO();
        List<AdminUserDO> scopedUsers = List.of(new AdminUserDO().setId(11L).setDeptId(7L));
        when(userService.getUserPage(request)).thenReturn(new PageResult<>(scopedUsers, 1L));
        when(userDisplayService.describe(scopedUsers))
                .thenThrow(new IllegalStateException("lookup unavailable"));
        assertThrows(IllegalStateException.class, () -> controller.getUserPage(request));
    }
}
