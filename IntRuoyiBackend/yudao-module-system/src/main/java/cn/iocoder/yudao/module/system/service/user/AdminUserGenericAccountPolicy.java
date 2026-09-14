package cn.iocoder.yudao.module.system.service.user;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Locale;

final class AdminUserGenericAccountPolicy {

    private static final List<String> GENERIC_USERNAME_BASES = List.of(
            "admin", "test", "user", "guest", "root", "demo", "system");

    private AdminUserGenericAccountPolicy() {
    }

    static boolean isGenericUsername(String username) {
        String normalizedUsername = normalizeUsername(username);
        if (StrUtil.isBlank(normalizedUsername)) {
            return false;
        }
        for (String base : GENERIC_USERNAME_BASES) {
            if (normalizedUsername.equals(base) || hasOnlyNumericSuffix(normalizedUsername, base)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeUsername(String username) {
        return StrUtil.trimToEmpty(username).toLowerCase(Locale.ROOT);
    }

    private static boolean hasOnlyNumericSuffix(String username, String base) {
        if (!username.startsWith(base) || username.length() == base.length()) {
            return false;
        }
        for (int i = base.length(); i < username.length(); i++) {
            if (!Character.isDigit(username.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
