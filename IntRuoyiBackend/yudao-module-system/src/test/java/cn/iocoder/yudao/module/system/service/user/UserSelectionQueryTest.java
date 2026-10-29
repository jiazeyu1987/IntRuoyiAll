package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserSelectionQueryTest {
    private final AdminUserServiceImpl service = new AdminUserServiceImpl();
    private final AdminUserMapper users = mock(AdminUserMapper.class);
    private final UserPostMapper posts = mock(UserPostMapper.class);
    private final DeptService depts = mock(DeptService.class);
    private final PermissionService permissions = mock(PermissionService.class);

    UserSelectionQueryTest() {
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "userPostMapper", posts);
        ReflectionTestUtils.setField(service, "deptService", depts);
        ReflectionTestUtils.setField(service, "permissionService", permissions);
    }

    @Test
    void roleAndPostAreIntersectedAndEmptyIntersectionDoesNotQueryAllUsers() {
        UserPageReqVO req = new UserPageReqVO();
        req.setRoleId(10L);
        req.setPostId(20L);
        when(permissions.getUserRoleIdListByRoleId(any())).thenReturn(Set.of(1L));
        when(posts.selectListByPostIds(any())).thenReturn(List.of(new UserPostDO().setUserId(2L).setPostId(20L)));
        assertEquals(0L, service.getUserPage(req).getTotal());
        verifyNoInteractions(users);
    }

    @Test
    void exactDepartmentCannotEscapeFixedDepartmentScope() {
        UserPageReqVO req = new UserPageReqVO();
        req.setDeptId(30L);
        req.setScopeDeptId(10L);
        req.setIncludeChildDepts(false);
        when(depts.getChildDeptList(10L)).thenReturn(List.of(new DeptDO().setId(11L)));
        assertEquals(0L, service.getUserPage(req).getTotal());
        verifyNoInteractions(users);
        verify(depts, never()).getChildDeptList(30L);
    }

    @Test
    void clearingUserDepartmentKeepsFixedScope() {
        UserPageReqVO req = new UserPageReqVO();
        req.setScopeDeptId(10L);
        when(depts.getChildDeptList(10L)).thenReturn(List.of(new DeptDO().setId(11L)));
        when(users.selectPage(eq(req), eq(Set.of(10L, 11L)), isNull())).thenReturn(PageResult.empty());
        assertEquals(0L, service.getUserPage(req).getTotal());
        verify(users).selectPage(eq(req), eq(Set.of(10L, 11L)), isNull());
    }
    @Test
    void multipleDepartmentsIncludeChildrenWithoutDuplicates() {
        UserPageReqVO req = new UserPageReqVO();
        req.setDeptIds(List.of(10L, 11L, 20L));
        when(depts.getChildDeptList(10L)).thenReturn(List.of(new DeptDO().setId(11L)));
        when(depts.getChildDeptList(11L)).thenReturn(List.of());
        when(depts.getChildDeptList(20L)).thenReturn(List.of(new DeptDO().setId(21L)));
        when(users.selectPage(eq(req), eq(Set.of(10L, 11L, 20L, 21L)), isNull())).thenReturn(PageResult.empty());
        service.getUserPage(req);
        verify(users).selectPage(eq(req), eq(Set.of(10L, 11L, 20L, 21L)), isNull());
    }

    @Test
    void multipleExactDepartmentsDoNotExpandChildren() {
        UserPageReqVO req = new UserPageReqVO();
        req.setDeptIds(List.of(10L, 20L, 30L));
        req.setIncludeChildDepts(false);
        when(users.selectPage(eq(req), eq(Set.of(10L, 20L, 30L)), isNull())).thenReturn(PageResult.empty());
        service.getUserPage(req);
        verify(users).selectPage(eq(req), eq(Set.of(10L, 20L, 30L)), isNull());
        verifyNoInteractions(depts);
    }

    @Test
    void multipleDepartmentsIntersectFixedScope() {
        UserPageReqVO req = new UserPageReqVO();
        req.setDeptIds(List.of(11L, 20L));
        req.setIncludeChildDepts(false);
        req.setScopeDeptId(10L);
        when(depts.getChildDeptList(10L)).thenReturn(List.of(new DeptDO().setId(11L)));
        when(users.selectPage(eq(req), eq(Set.of(11L)), isNull())).thenReturn(PageResult.empty());
        service.getUserPage(req);
        verify(users).selectPage(eq(req), eq(Set.of(11L)), isNull());
    }

    @Test
    void disjointMultipleDepartmentsDoNotQueryAllUsers() {
        UserPageReqVO req = new UserPageReqVO();
        req.setDeptIds(List.of(20L, 30L));
        req.setIncludeChildDepts(false);
        req.setScopeDeptId(10L);
        when(depts.getChildDeptList(10L)).thenReturn(List.of(new DeptDO().setId(11L)));
        assertEquals(0L, service.getUserPage(req).getTotal());
        verifyNoInteractions(users);
    }

    @Test
    void multipleDepartmentsCanLimitCandidatesToTheirLeaders() {
        UserPageReqVO req = new UserPageReqVO();
        req.setDeptIds(List.of(10L, 20L));
        req.setIncludeChildDepts(false);
        req.setDeptLeaderMode("LEADER");
        when(depts.getDeptList(eq(Set.of(10L, 20L)))).thenReturn(List.of(
                new DeptDO().setId(10L).setLeaderUserId(101L), new DeptDO().setId(20L).setLeaderUserId(201L)));
        when(users.selectPage(eq(req), eq(Set.of()), eq(Set.of(101L, 201L)))).thenReturn(PageResult.empty());
        service.getUserPage(req);
        verify(users).selectPage(eq(req), eq(Set.of()), eq(Set.of(101L, 201L)));
    }

    @Test
    void departmentMembersExcludeSelectedDepartmentLeaders() {
        UserPageReqVO req = new UserPageReqVO();
        req.setDeptIds(List.of(10L, 20L));
        req.setIncludeChildDepts(false);
        req.setDeptLeaderMode("MEMBER");
        when(depts.getDeptList(eq(Set.of(10L, 20L)))).thenReturn(List.of(
                new DeptDO().setId(10L).setLeaderUserId(101L), new DeptDO().setId(20L).setLeaderUserId(201L)));
        when(users.selectPage(eq(req), eq(Set.of(10L, 20L)), isNull(), eq(Set.of(101L, 201L)))).thenReturn(PageResult.empty());
        service.getUserPage(req);
        verify(users).selectPage(eq(req), eq(Set.of(10L, 20L)), isNull(), eq(Set.of(101L, 201L)));
    }

    @Test
    void missingLeadersNeverExpandToAllDepartmentUsers() {
        UserPageReqVO req = new UserPageReqVO();
        req.setDeptIds(List.of(10L));
        req.setIncludeChildDepts(false);
        req.setDeptLeaderMode("LEADER");
        when(depts.getDeptList(eq(Set.of(10L)))).thenReturn(List.of(new DeptDO().setId(10L)));
        assertEquals(0L, service.getUserPage(req).getTotal());
        verifyNoInteractions(users);
    }

    @Test
    void leadersStillIntersectFixedRoleAndDepartmentScope() {
        UserPageReqVO req = new UserPageReqVO();
        req.setDeptIds(List.of(10L, 20L));
        req.setIncludeChildDepts(false);
        req.setDeptLeaderMode("LEADER");
        req.setScopeDeptId(10L);
        req.setScopeRoleId(5L);
        when(depts.getChildDeptList(10L)).thenReturn(List.of(new DeptDO().setId(11L)));
        when(depts.getDeptList(eq(Set.of(10L)))).thenReturn(List.of(new DeptDO().setId(10L).setLeaderUserId(101L)));
        when(permissions.getUserRoleIdListByRoleId(Set.of(5L))).thenReturn(Set.of(101L, 201L));
        when(users.selectPage(eq(req), eq(Set.of(10L, 11L)), eq(Set.of(101L)))).thenReturn(PageResult.empty());
        service.getUserPage(req);
        verify(users).selectPage(eq(req), eq(Set.of(10L, 11L)), eq(Set.of(101L)));
    }

    @Test
    void memberModeWithoutSelectedDepartmentsIsInvalid() {
        UserPageReqVO req = new UserPageReqVO();
        req.setDeptLeaderMode("MEMBER");
        assertFalse(req.isDepartmentMemberFilterValid());
        req.setDeptIds(List.of(10L));
        assertTrue(req.isDepartmentMemberFilterValid());
    }

    @Test
    void fixedRoleAndPostIntersectEvenWhenSearchFiltersAreCleared() {
        UserPageReqVO req = new UserPageReqVO();
        req.setScopeRoleId(10L);
        req.setScopePostId(20L);
        when(permissions.getUserRoleIdListByRoleId(any())).thenReturn(Set.of(1L, 2L));
        when(posts.selectListByPostIds(any())).thenReturn(List.of(new UserPostDO().setUserId(2L).setPostId(20L)));
        when(users.selectPage(eq(req), any(), eq(Set.of(2L)))).thenReturn(PageResult.empty());
        assertEquals(0L, service.getUserPage(req).getTotal());
        verify(users).selectPage(eq(req), any(), eq(Set.of(2L)));
    }

}
