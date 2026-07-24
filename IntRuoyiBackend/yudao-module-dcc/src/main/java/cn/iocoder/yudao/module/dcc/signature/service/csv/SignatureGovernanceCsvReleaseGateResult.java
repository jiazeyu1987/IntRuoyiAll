package cn.iocoder.yudao.module.dcc.signature.service.csv;

import java.util.List;

public record SignatureGovernanceCsvReleaseGateResult(String releaseId,
                                                      SignatureGovernanceCsvReleaseGateStatus status,
                                                      SignatureGovernanceCsvPackageResult packageResult,
                                                      boolean engineeringVerificationPassed,
                                                      boolean qaApproved,
                                                      List<SignatureGovernanceCsvBlocker> blockers) {

    public SignatureGovernanceCsvReleaseGateResult {
        if (status == null || packageResult == null) {
            throw new IllegalArgumentException("CSV release gate result requires status and package result");
        }
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
