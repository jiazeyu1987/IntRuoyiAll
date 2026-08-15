package cn.iocoder.yudao.module.mes.productionrelease.core;

import java.util.Set;

public final class MesReleaseFlowStatus {

    public static final String PQC_RELEASE_PENDING = "PQC_RELEASE_PENDING";
    public static final String PQC_RELEASE_REJECTED = "PQC_RELEASE_REJECTED";
    public static final String REPORT_UPLOAD_PENDING = "REPORT_UPLOAD_PENDING";
    public static final String MANAGER_RELEASE_PENDING = "MANAGER_RELEASE_PENDING";
    public static final String RELEASED = "RELEASED";

    private static final Set<String> PERSISTENT_STATUSES = Set.of(
            PQC_RELEASE_PENDING,
            PQC_RELEASE_REJECTED,
            REPORT_UPLOAD_PENDING,
            MANAGER_RELEASE_PENDING,
            RELEASED);

    private MesReleaseFlowStatus() {
    }

    public static boolean isPersistentStatus(String status) {
        return PERSISTENT_STATUSES.contains(status);
    }

    public static boolean isAllowedTransition(String fromStatus, String toStatus) {
        return switch (fromStatus) {
            case PQC_RELEASE_PENDING -> PQC_RELEASE_REJECTED.equals(toStatus)
                    || REPORT_UPLOAD_PENDING.equals(toStatus);
            case REPORT_UPLOAD_PENDING -> REPORT_UPLOAD_PENDING.equals(toStatus)
                    || MANAGER_RELEASE_PENDING.equals(toStatus);
            case MANAGER_RELEASE_PENDING -> RELEASED.equals(toStatus);
            default -> false;
        };
    }
}
