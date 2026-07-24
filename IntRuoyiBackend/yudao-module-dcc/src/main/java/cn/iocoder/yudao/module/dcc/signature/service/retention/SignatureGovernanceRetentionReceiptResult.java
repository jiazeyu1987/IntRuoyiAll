package cn.iocoder.yudao.module.dcc.signature.service.retention;

import java.util.List;
import java.util.Optional;

public class SignatureGovernanceRetentionReceiptResult {

    private final SignatureGovernanceRetentionStatus status;
    private final List<SignatureGovernanceRetentionBlocker> blockers;
    private final String receiptId;

    private SignatureGovernanceRetentionReceiptResult(SignatureGovernanceRetentionStatus status,
            List<SignatureGovernanceRetentionBlocker> blockers, String receiptId) {
        this.status = status;
        this.blockers = List.copyOf(blockers);
        this.receiptId = receiptId;
    }

    public static SignatureGovernanceRetentionReceiptResult recorded(String receiptId) {
        return new SignatureGovernanceRetentionReceiptResult(SignatureGovernanceRetentionStatus.RECORDED, List.of(),
                receiptId);
    }

    public static SignatureGovernanceRetentionReceiptResult blocked(
            List<SignatureGovernanceRetentionBlocker> blockers) {
        return new SignatureGovernanceRetentionReceiptResult(SignatureGovernanceRetentionStatus.BLOCKED, blockers,
                null);
    }

    public SignatureGovernanceRetentionStatus getStatus() {
        return status;
    }

    public boolean isRecorded() {
        return status == SignatureGovernanceRetentionStatus.RECORDED;
    }

    public List<SignatureGovernanceRetentionBlocker> getBlockers() {
        return blockers;
    }

    public Optional<String> getReceiptId() {
        return Optional.ofNullable(receiptId);
    }
}
