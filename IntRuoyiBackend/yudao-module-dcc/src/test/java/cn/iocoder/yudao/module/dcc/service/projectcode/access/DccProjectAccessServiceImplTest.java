package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
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
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_RULE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_SUBJECT_DUPLICATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccProjectAccessServiceImplTest {

    @Mock private DccProjectAccessRuleMapper accessRuleMapper;
    @Mock private DccProjectCodeMapper projectCodeMapper;
    @Mock private AdminUserApi adminUserApi;
    @Mock private PermissionApi permissionApi;
    @Mock private DeptApi deptApi;
    @Mock private cn.iocoder.yudao.module.system.api.permission.RoleApi roleApi;
    @Mock private cn.iocoder.yudao.module.system.api.dept.PostApi postApi;
    @InjectMocks private DccProjectAccessServiceImpl service;

    @BeforeEach
    void setUpUser() {
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setIgnore(false);
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(99L);
        user.setTenantId(1L);
        user.setStatus(0);
        user.setDeptId(20L);
        user.setPostIds(Set.of(30L));
        lenient().when(adminUserApi.getUser(99L)).thenReturn(user);
        lenient().when(permissionApi.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(40L));
        lenient().when(deptApi.getDept(20L)).thenReturn(null);
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings={"   ","\u00A0\uFEFF\u3000"})
    void initialOwnerGrantRequiresItsOwnNonblankChangeReason(String reason) {
        var project=DccProjectCodeDO.builder().id(100L).status("ENABLE").projectLeaderUserId(99L).build();
        project.setTenantId(1L);
        when(projectCodeMapper.selectByIdForUpdate(100L)).thenReturn(project);
        var failure=assertThrows(ServiceException.class,()->service.initializeApprovedProjectLeaderOwner(100L,99L,reason));
        assertEquals(DCC_PROJECT_ACCESS_RULE_INVALID.getCode(),failure.getCode());
        verify(accessRuleMapper,never()).insert(any(DccProjectAccessRuleDO.class));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(ints = {1})
    void disabledOrUnknownAccountStatusCannotUseProjectOwnerOrEditor(Integer status) {
        var user = new AdminUserRespDTO(); user.setId(99L); user.setTenantId(1L); user.setStatus(status);
        when(adminUserApi.getUser(99L)).thenReturn(user);
        lenient().when(accessRuleMapper.selectActiveRules(eq(100L), any(LocalDateTime.class)))
                .thenReturn(List.of(rule("USER", 99L, "OWNER")));
        org.junit.jupiter.api.Assertions.assertFalse(service.hasProjectOwner(99L, 100L));
        org.junit.jupiter.api.Assertions.assertFalse(service.hasProjectEditorOrOwner(99L, 100L));
        assertThrows(ServiceException.class, () -> service.assertProjectEditorOrOwner(99L, 100L));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"USER", "DEPT", "ROLE", "POSITION"})
    void replaceProjectAccessRules_invalidSubjectCannotReplaceExistingOwner(String subjectType) {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());
        ServiceException invalidSubject = new ServiceException(400017, "授权主体不存在或已停用");
        switch (subjectType) {
            case "USER" -> lenient().doThrow(invalidSubject).when(adminUserApi).validateUserList(List.of(999999L));
            case "DEPT" -> lenient().doThrow(invalidSubject).when(deptApi).validateDeptList(List.of(999999L));
            case "ROLE" -> lenient().doThrow(invalidSubject).when(roleApi).validRoleList(List.of(999999L));
            case "POSITION" -> lenient().doThrow(invalidSubject).when(postApi).validPostList(List.of(999999L));
            default -> throw new AssertionError(subjectType);
        }

        ServiceException result = assertThrows(ServiceException.class,
                () -> service.replaceProjectAccessRules(100L, List.of(saveRule(subjectType, 999999L, "OWNER"))));

        org.junit.jupiter.api.Assertions.assertSame(invalidSubject, result);
        verify(accessRuleMapper, never()).deleteByProjectCodeId(any(Long.class));
        verify(accessRuleMapper, never()).insert(any(DccProjectAccessRuleDO.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"USER", "DEPT", "ROLE", "POSITION"})
    void replaceProjectAccessRules_validSubjectIsVerifiedBeforePersisting(String subjectType) {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());
        service.replaceProjectAccessRules(100L, List.of(saveRule(subjectType, 99L, "OWNER")));
        switch (subjectType) {
            case "USER" -> verify(adminUserApi).validateUserList(List.of(99L));
            case "DEPT" -> verify(deptApi).validateDeptList(List.of(99L));
            case "ROLE" -> verify(roleApi).validRoleList(List.of(99L));
            case "POSITION" -> verify(postApi).validPostList(List.of(99L));
            default -> throw new AssertionError(subjectType);
        }
        verify(accessRuleMapper).insert(any(DccProjectAccessRuleDO.class));
    }

    @Test
    void getProjectAccessRules_returnsFormalRulesForProject() {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());
        when(accessRuleMapper.selectListByProjectCodeId(100L))
                .thenReturn(List.of(rule("USER", 99L, "OWNER")));

        List<DccProjectAccessRuleDO> rules = service.getProjectAccessRules(100L);

        assertEquals(1, rules.size());
        assertEquals("OWNER", rules.get(0).getAccessLevel());
        verify(accessRuleMapper).selectListByProjectCodeId(100L);
    }

    @Test
    void replaceProjectAccessRules_requiresActiveOwnerAndPersistsFormalRules() {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());
        List<DccProjectAccessRuleSaveReqVO> rules = List.of(
                saveRule("USER", 99L, "OWNER"),
                saveRule("USER", 100L, "EDIT"));

        List<DccProjectAccessRuleDO> result = service.replaceProjectAccessRules(100L, rules);

        ArgumentCaptor<DccProjectAccessRuleDO> captor = ArgumentCaptor.forClass(DccProjectAccessRuleDO.class);
        verify(accessRuleMapper).deleteByProjectCodeId(100L);
        verify(accessRuleMapper, times(2)).insert(captor.capture());
        assertEquals(2, result.size());
        assertEquals("OWNER", captor.getAllValues().get(0).getAccessLevel());
        assertEquals("EDIT", captor.getAllValues().get(1).getAccessLevel());
        assertEquals(100L, captor.getAllValues().get(0).getDccProjectCodeId());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"USER,OWNER,EDIT", "USER,EDIT,VIEW", "USER,VIEW,VIEW",
            "DEPT,OWNER,VIEW", "ROLE,OWNER,EDIT", "POSITION,EDIT,VIEW"})
    void duplicateActiveSubjectIsRejectedBeforeDeletingExistingRules(String type, String first, String second) {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());
        ServiceException error = assertThrows(ServiceException.class, () -> service.replaceProjectAccessRules(100L,
                List.of(saveRule(type, 99L, first), saveRule(" " + type.toLowerCase() + " ", 99L, second))));
        assertEquals(DCC_PROJECT_ACCESS_SUBJECT_DUPLICATE.getCode(), error.getCode());
        verify(accessRuleMapper, never()).deleteByProjectCodeId(any(Long.class));
        verify(accessRuleMapper, never()).insert(any(DccProjectAccessRuleDO.class));
    }

    @Test
    void inactiveRulesAndDifferentSubjectTypesDoNotConflict() {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());
        var inactive = saveRule("USER", 99L, "VIEW");
        inactive.setActive(false);
        var result = service.replaceProjectAccessRules(100L, List.of(
                saveRule("USER", 99L, "OWNER"), inactive, saveRule("ROLE", 99L, "EDIT")));
        assertEquals(3, result.size());
        verify(accessRuleMapper, times(3)).insert(any(DccProjectAccessRuleDO.class));
    }

    @Test
    void replaceProjectAccessRules_rejectsRulesWithoutActiveOwner() {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.replaceProjectAccessRules(100L, List.of(saveRule("USER", 100L, "EDIT"))));

        assertEquals(DCC_PROJECT_ACCESS_RULE_INVALID.getCode(), error.getCode());
        verify(accessRuleMapper, never()).deleteByProjectCodeId(any(Long.class));
        verify(accessRuleMapper, never()).insert(any(DccProjectAccessRuleDO.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"  ", " 项目授权 "})
    void replaceProjectAccessRules_acceptsOptionalChangeReason(String reason) {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());
        DccProjectAccessRuleSaveReqVO ownerRule = saveRule("USER", 99L, "OWNER");
        ownerRule.setChangeReason(reason);
        DccProjectAccessRuleSaveReqVO viewerRule = saveRule("USER", 100L, "VIEW");
        viewerRule.setChangeReason(reason);

        try (var validatorFactory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertEquals(Set.of(), validatorFactory.getValidator().validate(viewerRule));
        }
        var saved = service.replaceProjectAccessRules(100L, List.of(ownerRule, viewerRule));

        String expected = reason == null ? null : reason.trim();
        assertEquals(expected, saved.get(0).getChangeReason());
        assertEquals(expected, saved.get(1).getChangeReason());
        ArgumentCaptor<DccProjectAccessRuleDO> captor = ArgumentCaptor.forClass(DccProjectAccessRuleDO.class);
        verify(accessRuleMapper).deleteByProjectCodeId(100L);
        verify(accessRuleMapper, times(2)).insert(captor.capture());
        assertEquals(expected, captor.getAllValues().get(1).getChangeReason());
    }

    @Test
    void assertProjectOwner_userOwnerRuleAllowsAccess() {
        when(accessRuleMapper.selectActiveRules(eq(100L), any(LocalDateTime.class)))
                .thenReturn(List.of(rule("USER", 99L, "OWNER")));

        assertDoesNotThrow(() -> service.assertProjectOwner(99L, 100L));
    }

    @Test
    void assertProjectOwner_noAuthoritativeRuleDeniesCorrectionAssignee() {
        when(accessRuleMapper.selectActiveRules(eq(100L), any(LocalDateTime.class))).thenReturn(List.of());

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.assertProjectOwner(99L, 100L));
        assertEquals(DCC_PROJECT_ACCESS_DENIED.getCode(), error.getCode());
    }

    @Test
    void assertProjectOwner_editRuleDoesNotGrantOwnerAccess() {
        when(accessRuleMapper.selectActiveRules(eq(100L), any(LocalDateTime.class)))
                .thenReturn(List.of(rule("USER", 99L, "EDIT")));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.assertProjectOwner(99L, 100L));
        assertEquals(DCC_PROJECT_ACCESS_DENIED.getCode(), error.getCode());
    }

    @Test
    void assertProjectEditorOrOwner_acceptsEditAndOwnerButRejectsView() {
        when(accessRuleMapper.selectActiveRules(eq(100L), any())).thenReturn(List.of(rule("USER", 99L, "EDIT")));
        assertDoesNotThrow(() -> service.assertProjectEditorOrOwner(99L, 100L));

        when(accessRuleMapper.selectActiveRules(eq(100L), any())).thenReturn(List.of(rule("USER", 99L, "OWNER")));
        assertDoesNotThrow(() -> service.assertProjectEditorOrOwner(99L, 100L));

        when(accessRuleMapper.selectActiveRules(eq(100L), any())).thenReturn(List.of(rule("USER", 99L, "VIEW")));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.assertProjectEditorOrOwner(99L, 100L));
        assertEquals(DCC_PROJECT_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    void assertProjectOwner_roleAndPositionSubjectsAreResolved() {
        when(accessRuleMapper.selectActiveRules(eq(100L), any(LocalDateTime.class)))
                .thenReturn(List.of(rule("ROLE", 40L, "OWNER")));
        assertDoesNotThrow(() -> service.assertProjectOwner(99L, 100L));

        when(accessRuleMapper.selectActiveRules(eq(100L), any(LocalDateTime.class)))
                .thenReturn(List.of(rule("POSITION", 30L, "OWNER")));
        assertDoesNotThrow(() -> service.assertProjectOwner(99L, 100L));
    }

    @Test
    void independentLibraryRoleDoesNotGrantAccessToAnotherProject() {
        var library = DccProjectCodeDO.builder().id(200L).libraryScope("QMS").status("ENABLE").build();
        library.setTenantId(1L);
        when(projectCodeMapper.selectById(200L)).thenReturn(library);
        when(permissionApi.hasAnyPermissions(99L,"dcc:library:qms:edit")).thenReturn(true);
        when(accessRuleMapper.selectActiveRules(eq(100L), any())).thenReturn(List.of());
        org.junit.jupiter.api.Assertions.assertTrue(service.hasProjectEditorOrOwner(99L,200L));
        org.junit.jupiter.api.Assertions.assertFalse(service.hasProjectEditorOrOwner(99L,100L));
    }

    @Test
    void libraryRulesCannotBeReadThroughLegacyProjectEndpointWithoutSuperAdmin() {
        when(projectCodeMapper.selectById(200L)).thenReturn(DccProjectCodeDO.builder().id(200L).libraryScope("QMS").build());
        try (var login = org.mockito.Mockito.mockStatic(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.class)) {
            login.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertThrows(ServiceException.class, () -> service.getProjectAccessRules(200L));
            verify(accessRuleMapper, never()).selectListByProjectCodeId(200L);
            when(permissionApi.hasAnyRoles(99L,"super_admin","tenant_admin")).thenReturn(true);
            when(accessRuleMapper.selectListByProjectCodeId(200L)).thenReturn(List.of(rule("USER",99L,"VIEW")));
            assertEquals(1,service.getProjectAccessRules(200L).size());
        }
    }

    @Test
    void libraryAclWritesAreRetiredEvenForSuperAdministrator() {
        when(projectCodeMapper.selectById(200L)).thenReturn(DccProjectCodeDO.builder().id(200L).libraryScope("QMS").build());
        assertThrows(ServiceException.class, () -> service.replaceProjectAccessRules(200L,List.of(saveRule("USER",99L,"VIEW"))));
        assertThrows(ServiceException.class, () -> service.replaceProjectAccessRules(200L,List.of()));
        verify(accessRuleMapper,never()).deleteByProjectCodeId(any(Long.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"QMS,dcc:library:qms:view,dcc:library:qms:edit", "GENERAL,dcc:library:general:view,dcc:library:general:edit"})
    void libraryViewEditRevocationAndTenantBoundaries(String scope, String view, String edit) {
        when(permissionApi.hasAnyPermissions(99L,"dcc:project-code:scope:all")).thenReturn(false);
        when(permissionApi.hasAnyPermissions(99L,"dcc:project-code-assignment:execute")).thenReturn(false);
        when(permissionApi.hasAnyPermissions(99L,view,edit)).thenReturn(false);
        when(permissionApi.hasAnyPermissions(99L,edit)).thenReturn(false);
        var library = DccProjectCodeDO.builder().id(200L).libraryScope(scope).status("ENABLE").build();
        library.setTenantId(1L);
        when(projectCodeMapper.selectById(200L)).thenReturn(library);
        var staleRule = rule("USER",99L,"EDIT"); staleRule.setDccProjectCodeId(200L);
        when(accessRuleMapper.selectActiveReaderRules(eq(1L),any())).thenReturn(List.of(staleRule));
        when(projectCodeMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(library));
        assertThrows(ServiceException.class, () -> service.assertProjectViewerOrAbove(99L,200L));
        assertEquals(List.of(),service.listReadableProjectIds(99L));
        when(permissionApi.hasAnyPermissions(99L,view,edit)).thenReturn(true);
        assertDoesNotThrow(() -> service.assertProjectViewerOrAbove(99L,200L));
        assertEquals(List.of(200L),service.listReadableProjectIds(99L));
        org.junit.jupiter.api.Assertions.assertFalse(service.hasProjectEditorOrOwner(99L,200L));
        when(permissionApi.hasAnyPermissions(99L,edit)).thenReturn(true);
        org.junit.jupiter.api.Assertions.assertTrue(service.hasProjectEditorOrOwner(99L,200L));
        org.junit.jupiter.api.Assertions.assertFalse(service.hasProjectOwner(99L,200L));
        when(permissionApi.hasAnyPermissions(99L,view,edit)).thenReturn(false);
        when(permissionApi.hasAnyPermissions(99L,edit)).thenReturn(false);
        assertEquals(List.of(),service.listReadableProjectIds(99L));
        org.junit.jupiter.api.Assertions.assertFalse(service.hasProjectEditorOrOwner(99L,200L));
        when(permissionApi.hasAnyPermissions(99L,edit)).thenReturn(true);
        org.junit.jupiter.api.Assertions.assertTrue(service.hasProjectEditorOrOwner(99L,200L));
        library.setTenantId(2L);
        org.junit.jupiter.api.Assertions.assertFalse(service.hasProjectEditorOrOwner(99L,200L));
        assertEquals(List.of(),service.listReadableProjectIds(99L));
        library.setTenantId(1L); library.setStatus("DISABLE");
        org.junit.jupiter.api.Assertions.assertFalse(service.hasProjectEditorOrOwner(99L,200L));
        assertEquals(List.of(),service.listReadableProjectIds(99L));
        library.setStatus("ENABLE");
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(1));
        org.junit.jupiter.api.Assertions.assertFalse(service.hasProjectEditorOrOwner(99L,200L));
        assertThrows(ServiceException.class, () -> service.listReadableProjectIds(99L));
        verify(accessRuleMapper,never()).selectActiveRules(eq(200L),any());
    }

    private static DccProjectAccessRuleDO rule(String subjectType, Long subjectId, String accessLevel) {
        DccProjectAccessRuleDO rule = new DccProjectAccessRuleDO();
        rule.setTenantId(1L);
        rule.setDccProjectCodeId(100L);
        rule.setSubjectType(subjectType);
        rule.setSubjectId(subjectId);
        rule.setAccessLevel(accessLevel);
        rule.setActive(true);
        return rule;
    }

    private static DccProjectAccessRuleSaveReqVO saveRule(String subjectType, Long subjectId, String accessLevel) {
        DccProjectAccessRuleSaveReqVO rule = new DccProjectAccessRuleSaveReqVO();
        rule.setSubjectType(subjectType);
        rule.setSubjectId(subjectId);
        rule.setAccessLevel(accessLevel);
        rule.setActive(true);
        rule.setChangeReason("DCC-STATIC-001 regression");
        return rule;
    }
}
