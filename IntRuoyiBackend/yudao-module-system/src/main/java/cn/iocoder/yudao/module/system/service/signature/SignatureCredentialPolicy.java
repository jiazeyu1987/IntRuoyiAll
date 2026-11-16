package cn.iocoder.yudao.module.system.service.signature;

import cn.iocoder.yudao.module.system.service.user.AdminUserPasswordPolicy;
import java.time.LocalDateTime;

/** Common rules only. Personnel domains keep their own stores and authorization. */
public final class SignatureCredentialPolicy {
    public static final int FAILURE_LIMIT = 5;
    public static final int FAILURE_WINDOW_MINUTES = 15;
    public static final int LOCK_MINUTES = 30;
    public static final int PASSWORD_HISTORY_LIMIT = 5;
    // 临时人员独立签名凭据保留原有周期；系统账号使用参数配置中的有效期。
    private static final int TEMPORARY_CREDENTIAL_MAX_AGE_DAYS = 90;
    private SignatureCredentialPolicy() {}
    public static String requireType(String type) {
        if (!"PIN".equals(type) && !"PASSWORD".equals(type))
            throw new IllegalStateException("签名方式缺失或无效，请核对签名配置");
        return type;
    }
    public static boolean isPin(String type) { return "PIN".equals(requireType(type)); }
    public static boolean validPin(String value) { return value != null && value.matches("[0-9]{6}"); }
    public static String credentialHash(String type, String pinHash, String passwordHash) {
        String hash = isPin(type) ? pinHash : passwordHash;
        if (hash == null || hash.isBlank()) throw new IllegalStateException("签名凭据未初始化，请核对数据库迁移");
        return hash;
    }
    /** Accepts the two explicit methods for the same identity context, including historical password signatures. */
    public static boolean matchesMethod(String actual, String passwordMethod) {
        if (actual == null || passwordMethod == null || !passwordMethod.matches("SESSION_PLUS_(SELECTED_USER_|EMPLOYEE_PROFILE_)?PASSWORD")) return false;
        return actual.equals(passwordMethod) || actual.equals(passwordMethod.replace("PASSWORD", "PIN"));
    }
    public static String authenticationMethod(String domain, boolean selectedUser, String type) {
        requireType(type);
        if ("MES_EMPLOYEE_PROFILE".equals(domain)) return "SESSION_PLUS_EMPLOYEE_PROFILE_" + type;
        if (!"SYSTEM_USER".equals(domain)) throw new IllegalArgumentException("SIGNATURE_IDENTITY_DOMAIN_UNSUPPORTED");
        return "SESSION_PLUS_" + (selectedUser ? "SELECTED_USER_" : "") + type;
    }
    public static boolean isStrong(String password) { return AdminUserPasswordPolicy.isStrong(password); }
    public static boolean isExpired(LocalDateTime changedAt, LocalDateTime now) {
        return changedAt == null || !changedAt.plusDays(TEMPORARY_CREDENTIAL_MAX_AGE_DAYS).isAfter(now);
    }
}
