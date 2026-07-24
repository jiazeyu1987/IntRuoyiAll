package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class MesProBatchRecordRecognitionRouteKeys {

    public static final String LEGACY = "LEGACY";
    public static final String A = "A";
    public static final String B = "B";
    public static final String C = "C";
    public static final String D = "D";
    public static final String E = "E";
    public static final String F = "F";

    private static final Set<String> FIXED_ROUTE_KEYS = Set.of(A, B, C, D, E, F);

    private MesProBatchRecordRecognitionRouteKeys() {
    }

    public static boolean isFixedRoute(String routeKey) {
        return FIXED_ROUTE_KEYS.contains(normalize(routeKey));
    }

    public static Set<String> fixedRouteKeysInOrder() {
        return new LinkedHashSet<>(FIXED_ROUTE_KEYS);
    }

    public static String normalize(String routeKey) {
        return routeKey == null ? "" : routeKey.trim().toUpperCase(Locale.ROOT);
    }
}
