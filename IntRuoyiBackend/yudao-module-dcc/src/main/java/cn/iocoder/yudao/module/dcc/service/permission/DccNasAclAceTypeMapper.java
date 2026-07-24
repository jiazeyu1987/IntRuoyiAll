package cn.iocoder.yudao.module.dcc.service.permission;

import java.util.Locale;
import java.util.Set;

final class DccNasAclAceTypeMapper {

    private static final Set<String> ALLOW_TYPES = Set.of("ALLOW", "ACCESS_ALLOWED_ACE_TYPE");
    private static final Set<String> DENY_TYPES = Set.of("DENY", "ACCESS_DENIED_ACE_TYPE");

    private DccNasAclAceTypeMapper() {
    }

    static boolean isAllow(String aceType) {
        return isOneOf(aceType, ALLOW_TYPES);
    }

    static boolean isDeny(String aceType) {
        return isOneOf(aceType, DENY_TYPES);
    }

    private static boolean isOneOf(String aceType, Set<String> supportedTypes) {
        if (aceType == null) {
            return false;
        }
        return supportedTypes.contains(aceType.trim().toUpperCase(Locale.ROOT));
    }
}
