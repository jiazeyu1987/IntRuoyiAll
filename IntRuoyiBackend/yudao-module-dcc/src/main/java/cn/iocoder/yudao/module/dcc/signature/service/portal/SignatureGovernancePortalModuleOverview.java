package cn.iocoder.yudao.module.dcc.signature.service.portal;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyOverview;

import java.util.List;

public record SignatureGovernancePortalModuleOverview(SignatureGovernanceModuleCode moduleCode,
                                                      String moduleName,
                                                      String moduleDescription,
                                                      String status,
                                                      boolean ready,
                                                      SignatureGovernancePortalAuthorizationOverview authorization,
                                                      SignatureGovernancePolicyOverview policy,
                                                      SignatureGovernancePortalMetrics metrics,
                                                      SignatureGovernancePortalRouteOverview routes,
                                                      List<SignatureGovernancePortalBlocker> blockers) {

    public SignatureGovernancePortalModuleOverview {
        if (moduleCode == null) {
            throw new IllegalArgumentException("Signature governance portal module requires module code");
        }
        status = status == null ? null : status.trim();
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
