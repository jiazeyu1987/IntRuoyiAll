package cn.iocoder.yudao.module.mes.service.pro.productionrelease.role;

public enum MesProductionReleaseRoleBlockerType {

    PQC_RELEASE_ROLE_REQUIRED,
    MANAGEMENT_REPRESENTATIVE_ROLE_REQUIRED;

    public static MesProductionReleaseRoleBlockerType forRoleCode(String roleCode) {
        return switch (roleCode) {
            case MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER -> PQC_RELEASE_ROLE_REQUIRED;
            case MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE ->
                    MANAGEMENT_REPRESENTATIVE_ROLE_REQUIRED;
            default -> throw new IllegalArgumentException("Unsupported production release role code: " + roleCode);
        };
    }
}
