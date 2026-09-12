package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectAccessRuleMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccProjectAccessServiceImplTest {

    @Mock private DccProjectAccessRuleMapper accessRuleMapper;
    @Mock private AdminUserApi adminUserApi;
    @Mock private PermissionApi permissionApi;
    @Mock private DeptApi deptApi;
    @InjectMocks private DccProjectAccessServiceImpl service;

    @BeforeEach
    void setUpUser() {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(99L);
        user.setDeptId(20L);
        user.setPostIds(Set.of(30L));
        when(adminUserApi.getUser(99L)).thenReturn(user);
        when(permissionApi.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(40L));
        when(deptApi.getDept(20L)).thenReturn(null);
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
}
