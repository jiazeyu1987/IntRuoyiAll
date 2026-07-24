package cn.iocoder.yudao.module.dcc.signature.service.portal;

import java.util.List;

public record SignatureGovernancePortalOverview(String status,
                                               boolean ready,
                                               SignatureGovernancePortalAuthorizationOverview authorization,
                                               SignatureGovernancePortalSummary summary,
                                               List<SignatureGovernancePortalModuleOverview> modules,
                                               List<SignatureGovernancePortalBlocker> blockers) {

    public SignatureGovernancePortalOverview {
        status = status == null ? null : status.trim();
        if (authorization == null || summary == null) {
            throw new IllegalArgumentException("Signature governance portal overview requires authorization and summary");
        }
        modules = modules == null ? List.of() : List.copyOf(modules);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
