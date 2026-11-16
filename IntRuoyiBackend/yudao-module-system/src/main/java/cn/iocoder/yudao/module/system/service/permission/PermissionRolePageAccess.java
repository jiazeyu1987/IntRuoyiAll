package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Read access follows the permission-role menu, including grants to its child buttons. */
@Service
public class PermissionRolePageAccess {
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;

    public boolean canRead() {
        return canRead(SecurityFrameworkUtils.getLoginUserId());
    }

    boolean canRead(Long userId) {
        if (userId == null) return false;
        Set<Long> roleIds = roleService.getRoleList(permissionService.getUserRoleIdListByUserId(userId))
                .stream().filter(role -> CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()))
                .map(RoleDO::getId).collect(Collectors.toSet());
        Set<Long> menuIds = new HashSet<>(permissionService.getRoleMenuListByRoleId(roleIds));
        menuIds.addAll(permissionService.getDynamicMenuListByUserId(userId));
        menuIds.addAll(permissionService.getTemporaryMenuListByUserId(userId));
        for (MenuDO granted : menuService.getMenuList(menuIds)) {
            if (!CommonStatusEnum.ENABLE.getStatus().equals(granted.getStatus())) continue;
            MenuDO node = granted;
            boolean pageFound = false;
            Set<Long> visited = new HashSet<>();
            while (node != null && visited.add(node.getId())) {
                if (!CommonStatusEnum.ENABLE.getStatus().equals(node.getStatus())) break;
                if ("system/role/index".equals(node.getComponent()) && Boolean.TRUE.equals(node.getVisible())) {
                    pageFound = true;
                }
                if (MenuDO.ID_ROOT.equals(node.getParentId())) {
                    if (pageFound) return true;
                    break;
                }
                node = menuService.getMenu(node.getParentId());
            }
        }
        return false;
    }
}
