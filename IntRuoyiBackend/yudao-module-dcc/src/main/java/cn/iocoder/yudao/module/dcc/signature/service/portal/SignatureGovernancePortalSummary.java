package cn.iocoder.yudao.module.dcc.signature.service.portal;

public record SignatureGovernancePortalSummary(Long moduleTotal,
                                               Long readyModuleTotal,
                                               Long blockedModuleTotal,
                                               Long pendingTotal,
                                               Long signatureTotal) {

    public SignatureGovernancePortalSummary {
        if (moduleTotal == null || readyModuleTotal == null || blockedModuleTotal == null
                || pendingTotal == null || signatureTotal == null) {
            throw new IllegalArgumentException("Signature governance portal summary requires all totals");
        }
    }
}
