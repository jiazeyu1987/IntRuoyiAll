package cn.iocoder.yudao.module.signature.api.dto;

/** Internal business authorization result; never a public request DTO. */
public record AuthorizedSignatureIdentity(String domain, Long signerId, String displayName) {
    public static final String SYSTEM_USER = "SYSTEM_USER";
    public static final String EMPLOYEE_PROFILE = "MES_EMPLOYEE_PROFILE";

    public AuthorizedSignatureIdentity {
        if ((!SYSTEM_USER.equals(domain) && !EMPLOYEE_PROFILE.equals(domain))
                || signerId == null || signerId <= 0 || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Invalid authorized signature identity");
        }
    }
}
