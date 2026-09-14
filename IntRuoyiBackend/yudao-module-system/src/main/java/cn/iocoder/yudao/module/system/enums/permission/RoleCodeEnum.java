package cn.iocoder.yudao.module.system.enums.permission;

import cn.iocoder.yudao.framework.common.util.object.ObjectUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 角色标识枚举
 */
@Getter
@AllArgsConstructor
public enum RoleCodeEnum {

    SUPER_ADMIN("super_admin", "超级管理员"),
    TENANT_ADMIN("tenant_admin", "租户管理员"),
    BPM_ADMIN("bpm_admin", "BPM管理员"),
    APPROVAL_ADMIN("approval_admin", "审批中心管理员"),
    AUDIT_ADMIN("audit_admin", "审计管理员"),
    SRM_ADMIN("srm_admin", "SRM管理员"),
    PUHUI_SCHEDULE_ADMIN("mes_puhui_schedule_admin", "璞慧排产管理员"),
    CRM_ADMIN("crm_admin", "CRM 管理员"); // CRM 系统专用
    ;

    /**
     * 角色编码
     */
    private final String code;
    /**
     * 名字
     */
    private final String name;

    public static boolean isSuperAdmin(String code) {
        return ObjectUtils.equalsAny(code, SUPER_ADMIN.getCode());
    }

    public static boolean isAdminRole(String code) {
        return Arrays.stream(values()).anyMatch(role -> Objects.equals(role.getCode(), code));
    }

}
