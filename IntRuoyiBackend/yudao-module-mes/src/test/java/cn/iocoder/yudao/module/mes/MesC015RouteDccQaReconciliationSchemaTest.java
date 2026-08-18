package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesC015RouteDccQaReconciliationSchemaTest {

    private static final String PREFIX = "sql/mysql/20260814_mes_c015_route_dcc_qa_reconciliation_";

    @Test
    void reconciliationMustConvergeToVersionedRouteBindingAndGeneratedQaActiveIdentity() throws Exception {
        String bootstrap = readRequired(PREFIX + "bootstrap.sql");
        String preflight = readRequired(PREFIX + "preflight.sql");
        String schema = readRequired(PREFIX + "schema.sql");
        String backfill = readRequired(PREFIX + "backfill.sql");
        String postflight = readRequired(PREFIX + "postflight.sql");
        String all = bootstrap + preflight + schema + backfill + postflight;

        assertTrue(bootstrap.contains("dependsOn=20260812_mes_pqc_dcc_qa_c00_postflight"));
        assertTrue(bootstrap.contains("`product_master_id` bigint DEFAULT NULL"));
        assertTrue(bootstrap.contains("idx_mes_md_item_product_master"));
        assertFalse(bootstrap.matches("(?s).*(UPDATE|INSERT)\\s+`?mes_md_item`?.*"),
                "bootstrap must not infer or populate product-master relationships");
        assertTrue(preflight.contains("dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_bootstrap"));
        assertTrue(backfill.contains("dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_preflight"));
        assertTrue(schema.contains("dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_backfill"));
        assertTrue(postflight.contains("dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_schema"));

        assertTrue(preflight.contains("information_schema.columns"));
        assertTrue(preflight.contains("information_schema.statistics"));
        assertTrue(preflight.contains("generation_expression"));
        assertTrue(preflight.contains("c015_reconciliation_blocker_report"));
        assertTrue(preflight.contains("SIGNAL SQLSTATE '45000'"));

        assertFalse(schema.contains("ADD COLUMN `product_master_id`"),
                "the structural prerequisite must be owned only by the bootstrap stage");
        assertTrue(schema.contains("`version` bigint NOT NULL"));
        assertTrue(schema.contains("`active_route_id` BIGINT GENERATED ALWAYS AS"));
        assertTrue(schema.contains("uk_mes_pro_route_dcc_current"));
        assertTrue(schema.contains("uk_mes_pro_route_dcc_history_version"));
        assertFalse(schema.contains("binding_status"));
        assertFalse(schema.contains("route_version_id"));

        assertTrue(schema.contains("`active_dcc_project_code_id` BIGINT GENERATED ALWAYS AS"));
        assertTrue(schema.contains("uk_mes_qa_regulation_active_dcc"));
        assertTrue(schema.contains("uk_mes_qa_regulation_dcc_project"));
        assertTrue(schema.contains("DROP INDEX"));
        assertTrue(schema.contains("`inspection_rule_key` varchar(32) NOT NULL"));
        assertFalse(schema.contains("varchar(128)"));

        assertTrue(backfill.contains("input_manifest_sha256"));
        assertTrue(backfill.contains("approved"));
        assertTrue(backfill.contains("affected_row_count"));
        assertFalse(backfill.matches("(?s).*dcc_project_code_id`?\s*=\s*(129|147).*"));
        assertTrue(postflight.contains("c015_reconciliation_postflight"));
        assertTrue(postflight.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(preflight.contains("removed_active_order_route_identity"));
        assertTrue(preflight.contains("removed_active_order_process_snapshot_identity"));
        assertTrue(preflight.contains("removed_active_order_pqc_task_identity"));
        assertTrue(postflight.contains("removed_active_order_route_identity"));
        assertTrue(postflight.contains("removed_active_order_process_snapshot_identity"));
        assertTrue(postflight.contains("removed_active_order_pqc_task_identity"));
        assertTrue(all.contains("FIRST"));
        assertTrue(all.contains("PATROL_AM"));
        assertTrue(all.contains("PATROL_PM"));
        assertTrue(all.contains("FINAL"));
    }

    @Test
    void preflightMustSeparateCurrentAuthoringFromHistoricalQaReferentialValidity() throws Exception {
        String preflight = readRequired(PREFIX + "preflight.sql");

        assertTrue(preflight.contains("qa_dcc_referential_identity"));
        assertTrue(preflight.contains("active_order_qa_history_identity"));
        assertTrue(preflight.contains("version.lifecycle_status IN ('PUBLISHED', 'RETIRED')"));
        assertTrue(preflight.contains("project.status = 'ENABLE'"),
                "current route-DCC production eligibility must still require enabled DCC");

        int referentialStart = preflight.indexOf("qa_dcc_referential_identity");
        int historicalStart = preflight.indexOf("active_order_qa_history_identity");
        assertTrue(referentialStart >= 0 && historicalStart > referentialStart);
        String referentialCheck = preflight.substring(referentialStart, historicalStart);
        assertFalse(referentialCheck.contains("project.status = 'ENABLE'"),
                "disabled DCC must remain valid for same-tenant historical QA ownership");
    }

    @Test
    void removedHistoryGateMustRequireExactFrozenRouteProcessAndQaOwnedTaskIdentities() throws Exception {
        String preflight = readRequired(PREFIX + "preflight.sql");
        String postflight = readRequired(PREFIX + "postflight.sql");

        for (String sql : new String[]{preflight, postflight}) {
            assertTrue(sql.contains("active_order.active_status = 'REMOVED'"));
            assertTrue(sql.contains("route_version.route_id = active_order.route_id"));
            assertTrue(sql.contains("process_snapshot.work_order_id <> active_order.work_order_id"));
            assertTrue(sql.contains("process_snapshot.route_version_id <> active_order.route_version_id"));
            assertTrue(sql.contains("pqc_task.regulation_version_id <> active_order.qa_regulation_version_id"));
            assertTrue(sql.contains("qa_process.regulation_version_id = active_order.qa_regulation_version_id"));
            assertFalse(sql.contains("ORDER BY qa_process.id LIMIT 1"));
            assertFalse(sql.contains("ORDER BY process_snapshot.id LIMIT 1"));
        }
    }

    private static String readRequired(String relative) throws Exception {
        Path path = resolveBackendPath(relative);
        assertTrue(Files.exists(path), "missing reconciliation migration: " + relative);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path resolveBackendPath(String relative) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.getParent().resolve(relative);
        }
        return cwd.resolve(relative);
    }
}
