package cn.iocoder.yudao.module.mes.service.pro.productionrelease.role;

public interface MesProductionReleaseRequiredCandidateResolver {

    MesProductionReleaseRoleCandidates resolveRequiredCandidates(Long tenantId, String roleCode);
}
