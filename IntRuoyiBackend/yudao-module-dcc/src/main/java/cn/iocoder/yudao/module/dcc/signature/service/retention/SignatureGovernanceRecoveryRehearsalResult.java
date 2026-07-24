package cn.iocoder.yudao.module.dcc.signature.service.retention;

import java.util.List;

public class SignatureGovernanceRecoveryRehearsalResult {

    private final SignatureGovernanceRetentionStatus status;
    private final List<SignatureGovernanceRetentionBlocker> blockers;

    private SignatureGovernanceRecoveryRehearsalResult(SignatureGovernanceRetentionStatus status,
            List<SignatureGovernanceRetentionBlocker> blockers) {
        this.status = status;
        this.blockers = List.copyOf(blockers);
    }

    public static SignatureGovernanceRecoveryRehearsalResult passed() {
        return new SignatureGovernanceRecoveryRehearsalResult(SignatureGovernanceRetentionStatus.PASSED, List.of());
    }

    public static SignatureGovernanceRecoveryRehearsalResult blocked(
            List<SignatureGovernanceRetentionBlocker> blockers) {
        return new SignatureGovernanceRecoveryRehearsalResult(SignatureGovernanceRetentionStatus.BLOCKED, blockers);
    }

    public SignatureGovernanceRetentionStatus getStatus() {
        return status;
    }

    public boolean isPassed() {
        return status == SignatureGovernanceRetentionStatus.PASSED;
    }

    public List<SignatureGovernanceRetentionBlocker> getBlockers() {
        return blockers;
    }
}
