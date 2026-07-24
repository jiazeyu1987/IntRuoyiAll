package cn.iocoder.yudao.module.dcc.signature.service.review;

public enum SignatureGovernanceReviewFindingCode {

    VALID(false),
    SIGNATURE_PERMISSION_EXCEPTION(true),
    SIGNATURE_LOCK_EXCEPTION(true),
    SIGNATURE_FAILURE_RECORDED(true),
    ABNORMAL_SIGNATURE_EVIDENCE(true),
    HASH_MISMATCH(true),
    HISTORICAL_UNBOUND(true),
    POLICY_EXCEPTION(true);

    private final boolean remediationRequired;

    SignatureGovernanceReviewFindingCode(boolean remediationRequired) {
        this.remediationRequired = remediationRequired;
    }

    public boolean isRemediationRequired() {
        return remediationRequired;
    }
}
