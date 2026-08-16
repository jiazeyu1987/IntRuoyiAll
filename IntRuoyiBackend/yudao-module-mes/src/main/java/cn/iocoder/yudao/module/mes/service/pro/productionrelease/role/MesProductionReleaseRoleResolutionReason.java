package cn.iocoder.yudao.module.mes.service.pro.productionrelease.role;

public enum MesProductionReleaseRoleResolutionReason {

    TENANT_CONTEXT_MISMATCH,
    ROLE_NOT_FOUND,
    ROLE_NOT_UNIQUE,
    ROLE_DISABLED,
    ROLE_DATA_INCONSISTENT,
    CANDIDATE_EMPTY,
    CANDIDATE_USER_NOT_IN_TENANT,
    CANDIDATE_USER_DISABLED
}
