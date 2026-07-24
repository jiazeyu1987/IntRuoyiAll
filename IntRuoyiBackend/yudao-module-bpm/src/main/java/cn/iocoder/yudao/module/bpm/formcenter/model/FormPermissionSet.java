package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.EnumSet;
import java.util.Set;

public class FormPermissionSet {

    private final Set<FormResourcePermission> permissions;

    private FormPermissionSet(Set<FormResourcePermission> permissions) {
        this.permissions = permissions.isEmpty()
                ? EnumSet.noneOf(FormResourcePermission.class)
                : EnumSet.copyOf(permissions);
    }

    public static FormPermissionSet of(FormResourcePermission... permissions) {
        return new FormPermissionSet(Set.of(permissions));
    }

    public boolean has(FormResourcePermission permission) {
        return permissions.contains(permission);
    }

}
