package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 操作日志入口仅允许管理员和 eDHR 审核员；业务模块自身的查询权限保持独立。 */
public final class OperationLogAccessPolicy {
    private static final Set<String> ROLES = Set.of("super_admin", "tenant_admin", "audit_admin", "edhr_auditor");

    private OperationLogAccessPolicy() { }

    public static boolean canAccess(Collection<RoleDO> roles) {
        return roles.stream().anyMatch(role -> Integer.valueOf(0).equals(role.getStatus()) && ROLES.contains(role.getCode()));
    }

    public static List<MenuDO> filterNavigation(List<MenuDO> menus, Collection<RoleDO> roles) {
        if (canAccess(roles)) return menus;
        Set<Long> excluded = new HashSet<>();
        menus.stream().filter(menu -> "system/operatelog/index".equals(menu.getComponent()))
                .forEach(menu -> excluded.add(menu.getId()));
        boolean changed;
        do {
            changed = false;
            for (MenuDO menu : menus) {
                if (excluded.contains(menu.getParentId())) changed |= excluded.add(menu.getId());
            }
        } while (changed);
        return menus.stream().filter(menu -> !excluded.contains(menu.getId())).toList();
    }
}
