package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import java.util.Collection;
import java.util.Set;

/** 租户管理员统一业务授权；不扩展 system、infra 等平台权限。 */
public final class TenantAdminBusinessPolicy {
    private static final Set<String> MODULES = Set.of("ai", "bpm", "crm", "dcc", "erp",
            "iot", "mall", "member", "mes", "mdm", "mp", "pay", "report", "showroom",
            "signature", "srm", "product", "trade", "promotion", "statistics");
    private static final Set<String> BUSINESS_PATHS = Set.of("controlled-file", "form-center", "signature-governance");

    private TenantAdminBusinessPolicy() { }

    public static boolean isTenantAdmin(Collection<RoleDO> roles) {
        return roles.stream().anyMatch(role -> RoleCodeEnum.TENANT_ADMIN.getCode().equals(role.getCode())
                && CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
    }

    public static boolean isBusinessPermission(String permission) {
        // 租户管理员可查看全部电子签名治理内容；治理写操作仍按原权限授权。
        if (permission != null && permission.startsWith("signature-governance:") && permission.endsWith(":query")) {
            return true;
        }
        return permission != null && permission.contains(":")
                && MODULES.contains(permission.substring(0, permission.indexOf(':')));
    }

    public static boolean isBusinessMenu(MenuDO menu) {
        if (!CommonStatusEnum.ENABLE.getStatus().equals(menu.getStatus())) return false;
        String permission = menu.getPermission();
        if (permission != null && !permission.isBlank()) return isBusinessPermission(permission);
        return isBusinessPath(menu.getPath()) || isBusinessPath(menu.getComponent());
    }

    private static boolean isBusinessPath(String path) {
        if (path == null) return false;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        String root = normalized.split("/", 2)[0];
        return MODULES.contains(root) || BUSINESS_PATHS.contains(root);
    }
}
