package cn.iocoder.yudao.module.system.api.signature.dto;

/** A rejection is returned normally so its independent transaction commits before the facade throws. */
public record SignatureAuthenticationOutcome(AuthenticatedSigner signer, String rejectionCode) {
    public SignatureAuthenticationOutcome {
        if ((signer == null) == (rejectionCode == null) || rejectionCode != null && rejectionCode.isBlank()) {
            throw new IllegalArgumentException("SIGNATURE_AUTHENTICATION_OUTCOME_INVALID");
        }
    }
    public boolean succeeded() { return signer != null; }
}
