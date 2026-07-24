package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessSubjectTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DccDirectoryAccessPermissionServiceImpl implements DccDirectoryAccessPermissionService {

    private static final String DIRECTORY_MANAGE_PERMISSION = "dcc:controlled-file:directory:manage";
    private static final String ACCESS_RULE_MANAGE_PERMISSION = "dcc:controlled-file:access-rule:manage";

    @Resource
    private DccDirectoryAccessRuleMapper accessRuleMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;

    @Override
    public boolean hasDirectoryManagementPermission(Long userId) {
        return userId != null && permissionApi.hasAnyPermissions(userId,
                DIRECTORY_MANAGE_PERMISSION, ACCESS_RULE_MANAGE_PERMISSION);
    }

    @Override
    public Set<Long> getAuthorizedDirectoryIds(Long userId, DccAccessTypeEnum accessType) {
        if (userId == null) {
            return Collections.emptySet();
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            return Collections.emptySet();
        }
        List<DccDirectoryAccessRuleDO> rules = accessRuleMapper.selectList(DccDirectoryAccessRuleDO::getActive, Boolean.TRUE);
        Set<Long> userRoleIds = rules.stream()
                .anyMatch(rule -> matchesSubjectType(rule.getSubjectType(), DccAccessSubjectTypeEnum.ROLE))
                ? permissionApi.getUserRoleIdListByUserId(userId)
                : Collections.emptySet();
        Set<Long> userDeptAndAncestorIds = rules.stream()
                .anyMatch(rule -> matchesSubjectType(rule.getSubjectType(), DccAccessSubjectTypeEnum.DEPT))
                ? collectUserDeptAndAncestorIds(user.getDeptId())
                : Collections.emptySet();
        return rules.stream()
                .filter(rule -> matchesSubject(rule, userId, user, userRoleIds, userDeptAndAncestorIds))
                .filter(rule -> allows(rule, accessType))
                .map(DccDirectoryAccessRuleDO::getDirectoryId)
                .collect(Collectors.toSet());
    }

    private boolean matchesSubject(DccDirectoryAccessRuleDO rule, Long userId, AdminUserRespDTO user,
                                   Set<Long> userRoleIds, Set<Long> userDeptAndAncestorIds) {
        String subjectType = rule.getSubjectType();
        if (matchesSubjectType(subjectType, DccAccessSubjectTypeEnum.USER) && userId.equals(rule.getSubjectId())) {
            return true;
        }
        if (matchesSubjectType(subjectType, DccAccessSubjectTypeEnum.DEPT)
                && matchesDepartment(rule.getSubjectId(), userDeptAndAncestorIds)) {
            return true;
        }
        if (matchesSubjectType(subjectType, DccAccessSubjectTypeEnum.POSITION)
                && user.getPostIds() != null && user.getPostIds().contains(rule.getSubjectId())) {
            return true;
        }
        return matchesSubjectType(subjectType, DccAccessSubjectTypeEnum.ROLE)
                && userRoleIds.contains(rule.getSubjectId());
    }

    private boolean matchesSubjectType(String actual, DccAccessSubjectTypeEnum expected) {
        return String.valueOf(expected.getType()).equals(actual) || expected.name().equalsIgnoreCase(actual);
    }

    private Set<Long> collectUserDeptAndAncestorIds(Long userDeptId) {
        if (userDeptId == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = new LinkedHashSet<>();
        Long currentDeptId = userDeptId;
        while (currentDeptId != null && deptIds.add(currentDeptId)) {
            DeptRespDTO dept = deptApi.getDept(currentDeptId);
            if (dept == null) {
                break;
            }
            Long parentId = dept.getParentId();
            if (parentId == null) {
                break;
            }
            if (parentId <= 0) {
                deptIds.add(parentId);
                break;
            }
            currentDeptId = parentId;
        }
        return deptIds;
    }

    private boolean matchesDepartment(Long subjectDeptId, Set<Long> userDeptAndAncestorIds) {
        if (subjectDeptId == null || userDeptAndAncestorIds.isEmpty()) {
            return false;
        }
        return userDeptAndAncestorIds.contains(subjectDeptId);
    }

    private boolean allows(DccDirectoryAccessRuleDO rule, DccAccessTypeEnum accessType) {
        boolean mergedReadAllowed = Boolean.TRUE.equals(rule.getCanQuery()) || Boolean.TRUE.equals(rule.getCanPreview());
        return switch (accessType) {
            case QUERY, PREVIEW -> mergedReadAllowed;
            case DOWNLOAD -> Boolean.TRUE.equals(rule.getCanDownload());
        };
    }
}
