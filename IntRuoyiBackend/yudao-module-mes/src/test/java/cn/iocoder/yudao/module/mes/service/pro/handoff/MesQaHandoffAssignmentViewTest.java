package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrCandidateResolver;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesQaHandoffAssignmentViewTest {
    MesQaHandoffAssignmentService service;
    MesProEdhrWorkTaskAssignmentRuleMapper rules;
    MesProEdhrCandidateResolver candidates;
    MesActiveOrderHandoffOwnerResolver owners;
    AdminUserApi users;
    RoleService roles;
    PermissionApi permissions;
    MesProEdhrWorkTaskAssignmentRuleDO rule;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        service = new MesQaHandoffAssignmentService();
        rules = mock(MesProEdhrWorkTaskAssignmentRuleMapper.class);
        var routes = mock(MesProRouteMapper.class);
        candidates = mock(MesProEdhrCandidateResolver.class);
        owners = mock(MesActiveOrderHandoffOwnerResolver.class);
        users = mock(AdminUserApi.class);
        roles = mock(RoleService.class);
        permissions = mock(PermissionApi.class);
        for (var entry : Map.of("rules", rules, "routes", routes, "candidates", candidates,
                "owners", owners, "users", users, "roles", roles, "permissions", permissions).entrySet())
            ReflectionTestUtils.setField(service, entry.getKey(), entry.getValue());
        when(routes.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(MesProRouteDO.builder().id(98L).status(0).build());
        rule = MesQaHandoffAssignmentServiceTest.rule();
        when(rules.selectListByScopeAndType("ROUTE", 98L, "QA_REVIEW")).thenReturn(List.of(rule));
        when(candidates.resolveAssignmentRule(rule)).thenReturn(
                new MesProEdhrCandidateResolver.MesProEdhrCandidateContract("USER", 345L, "345"));
        when(users.getUser(345L)).thenReturn(user(345L, "qa-test", "正式QA"));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void userEvidenceComesFromTheValidatedSnapshot() {
        var view = service.view(98L);
        assertEquals("正式QA（qa-test）", view.candidateLabel());
        assertEquals(List.of(new MesQaHandoffAssignmentService.CandidateUser(345L, "qa-test", "正式QA")), view.candidateUsers());
        verify(candidates, times(1)).resolveAssignmentRule(rule);
        verify(owners).user(345L, MesQaHandoffAssignmentService.DISPOSE_PERMISSION);
        verify(users, times(1)).getUser(345L);
    }
    @Test void roleEvidenceIncludesOnlyTheSameValidatedCandidateSet() {
        role();
        when(candidates.resolveAssignmentRule(rule)).thenReturn(
                new MesProEdhrCandidateResolver.MesProEdhrCandidateContract("ROLE_GROUP", 50L, "345,346"));
        when(users.getUser(346L)).thenReturn(user(346L, "qa-second", "第二QA"));
        var view = service.view(98L);
        assertEquals("正式QA角色", view.candidateLabel());
        assertEquals(List.of(345L, 346L), view.candidateUsers().stream().map(MesQaHandoffAssignmentService.CandidateUser::id).toList());
        verify(candidates, times(1)).resolveAssignmentRule(rule);
        verify(owners).user(345L, MesQaHandoffAssignmentService.DISPOSE_PERMISSION);
        verify(owners).user(346L, MesQaHandoffAssignmentService.DISPOSE_PERMISSION);
        verify(users, never()).getUser(347L);
    }
    @Test void missingAndDisabledRulesRemainVisibleAsTheirRealState() {
        rule.setEnabled(false);
        assertFalse(service.view(98L).enabled());
        when(rules.selectListByScopeAndType("ROUTE", 98L, "QA_REVIEW")).thenReturn(List.of());
        assertNull(service.view(98L));
    }
    @Test void disabledRoleOrMissingDispositionPermissionFailsInsteadOfReturningEmptyEvidence() {
        role();
        when(roles.getRole(50L)).thenReturn(new RoleDO().setId(50L).setName("停用QA").setStatus(1));
        assertThrows(RuntimeException.class, () -> service.view(98L));
        role();
        when(permissions.hasAnyPermissionsInRoles(Set.of(50L), MesQaHandoffAssignmentService.DISPOSE_PERMISSION)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.view(98L));
        verifyNoInteractions(owners);
    }
    @Test void invalidResolvedAccountOrMissingVisibleIdentityNeverBecomesAnEmptySuccessfulRule() {
        doThrow(new IllegalStateException("candidate disabled or lacks dispose")).when(owners)
                .user(345L, MesQaHandoffAssignmentService.DISPOSE_PERMISSION);
        assertThrows(RuntimeException.class, () -> service.view(98L));
        reset(owners);
        when(users.getUser(345L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.view(98L));
    }
    private void role() {
        rule.setCandidateSourceType("ROLE_GROUP").setCandidateSourceId(50L);
        when(roles.getRole(50L)).thenReturn(new RoleDO().setId(50L).setName("正式QA角色").setStatus(0));
        when(permissions.hasAnyPermissionsInRoles(Set.of(50L), MesQaHandoffAssignmentService.DISPOSE_PERMISSION)).thenReturn(true);
    }
    private static AdminUserRespDTO user(Long id, String account, String name) {
        return new AdminUserRespDTO().setId(id).setUsername(account).setNickname(name).setStatus(0).setTenantId(1L);
    }
}
