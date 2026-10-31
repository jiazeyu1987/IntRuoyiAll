package cn.iocoder.yudao.module.mes.service.pro.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.*;

/** JDBC-only, release-scoped archival. No business runner or native snapshot rewrite. */
final class MesRouteLegacySummaryMigration {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String EVENT = MesRouteLegacySummaryIdentity.MIGRATION_ID;
    private static final List<String> REFERENCE_TABLES = List.of(
            "mes_independent_batch_prerequisite_receipt", "mes_pro_edhr_batch_execution", "mes_pro_feedback_material",
            "mes_pro_route_process_loss_reason", "mes_pro_route_schedule_config", "mes_pro_schedule_order",
            "mes_pro_schedule_order_process", "mes_pqc_inspection_task", "mes_pqc_process_inspection_aggregate_detail",
            "mes_pro_process_pool_active_order_completion_receipt", "mes_pro_process_pool_active_order",
            "mes_pro_process_pool_active_order_process_snapshot", "mes_pro_process_pool_active_order_release_application",
            "mes_pro_process_pool_active_order_transfer_trace", "mes_qa_inspection_regulation");
    private final Connection connection;
    private final boolean write;
    private final Map<String, String> context;
    private final Map<Long, Ref> refs = new HashMap<>();

    MesRouteLegacySummaryMigration(Connection connection, boolean write, Map<String, String> context) {
        this.connection = connection; this.write = write; this.context = context;
    }

    List<MesRouteSnapshotMigrationCli.Blocker> validate(List<MesRouteSnapshotMigrationCli.Row> rows, boolean readiness) throws SQLException {
        List<MesRouteSnapshotMigrationCli.Blocker> blockers = new ArrayList<>();
        List<MesRouteSnapshotMigrationCli.Row> summaries = rows.stream().filter(MesRouteSnapshotMigrationCli.Row::isApprovedSummary).toList();
        if (summaries.isEmpty()) return blockers;
        if (summaries.size() != 5) blockers.add(new MesRouteSnapshotMigrationCli.Blocker(null, null, null, "LEGACY_SUMMARY_SET_INCOMPLETE"));
        try (Statement query = connection.createStatement(); ResultSet result = query.executeQuery("SELECT COUNT(*) FROM information_schema.tables"
                + " WHERE table_schema=DATABASE() AND table_name IN ('mes_pro_route_version','controlled_content_version_ref',"
                + "'controlled_content_transition_audit') AND engine='InnoDB'")) {
            result.next();
            if (result.getInt(1) != 3) {
                blockers.add(new MesRouteSnapshotMigrationCli.Blocker(null, null, null, "LEGACY_TRANSACTIONAL_TABLES_REQUIRED"));
                return blockers;
            }
        }
        if (!columns("controlled_content_version_ref").containsAll(Set.of("id", "tenant_id", "content_type", "content_key",
                "native_master_id", "native_version_id", "version_no", "canonical_status", "domain_status",
                "source_version_ref_id", "source_native_version_id", "successor_version_ref_id", "successor_native_version_id",
                "active_unique_flag", "open_candidate_unique_flag", "approval_process_instance_id", "last_transition_time",
                "create_time", "update_time", "deleted"))
                || !columns("controlled_content_transition_audit").containsAll(Set.of("tenant_id", "version_ref_id", "content_type",
                "content_key", "from_status", "to_status", "domain_from_status", "domain_to_status", "action", "event_key", "reason"))) {
            blockers.add(new MesRouteSnapshotMigrationCli.Blocker(null, null, null, "LEGACY_CONTROLLED_SCHEMA_MISSING"));
            return blockers;
        }
        Map<String, Set<String>> referenceColumns = new LinkedHashMap<>();
        for (String table : REFERENCE_TABLES) referenceColumns.put(table, columns(table));
        for (var row : summaries) {
            Ref ref = readRef(row);
            boolean archived = MesRouteLegacySummaryIdentity.STATUS.equals(row.lifecycle());
            if (ref == null || !ref.valid(row, archived)) {
                blockers.add(row.blocker("LEGACY_CONTROLLED_REF_MISMATCH"));
                continue;
            }
            Ref previous = refs.get(row.id());
            if (previous != null && previous.id() != ref.id()) blockers.add(row.blocker("LEGACY_CONTROLLED_REF_ID_CHANGED"));
            refs.put(row.id(), ref);
            if (!auditValid(row, ref, archived)) blockers.add(row.blocker("LEGACY_AUDIT_MISMATCH"));
            if (readiness && !archived) blockers.add(row.blocker("LEGACY_ARCHIVAL_REQUIRED"));
            if (hasReferences(row, referenceColumns, rows)) blockers.add(row.blocker("LEGACY_EXECUTION_REFERENCE_EXISTS"));
        }
        return blockers;
    }

