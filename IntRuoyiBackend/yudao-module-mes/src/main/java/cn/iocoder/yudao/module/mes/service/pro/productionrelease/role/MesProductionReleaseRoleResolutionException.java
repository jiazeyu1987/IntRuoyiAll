package cn.iocoder.yudao.module.mes.service.pro.productionrelease.role;

import lombok.Getter;

@Getter
public final class MesProductionReleaseRoleResolutionException extends RuntimeException {

    private final MesProductionReleaseRoleBlockerType blockerType;
    private final MesProductionReleaseRoleResolutionReason reason;
    private final Long tenantId;
    private final String roleCode;

    public MesProductionReleaseRoleResolutionException(Long tenantId, String roleCode,
            MesProductionReleaseRoleResolutionReason reason) {
        super("Production release role resolution failed: roleCode=" + roleCode + ", tenantId=" + tenantId
                + ", reason=" + reason);
        this.blockerType = MesProductionReleaseRoleBlockerType.forRoleCode(roleCode);
        this.reason = reason;
        this.tenantId = tenantId;
        this.roleCode = roleCode;
    }

    public MesProductionReleaseRoleResolutionException(Long tenantId, String roleCode,
            MesProductionReleaseRoleResolutionReason reason, Throwable cause) {
        super("Production release role resolution failed: roleCode=" + roleCode + ", tenantId=" + tenantId
                + ", reason=" + reason, cause);
        this.blockerType = MesProductionReleaseRoleBlockerType.forRoleCode(roleCode);
        this.reason = reason;
        this.tenantId = tenantId;
        this.roleCode = roleCode;
    }
}
