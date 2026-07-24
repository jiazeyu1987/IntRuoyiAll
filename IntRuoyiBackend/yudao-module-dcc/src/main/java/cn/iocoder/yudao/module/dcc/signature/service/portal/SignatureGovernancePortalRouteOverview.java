package cn.iocoder.yudao.module.dcc.signature.service.portal;

import cn.hutool.core.util.StrUtil;

public record SignatureGovernancePortalRouteOverview(String primaryLabel,
                                                    String primaryPath,
                                                    String secondaryLabel,
                                                    String secondaryPath) {

    public SignatureGovernancePortalRouteOverview {
        if (StrUtil.isBlank(primaryLabel) || StrUtil.isBlank(primaryPath)) {
            throw new IllegalArgumentException("Signature governance portal routes require a primary label and path");
        }
        primaryLabel = primaryLabel.trim();
        primaryPath = primaryPath.trim();
        secondaryLabel = trimToNull(secondaryLabel);
        secondaryPath = trimToNull(secondaryPath);
    }

    public static SignatureGovernancePortalRouteOverview of(String primaryLabel, String primaryPath,
                                                            String secondaryLabel, String secondaryPath) {
        return new SignatureGovernancePortalRouteOverview(primaryLabel, primaryPath, secondaryLabel, secondaryPath);
    }

    private static String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
