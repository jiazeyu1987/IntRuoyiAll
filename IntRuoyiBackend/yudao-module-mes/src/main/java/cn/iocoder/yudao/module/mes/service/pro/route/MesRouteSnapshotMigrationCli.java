package cn.iocoder.yudao.module.mes.service.pro.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.util.*;

/** Offline release command. No Spring application, business runner, or current-route reconstruction. */
public final class MesRouteSnapshotMigrationCli {
    private static final String MIGRATION = MesRouteLegacySummaryIdentity.MIGRATION_ID;
    private static final Set<String> CANDIDATE = Set.of("DRAFT", "PENDING_APPROVAL", "READY_TO_PUBLISH", "REJECTED", "CANCELLED");
    private static final Set<String> MODES = Set.of("PRECHECK", "BACKFILL", "READINESS");
    private static final ObjectMapper JSON = new ObjectMapper();
    private final MesProRouteSnapshotCanonicalizer canonicalizer = new MesProRouteSnapshotCanonicalizer();

    public static void main(String[] args) {
        int exit = 3;
        try {
            Map<String, String> options = options(args);
            Path report = Path.of(options.get("report"));
            if (Files.exists(report) || report.getParent() == null || !Files.isDirectory(report.getParent())) {
                throw new IllegalArgumentException("fresh report path required");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("schemaVersion", "route-snapshot-migration/v1");
            for (String key : List.of("mode", "release-tag", "migration-id", "image-id", "operation-id", "package-identity")) {
                result.put(switch (key) {
                    case "release-tag" -> "releaseTag";
                    case "migration-id" -> "migrationId";
                    case "image-id" -> "imageId";
                    case "operation-id" -> "operationId";
                    case "package-identity" -> "packageIdentity";
                    default -> key;
                }, options.get(key));
            }
            try (Connection connection = DriverManager.getConnection(requiredEnv("MES_SNAPSHOT_JDBC_URL"),
                    requiredEnv("MES_SNAPSHOT_JDBC_USER"), requiredEnv("MES_SNAPSHOT_JDBC_PASSWORD"))) {
                result.put("targetDatabase", connection.getCatalog());
                Outcome outcome = new MesRouteSnapshotMigrationCli().execute(connection, options.get("mode"), options);
                result.put("status", outcome.blockers().isEmpty() ? "READY" : "BLOCKED");
                result.put("scannedCount", outcome.scannedCount());
                result.put("updatedCount", outcome.updatedCount());
                result.put("blockerCount", outcome.blockers().size());
                result.put("blockers", outcome.blockers());
                exit = outcome.blockers().isEmpty() ? 0 : 2;
            } catch (Exception failure) {
                // Driver/server messages may contain JDBC credentials or business JSON. Never serialize them.
                for (String field : List.of("scannedCount", "updatedCount", "blockerCount", "blockers")) result.remove(field);
                result.put("status", "ERROR");
                result.put("errorCode", failure instanceof SQLException ? "DATABASE_COMMAND_FAILED" : "COMMAND_FAILED");
                exit = 3;
            }
            Files.writeString(report, JSON.writeValueAsString(result), StandardOpenOption.CREATE_NEW);
        } catch (Exception failure) {
            exit = 3;
            System.err.println("ROUTE_SNAPSHOT_COMMAND_FAILED: inspect the private bound report; no sensitive diagnostics emitted");
        }
        System.exit(exit);
    }

    private static Map<String, String> options(String[] args) {
        Map<String, String> values = new LinkedHashMap<>();
        Set<String> keys = Set.of("mode", "report", "release-tag", "migration-id", "image-id", "operation-id", "package-identity");
        for (String argument : args) {
            int equals = argument.indexOf('=');
            if (!argument.startsWith("--") || equals < 3) throw new IllegalArgumentException("invalid argument");
            String key = argument.substring(2, equals), value = argument.substring(equals + 1);
            if (!keys.contains(key) || value.isBlank() || values.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("unknown or duplicate argument");
            }
        }
        if (!values.keySet().equals(keys) || !MODES.contains(values.get("mode"))
                || !MIGRATION.equals(values.get("migration-id"))) throw new IllegalArgumentException("invalid command contract");
        return values;
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("required credential input missing");
        return value;
    }

    public Outcome execute(Connection connection, String mode) throws SQLException {
        return execute(connection, mode, Map.of());
    }

    public Outcome execute(Connection connection, String mode, Map<String, String> auditContext) throws SQLException {
        if (!MODES.contains(mode) || !connection.getAutoCommit()) throw new IllegalArgumentException("fresh connection and explicit mode required");
        boolean write = "BACKFILL".equals(mode);
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        connection.setReadOnly(!write);
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(write ? "SET TRANSACTION READ WRITE" : "SET TRANSACTION READ ONLY");
            }
            Set<String> columns = columns(connection);
            if (!columns.containsAll(Set.of("id", "tenant_id", "route_id", "route_snapshot_json"))) {
                connection.rollback();
                return new Outcome(0, 0, List.of(new Blocker(null, null, null, "SCHEMA_MISSING")));
            }
            boolean hasHash = columns.contains("route_snapshot_sha256"), hasFormat = columns.contains("route_snapshot_format_version");
            if (hasHash != hasFormat || (!"PRECHECK".equals(mode) && (!hasHash || !columns.contains("lifecycle_status") || !columns.contains("update_time")))) {
                connection.rollback();
                return new Outcome(0, 0, List.of(new Blocker(null, null, null, "IDENTITY_SCHEMA_MISSING")));
            }
            List<Row> rows = rows(connection, columns, write);
            List<Blocker> blockers = validate(rows, "READINESS".equals(mode));
            MesRouteLegacySummaryMigration legacy = new MesRouteLegacySummaryMigration(connection, write, auditContext);
            blockers.addAll(legacy.validate(rows, "READINESS".equals(mode)));
            if (!blockers.isEmpty()) {
                connection.rollback();
                return new Outcome(rows.size(), 0, blockers);
            }
            int updated = 0;
            if (write) {
                try (PreparedStatement statement = connection.prepareStatement("UPDATE mes_pro_route_version SET route_snapshot_sha256=?,route_snapshot_format_version=?,update_time=update_time WHERE id=? AND tenant_id=? AND route_snapshot_sha256 IS NULL AND route_snapshot_format_version IS NULL")) {
                    for (Row row : rows) {
                        if (row.hash() != null) continue;
                        if (row.isApprovedSummary()) {
                            legacy.archive(row);
                            updated++;
                            continue;
                        }
                        statement.setString(1, canonicalizer.sha256(row.json()));
                        statement.setString(2, MesProRouteSnapshotCanonicalizer.FORMAT_VERSION);
                        statement.setLong(3, row.id());
                        statement.setLong(4, row.tenantId());
                        if (statement.executeUpdate() != 1) throw new SQLException("identity update count mismatch");
                        updated++;
                    }
                }
                List<Row> persisted = rows(connection, columns, true);
                if (persisted.size() != rows.size()) throw new SQLException("row set changed");
                for (int i = 0; i < rows.size(); i++) {
                    Row before = rows.get(i), after = persisted.get(i);
                    if (before.id() != after.id() || before.routeId() != after.routeId() || before.tenantId() != after.tenantId()
                            || !Objects.equals(before.json(), after.json())
                            || !Objects.equals(before.versionNo(), after.versionNo()) || !Objects.equals(before.remark(), after.remark())
                            || !Objects.equals(before.created(), after.created()) || !Objects.equals(before.updated(), after.updated())
                            || !Objects.equals(before.deleted(), after.deleted()) || !Objects.equals(before.approval(), after.approval())
                            || !Objects.equals(before.sourceVersionId(), after.sourceVersionId())
                            || (!before.isApprovedSummary() && (!Objects.equals(before.lifecycle(), after.lifecycle())
                            || !Objects.equals(before.active(), after.active())))) {
                        throw new SQLException("immutable snapshot changed");
                    }
                }
                blockers = validate(persisted, true);
                blockers.addAll(legacy.validate(persisted, true));
                if (!blockers.isEmpty()) {
                    connection.rollback();
                    return new Outcome(rows.size(), 0, blockers);
                }
                connection.commit();
            } else {
                connection.rollback();
            }
            return new Outcome(rows.size(), updated, List.of());
        } catch (SQLException | RuntimeException failure) {
            connection.rollback();
            throw failure;
        }
    }

    private static Set<String> columns(Connection connection) throws SQLException {
        Set<String> result = new HashSet<>();
        // Inspect the exact database before referencing application columns.
        try (Statement statement = connection.createStatement(); ResultSet tables = statement.executeQuery("SHOW TABLES LIKE 'mes_pro_route_version'")) {
            if (!tables.next()) return result;
        }
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("DESCRIBE mes_pro_route_version")) {
            while (rows.next()) result.add(rows.getString(1));
        }
        return result;
    }

    private static List<Row> rows(Connection connection, Set<String> columns, boolean lock) throws SQLException {
        // The immutable 20260715 lifecycle migration adds exactly DEFAULT ACTIVE; PRECHECK models that DDL only.
        String lifecycle = columns.contains("lifecycle_status") ? "lifecycle_status" : "'ACTIVE'";
        String identity = columns.contains("route_snapshot_sha256") ? "route_snapshot_sha256,route_snapshot_format_version" : "NULL,NULL";
        String metadata = "," + expression(columns, "version_no") + "," + expression(columns, "active")
                + "," + expression(columns, "remark") + "," + expression(columns, "create_time")
                + "," + expression(columns, "update_time") + "," + expression(columns, "deleted")
                + "," + expression(columns, "approval_process_instance_id") + "," + expression(columns, "source_route_version_id");
        String query = "SELECT id,tenant_id,route_id,route_snapshot_json," + lifecycle + "," + identity + metadata
                + " FROM mes_pro_route_version ORDER BY id" + (lock ? " FOR UPDATE" : "");
        List<Row> result = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(query)) {
            while (rows.next()) result.add(new Row(rows.getLong(1), rows.getLong(2), rows.getLong(3), rows.getString(4),
                    rows.getString(5), rows.getString(6), rows.getString(7), rows.getString(8),
                    (Boolean) rows.getObject(9, Boolean.class), rows.getString(10),
                    localTime(rows.getTimestamp(11)), localTime(rows.getTimestamp(12)),
                    (Boolean) rows.getObject(13, Boolean.class), rows.getString(14), (Long) rows.getObject(15, Long.class)));
        }
        return result;
    }

    private static String expression(Set<String> columns, String column) { return columns.contains(column) ? column : "NULL"; }
    private static java.time.LocalDateTime localTime(Timestamp value) { return value == null ? null : value.toLocalDateTime(); }

    private List<Blocker> validate(List<Row> rows, boolean requireIdentity) {
        List<Blocker> result = new ArrayList<>();
        for (Row row : rows) {
            if (row.isApprovedSummary()) {
                boolean before = "ACTIVE".equals(row.lifecycle()) && Boolean.TRUE.equals(row.active()) && row.hash() == null && row.format() == null;
                boolean after = MesRouteLegacySummaryIdentity.STATUS.equals(row.lifecycle()) && Boolean.FALSE.equals(row.active())
                        && MesRouteLegacySummaryIdentity.FORMAT_VERSION.equals(row.format())
                        && MesRouteLegacySummaryIdentity.approved(row.id()).rawSha256().equals(row.hash());
                if (!(after || before && !requireIdentity)) result.add(row.blocker("LEGACY_SUMMARY_STATE_INVALID"));
                continue;
            }
            if (MesRouteLegacySummaryIdentity.isLegacyMarker(row.lifecycle(), row.format())) {
                result.add(row.blocker("LEGACY_SUMMARY_PROVENANCE_MISMATCH"));
                continue;
            }
            var validation = (row.lifecycle() != null && CANDIDATE.contains(row.lifecycle())) ? canonicalizer.validateCandidate(row.routeId(), row.json())
                    : canonicalizer.validate(row.routeId(), row.json());
            for (var blocker : validation.blockers()) result.add(row.blocker(blocker.reasonCode()));
            if (!validation.ready()) continue;
            if ((row.hash() == null) != (row.format() == null)) result.add(row.blocker("SNAPSHOT_IDENTITY_PARTIAL"));
            else if (row.hash() == null) {
                if (requireIdentity) result.add(row.blocker("SNAPSHOT_IDENTITY_MISSING"));
            } else if (!MesProRouteSnapshotCanonicalizer.FORMAT_VERSION.equals(row.format())) result.add(row.blocker("SNAPSHOT_FORMAT_UNSUPPORTED"));
            else if (!canonicalizer.sha256(row.json()).equals(row.hash())) result.add(row.blocker("SNAPSHOT_HASH_MISMATCH"));
        }
        return result;
    }

    record Row(long id, long tenantId, long routeId, String json, String lifecycle, String hash, String format,
               String versionNo, Boolean active, String remark, java.time.LocalDateTime created,
               java.time.LocalDateTime updated, Boolean deleted, String approval, Long sourceVersionId) {
        boolean isApprovedSummary() {
            return Boolean.FALSE.equals(deleted) && (approval == null || approval.isBlank()) && sourceVersionId == null
                    && MesRouteLegacySummaryIdentity.matchesProvenance(id, tenantId, routeId, versionNo, json, remark, created, updated);
        }
        Blocker blocker(String reason) { return new Blocker(id, routeId, tenantId, reason); }
    }
    public record Blocker(Long routeVersionId, Long routeId, Long tenantId, String reasonCode) { }
    public record Outcome(int scannedCount, int updatedCount, List<Blocker> blockers) { }
}
