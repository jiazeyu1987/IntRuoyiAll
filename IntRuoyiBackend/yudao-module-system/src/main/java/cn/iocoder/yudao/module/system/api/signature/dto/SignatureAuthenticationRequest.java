package cn.iocoder.yudao.module.system.api.signature.dto;

/** Credentials exist only in memory for the current authentication call. */
public record SignatureAuthenticationRequest(Long tenantId, String domain, Long actorId, Long operatorId,
        String requestIdentity, String credential) {
    public SignatureAuthenticationRequest {
        if (tenantId == null || tenantId <= 0 || actorId == null || actorId <= 0 || operatorId == null || operatorId <= 0
                || domain == null || requestIdentity == null || requestIdentity.isBlank() || requestIdentity.length() > 64
                || credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("SIGNATURE_AUTHENTICATION_REQUEST_INVALID");
        }
    }
    @Override public String toString() {
        return "SignatureAuthenticationRequest[tenantId=" + tenantId + ", domain=" + domain + ", actorId=" + actorId
                + ", operatorId=" + operatorId + ", requestIdentity=" + requestIdentity + ", credential=<redacted>]";
    }
}
