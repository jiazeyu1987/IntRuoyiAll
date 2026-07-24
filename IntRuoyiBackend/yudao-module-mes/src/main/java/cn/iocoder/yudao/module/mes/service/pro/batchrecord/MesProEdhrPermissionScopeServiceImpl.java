package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPermissionScopeMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_CONTEXT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_SCOPE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_VERSION_CONFLICT;

@Service
public class MesProEdhrPermissionScopeServiceImpl implements MesProEdhrPermissionScopeService {

    private static final String DECISION_ALLOW = "ALLOW";
    private static final String DECISION_DENY = "DENY";
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String OBJECT_TYPE_BATCH_RECORD_EXECUTION = "BATCH_RECORD_EXECUTION";
    private static final String ABILITY_VIEW = "VIEW";
    private static final String ADMIN_USERNAME = "admin";
    private static final String OPERATION_PERMISSION_EVALUATE = "PERMISSION_EVALUATE";
    private static final String OPERATION_PERMISSION_RULE_SAVE = "PERMISSION_RULE_SAVE";
    private static final Set<String> SUPPORTED_SUBJECT_TYPES = Set.of("USER", "ROLE", "DEPT");
    private static final Set<String> SUPPORTED_DECISIONS = Set.of(DECISION_ALLOW, DECISION_DENY);
    private static final Set<String> SUPPORTED_ABILITIES = Set.of(
            "VIEW", "FILL", "EQUIPMENT_FILL", "QUALITY_FILL", "SIGN", "APPROVE",
            "ARCHIVE", "AUDIT_VIEW", "ROUTE_EDIT", "PERMISSION_ADMIN");