    private Set<String> columns(String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=?")) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) { while (result.next()) columns.add(result.getString(1)); }
        }
        return columns;
    }

    private boolean hasReferences(MesRouteSnapshotMigrationCli.Row row, Map<String, Set<String>> tables,
                                  List<MesRouteSnapshotMigrationCli.Row> allRows) throws SQLException {
        if (allRows.stream().anyMatch(other -> other.tenantId() == row.tenantId() && Objects.equals(other.sourceVersionId(), row.id()))) return true;
        for (var entry : tables.entrySet()) {
            if (entry.getValue().isEmpty()) continue; // The relation is not yet created; no row count is inferred or reported.
            if ("mes_pqc_process_inspection_aggregate_detail".equals(entry.getKey())) {
                if (aggregateReferences(row, entry.getValue(), tables.get("mes_pqc_inspection_task"))) return true;
                continue;
            }
            if (!entry.getValue().containsAll(Set.of("tenant_id", "route_version_id"))) throw new SQLException("reference schema mismatch");
            try (PreparedStatement query = connection.prepareStatement("SELECT route_version_id FROM " + entry.getKey()
                    + " WHERE tenant_id=? AND route_version_id=? LIMIT 1" + (write ? " FOR UPDATE" : ""))) {
                query.setLong(1, row.tenantId()); query.setLong(2, row.id());
                try (ResultSet result = query.executeQuery()) { if (result.next()) return true; }
            }
        }
        return false;
    }

    private boolean aggregateReferences(MesRouteSnapshotMigrationCli.Row row, Set<String> columns,
                                        Set<String> taskColumns) throws SQLException {
        // The original AC-M21 table has only pqc_task_id. Its runtime-closure migration
        // later adds a nullable route_version_id. Both persisted identities are checked.
        if (!columns.containsAll(Set.of("tenant_id", "pqc_task_id", "route_id"))
                || taskColumns == null || !taskColumns.containsAll(Set.of("id", "tenant_id", "route_version_id"))) {
            throw new SQLException("aggregate reference schema mismatch");
        }
        String direct = columns.contains("route_version_id") ? "a.route_version_id=? OR " : "";
        String sql = "SELECT a.pqc_task_id FROM mes_pqc_process_inspection_aggregate_detail a"
                + " LEFT JOIN mes_pqc_inspection_task t ON t.id=a.pqc_task_id AND t.tenant_id=a.tenant_id"
                + " WHERE a.tenant_id=? AND (" + direct + "t.route_version_id=? OR (t.id IS NULL AND a.route_id=?)) LIMIT 1"
                + (write ? " FOR UPDATE" : "");
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            int position = 1;
            query.setLong(position++, row.tenantId());
            if (!direct.isEmpty()) query.setLong(position++, row.id());
            query.setLong(position++, row.id()); query.setLong(position, row.routeId());
            try (ResultSet result = query.executeQuery()) { return result.next(); }
        }
    }

    private Ref readRef(MesRouteSnapshotMigrationCli.Row row) throws SQLException {
        String sql = "SELECT id,content_key,native_master_id,version_no,canonical_status,domain_status,active_unique_flag,"
                + "open_candidate_unique_flag,source_version_ref_id,source_native_version_id,successor_version_ref_id,"
                + "successor_native_version_id,approval_process_instance_id,create_time,update_time,deleted,last_transition_time"
                + " FROM controlled_content_version_ref WHERE tenant_id=? AND content_type='MES_ROUTE' AND native_version_id=?"
                + (write ? " FOR UPDATE" : "");
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, row.tenantId()); query.setLong(2, row.id());
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) return null;
                Ref ref = new Ref(result.getLong(1), result.getString(2), result.getLong(3), result.getString(4), result.getString(5),
                        result.getString(6), (Integer) result.getObject(7, Integer.class), result.getObject(8), result.getObject(9),
                        result.getObject(10), result.getObject(11), result.getObject(12), result.getString(13), result.getTimestamp(14),
                        result.getTimestamp(15), result.getBoolean(16), result.getTimestamp(17));
                return result.next() ? null : ref;
            }
        }
    }

    private boolean auditValid(MesRouteSnapshotMigrationCli.Row row, Ref ref, boolean archived) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT tenant_id,content_type,content_key,from_status,to_status,"
                + "domain_from_status,domain_to_status,action,event_key,reason FROM controlled_content_transition_audit WHERE version_ref_id=?"
                + (write ? " FOR UPDATE" : ""))) {
            query.setLong(1, ref.id());
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) return !archived;
                if (!archived) return false;
                boolean valid = result.getLong(1) == row.tenantId() && "MES_ROUTE".equals(result.getString(2))
                        && Long.toString(row.routeId()).equals(result.getString(3)) && "ACTIVE".equals(result.getString(4))
                        && MesRouteLegacySummaryIdentity.STATUS.equals(result.getString(5)) && "ACTIVE".equals(result.getString(6))
                        && MesRouteLegacySummaryIdentity.STATUS.equals(result.getString(7))
                        && MesRouteLegacySummaryIdentity.AUDIT_ACTION.equals(result.getString(8)) && EVENT.equals(result.getString(9))
                        && reasonValid(row, result.getString(10));
                return valid && !result.next();
            }
        }
    }

    private boolean reasonValid(MesRouteSnapshotMigrationCli.Row row, String reason) {
        try {
            JsonNode json = JSON.readTree(reason);
            return "mes-route-legacy-summary/v1".equals(json.path("schemaVersion").asText())
                    && MesRouteLegacySummaryIdentity.approved(row.id()).rawSha256().equals(json.path("rawSha256").asText())
                    && json.path("beforeHash").isNull() && json.path("beforeFormat").isNull()
                    && "ACTIVE".equals(json.path("beforeLifecycle").asText()) && json.path("beforeActive").asBoolean()
                    && MesRouteLegacySummaryIdentity.ORIGINAL_TIME.toString().equals(json.path("originalTime").asText())
                    && EVENT.equals(json.path("migrationId").asText())
                    && !json.path("releaseTag").asText().isBlank() && !json.path("imageId").asText().isBlank()
                    && !json.path("operationId").asText().isBlank() && json.path("packageIdentity").asText().matches("[0-9a-f]{64}");
        } catch (Exception invalid) { return false; }
    }

    void archive(MesRouteSnapshotMigrationCli.Row row) throws SQLException {
        Ref ref = refs.get(row.id());
        if (ref == null || !row.isApprovedSummary()) throw new SQLException("unvalidated summary");
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("schemaVersion", "mes-route-legacy-summary/v1");
        audit.put("rawSha256", MesRouteLegacySummaryIdentity.approved(row.id()).rawSha256());
        audit.put("beforeHash", null); audit.put("beforeFormat", null); audit.put("beforeLifecycle", "ACTIVE"); audit.put("beforeActive", true);
        audit.put("originalTime", MesRouteLegacySummaryIdentity.ORIGINAL_TIME.toString()); audit.put("migrationId", EVENT);
        for (String key : List.of("release-tag", "image-id", "operation-id", "package-identity")) {
            String value = context.get(key);
            if (value == null || value.isBlank()) throw new SQLException("audit context required");
            audit.put(switch (key) { case "release-tag" -> "releaseTag"; case "image-id" -> "imageId";
                case "operation-id" -> "operationId"; default -> "packageIdentity"; }, value);
        }
        final String reason;
        try { reason = JSON.writeValueAsString(audit); } catch (Exception invalid) { throw new SQLException("invalid audit context", invalid); }
        if (reason.length() > 1024 || !reasonValid(row, reason)) throw new SQLException("invalid audit binding");
        try (PreparedStatement update = connection.prepareStatement("UPDATE mes_pro_route_version SET lifecycle_status=?,active=b'0',"
                + "route_snapshot_sha256=?,route_snapshot_format_version=?,update_time=update_time WHERE id=? AND tenant_id=?"
                + " AND lifecycle_status='ACTIVE' AND active=b'1' AND route_snapshot_sha256 IS NULL AND route_snapshot_format_version IS NULL")) {
            update.setString(1, MesRouteLegacySummaryIdentity.STATUS); update.setString(2, MesRouteLegacySummaryIdentity.approved(row.id()).rawSha256());
            update.setString(3, MesRouteLegacySummaryIdentity.FORMAT_VERSION); update.setLong(4, row.id()); update.setLong(5, row.tenantId());
            if (update.executeUpdate() != 1) throw new SQLException("summary update count mismatch");
        }
        try (PreparedStatement update = connection.prepareStatement("UPDATE controlled_content_version_ref SET canonical_status=?,domain_status=?,"
                + "active_unique_flag=NULL,open_candidate_unique_flag=NULL,last_transition_time=NOW(),update_time=update_time WHERE id=? AND tenant_id=?"
                + " AND canonical_status='ACTIVE' AND domain_status='ACTIVE' AND active_unique_flag=1")) {
            update.setString(1, MesRouteLegacySummaryIdentity.STATUS); update.setString(2, MesRouteLegacySummaryIdentity.STATUS);
            update.setLong(3, ref.id()); update.setLong(4, row.tenantId());
            if (update.executeUpdate() != 1) throw new SQLException("controlled ref update count mismatch");
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO controlled_content_transition_audit"
                + "(tenant_id,version_ref_id,content_type,content_key,from_status,to_status,domain_from_status,domain_to_status,action,event_key,actor_id,reason)"
                + " VALUES(?,?,'MES_ROUTE',?,'ACTIVE',?,'ACTIVE',?,?,?,NULL,?)")) {
            insert.setLong(1, row.tenantId()); insert.setLong(2, ref.id()); insert.setString(3, Long.toString(row.routeId()));
            insert.setString(4, MesRouteLegacySummaryIdentity.STATUS); insert.setString(5, MesRouteLegacySummaryIdentity.STATUS);
            insert.setString(6, MesRouteLegacySummaryIdentity.AUDIT_ACTION); insert.setString(7, EVENT); insert.setString(8, reason);
            if (insert.executeUpdate() != 1) throw new SQLException("audit insert count mismatch");
        }
    }

    private record Ref(long id, String key, long routeId, String version, String canonical, String domain, Integer active,
                       Object open, Object sourceRef, Object sourceNative, Object successorRef, Object successorNative,
                       String approval, Timestamp created, Timestamp updated, boolean deleted, Timestamp transitionTime) {
        boolean valid(MesRouteSnapshotMigrationCli.Row row, boolean archived) {
            return !deleted && Long.toString(row.routeId()).equals(key) && routeId == row.routeId() && "V1".equals(version)
                    && sourceRef == null && sourceNative == null && successorRef == null && successorNative == null && open == null
                    && (approval == null || approval.isBlank()) && created != null && updated != null
                    && MesRouteLegacySummaryIdentity.ORIGINAL_TIME.equals(created.toLocalDateTime())
                    && MesRouteLegacySummaryIdentity.ORIGINAL_TIME.equals(updated.toLocalDateTime()) && transitionTime != null
                    && (archived ? MesRouteLegacySummaryIdentity.STATUS.equals(canonical) && canonical.equals(domain) && active == null
                    : "ACTIVE".equals(canonical) && "ACTIVE".equals(domain) && Integer.valueOf(1).equals(active)
                    && MesRouteLegacySummaryIdentity.ORIGINAL_TIME.equals(transitionTime.toLocalDateTime()));
        }
    }
}
