package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

public interface MesPqcProductionReleaseService {

    MesPqcProductionReleaseDecisionResult approve(
            Long actorUserId, MesPqcProductionReleaseApproveCommand command);

    MesPqcProductionReleaseDecisionResult reject(
            Long actorUserId, MesPqcProductionReleaseRejectCommand command);

    MesPqcProductionReleaseDecisionResult get(Long actorUserId, Long applicationId);
}
