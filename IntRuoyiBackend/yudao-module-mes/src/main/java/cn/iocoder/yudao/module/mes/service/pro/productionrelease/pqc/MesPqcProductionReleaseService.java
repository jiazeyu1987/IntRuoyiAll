package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.common.pojo.PageResult;

public interface MesPqcProductionReleaseService {

    MesPqcProductionReleaseDecisionResult approve(
            Long actorUserId, MesPqcProductionReleaseApproveCommand command);

    MesPqcProductionReleaseDecisionResult reject(
            Long actorUserId, MesPqcProductionReleaseRejectCommand command);

    MesPqcProductionReleaseDecisionResult get(Long actorUserId, Long applicationId);

    PageResult<MesPqcProductionReleasePageItem> getPqcReleasePage(
            Long actorUserId, MesPqcProductionReleasePageQuery query);
}
