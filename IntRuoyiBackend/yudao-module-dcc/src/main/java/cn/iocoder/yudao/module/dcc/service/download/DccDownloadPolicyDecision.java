package cn.iocoder.yudao.module.dcc.service.download;

public record DccDownloadPolicyDecision(
        boolean allowed,
        String reason,
        String policyVersion
) {

    public static final String POLICY_VERSION = "dcc-download-policy-v1";

    public static DccDownloadPolicyDecision allow() {
        return new DccDownloadPolicyDecision(true, "OK", POLICY_VERSION);
    }

    public static DccDownloadPolicyDecision deny(String reason) {
        return new DccDownloadPolicyDecision(false, reason, null);
    }
}
