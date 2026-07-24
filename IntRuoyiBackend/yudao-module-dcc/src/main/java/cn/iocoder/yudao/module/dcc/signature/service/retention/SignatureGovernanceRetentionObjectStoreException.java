package cn.iocoder.yudao.module.dcc.signature.service.retention;

public class SignatureGovernanceRetentionObjectStoreException extends RuntimeException {

    private final SignatureGovernanceRetentionBlockerCode blockerCode;

    public SignatureGovernanceRetentionObjectStoreException(SignatureGovernanceRetentionBlockerCode blockerCode,
            String message, Throwable cause) {
        super(message, cause);
        this.blockerCode = blockerCode;
    }

    public SignatureGovernanceRetentionBlockerCode getBlockerCode() {
        return blockerCode;
    }
}
