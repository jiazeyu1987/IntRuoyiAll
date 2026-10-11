package cn.iocoder.yudao.module.bpm.approval.content;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewCommand;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService.VerifiedSignatureFact;
public interface ApprovalTrustedDecisionProvider {
    ApprovalDomainDecision decide(Long actorId, Long receiptId, ApprovalTaskReviewCommand command,
            PreparedApprovalContent content, VerifiedSignatureFact signature);
}
