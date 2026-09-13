package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.access.DccProjectAccessRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.PostApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.dept.dto.PostRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_RULE_INVALID;
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
    @Mock private RoleApi roleApi;
    @Mock private DeptApi deptApi;
    @Mock private PostApi postApi;
    @InjectMocks private DccProjectAccessServiceImpl service;

    @BeforeEach
    void setUpUser() {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(99L);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        user.setDeptId(20L);
        user.setPostIds(Set.of(30L));
        AdminUserRespDTO editor = new AdminUserRespDTO();
        editor.setId(100L);
        editor.setStatus(CommonStatusEnum.ENABLE.getStatus());
        lenient().when(adminUserApi.getUser(99L)).thenReturn(user);
        lenient().when(adminUserApi.getUser(100L)).thenReturn(editor);
        lenient().when(permissionApi.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(40L));
        lenient().when(roleApi.getRoleList(List.of(40L))).thenReturn(List.of(role(40L)));
        lenient().when(deptApi.getDept(20L)).thenReturn(dept(20L));
        lenient().when(postApi.getPostList(List.of(30L))).thenReturn(List.of(post(30L)));
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

    @Test
    void replaceProjectAccessRules_rejectsRulesWithoutActiveOwner() {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.replaceProjectAccessRules(100L, List.of(saveRule("USER", 100L, "EDIT"))));

        assertEquals(DCC_PROJECT_ACCESS_RULE_INVALID.getCode(), error.getCode());
        verify(accessRuleMapper, never()).deleteByProjectCodeId(any(Long.class));
        verify(accessRuleMapper, never()).insert(any(DccProjectAccessRuleDO.class));
    }

    @Test
    void replaceProjectAccessRules_rejectsBlankChangeReasonBeforeDeletingRules() {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());
        DccProjectAccessRuleSaveReqVO ownerRule = saveRule("USER", 99L, "OWNER");
        ownerRule.setChangeReason("  ");

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.replaceProjectAccessRules(100L, List.of(ownerRule)));

        assertEquals(DCC_PROJECT_ACCESS_RULE_INVALID.getCode(), error.getCode());
        verify(accessRuleMapper, never()).deleteByProjectCodeId(any(Long.class));
        verify(accessRuleMapper, never()).insert(any(DccProjectAccessRuleDO.class));
    }

    @Test
    void replaceProjectAccessRules_rejectsMissingUserRoleDeptAndPositionOwnerBeforeDeletingRules() {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());

        for (String subjectType : List.of("USER", "ROLE", "DEPT", "POSITION")) {
            ServiceException error = assertThrows(ServiceException.class,
                    () -> service.replaceProjectAccessRules(100L, List.of(saveRule(subjectType, 404L, "OWNER"))));
            assertEquals(DCC_PROJECT_ACCESS_RULE_INVALID.getCode(), error.getCode());
        }
        verify(accessRuleMapper, never()).deleteByProjectCodeId(any(Long.class));
        verify(accessRuleMapper, never()).insert(any(DccProjectAccessRuleDO.class));
    }

    @Test
    void replaceProjectAccessRules_acceptsExistingAssignableUserRoleDeptAndPositionSubjects() {
        when(projectCodeMapper.selectById(100L)).thenReturn(DccProjectCodeDO.builder().id(100L).build());

        List<DccProjectAccessRuleDO> result = service.replaceProjectAccessRules(100L, List.of(
                saveRule("USER", 99L, "OWNER"),
                saveRule("ROLE", 40L, "EDIT"),
                saveRule("DEPT", 20L, "VIEW"),
                saveRule("POSITION", 30L, "VIEW")));

        assertEquals(4, result.size());
        verify(accessRuleMapper).deleteByProjectCodeId(100L);
        verify(accessRuleMapper, times(4)).insert(any(DccProjectAccessRuleDO.class));
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

    private static DccProjectAccessRuleDO rule(String subjectType, Long subjectId, String accessLevel) {
        DccProjectAccessRuleDO rule = new DccProjectAccessRuleDO();
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

    private static RoleRespDTO role(Long id) {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(id);
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return role;
    }

    private static DeptRespDTO dept(Long id) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return dept;
    }

    private static PostRespDTO post(Long id) {
        PostRespDTO post = new PostRespDTO();
        post.setId(id);
        post.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return post;
    }
}
