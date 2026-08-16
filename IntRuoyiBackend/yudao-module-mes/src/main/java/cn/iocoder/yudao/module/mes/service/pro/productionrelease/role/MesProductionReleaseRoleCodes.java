package cn.iocoder.yudao.module.mes.service.pro.productionrelease.role;

import java.util.Set;

public final class MesProductionReleaseRoleCodes {

    public static final String PQC_RELEASE_OWNER = "MES_PQC_RELEASE_OWNER";
    public static final String MANAGEMENT_REPRESENTATIVE = "MES_MANAGEMENT_REPRESENTATIVE";

    private static final Set<String> ALLOWED_ROLE_CODES = Set.of(
            PQC_RELEASE_OWNER,
            MANAGEMENT_REPRESENTATIVE);

    private MesProductionReleaseRoleCodes() {
    }

    public static boolean isAllowed(String roleCode) {
        return ALLOWED_ROLE_CODES.contains(roleCode);
    }
}
