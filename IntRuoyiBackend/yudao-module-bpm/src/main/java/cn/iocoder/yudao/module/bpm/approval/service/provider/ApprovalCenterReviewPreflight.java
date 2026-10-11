package cn.iocoder.yudao.module.bpm.approval.service.provider;

import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;

/** Explicit, signature-independent eligibility check. Implementations must perform no writes. */
public interface ApprovalCenterReviewPreflight {

    void preflightReview(ApprovalTaskReviewContext context);
}
