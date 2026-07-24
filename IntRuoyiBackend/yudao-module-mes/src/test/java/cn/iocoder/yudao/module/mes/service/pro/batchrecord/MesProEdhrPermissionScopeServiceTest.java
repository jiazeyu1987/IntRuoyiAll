package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPermissionScopeMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_OBJECT_PERMISSION_DENIED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_CONTEXT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_SCOPE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_VERSION_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({MesProEdhrPermissionScopeServiceImpl.class, MesProEdhrPermissionGateServiceImpl.class})
class MesProEdhrPermissionScopeServiceTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrPermissionScopeService permissionScopeService;
    @Resource
    private MesProEdhrPermissionGateService permissionGateService;
    @Resource
    private MesProEdhrPermissionScopeMapper scopeMapper;
    @Resource
    private MesProEdhrPermissionRuleMapper ruleMapper;

    @MockitoBean
    private PermissionApi permissionApi;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;

    @Test
    void saveRules_createsScopeRulesAndRecordsAudit() {
        MesProEdhrPermissionScopeDetailResult result = permissionScopeService.saveRules(
                new MesProEdhrPermissionScopeSaveCommand()
                        .setScopeName("route-922045-internal-record")
                        .setObjectType("ROUTE")
                        .setObjectId("922045")
                        .setActorUserId(113L)
                        .setActorUsername("aoteman")
                        .setRules(List.of(
                                new MesProEdhrPermissionRuleCommand()
                                        .setSubjectType("USER")
                                        .setSubjectId(113L)
                                        .setAbility("VIEW")
                                        .setDecision("ALLOW")
                                        .setPriority(10),
                                new MesProEdhrPermissionRuleCommand()
                                        .setSubjectType("USER")
                                        .setSubjectId(113L)
                                        .setAbility("ROUTE_EDIT")
                                        .setDecision("ALLOW")
                                        .setPriority(10),
                                new MesProEdhrPermissionRuleCommand()
                                        .setSubjectType("USER")
                                        .setSubjectId(910245L)
                                        .setAbility("VIEW")
                                        .setDecision("DENY")
                                        .setPriority(1))));

        assertEquals("ROUTE", result.getObjectType());
        assertEquals("922045", result.getObjectId());
        assertEquals(1, result.getVersion());
        assertEquals(3, result.getRules().size());
        MesProEdhrPermissionEvaluateResult allowed = permissionScopeService.evaluate(
                new MesProEdhrPermissionEvaluateCommand()
                        .setScopeId(result.getScopeId())
                        .setAbilities(List.of("VIEW", "ROUTE_EDIT"))
                        .setActorUserId(113L));
        assertEquals("ALLOW", allowed.getDecisions().get("VIEW"));
        assertEquals("ALLOW", allowed.getDecisions().get("ROUTE_EDIT"));
        MesProEdhrPermissionEvaluateResult denied = permissionScopeService.evaluate(
                new MesProEdhrPermissionEvaluateCommand()
                        .setScopeId(result.getScopeId())
                        .setAbilities(List.of("VIEW"))
                        .setActorUserId(910245L));
        assertEquals("DENY", denied.getDecisions().get("VIEW"));
        verify(operationAuditService).record(argThat(audit ->
                "PERMISSION_RULE_SAVE".equals(audit.getOperationType())
                        && "ROUTE".equals(audit.getObjectType())
                        && "922045".equals(audit.getObjectId())
                        && "ALLOW".equals(audit.getPermissionDecision())
                        && "SUCCESS".equals(audit.getResultStatus())
                        && audit.getActorUserId().equals(113L)));
    }

    @Test
    void saveRules_updatesExistingScopeAndRejectsStaleVersion() {
        MesProEdhrPermissionScopeDetailResult created = permissionScopeService.saveRules(
                new MesProEdhrPermissionScopeSaveCommand()
                        .setScopeName("record-table-RPT-1")
                        .setObjectType("RECORD_TABLE")
                        .setObjectId("RPT-1")
                        .setActorUserId(113L)
                        .setRules(List.of(new MesProEdhrPermissionRuleCommand()
                                .setSubjectType("USER")
                                .setSubjectId(113L)
                                .setAbility("VIEW")
                                .setDecision("ALLOW")
                                .setPriority(10))));

        MesProEdhrPermissionScopeDetailResult updated = permissionScopeService.saveRules(
                new MesProEdhrPermissionScopeSaveCommand()
                        .setScopeId(created.getScopeId())
                        .setScopeName("record-table-RPT-1-updated")
                        .setObjectType("RECORD_TABLE")
                        .setObjectId("RPT-1")
                        .setExpectedVersion(1)
                        .setActorUserId(113L)
                        .setRules(List.of(new MesProEdhrPermissionRuleCommand()
                                .setSubjectType("USER")
                                .setSubjectId(113L)
                                .setAbility("FILL")
                                .setDecision("ALLOW")
                                .setPriority(10))));

        assertEquals(2, updated.getVersion());
        assertEquals(1, updated.getRules().size());
        assertEquals("FILL", updated.getRules().get(0).getAbility());
        ServiceException exception = assertThrows(ServiceException.class, () -> permissionScopeService.saveRules(
                new MesProEdhrPermissionScopeSaveCommand()
                        .setScopeId(created.getScopeId())
                        .setScopeName("record-table-RPT-1-stale")
                        .setObjectType("RECORD_TABLE")
                        .setObjectId("RPT-1")
                        .setExpectedVersion(1)
                        .setActorUserId(113L)
                        .setRules(List.of())));
        assertEquals(PRO_EDHR_PERMISSION_VERSION_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void saveRules_rejectsMissingScopeIdInsteadOfCreatingFallbackScope() {
        ServiceException exception = assertThrows(ServiceException.class, () -> permissionScopeService.saveRules(
                new MesProEdhrPermissionScopeSaveCommand()
                        .setScopeId(999_999L)
                        .setScopeName("missing-scope")
                        .setObjectType("RECORD_TABLE")
                        .setObjectId("RPT-MISSING")
                        .setActorUserId(113L)
                        .setRules(List.of(new MesProEdhrPermissionRuleCommand()
                                .setSubjectType("USER")
                                .setSubjectId(113L)
                                .setAbility("VIEW")
                                .setDecision("ALLOW")
                                .setPriority(10)))));
        assertEquals(PRO_EDHR_PERMISSION_SCOPE_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void getDetail_returnsScopeByObjectWithRules() {
        MesProEdhrPermissionScopeDetailResult saved = permissionScopeService.saveRules(
                new MesProEdhrPermissionScopeSaveCommand()
                        .setScopeName("execution-8001")
                        .setObjectType("BATCH_RECORD_EXECUTION")
                        .setObjectId("8001")
                        .setActorUserId(113L)
                        .setRules(List.of(new MesProEdhrPermissionRuleCommand()
                                .setSubjectType("USER")
                                .setSubjectId(113L)
                                .setAbility("SIGN")
                                .setDecision("ALLOW")
                                .setPriority(20))));

        MesProEdhrPermissionScopeDetailResult detail = permissionScopeService.getDetail(
                new MesProEdhrPermissionScopeQueryCommand()
                        .setObjectType("BATCH_RECORD_EXECUTION")
                        .setObjectId("8001"));

        assertEquals(saved.getScopeId(), detail.getScopeId());
        assertEquals("SIGN", detail.getRules().get(0).getAbility());
    }

    @Test
    void evaluate_userRoleDeptRulesAndDefaultDeny() {
        insertScope(1001L, "RECORD_TABLE", "RPT-4001", null);
        insertRule(11L, 1001L, "USER", 101L, "VIEW", "ALLOW", 10);
        insertRule(12L, 1001L, "USER", 101L, "FILL", "DENY", 1);
        insertRule(13L, 1001L, "USER", 101L, "FILL", "ALLOW", 10);
        insertRule(14L, 1001L, "ROLE", 7001L, "SIGN", "ALLOW", 10);
        insertRule(15L, 1001L, "DEPT", 8001L, "APPROVE", "ALLOW", 10);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(7001L))).thenReturn(Set.of(101L));
        when(deptApi.getChildDeptList(8001L)).thenReturn(List.of());

        MesProEdhrPermissionEvaluateResult result = permissionScopeService.evaluate(
                new MesProEdhrPermissionEvaluateCommand()
                        .setObjectType("RECORD_TABLE")
                        .setObjectId("RPT-4001")
                        .setAbilities(List.of("VIEW", "FILL", "SIGN", "APPROVE", "ARCHIVE"))
                        .setActorUserId(101L)
                        .setActorDeptId(8001L));

        assertEquals("ALLOW", result.getDecisions().get("VIEW"));
        assertEquals("DENY", result.getDecisions().get("FILL"));
        assertEquals("ALLOW", result.getDecisions().get("SIGN"));
        assertEquals("ALLOW", result.getDecisions().get("APPROVE"));
        assertEquals("DENY", result.getDecisions().get("ARCHIVE"));
        assertTrue(result.getMatchedRuleIds().containsAll(List.of(11L, 12L, 14L, 15L)));
        verify(operationAuditService).record(argThat(audit ->
                "PERMISSION_EVALUATE".equals(audit.getOperationType())
                        && "RECORD_TABLE".equals(audit.getObjectType())
                        && "RPT-4001".equals(audit.getObjectId())
                        && "DENY".equals(audit.getPermissionDecision())
                        && "REJECTED".equals(audit.getResultStatus())
                        && audit.getMatchedRuleIds().contains("12")));
    }

    @Test
    void requireAbility_deniedRecordsDeniedAuditAndThrows() {
        insertScope(1002L, "RECORD_TABLE", "RPT-4002", null);

        ServiceException exception = assertThrows(ServiceException.class, () -> permissionGateService.requireAbility(
                new MesProEdhrPermissionGateCommand()
                        .setObjectType("RECORD_TABLE")
                        .setObjectId("RPT-4002")
                        .setAbility("VIEW")
                        .setActorUserId(202L)
                        .setActorDeptId(9001L)
                        .setPermissionCode("mes:pro-batch-record-execution:query")));
        assertEquals(PRO_EDHR_OBJECT_PERMISSION_DENIED.getCode(), exception.getCode());
        verify(operationAuditService).record(argThat(audit ->
                "PERMISSION_EVALUATE".equals(audit.getOperationType())
                        && "DENY".equals(audit.getPermissionDecision())
                        && "REJECTED".equals(audit.getResultStatus())));
    }

    @Test
    void requireAbility_superAdminBypassesObjectRules() {
        insertScope(1004L, "RECORD_TABLE", "RPT-4004", null);
        when(permissionApi.hasAnyRoles(1L, RoleCodeEnum.SUPER_ADMIN.getCode())).thenReturn(true);

        permissionGateService.requireAbility(new MesProEdhrPermissionGateCommand()
                .setObjectType("RECORD_TABLE")
                .setObjectId("RPT-4004")
                .setAbility("FILL")
                .setActorUserId(1L)
                .setActorDeptId(9004L)
                .setPermissionCode("mes:pro-batch-record-execution:field-audit-update"));

        verify(operationAuditService).record(argThat(audit ->
                "PERMISSION_EVALUATE".equals(audit.getOperationType())
                        && "ALLOW".equals(audit.getPermissionDecision())
                        && "SUCCESS".equals(audit.getResultStatus())
                        && "RECORD_TABLE".equals(audit.getObjectType())
                        && "RPT-4004".equals(audit.getObjectId())));
    }

    @Test
    void evaluate_adminUserCanViewAnyBatchExecutionWithoutScope() {
        when(adminUserApi.getUser(1L)).thenReturn(adminUser(1L, "admin"));

        MesProEdhrPermissionEvaluateResult result = permissionScopeService.evaluate(
                new MesProEdhrPermissionEvaluateCommand()
                        .setObjectType("BATCH_RECORD_EXECUTION")
                        .setObjectId("9001")
                        .setAbilities(List.of("VIEW"))
                        .setActorUserId(1L));

        assertEquals("ALLOW", result.getDecisions().get("VIEW"));
        verify(operationAuditService).record(argThat(audit ->
                "PERMISSION_EVALUATE".equals(audit.getOperationType())
                        && "ALLOW".equals(audit.getPermissionDecision())
                        && "SUCCESS".equals(audit.getResultStatus())
                        && "BATCH_RECORD_EXECUTION".equals(audit.getObjectType())
                        && "9001".equals(audit.getObjectId())
                        && audit.getMetadataJson().contains("\"adminReadonlyBypass\":true")));
    }

    @Test
    void evaluate_adminUserReadonlyDoesNotAllowFill() {
        when(adminUserApi.getUser(1L)).thenReturn(adminUser(1L, "admin"));

        MesProEdhrPermissionEvaluateResult result = permissionScopeService.evaluate(
                new MesProEdhrPermissionEvaluateCommand()
                        .setObjectType("BATCH_RECORD_EXECUTION")
                        .setObjectId("9001")
                        .setAbilities(List.of("FILL"))
                        .setActorUserId(1L));

        assertEquals("DENY", result.getDecisions().get("FILL"));
    }

    @Test
    void evaluate_malformedRuleFailsFast() {
        insertScope(1003L, "RECORD_TABLE", "RPT-4003", null);
        insertRule(31L, 1003L, "TEAM", 9003L, "VIEW", "ALLOW", 1);

        ServiceException exception = assertThrows(ServiceException.class, () -> permissionScopeService.evaluate(
                new MesProEdhrPermissionEvaluateCommand()
                        .setObjectType("RECORD_TABLE")
                        .setObjectId("RPT-4003")
                        .setAbilities(List.of("VIEW"))
                        .setActorUserId(303L)
                        .setActorDeptId(9003L)));

        assertEquals(PRO_EDHR_PERMISSION_CONTEXT_MISSING.getCode(), exception.getCode());
    }

    private void insertScope(Long id, String objectType, String objectId, Long parentScopeId) {
        scopeMapper.insert(new MesProEdhrPermissionScopeDO()
                .setId(id)
                .setScopeName(objectType + "-" + objectId)
                .setObjectType(objectType)
                .setObjectId(objectId)
                .setParentScopeId(parentScopeId)
                .setStatus("ENABLED")
                .setVersion(1));
    }

    private void insertRule(Long id, Long scopeId, String subjectType, Long subjectId,
                            String ability, String decision, Integer priority) {
        ruleMapper.insert(new MesProEdhrPermissionRuleDO()
                .setId(id)
                .setScopeId(scopeId)
                .setSubjectType(subjectType)
                .setSubjectId(subjectId)
                .setAbility(ability)
                .setDecision(decision)
                .setPriority(priority)
                .setEffectiveFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .setEffectiveTo(LocalDateTime.of(2026, 12, 31, 23, 59))
                .setStatus("ENABLED")
                .setVersion(1));
    }

    private AdminUserRespDTO adminUser(Long userId, String username) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setUsername(username);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return user;
    }
}
