package cn.iocoder.yudao.module.dcc.signature.service.retention;

public interface SignatureGovernanceRetentionService {

    SignatureGovernanceRetentionPrecheckResult precheck(SignatureGovernanceRetentionPrecheckCommand command);

    SignatureGovernanceRetentionReceiptResult createDccEvidenceReceipt(
            SignatureGovernanceRetentionReceiptCommand command);

    SignatureGovernanceRetentionReceiptResult createEdhrArchiveReceipt(
            SignatureGovernanceRetentionReceiptCommand command);

    SignatureGovernanceRecoveryRehearsalResult runRecoveryRehearsal(
            SignatureGovernanceRecoveryRehearsalCommand command);
}
