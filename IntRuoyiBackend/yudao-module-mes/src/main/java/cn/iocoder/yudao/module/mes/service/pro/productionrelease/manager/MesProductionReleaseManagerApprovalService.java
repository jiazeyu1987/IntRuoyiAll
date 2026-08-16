package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;

public interface MesProductionReleaseManagerApprovalService {

    boolean isManagedReleaseTransaction(Long releaseTransactionId);

    MesProductionReleaseManagerApprovalResult approve(Long actorUserId, MesProEdhrReleaseApproveReqVO command);

    void assertActionSupported(Long releaseTransactionId, String action);
}
