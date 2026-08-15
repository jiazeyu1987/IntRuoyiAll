package cn.iocoder.yudao.module.mes.service.pro.productionrelease.role;

import java.util.List;

public record MesProductionReleaseRoleCandidates(
        Long roleId,
        String roleCode,
        List<Long> candidateUserIds,
        String candidateSnapshotHash) {

    public MesProductionReleaseRoleCandidates {
        candidateUserIds = List.copyOf(candidateUserIds);
    }
}
