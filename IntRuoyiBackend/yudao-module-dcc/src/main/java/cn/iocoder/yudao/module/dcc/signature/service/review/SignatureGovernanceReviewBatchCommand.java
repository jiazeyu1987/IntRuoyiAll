package cn.iocoder.yudao.module.dcc.signature.service.review;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record SignatureGovernanceReviewBatchCommand(String reviewOwner,
                                                     String periodCode,
                                                     String ruleVersion,
                                                     LocalDate dueDate,
                                                     String reason,
                                                     Set<SignatureGovernanceModuleCode> scopeModules,
                                                     Set<SignatureGovernanceModuleCode> permittedModules,
                                                     List<SignatureGovernanceReviewSourceProjection> projections,
                                                     boolean reviewSignatureStrategyConfigured) {

    public SignatureGovernanceReviewBatchCommand {
        reviewOwner = trimToNull(reviewOwner);
        periodCode = trimToNull(periodCode);
        ruleVersion = trimToNull(ruleVersion);
        reason = trimToNull(reason);
        scopeModules = scopeModules == null ? null : Set.copyOf(scopeModules);
        permittedModules = permittedModules == null ? null : Set.copyOf(permittedModules);
        projections = projections == null ? null : List.copyOf(projections);
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
