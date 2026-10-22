package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductActorSupport;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccW01ProjectAccessIdentityTest {
    DccProjectAccessServiceImpl service;
    AdminUserApi users;
    PermissionApi permissions;
    DeptApi departments;
    DccProjectAccessRuleMapper rules;
    DccProjectCodeMapper projects;
    DccProjectCodeAssignmentMapper assignments;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L); TenantContextHolder.setIgnore(false);
        service = new DccProjectAccessServiceImpl();
        users = mock(AdminUserApi.class); permissions = mock(PermissionApi.class);
        departments = mock(DeptApi.class); rules = mock(DccProjectAccessRuleMapper.class);
        projects = mock(DccProjectCodeMapper.class); assignments = mock(DccProjectCodeAssignmentMapper.class);
        ReflectionTestUtils.setField(service, "adminUserApi", users);
        ReflectionTestUtils.setField(service, "permissionApi", permissions);
        ReflectionTestUtils.setField(service, "deptApi", departments);
        ReflectionTestUtils.setField(service, "accessRuleMapper", rules);
        ReflectionTestUtils.setField(service, "projectCodeMapper", projects);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignments);
        var project = DccProjectCodeDO.builder().id(100L).status("ENABLE").build(); project.setTenantId(1L);
        when(projects.selectById(100L)).thenReturn(project);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    AdminUserRespDTO user() {
        return new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0)
                .setUsername("admin").setNickname("正式账号").setDeptId(20L).setPostIds(Set.of(30L));
    }
    DccProjectAccessRuleDO rule(String type, Long tenant, String level) {
        var row = new DccProjectAccessRuleDO(); row.setTenantId(tenant); row.setDccProjectCodeId(100L);
        row.setSubjectType(type); row.setSubjectId(switch(type) { case "USER" -> 99L; case "DEPT" -> 20L; case "ROLE" -> 40L; default -> 30L; });
        row.setAccessLevel(level); row.setActive(true); return row;
    }
    void grant(String type, Long tenant) {
        when(permissions.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(40L));
        when(rules.selectActiveRules(eq(100L), any(LocalDateTime.class))).thenReturn(List.of(rule(type, tenant, "OWNER")));
        when(rules.selectActiveReaderRules(eq(1L), any(LocalDateTime.class))).thenReturn(List.of(rule(type, tenant, "VIEW")));
    }
    void rejectAccount(AdminUserRespDTO account, String type) {
        grant(type, 1L); when(users.getUser(99L)).thenReturn(account);
        clearInvocations(permissions, departments, rules, assignments);
        assertFalse(service.hasProjectOwner(99L, 100L), type + " must reject OWNER");
        assertFalse(service.hasProjectEditorOrOwner(99L, 100L), type + " must reject EDIT");
        assertThrows(ServiceException.class, () -> service.assertProjectOwner(99L, 100L));
        assertThrows(ServiceException.class, () -> service.assertProjectEditorOrOwner(99L, 100L));
        assertThrows(ServiceException.class, () -> service.assertProjectViewerOrAbove(99L, 100L));
        assertThrows(ServiceException.class, () -> service.listReadableProjectIds(99L));
        verifyNoInteractions(permissions, departments, rules, assignments);
    }
    @Test void foreignReturnedIdCannotBorrowAnySubjectOrReaderScope() {
        for (String type : List.of("USER", "DEPT", "ROLE", "POSITION")) rejectAccount(user().setId(98L), type);
    }
    @Test void foreignTenantCannotBorrowAnySubjectOrReaderScope() {
        for (String type : List.of("USER", "DEPT", "ROLE", "POSITION")) rejectAccount(user().setTenantId(2L), type);
    }
    @Test void disabledAndUnknownStatusCannotBorrowAnySubjectOrReaderScope() {
        for (String type : List.of("USER", "DEPT", "ROLE", "POSITION")) {
            rejectAccount(user().setStatus(1), type); rejectAccount(user().setStatus(null), type);
        }
    }
    @Test void missingTenantAndIgnoredContextCannotBorrowReaderScope() {
        rejectAccount(user().setTenantId(null), "USER");
        TenantContextHolder.setIgnore(true); rejectAccount(user(), "USER");
    }
    @Test void foreignRuleTenantCannotGrantOwnerEditorOrReadableProjects() {
        when(users.getUser(99L)).thenReturn(user());
        for (String type : List.of("USER", "DEPT", "ROLE", "POSITION")) {
            grant(type, 2L);
            assertFalse(service.hasProjectOwner(99L, 100L), type);
            assertFalse(service.hasProjectEditorOrOwner(99L, 100L), type);
            assertTrue(service.listReadableProjectIds(99L).isEmpty(), type);
        }
    }
    @Test void exactEnabledTenantAccountKeepsAllFourFormalSubjects() {
        when(users.getUser(99L)).thenReturn(user());
        for (String type : List.of("USER", "DEPT", "ROLE", "POSITION")) {
            grant(type, 1L);
            assertTrue(service.hasProjectOwner(99L, 100L), type);
            assertTrue(service.hasProjectEditorOrOwner(99L, 100L), type);
            assertDoesNotThrow(() -> service.assertProjectViewerOrAbove(99L, 100L));
            assertEquals(List.of(100L), service.listReadableProjectIds(99L), type);
        }
    }
    @Test void exactIdentityDoesNotRemoveExistingAssignedProjectHardScope() {
        when(users.getUser(99L)).thenReturn(user()); grant("USER", 1L);
        when(permissions.hasAnyPermissions(99L, "dcc:project-code-assignment:execute")).thenReturn(true);
        when(assignments.selectActiveProjectCodeIdsByAssigneeUserId(eq(1L), eq(99L), any())).thenReturn(List.of(101L));
        assertTrue(service.listReadableProjectIds(99L).isEmpty());
        assertThrows(ServiceException.class, () -> service.assertProjectViewerOrAbove(99L, 100L));
    }
    @Test void existingProjectProductActorHelperAlreadyRejectsForeignAndDisabledAccounts() {
        var actor = new DccProjectProductActorSupport();
        ReflectionTestUtils.setField(actor, "users", users); ReflectionTestUtils.setField(actor, "permissions", permissions);
        for (var invalid : List.of(user().setId(98L), user().setTenantId(2L), user().setStatus(1))) {
            when(users.getUser(99L)).thenReturn(invalid);
            assertThrows(ServiceException.class, () -> actor.requireReadableAccount(99L));
            assertThrows(ServiceException.class, () -> actor.isApprover(99L));
        }
        verifyNoInteractions(permissions);
    }
}
