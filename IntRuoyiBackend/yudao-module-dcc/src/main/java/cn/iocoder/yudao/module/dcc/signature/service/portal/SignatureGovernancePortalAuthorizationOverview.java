package cn.iocoder.yudao.module.dcc.signature.service.portal;

import java.util.List;

public record SignatureGovernancePortalAuthorizationOverview(String status,
                                                             boolean enabled,
                                                             List<SignatureGovernancePortalBlocker> blockers) {

    public SignatureGovernancePortalAuthorizationOverview {
        status = status == null ? null : status.trim();
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }

    public static SignatureGovernancePortalAuthorizationOverview of(String status,
                                                                    boolean enabled,
                                                                    List<SignatureGovernancePortalBlocker> blockers) {
        return new SignatureGovernancePortalAuthorizationOverview(status, enabled, blockers);
    }
}
