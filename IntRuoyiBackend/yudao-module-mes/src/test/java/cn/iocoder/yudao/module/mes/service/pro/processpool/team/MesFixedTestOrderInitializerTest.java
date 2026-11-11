package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteSnapshotCanonicalizer;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteControlledContentAdapter;
import org.mockito.ArgumentCaptor;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
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
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderReferenceGraph.*;

/** Isolated SQL tests; no connection to the application database and no production operations. */
class MesFixedTestOrderInitializerTest {
    final MesProRouteControlledContentAdapter controlledContentAdapter = mock(MesProRouteControlledContentAdapter.class);
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
    void completionSourcesReuseCategoryIdentityAndKeepItsLocalLabel() {
        localCategoryFixture();
        graph.include("mes_md_item", 10L);
        tx.executeWithoutResult(s -> graph.insertAll());
        assertEquals(902L, jdbc.queryForObject("SELECT item_type_id FROM mes_md_item WHERE tenant_id=165", Long.class));
        assertEquals("金蝶同步产品", jdbc.queryForObject("SELECT name FROM mes_md_item_type WHERE id=902", String.class));
        assertEquals("Kingdee Imported Product", jdbc.queryForObject("SELECT name FROM mes_md_item_type WHERE id=2", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item_type WHERE tenant_id=165", Integer.class));
    }

    @Test
    void differentCategoryStatusStillBlocksBeforeImportingMaterial() {
        localCategoryFixture();
        jdbc.update("UPDATE mes_md_item_type SET status=1 WHERE id=902");
        graph.include("mes_md_item", 10L);
        var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> graph.insertAll()));
        assertTrue(failure.getMessage().contains("mes_md_item_type/status"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=165", Integer.class));
    }

    @Test
    void differentCategoryParentStillBlocksBeforeImportingMaterial() {
        localCategoryFixture();
        jdbc.update("UPDATE mes_md_item_type SET parent_id=999 WHERE id=902");
        graph.include("mes_md_item", 10L);
        var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> graph.insertAll()));
        assertTrue(failure.getMessage().contains("mes_md_item_type/parent_id"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=165", Integer.class));
    }

    private void localCategoryFixture() {
        createTable("mes_md_item_type");
        jdbc.update("INSERT INTO mes_md_item_type(id,tenant_id,deleted,code,name,parent_id,item_or_product,status,sort) VALUES(2,1,0,'KINGDEE_PRODUCT','Kingdee Imported Product',0,'PRODUCT',0,0),(902,165,0,'KINGDEE_PRODUCT','金蝶同步产品',0,'PRODUCT',0,0)");
        jdbc.update("UPDATE mes_md_item SET item_type_id=2 WHERE id=10");
    }

    @Test
    void testResetRestoresOnlyMissingBindingToLocalProjectRouteAndIsIdempotent() {
        testBindingFixture();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            tx.executeWithoutResult(s -> initializer.ensureTestRouteProductBinding(77L));
            var binding = jdbc.queryForMap("SELECT * FROM mes_pro_route_product WHERE tenant_id=165 AND deleted=0");
            assertEquals(920L, binding.get("route_id"));
            assertEquals(910L, binding.get("item_id"));
            assertEquals(0, java.math.BigDecimal.ONE.compareTo(new java.math.BigDecimal(String.valueOf(binding.get("quantity")))));
            assertEquals("MINUTE", binding.get("time_unit_type"));
            assertEquals("77", binding.get("creator"));
            // No source-tenant route, MDM relation or enabled product metadata is needed for this test repair.
            jdbc.update("DELETE FROM dcc_project_code WHERE tenant_id=165");
            tx.executeWithoutResult(s -> initializer.ensureTestRouteProductBinding(88L));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_product WHERE tenant_id=165 AND deleted=0", Integer.class));
            assertEquals("77", jdbc.queryForObject("SELECT creator FROM mes_pro_route_product WHERE tenant_id=165 AND deleted=0", String.class));
            assertEquals(1, jdbc.queryForObject("SELECT status FROM mes_md_item WHERE id=910", Integer.class));
            assertEquals("V27", jdbc.queryForObject("SELECT version_no FROM mes_pro_route_version WHERE id=960", String.class));
            verifyNoInteractions(controlledContentAdapter);
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void testResetDoesNotGuessBetweenProjectRoutesOrPublishedVersions() {
        testBindingFixture();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            jdbc.update("INSERT INTO mes_pro_route(id,tenant_id,deleted,code) VALUES(921,165,0,'OTHER')");
            jdbc.update("INSERT INTO mes_pro_route_dcc_project_binding(id,tenant_id,deleted,route_id,dcc_project_code_id) VALUES(933,165,0,921,931)");
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.ensureTestRouteProductBinding(77L)));
            jdbc.update("UPDATE mes_pro_route SET deleted=1 WHERE id=921");
            jdbc.update("INSERT INTO mes_pro_route_version(id,tenant_id,deleted,route_id,version_no,active,lifecycle_status) VALUES(961,165,0,920,'V28',true,'ACTIVE')");
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.ensureTestRouteProductBinding(77L)));
            jdbc.update("UPDATE mes_pro_route_version SET active=false WHERE route_id=920");
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.ensureTestRouteProductBinding(77L)));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_product WHERE tenant_id=165 AND deleted=0", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void testResetDoesNotUseAnotherTenantsProjectAndRollsBackBindingOnLaterFailure() {
        testBindingFixture();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            jdbc.update("UPDATE dcc_project_code SET status=1 WHERE id=931");
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.ensureTestRouteProductBinding(77L)));
            jdbc.update("UPDATE dcc_project_code SET status=0 WHERE id=931");
            assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(s -> {
                initializer.ensureTestRouteProductBinding(77L);
                throw new IllegalStateException("later reset activation rejected");
            }));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_product WHERE tenant_id=165 AND deleted=0", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_product WHERE tenant_id=1 AND deleted=0", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    private void testBindingFixture() {
        fullReference();
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,status) VALUES(910,165,0,'IDI',1)");
        jdbc.update("INSERT INTO mes_pro_work_order(id,tenant_id,deleted,code,product_id) VALUES(930,165,0,?,910)", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,deleted,project_code,status) VALUES(931,165,0,'IDI',0),(941,1,0,'IDI',0)");
        jdbc.update("INSERT INTO mes_pro_route(id,tenant_id,deleted,code) VALUES(920,165,0,'REBUILT-ROUTE')");
        jdbc.update("INSERT INTO mes_pro_route_dcc_project_binding(id,tenant_id,deleted,route_id,dcc_project_code_id) VALUES(932,165,0,920,931),(942,1,0,20,941)");
        jdbc.update("INSERT INTO mes_pro_route_version(id,tenant_id,deleted,route_id,version_no,active,lifecycle_status) VALUES(960,165,0,920,'V27',true,'ACTIVE')");
        jdbc.update("UPDATE mes_pro_route_version SET route_snapshot_json=? WHERE id=960",
                "{\"configSnapshots\":{\"products\":[{\"itemId\":910}]}}");
    }

    @Test
    void testResetRejectsProductMissingFromPublishedVersionForNewAndExistingBindings() {
        testBindingFixture();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            jdbc.update("UPDATE mes_pro_route_version SET route_snapshot_json=? WHERE id=960",
                    "{\"configSnapshots\":{\"products\":[{\"itemId\":999}]}}");
            var error = assertThrows(RuntimeException.class, () ->
                    tx.executeWithoutResult(s -> initializer.ensureTestRouteProductBinding(77L)));
            assertTrue(error.getMessage().contains("发布新版本"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_product WHERE tenant_id=165", Integer.class));
            jdbc.update("INSERT INTO mes_pro_route_product(id,tenant_id,deleted,route_id,item_id) VALUES(970,165,0,920,910)");
            assertThrows(RuntimeException.class, () ->
                    tx.executeWithoutResult(s -> initializer.ensureTestRouteProductBinding(77L)));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_product WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void missingOrderCopiesReferenceFieldsAndBomWithoutImportingRouteQaOrRuntime() {
        fullReference();
        jdbc.update("UPDATE mes_pro_work_order SET name='Reference order',material_specification='AI30',demand_bill_no='DEMAND-1',quantity_scheduled=6,temporary_frozen=true WHERE id=300");
        jdbc.update("INSERT INTO mes_pro_work_order_bom(id,tenant_id,deleted,work_order_id,item_id,quantity,remark) VALUES(301,1,0,300,11,2.5,'Reference BOM')");
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            tx.executeWithoutResult(s -> initializer.createWorkOrderIfMissing(77L));
            var order = jdbc.queryForMap("SELECT * FROM mes_pro_work_order WHERE tenant_id=165");
            assertEquals(MesFixedTestOrderInitializer.WORK_ORDER_CODE, order.get("code"));
            assertEquals("Reference order", order.get("name"));
            assertEquals("AI30", order.get("material_specification"));
            assertEquals("DEMAND-1", order.get("demand_bill_no"));
            assertEquals(0, new java.math.BigDecimal("10").compareTo((java.math.BigDecimal) order.get("quantity")));
            assertEquals(0, ((java.math.BigDecimal) order.get("quantity_produced")).signum());
            assertEquals(0, ((java.math.BigDecimal) order.get("quantity_scheduled")).signum());
            assertEquals(false, order.get("temporary_frozen"));
            assertEquals("PRODUCT", jdbc.queryForObject("SELECT code FROM mes_md_item WHERE tenant_id=165 AND id=?", String.class, order.get("product_id")));
            var bom = jdbc.queryForMap("SELECT * FROM mes_pro_work_order_bom WHERE tenant_id=165");
            assertEquals(order.get("id"), bom.get("work_order_id"));
            assertEquals("MATERIAL", jdbc.queryForObject("SELECT code FROM mes_md_item WHERE tenant_id=165 AND id=?", String.class, bom.get("item_id")));
            assertEquals(0, new java.math.BigDecimal("2.5").compareTo((java.math.BigDecimal) bom.get("quantity")));
            for (String table : List.of("mes_pro_route", "mes_pro_route_version", "mes_qa_inspection_regulation", "mes_wm_product_issue"))
                assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=165", Integer.class));
            assertEquals(7, jdbc.queryForObject("SELECT quantity_produced FROM mes_pro_work_order WHERE id=300", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mdm_product WHERE tenant_id=165", Integer.class));
            assertNull(jdbc.queryForObject("SELECT product_master_id FROM mes_md_item WHERE tenant_id=165 AND code='PRODUCT'", Long.class));
            // Existing orders survive later source changes and are not compared against reference definitions.
            jdbc.update("UPDATE mes_pro_work_order SET name='Local order' WHERE tenant_id=165");
            jdbc.update("DELETE FROM mes_pro_work_order WHERE tenant_id=1");
            tx.executeWithoutResult(s -> initializer.createWorkOrderIfMissing(88L));
            assertEquals("Local order", jdbc.queryForObject("SELECT name FROM mes_pro_work_order WHERE tenant_id=165", String.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void orderCreationRetainsExistingProductStatusAndMetadataAndIgnoresQaConflicts() {
        fullReference();
        jdbc.update("INSERT INTO mdm_product(id,tenant_id,deleted,product_code) VALUES(914,165,0,'MASTER')");
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,name,product_master_id,status,unit_measure_id,item_type_id) VALUES(910,165,0,'PRODUCT','Local label',914,1,123,456)");
        jdbc.update("INSERT INTO mes_qa_inspection_regulation(id,tenant_id,deleted,owner_module,regulation_code,current_version_id) VALUES(940,165,0,'MES_QA_COMMON','COMMON',941),(942,165,0,'MES_QA_COMMON','COMMON',943)");
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).createWorkOrderIfMissing(77L));
            assertEquals(910L, jdbc.queryForObject("SELECT product_id FROM mes_pro_work_order WHERE tenant_id=165", Long.class));
            assertEquals(1, jdbc.queryForObject("SELECT status FROM mes_md_item WHERE id=910", Integer.class));
            assertEquals(123L, jdbc.queryForObject("SELECT unit_measure_id FROM mes_md_item WHERE id=910", Long.class));
            assertEquals("Local label", jdbc.queryForObject("SELECT name FROM mes_md_item WHERE id=910", String.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_inspection_regulation WHERE tenant_id=165", Integer.class));
            verifyNoInteractions(controlledContentAdapter);
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void orderCreationReusesProductCodeRegardlessOfShowroomMaster() {
        fullReference();
        jdbc.update("INSERT INTO mdm_product(id,tenant_id,deleted,product_code) VALUES(914,165,0,'OTHER')");
        jdbc.update("INSERT INTO mes_md_item(id,tenant_id,deleted,code,name,product_master_id) VALUES(910,165,0,'PRODUCT','Other',914)");
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).createWorkOrderIfMissing(77L));
            assertEquals(910L, jdbc.queryForObject("SELECT product_id FROM mes_pro_work_order WHERE tenant_id=165", Long.class));
            assertEquals(914L, jdbc.queryForObject("SELECT product_master_id FROM mes_md_item WHERE id=910", Long.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void missingReferenceOrderFailsWithoutInventingData() {
        fullReference();
        jdbc.update("DELETE FROM mes_pro_work_order WHERE tenant_id=1");
        TenantContextHolder.setTenantId(165L);
        try {
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s ->
                    new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).createWorkOrderIfMissing(77L)));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void laterResetFailureRollsBackOrderAndItsNewMasters() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(s -> {
                new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).createWorkOrderIfMissing(77L);
                throw new IllegalStateException("route prerequisite failed");
            }));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_item WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void productQaImportAllowsSameDocumentCodeInOtherProjectsAndOwnerModules() {
        fullReference();
        jdbc.update("INSERT INTO mes_qa_inspection_regulation(id,tenant_id,deleted,dcc_project_code_id,owner_module,regulation_code,lifecycle_status) VALUES(901,165,0,9001,'MES_QA','QA-FIXED','PUBLISHED'),(902,165,0,9002,'MES_QA','QA-FIXED','PUBLISHED'),(903,165,0,NULL,'MES_QA_COMMON','QA-FIXED','PUBLISHED')");
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L));
            Long targetDcc = jdbc.queryForObject("SELECT id FROM dcc_project_code WHERE tenant_id=165", Long.class);
            Long targetQa = jdbc.queryForObject("SELECT id FROM mes_qa_inspection_regulation WHERE tenant_id=165 AND owner_module='MES_QA' AND dcc_project_code_id=?", Long.class, targetDcc);
            var repeat = new MesFixedTestOrderReferenceGraph(jdbc, 165L, 77L);
            repeat.include("mes_qa_inspection_regulation", 140L);
            tx.executeWithoutResult(s -> repeat.resolveAll());
            assertEquals(targetQa, repeat.id("mes_qa_inspection_regulation", 140L));
            assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_inspection_regulation WHERE tenant_id=165", Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_inspection_regulation WHERE id IN (901,902) AND regulation_code='QA-FIXED'", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void productQaStillRejectsDuplicateProjectIdentityRegardlessOfDocumentCode() {
        fullReference();
        graph.include("mes_qa_inspection_regulation", 140L);
        Object dcc = graph.id("dcc_project_code", 131L);
        jdbc.update("INSERT INTO mes_qa_inspection_regulation(id,tenant_id,deleted,dcc_project_code_id,owner_module,regulation_code) VALUES(901,165,0,?,'MES_QA','CODE-A'),(902,165,0,?,'MES_QA','CODE-B')", dcc, dcc);
        var error = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> tx.executeWithoutResult(s -> graph.insertAll()));
        assertTrue(error.getMessage().contains("自然身份不唯一"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_inspection_regulation WHERE tenant_id=165", Integer.class));
    }

    @Test
    void commonQaIdentityUsesOwnerAndCodeWithoutProjectAndRejectsRealDuplicates() {
        createTable("mes_qa_inspection_regulation");
        jdbc.update("INSERT INTO mes_qa_inspection_regulation(id,tenant_id,deleted,owner_module,regulation_code) VALUES(140,1,0,'MES_QA_COMMON','COMMON'),(901,165,0,'MES_QA','COMMON'),(902,165,0,'MES_QA_COMMON','COMMON'),(903,166,0,'MES_QA_COMMON','COMMON')");
        graph.include("mes_qa_inspection_regulation", 140L);
        tx.executeWithoutResult(s -> graph.resolveAll());
        assertEquals(902L, graph.id("mes_qa_inspection_regulation", 140L));
        jdbc.update("INSERT INTO mes_qa_inspection_regulation(id,tenant_id,deleted,owner_module,regulation_code) VALUES(904,165,0,'MES_QA_COMMON','COMMON')");
        var duplicate = new MesFixedTestOrderReferenceGraph(jdbc, 165L, 77L);
        duplicate.include("mes_qa_inspection_regulation", 140L);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> tx.executeWithoutResult(s -> duplicate.insertAll()));
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_inspection_regulation", Integer.class));
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L)));
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
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L)));
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            jdbc.update("UPDATE mes_pro_edhr_permission_rule SET ability='SIGN' WHERE tenant_id=165 AND ability='VIEW'");
            // Reusing the tenant route does not reconcile its permissions against another tenant.
            tx.executeWithoutResult(s -> initializer.initialize(88L));
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
        var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
        var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(initializer, "replaceStartLeaders", snapshot(), 165L));
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> initializer.initialize(77L));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM system_users", Integer.class));
    }

    @Test
    void initializerCreatesMissingOrderThenReusesTheSameDefinitions() {
        fullReference();
        var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
    void resetUsesTenantsOwnProjectRouteAndCurrentVersionWithoutImportingAnotherRoute() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            Long route = jdbc.queryForObject("SELECT id FROM mes_pro_route WHERE tenant_id=165", Long.class);
            Long version = jdbc.queryForObject("SELECT id FROM mes_pro_route_version WHERE tenant_id=165", Long.class);
            String snapshot = jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE id=?", String.class, version);
            jdbc.update("UPDATE mes_pro_route SET code='TENANT-ROUTE',name='Tenant maintained route',remark=NULL WHERE id=?", route);
            jdbc.update("UPDATE mes_pro_route_version SET version_no='TENANT-V9' WHERE id=?", version);
            jdbc.execute("CREATE UNIQUE INDEX project_one_route ON mes_pro_route_dcc_project_binding(tenant_id,dcc_project_code_id)");
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route WHERE tenant_id=165", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_version WHERE tenant_id=165", Integer.class));
            assertEquals("TENANT-V9", jdbc.queryForObject("SELECT version_no FROM mes_pro_route_version WHERE id=?", String.class, version));
            assertEquals(snapshot, jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE id=?", String.class, version));
            verify(controlledContentAdapter).recordActiveRegisteredIfMissing(
                    argThat(v -> version.equals(v.getId()) && "TENANT-V9".equals(v.getVersionNo())), eq(77L), eq("固定测试订单使用当前租户正式工艺版本"));
            // Even a route originally imported by this initializer follows the tenant's current version.
            jdbc.update("UPDATE mes_pro_route SET code='RT-FIXED',remark=? WHERE id=?", MARKER, route);
            jdbc.update("UPDATE mes_pro_route_version SET version_no='TENANT-V10' WHERE id=?", version);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            assertEquals("TENANT-V10", jdbc.queryForObject("SELECT version_no FROM mes_pro_route_version WHERE id=?", String.class, version));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_version WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void tenantRouteWithInvalidPublishedSnapshotStopsWithoutImportingAReplacement() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            jdbc.update("UPDATE mes_pro_route SET remark=NULL WHERE tenant_id=165");
            jdbc.update("UPDATE mes_pro_route_version SET route_snapshot_sha256='invalid' WHERE tenant_id=165");
            var error = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.initialize(77L)));
            assertTrue(error.getMessage().contains("当前租户正式路线快照校验失败"));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route WHERE tenant_id=165", Integer.class));
            assertEquals("invalid", jdbc.queryForObject("SELECT route_snapshot_sha256 FROM mes_pro_route_version WHERE tenant_id=165", String.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void tenantRouteRetainsCompletedProductMetadataAndRequiresPublishedProductBinding() {
        fullReference();
        jdbc.execute("ALTER TABLE mes_md_item ALTER COLUMN safe_stock_flag BOOLEAN");
        jdbc.execute("ALTER TABLE mes_md_item ALTER COLUMN min_stock DECIMAL(18,6)");
        jdbc.execute("ALTER TABLE mes_md_item ALTER COLUMN max_stock DECIMAL(18,6)");
        jdbc.update("UPDATE mes_md_item SET safe_stock_flag='false' WHERE id=10");
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            jdbc.update("UPDATE mes_md_item SET unit_measure_id=901,item_type_id=902,min_stock='0',max_stock='0' WHERE tenant_id=165 AND code='PRODUCT'");
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            assertEquals(901L, jdbc.queryForObject("SELECT unit_measure_id FROM mes_md_item WHERE tenant_id=165 AND code='PRODUCT'", Long.class));
            assertEquals(902L, jdbc.queryForObject("SELECT item_type_id FROM mes_md_item WHERE tenant_id=165 AND code='PRODUCT'", Long.class));
            ObjectNode snapshot = (ObjectNode) graph.parse(jdbc.queryForObject("SELECT route_snapshot_json FROM mes_pro_route_version WHERE tenant_id=165", String.class));
            ((ObjectNode) snapshot.path("configSnapshots")).putArray("products");
            String json = canonicalizer.canonicalize(snapshot.toString());
            jdbc.update("UPDATE mes_pro_route_version SET route_snapshot_json=?,route_snapshot_sha256=? WHERE tenant_id=165", json, canonicalizer.sha256(json));
            var error = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.initialize(77L)));
            assertTrue(error.getMessage().contains("先发布产品关联"));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerRegistersTheMappedActiveVersionOnImportAndReuse() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            var versions = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
            verify(controlledContentAdapter, times(2)).recordActiveRegisteredIfMissing(
                    versions.capture(), anyLong(), anyString());
            Long versionId = jdbc.queryForObject("SELECT id FROM mes_pro_route_version WHERE tenant_id=165", Long.class);
            Long routeId = jdbc.queryForObject("SELECT id FROM mes_pro_route WHERE tenant_id=165", Long.class);
            for (var version : versions.getAllValues()) {
                assertEquals(versionId, version.getId());
                assertEquals(routeId, version.getRouteId());
                assertEquals("V1", version.getVersionNo());
                assertEquals(Boolean.TRUE, version.getActive());
                assertEquals("ACTIVE", version.getLifecycleStatus());
                assertNotEquals(60L, version.getId());
            }
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void controlledVersionRegistrationFailureRollsBackImportedDefinitions() {
        fullReference();
        doThrow(new IllegalStateException("controlled content active ref drifted"))
                .when(controlledContentAdapter).recordActiveRegisteredIfMissing(any(), eq(77L), anyString());
        TenantContextHolder.setTenantId(165L);
        try {
            assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(s ->
                    new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L)));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route_version WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initializerUsesActiveCommonQaIdentityAndPreservesDisabledBindingHistory() {
        fullReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L)));
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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

    @Test
    void newCommonBindingUsesTenantPublishedVersionAndLeavesBothCatalogsUnchanged() {
        commonCatalogWithDifferentTenantVersions();
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            String snapshot = jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=701", String.class);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            assertEquals(701L, jdbc.queryForObject("SELECT regulation_version_id FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165", Long.class));
            assertEquals(701L, jdbc.queryForObject("SELECT regulation_version_id FROM mes_qa_common_regulation_set_version_member WHERE tenant_id=165", Long.class));
            assertEquals(701L, jdbc.queryForObject("SELECT current_version_id FROM mes_qa_inspection_regulation WHERE id=700", Long.class));
            assertEquals("TENANT-A8", jdbc.queryForObject("SELECT version_no FROM mes_qa_inspection_regulation_version WHERE id=701", String.class));
            assertEquals(snapshot, jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=701", String.class));
            assertEquals(151L, jdbc.queryForObject("SELECT current_version_id FROM mes_qa_inspection_regulation WHERE id=150", Long.class));
            assertEquals(152L, jdbc.queryForObject("SELECT regulation_version_id FROM mes_qa_common_regulation_product_binding WHERE id=500", Long.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_inspection_regulation_version WHERE tenant_id=165 AND regulation_id=700", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_common_regulation_set_version WHERE tenant_id=165", Integer.class));
            tx.executeWithoutResult(s -> initializer.initialize(88L));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void newBindingUsesExistingTenantSetCurrentVersionAndItsPinnedMembers() {
        commonCatalogWithDifferentTenantVersions();
        jdbc.update("INSERT INTO mes_qa_inspection_regulation_version(id,tenant_id,deleted,regulation_id,version_no,lifecycle_status,snapshot_json) SELECT 702,tenant_id,0,regulation_id,'TENANT-A7','RETIRED',snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=701");
        jdbc.update("INSERT INTO mes_qa_common_regulation_set(id,tenant_id,deleted,set_code,set_status,current_version_id) VALUES(800,165,0,'SHARED-SET','ENABLED',801)");
        jdbc.update("INSERT INTO mes_qa_common_regulation_set_version(id,tenant_id,deleted,set_id,version_no,lifecycle_status) VALUES(801,165,0,800,'TENANT-SET-V4','PUBLISHED')");
        jdbc.update("INSERT INTO mes_qa_common_regulation_set_version_member(id,tenant_id,deleted,set_version_id,regulation_id,regulation_version_id,sort,member_role) VALUES(802,165,0,801,700,702,10,'Packaging')");
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L));
            assertEquals(801L, jdbc.queryForObject("SELECT common_regulation_set_version_id FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165", Long.class));
            assertEquals(702L, jdbc.queryForObject("SELECT regulation_version_id FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165", Long.class));
            assertEquals(701L, jdbc.queryForObject("SELECT current_version_id FROM mes_qa_inspection_regulation WHERE id=700", Long.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_common_regulation_set_version WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void commonPublicationWithoutProjectUsesLocalVersionDespiteLegacyParentProject() {
        commonCatalogWithDifferentTenantVersions();
        ObjectNode snapshot = (ObjectNode) graph.parse(jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=701", String.class));
        snapshot.remove("dccProjectCodeId");
        jdbc.update("UPDATE mes_qa_inspection_regulation_version SET snapshot_json=? WHERE id=701", snapshot.toString());
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            tx.executeWithoutResult(s -> initializer.initialize(77L));
            assertEquals(701L, jdbc.queryForObject("SELECT regulation_version_id FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165", Long.class));
            assertEquals(9131L, jdbc.queryForObject("SELECT dcc_project_code_id FROM mes_qa_inspection_regulation WHERE id=700", Long.class));
            assertEquals(snapshot.toString(), jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=701", String.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void newBindingRejectsTenantSnapshotWithAnotherRegulationIdentity() {
        commonCatalogWithDifferentTenantVersions();
        ObjectNode snapshot = (ObjectNode) graph.parse(jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=701", String.class));
        snapshot.put("regulationId", 999L);
        jdbc.update("UPDATE mes_qa_inspection_regulation_version SET snapshot_json=? WHERE id=701", snapshot.toString());
        TenantContextHolder.setTenantId(165L);
        try {
            var error = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L)));
            assertTrue(error.getMessage().contains("规程ID=700"));
            assertTrue(error.getMessage().contains("版本ID=701"));
            assertTrue(error.getMessage().contains("快照规程ID=999"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165", Integer.class));
            assertEquals(snapshot.toString(), jdbc.queryForObject("SELECT snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=701", String.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void newBindingRejectsUnpublishedTenantCommonVersionWithoutImportingSourceOverIt() {
        commonCatalogWithDifferentTenantVersions();
        jdbc.update("UPDATE mes_qa_inspection_regulation_version SET lifecycle_status='DRAFT' WHERE id=701");
        TenantContextHolder.setTenantId(165L);
        try {
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L)));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_qa_common_regulation_product_binding WHERE tenant_id=165", Integer.class));
            assertEquals(701L, jdbc.queryForObject("SELECT current_version_id FROM mes_qa_inspection_regulation WHERE id=700", Long.class));
            assertEquals("DRAFT", jdbc.queryForObject("SELECT lifecycle_status FROM mes_qa_inspection_regulation_version WHERE id=701", String.class));
        } finally { TenantContextHolder.clear(); }
    }

    private void commonCatalogWithDifferentTenantVersions() {
        fullReference();
        addCommonQaDefinition(1, 150, 151, 131, "SHARED-COMMON");
        addCommonQaDefinition(165, 700, 701, 9131, "SHARED-COMMON");
        jdbc.update("UPDATE mes_qa_inspection_regulation_version SET version_no=CASE WHEN id=151 THEN 'SOURCE-A9' ELSE 'TENANT-A8' END WHERE id IN (151,701)");
        jdbc.update("INSERT INTO mes_qa_inspection_regulation_version(id,tenant_id,deleted,regulation_id,version_no,lifecycle_status,snapshot_json) SELECT 152,tenant_id,0,regulation_id,'SOURCE-A7','RETIRED',snapshot_json FROM mes_qa_inspection_regulation_version WHERE id=151");
        jdbc.update("INSERT INTO mes_qa_common_regulation_set(id,tenant_id,deleted,set_code,set_status,current_version_id) VALUES(600,1,0,'SHARED-SET','ENABLED',603)");
        jdbc.update("INSERT INTO mes_qa_common_regulation_set_version(id,tenant_id,deleted,set_id,version_no,lifecycle_status) VALUES(601,1,0,600,'SOURCE-SET-V1','PUBLISHED'),(603,1,0,600,'SOURCE-SET-V2','PUBLISHED')");
        jdbc.update("INSERT INTO mes_qa_common_regulation_set_version_member(id,tenant_id,deleted,set_version_id,regulation_id,regulation_version_id,sort,member_role) VALUES(602,1,0,601,150,152,10,'Packaging'),(604,1,0,603,150,151,10,'Packaging')");
        jdbc.update("INSERT INTO mes_qa_common_regulation_product_binding(id,tenant_id,deleted,product_id,dcc_project_code_id,common_regulation_set_id,common_regulation_set_version_id,regulation_id,regulation_version_id,scope_code,binding_status,version) VALUES(500,1,0,14,131,600,601,150,152,'COMMON_PACKAGING','ENABLED',1)");
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
            var failure = assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L)));
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L)));
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
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L));
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L)));
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
            var failure = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L)));
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
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(77L));
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
            var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
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
                    tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).initialize(actor));
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
        var initializer = new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter);
        jdbc.update("INSERT INTO mes_pro_process_pool_event VALUES(1,166,300,0),(2,165,301,0),(3,165,300,1)");
        assertDoesNotThrow(() -> initializer.requireNoOrphanRuntime(165L, 300L));
        jdbc.update("INSERT INTO mes_pro_process_pool_event VALUES(4,165,300,0)");
        assertThrows(RuntimeException.class, () -> initializer.requireNoOrphanRuntime(165L, 300L));
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_event", Integer.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void fixedP1ImportsOnlyItsPickLinesRemapsHeadersAndIsIdempotent() {
        var service = dataReference();
        jdbc.update("UPDATE erp_kingdee_production_pick_list SET source_bill_no='PICK-1' WHERE id=210");
        jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET source_bill_no='PICK-1',source_entry_id='ENTRY-1',lot_number='LOT-1' WHERE id=211");
        jdbc.update("INSERT INTO erp_kingdee_production_pick_list_item(id,tenant_id,deleted,production_pick_list_id,source_line_key,production_order_no) VALUES(212,1,0,210,'OTHER-LINE','OTHER-ORDER')");
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).createWorkOrderIfMissing(77L));
            var initializer = new MesFixedTestOrderPickListInitializer(service);
            tx.executeWithoutResult(s -> initializer.prepare(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L));
            tx.executeWithoutResult(s -> initializer.prepare(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L));
            var line = jdbc.queryForMap("SELECT * FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165");
            assertEquals("LOT-1", line.get("lot_number"));
            assertEquals("LINE-FID", line.get("source_fid"));
            assertNotEquals(210L, ((Number) line.get("production_pick_list_id")).longValue());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list WHERE tenant_id=165 AND id=?", Integer.class, line.get("production_pick_list_id")));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165 AND production_order_no='OTHER-ORDER'", Integer.class));
            jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET lot_number='LOCAL-LOT' WHERE tenant_id=165");
            tx.executeWithoutResult(s -> initializer.prepare(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L));
            assertEquals("LOT-1", jdbc.queryForObject("SELECT lot_number FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165", String.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void fixedP1AutomaticallyPreparesEachCurrentTenantWithoutDuplicatingOrCrossingTenants() {
        var service = dataReference();
        var initializer = new MesFixedTestOrderPickListInitializer(service);
        List<Long> tenants = List.of(165L, 170L, 271L);
        try {
            for (Long tenant : tenants) {
                if (tenant != 165L) jdbc.update("INSERT INTO system_tenant VALUES(?,0)", tenant);
                jdbc.update("INSERT INTO mes_pro_work_order(id,tenant_id,deleted,code) VALUES(?,?,0,?)",
                        10000L + tenant, tenant, MesFixedTestOrderInitializer.WORK_ORDER_CODE);
                TenantContextHolder.setTenantId(tenant);
                tx.executeWithoutResult(s -> initializer.prepare(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L));
                tx.executeWithoutResult(s -> initializer.prepare(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L));
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item WHERE tenant_id=?", Integer.class, tenant));
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list WHERE tenant_id=?", Integer.class, tenant));
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item i JOIN erp_kingdee_production_pick_list h ON h.id=i.production_pick_list_id AND h.tenant_id=i.tenant_id WHERE i.tenant_id=?", Integer.class, tenant));
            }
            assertEquals(3, jdbc.queryForObject("SELECT COUNT(DISTINCT production_pick_list_id) FROM erp_kingdee_production_pick_list_item WHERE tenant_id IN (165,170,271)", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item WHERE tenant_id=1", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void failedP1RollsBackImportedPickSourcesAndOrdinaryOrdersAreUntouched() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).createWorkOrderIfMissing(77L));
            var initializer = new MesFixedTestOrderPickListInitializer(service);
            tx.executeWithoutResult(s -> initializer.prepare("OTHER-ORDER", 77L));
            assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(s -> {
                initializer.prepare(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L);
                throw new IllegalStateException("later P1 failure");
            }));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = {165L, 170L, 271L})
    void completionImportsMissingLocalIssuesForEachTenantWithoutChangingRouteOrDuplicating(long tenant) {
        fullReference();
        if (tenant != 165L) jdbc.update("INSERT INTO system_tenant VALUES(?,0)", tenant);
        jdbc.update("INSERT INTO mes_pro_work_order(id,tenant_id,deleted,code) VALUES(900,?,0,?)", tenant, MesFixedTestOrderInitializer.WORK_ORDER_CODE);
        jdbc.update("UPDATE mes_wm_product_issue_detail SET batch_code='LOT' WHERE id=207");
        TenantContextHolder.setTenantId(tenant);
        try {
            var initializer = new MesFixedTestOrderCompletionSourceInitializer(jdbc);
            tx.executeWithoutResult(s -> initializer.prepare(900L, 77L));
            tx.executeWithoutResult(s -> initializer.prepare(900L, 77L));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_product_issue WHERE tenant_id=? AND work_order_id=900 AND status=4", Integer.class, tenant));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_batch WHERE tenant_id=? AND work_order_id=900", Integer.class, tenant));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_product_issue_detail d JOIN mes_wm_product_issue i ON i.id=d.issue_id AND i.tenant_id=d.tenant_id JOIN mes_wm_material_stock s ON s.id=d.material_stock_id AND s.tenant_id=d.tenant_id JOIN mes_wm_batch b ON b.id=d.batch_id AND b.tenant_id=d.tenant_id WHERE d.tenant_id=? AND i.work_order_id=900 AND b.work_order_id=900", Integer.class, tenant));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_route WHERE tenant_id=?", Integer.class, tenant));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=?", Integer.class, tenant));
            jdbc.update("UPDATE mes_wm_product_issue SET status=1 WHERE tenant_id=?", tenant);
            tx.executeWithoutResult(s -> initializer.prepare(900L, 77L));
            assertEquals(1, jdbc.queryForObject("SELECT status FROM mes_wm_product_issue WHERE tenant_id=?", Integer.class, tenant));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void completionFixtureRejectsAnotherOrdersBatchAndRollsBackOnLaterFailure() {
        fullReference();
        jdbc.update("INSERT INTO mes_pro_work_order(id,tenant_id,deleted,code) VALUES(900,165,0,?)", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
        jdbc.update("UPDATE mes_wm_product_issue_detail SET batch_code='LOT' WHERE id=207");
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderCompletionSourceInitializer(jdbc);
            jdbc.update("UPDATE mes_wm_batch SET work_order_id=999 WHERE id=202");
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.prepare(900L, 77L)));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_product_issue WHERE tenant_id=165", Integer.class));
            jdbc.update("UPDATE mes_wm_batch SET work_order_id=300 WHERE id=202");
            assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(s -> {
                initializer.prepare(900L, 77L);
                throw new IllegalStateException("later completion failure");
            }));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_product_issue WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_material_stock WHERE tenant_id=165", Integer.class));
            jdbc.update("UPDATE mes_pro_work_order SET code='ORDINARY' WHERE id=900");
            tx.executeWithoutResult(s -> initializer.prepare(900L, 77L));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_wm_product_issue WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void resetRebuildsAllMaterialDocumentsWithExactBusinessData() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter).createWorkOrderIfMissing(77L));
            var initializer = new MesFixedTestOrderPickListInitializer(service);
            // Both entry points rebuild all three document types using the same source.
            tx.executeWithoutResult(s -> initializer.prepare(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L));
            Object pickId = jdbc.queryForObject("SELECT id FROM erp_kingdee_production_pick_list WHERE tenant_id=165", Long.class);
            tx.executeWithoutResult(s -> initializer.prepareForReset(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 88L));
            tx.executeWithoutResult(s -> initializer.prepareForReset(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 99L));
            assertEquals(pickId, jdbc.queryForObject("SELECT id FROM erp_kingdee_production_pick_list WHERE tenant_id=165", Long.class));
            assertEquals("99", jdbc.queryForObject("SELECT creator FROM erp_kingdee_production_pick_list WHERE tenant_id=165", String.class));
            @SuppressWarnings("unchecked")
            Map<String, Spec> specs = (Map<String, Spec>) ReflectionTestUtils.getField(graph, "specs");
            for (String kind : List.of("pick", "replenishment")) {
                String head = "erp_kingdee_production_" + kind + "_list";
                String item = head + "_item";
                String fk = "production_" + kind + "_list_id";
                var targetHead = jdbc.queryForMap("SELECT * FROM " + head + " WHERE tenant_id=165");
                var targetItem = jdbc.queryForMap("SELECT * FROM " + item + " WHERE tenant_id=165");
                var sourceHead = jdbc.queryForMap("SELECT * FROM " + head + " WHERE tenant_id=1");
                var sourceItem = jdbc.queryForMap("SELECT * FROM " + item + " WHERE tenant_id=1 AND production_order_no=?", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
                for (String field : specs.get(head).columns().split(",")) assertEquals(sourceHead.get(field), targetHead.get(field), head + "/" + field);
                for (String field : specs.get(item).columns().split(",")) {
                    if (!field.equals(fk)) assertEquals(sourceItem.get(field), targetItem.get(field), item + "/" + field);
                }
                assertEquals(targetHead.get("id"), targetItem.get(fk));
                assertNotEquals(sourceHead.get("id"), targetHead.get("id"));
            }
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_replenishment_list_item WHERE tenant_id=165 AND production_order_no='OTHER-ORDER'", Integer.class));
            // A deleted/missing line is restored under its existing local header.
            Object headerId = jdbc.queryForObject("SELECT id FROM erp_kingdee_production_replenishment_list WHERE tenant_id=165", Long.class);
            jdbc.update("UPDATE erp_kingdee_production_replenishment_list_item SET deleted=1 WHERE tenant_id=165");
            tx.executeWithoutResult(s -> initializer.prepareForReset(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 99L));
            assertEquals(headerId, jdbc.queryForObject("SELECT production_replenishment_list_id FROM erp_kingdee_production_replenishment_list_item WHERE tenant_id=165 AND deleted=0", Long.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void resetMaterialRebuildIsAtomicAndReplacesEditedLocalFacts() {
        var service = dataReference();
        jdbc.update("INSERT INTO mes_pro_work_order(id,tenant_id,deleted,code) VALUES(900,165,0,?)", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
        TenantContextHolder.setTenantId(165L);
        try {
            var initializer = new MesFixedTestOrderPickListInitializer(service);
            jdbc.update("UPDATE erp_kingdee_production_replenishment_list SET document_status='A' WHERE tenant_id=1");
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> initializer.prepareForReset(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L)));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list WHERE tenant_id=165", Integer.class));
            jdbc.update("UPDATE erp_kingdee_production_replenishment_list SET document_status='C' WHERE tenant_id=1");
            assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(s -> {
                initializer.prepareForReset(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L);
                throw new IllegalStateException("later reset failure");
            }));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_replenishment_list WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list WHERE tenant_id=165", Integer.class));
            tx.executeWithoutResult(s -> initializer.prepareForReset(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L));
            jdbc.update("UPDATE erp_kingdee_production_replenishment_list_item SET actual_quantity=99 WHERE tenant_id=165");
            tx.executeWithoutResult(s -> initializer.prepareForReset(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 77L));
            assertEquals(0, new java.math.BigDecimal("2.5").compareTo(jdbc.queryForObject("SELECT actual_quantity FROM erp_kingdee_production_replenishment_list_item WHERE tenant_id=165", java.math.BigDecimal.class)));
            assertEquals(0, new java.math.BigDecimal("2.5").compareTo(jdbc.queryForObject("SELECT actual_quantity FROM erp_kingdee_production_replenishment_list_item WHERE tenant_id=1 AND id=221", java.math.BigDecimal.class)));
        } finally { TenantContextHolder.clear(); }
    }

    private void replenishmentReference() {
        jdbc.update("INSERT INTO erp_kingdee_production_replenishment_list(id,tenant_id,deleted,source_form_id,source_fid,source_bill_no,document_status,description) VALUES(220,1,0,'REPLENISH','R-HEADER','REPL-001','C','Reference replenishment')");
        jdbc.update("INSERT INTO erp_kingdee_production_replenishment_list_item(id,tenant_id,deleted,production_replenishment_list_id,source_form_id,source_fid,source_entry_id,source_line_key,source_bill_no,material_number,material_name,material_specification,unit_name,actual_quantity,requested_quantity,base_actual_quantity,warehouse_number,lot_number,production_order_no) VALUES(221,1,0,220,'REPLENISH','R-LINE','ENTRY-R','REPL-LINE','REPL-001','MATERIAL','Material','Reference spec','PCS',2.5,3,2.5,'WH','LOT-R',?)", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
        jdbc.update("INSERT INTO erp_kingdee_production_replenishment_list_item(id,tenant_id,deleted,production_replenishment_list_id,source_form_id,source_line_key,production_order_no) VALUES(222,1,0,220,'REPLENISH','OTHER-REPL','OTHER-ORDER')");
    }

    @Test
    void explicitDataPreparationSkipsExistingDocumentsAndRetainsOrderProgress() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var first = tx.execute(s -> service.prepare(77L));
            assertEquals(List.of(1, 1, 1, 2), first.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::createdCount).toList());
            jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET actual_quantity=99 WHERE tenant_id=165");
            jdbc.update("UPDATE erp_kingdee_production_pick_list SET description='Keep local header' WHERE tenant_id=165");
            jdbc.update("UPDATE mes_kingdee_production_material_list SET required_quantity='99' WHERE tenant_id=165");
            var documentTables = List.of("erp_kingdee_production_pick_list", "erp_kingdee_production_pick_list_item",
                    "erp_kingdee_production_replenishment_list", "erp_kingdee_production_replenishment_list_item", "mes_kingdee_production_material_list");
            var documentsBefore = documentTables.stream().map(table -> jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=165 ORDER BY id")).toList();
            Object orderId = jdbc.queryForObject("SELECT id FROM mes_pro_work_order WHERE tenant_id=165", Long.class);
            jdbc.update("UPDATE mes_pro_work_order SET quantity_produced=9,status='2' WHERE tenant_id=165");
            var second = tx.execute(s -> service.prepare(77L));
            assertEquals(List.of(0, 0, 0, 0), second.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::createdCount).toList());
            assertEquals(List.of(0, 0, 0, 0), second.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::deletedCount).toList());
            assertEquals(List.of(1, 1, 1, 2), second.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::skippedCount).toList());
            for (int i = 0; i < documentTables.size(); i++)
                assertEquals(documentsBefore.get(i), jdbc.queryForList("SELECT * FROM " + documentTables.get(i) + " WHERE tenant_id=165 ORDER BY id"));
            assertEquals(9, jdbc.queryForObject("SELECT quantity_produced FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_kingdee_production_material_list WHERE tenant_id=165 AND work_order_id=?", Integer.class, orderId));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_kingdee_production_material_list WHERE tenant_id=165 AND (product_id=10 OR child_material_id=11)", Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_kingdee_production_material_list WHERE tenant_id=1", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_active_order WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void explicitDataPreparationFillsPartialLinesAndKeepsOtherTenantUntouched() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> service.prepare(77L));
            jdbc.update("DELETE FROM mes_kingdee_production_material_list WHERE tenant_id=165 AND source_entry_id='ENTRY-2'");
            jdbc.update("DELETE FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165");
            var result = tx.execute(s -> service.prepare(77L));
            assertEquals(List.of(0, 1, 0, 1), result.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::createdCount).toList());
            assertEquals(List.of(0, 0, 0, 0), result.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::deletedCount).toList());
            assertEquals(List.of(1, 0, 1, 1), result.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::skippedCount).toList());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list WHERE tenant_id=165", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item WHERE tenant_id=1 AND production_order_no=?", Integer.class, MesFixedTestOrderInitializer.WORK_ORDER_CODE));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item i JOIN erp_kingdee_production_pick_list h ON h.id=i.production_pick_list_id WHERE i.tenant_id=165 AND h.tenant_id<>165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void explicitDataPreparationRollsBackAllDocumentsWhenRequiredSourceIsMissing() {
        var service = dataReference();
        jdbc.update("DELETE FROM erp_kingdee_production_replenishment_list_item WHERE tenant_id=1");
        TenantContextHolder.setTenantId(165L);
        try {
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> service.prepare(77L)));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void secondDataSetConnectsBothIssueTypesToItsSingleMaterialBillDespiteHistoricalSourceNumbers() {
        var service = dataReference();
        jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET production_material_list_no='OLD-PICK-PML' WHERE tenant_id=1");
        jdbc.update("UPDATE erp_kingdee_production_replenishment_list_item SET production_material_list_no='OLD-REPLEN-PML' WHERE tenant_id=1");
        TenantContextHolder.setTenantId(165L);
        try {
            var result = tx.execute(s -> service.prepareSecond(77L));
            String bill = jdbc.queryForObject("SELECT DISTINCT source_bill_no FROM mes_kingdee_production_material_list WHERE tenant_id=165", String.class);
            for (String kind : List.of("pick", "replenishment")) {
                assertEquals(bill, jdbc.queryForObject("SELECT production_material_list_no FROM erp_kingdee_production_" + kind + "_list_item WHERE tenant_id=165", String.class));
            }
            assertEquals(List.of(0, 0, 0, 0), result.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::repairedCount).toList());
            assertEquals("OLD-PICK-PML", jdbc.queryForObject("SELECT production_material_list_no FROM erp_kingdee_production_pick_list_item WHERE tenant_id=1 AND production_order_no=?", String.class, MesFixedTestOrderInitializer.WORK_ORDER_CODE));
            assertEquals("OLD-REPLEN-PML", jdbc.queryForObject("SELECT production_material_list_no FROM erp_kingdee_production_replenishment_list_item WHERE tenant_id=1 AND production_order_no=?", String.class, MesFixedTestOrderInitializer.WORK_ORDER_CODE));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void secondDataSetRepairsOnlyLegacyGeneratedReferencesWithoutReplacingQuantitiesOrIds() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> service.prepareSecond(77L));
            String bill = jdbc.queryForObject("SELECT DISTINCT source_bill_no FROM mes_kingdee_production_material_list WHERE tenant_id=165", String.class);
            for (String kind : List.of("pick", "replenishment")) legacySecondIssue(kind);
            var ids = jdbc.queryForList("SELECT id FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165", Long.class);
            jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET actual_quantity=99 WHERE tenant_id=165");
            var result = tx.execute(s -> service.prepareSecond(88L));
            assertEquals(List.of(0, 1, 1, 0), result.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::repairedCount).toList());
            assertEquals(List.of(1, 0, 0, 2), result.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::skippedCount).toList());
            assertEquals(List.of(0, 0, 0, 0), result.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::createdCount).toList());
            assertEquals(ids, jdbc.queryForList("SELECT id FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165", Long.class));
            assertEquals(99, jdbc.queryForObject("SELECT actual_quantity FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165", Integer.class));
            for (String kind : List.of("pick", "replenishment")) {
                var row = jdbc.queryForMap("SELECT * FROM erp_kingdee_production_" + kind + "_list_item WHERE tenant_id=165");
                assertEquals(bill, row.get("production_material_list_no"));
                var raw = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(row.get("raw_payload").toString());
                assertEquals(bill, raw.path("document").path("production_material_list_no").asText());
                assertEquals(bill, raw.path("materialListReferenceRepair").path("corrected").asText());
                assertEquals(88L, raw.path("materialListReferenceRepair").path("actor").asLong());
            }
            var repeated = tx.execute(s -> service.prepareSecond(99L));
            assertEquals(List.of(0, 0, 0, 0), repeated.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::repairedCount).toList());
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void secondDataSetDoesNotGuessBetweenMultipleSourceMaterialBills() {
        var service = dataReference();
        jdbc.update("UPDATE mes_kingdee_production_material_list SET source_bill_no='SECOND-BILL' WHERE tenant_id=1 AND source_entry_id='ENTRY-2'");
        TenantContextHolder.setTenantId(165L);
        try {
            var error = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> service.prepareSecond(77L)));
            assertTrue(error.getMessage().contains("不能推断"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void secondDataSetRefusesManualReferenceChangesAndRollsBackEarlierRepairs() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> service.prepareSecond(77L));
            legacySecondIssue("pick");
            var before = jdbc.queryForList("SELECT * FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165");
            jdbc.update("UPDATE erp_kingdee_production_replenishment_list_item SET production_material_list_no='MANUAL-BILL' WHERE tenant_id=165");
            var error = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> service.prepareSecond(88L)));
            assertTrue(error.getMessage().contains("不能自动覆盖"));
            assertEquals(before, jdbc.queryForList("SELECT * FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165"));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void secondDataSetCannotRepairReferencesAfterAnOrderHasEnteredProduction() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> service.prepareSecond(77L));
            legacySecondIssue("pick");
            jdbc.update("UPDATE mes_pro_work_order SET quantity_produced=1 WHERE tenant_id=165");
            var error = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> service.prepareSecond(88L)));
            assertTrue(error.getMessage().contains("已进入生产"));
            jdbc.update("UPDATE mes_pro_work_order SET quantity_produced=0 WHERE tenant_id=165");
            jdbc.update("INSERT INTO mes_pro_process_pool_active_order(id,tenant_id,deleted,work_order_id) SELECT 9999,165,0,id FROM mes_pro_work_order WHERE tenant_id=165");
            var active = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> service.prepareSecond(88L)));
            assertTrue(active.getMessage().contains("已进入生产"));
        } finally { TenantContextHolder.clear(); }
    }

    private void legacySecondIssue(String kind) {
        String table = "erp_kingdee_production_" + kind + "_list_item";
        var source = jdbc.queryForMap("SELECT * FROM " + table + " WHERE tenant_id=1 AND production_order_no=?", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
        Object previous = MesFixedTestOrderDataSet.SECOND.document(source).get("production_material_list_no");
        var local = jdbc.queryForMap("SELECT * FROM " + table + " WHERE tenant_id=165");
        var raw = (ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(local.get("raw_payload").toString());
        var document = (ObjectNode) raw.get("document");
        if (previous == null) document.putNull("production_material_list_no"); else document.put("production_material_list_no", previous.toString());
        jdbc.update("UPDATE " + table + " SET production_material_list_no=?,raw_payload=? WHERE tenant_id=165", previous, raw.toString());
    }

    @Test
    void secondDataSetHasIndependentOrderAndCompleteDocumentAndBomReferences() {
        var service = dataReference();
        jdbc.update("UPDATE mes_pro_work_order SET batch_code='FIRST-BATCH' WHERE tenant_id=1");
        jdbc.update("UPDATE erp_kingdee_production_pick_list SET source_bill_no='PICK-001' WHERE tenant_id=1");
        jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET source_bill_no='PICK-001',production_material_list_no='MATERIAL-001' WHERE tenant_id=1 AND production_order_no=?", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
        jdbc.update("INSERT INTO mes_pro_work_order_bom(id,tenant_id,deleted,work_order_id,item_id,quantity) VALUES(600,1,0,300,11,1)");
        jdbc.update("UPDATE mes_kingdee_production_material_list SET work_order_bom_id=600 WHERE tenant_id=1");
        var tables = List.of("mes_pro_work_order", "mes_pro_work_order_bom", "erp_kingdee_production_pick_list",
                "erp_kingdee_production_pick_list_item", "erp_kingdee_production_replenishment_list",
                "erp_kingdee_production_replenishment_list_item", "mes_kingdee_production_material_list");
        var sourceBefore = tables.stream().map(table -> jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=1 ORDER BY id")).toList();
        TenantContextHolder.setTenantId(165L);
        try {
            // Data2 is usable even when the user has not clicked Data1 first.
            var second = tx.execute(s -> service.prepareSecond(77L));
            assertNotEquals(MesFixedTestOrderInitializer.WORK_ORDER_CODE, second.workOrderCode());
            assertTrue(second.workOrderCode().length() <= 64);
            assertEquals(List.of(1, 1, 1, 2), second.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::createdCount).toList());
            var order = jdbc.queryForMap("SELECT * FROM mes_pro_work_order WHERE tenant_id=165 AND code=?", second.workOrderCode());
            assertNotEquals("FIRST-BATCH", order.get("batch_code"));
            assertEquals(0, jdbc.queryForObject("SELECT quantity_produced FROM mes_pro_work_order WHERE id=?", Integer.class, order.get("id")));
            for (String kind : List.of("pick", "replenishment")) {
                String header = "erp_kingdee_production_" + kind + "_list";
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM " + header + "_item i JOIN " + header
                        + " h ON h.id=i.production_" + kind + "_list_id AND h.tenant_id=i.tenant_id"
                        + " WHERE i.tenant_id=165 AND i.production_order_no=? AND i.source_bill_no=h.source_bill_no", Integer.class, second.workOrderCode()));
            }
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_kingdee_production_material_list m JOIN mes_pro_work_order_bom b"
                    + " ON b.id=m.work_order_bom_id AND b.tenant_id=m.tenant_id AND b.work_order_id=m.work_order_id"
                    + " WHERE m.tenant_id=165 AND m.production_order_no=? AND m.work_order_id=? AND m.work_order_code=?", Integer.class,
                    second.workOrderCode(), order.get("id"), second.workOrderCode()));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item i WHERE i.tenant_id=165"
                    + " AND EXISTS (SELECT 1 FROM mes_kingdee_production_material_list m WHERE m.tenant_id=i.tenant_id"
                    + " AND m.source_bill_no=i.production_material_list_no AND m.production_order_no=i.production_order_no)", Integer.class));
            var raw = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(jdbc.queryForObject(
                    "SELECT raw_payload FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165", String.class));
            assertEquals(2, raw.path("testDataSet").asInt());
            assertEquals(second.workOrderCode(), raw.path("document").path("production_order_no").asText());
            assertEquals(1L, raw.path("reference").path("tenantId").asLong());
            var secondBefore = tables.stream().map(table -> jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=165 ORDER BY id")).toList();
            tx.executeWithoutResult(s -> service.prepare(88L));
            // First fixture creation and reset cannot reuse or remove the second fixture's identities.
            tx.executeWithoutResult(s -> service.rebuildForReset(88L));
            for (int i = 0; i < tables.size(); i++) {
                var current = jdbc.queryForList("SELECT * FROM " + tables.get(i) + " WHERE tenant_id=165 ORDER BY id");
                assertTrue(current.containsAll(secondBefore.get(i)), tables.get(i));
                assertEquals(sourceBefore.get(i), jdbc.queryForList("SELECT * FROM " + tables.get(i) + " WHERE tenant_id=1 ORDER BY id"));
            }
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_active_order WHERE tenant_id=165", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void secondDataSetIsIdempotentAndCanFillOnlyMissingLines() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            var first = tx.execute(s -> service.prepareSecond(77L));
            jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET actual_quantity=99 WHERE tenant_id=165");
            jdbc.update("UPDATE mes_pro_work_order SET quantity_produced=9,status='2' WHERE tenant_id=165");
            var before = jdbc.queryForList("SELECT * FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165");
            var repeated = tx.execute(s -> service.prepareSecond(88L));
            assertEquals(first.workOrderCode(), repeated.workOrderCode());
            assertEquals(List.of(0, 0, 0, 0), repeated.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::createdCount).toList());
            assertEquals(List.of(1, 1, 1, 2), repeated.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::skippedCount).toList());
            assertEquals(before, jdbc.queryForList("SELECT * FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165"));
            assertEquals(9, jdbc.queryForObject("SELECT quantity_produced FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            jdbc.update("DELETE FROM mes_kingdee_production_material_list WHERE id=(SELECT MIN(id) FROM mes_kingdee_production_material_list WHERE tenant_id=165)");
            var filled = tx.execute(s -> service.prepareSecond(88L));
            assertEquals(List.of(0, 0, 0, 1), filled.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::createdCount).toList());
            assertEquals(List.of(0, 0, 0, 0), filled.documents().stream().map(MesFixedTestOrderDataService.DocumentResult::deletedCount).toList());
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void secondDataSetCannotAdoptAnUnrelatedOrderOrReuseFirstDataSetAssociations() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> service.prepare(77L));
            tx.executeWithoutResult(s -> service.prepareSecond(77L));
            String second = MesFixedTestOrderDataSet.SECOND.workOrderCode();
            jdbc.update("UPDATE mes_pro_work_order SET remark='Ordinary order' WHERE tenant_id=165 AND code=?", second);
            var occupied = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> service.prepareSecond(77L)));
            assertTrue(occupied.getMessage().contains("已被其他订单占用"));
            jdbc.update("UPDATE mes_pro_work_order SET remark=? WHERE tenant_id=165 AND code=?", MARKER, second);

            Long firstHeader = jdbc.queryForObject("SELECT production_pick_list_id FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165 AND production_order_no=?", Long.class, MesFixedTestOrderInitializer.WORK_ORDER_CODE);
            Long secondHeader = jdbc.queryForObject("SELECT production_pick_list_id FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165 AND production_order_no=?", Long.class, second);
            jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET production_pick_list_id=? WHERE tenant_id=165 AND production_order_no=?", firstHeader, second);
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> service.prepareSecond(77L)));
            jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET production_pick_list_id=? WHERE tenant_id=165 AND production_order_no=?", secondHeader, second);

            Long firstOrder = jdbc.queryForObject("SELECT id FROM mes_pro_work_order WHERE tenant_id=165 AND code=?", Long.class, MesFixedTestOrderInitializer.WORK_ORDER_CODE);
            jdbc.update("UPDATE mes_kingdee_production_material_list SET work_order_id=? WHERE tenant_id=165 AND production_order_no=?", firstOrder, second);
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> service.prepareSecond(77L)));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=165", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165 AND production_order_no=? AND production_pick_list_id=?", Integer.class, MesFixedTestOrderInitializer.WORK_ORDER_CODE, firstHeader));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void secondDataSetFailureRollsBackAndReferenceTenantIsReadOnly() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(s -> service.prepare(77L));
            var firstOrder = jdbc.queryForMap("SELECT * FROM mes_pro_work_order WHERE tenant_id=165");
            jdbc.update("DELETE FROM erp_kingdee_production_replenishment_list_item WHERE tenant_id=1");
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> service.prepareSecond(77L)));
            assertEquals(List.of(firstOrder), jdbc.queryForList("SELECT * FROM mes_pro_work_order WHERE tenant_id=165"));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list WHERE tenant_id=165", Integer.class));
            TenantContextHolder.setTenantId(1L);
            var error = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> service.prepareSecond(77L)));
            assertTrue(error.getMessage().contains("参考租户只读"));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order WHERE tenant_id=1", Integer.class));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void initialDataAndResetEntryPointsCopyCompleteSourceFieldsAndResetRemovesExtraRows() {
        var service = dataReference();
        var initializer = new MesFixedTestOrderPickListInitializer(service);
        jdbc.update("UPDATE erp_kingdee_production_pick_list SET raw_payload='pick header payload' WHERE tenant_id=1");
        jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET raw_payload='pick line payload' WHERE tenant_id=1");
        jdbc.update("UPDATE erp_kingdee_production_replenishment_list SET raw_payload='replenishment payload' WHERE tenant_id=1");
        jdbc.update("UPDATE mes_kingdee_production_material_list SET raw_payload='material payload',required_quantity='12' WHERE tenant_id=1");
        TenantContextHolder.setTenantId(165L);
        try {
            List<Runnable> entries = List.of(() -> service.prepare(77L),
                    () -> initializer.prepareForReset(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 88L),
                    () -> initializer.prepare(MesFixedTestOrderInitializer.WORK_ORDER_CODE, 99L));
            Object firstPickId = null;
            for (Runnable entry : entries) {
                tx.executeWithoutResult(status -> entry.run());
                var pickId = jdbc.queryForObject("SELECT id FROM erp_kingdee_production_pick_list_item WHERE tenant_id=165", Long.class);
                if (firstPickId != null) assertEquals(firstPickId, pickId);
                firstPickId = pickId;
                for (String kind : List.of("pick", "replenishment")) {
                    String head = "erp_kingdee_production_" + kind + "_list";
                    String item = head + "_item";
                    var targetHead = jdbc.queryForMap("SELECT * FROM " + head + " WHERE tenant_id=165");
                    var sourceHead = jdbc.queryForMap("SELECT * FROM " + head + " WHERE tenant_id=1");
                    for (String field : MesFixedTestOrderDataService.HEADER_COLUMNS.split(","))
                        assertEquals(sourceHead.get(field), targetHead.get(field), head + "/" + field);
                    var targetItem = jdbc.queryForMap("SELECT * FROM " + item + " WHERE tenant_id=165");
                    var sourceItem = jdbc.queryForMap("SELECT * FROM " + item + " WHERE tenant_id=1 AND production_order_no=?", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
                    for (String field : MesFixedTestOrderDataService.ITEM_COLUMNS.split(","))
                        assertEquals(sourceItem.get(field), targetItem.get(field), item + "/" + field);
                }
                var sourceMaterials = jdbc.queryForList("SELECT * FROM mes_kingdee_production_material_list WHERE tenant_id=1 ORDER BY source_entry_id");
                var targetMaterials = jdbc.queryForList("SELECT * FROM mes_kingdee_production_material_list WHERE tenant_id=165 ORDER BY source_entry_id");
                assertEquals(sourceMaterials.size(), targetMaterials.size());
                for (int i = 0; i < sourceMaterials.size(); i++) {
                    for (String field : MesFixedTestOrderDataService.MATERIAL_COLUMNS.split(",")) {
                        if (!Set.of("work_order_id", "work_order_bom_id", "product_id", "child_material_id").contains(field))
                            assertEquals(sourceMaterials.get(i).get(field), targetMaterials.get(i).get(field), field);
                    }
                }
                jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET actual_quantity=99,raw_payload='edited' WHERE tenant_id=165");
                jdbc.update("UPDATE erp_kingdee_production_replenishment_list_item SET actual_quantity=99 WHERE tenant_id=165");
                jdbc.update("UPDATE mes_kingdee_production_material_list SET required_quantity='99' WHERE tenant_id=165");
                jdbc.update("INSERT INTO mes_kingdee_production_material_list(id,tenant_id,deleted,production_order_no,source_entry_id) VALUES(9999,165,0,?,'EXTRA')", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
            }
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void failureAfterDeletingExistingRowsRestoresAllThreeDocumentTypes() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(status -> service.prepare(77L));
            jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET actual_quantity=99 WHERE tenant_id=165");
            var tables = List.of("erp_kingdee_production_pick_list", "erp_kingdee_production_pick_list_item",
                    "erp_kingdee_production_replenishment_list", "erp_kingdee_production_replenishment_list_item",
                    "mes_kingdee_production_material_list");
            var before = tables.stream().map(table -> jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=165 ORDER BY id")).toList();
            jdbc.update("UPDATE mes_kingdee_production_material_list SET child_material_id=999999 WHERE tenant_id=1");
            assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(status -> service.rebuildForReset(88L)));
            for (int i = 0; i < tables.size(); i++)
                assertEquals(before.get(i), jdbc.queryForList("SELECT * FROM " + tables.get(i) + " WHERE tenant_id=165 ORDER BY id"));
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void rebuildPreservesSharedOtherOrderLinesAndRemovesObsoleteOrderDocuments() {
        var service = dataReference();
        TenantContextHolder.setTenantId(165L);
        try {
            tx.executeWithoutResult(status -> service.prepare(77L));
            var sharedHeader = jdbc.queryForObject("SELECT id FROM erp_kingdee_production_pick_list WHERE tenant_id=165", Long.class);
            jdbc.update("INSERT INTO erp_kingdee_production_pick_list_item(id,tenant_id,deleted,production_pick_list_id,source_line_key,production_order_no,actual_quantity) VALUES(9900,165,0,?,'OTHER-LOCAL','OTHER-ORDER',8)", sharedHeader);
            jdbc.update("INSERT INTO erp_kingdee_production_pick_list(id,tenant_id,deleted,source_form_id,source_fid) VALUES(9901,165,0,'PICK','OBSOLETE')");
            jdbc.update("INSERT INTO erp_kingdee_production_pick_list_item(id,tenant_id,deleted,production_pick_list_id,source_line_key,production_order_no) VALUES(9902,165,0,9901,'OBSOLETE-LINE',?)", MesFixedTestOrderInitializer.WORK_ORDER_CODE);
            var otherLine = jdbc.queryForMap("SELECT * FROM erp_kingdee_production_pick_list_item WHERE id=9900");
            tx.executeWithoutResult(status -> service.rebuildForReset(88L));
            assertEquals(otherLine, jdbc.queryForMap("SELECT * FROM erp_kingdee_production_pick_list_item WHERE id=9900"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list WHERE id=9901", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list_item WHERE id=9902", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_production_pick_list WHERE id=? AND tenant_id=165", Integer.class, sharedHeader));
        } finally { TenantContextHolder.clear(); }
    }

    private MesFixedTestOrderDataService dataReference() {
        fullReference();
        replenishmentReference();
        for (String table : List.of("erp_kingdee_production_pick_list", "erp_kingdee_production_pick_list_item", "erp_kingdee_production_replenishment_list", "erp_kingdee_production_replenishment_list_item"))
            jdbc.execute("ALTER TABLE " + table + " ADD raw_payload VARCHAR(2000)");
        List<String> columns = new ArrayList<>(List.of("id BIGINT PRIMARY KEY", "tenant_id BIGINT", "deleted INT", "creator VARCHAR(80)", "updater VARCHAR(80)"));
        for (String field : MesFixedTestOrderDataService.MATERIAL_COLUMNS.split(","))
            columns.add(field + (Set.of("work_order_id", "work_order_bom_id", "product_id", "child_material_id").contains(field) ? " BIGINT" : " VARCHAR(2000)"));
        jdbc.execute("CREATE TABLE mes_kingdee_production_material_list (" + String.join(",", columns) + ")");
        jdbc.execute("CREATE TABLE mes_pro_process_pool_active_order (id BIGINT,tenant_id BIGINT,work_order_id BIGINT,deleted INT DEFAULT 0)");
        for (int i = 1; i <= 2; i++) jdbc.update("INSERT INTO mes_kingdee_production_material_list(id,tenant_id,deleted,source_form_id,source_bill_no,source_entry_id,production_order_no,work_order_id,work_order_code,product_id,child_material_id) VALUES(?,1,0,'PRD_PPBOM','MATERIAL-001',?,?,300,?,10,11)", 500+i, "ENTRY-"+i, MesFixedTestOrderInitializer.WORK_ORDER_CODE, MesFixedTestOrderInitializer.WORK_ORDER_CODE);
        var service = new MesFixedTestOrderDataService(jdbc, mock(MesFixedTestOrderModeGuard.class), new MesFixedTestOrderInitializer(jdbc, canonicalizer, controlledContentAdapter));
        ReflectionTestUtils.setField(service, "actionLog", mock(MesActiveOrderActionLog.class));
        return service;
    }

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
