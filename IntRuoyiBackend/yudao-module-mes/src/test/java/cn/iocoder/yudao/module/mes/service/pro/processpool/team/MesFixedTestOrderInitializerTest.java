package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteSnapshotCanonicalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderReferenceGraph.*;

/** Isolated SQL tests; no connection to the application database and no production operations. */
class MesFixedTestOrderInitializerTest {
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    MesFixedTestOrderReferenceGraph graph;
    final MesProRouteSnapshotCanonicalizer canonicalizer = new MesProRouteSnapshotCanonicalizer();

    @BeforeEach
    void setup() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:fixed" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        graph = new MesFixedTestOrderReferenceGraph(jdbc, 165L, 77L);
        // The import allowlist supplies table columns; assertions below exercise real SQL identities,
        // conflicting existing rows, reference traversal and actual database rollback.
        for (String table : List.of("mes_md_item", "mes_pro_route", "mes_pro_route_product", "mes_pro_process",
                "mes_pro_route_process", "mes_pro_route_version", "mes_pro_route_schedule_config")) createTable(table);
        jdbc.execute("CREATE TABLE system_users (id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(80),nickname VARCHAR(80),status INT,deleted INT)");
        jdbc.execute("CREATE TABLE mes_pro_edhr_permission_scope (id BIGINT PRIMARY KEY,tenant_id BIGINT,object_type VARCHAR(80),object_id VARCHAR(160),status VARCHAR(32),deleted INT)");
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,name) VALUES(10,1,0,'PRODUCT','Fixed product')");
        jdbc.update("INSERT INTO mes_pro_route(id,tenant_id,deleted,code,name,status) VALUES(20,1,0,'RT-FIXED','Fixed route','0')");
        jdbc.update("INSERT INTO mes_pro_route_product(id,tenant_id,deleted,route_id,item_id) VALUES(30,1,0,20,10)");
        jdbc.update("INSERT INTO mes_pro_process(id,tenant_id,deleted,code,name) VALUES(40,1,0,'PROCESS','Fixed process')");
        jdbc.update("INSERT INTO mes_pro_route_process(id,tenant_id,deleted,route_id,process_id,sort) VALUES(50,1,0,20,40,'1')");
        jdbc.update("INSERT INTO mes_pro_route_version(id,tenant_id,deleted,route_id,version_no,active,lifecycle_status) VALUES(60,1,0,20,'V1','1','ACTIVE')");
        jdbc.update("INSERT INTO mes_pro_route_schedule_config(id,tenant_id,deleted,route_version_id,route_process_id) VALUES(70,1,0,60,50)");
    }

    @SuppressWarnings("unchecked")
    void createTable(String table) {
        Map<String, Spec> specs = (Map<String, Spec>) ReflectionTestUtils.getField(graph, "specs");
        List<String> columns = new ArrayList<>(List.of("id BIGINT PRIMARY KEY", "tenant_id BIGINT", "deleted INT DEFAULT 0", "creator VARCHAR(80)", "updater VARCHAR(80)"));
        Set<String> integers = Set.of("sort", "report_sort", "version", "config_version", "graph_version", "child_form_member_count", "source_table_index", "priority");
        Set<String> booleans = Set.of("active", "enabled", "key_flag", "check_flag", "temporary_frozen", "night_shift_enabled", "recordbook_enabled", "final_inspection_applicable");
        Map<String, String> generated = switch (table) {
            case "mes_pro_route_dcc_project_binding" -> Map.of("active_route_id", "BIGINT GENERATED ALWAYS AS (CASE WHEN deleted=0 THEN route_id ELSE NULL END)");
            case "mes_qa_inspection_regulation" -> Map.of("active_dcc_project_code_id", "BIGINT GENERATED ALWAYS AS (CASE WHEN deleted=0 AND owner_module='MES_QA' THEN dcc_project_code_id ELSE NULL END)");
            case "mes_qa_common_regulation_product_binding" -> Map.of("active_binding_key", "VARCHAR(80) GENERATED ALWAYS AS (CASE WHEN deleted=0 AND binding_status='ENABLED' THEN CAST(product_id AS VARCHAR) ELSE NULL END)");
            default -> Map.of();
        };
        for (String field : specs.get(table).columns().split(",")) columns.add(field + (generated.containsKey(field) ? " " + generated.get(field)
                : field.endsWith("_id") && !field.startsWith("source_")
                && !specs.get(table).references().getOrDefault(field, "").startsWith("jimu_") ? " BIGINT"
                : field.contains("quantity") ? " DECIMAL(18,6)"
                : integers.contains(field) || field.equals("status") && !table.equals("mes_pro_edhr_permission_scope") && !table.equals("mes_pro_edhr_permission_rule") ? " INT"
                : booleans.contains(field) ? " BOOLEAN" : " VARCHAR(100000)"));
        jdbc.execute("CREATE TABLE " + table + " (" + String.join(",", columns) + ")");
    }

    ObjectNode snapshot() {
        return (ObjectNode) graph.parse("""
                {"id":20,"routeId":20,"configSnapshots":{
                "products":[{"id":30,"routeId":20,"itemId":10,"creator":"source-user","createTime":"2020-01-01"}],
                "flowGraph":{"routeId":20,"nodes":[{"routeProcessId":50,"processId":40}],"edges":[],"boundaryEdges":[],"layouts":[]},
                "scheduleConfigs":{"50":{"id":70,"routeId":20,"routeVersionId":60,"routeProcessId":50}},
                "batchUseConfigs":[],"scheduleUseConfigs":[],"productionProcessConfigs":[]}}
                """);
    }

    void importSnapshot(MesFixedTestOrderReferenceGraph target, ObjectNode json) {
        var mapper = new MesFixedTestOrderSnapshotMapper(target, jdbc, 165L);
        mapper.collect(json);
        target.resolveAll();
        mapper.remap(json);
        var version = target.entry("mes_pro_route_version", 60L);
        String canonical = canonicalizer.canonicalize(json.toString());
        version.overrides.put("route_snapshot_json", canonical);
        version.overrides.put("route_snapshot_sha256", canonicalizer.sha256(canonical));
        target.insertAll();
    }

    @Test
    void firstImportRemapsJsonAndRelationalReferencesWithoutChangingSource() {
        ObjectNode json = snapshot();
        tx.executeWithoutResult(s -> importSnapshot(graph, json));
        long route = ((Number) graph.id("mes_pro_route", 20L)).longValue();
        long process = ((Number) graph.id("mes_pro_route_process", 50L)).longValue();
        assertNotEquals(20L, route);
        assertNotEquals(50L, process);
        assertTrue(canonicalizer.validate(route, json.toString()).ready());
        assertTrue(json.path("configSnapshots").path("scheduleConfigs").has(String.valueOf(process)));
        assertFalse(json.path("configSnapshots").path("products").get(0).has("creator"));
        assertFalse(json.toString().contains("source-user"));
        assertEquals(route, jdbc.queryForObject("SELECT route_id FROM mes_pro_route_process WHERE tenant_id=165", Long.class));
        assertEquals(10L, jdbc.queryForObject("SELECT item_id FROM mes_pro_route_product WHERE tenant_id=1", Long.class));
        assertEquals("Fixed route", jdbc.queryForObject("SELECT name FROM mes_pro_route WHERE id=20 AND tenant_id=1", String.class));
        assertEquals("77", jdbc.queryForObject("SELECT creator FROM mes_pro_route WHERE tenant_id=165", String.class));
    }

    @Test
    void repeatImportKeepsIdentitiesAndDoesNotDuplicateDefinitions() {
        tx.executeWithoutResult(s -> importSnapshot(graph, snapshot()));
        Object original = graph.id("mes_pro_route", 20L);
        var repeat = new MesFixedTestOrderReferenceGraph(jdbc, 165L, 88L);
        tx.executeWithoutResult(s -> importSnapshot(repeat, snapshot()));
        assertEquals(original, repeat.id("mes_pro_route", 20L));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_version WHERE tenant_id=165", Integer.class));
        assertEquals("77", jdbc.queryForObject("SELECT creator FROM mes_pro_route WHERE tenant_id=165", String.class));
    }

    @Test
    void partialTargetReusesNaturalProductAndCreatesOnlyMissingRelationships() {
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,name,remark) VALUES(901,165,0,'PRODUCT','Fixed product','Keep formal note')");
        tx.executeWithoutResult(s -> importSnapshot(graph, snapshot()));
        assertEquals(901L, graph.id("mes_md_item", 10L));
        assertEquals(901L, jdbc.queryForObject("SELECT item_id FROM mes_pro_route_product WHERE tenant_id=165", Long.class));
        assertEquals("Keep formal note", jdbc.queryForObject("SELECT remark FROM mes_md_item WHERE id=901", String.class));
    }

    @Test
    void otherTargetTenantGetsIndependentIds() {
        tx.executeWithoutResult(s -> importSnapshot(graph, snapshot()));
        var other = new MesFixedTestOrderReferenceGraph(jdbc, 166L, 99L);
        tx.executeWithoutResult(s -> importSnapshot(other, snapshot()));
        assertNotEquals(graph.id("mes_pro_route", 20L), other.id("mes_pro_route", 20L));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route WHERE tenant_id=165", Integer.class));
        assertEquals("99", jdbc.queryForObject("SELECT creator FROM mes_pro_route WHERE tenant_id=166", String.class));
    }

    @Test
    void conflictingFormalTargetStopsBeforeAnyInsertAndPreservesTarget() {
        jdbc.update("INSERT INTO mes_pro_route(id,tenant_id,deleted,code,name,status) VALUES(902,165,0,'RT-FIXED','Do not overwrite','0')");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> tx.executeWithoutResult(s -> importSnapshot(graph, snapshot())));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=165", Integer.class));
        assertEquals("Do not overwrite", jdbc.queryForObject("SELECT name FROM mes_pro_route WHERE id=902", String.class));
    }

    @Test
    void databaseFailureAfterFirstInsertRollsBackWholeImport() {
        graph.include("mes_md_item", 10L); // This insert precedes the deliberately failing route insert.
        jdbc.execute("ALTER TABLE mes_pro_route ADD CONSTRAINT reject_target_route CHECK (tenant_id=1)");
        var failure = assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> tx.executeWithoutResult(s -> importSnapshot(graph, snapshot())));
        assertTrue(failure.getMessage().contains("reject_target_route"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=165", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=1", Integer.class));
    }

    @Test
    void missingReferenceOrUnknownSnapshotIdentityNeverWrites() {
        assertThrows(RuntimeException.class, () -> graph.include("mes_pro_route", 999L));
        ObjectNode json = snapshot(); json.put("unmappedTenantId", 1L);
        assertThrows(RuntimeException.class, () -> new MesFixedTestOrderSnapshotMapper(graph, jdbc, 165L).collect(json));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route WHERE tenant_id=165", Integer.class));
    }

    @Test
    void historicalSnapshotIdentityMapsOnlyToTheExplicitCurrentAlias() {
        var current = graph.include("mes_pro_route_schedule_config", 70L);
        graph.alias("mes_pro_route_schedule_config", 69L, current);
        ObjectNode json = snapshot(); ((ObjectNode) json.path("configSnapshots").path("scheduleConfigs").path("50")).put("id", 69L);
        tx.executeWithoutResult(s -> importSnapshot(graph, json));
        assertEquals(graph.id("mes_pro_route_schedule_config", 70L), graph.id("mes_pro_route_schedule_config", 69L));
    }

    @Test
    void missingFormalPermissionScopeIsExplicitAndHasNoReplacement() {
        ObjectNode json = snapshot(); json.put("permissionScopeId", 9908090475L);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> new MesFixedTestOrderSnapshotMapper(graph, jdbc, 165L).collect(json));
        assertTrue(ex.getMessage().contains("9908090475"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_permission_scope", Integer.class));
    }

    @Test
    void fixedOrderRepairsKnownMissingPermissionWithSharedUserAndReusesIt() {
        fullReferenceWithMissingFormPermission(9908090475L);
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            Long scope = jdbc.queryForObject("SELECT id FROM mes_pro_edhr_permission_scope WHERE tenant_id=165", Long.class);
            assertNotEquals(9908090475L, scope);
            assertEquals(scope, jdbc.queryForObject("SELECT permission_scope_id FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=165", Long.class));
            JsonNode copied = graph.parse(jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE tenant_id=165", String.class));
            JsonNode report = copied.path("configSnapshots").path("batchUseConfigs").get(0).path("batchRecordReports").get(0);
            assertEquals(scope, report.path("permissionScopeId").longValue());
            assertEquals(report.path("slotConfigSnapshotHash").asText(), jdbc.queryForObject("SELECT slot_config_snapshot_hash FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=165", String.class));
            assertEquals(copied.path("configSnapshots").path("batchUseConfigs").get(0).path("routeProcessId").longValue() + "|" + report.path("batchRecordReportId").asText(), jdbc.queryForObject("SELECT object_id FROM mes_pro_edhr_permission_scope WHERE tenant_id=165", String.class));
            assertEquals(Set.of("VIEW", "FILL"), new HashSet<>(jdbc.queryForList("SELECT ability FROM mes_pro_edhr_permission_rule WHERE tenant_id=165", String.class)));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_permission_rule WHERE tenant_id=165 AND subject_type='USER' AND subject_id=900 AND decision='ALLOW' AND status='ENABLED'", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_permission_scope WHERE tenant_id=1", Integer.class));
            assertEquals(9908090475L, jdbc.queryForObject("SELECT permission_scope_id FROM mes_pro_route_flow_process_batch_record WHERE tenant_id=1", Long.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM system_users", Integer.class));
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            assertEquals(scope, jdbc.queryForObject("SELECT id FROM mes_pro_edhr_permission_scope WHERE tenant_id=165", Long.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_permission_rule WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void fixedOrderDoesNotRepairUnknownMissingScope() {
        fullReferenceWithMissingFormPermission(9908090490L);
        TenantContextHolder.setTenantId(165L);
        try {
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer).initialize(77L)));
            assertTrue(failure.getMessage().contains("9908090490"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_permission_scope WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void fixedOrderPreservesOrphanPermissionRulesAndRollsBack() {
        fullReferenceWithMissingFormPermission(9908090475L);
        jdbc.update("INSERT INTO mes_pro_edhr_permission_rule(id,tenant_id,deleted,scope_id,subject_type,subject_id,ability,decision,priority,status,version) VALUES(999,1,0,9908090475,'USER',100,'SIGN','ALLOW',10,'ENABLED',1)");
        TenantContextHolder.setTenantId(165L);
        try {
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer).initialize(77L)));
            assertTrue(failure.getMessage().contains("孤立规则"));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_permission_rule WHERE tenant_id=1", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void fixedOrderPreservesChangedTargetPermissionInsteadOfExpandingOrRevokingIt() {
        fullReferenceWithMissingFormPermission(9908090475L);
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            jdbc.update("UPDATE mes_pro_edhr_permission_rule SET ability='SIGN' WHERE tenant_id=165 AND ability='VIEW'");
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.initialize(88L)));
            assertTrue(failure.getMessage().contains("保留原规则"));
            assertEquals(Set.of("SIGN", "FILL"), new HashSet<>(jdbc.queryForList("SELECT ability FROM mes_pro_edhr_permission_rule WHERE tenant_id=165", String.class)));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void validFormalFormPolicyMapsOnlyItsExplicitExistingPrincipalsAndAbilities() {
        for (String column : List.of("scope_name VARCHAR(128)", "parent_scope_id BIGINT", "version INT", "creator VARCHAR(80)", "updater VARCHAR(80)"))
            jdbc.execute("ALTER TABLE mes_pro_edhr_permission_scope ADD " + column);
        jdbc.execute("CREATE TABLE jimu_report_category (id VARCHAR(64) PRIMARY KEY,tenant_id BIGINT,del_flag INT,name VARCHAR(100),parent_id VARCHAR(64),iz_leaf INT,source_type VARCHAR(32),sort_no INT,create_by VARCHAR(80),update_by VARCHAR(80),create_time TIMESTAMP,update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE jimu_report (id VARCHAR(64) PRIMARY KEY,tenant_id BIGINT,del_flag INT,code VARCHAR(80),name VARCHAR(80),note VARCHAR(255),status VARCHAR(32),type VARCHAR(64),json_str VARCHAR(1000),template INT,css_str VARCHAR(1000),submit_form VARCHAR(1000),is_multi_sheet INT,create_by VARCHAR(80),update_by VARCHAR(80),create_time TIMESTAMP,update_time TIMESTAMP)");
        createTable("mes_pro_edhr_permission_rule");
        jdbc.update("INSERT INTO system_users VALUES(100,1,'aidongxiang','Source name',0,0),(900,165,'aidongxiang','Target name',0,0)");
        jdbc.update("INSERT INTO jimu_report_category(id,tenant_id,del_flag,name,parent_id) VALUES('category',1,0,'Forms','0')");
        jdbc.update("INSERT INTO jimu_report(id,tenant_id,del_flag,code,name,type,json_str) VALUES('report',1,0,'TEMPLATE','Form','category','{}')");
        createTable("mes_pro_batch_record_definition"); createTable("mes_pro_batch_record_version"); createTable("mes_pro_batch_record_report");
        jdbc.execute("ALTER TABLE mes_pro_batch_record_report ADD last_import_time TIMESTAMP NOT NULL");
        jdbc.update("INSERT INTO mes_pro_batch_record_definition(id,tenant_id,deleted,batch_record_name,route_key,current_version_id) VALUES(2000,1,0,'Fixed batch record','B',2001)");
        jdbc.update("INSERT INTO mes_pro_batch_record_version(id,tenant_id,deleted,definition_id,version_no,route_id) VALUES(2001,1,0,2000,'V1',20)");
        jdbc.update("INSERT INTO mes_pro_batch_record_report(id,tenant_id,deleted,batch_record_definition_id,batch_record_version_id,report_id,source_table_index,last_import_time) VALUES(2002,1,0,2000,2001,'report',1,TIMESTAMP '2000-01-01 00:00:00')");
        graph.include("mes_pro_batch_record_report", 2002L);
        jdbc.update("INSERT INTO mes_pro_edhr_permission_scope(id,tenant_id,deleted,scope_name,object_type,object_id,status,version) VALUES(1000,1,0,'Source scope','ROUTE_PROCESS_BATCH_RECORD','50|report','ENABLED',1)");
        jdbc.update("INSERT INTO mes_pro_edhr_permission_rule(id,tenant_id,deleted,scope_id,subject_type,subject_id,ability,decision,priority,status,version) VALUES(1001,1,0,1000,'USER',100,'FILL','ALLOW','100','ENABLED','1')");
        ObjectNode json = (ObjectNode) graph.parse("{\"permissionScopeId\":1000}");
        var mapper = new MesFixedTestOrderSnapshotMapper(graph, jdbc, 165L);
        tx.executeWithoutResult(s -> { mapper.collect(json); mapper.prepareScopes(); graph.resolveAll(); mapper.remap(json); graph.insertAll(); });
        assertNotEquals(1000L, json.path("permissionScopeId").asLong());
        assertEquals(900L, jdbc.queryForObject("SELECT subject_id FROM mes_pro_edhr_permission_rule WHERE tenant_id=165", Long.class));
        assertEquals("FILL", jdbc.queryForObject("SELECT ability FROM mes_pro_edhr_permission_rule WHERE tenant_id=165", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_permission_rule WHERE tenant_id=165", Integer.class));
        assertEquals(graph.id("mes_pro_route_process", 50L) + "|" + graph.id("jimu_report", "report"), jdbc.queryForObject("SELECT object_id FROM mes_pro_edhr_permission_scope WHERE tenant_id=165", String.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM system_users", Integer.class));
        assertEquals(graph.id("mes_pro_batch_record_version", 2001L), jdbc.queryForObject("SELECT current_version_id FROM mes_pro_batch_record_definition WHERE tenant_id=165", Long.class));
        assertTrue(jdbc.queryForObject("SELECT last_import_time FROM mes_pro_batch_record_report WHERE tenant_id=165", java.sql.Timestamp.class).after(java.sql.Timestamp.valueOf("2000-01-01 00:00:00")));
    }

    @Test
    void approvedSharedLeaderUsesTargetIdentityWithoutCreatingUsersOrRoles() {
        jdbc.update("INSERT INTO system_users VALUES(100,1,'aidongxiang','Source name',0,0),(900,165,'aidongxiang','Target name',0,0)");
        ObjectNode json = snapshot();
        ObjectNode configs = (ObjectNode) json.get("configSnapshots");
        configs.putArray("routeStartProductionLeaders").addObject().put("productionLineId", 20L).putArray("candidateSourceIds").add(1L);
        var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
        ReflectionTestUtils.invokeMethod(initializer, "replaceStartLeaders", configs, 165L);
        var mapper = new MesFixedTestOrderSnapshotMapper(graph, jdbc, 165L);
        mapper.collect(json); graph.resolveAll(); mapper.remap(json);
        JsonNode leader = configs.path("routeStartProductionLeaders").get(0);
        assertEquals(900L, leader.path("candidateSourceIds").get(0).asLong());
        assertEquals("Target name", leader.path("candidateSourceNames").get(0).asText());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM system_users", Integer.class));
    }

    @Test
    void missingSharedLeaderOrTenantFailsInsteadOfCopyingPrincipals() {
        var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(initializer, "replaceStartLeaders", snapshot(), 165L));
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> initializer.initialize(77L));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM system_users", Integer.class));
    }

    @Test
    void initializerCreatesMissingOrderThenReusesTheSameDefinitions() {
        fullReference();
        var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            Long first = jdbc.queryForObject("SELECT id FROM mes_pro_work_order WHERE tenant_id=165", Long.class);
            assertNotEquals(300L, first);
            assertEquals("0.000000", jdbc.queryForObject("SELECT quantity_produced FROM mes_pro_work_order WHERE tenant_id=165", java.math.BigDecimal.class).toPlainString());
            assertNotEquals(10L, jdbc.queryForObject("SELECT product_id FROM mes_pro_work_order WHERE tenant_id=165", Long.class));
            assertNotEquals(300L, jdbc.queryForObject("SELECT work_order_id FROM mes_wm_product_issue WHERE tenant_id=165", Long.class));
            assertEquals(first, jdbc.queryForObject("SELECT work_order_id FROM mes_wm_batch WHERE tenant_id=165", Long.class));
            assertEquals("4", jdbc.queryForObject("SELECT status FROM mes_wm_product_issue WHERE tenant_id=165", String.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_replenishment_list WHERE tenant_id=165", Integer.class));
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            assertEquals(first, jdbc.queryForObject("SELECT id FROM mes_pro_work_order WHERE tenant_id=165", Long.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165", Integer.class));
            assertEquals("HEADER-FID", jdbc.queryForObject("SELECT source_fid FROM erp_kingdee_production_pick_list WHERE tenant_id=165", String.class));
            assertEquals("LINE-FID", jdbc.queryForObject("SELECT source_fid FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165", String.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM system_users", Integer.class));
            assertEquals("7.000000", jdbc.queryForObject("SELECT quantity_produced FROM mes_pro_work_order WHERE tenant_id=1", java.math.BigDecimal.class).toPlainString());
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerUsesActiveCommonQaIdentityAndPreservesDisabledBindingHistory() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            Long product = jdbc.queryForObject("SELECT id FROM mdm_product WHERE tenant_id=165", Long.class);
            jdbc.execute("CREATE UNIQUE INDEX common_qa_active_identity ON mes_qa_common_regulation_product_binding(tenant_id,active_binding_key)");
            addCommonQaDefinition(1L, 150L, 151L, 131L, "SOURCE-COMMON");
            jdbc.update("INSERT INTO mes_qa_common_regulation_product_binding(id,tenant_id,deleted,product_id,dcc_project_code_id,regulation_id,regulation_version_id,scope_code,binding_status,version) VALUES(500,1,0,14,131,150,151,'COMMON_PACKAGING','ENABLED',1)");
            jdbc.update("INSERT INTO mes_qa_common_regulation_product_binding(id,tenant_id,deleted,product_id,scope_code,binding_status) VALUES(501,165,0,?,'COMMON_PACKAGING','DISABLED'),(502,165,0,?,'COMMON_PACKAGING','DISABLED')", product, product);
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            Long active = jdbc.queryForObject("SELECT id FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165 AND binding_status='ENABLED'", Long.class);
            assertNotEquals(500L, active);
            assertEquals(product.toString(), jdbc.queryForObject("SELECT active_binding_key FROM mes_qa_common_regulation_product_binding WHERE id=?", String.class, active));
            tx.executeWithoutResult(s -> initializer.initialize(99L));
            assertEquals(active, jdbc.queryForObject("SELECT id FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165 AND binding_status='ENABLED'", Long.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_common_regulation_product_binding WHERE id IN (501,502) AND binding_status='DISABLED' AND active_binding_key IS NULL", Integer.class));
            assertEquals("14", jdbc.queryForObject("SELECT active_binding_key FROM mes_qa_common_regulation_product_binding WHERE id=500 AND tenant_id=1", String.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerReusesCraftByProcessAndFormalParametersAcrossDifferentCatalogCodes() {
        fullReferenceWithCraft();
        jdbc.update("INSERT INTO mes_pro_process(id,tenant_id,deleted,code,name) VALUES(940,165,0,'PROCESS','Fixed process')");
        // Field order and omitted optional nulls do not change the normalized execution schema.
        jdbc.update("INSERT INTO mes_pro_craft(id,tenant_id,deleted,code,name,process_id,parameters_json,fingerprint) VALUES(950,165,0,'LOCAL-CRAFT','Keep local name',940,?,'stale-cache')", craftParameters());
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            JsonNode copied = graph.parse(jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE tenant_id=165", String.class));
            JsonNode config = copied.path("configSnapshots").path("productionProcessConfigs").get(0);
            assertEquals(950L, config.path("craftId").longValue());
            assertEquals("LOCAL-CRAFT", config.path("craftCode").asText());
            assertEquals("Keep local name", jdbc.queryForObject("SELECT name FROM mes_pro_craft WHERE id=950", String.class));
            assertEquals("stale-cache", jdbc.queryForObject("SELECT fingerprint FROM mes_pro_craft WHERE id=950", String.class));
            assertEquals(config.path("craftFingerprint").asText(), cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.fingerprint(940L, com.alibaba.fastjson.JSON.parseArray(craftParameters())));
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_craft WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerDoesNotReuseSameCodeCraftWithDifferentProcessOrParameterLimits() {
        fullReferenceWithCraft();
        jdbc.update("INSERT INTO mes_pro_process(id,tenant_id,deleted,code,name) VALUES(940,165,0,'PROCESS','Fixed process'),(941,165,0,'OTHER','Other process')");
        jdbc.update("INSERT INTO mes_pro_craft(id,tenant_id,deleted,code,name,process_id,parameters_json) VALUES(950,165,0,'GY-0003','Other process craft',941,?),(951,165,0,'LOCAL-LIMIT','Other limit',940,?)", craftParameters(), craftParameters().replace("\"lowerLimit\":20", "\"lowerLimit\":19"));
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            Long created = jdbc.queryForObject("SELECT id FROM mes_pro_craft WHERE tenant_id=165 AND code LIKE 'FT_%'", Long.class);
            assertNotEquals(950L, created); assertNotEquals(951L, created);
            assertEquals(940L, jdbc.queryForObject("SELECT process_id FROM mes_pro_craft WHERE id=?", Long.class, created));
            JsonNode config = graph.parse(jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE tenant_id=165", String.class)).path("configSnapshots").path("productionProcessConfigs").get(0);
            assertEquals(created, config.path("craftId").longValue());
            assertEquals(jdbc.queryForObject("SELECT code FROM mes_pro_craft WHERE id=?", String.class, created), config.path("craftCode").asText());
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            assertEquals(created, jdbc.queryForObject("SELECT id FROM mes_pro_craft WHERE tenant_id=165 AND code LIKE 'FT_%'", Long.class));
            assertEquals("GY-0003", jdbc.queryForObject("SELECT code FROM mes_pro_craft WHERE id=950", String.class));
            assertEquals("Other limit", jdbc.queryForObject("SELECT name FROM mes_pro_craft WHERE id=951", String.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerRejectsAmbiguousEquivalentCraftsWithoutWriting() {
        fullReferenceWithCraft();
        jdbc.update("INSERT INTO mes_pro_process(id,tenant_id,deleted,code,name) VALUES(940,165,0,'PROCESS','Fixed process')");
        jdbc.update("INSERT INTO mes_pro_craft(id,tenant_id,deleted,code,name,process_id,parameters_json) VALUES(950,165,0,'LOCAL-1','One',940,?),(951,165,0,'LOCAL-2','Two',940,?)", craftParameters(), craftParameters());
        TenantContextHolder.setTenantId(165L);
        try {
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer).initialize(77L)));
            assertTrue(failure.getMessage().contains("多个参数完全相同"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_craft WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerUsesCurrentPublishedProductQaByDccInsteadOfCatalogCodeOrSourceVersion() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            Long regulation = jdbc.queryForObject("SELECT id FROM mes_qa_inspection_regulation WHERE tenant_id=165", Long.class);
            Long version = jdbc.queryForObject("SELECT current_version_id FROM mes_qa_inspection_regulation WHERE id=?", Long.class, regulation);
            jdbc.update("UPDATE mes_qa_inspection_regulation SET regulation_code='LOCAL-PRODUCT-QA' WHERE id=?", regulation);
            jdbc.update("UPDATE mes_qa_inspection_regulation_version SET version_no='LOCAL-V9' WHERE id=?", version);
            ObjectNode creationSnapshot = (ObjectNode) graph.parse(jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=?", String.class, version));
            creationSnapshot.remove("regulationId"); // A valid first-publication request has no regulation ID yet.
            jdbc.update("UPDATE mes_qa_inspection_regulation_version SET snapshot_json=? WHERE id=?", creationSnapshot.toString(), version);
            String before = jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=?", String.class, version);
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            assertEquals(regulation, jdbc.queryForObject("SELECT id FROM mes_qa_inspection_regulation WHERE tenant_id=165", Long.class));
            assertEquals(version, jdbc.queryForObject("SELECT current_version_id FROM mes_qa_inspection_regulation WHERE id=?", Long.class, regulation));
            assertEquals("LOCAL-PRODUCT-QA", jdbc.queryForObject("SELECT regulation_code FROM mes_qa_inspection_regulation WHERE id=?", String.class, regulation));
            assertEquals("LOCAL-V9", jdbc.queryForObject("SELECT version_no FROM mes_qa_inspection_regulation_version WHERE id=?", String.class, version));
            assertEquals(before, jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=?", String.class, version));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerRejectsProductQaSnapshotThatExplicitlyReferencesAnotherRegulation() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            Long version = jdbc.queryForObject("SELECT id FROM mes_qa_inspection_regulation_version WHERE tenant_id=165", Long.class);
            ObjectNode snapshot = (ObjectNode) graph.parse(jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=?", String.class, version));
            snapshot.put("regulationId", 999L);
            jdbc.update("UPDATE mes_qa_inspection_regulation_version SET snapshot_json=? WHERE id=?", snapshot.toString(), version);
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.initialize(88L)));
            assertTrue(failure.getMessage().contains("快照与规程身份不一致"));
            assertEquals(snapshot.toString(), jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=?", String.class, version));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerRejectsDraftCurrentProductQaWithoutReplacingTheExistingRegulation() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            Long regulation = jdbc.queryForObject("SELECT id FROM mes_qa_inspection_regulation WHERE tenant_id=165", Long.class);
            jdbc.update("UPDATE mes_qa_inspection_regulation_version SET lifecycle_status='DRAFT' WHERE tenant_id=165");
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.initialize(88L)));
            assertTrue(failure.getMessage().contains("发布状态"));
            assertEquals(regulation, jdbc.queryForObject("SELECT id FROM mes_qa_inspection_regulation WHERE tenant_id=165", Long.class));
            assertEquals("DRAFT", jdbc.queryForObject("SELECT lifecycle_status FROM mes_qa_inspection_regulation_version WHERE tenant_id=165", String.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerUsesTheProductsValidLocalCommonQaSetWithoutComparingSourceCodesOrVersions() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            addExistingCommonQaBinding();
            String before = jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=701", String.class);
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            tx.executeWithoutResult(s -> initializer.initialize(99L));
            assertEquals(810L, jdbc.queryForObject("SELECT id FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165 AND binding_status='ENABLED'", Long.class));
            assertEquals(701L, jdbc.queryForObject("SELECT regulation_version_id FROM mes_qa_common_regulation_product_binding WHERE id=810", Long.class));
            assertEquals(before, jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=701", String.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_inspection_regulation WHERE tenant_id=165 AND regulation_code='SOURCE-COMMON'", Integer.class));
            assertEquals(151L, jdbc.queryForObject("SELECT regulation_version_id FROM mes_qa_common_regulation_product_binding WHERE id=500 AND tenant_id=1", Long.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerRejectsInvalidLocalCommonQaInsteadOfSilentlyReplacingItsBinding() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            addExistingCommonQaBinding();
            jdbc.update("UPDATE mes_qa_inspection_regulation_version SET lifecycle_status='DRAFT' WHERE id=701");
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.initialize(88L)));
            assertTrue(failure.getMessage().contains("发布状态"));
            assertEquals(701L, jdbc.queryForObject("SELECT regulation_version_id FROM mes_qa_common_regulation_product_binding WHERE id=810", Long.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerRejectsLocalCommonQaSetMissingARequiredBusinessRole() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            addExistingCommonQaBinding();
            jdbc.update("UPDATE mes_qa_common_regulation_set_version_member SET member_role='Other scope' WHERE id=803");
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.initialize(88L)));
            assertTrue(failure.getMessage().contains("缺少所需适用环节"));
            assertEquals("Other scope", jdbc.queryForObject("SELECT member_role FROM mes_qa_common_regulation_set_version_member WHERE id=803", String.class));
            assertEquals(701L, jdbc.queryForObject("SELECT regulation_version_id FROM mes_qa_common_regulation_product_binding WHERE id=810", Long.class));
        } finally { TenantContextHolder.clear(); }
    }

    private String craftParameters() {
        return "[{\"parameterCode\":\"TEMP\",\"parameterName\":\"温度\",\"valueType\":\"INTEGER\",\"standardText\":\"20-30℃\",\"unit\":\"℃\",\"lowerLimit\":20,\"upperLimit\":30,\"defaultValue\":25}]";
    }

    private void fullReferenceWithCraft() {
        fullReference();
        jdbc.update("INSERT INTO mes_dv_machinery(id,tenant_id,deleted,code,name) VALUES(260,1,0,'DEVICE','Execution device')");
        jdbc.update("INSERT INTO mes_pro_craft(id,tenant_id,deleted,code,name,process_id,parameters_json) VALUES(500,1,0,'GY-0003','Reference craft',40,?)", craftParameters());
        ObjectNode json = (ObjectNode) graph.parse(jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE id=60", String.class));
        ObjectNode config = ((ObjectNode) json.path("configSnapshots")).putArray("productionProcessConfigs").addObject();
        config.put("routeProcessId", 50).put("processId", 40).put("craftId", 500).put("craftCode", "GY-0003");
        config.putArray("craftDeviceIds").add(260);
        config.putArray("deviceSelectionGroups").addObject().putArray("deviceIds").add(260);
        config.putArray("parameterRules").add(((ObjectNode) graph.parse(craftParameters()).get(0)).put("deviceId", 260));
        String canonical = canonicalizer.canonicalize(json.toString());
        jdbc.update("UPDATE mes_pro_route_version SET route_snapshot_json=?,route_snapshot_sha256=? WHERE id=60", canonical, canonicalizer.sha256(canonical));
    }

    private void addCommonQaDefinition(long tenant, long regulation, long version, long dcc, String code) {
        jdbc.update("INSERT INTO mes_qa_inspection_regulation(id,tenant_id,deleted,dcc_project_code_id,owner_module,regulation_code,lifecycle_status,current_version_id) VALUES(?,?,0,?,'MES_QA_COMMON',?,'PUBLISHED',?)", regulation, tenant, dcc, code, version);
        jdbc.update("INSERT INTO mes_qa_inspection_regulation_version(id,tenant_id,deleted,regulation_id,version_no,lifecycle_status,snapshot_json) VALUES(?,?,0,?,'LOCAL-VERSION','PUBLISHED',?)", version, tenant, regulation, "{\"regulationId\":" + regulation + ",\"dccProjectCodeId\":" + dcc + ",\"ownerModule\":\"MES_QA_COMMON\",\"processes\":[]}");
    }

    private void addExistingCommonQaBinding() {
        Long product = jdbc.queryForObject("SELECT id FROM mdm_product WHERE tenant_id=165", Long.class);
        Long dcc = jdbc.queryForObject("SELECT id FROM dcc_project_code WHERE tenant_id=165", Long.class);
        addCommonQaDefinition(1, 150, 151, 131, "SOURCE-COMMON");
        addCommonQaDefinition(165, 700, 701, dcc, "LOCAL-COMMON");
        jdbc.update("INSERT INTO mes_qa_common_regulation_set(id,tenant_id,deleted,set_code,set_status,current_version_id) VALUES(600,1,0,'SOURCE-SET','ENABLED',601),(800,165,0,'LOCAL-SET','ENABLED',801)");
        jdbc.update("INSERT INTO mes_qa_common_regulation_set_version(id,tenant_id,deleted,set_id,version_no,lifecycle_status) VALUES(601,1,0,600,'SOURCE-V1','PUBLISHED'),(801,165,0,800,'LOCAL-V9','PUBLISHED')");
        jdbc.update("INSERT INTO mes_qa_common_regulation_set_version_member(id,tenant_id,deleted,set_version_id,regulation_id,regulation_version_id,sort,member_role) VALUES(602,1,0,601,150,151,10,'初包装'),(603,1,0,601,150,151,20,'大中包装'),(802,165,0,801,700,701,10,'初包装'),(803,165,0,801,700,701,20,'大中包装')");
        jdbc.update("INSERT INTO mes_qa_common_regulation_product_binding(id,tenant_id,deleted,product_id,dcc_project_code_id,common_regulation_set_id,common_regulation_set_version_id,regulation_id,regulation_version_id,scope_code,binding_status,version) VALUES(500,1,0,14,131,600,601,150,151,'COMMON_PACKAGING','ENABLED',1),(810,165,0,?,?,800,801,700,701,'COMMON_PACKAGING','ENABLED',9)", product, dcc);
    }

    @Test
    void initializerFailureAfterDependencyInsertRollsBackOrderAndDependencies() {
        fullReference();
        jdbc.execute("ALTER TABLE mes_wm_product_issue ADD CONSTRAINT reject_target_issue CHECK (tenant_id=1)");
        TenantContextHolder.setTenantId(165L);
        try {
            var failure = assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer).initialize(77L)));
            assertTrue(failure.getMessage().contains("reject_target_issue"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_inspection_regulation WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=165", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=1", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void materialUnitUsesItsMeaningAndConversionInsteadOfCoincidingUnitCodes() {
        fullReference();
        jdbc.update("INSERT INTO mes_md_unit_measure(id,tenant_id,deleted,code,name,primary_flag,change_rate,status) VALUES(600,1,0,'kg','千克','1','1',0),(700,165,0,'千克','千克','1','1',0),(701,165,0,'kg','kg','1','1',0)");
        jdbc.update("UPDATE mes_md_item SET unit_measure_id=600 WHERE id=11");
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,name,unit_measure_id) VALUES(911,165,0,'MATERIAL','Local material',700)");
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            assertEquals(911L, jdbc.queryForObject("SELECT item_id FROM mes_wm_product_issue_line WHERE tenant_id=165", Long.class));
            assertEquals(700L, jdbc.queryForObject("SELECT unit_measure_id FROM mes_md_item WHERE id=911", Long.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_unit_measure WHERE tenant_id=165", Integer.class));
            assertEquals("千克", jdbc.queryForObject("SELECT code FROM mes_md_unit_measure WHERE id=700", String.class));
            assertEquals("kg", jdbc.queryForObject("SELECT name FROM mes_md_unit_measure WHERE id=701", String.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void sameUnitNameWithDifferentConversionCannotValidateAnExistingMaterial() {
        fullReference();
        jdbc.update("INSERT INTO mes_md_unit_measure(id,tenant_id,deleted,code,name,primary_flag,change_rate,status) VALUES(600,1,0,'kg','千克','1','1',0),(700,165,0,'kg','千克','1','1000',0)");
        jdbc.update("UPDATE mes_md_item SET unit_measure_id=600 WHERE id=11");
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,name,unit_measure_id) VALUES(911,165,0,'MATERIAL','Material',700)");
        TenantContextHolder.setTenantId(165L);
        try {
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer).initialize(77L)));
            assertTrue(failure.getMessage().contains("mes_md_item/unit_measure_id"));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_unit_measure WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void existingMaterialKeepsItsLocalLabelWhenSpecificationAndReferencesMatch() {
        fullReference();
        jdbc.update("UPDATE mes_md_item SET specification='Box 20x30' WHERE id=11");
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,name,specification) VALUES(911,165,0,'MATERIAL','Local material label#','Box 20x30')");
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer).initialize(77L));
            assertEquals(911L, jdbc.queryForObject("SELECT item_id FROM mes_wm_product_issue_line WHERE tenant_id=165", Long.class));
            assertEquals("Local material label#", jdbc.queryForObject("SELECT name FROM mes_md_item WHERE id=911", String.class));
            assertEquals("Material", jdbc.queryForObject("SELECT name FROM mes_md_item WHERE id=11 AND tenant_id=1", String.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void erpSyncedMaterialRetainsTheTargetSpecificationAfterVerifyingBothErpMasters() {
        fullReference();
        addErpSyncedMaterial();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            assertEquals(911L, jdbc.queryForObject("SELECT item_id FROM mes_wm_product_issue_line WHERE tenant_id=165", Long.class));
            assertEquals("Current ERP specification", jdbc.queryForObject("SELECT specification FROM mes_md_item WHERE id=911", String.class));
            assertEquals("Reference ERP specification", jdbc.queryForObject("SELECT specification FROM mes_md_item WHERE id=11 AND tenant_id=1", String.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM erp_product", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void unverifiedErpSpecificationDifferenceStillStopsBeforeAnyWrite() {
        fullReference();
        addErpSyncedMaterial();
        jdbc.update("UPDATE erp_product SET standard='Another specification' WHERE tenant_id=165");
        TenantContextHolder.setTenantId(165L);
        try {
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer).initialize(77L)));
            assertTrue(failure.getMessage().contains("无法由两边有效ERP主档核实"));
            assertEquals("Current ERP specification", jdbc.queryForObject("SELECT specification FROM mes_md_item WHERE id=911", String.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    private void addErpSyncedMaterial() {
        jdbc.update("INSERT INTO mes_md_item_type(id,tenant_id,deleted,code,name,parent_id) VALUES(2,1,0,'KINGDEE_PRODUCT','Kingdee Imported Product',0),(10,165,0,'KINGDEE_PRODUCT','Kingdee Imported Product',0)");
        jdbc.update("UPDATE mes_md_item SET specification='Reference ERP specification',item_type_id=2 WHERE id=11");
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,name,specification,item_type_id) VALUES(911,165,0,'MATERIAL','Material','Current ERP specification',10)");
        jdbc.execute("CREATE TABLE erp_product(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,bar_code VARCHAR(80),name VARCHAR(80),standard VARCHAR(200),status INT)");
        jdbc.update("INSERT INTO erp_product VALUES(1,1,0,'MATERIAL','Material','Reference ERP specification',0),(2,165,0,'MATERIAL','Material','Current ERP specification',0)");
    }

    @Test
    void sameMaterialCodeWithDifferentSpecificationCannotBeReused() {
        fullReference();
        jdbc.update("UPDATE mes_md_item SET specification='Box 20x30' WHERE id=11");
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,name,specification) VALUES(911,165,0,'MATERIAL','Material','Box 40x50')");
        TenantContextHolder.setTenantId(165L);
        try {
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer).initialize(77L)));
            assertTrue(failure.getMessage().contains("mes_md_item/specification"));
            assertEquals("Box 40x50", jdbc.queryForObject("SELECT specification FROM mes_md_item WHERE id=911", String.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerPreservesExternalErpMaterialTextWithoutInventingAnItemMaster() {
        fullReference();
        jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET material_number='/' WHERE tenant_id=1");
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer).initialize(77L));
            assertEquals("/", jdbc.queryForObject("SELECT material_number FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165", String.class));
            assertEquals(Set.of("PRODUCT", "MATERIAL"), new HashSet<>(jdbc.queryForList("SELECT code FROM mes_md_item WHERE tenant_id=165", String.class)));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item WHERE code='/'", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerMapsVerifiedLegacyDeviceCodesWithoutCopyingForeignDeletedRows() {
        fullReference();
        jdbc.update("INSERT INTO mes_dv_machinery_type(id,tenant_id,deleted,code,name) VALUES(1,1,0,'TYPE','Device type'),(11,165,0,'TYPE','Device type')");
        jdbc.update("INSERT INTO mes_dv_machinery(id,tenant_id,deleted,code,name,machinery_type_id) VALUES(980021,1,0,'AL150158','扭力扳手',1),(980024,165,1,'AL150158','扭力扳手',11),(9900021,165,0,'AL150158','扭力扳手',11)");
        jdbc.update("UPDATE mes_dv_machinery SET status=2 WHERE id=9900021");
        ObjectNode json = (ObjectNode) graph.parse(jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE id=60", String.class));
        ObjectNode process = ((ObjectNode) json.path("configSnapshots")).putArray("productionProcessConfigs").addObject();
        process.put("routeProcessId", 50).put("processId", 40);
        process.putArray("parameterRules");
        process.putArray("deviceSelectionGroups").addObject().putArray("deviceIds").add(980024L);
        String canonical = canonicalizer.canonicalize(json.toString());
        jdbc.update("UPDATE mes_pro_route_version SET route_snapshot_json=?,route_snapshot_sha256=? WHERE id=60", canonical, canonicalizer.sha256(canonical));
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            JsonNode copied = graph.parse(jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE tenant_id=165", String.class));
            long device = copied.path("configSnapshots").path("productionProcessConfigs").get(0).path("deviceSelectionGroups").get(0).path("deviceIds").get(0).longValue();
            assertNotEquals(980024L, device); assertNotEquals(980021L, device);
            assertEquals(device, jdbc.queryForObject("SELECT id FROM mes_dv_machinery WHERE tenant_id=165 AND deleted=0", Long.class));
            assertEquals(2, jdbc.queryForObject("SELECT status FROM mes_dv_machinery WHERE id=9900021", Integer.class));
            assertEquals("AL150158", jdbc.queryForObject("SELECT code FROM mes_dv_machinery WHERE tenant_id=165 AND deleted=0", String.class));
            assertEquals(canonical, jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE tenant_id=1", String.class));
            assertEquals(1, jdbc.queryForObject("SELECT deleted FROM mes_dv_machinery WHERE id=980024", Integer.class));
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_dv_machinery WHERE tenant_id=165 AND deleted=0", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void concurrentFirstInitializationSerializesOnCurrentTenantAndReusesTheWinner() throws Exception {
        fullReference();
        var start = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
            for (long actor : List.of(77L, 88L)) futures.add(executor.submit(() -> {
                TenantContextHolder.setTenantId(165L);
                try {
                    start.await();
                    tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer).initialize(actor));
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                finally { TenantContextHolder.clear(); }
            }));
            start.countDown();
            for (var future : futures) future.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_version WHERE tenant_id=165", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_product_issue WHERE tenant_id=165", Integer.class));
        } finally { executor.shutdownNow(); }
    }

    @Test
    void zeroHistoricalOwnerRejectsOnlyCurrentTenantLiveRuntimeAndNeverDeletesIt() {
        for (String table : List.of("mes_pro_process_pool_event", "mes_pro_edhr_batch_execution", "mes_pro_feedback", "mes_pro_process_pool", "mes_pqc_inspection_task", "mes_pro_process_pool_order_process_completion", "mes_pro_process_pool_work_order_abnormal"))
            jdbc.execute("CREATE TABLE " + table + " (id BIGINT,tenant_id BIGINT,work_order_id BIGINT,deleted INT)");
        var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer);
        jdbc.update("INSERT INTO mes_pro_process_pool_event VALUES(1,166,300,0),(2,165,301,0),(3,165,300,1)");
        assertDoesNotThrow(() -> initializer.requireNoOrphanRuntime(165L, 300L));
        jdbc.update("INSERT INTO mes_pro_process_pool_event VALUES(4,165,300,0)");
        assertThrows(RuntimeException.class, () -> initializer.requireNoOrphanRuntime(165L, 300L));
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_event", Integer.class));
    }

    @SuppressWarnings("unchecked")
    private void fullReference() {
        Map<String, Spec> specs = (Map<String, Spec>) ReflectionTestUtils.getField(graph, "specs");
        for (String table : specs.keySet()) {
            if (table.startsWith("jimu_")) continue; // This minimal formal route has no bound batch-record forms.
            if (jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name=?", Integer.class, table) == 0) createTable(table);
        }
        jdbc.execute("ALTER TABLE mes_wm_product_issue ALTER COLUMN status INTEGER");
        jdbc.execute("CREATE TABLE system_tenant (id BIGINT PRIMARY KEY,deleted INT)");
        jdbc.update("INSERT INTO system_tenant VALUES(165,0)");
        jdbc.update("INSERT INTO system_users VALUES(100,1,'aidongxiang','Source name',0,0),(900,165,'aidongxiang','Target name',0,0)");
        jdbc.update("INSERT INTO mdm_product(id,tenant_id,deleted,product_code) VALUES(14,1,0,'MASTER')");
        jdbc.update("UPDATE mes_md_item SET product_master_id=14 WHERE id=10");
        jdbc.update("INSERT INTO mes_pro_work_order(id,tenant_id,deleted,code,product_id,quantity,quantity_produced) VALUES(300,1,0,?,10,10,7)", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
        ObjectNode formal = snapshot();
        ((ObjectNode) formal.get("configSnapshots")).putArray("routeStartProductionLeaders").addObject().put("productionLineId", 20L).put("candidateSourceType", "USERS").putArray("candidateSourceIds").add(1L);
        String json = canonicalizer.canonicalize(formal.toString());
        jdbc.update("UPDATE mes_pro_route_version SET route_snapshot_json=?,route_snapshot_sha256=? WHERE id=60", json, canonicalizer.sha256(json));
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,deleted,product_master_id,project_code) VALUES(131,1,0,14,'DCC-FIXED')");
        jdbc.update("INSERT INTO mes_pro_route_dcc_project_binding(id,tenant_id,deleted,route_id,dcc_project_code_id) VALUES(132,1,0,20,131)");
        jdbc.update("INSERT INTO mes_qa_inspection_regulation(id,tenant_id,deleted,dcc_project_code_id,product_id,owner_module,regulation_code,lifecycle_status,current_version_id) VALUES(140,1,0,131,14,'MES_QA','QA-FIXED','PUBLISHED',141)");
        jdbc.update("INSERT INTO mes_qa_inspection_regulation_version(id,tenant_id,deleted,regulation_id,version_no,lifecycle_status,snapshot_json) VALUES(141,1,0,140,'V1','PUBLISHED','{\"regulationId\":140,\"dccProjectCodeId\":131,\"ownerModule\":\"MES_QA\",\"processes\":[]}')");
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,name) VALUES(11,1,0,'MATERIAL','Material')");
        jdbc.update("INSERT INTO mes_wm_warehouse(id,tenant_id,deleted,code) VALUES(204,1,0,'WH')");
        jdbc.update("INSERT INTO mes_wm_warehouse_location(id,tenant_id,deleted,code,warehouse_id) VALUES(205,1,0,'LOC',204)");
        jdbc.update("INSERT INTO mes_wm_warehouse_area(id,tenant_id,deleted,code,location_id) VALUES(206,1,0,'AREA',205)");
        jdbc.update("INSERT INTO mes_wm_batch(id,tenant_id,deleted,code,item_id,work_order_id) VALUES(202,1,0,'LOT',11,300)");
        jdbc.update("INSERT INTO mes_wm_material_stock(id,tenant_id,deleted,item_id,batch_id,warehouse_id,location_id,area_id) VALUES(203,1,0,11,202,204,205,206)");
        jdbc.update("INSERT INTO mes_wm_product_issue(id,tenant_id,deleted,code,work_order_id,status) VALUES(200,1,0,'ISSUE',300,4)");
        jdbc.update("INSERT INTO mes_wm_product_issue_line(id,tenant_id,deleted,issue_id,item_id,quantity,batch_id) VALUES(201,1,0,200,11,1,202)");
        jdbc.update("INSERT INTO mes_wm_product_issue_detail(id,tenant_id,deleted,issue_id,line_id,material_stock_id,item_id,quantity,batch_id,warehouse_id,location_id,area_id) VALUES(207,1,0,200,201,203,11,1,202,204,205,206)");
        jdbc.update("INSERT INTO erp_kingdee_production_pick_list(id,tenant_id,deleted,source_form_id,source_fid,document_status) VALUES(210,1,0,'PICK','HEADER-FID','C')");
        jdbc.update("INSERT INTO erp_kingdee_production_pick_list_item(id,tenant_id,deleted,production_pick_list_id,source_form_id,source_fid,source_line_key,material_number,actual_quantity,production_order_no) VALUES(211,1,0,210,'PICK','LINE-FID','LINE','MATERIAL',1,?)", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
    }

    private void fullReferenceWithMissingFormPermission(long scope) {
        fullReference();
        jdbc.execute("CREATE TABLE jimu_report_db (id VARCHAR(64) PRIMARY KEY,jimu_report_id VARCHAR(64))");
        for (String column : List.of("scope_name VARCHAR(128)", "parent_scope_id BIGINT", "version INT", "creator VARCHAR(80)", "updater VARCHAR(80)"))
            jdbc.execute("ALTER TABLE mes_pro_edhr_permission_scope ADD " + column);
        jdbc.execute("CREATE TABLE jimu_report_category (id VARCHAR(64) PRIMARY KEY,tenant_id BIGINT,del_flag INT,name VARCHAR(100),parent_id VARCHAR(64),iz_leaf INT,source_type VARCHAR(32),sort_no INT,create_by VARCHAR(80),update_by VARCHAR(80),create_time TIMESTAMP,update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE jimu_report (id VARCHAR(64) PRIMARY KEY,tenant_id BIGINT,del_flag INT,code VARCHAR(80),name VARCHAR(80),note VARCHAR(255),status VARCHAR(32),type VARCHAR(64),json_str VARCHAR(1000),template INT,css_str VARCHAR(1000),submit_form VARCHAR(1000),is_multi_sheet INT,create_by VARCHAR(80),update_by VARCHAR(80),create_time TIMESTAMP,update_time TIMESTAMP)");
        jdbc.update("INSERT INTO jimu_report_category(id,tenant_id,del_flag,name,parent_id) VALUES('category',1,0,'Forms','0')");
        jdbc.update("INSERT INTO jimu_report(id,tenant_id,del_flag,code,name,type,json_str) VALUES('report',1,0,'TEMPLATE','Form','category','{}')");
        jdbc.execute("ALTER TABLE mes_pro_batch_record_report ADD last_import_time TIMESTAMP NOT NULL");
        jdbc.update("INSERT INTO mes_pro_batch_record_definition(id,tenant_id,deleted,batch_record_name,route_key,current_version_id) VALUES(2000,1,0,'Fixed batch record','B',2001)");
        jdbc.update("INSERT INTO mes_pro_batch_record_version(id,tenant_id,deleted,definition_id,version_no,route_id,child_form_member_count) VALUES(2001,1,0,2000,'V1',20,0)");
        jdbc.update("INSERT INTO mes_pro_batch_record_report(id,tenant_id,deleted,batch_record_definition_id,batch_record_version_id,report_id,source_table_index,last_import_time) VALUES(2002,1,0,2000,2001,'report',1,TIMESTAMP '2000-01-01 00:00:00')");
        jdbc.update("INSERT INTO mes_pro_route_flow_config(id,tenant_id,deleted,route_id,use_type,enabled) VALUES(2100,1,0,20,'BATCH',1)");
        jdbc.update("INSERT INTO mes_pro_route_flow_process_config(id,tenant_id,deleted,route_flow_config_id,route_id,route_process_id,use_type,enabled) VALUES(2101,1,0,2100,20,50,'BATCH',1)");
        jdbc.update("INSERT INTO mes_pro_route_flow_process_batch_record(id,tenant_id,deleted,route_flow_process_config_id,route_id,route_process_id,use_type,batch_record_report_id,batch_record_definition_id,batch_record_version_id,form_slot_type,owner_role_key,permission_scope_id,recordbook_enabled) VALUES(2102,1,0,2101,20,50,'BATCH','report',2000,2001,'MAIN','PRODUCTION',?,1)", scope);
        ObjectNode json = (ObjectNode) graph.parse(jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE id=60", String.class));
        ObjectNode config = ((ObjectNode) json.path("configSnapshots")).putArray("batchUseConfigs").addObject();
        config.put("id", 2101).put("routeFlowConfigId", 2100).put("routeProcessId", 50).put("useType", "BATCH").put("enabled", true);
        config.putArray("batchRecordReports").addObject().put("routeBindingId", 2102).put("batchRecordReportId", "report").put("batchRecordDefinitionId", 2000).put("batchRecordVersionId", 2001).put("permissionScopeId", scope).put("formSlotType", "MAIN").put("ownerRoleKey", "PRODUCTION");
        String canonical = canonicalizer.canonicalize(json.toString());
        jdbc.update("UPDATE mes_pro_route_version SET route_snapshot_json=?,route_snapshot_sha256=? WHERE id=60", canonical, canonicalizer.sha256(canonical));
    }
}
