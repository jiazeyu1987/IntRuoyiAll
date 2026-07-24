package cn.iocoder.yudao.module.bpm.approval.service.signature;

import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;

public interface ApprovalSignatureRecordService {

    ApprovalSignatureRecordResult recordReviewSignature(ApprovalTaskReviewContext context);

}
