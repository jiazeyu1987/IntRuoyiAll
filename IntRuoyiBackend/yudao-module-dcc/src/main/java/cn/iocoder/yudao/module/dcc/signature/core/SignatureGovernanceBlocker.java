package cn.iocoder.yudao.module.dcc.signature.core;

import cn.hutool.core.util.StrUtil;

public class SignatureGovernanceBlocker {

    private final SignatureGovernanceBlockerCode code;
    private final String message;
    private final String impact;

    private SignatureGovernanceBlocker(SignatureGovernanceBlockerCode code, String message, String impact) {
        this.code = code;
        this.message = message;
        this.impact = impact;
    }

    public static SignatureGovernanceBlocker of(SignatureGovernanceBlockerCode code, String message, String impact) {
        if (code == null || StrUtil.isBlank(message) || StrUtil.isBlank(impact)) {
            throw new IllegalArgumentException("Signature governance blocker requires code, message, and impact");
        }
        return new SignatureGovernanceBlocker(code, StrUtil.trim(message), StrUtil.trim(impact));
    }

    public SignatureGovernanceBlockerCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getImpact() {
        return impact;
    }
}
