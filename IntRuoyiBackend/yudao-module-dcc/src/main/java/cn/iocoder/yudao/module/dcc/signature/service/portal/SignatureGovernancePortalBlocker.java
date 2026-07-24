package cn.iocoder.yudao.module.dcc.signature.service.portal;

import cn.hutool.core.util.StrUtil;

public record SignatureGovernancePortalBlocker(String code, String message, String impact) {

    public SignatureGovernancePortalBlocker {
        if (StrUtil.isBlank(code) || StrUtil.isBlank(message) || StrUtil.isBlank(impact)) {
            throw new IllegalArgumentException("Signature governance portal blocker requires code, message, and impact");
        }
        code = code.trim();
        message = message.trim();
        impact = impact.trim();
    }

    public static SignatureGovernancePortalBlocker of(String code, String message, String impact) {
        return new SignatureGovernancePortalBlocker(code, message, impact);
    }
}
