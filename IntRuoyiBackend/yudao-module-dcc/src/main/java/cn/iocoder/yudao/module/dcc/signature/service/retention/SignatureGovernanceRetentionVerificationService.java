package cn.iocoder.yudao.module.dcc.signature.service.retention;

public interface SignatureGovernanceRetentionVerificationService {

    SignatureGovernanceRetentionVerificationResult verify(SignatureGovernanceRetentionPrecheckCommand command);

    SignatureGovernanceRetentionVerificationResult verifyReceipt(SignatureGovernanceRetentionReceiptCommand command);

    SignatureGovernanceRetentionVerificationResult verifyRecoveryRehearsal(
            SignatureGovernanceRecoveryRehearsalCommand command);
}
