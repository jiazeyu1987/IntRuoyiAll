package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccDirectoryAccessPermissionServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccDirectoryAccessRuleMapper accessRuleMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DeptApi deptApi;

    @InjectMocks
    private DccDirectoryAccessPermissionServiceImpl permissionService;

    @Test
    void getAuthorizedDirectoryIds_queryMatchesUserDeptRoleAndPost() {
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO()
                .setId(99L)
                .setDeptId(10L)
                .setPostIds(Set.of(20L)));
        doReturn(List.of(
                DccDirectoryAccessRuleDO.builder().directoryId(1L).subjectType("1").subjectId(99L).canQuery(true).active(true).build(),
                DccDirectoryAccessRuleDO.builder().directoryId(2L).subjectType("DEPT").subjectId(10L).canQuery(true).active(true).build(),
                DccDirectoryAccessRuleDO.builder().directoryId(3L).subjectType("POSITION").subjectId(20L).canQuery(true).active(true).build(),
                DccDirectoryAccessRuleDO.builder().directoryId(4L).subjectType("ROLE").subjectId(30L).canQuery(true).active(true).build(),
                DccDirectoryAccessRuleDO.builder().directoryId(5L).subjectType("ROLE").subjectId(40L).canQuery(false).active(true).build()
        )).when(accessRuleMapper).selectList(org.mockito.ArgumentMatchers.<SFunction<DccDirectoryAccessRuleDO, ?>>any(), eq(Boolean.TRUE));
        when(permissionApi.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(30L));

        Set<Long> actual = permissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.QUERY);

        assertEquals(Set.of(1L, 2L, 3L, 4L), actual);
    }

    @Test
    void getAuthorizedDirectoryIds_roleRulesUseCurrentUserRoleIdsOnce() {
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO()
                .setId(99L)
                .setDeptId(10L)
                .setPostIds(Set.of()));
        doReturn(List.of(
                DccDirectoryAccessRuleDO.builder().directoryId(4L).subjectType("ROLE").subjectId(30L)
                        .canQuery(true).active(true).build(),
                DccDirectoryAccessRuleDO.builder().directoryId(5L).subjectType("ROLE").subjectId(40L)
                        .canQuery(true).active(true).build()
        )).when(accessRuleMapper).selectList(org.mockito.ArgumentMatchers.<SFunction<DccDirectoryAccessRuleDO, ?>>any(), eq(Boolean.TRUE));
        when(permissionApi.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(30L));

        Set<Long> actual = permissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.QUERY);

        assertEquals(Set.of(4L), actual);
        verify(permissionApi).getUserRoleIdListByUserId(99L);
        verify(permissionApi, never()).getUserRoleIdListByRoleIds(any());
    }

    @Test
    void getAuthorizedDirectoryIds_deptRuleMatchesUserDeptAncestorWithoutChildTraversal() {
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO()
                .setId(99L)
                .setDeptId(11L)
                .setPostIds(Set.of()));
        doReturn(List.of(
                DccDirectoryAccessRuleDO.builder().directoryId(2L).subjectType("DEPT").subjectId(10L)
                        .canQuery(true).canPreview(false).active(true).build()
        )).when(accessRuleMapper).selectList(org.mockito.ArgumentMatchers.<SFunction<DccDirectoryAccessRuleDO, ?>>any(), eq(Boolean.TRUE));
        when(deptApi.getDept(11L)).thenReturn(childDept(11L, 10L));
        when(deptApi.getDept(10L)).thenReturn(childDept(10L, null));

        Set<Long> actual = permissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.PREVIEW);

        assertEquals(Set.of(2L), actual);
        verify(deptApi, never()).getChildDeptList(any());
    }

    @Test
    void getAuthorizedDirectoryIds_queryTreatsLegacyPreviewOnlyRuleAsMergedReadPermission() {
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO()
                .setId(99L)
                .setDeptId(10L)
                .setPostIds(Set.of()));
        doReturn(List.of(
                DccDirectoryAccessRuleDO.builder().directoryId(8L).subjectType("1").subjectId(99L)
                        .canQuery(false).canPreview(true).active(true).build()
        )).when(accessRuleMapper).selectList(org.mockito.ArgumentMatchers.<SFunction<DccDirectoryAccessRuleDO, ?>>any(), eq(Boolean.TRUE));

        Set<Long> actual = permissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.QUERY);

        assertEquals(Set.of(8L), actual);
    }

    @Test
    void hasDirectoryManagementPermission_usesPermissionApi() {
        when(permissionApi.hasAnyPermissions(99L,
                "dcc:controlled-file:directory:manage", "dcc:controlled-file:access-rule:manage"))
                .thenReturn(true);

        assertTrue(permissionService.hasDirectoryManagementPermission(99L));
        assertFalse(permissionService.hasDirectoryManagementPermission(null));
    }

    private DeptRespDTO childDept(Long id, Long parentId) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setParentId(parentId);
        return dept;
    }
}
