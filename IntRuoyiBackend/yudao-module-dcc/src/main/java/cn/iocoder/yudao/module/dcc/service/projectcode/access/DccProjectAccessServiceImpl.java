package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.access.DccProjectAccessRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.PostApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_RULE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;

@Service
public class DccProjectAccessServiceImpl implements DccProjectAccessService {

    private static final Set<String> SUBJECT_TYPES = Set.of("USER", "DEPT", "ROLE", "POSITION");
    private static final Set<String> ACCESS_LEVELS = Set.of("OWNER", "EDIT", "VIEW");

    @Resource
    private DccProjectAccessRuleMapper accessRuleMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private RoleApi roleApi;
    @Resource
    private PostApi postApi;
    @Resource
    private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper assignmentMapper;

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY,
            rollbackFor = Exception.class)
    public void initializeApprovedProjectLeaderOwner(Long projectCodeId, Long selectedLeaderUserId, String reason) {
        Long tenant = cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        var project = projectCodeId == null ? null : projectCodeMapper.selectByIdForUpdate(projectCodeId);
        AdminUserRespDTO leader = selectedLeaderUserId == null ? null : adminUserApi.getUser(selectedLeaderUserId);
        if (project == null || !java.util.Objects.equals(project.getTenantId(), tenant)
                || !"ENABLE".equals(project.getStatus())
                || !java.util.Objects.equals(project.getProjectLeaderUserId(), selectedLeaderUserId)
                || leader == null || !java.util.Objects.equals(leader.getId(), selectedLeaderUserId)
                || !java.util.Objects.equals(leader.getTenantId(), tenant) || !Integer.valueOf(0).equals(leader.getStatus())
                || reason == null || reason.isBlank() || reason.length() > 500) {
            throw exception(DCC_PROJECT_ACCESS_RULE_INVALID);
        }
        if (!accessRuleMapper.selectListByProjectCodeId(projectCodeId).isEmpty()) {
            throw exception(DCC_PROJECT_ACCESS_RULE_INVALID);
        }
        var rule = new DccProjectAccessRuleDO();
        rule.setTenantId(tenant);
        rule.setDccProjectCodeId(projectCodeId);
        rule.setSubjectType("USER");
        rule.setSubjectId(selectedLeaderUserId);
        rule.setAccessLevel("OWNER");
        rule.setActive(true);
        rule.setChangeReason(reason.trim());
        if (accessRuleMapper.insert(rule) != 1 || rule.getId() == null) {
            throw new IllegalStateException("approved project initial owner rule persistence incomplete");
        }
    }

    @Override
    public List<DccProjectAccessRuleDO> getProjectAccessRules(Long projectCodeId) {
        validateProjectCodeExists(projectCodeId);
        return accessRuleMapper.selectListByProjectCodeId(projectCodeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DccProjectAccessRuleDO> replaceProjectAccessRules(Long projectCodeId,
                                                                  List<DccProjectAccessRuleSaveReqVO> rules) {
        validateProjectCodeExists(projectCodeId);
        List<DccProjectAccessRuleDO> normalizedRules = normalizeRules(projectCodeId, rules);
        if (normalizedRules.stream().noneMatch(this::isCurrentlyActiveOwner)) {
            throw exception(DCC_PROJECT_ACCESS_RULE_INVALID);
        }
        accessRuleMapper.deleteByProjectCodeId(projectCodeId);
        normalizedRules.forEach(accessRuleMapper::insert);
        return normalizedRules;
    }

    @Override
    public boolean hasProjectOwner(Long userId, Long projectCodeId) {
        return hasProjectAccess(userId, projectCodeId, Set.of("OWNER"));
    }

    @Override
    public boolean hasProjectEditorOrOwner(Long userId, Long projectCodeId) {
        return hasProjectAccess(userId, projectCodeId, Set.of("OWNER", "EDIT"));
    }

    @Override
    public void assertProjectOwner(Long userId, Long projectCodeId) {
        assertProjectAccess(userId, projectCodeId, Set.of("OWNER"));
    }

    @Override
    public void assertProjectEditorOrOwner(Long userId, Long projectCodeId) {
        assertProjectAccess(userId, projectCodeId, Set.of("OWNER", "EDIT"));
    }

    @Override
    public void assertProjectViewerOrAbove(Long userId, Long projectCodeId) {
        Long tenant = cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        var project = projectCodeId == null || projectCodeId <= 0 ? null : projectCodeMapper.selectById(projectCodeId);
        if (project == null || !java.util.Objects.equals(project.getTenantId(), tenant)
                || !"ENABLE".equals(project.getStatus())
                || !hasProjectAccess(userId, projectCodeId, Set.of("VIEW", "EDIT", "OWNER"), tenant)
                || !withinAssignedProjectScope(userId, projectCodeId, tenant)) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
    }

    @Override
    public List<Long> listReadableProjectIds(Long userId) {
        Long tenant = cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        AdminUserRespDTO user = userId == null ? null : adminUserApi.getUser(userId);
        if (user == null || !java.util.Objects.equals(user.getStatus(), 0)) throw exception(DCC_PROJECT_ACCESS_DENIED);
        Set<Long> roleIds = permissionApi.getUserRoleIdListByUserId(userId);
        Set<Long> deptIds = collectDeptIds(user.getDeptId());
        Set<Long> allowed = accessRuleMapper.selectActiveReaderRules(tenant, LocalDateTime.now()).stream()
                .filter(rule -> matches(rule, userId, user, roleIds, deptIds))
                .map(DccProjectAccessRuleDO::getDccProjectCodeId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        List<Long> hardScope = assignedProjectScope(userId, tenant);
        if (hardScope != null) allowed.retainAll(hardScope);
        return List.copyOf(allowed);
    }

    private boolean withinAssignedProjectScope(Long userId, Long projectId, Long tenant) {
        List<Long> hardScope = assignedProjectScope(userId, tenant);
        return hardScope == null || hardScope.contains(projectId);
    }

    private List<Long> assignedProjectScope(Long userId, Long tenant) {
        // scope:all removes only the pre-existing assignment restriction, never the formal project rule.
        if (permissionApi.hasAnyPermissions(userId, "dcc:project-code:scope:all")
                || !permissionApi.hasAnyPermissions(userId, "dcc:project-code-assignment:execute")) return null;
        return java.util.Objects.requireNonNull(assignmentMapper.selectActiveProjectCodeIdsByAssigneeUserId(tenant, userId, LocalDateTime.now()),
                "active assigned project scope");
    }

    private void assertProjectAccess(Long userId, Long projectCodeId, Set<String> allowedAccessLevels) {
        if (!hasProjectAccess(userId, projectCodeId, allowedAccessLevels)) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
    }

    private boolean hasProjectAccess(Long userId, Long projectCodeId, Set<String> allowedAccessLevels) {
        return hasProjectAccess(userId, projectCodeId, allowedAccessLevels, null);
    }

    private boolean hasProjectAccess(Long userId, Long projectCodeId, Set<String> allowedAccessLevels, Long requiredTenant) {
        if (userId == null || projectCodeId == null) {
            return false;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null || !java.util.Objects.equals(user.getStatus(), 0)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        Set<Long> roleIds = permissionApi.getUserRoleIdListByUserId(userId);
        Set<Long> deptIds = collectDeptIds(user.getDeptId());
        return accessRuleMapper.selectActiveRules(projectCodeId, now).stream()
                .filter(rule -> requiredTenant == null || java.util.Objects.equals(rule.getTenantId(), requiredTenant))
                .filter(rule -> rule.getAccessLevel() != null
                        && allowedAccessLevels.stream().anyMatch(level -> level.equalsIgnoreCase(rule.getAccessLevel())))
                .anyMatch(rule -> matches(rule, userId, user, roleIds, deptIds));
    }

    private boolean matches(DccProjectAccessRuleDO rule, Long userId, AdminUserRespDTO user,
                            Set<Long> roleIds, Set<Long> deptIds) {
        String type = rule.getSubjectType();
        if ("USER".equalsIgnoreCase(type)) return userId.equals(rule.getSubjectId());
        if ("DEPT".equalsIgnoreCase(type)) return deptIds.contains(rule.getSubjectId());
        if ("ROLE".equalsIgnoreCase(type)) return roleIds.contains(rule.getSubjectId());
        if ("POSITION".equalsIgnoreCase(type)) {
            return user.getPostIds() != null && user.getPostIds().contains(rule.getSubjectId());
        }
        return false;
    }

    private Set<Long> collectDeptIds(Long deptId) {
        Set<Long> ids = new HashSet<>();
        Long current = deptId;
        while (current != null && current > 0 && ids.add(current)) {
            DeptRespDTO dept = deptApi.getDept(current);
            current = dept == null ? null : dept.getParentId();
        }
        return ids;
    }

    private void validateProjectCodeExists(Long projectCodeId) {
        if (projectCodeId == null || projectCodeMapper.selectById(projectCodeId) == null) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
    }

    private List<DccProjectAccessRuleDO> normalizeRules(Long projectCodeId,
                                                        List<DccProjectAccessRuleSaveReqVO> rules) {
        if (rules == null || rules.isEmpty()) {
            throw exception(DCC_PROJECT_ACCESS_RULE_INVALID);
        }
        List<DccProjectAccessRuleDO> normalizedRules = new ArrayList<>(rules.size());
        for (DccProjectAccessRuleSaveReqVO rule : rules) {
            normalizedRules.add(normalizeRule(projectCodeId, rule));
        }
        return normalizedRules;
    }

    private DccProjectAccessRuleDO normalizeRule(Long projectCodeId, DccProjectAccessRuleSaveReqVO rule) {
        if (rule == null || rule.getSubjectId() == null || rule.getSubjectId() <= 0) {
            throw exception(DCC_PROJECT_ACCESS_RULE_INVALID);
        }
        String subjectType = normalizeToken(rule.getSubjectType(), SUBJECT_TYPES);
        String accessLevel = normalizeToken(rule.getAccessLevel(), ACCESS_LEVELS);
        if (subjectType == null || accessLevel == null || rule.getActive() == null) {
            throw exception(DCC_PROJECT_ACCESS_RULE_INVALID);
        }
        LocalDateTime validFrom = rule.getValidFrom();
        LocalDateTime expireTime = rule.getExpireTime();
        if (validFrom != null && expireTime != null && !expireTime.isAfter(validFrom)) {
            throw exception(DCC_PROJECT_ACCESS_RULE_INVALID);
        }
        String changeReason = rule.getChangeReason() == null ? null : rule.getChangeReason().trim();
        if (changeReason == null || changeReason.isEmpty()) {
            throw exception(DCC_PROJECT_ACCESS_RULE_INVALID);
        }
        List<Long> subjectIds = List.of(rule.getSubjectId());
        switch (subjectType) {
            case "USER" -> adminUserApi.validateUserList(subjectIds);
            case "DEPT" -> deptApi.validateDeptList(subjectIds);
            case "ROLE" -> roleApi.validRoleList(subjectIds);
            case "POSITION" -> postApi.validPostList(subjectIds);
            default -> throw exception(DCC_PROJECT_ACCESS_RULE_INVALID);
        }
        DccProjectAccessRuleDO accessRule = new DccProjectAccessRuleDO();
        accessRule.setDccProjectCodeId(projectCodeId);
        accessRule.setSubjectType(subjectType);
        accessRule.setSubjectId(rule.getSubjectId());
        accessRule.setAccessLevel(accessLevel);
        accessRule.setActive(rule.getActive());
        accessRule.setValidFrom(validFrom);
        accessRule.setExpireTime(expireTime);
        accessRule.setChangeReason(changeReason);
        return accessRule;
    }

    private String normalizeToken(String value, Set<String> allowedValues) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return allowedValues.contains(normalized) ? normalized : null;
    }

    private boolean isCurrentlyActiveOwner(DccProjectAccessRuleDO rule) {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(rule.getActive())
                && "OWNER".equals(rule.getAccessLevel())
                && (rule.getValidFrom() == null || !rule.getValidFrom().isAfter(now))
                && (rule.getExpireTime() == null || rule.getExpireTime().isAfter(now));
    }

}
