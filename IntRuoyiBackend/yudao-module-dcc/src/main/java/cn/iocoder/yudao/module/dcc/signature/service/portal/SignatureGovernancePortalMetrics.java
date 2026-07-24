package cn.iocoder.yudao.module.dcc.signature.service.portal;

public record SignatureGovernancePortalMetrics(Long pendingCount,
                                               Long signatureCount) {

    public SignatureGovernancePortalMetrics {
        if (pendingCount == null || signatureCount == null) {
            throw new IllegalArgumentException("Signature governance portal metrics require pending and signature counts");
        }
        if (pendingCount < 0 || signatureCount < 0) {
            throw new IllegalArgumentException("Signature governance portal metrics cannot be negative");
        }
    }

    public static SignatureGovernancePortalMetrics of(Long pendingCount, Long signatureCount) {
        return new SignatureGovernancePortalMetrics(pendingCount, signatureCount);
    }
}
