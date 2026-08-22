package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;

public interface MesProductionReleaseManagerApprovalService {

    boolean isManagedReleaseTransaction(Long releaseTransactionId);

    /**
     * Validate and lock the manager-owned application/task before flow10 writes
     * the sole release terminal decision.
     */
    MesProductionReleaseManagerApprovalResult prepareForFinalization(
            Long actorUserId, MesProEdhrReleaseApproveReqVO command);

    /**
     * Complete manager-owned application/task/event/audit side effects after
     * flow10 has atomically changed the release transaction to RELEASED.
     */
    MesProductionReleaseManagerApprovalResult completeAfterFinalization(
            Long actorUserId, MesProEdhrReleaseApproveReqVO command,
            MesProductionReleaseManagerApprovalResult prepared,
            cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO released);

    /**
     * Direct manager approval is intentionally disabled. All manager approvals
     * must enter through the flow10 finalization command.
     */
    @Deprecated
    MesProductionReleaseManagerApprovalResult approve(Long actorUserId, MesProEdhrReleaseApproveReqVO command);

    void assertActionSupported(Long releaseTransactionId, String action);
}
