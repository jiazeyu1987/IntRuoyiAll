package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.access.DccProjectAccessRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccProjectAccessManagementTest {
    private DccProjectAccessServiceImpl service;
    private DccProjectCodeMapper projects;
    private DccProjectAccessRuleMapper rules;
    private AdminUserApi users;
    private PermissionApi permissions;
    private DccProjectCodeDO project;
    private AdminUserRespDTO actor;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setIgnore(false);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser().setId(99L), null, List.of()));
        service = new DccProjectAccessServiceImpl();
        projects = mock(DccProjectCodeMapper.class);
        rules = mock(DccProjectAccessRuleMapper.class);
        users = mock(AdminUserApi.class);
        permissions = mock(PermissionApi.class);
        ReflectionTestUtils.setField(service, "projectCodeMapper", projects);
        ReflectionTestUtils.setField(service, "accessRuleMapper", rules);
        ReflectionTestUtils.setField(service, "adminUserApi", users);
        ReflectionTestUtils.setField(service, "permissionApi", permissions);
        ReflectionTestUtils.setField(service, "deptApi", mock(DeptApi.class));
        project = DccProjectCodeDO.builder().id(100L).status("ENABLE").build();
        project.setTenantId(1L);
        when(projects.selectById(100L)).thenReturn(project);
        actor = new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0);
        when(users.getUser(99L)).thenReturn(actor);
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"super_admin", "tenant_admin", "dcc_project_code_admin"})
    void eligibleManagerCanReadAndInitializeAclWithoutPriorProjectRules(String role) {
        grantRole(role);
        when(rules.selectListByProjectCodeId(100L)).thenReturn(List.of());
        assertEquals(List.of(), service.getProjectAccessRules(100L));
        assertEquals(1, service.replaceProjectAccessRules(100L, List.of(ownerGrant())).size());
        verify(rules).deleteByProjectCodeId(100L);
        verify(rules).insert(any(DccProjectAccessRuleDO.class));
        if ("dcc_project_code_admin".equals(role)) {
            assertFalse(service.hasProjectOwner(99L, 100L));
            assertFalse(service.hasProjectEditorOrOwner(99L, 100L));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEW", "EDIT", "OWNER"})
    void formalProjectAccessAndGlobalUpdateDoNotGrantAclManagement(String level) {
        grantFormalAccess(level);
        when(permissions.hasAnyPermissions(99L, "dcc:project-code:update")).thenReturn(true);
        assertTrue(permissions.hasAnyPermissions(99L, "dcc:project-code:update"));
        assertDeniedWrite();
    }

    @Test
    void documentControlRoleAloneDoesNotGrantAclManagement() {
        grantRole("doc_control");
        grantFormalAccess("OWNER");
        assertDeniedWrite();
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEW", "EDIT", "OWNER"})
    void readableProjectAllowsInspectingRulesWithoutAclManagement(String level) {
        grantFormalAccess(level);
        var visibleRules = List.of(formalRule(level));
        when(rules.selectListByProjectCodeId(100L)).thenReturn(visibleRules);
        assertEquals(visibleRules, service.getProjectAccessRules(100L));
        assertDeniedWrite();
    }

    @Test
    void invisibleProjectCannotExposeAclSubjects() {
        assertDeniedRead();
        assertDeniedWrite();
    }

    @Test
    void revokedAccessCannotContinueReadingAcl() {
        grantFormalAccess("VIEW");
        assertDoesNotThrow(() -> service.getProjectAccessRules(100L));
        clearInvocations(rules);
        when(rules.selectActiveRules(eq(100L), any())).thenReturn(List.of());
        assertDeniedRead();
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEW", "EDIT", "OWNER"})
    void formalViewingDoesNotRequireCorrectionAssignment(String level) {
        grantFormalAccess(level);
        when(permissions.hasAnyPermissions(99L, "dcc:project-code-assignment:execute")).thenReturn(true);
        when(rules.selectActiveReaderRules(eq(1L), any())).thenReturn(List.of(formalRule(level)));
        assertDoesNotThrow(() -> service.assertProjectViewerOrAbove(99L, 100L));
        assertDoesNotThrow(() -> service.getProjectAccessRules(100L));
        assertEquals(List.of(100L), service.listReadableProjectIds(99L));
        if ("VIEW".equals(level)) {
            assertFalse(service.hasProjectEditorOrOwner(99L, 100L));
            assertThrows(ServiceException.class, () -> service.assertProjectMetadataEditable(99L, 100L));
        }
        assertDeniedWrite();
    }

    @Test
    void correctionExecutionPermissionAloneDoesNotGrantViewing() {
        when(permissions.hasAnyPermissions(99L, "dcc:project-code-assignment:execute")).thenReturn(true);
        assertDeniedRead();
        assertTrue(service.listReadableProjectIds(99L).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"super_admin", "tenant_admin", "dcc_project_code_admin"})
    void managerCannotReadOrWriteForeignOrDisabledProject(String role) {
        grantRole(role);
        project.setTenantId(2L);
        assertDeniedRead();
        assertDeniedWrite();
        project.setTenantId(1L);
        project.setStatus("DISABLE");
        assertDeniedRead();
        assertDeniedWrite();
    }

    @ParameterizedTest
    @ValueSource(strings = {"super_admin", "tenant_admin", "dcc_project_code_admin"})
    void managerMustRemainAnEnabledMatchingTenantAccount(String role) {
        grantRole(role);
        actor.setStatus(1);
        assertDeniedRead();
        assertDeniedWrite();
        actor.setStatus(null);
        assertDeniedRead();
        assertDeniedWrite();
        actor.setStatus(0).setTenantId(2L);
        assertDeniedRead();
        assertDeniedWrite();
        actor.setTenantId(1L).setId(98L);
        assertDeniedRead();
        assertDeniedWrite();
        when(users.getUser(99L)).thenReturn(null);
        assertDeniedRead();
        assertDeniedWrite();
    }

    @Test
    void unauthenticatedCallerCannotReadOrWriteAcl() {
        SecurityContextHolder.clearContext();
        assertDeniedRead();
        assertDeniedWrite();
    }

    private void grantRole(String role) {
        when(permissions.hasAnyRoles(eq(99L), any(String[].class))).thenAnswer(invocation ->
                Arrays.asList((String[]) invocation.getRawArguments()[1]).contains(role));
    }

    private void grantFormalAccess(String level) {
        when(rules.selectActiveRules(eq(100L), any())).thenReturn(List.of(formalRule(level)));
    }

    private DccProjectAccessRuleDO formalRule(String level) {
        var rule = new DccProjectAccessRuleDO();
        rule.setTenantId(1L);
        rule.setDccProjectCodeId(100L);
        rule.setSubjectType("USER");
        rule.setSubjectId(99L);
        rule.setAccessLevel(level);
        rule.setActive(true);
        return rule;
    }

    private DccProjectAccessRuleSaveReqVO ownerGrant() {
        var rule = new DccProjectAccessRuleSaveReqVO();
        rule.setSubjectType("USER");
        rule.setSubjectId(99L);
        rule.setAccessLevel("OWNER");
        rule.setActive(true);
        return rule;
    }

    private void assertDeniedRead() {
        var failure = assertThrows(ServiceException.class, () -> service.getProjectAccessRules(100L));
        assertEquals(DCC_PROJECT_ACCESS_DENIED.getCode(), failure.getCode());
        verify(rules, never()).selectListByProjectCodeId(100L);
    }

    private void assertDeniedWrite() {
        var failure = assertThrows(ServiceException.class,
                () -> service.replaceProjectAccessRules(100L, List.of(ownerGrant())));
        assertEquals(DCC_PROJECT_ACCESS_DENIED.getCode(), failure.getCode());
        verify(rules, never()).deleteByProjectCodeId(anyLong());
        verify(rules, never()).insert(any(DccProjectAccessRuleDO.class));
        verify(users, never()).validateUserList(anyCollection());
    }
}
