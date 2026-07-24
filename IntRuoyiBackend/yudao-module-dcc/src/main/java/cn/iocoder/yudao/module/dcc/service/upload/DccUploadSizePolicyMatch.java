package cn.iocoder.yudao.module.dcc.service.upload;

public record DccUploadSizePolicyMatch(Long policyId,
                                       String policyCode,
                                       DccUploadSizePolicyScopeType scopeType,
                                       Long categoryId,
                                       String purpose,
                                       Long maxBytes,
                                       String policyVersion,
                                       Integer policyPriority,
                                       int scopePriority) {
}
