package cn.iocoder.yudao.module.system.service.gxpaudit;

public record GxpAuditPolicyActivationResult(Long activationId, String policyVersion,
                                             String policyHash, boolean replayed) {
}
