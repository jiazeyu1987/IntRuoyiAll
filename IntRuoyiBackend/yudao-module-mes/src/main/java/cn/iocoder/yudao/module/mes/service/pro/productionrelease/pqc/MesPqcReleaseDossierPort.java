package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;

public interface MesPqcReleaseDossierPort {

    MesPqcReleaseDossierPlan plan(MesProcessPoolActiveOrderReleaseApplicationDO application, Long actorUserId);

    MesPqcReleaseDossierReadiness readiness(
            MesProcessPoolActiveOrderReleaseApplicationDO application, Long actorUserId);

    /**
     * Plans the same formal dossier sources after active-order completion without creating a release application.
     * Stage2.5 uses this path so simulation does not create a pending production-release todo.
     */
    MesPqcReleaseDossierPlan planForActiveOrder(Long activeOrderId, Long actorUserId);

    MesPqcReleaseDossierWriteResult write(MesPqcReleaseDossierPlan plan, Long batchExecutionId);
}
