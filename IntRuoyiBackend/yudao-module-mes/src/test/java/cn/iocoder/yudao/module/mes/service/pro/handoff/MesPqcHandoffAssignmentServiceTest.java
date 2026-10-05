package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.handoff.MesActiveOrderHandoffController;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesPqcHandoffAssignmentServiceTest {
    MesPqcHandoffAssignmentService service;
    MesProEdhrWorkTaskAssignmentRuleMapper rules;
    MesProRouteMapper routes;
    MesActiveOrderHandoffOwnerResolver owners;
    AdminUserApi users;
    RoleService roles;
    PermissionApi permissions;
    MesProEdhrOperationAuditService operationAudit;
    MesActiveOrderHandoffAudit audit;

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);service=new MesPqcHandoffAssignmentService();
        rules=mock(MesProEdhrWorkTaskAssignmentRuleMapper.class);routes=mock(MesProRouteMapper.class);
        owners=mock(MesActiveOrderHandoffOwnerResolver.class);users=mock(AdminUserApi.class);
        roles=mock(RoleService.class);permissions=mock(PermissionApi.class);
        operationAudit=mock(MesProEdhrOperationAuditService.class);audit=mock(MesActiveOrderHandoffAudit.class);
        for(var entry:Map.of("rules",rules,"routes",routes,"owners",owners,"users",users,"roles",roles,
                "permissions",permissions,"operationAudit",operationAudit,"audit",audit).entrySet())
            ReflectionTestUtils.setField(service,entry.getKey(),entry.getValue());
        when(routes.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(MesProRouteDO.builder().id(98L).status(0).build());
        when(owners.validatePqcHandoffCandidates("344")).thenReturn(List.of(343L));
        var user=new AdminUserRespDTO();user.setId(344L);user.setNickname("正式PQC检验员");user.setUsername("pqc-user");user.setStatus(0);
        when(users.getUser(344L)).thenReturn(user);
    }
    @AfterEach void clear() {TenantContextHolder.clear();}

    @Test void missingExactRouteRuleFailsWithoutScanningTenantPersonnelOrQaRules() {
        var error=assertThrows(RuntimeException.class,()->service.resolve(98L));assertTrue(error.getMessage().contains("未配置"));
        verify(rules).selectListByScopeAndType("ROUTE",98L,"PQC_HANDOFF");verifyNoInteractions(owners,users,roles,permissions);
    }
    @Test void anotherRouteNeverSharesTheExistingDispatchRule() {
        when(routes.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(MesProRouteDO.builder().id(99L).status(0).build());
        when(rules.selectListByScopeAndType("ROUTE",98L,"PQC_HANDOFF")).thenReturn(List.of(rule()));
        assertThrows(RuntimeException.class,()->service.resolve(99L));verify(rules).selectListByScopeAndType("ROUTE",99L,"PQC_HANDOFF");
        verify(rules,never()).selectListByScopeAndType("ROUTE",98L,"PQC_HANDOFF");verifyNoInteractions(owners);
    }
    @ParameterizedTest @ValueSource(strings={"disabled","duplicate","scopeType","scopeId","taskType","routeProcess","ruleId","sourceType","sourceId"})
    void invalidOrDuplicateExactRuleIsRejectedBeforeAnyTaskCandidateIsFrozen(String defect) {
        var r=rule();List<MesProEdhrWorkTaskAssignmentRuleDO> rows=List.of(r);
        switch(defect) {
            case "disabled" -> r.setEnabled(false);
            case "duplicate" -> rows=List.of(r,rule().setId(902L));
            case "scopeType" -> r.setScopeType("GLOBAL");
            case "scopeId" -> r.setScopeId(99L);
            case "taskType" -> r.setTaskType("QA_REVIEW");
            case "routeProcess" -> r.setRouteProcessId(91L);
            case "ruleId" -> r.setId(null);
            case "sourceType" -> r.setCandidateSourceType("DEPARTMENT");
            case "sourceId" -> r.setCandidateSourceId(0L);
            default -> throw new IllegalArgumentException(defect);
        }
        when(rules.selectListByScopeAndType("ROUTE",98L,"PQC_HANDOFF")).thenReturn(rows);
        assertThrows(RuntimeException.class,()->service.resolve(98L));verifyNoInteractions(owners,users,roles,permissions);
    }
    @Test void exactUserRuleFreezesOnlyExplicitSelectedInspectorAndFormalLeader() {
        when(rules.selectListByScopeAndType("ROUTE",98L,"PQC_HANDOFF")).thenReturn(List.of(rule()));
        var resolved=service.resolve(98L);assertEquals("344",resolved.candidateUserSnapshot());assertEquals(List.of(343L),resolved.handlerLeaderUserIds());
        assertEquals(901L,resolved.ruleId());assertEquals(98L,resolved.routeId());assertEquals("PQC_HANDOFF",resolved.snapshot().get("taskType"));
        var view=service.view(98L);assertEquals("正式PQC检验员（pqc-user）",view.candidateLabel());assertEquals("344",view.candidateUserSnapshot());
        verify(owners,times(2)).validatePqcHandoffCandidates("344");verifyNoInteractions(roles,permissions);
    }
    @Test void roleGroupValidatesAllMembersWithoutSilentlyDroppingAnySelectedAccount() {
        var r=rule().setCandidateSourceType("ROLE_GROUP").setCandidateSourceId(700L);roleFixture(r);
        when(permissions.getUserRoleIdListByRoleIds(Set.of(700L))).thenReturn(Set.of(339L,344L));
        when(owners.validatePqcHandoffCandidates("339,344")).thenReturn(List.of(338L,343L));
        var resolved=service.resolve(98L);assertEquals("339,344",resolved.candidateUserSnapshot());assertEquals(List.of(338L,343L),resolved.handlerLeaderUserIds());
        when(owners.validatePqcHandoffCandidates("339,344")).thenThrow(new IllegalStateException("selected disabled employee"));
        assertThrows(IllegalStateException.class,()->service.resolve(98L));verify(rules,never()).insert(any(MesProEdhrWorkTaskAssignmentRuleDO.class));
    }
    @ParameterizedTest @ValueSource(strings={"missingRole","disabledRole","foreignRole","wrongRoleId","missingCreate","emptyMembers","invalidMember"})
    void roleGroupNeedsCurrentEnabledTenantRoleAndEveryRealSelectedMember(String defect) {
        var r=rule().setCandidateSourceType("ROLE_GROUP").setCandidateSourceId(700L);var role=roleFixture(r);
        switch(defect) {
            case "missingRole" -> when(roles.getRole(700L)).thenReturn(null);
            case "disabledRole" -> role.setStatus(1);
            case "foreignRole" -> role.setTenantId(2L);
            case "wrongRoleId" -> role.setId(701L);
            case "missingCreate" -> when(permissions.hasAnyPermissionsInRoles(Set.of(700L),MesPqcHandoffAssignmentService.CREATE_PERMISSION)).thenReturn(false);
            case "emptyMembers" -> when(permissions.getUserRoleIdListByRoleIds(Set.of(700L))).thenReturn(Set.of());
            case "invalidMember" -> when(permissions.getUserRoleIdListByRoleIds(Set.of(700L))).thenReturn(Set.of(0L));
            default -> throw new IllegalArgumentException(defect);
        }
        assertThrows(RuntimeException.class,()->service.resolve(98L));verifyNoInteractions(owners);
    }
    @Test void selectedInspectorScopeOrPermissionFailureIsVisibleAndDoesNotWriteRule() {
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            doThrow(new IllegalStateException("selected inspector no permission or ambiguous leader")).when(owners).validatePqcHandoffCandidates("344");
            var error=assertThrows(IllegalStateException.class,()->service.save(98L,"USER",344L,true,"明确派发本路线",null));
            assertTrue(error.getMessage().contains("selected inspector"));verify(rules,never()).insert(any(MesProEdhrWorkTaskAssignmentRuleDO.class));verifyNoInteractions(operationAudit);
        }
    }
    @Test void saveCapturesActualConfigurationAndActorInCallerTransactionSpecializedAudit() throws Exception {
        var after=rule().setRemark("明确派发本路线");when(rules.selectListByScopeAndType("ROUTE",98L,"PQC_HANDOFF"))
                .thenReturn(List.of(),List.of(after));
        when(rules.insert(any(MesProEdhrWorkTaskAssignmentRuleDO.class))).thenAnswer(i->{i.<MesProEdhrWorkTaskAssignmentRuleDO>getArgument(0).setId(901L);return 1;});
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("实际配置人");
            assertSame(after,service.save(98L,"USER",344L,true," 明确派发本路线 ",null));
        }
        var command=ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);var order=inOrder(rules,operationAudit);
        order.verify(rules).insert(any(MesProEdhrWorkTaskAssignmentRuleDO.class));order.verify(operationAudit).recordInCallerTransaction(command.capture());
        var c=command.getValue();assertEquals("WORK_TASK_RULE_SAVE",c.getOperationType());assertEquals("WORK_TASK_ASSIGNMENT_RULE",c.getObjectType());
        assertEquals("901",c.getObjectId());assertEquals(98L,c.getRouteId());assertEquals(1L,c.getActorUserId());assertEquals("实际配置人",c.getActorUsername());
        assertEquals("mes:pro-edhr-work-task-rule:update",c.getPermissionCode());assertNotEquals(c.getBeforeSummaryHash(),c.getAfterSummaryHash());
        var metadata=JsonUtils.parseTree(c.getMetadataJson());assertEquals("PQC_HANDOFF_ASSIGNMENT_CONFIG",metadata.path("requestSource").asText());
        assertEquals("明确派发本路线",metadata.path("reason").asText());assertEquals(0,metadata.path("beforeRules").size());
        assertEquals("PQC_HANDOFF",metadata.path("afterRules").get(0).path("taskType").asText());assertEquals(344L,metadata.path("afterRules").get(0).path("candidateSourceId").longValue());
        verify(operationAudit,never()).record(any());verify(audit,never()).append(anyString(),anyString(),anyString(),anyLong(),anyInt(),any(),any(),anyString());
        var annotation=MesPqcHandoffAssignmentService.class.getMethod("save",Long.class,String.class,Long.class,Boolean.class,String.class,Long.class).getAnnotation(Transactional.class);
        assertArrayEquals(new Class<?>[]{Exception.class},annotation.rollbackFor());
    }
    @Test void specializedAuditFailurePropagatesToTransactionalCallerWithoutSuccessReturn() {
        var after=rule();when(rules.selectListByScopeAndType("ROUTE",98L,"PQC_HANDOFF")).thenReturn(List.of(),List.of(after));
        when(rules.insert(any(MesProEdhrWorkTaskAssignmentRuleDO.class))).thenAnswer(i->{i.<MesProEdhrWorkTaskAssignmentRuleDO>getArgument(0).setId(901L);return 1;});
        when(operationAudit.recordInCallerTransaction(any())).thenThrow(new IllegalStateException("configuration audit failed"));
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            assertThrows(IllegalStateException.class,()->service.save(98L,"USER",344L,true,"明确派发本路线",null));
        }
        verify(operationAudit,never()).record(any());
    }
    @Test void staleExpectedRuleIdRejectsOverwriteBeforeCandidateValidationOrAudit() {
        when(rules.selectListByScopeAndType("ROUTE",98L,"PQC_HANDOFF")).thenReturn(List.of(rule()));
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            assertThrows(RuntimeException.class,()->service.save(98L,"USER",344L,true,"明确派发本路线",902L));
        }
        verify(rules,never()).updateById(any(MesProEdhrWorkTaskAssignmentRuleDO.class));verifyNoInteractions(owners,operationAudit);
    }
    @Test void updateKeepsSameExactRouteRuleAndAuditsOriginalAndNewSource() {
        var before=rule().setCandidateSourceId(339L);var after=rule().setRemark("改为正式接手人");
        when(rules.selectListByScopeAndType("ROUTE",98L,"PQC_HANDOFF")).thenReturn(List.of(before),List.of(after));
        when(rules.updateById(any(MesProEdhrWorkTaskAssignmentRuleDO.class))).thenReturn(1);
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            assertSame(after,service.save(98L,"USER",344L,true,"改为正式接手人",901L));
        }
        var row=ArgumentCaptor.forClass(MesProEdhrWorkTaskAssignmentRuleDO.class);verify(rules).updateById(row.capture());
        assertEquals(901L,row.getValue().getId());assertEquals(98L,row.getValue().getScopeId());assertNull(row.getValue().getRouteProcessId());
        var command=ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);verify(operationAudit).recordInCallerTransaction(command.capture());
        var metadata=JsonUtils.parseTree(command.getValue().getMetadataJson());assertEquals(339L,metadata.path("beforeRules").get(0).path("candidateSourceId").longValue());
        assertEquals(344L,metadata.path("afterRules").get(0).path("candidateSourceId").longValue());verify(rules,never()).insert(any(MesProEdhrWorkTaskAssignmentRuleDO.class));
    }
    @ParameterizedTest @ValueSource(strings={"disabledRoute","wrongRoute"})
    void invalidRouteIsRejectedBeforeResponsibilityQuery(String defect) {
        when(routes.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(MesProRouteDO.builder().id("wrongRoute".equals(defect)?99L:98L).status("disabledRoute".equals(defect)?1:0).build());
        assertThrows(RuntimeException.class,()->service.resolve(98L));verifyNoInteractions(rules,owners);
    }
    @Test void allFiveConfigurationEndpointsUseExistingRulePermissionsWithoutBusinessQaOrInspectorExpansion() throws Exception {
        var type=MesActiveOrderHandoffController.class;
        for(String name:List.of("pqcRoutes","pqcRoles")) assertEquals("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')",type.getMethod(name).getAnnotation(PreAuthorize.class).value());
        assertEquals("/pqc-route-options",type.getMethod("pqcRoutes").getAnnotation(GetMapping.class).value()[0]);
        assertEquals("/pqc-role-options",type.getMethod("pqcRoles").getAnnotation(GetMapping.class).value()[0]);
        assertEquals("/pqc-user-options",type.getMethod("pqcUsers",String.class).getAnnotation(GetMapping.class).value()[0]);
        assertEquals("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')",type.getMethod("pqcUsers",String.class).getAnnotation(PreAuthorize.class).value());
        assertEquals("/pqc-assignment",type.getMethod("getPqcRule",Long.class).getAnnotation(GetMapping.class).value()[0]);
        assertEquals("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')",type.getMethod("getPqcRule",Long.class).getAnnotation(PreAuthorize.class).value());
        var save=type.getMethod("savePqcRule",MesActiveOrderHandoffController.SaveRule.class);
        assertEquals("/pqc-assignment",save.getAnnotation(PostMapping.class).value()[0]);assertEquals("@ss.hasPermission('mes:pro-edhr-work-task-rule:update')",save.getAnnotation(PreAuthorize.class).value());
    }
    RoleDO roleFixture(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        when(rules.selectListByScopeAndType("ROUTE",98L,"PQC_HANDOFF")).thenReturn(List.of(rule));
        var role=new RoleDO();role.setId(700L);role.setName("正式PQC候选角色");role.setStatus(0);role.setTenantId(1L);
        when(roles.getRole(700L)).thenReturn(role);when(permissions.hasAnyPermissionsInRoles(Set.of(700L),MesPqcHandoffAssignmentService.CREATE_PERMISSION)).thenReturn(true);
        when(permissions.getUserRoleIdListByRoleIds(Set.of(700L))).thenReturn(Set.of(344L));return role;
    }
    static MesProEdhrWorkTaskAssignmentRuleDO rule() {
        return new MesProEdhrWorkTaskAssignmentRuleDO().setId(901L).setScopeType("ROUTE").setScopeId(98L).setTaskType("PQC_HANDOFF")
                .setCandidateSourceType("USER").setCandidateSourceId(344L).setEnabled(true);
    }
}
