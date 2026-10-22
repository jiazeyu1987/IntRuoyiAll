package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalIdentity;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseCheckItemPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseCheckItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePrecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseWithdrawReqVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationCommand;

public interface MesProEdhrReleaseService {
    cn.iocoder.yudao.module.mes.approval.content.MesEdhrApprovalFinalizationGraph readForApproval(Long workTaskId, Long actorId, boolean locked);
    MesProEdhrReleaseRespVO approveFromCenter(cn.iocoder.yudao.module.mes.approval.content.EdhrCenterReviewCommand command);

    /** Read-only approval-center eligibility; formal approval must validate again. */
    void preflightApprovalCenterReview(Long actorUserId, Long workTaskId);

    /** Internal read-only identity; formal submission must lock and authorize again. */
    ApprovalIdentity resolveApprovalContentIdentity(Long actorUserId, Long workTaskId);

    PageResult<MesProEdhrReleaseRespVO> getPage(MesProEdhrReleasePageReqVO reqVO);

    MesProEdhrReleaseRespVO get(Long id);

    MesProEdhrReleaseRespVO precheck(MesProEdhrReleasePrecheckReqVO reqVO);

    MesProEdhrReleaseRespVO submit(MesProEdhrReleaseSubmitReqVO reqVO);

    MesProEdhrReleaseRespVO submitForApproval(MesProEdhrReleaseSubmitForApprovalCommand command);

    MesProEdhrReleaseRespVO finalizeRelease(MesReleaseFinalizationCommand command);

    MesProEdhrReleaseRespVO approve(MesProEdhrReleaseApproveReqVO reqVO);

    MesProEdhrReleaseRespVO reject(MesProEdhrReleaseRejectReqVO reqVO);

    MesProEdhrReleaseRespVO withdraw(MesProEdhrReleaseWithdrawReqVO reqVO);

    PageResult<MesProEdhrReleaseCheckItemRespVO> getCheckItemPage(MesProEdhrReleaseCheckItemPageReqVO reqVO);

    PageResult<MesProEdhrReleaseEventRespVO> getEventPage(MesProEdhrReleaseEventPageReqVO reqVO);
}
