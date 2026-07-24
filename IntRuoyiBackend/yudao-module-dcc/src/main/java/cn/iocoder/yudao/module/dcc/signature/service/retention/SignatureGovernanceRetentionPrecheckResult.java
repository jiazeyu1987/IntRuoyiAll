package cn.iocoder.yudao.module.dcc.signature.service.retention;

import java.util.List;
import java.util.Optional;

public class SignatureGovernanceRetentionPrecheckResult {

    private final SignatureGovernanceRetentionStatus status;
    private final List<SignatureGovernanceRetentionBlocker> blockers;
    private final String receiptId;

    private SignatureGovernanceRetentionPrecheckResult(SignatureGovernanceRetentionStatus status,
            List<SignatureGovernanceRetentionBlocker> blockers, String receiptId) {
        this.status = status;
        this.blockers = List.copyOf(blockers);
        this.receiptId = receiptId;
    }

    public static SignatureGovernanceRetentionPrecheckResult ready() {
        return new SignatureGovernanceRetentionPrecheckResult(SignatureGovernanceRetentionStatus.READY, List.of(), null);
    }

    public static SignatureGovernanceRetentionPrecheckResult blocked(
            List<SignatureGovernanceRetentionBlocker> blockers) {
        return new SignatureGovernanceRetentionPrecheckResult(SignatureGovernanceRetentionStatus.BLOCKED, blockers,
                null);
    }

    public SignatureGovernanceRetentionStatus getStatus() {
        return status;
    }

    public boolean isReady() {
        return status == SignatureGovernanceRetentionStatus.READY;
    }

    public List<SignatureGovernanceRetentionBlocker> getBlockers() {
        return blockers;
    }

    public Optional<String> getReceiptId() {
        return Optional.ofNullable(receiptId);
    }
}
