package cn.iocoder.yudao.module.system.api.signature.dto;

import java.util.Set;
import java.util.UUID;

/** Internal per-request authentication result. Never accepted from an HTTP request. */
public record AuthenticatedSigner(Long tenantId, String domain, Long actorId, Long operatorId,
        String credentialGeneration, Long credentialVersion, String requestIdentity) {
    public AuthenticatedSigner {
        if (tenantId == null || tenantId <= 0 || actorId == null || actorId <= 0 || operatorId == null || operatorId <= 0
                || !Set.of("SYSTEM_USER", "MES_EMPLOYEE_PROFILE").contains(domain == null ? "" : domain)
                || credentialVersion == null || credentialVersion <= 0
                || credentialGeneration == null || !UUID.fromString(credentialGeneration).toString().equals(credentialGeneration)
                || requestIdentity == null || requestIdentity.isBlank() || requestIdentity.length() > 64) {
            throw new IllegalArgumentException("SIGNATURE_AUTHENTICATED_IDENTITY_INVALID");
        }
    }
}
