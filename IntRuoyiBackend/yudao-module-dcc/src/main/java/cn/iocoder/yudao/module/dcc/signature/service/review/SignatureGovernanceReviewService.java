package cn.iocoder.yudao.module.dcc.signature.service.review;

public interface SignatureGovernanceReviewService {

    SignatureGovernanceReviewBatchEvaluation createBatch(SignatureGovernanceReviewBatchCommand command);

    SignatureGovernanceReviewBatchEvaluation evaluateBatch(SignatureGovernanceReviewBatchCommand command);

    SignatureGovernanceReviewClosureResult signReview(SignatureGovernanceReviewClosureCommand command);

    SignatureGovernanceReviewClosureResult closeBatch(SignatureGovernanceReviewClosureCommand command);
}
