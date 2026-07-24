package cn.iocoder.yudao.module.dcc.signature.service.retention;

import java.util.List;

public record SignatureGovernanceRecoveryRehearsalCommand(
        String backupId,
        String recoveryRuntime,
        boolean ownerReviewed,
        boolean reportWritten,
        boolean auditWritten,
        List<SignatureGovernanceRecoverySample> samples) {
}
