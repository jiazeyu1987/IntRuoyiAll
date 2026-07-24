package cn.iocoder.yudao.module.dcc.signature.service.review;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

public record SignatureGovernanceReviewSourceProjection(SignatureGovernanceModuleCode moduleCode,
                                                         String sourceTable,
                                                         String sourceId,
                                                         String sourceHash,
                                                         String actionCode,
                                                         String meaningCode,
                                                         SignatureGovernanceReviewFindingCode findingCode) {

    public SignatureGovernanceReviewSourceProjection {
        if (moduleCode == null || isBlank(sourceTable) || isBlank(sourceId) || isBlank(sourceHash)
                || isBlank(actionCode) || isBlank(meaningCode) || findingCode == null) {
            throw new IllegalArgumentException("Signature review source projection requires module, source, hash, action, and finding");
        }
        sourceTable = sourceTable.trim();
        sourceId = sourceId.trim();
        sourceHash = sourceHash.trim();
        actionCode = actionCode.trim();
        meaningCode = meaningCode.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
