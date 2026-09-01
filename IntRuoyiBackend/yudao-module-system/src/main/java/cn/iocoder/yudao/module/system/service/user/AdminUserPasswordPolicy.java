package cn.iocoder.yudao.module.system.service.user;

import java.time.LocalDateTime;

/**
 * 后台用户密码策略。
 */
public final class AdminUserPasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_AGE_DAYS = 365;
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?`~";

    private AdminUserPasswordPolicy() {
    }

    public static boolean isStrong(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecialCharacter = false;
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            hasUppercase = hasUppercase || isAsciiUppercaseLetter(ch);
            hasLowercase = hasLowercase || isAsciiLowercaseLetter(ch);
            hasDigit = hasDigit || Character.isDigit(ch);
            hasSpecialCharacter = hasSpecialCharacter || isSpecialCharacter(ch);
            if (hasUppercase && hasLowercase && hasDigit && hasSpecialCharacter) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExpired(LocalDateTime passwordUpdateTime, LocalDateTime now) {
        return passwordUpdateTime == null || passwordUpdateTime.plusDays(MAX_AGE_DAYS).isBefore(now);
    }

    private static boolean isAsciiUppercaseLetter(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    private static boolean isAsciiLowercaseLetter(char ch) {
        return ch >= 'a' && ch <= 'z';
    }

    private static boolean isSpecialCharacter(char ch) {
        return SPECIAL_CHARACTERS.indexOf(ch) >= 0;
    }
}
