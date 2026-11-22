package cn.iocoder.yudao.framework.common.enums;

import java.util.Map;
import java.util.Set;

/** ERP 数据按页签独立授权，管理员身份不隐式授予这些权限。 */
public final class ErpDataPermission {
    public static final Set<String> LEGACY_ROLES = Set.of("tenant_admin", "super_admin",
            "mes_workshop_director", "mes_team_leader", "mes_scheduler");
    public static final String CONFIG = "erp:data-config:operate";
    // 生产补料单使用自动生成的菜单编号，按 ERP 数据目录（991511）的子树识别，不固化环境内编号。
    public static final Set<Long> PAGE_IDS = Set.of(991511L, 6013L, 991502L, 2565L, 2590L,
            6023L, 6021L, 6032L, 6025L, 5530L);
    private static final Set<String> PREFIXES = Set.of("erp:data-", "erp:product:", "erp:stock:",
            "erp:bom-list:", "erp:production-material-list:", "erp:production-pick-list:",
            "erp:production-replenishment-list:", "erp:inventory-list:", "mes:pro-work-order:",
            "erp:kingdee-sync:", "erp:kingdee-config:");
    private static final Map<String, String> SYNC_PERMISSIONS = Map.ofEntries(
            Map.entry("PRODUCT", "erp:data-product:operate"), Map.entry("STOCK", "erp:data-stock:operate"),
            Map.entry("INVENTORY_DETAIL", "erp:data-inventory:operate"),
            Map.entry("BOM", "erp:data-bom:operate"), Map.entry("PRODUCTION_MATERIAL_LIST", "erp:data-material:operate"),
            Map.entry("PRODUCTION_PICK_LIST", "erp:data-pick:operate"),
            Map.entry("PRODUCTION_REPLENISHMENT_LIST", "erp:data-replenishment:operate"),
            Map.entry("PRODUCTION_ORDER", "erp:data-order:operate"));

    private ErpDataPermission() { }

    public static boolean requiresExplicitGrant(String permission) {
        return permission != null && PREFIXES.stream().anyMatch(permission::startsWith);
    }

    public static String forSyncType(String syncType) {
        // 采购、销售、调拨同步属于 ERP 配置，不借用其他数据页签的权限。
        if (syncType == null) throw new IllegalArgumentException("ERP sync type is required");
        if (Set.of("PURCHASE_ORDER", "SALE_ORDER", "STOCK_MOVE").contains(syncType)) return CONFIG;
        String permission = SYNC_PERMISSIONS.get(syncType);
        if (permission == null) throw new IllegalArgumentException("Unsupported ERP sync type: " + syncType);
        return permission;
    }
}
