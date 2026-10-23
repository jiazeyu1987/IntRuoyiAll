package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

/** Approved historical summaries only. This identity never establishes an executable snapshot. */
public final class MesRouteLegacySummaryIdentity {
    public static final String STATUS = "LEGACY_INCOMPLETE";
    public static final String FORMAT_VERSION = "MES_ROUTE_LEGACY_SUMMARY_V1";
    public static final String MIGRATION_ID = "20261023_mes_route_snapshot_legacy_summary_enforce";
    public static final String AUDIT_ACTION = "ARCHIVE_LEGACY_SUMMARY";
    public static final String REMARK = "legacy route version backfill";
    public static final LocalDateTime ORIGINAL_TIME = LocalDateTime.of(2026, 6, 20, 18, 3, 42);
    private static final Set<String> KEYS = Set.of("routeId", "routeCode", "routeName", "description", "status", "remark");
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private static final Map<Long, ApprovedSummary> APPROVED = Map.of(
            1L, new ApprovedSummary(1, 1, 900021, "c9f10288575db4957db1d5b4d95ae0299be3c93c105fba1eaea8c42e26ca0ef6"),
            2L, new ApprovedSummary(2, 1, 900022, "05dbc0887aacf0d815286c679693a68591fa1d05de009ad356dfaee01269574c"),
            3L, new ApprovedSummary(3, 1, 900025, "9c6436a3f8e89695f2d1bb6e0f4420c4dc31143fc2448d116c31e8dbac00af1c"),
            4L, new ApprovedSummary(4, 1, 900026, "fc1360f8b3221d8789165f43b22296b367b518242133d811a41f728f3a41f5e4"),
            5L, new ApprovedSummary(5, 122, 922022, "19a6df382079f1510b65cbcc3c9a24dc4df38fecf1abe729b0b65f2a664776d9"));

    private MesRouteLegacySummaryIdentity() { }

    public static ApprovedSummary approved(Long id) { return id == null ? null : APPROVED.get(id); }

    public static boolean matchesProvenance(Long id, Long tenantId, Long routeId, String versionNo,
                                             String snapshot, String remark, LocalDateTime created, LocalDateTime updated) {
        ApprovedSummary expected = approved(id);
        if (expected == null || tenantId == null || routeId == null || expected.tenantId() != tenantId
                || expected.routeId() != routeId || !"V1".equals(versionNo) || !REMARK.equals(remark)
                || !ORIGINAL_TIME.equals(created) || !ORIGINAL_TIME.equals(updated) || snapshot == null
                || !expected.rawSha256().equals(rawSha256(snapshot))) return false;
        try {
            JsonNode root = JSON.readTree(snapshot);
            if (root == null || !root.isObject() || root.size() != KEYS.size()
                    || !root.path("routeId").isIntegralNumber() || root.path("routeId").longValue() != routeId) return false;
            var fields = root.fieldNames();
            while (fields.hasNext()) if (!KEYS.contains(fields.next())) return false;
            return true;
        } catch (Exception invalid) {
            return false;
        }
    }

    public static boolean isKnownArchivedIdentity(MesProRouteVersionDO version) {
        return version != null && Boolean.FALSE.equals(version.getDeleted()) && Boolean.FALSE.equals(version.getActive())
                && STATUS.equals(version.getLifecycleStatus()) && FORMAT_VERSION.equals(version.getRouteSnapshotFormatVersion())
                && matchesProvenance(version.getId(), version.getTenantId(), version.getRouteId(), version.getVersionNo(),
                version.getRouteSnapshotJson(), version.getRemark(), version.getCreateTime(), version.getUpdateTime())
                && approved(version.getId()).rawSha256().equals(version.getRouteSnapshotSha256());
    }

    public static boolean isLegacyMarker(String lifecycle, String format) {
        return STATUS.equals(lifecycle) || FORMAT_VERSION.equals(format);
    }

    public static String rawSha256(String snapshot) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(snapshot.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is required", unavailable);
        }
    }

    public record ApprovedSummary(long id, long tenantId, long routeId, String rawSha256) { }
}