    @Resource
    private MesProEdhrPermissionScopeMapper scopeMapper;
    @Resource
    private MesProEdhrPermissionRuleMapper ruleMapper;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrPermissionScopeDetailResult saveRules(MesProEdhrPermissionScopeSaveCommand command) {
        validateSaveCommand(command);
        Long actorUserId = resolveActorUserId(command.getActorUserId());
        MesProEdhrPermissionScopeDO scope = resolveExistingScope(command);
        if (scope == null) {
            scope = new MesProEdhrPermissionScopeDO()
                    .setScopeName(StrUtil.trim(command.getScopeName()))
                    .setObjectType(StrUtil.trim(command.getObjectType()))
                    .setObjectId(StrUtil.trim(command.getObjectId()))
                    .setParentScopeId(command.getParentScopeId())
                    .setStatus(STATUS_ENABLED)
                    .setVersion(1)
                    .setCreateUserId(actorUserId)
                    .setUpdateUserId(actorUserId);
            scopeMapper.insert(scope);
        } else {
            if (command.getExpectedVersion() != null
                    && !Objects.equals(scope.getVersion(), command.getExpectedVersion())) {
                throw exception(PRO_EDHR_PERMISSION_VERSION_CONFLICT, scope.getVersion(), command.getExpectedVersion());
            }
            scope.setScopeName(StrUtil.trim(command.getScopeName()))
                    .setObjectType(StrUtil.trim(command.getObjectType()))
                    .setObjectId(StrUtil.trim(command.getObjectId()))
                    .setParentScopeId(command.getParentScopeId())
                    .setStatus(STATUS_ENABLED)
                    .setVersion(scope.getVersion() == null ? 1 : scope.getVersion() + 1)
                    .setUpdateUserId(actorUserId);
            scopeMapper.updateById(scope);
            ruleMapper.deleteByScopeId(scope.getId());
        }
        List<MesProEdhrPermissionRuleCommand> rules = command.getRules() == null ? List.of() : command.getRules();
        for (MesProEdhrPermissionRuleCommand rule : rules) {
            validateRuleCommand(rule);
            ruleMapper.insert(new MesProEdhrPermissionRuleDO()
                    .setScopeId(scope.getId())
                    .setSubjectType(StrUtil.trim(rule.getSubjectType()))
                    .setSubjectId(rule.getSubjectId())
                    .setAbility(StrUtil.trim(rule.getAbility()))
                    .setDecision(StrUtil.trim(rule.getDecision()))
                    .setPriority(rule.getPriority() == null ? 100 : rule.getPriority())
                    .setEffectiveFrom(rule.getEffectiveFrom())
                    .setEffectiveTo(rule.getEffectiveTo())
                    .setStatus(StrUtil.blankToDefault(StrUtil.trim(rule.getStatus()), STATUS_ENABLED))
                    .setVersion(1)
                    .setCreateUserId(actorUserId)
                    .setUpdateUserId(actorUserId));
        }
        MesProEdhrOperationAuditRespVO audit = operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-PERM-SAVE-" + java.util.UUID.randomUUID())
                .setObjectType(scope.getObjectType())
                .setObjectId(scope.getObjectId())
                .setOperationType(OPERATION_PERMISSION_RULE_SAVE)
                .setActionName("保存 eDHR 对象级权限规则")
                .setActorUserId(actorUserId)
                .setActorUsername(StrUtil.blankToDefault(command.getActorUsername(),
                        SecurityFrameworkUtils.getLoginUserNickname()))
                .setPermissionCode("mes:pro-edhr-permission-scope:save")
                .setPermissionDecision(DECISION_ALLOW)
                .setResultStatus("SUCCESS")
                .setMetadataJson(JsonUtils.toJsonString(Map.of(
                        "scopeId", scope.getId(),
                        "version", scope.getVersion(),
                        "ruleCount", rules.size()))));
        return toDetail(scope, audit == null ? null : audit.getId());
    }

    @Override
    public MesProEdhrPermissionScopeDetailResult getDetail(MesProEdhrPermissionScopeQueryCommand command) {
        validateQueryCommand(command);
        MesProEdhrPermissionScopeDO scope = resolveScope(new MesProEdhrPermissionEvaluateCommand()
                .setScopeId(command.getScopeId())
                .setObjectType(command.getObjectType())
                .setObjectId(command.getObjectId())
                .setAbilities(List.of("VIEW")));
        if (scope == null) {
            throw exception(PRO_EDHR_PERMISSION_SCOPE_REQUIRED,
                    command.getScopeId() == null ? command.getObjectType() + ":" + command.getObjectId()
                            : command.getScopeId());
        }
        return toDetail(scope, null);
    }

    @Override
    public MesProEdhrPermissionEvaluateResult evaluate(MesProEdhrPermissionEvaluateCommand command) {
        validateCommand(command);
        Long actorUserId = command.getActorUserId() != null
                ? command.getActorUserId() : SecurityFrameworkUtils.getLoginUserId();
        if (actorUserId == null) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        Long actorDeptId = command.getActorDeptId() != null
                ? command.getActorDeptId() : SecurityFrameworkUtils.getLoginUserDeptId();
        MesProEdhrPermissionScopeDO scope = resolveScope(command);
        List<MesProEdhrPermissionScopeDO> scopeChain = scope == null ? List.of() : buildScopeChain(scope);
        boolean superAdminBypass = scope != null
                && permissionApi.hasAnyRoles(actorUserId, RoleCodeEnum.SUPER_ADMIN.getCode());
        boolean adminReadonlyBypass = isAdminReadonlyViewBypass(scope, command, actorUserId);

        Map<String, String> decisions = new LinkedHashMap<>();
        Set<Long> matchedRuleIds = new LinkedHashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (String ability : command.getAbilities()) {
            RuleDecision decision = superAdminBypass
                    ? new RuleDecision(DECISION_ALLOW, null)
                    : adminReadonlyBypass && ABILITY_VIEW.equals(ability)
                    ? new RuleDecision(DECISION_ALLOW, null)
                    : decide(scopeChain, ability, actorUserId, actorDeptId, now);
            decisions.put(ability, decision.decision());
            if (decision.ruleId() != null) {
                matchedRuleIds.add(decision.ruleId());
            }
        }
        boolean allAllowed = decisions.values().stream().allMatch(DECISION_ALLOW::equals);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scopeId", scope == null ? null : scope.getId());
        metadata.put("abilities", command.getAbilities());
        metadata.put("decisions", decisions);
        metadata.put("matchedRuleIds", matchedRuleIds);
        metadata.put("superAdminBypass", superAdminBypass);
        metadata.put("adminReadonlyBypass", adminReadonlyBypass);
        MesProEdhrOperationAuditRespVO audit = operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-PERM-" + java.util.UUID.randomUUID())
                .setObjectType(scope != null ? scope.getObjectType() : command.getObjectType())
                .setObjectId(scope != null ? scope.getObjectId() : command.getObjectId())
                .setBatchExecutionId(command.getBatchExecutionId())
                .setExecutionId(command.getExecutionId())
                .setWorkTaskId(command.getWorkTaskId())
                .setRouteId(command.getRouteId())
                .setRouteProcessId(command.getRouteProcessId())
                .setReportId(command.getReportId())
                .setRecordCategory(command.getRecordCategory())
                .setOperationType(OPERATION_PERMISSION_EVALUATE)
                .setActionName(StrUtil.blankToDefault(command.getActionName(), "计算 eDHR 对象级权限"))
                .setActorUserId(actorUserId)
                .setActorUsername(StrUtil.blankToDefault(command.getActorUsername(),
                        SecurityFrameworkUtils.getLoginUserNickname()))
                .setPermissionCode(StrUtil.blankToDefault(command.getPermissionCode(),
                        "mes:pro-edhr-permission-scope:evaluate"))
                .setPermissionDecision(allAllowed ? DECISION_ALLOW : DECISION_DENY)
                .setMatchedRuleIds(joinRuleIds(matchedRuleIds))
                .setResultStatus(allAllowed ? "SUCCESS" : "REJECTED")
                .setMetadataJson(JsonUtils.toJsonString(metadata)));
        return new MesProEdhrPermissionEvaluateResult()
                .setScopeId(scope == null ? null : scope.getId())
                .setObjectType(scope != null ? scope.getObjectType() : command.getObjectType())
                .setObjectId(scope != null ? scope.getObjectId() : command.getObjectId())
                .setDecisions(decisions)
                .setMatchedRuleIds(new ArrayList<>(matchedRuleIds))
                .setOperationAuditEventId(audit == null ? null : audit.getId());
    }

    private void validateSaveCommand(MesProEdhrPermissionScopeSaveCommand command) {
        if (command == null || StrUtil.isBlank(command.getScopeName()) || StrUtil.isBlank(command.getObjectType())
                || StrUtil.isBlank(command.getObjectId())) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        if (command.getRules() != null) {
            command.getRules().forEach(this::validateRuleCommand);
        }
    }

    private void validateQueryCommand(MesProEdhrPermissionScopeQueryCommand command) {
        if (command == null || (command.getScopeId() == null
                && (StrUtil.isBlank(command.getObjectType()) || StrUtil.isBlank(command.getObjectId())))) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
    }

    private void validateCommand(MesProEdhrPermissionEvaluateCommand command) {
        if (command == null || CollUtil.isEmpty(command.getAbilities())) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        if (command.getScopeId() == null
                && (StrUtil.isBlank(command.getObjectType()) || StrUtil.isBlank(command.getObjectId()))) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
    }

    private Long resolveActorUserId(Long actorUserId) {
        Long resolved = actorUserId != null ? actorUserId : SecurityFrameworkUtils.getLoginUserId();
        if (resolved == null) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        return resolved;
    }

    private MesProEdhrPermissionScopeDO resolveExistingScope(MesProEdhrPermissionScopeSaveCommand command) {
        if (command.getScopeId() != null) {
            MesProEdhrPermissionScopeDO scope = scopeMapper.selectById(command.getScopeId());
            if (scope == null) {
                throw exception(PRO_EDHR_PERMISSION_SCOPE_REQUIRED, command.getScopeId());
            }
            return scope;
        }
        return scopeMapper.selectByObject(StrUtil.trim(command.getObjectType()), StrUtil.trim(command.getObjectId()));
    }

    private MesProEdhrPermissionScopeDO resolveScope(MesProEdhrPermissionEvaluateCommand command) {
        MesProEdhrPermissionScopeDO scope;
        if (command.getScopeId() != null) {
            scope = scopeMapper.selectById(command.getScopeId());
        } else {
            scope = scopeMapper.selectByObject(command.getObjectType(), command.getObjectId());
        }
        if (scope == null) {
            return null;
        }
        if (!STATUS_ENABLED.equals(scope.getStatus())) {
            throw exception(PRO_EDHR_PERMISSION_SCOPE_REQUIRED, scope.getId());
        }
        return scope;
    }

    private List<MesProEdhrPermissionScopeDO> buildScopeChain(MesProEdhrPermissionScopeDO scope) {
        List<MesProEdhrPermissionScopeDO> chain = new ArrayList<>();
        MesProEdhrPermissionScopeDO current = scope;
        while (current != null) {
            chain.add(current);
            Long parentScopeId = current.getParentScopeId();
            if (parentScopeId == null) {
                break;
            }
            current = scopeMapper.selectById(parentScopeId);
            if (current == null || !STATUS_ENABLED.equals(current.getStatus())) {
                throw exception(PRO_EDHR_PERMISSION_SCOPE_REQUIRED, parentScopeId);
            }
        }
        return chain;
    }

    private RuleDecision decide(List<MesProEdhrPermissionScopeDO> scopeChain, String ability, Long actorUserId,
                                Long actorDeptId, LocalDateTime now) {
        for (MesProEdhrPermissionScopeDO scope : scopeChain) {
            List<MesProEdhrPermissionRuleDO> rules = ruleMapper.selectEnabledListByScopeAndAbilities(
                    scope.getId(), List.of(ability));
            for (MesProEdhrPermissionRuleDO rule : rules) {
                validateRule(rule);
                if (isRuleEffective(rule, now) && matchesSubject(rule, actorUserId, actorDeptId)) {
                    return new RuleDecision(rule.getDecision(), rule.getId());
                }
            }
        }
        return new RuleDecision(DECISION_DENY, null);
    }

    private void validateRuleCommand(MesProEdhrPermissionRuleCommand rule) {
        if (rule == null || StrUtil.isBlank(rule.getSubjectType()) || rule.getSubjectId() == null
                || StrUtil.isBlank(rule.getAbility()) || StrUtil.isBlank(rule.getDecision())
                || !SUPPORTED_SUBJECT_TYPES.contains(StrUtil.trim(rule.getSubjectType()))
                || !SUPPORTED_ABILITIES.contains(StrUtil.trim(rule.getAbility()))
                || !SUPPORTED_DECISIONS.contains(StrUtil.trim(rule.getDecision()))) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
    }

    private void validateRule(MesProEdhrPermissionRuleDO rule) {
        if (rule == null || rule.getScopeId() == null || StrUtil.isBlank(rule.getSubjectType())
                || rule.getSubjectId() == null || StrUtil.isBlank(rule.getAbility())
                || StrUtil.isBlank(rule.getDecision()) || !SUPPORTED_SUBJECT_TYPES.contains(rule.getSubjectType())
                || !SUPPORTED_DECISIONS.contains(rule.getDecision())) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
    }

    private boolean isRuleEffective(MesProEdhrPermissionRuleDO rule, LocalDateTime now) {
        return (rule.getEffectiveFrom() == null || !now.isBefore(rule.getEffectiveFrom()))
                && (rule.getEffectiveTo() == null || !now.isAfter(rule.getEffectiveTo()));
    }

    private boolean matchesSubject(MesProEdhrPermissionRuleDO rule, Long actorUserId, Long actorDeptId) {
        if ("USER".equals(rule.getSubjectType())) {
            return Objects.equals(rule.getSubjectId(), actorUserId);
        }
        if ("ROLE".equals(rule.getSubjectType())) {
            return permissionApi.getUserRoleIdListByRoleIds(Set.of(rule.getSubjectId())).contains(actorUserId);
        }
        if ("DEPT".equals(rule.getSubjectType())) {
            return matchesDepartment(rule.getSubjectId(), actorDeptId);
        }
        return false;
    }

    private boolean matchesDepartment(Long subjectDeptId, Long actorDeptId) {
        if (subjectDeptId == null || actorDeptId == null) {
            return false;
        }
        if (Objects.equals(subjectDeptId, actorDeptId)) {
            return true;
        }
        return deptApi.getChildDeptList(subjectDeptId).stream()
                .map(DeptRespDTO::getId)
                .anyMatch(actorDeptId::equals);
    }

    private boolean isAdminReadonlyViewBypass(MesProEdhrPermissionScopeDO scope,
                                              MesProEdhrPermissionEvaluateCommand command,
                                              Long actorUserId) {
        if (!command.getAbilities().contains(ABILITY_VIEW)) {
            return false;
        }
        String objectType = scope == null ? command.getObjectType() : scope.getObjectType();
        if (!OBJECT_TYPE_BATCH_RECORD_EXECUTION.equals(objectType)) {
            return false;
        }
        AdminUserRespDTO user = adminUserApi.getUser(actorUserId);
        return user != null
                && ADMIN_USERNAME.equals(user.getUsername())
                && CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus());
    }

    private String joinRuleIds(Set<Long> matchedRuleIds) {
        return matchedRuleIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private MesProEdhrPermissionScopeDetailResult toDetail(MesProEdhrPermissionScopeDO scope,
                                                           Long operationAuditEventId) {
        return new MesProEdhrPermissionScopeDetailResult()
                .setScopeId(scope.getId())
                .setScopeName(scope.getScopeName())
                .setObjectType(scope.getObjectType())
                .setObjectId(scope.getObjectId())
                .setParentScopeId(scope.getParentScopeId())
                .setStatus(scope.getStatus())
                .setVersion(scope.getVersion())
                .setRules(ruleMapper.selectListByScopeId(scope.getId()).stream().map(this::toRuleResult).toList())
                .setOperationAuditEventId(operationAuditEventId);
    }

    private MesProEdhrPermissionRuleResult toRuleResult(MesProEdhrPermissionRuleDO rule) {
        return new MesProEdhrPermissionRuleResult()
                .setId(rule.getId())
                .setScopeId(rule.getScopeId())
                .setSubjectType(rule.getSubjectType())
                .setSubjectId(rule.getSubjectId())
                .setAbility(rule.getAbility())
                .setDecision(rule.getDecision())
                .setPriority(rule.getPriority())
                .setEffectiveFrom(rule.getEffectiveFrom())
                .setEffectiveTo(rule.getEffectiveTo())
                .setStatus(rule.getStatus())
                .setVersion(rule.getVersion());
    }

    private record RuleDecision(String decision, Long ruleId) {
    }
}
