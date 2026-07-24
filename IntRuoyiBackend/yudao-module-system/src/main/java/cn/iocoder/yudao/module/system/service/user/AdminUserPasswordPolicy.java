package cn.iocoder.yudao.module.system.service.user;

import java.time.LocalDateTime;

/**
 * 后台用户密码策略。
 */
public final class AdminUserPasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_AGE_DAYS = 90;

    private AdminUserPasswordPolicy() {
    }

    public static boolean isStrong(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            hasLetter = hasLetter || isAsciiLetter(ch);
            hasDigit = hasDigit || Character.isDigit(ch);
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExpired(LocalDateTime passwordUpdateTime, LocalDateTime now) {
        return passwordUpdateTime == null || passwordUpdateTime.plusDays(MAX_AGE_DAYS).isBefore(now);
    }

    private static boolean isAsciiLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }
}
