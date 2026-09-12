package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectAccessRuleMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED;

@Service
public class DccProjectAccessServiceImpl implements DccProjectAccessService {

    @Resource
    private DccProjectAccessRuleMapper accessRuleMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;

    @Override
    public void assertProjectOwner(Long userId, Long projectCodeId) {
        assertProjectAccess(userId, projectCodeId, Set.of("OWNER"));
    }

    @Override
    public void assertProjectEditorOrOwner(Long userId, Long projectCodeId) {
        assertProjectAccess(userId, projectCodeId, Set.of("OWNER", "EDIT"));
    }

    private void assertProjectAccess(Long userId, Long projectCodeId, Set<String> allowedAccessLevels) {
        if (userId == null || projectCodeId == null) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
        LocalDateTime now = LocalDateTime.now();
        Set<Long> roleIds = permissionApi.getUserRoleIdListByUserId(userId);
        Set<Long> deptIds = collectDeptIds(user.getDeptId());
        boolean allowed = accessRuleMapper.selectActiveRules(projectCodeId, now).stream()
                .filter(rule -> rule.getAccessLevel() != null
                        && allowedAccessLevels.stream().anyMatch(level -> level.equalsIgnoreCase(rule.getAccessLevel())))
                .anyMatch(rule -> matches(rule, userId, user, roleIds, deptIds));
        if (!allowed) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
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

}
