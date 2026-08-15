package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;

public interface MesPqcReleaseDossierPort {

    MesPqcReleaseDossierPlan plan(MesProcessPoolActiveOrderReleaseApplicationDO application, Long actorUserId);

    MesPqcReleaseDossierWriteResult write(MesPqcReleaseDossierPlan plan, Long batchExecutionId);
}
