package cn.iocoder.yudao.module.system.service.signature;

import cn.iocoder.yudao.module.system.service.user.AdminUserPasswordPolicy;
import java.time.LocalDateTime;

/** Common rules only. Personnel domains keep their own stores and authorization. */
public final class SignatureCredentialPolicy {
    public static final int FAILURE_LIMIT = 5;
    public static final int FAILURE_WINDOW_MINUTES = 15;
    public static final int LOCK_MINUTES = 30;
    public static final int PASSWORD_HISTORY_LIMIT = 5;
    private SignatureCredentialPolicy() {}
    public static boolean isStrong(String password) { return AdminUserPasswordPolicy.isStrong(password); }
    public static boolean isExpired(LocalDateTime changedAt, LocalDateTime now) {
        return AdminUserPasswordPolicy.isExpired(changedAt, now);
    }
}
