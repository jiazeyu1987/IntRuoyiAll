package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DccProjectCodeListQueryTest {
    @Test
    void countAndPageShareDerivedFiltersAndBoundValuesBeforeLimit() {
        var request = new DccProjectCodePageReqVO();
        request.setPageNo(3);
        request.setRouteConfigured(true);
        request.setMainBatchRecordConfigured(false);
        request.setQaRegulationConfigured(true);
        request.setRequireDccProductCode(true);
        request.setFileCountSort("desc");
        request.setListFilters("[{\"key\":\"associatedFileCount\",\"operator\":\"eq\",\"value\":12},"
                + "{\"key\":\"routeStatus\",\"operator\":\"contains\",\"value\":\"A%_\"},"
                + "{\"key\":\"qaRegulationStatus\",\"operator\":\"contains\",\"value\":\"已发布\"}]");
        var wrapper = DccProjectCodeListQuery.filter(request, List.of(41L, 43L), true, true);
        var parameters = new HashMap<String, Object>();
        parameters.put("tenantId", 9L);
        parameters.put("req", request);
        parameters.put("ew", wrapper);
        parameters.put("offset", 20L);
        var config = new Configuration();
        String count = bind(config, DccProjectCodeListQuery.count(), parameters);
        String page = bind(config, DccProjectCodeListQuery.page(), parameters);
        assertEquals(count.substring(count.indexOf(" FROM (")).trim(),
                page.substring(page.indexOf(" FROM ("), page.indexOf(" ORDER BY ", page.indexOf(") q WHERE"))).trim());
        assertTrue(count.contains("q.id IN"));
        assertTrue(count.contains("q.status = ?"));
        assertTrue(count.contains("p.tenant_id=?"));
        assertTrue(page.contains("q.associated_file_count DESC"));
        assertTrue(page.contains("LIMIT ? OFFSET ?"));
        assertFalse(count.contains("LIMIT ? OFFSET ?"));
        assertFalse(page.contains("A%_"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%a!%!_%"));
    }

    @Test
    void qaPermissionUsesSameDeniedDisplayAndExportRetainsFilterWithoutLimit() {
        var request = new DccProjectCodePageReqVO();
        request.setPageSize(-1);
        request.setListFilters("[{\"key\":\"qaRegulationStatus\",\"operator\":\"eq\",\"value\":\"无查询权限\"}]");
        var filter = DccProjectCodeListQuery.filter(request, List.of(), false, false);
        assertTrue(filter.getSqlSegment().contains("无查询权限"));
        assertFalse(filter.getSqlSegment().contains("q.qa_display"));
        assertTrue(filter.getSqlSegment().contains("1 = 0"));
        Map<String, Object> parameters = Map.of("tenantId", 9L, "req", request, "ew", filter, "offset", 0L);
        assertFalse(bind(new Configuration(), DccProjectCodeListQuery.page(), parameters).contains("LIMIT ? OFFSET ?"));
    }

    @Test
    void formalBindingDrivesDisplayVersionAndConfiguredFilter() throws Exception {
        try (var db = java.sql.DriverManager.getConnection("jdbc:h2:mem:formal_project_route;MODE=MySQL;DB_CLOSE_DELAY=-1")) {
            var statement = db.createStatement();
            for (String ddl : List.of(
                    "CREATE TABLE dcc_project_code(id BIGINT,tenant_id BIGINT,deleted INT,status VARCHAR,library_scope VARCHAR,project_name VARCHAR,project_code VARCHAR,product_master_id BIGINT,doc_control_no VARCHAR,"
                            + "category VARCHAR,commissioned_production BOOLEAN,project_leader VARCHAR,project_leader_user_id BIGINT,default_attributes_json VARCHAR,project_engineer VARCHAR,storage_location VARCHAR,priority VARCHAR,last_import_batch_id BIGINT,batch_record_total_recognition_json VARCHAR,create_time TIMESTAMP,update_time TIMESTAMP,creator VARCHAR,updater VARCHAR)",
                    "CREATE TABLE mes_pro_route(id BIGINT,tenant_id BIGINT,deleted INT)",
                    "CREATE TABLE mes_pro_route_dcc_project_binding(id BIGINT,route_id BIGINT,dcc_project_code_id BIGINT,tenant_id BIGINT,deleted INT)",
                    "CREATE TABLE mes_pro_route_version(id BIGINT,route_id BIGINT,tenant_id BIGINT,deleted INT,active INT,lifecycle_status VARCHAR,version_no VARCHAR)",
                    "CREATE TABLE mes_pro_batch_record_definition(id BIGINT,tenant_id BIGINT,deleted INT,batch_record_name VARCHAR,current_version_id BIGINT,route_key VARCHAR)",
                    "CREATE TABLE mes_pro_batch_record_report(id BIGINT,tenant_id BIGINT,deleted INT,batch_record_definition_id BIGINT,form_slot_type VARCHAR)",
                    "CREATE TABLE mes_pro_batch_record_version(id BIGINT,tenant_id BIGINT,deleted INT,version_no VARCHAR)",
                    "CREATE TABLE mes_qa_inspection_regulation(id BIGINT,tenant_id BIGINT,deleted INT,owner_module VARCHAR,dcc_project_code_id BIGINT)",
                    "CREATE TABLE mes_qa_inspection_regulation_version(id BIGINT,regulation_id BIGINT,tenant_id BIGINT,deleted INT,lifecycle_status VARCHAR,version_no VARCHAR,published_at TIMESTAMP)",
                    "CREATE TABLE mdm_product(id BIGINT,tenant_id BIGINT,deleted INT,status VARCHAR,dcc_product_code VARCHAR)",
                    "CREATE TABLE dcc_controlled_file(id BIGINT,tenant_id BIGINT,deleted INT,dcc_project_code_id BIGINT)",
                    "CREATE TABLE dcc_controlled_file_recognition_record(id BIGINT,controlled_file_id BIGINT,tenant_id BIGINT,deleted INT,status VARCHAR,matched_project_code_id BIGINT)",
                    "CREATE TABLE dcc_registration_certificate_file(id BIGINT,tenant_id BIGINT,deleted INT,status VARCHAR,owner_type VARCHAR,owner_id BIGINT)",
                    "CREATE TABLE dcc_registration_certificate_version(id BIGINT,tenant_id BIGINT,deleted INT,certificate_id BIGINT)",
                    "CREATE TABLE dcc_registration_certificate_change(id BIGINT,tenant_id BIGINT,deleted INT,certificate_id BIGINT)",
                    "CREATE TABLE dcc_registration_certificate_product(tenant_id BIGINT,certificate_id BIGINT,project_code_id BIGINT,state VARCHAR)",
                    "CREATE TABLE dcc_product_catalog(id BIGINT,tenant_id BIGINT,deleted INT,product_code VARCHAR)",
                    "CREATE TABLE dcc_data_relation(id BIGINT,tenant_id BIGINT,deleted INT,project_code_id BIGINT,product_catalog_id BIGINT,relation_status VARCHAR)",
                    "CREATE TABLE dcc_project_product_relation(id BIGINT,tenant_id BIGINT,deleted INT,project_code_id BIGINT,product_catalog_id BIGINT,request_id BIGINT,relation_status VARCHAR)",
                    "CREATE TABLE dcc_project_product_create_request(id BIGINT,tenant_id BIGINT,deleted INT,status VARCHAR,generated_project_code_id BIGINT,generated_product_catalog_id BIGINT,relation_id BIGINT)")) {
                statement.execute(ddl);
            }
            statement.execute("INSERT INTO dcc_project_code(id,tenant_id,deleted,status,library_scope,project_name,project_code,product_master_id,doc_control_no) VALUES (1,9,0,'ENABLE','PROJECT','数显压力泵','IDE',NULL,'1'),(2,9,0,'ENABLE','PROJECT','未绑定项目','NONE',NULL,'2')");
            statement.execute("INSERT INTO mes_pro_route VALUES (40,9,0),(41,9,0),(42,9,1)");
            // Deleted and foreign-tenant bindings must not configure the second project.
            statement.execute("INSERT INTO mes_pro_route_dcc_project_binding VALUES (1,40,1,9,0),(2,41,2,9,1),(3,41,2,10,0),(4,42,2,9,0)");
            statement.execute("INSERT INTO mes_pro_route_version VALUES (50,40,9,0,1,'ACTIVE','V3'),(51,40,9,0,0,'DRAFT','V4')");
            var request = new DccProjectCodePageReqVO();
            request.setPageSize(-1);
            var parameters = new HashMap<String, Object>();
            parameters.put("tenantId", 9L);
            parameters.put("req", request);
            parameters.put("offset", 0L);
            statement.execute("INSERT INTO mes_qa_inspection_regulation VALUES (60,9,0,'MES_QA',1)");
            for (String column : List.of("routeStatus", "qaRegulationStatus")) {
                for (String direction : List.of("asc", "desc")) {
                    request.setRouteStatusSort(column.equals("routeStatus") ? direction : null);
                    request.setQaRegulationStatusSort(column.equals("qaRegulationStatus") ? direction : null);
                    request.setPageSize(1);
                    parameters.put("ew", DccProjectCodeListQuery.filter(request, null, false, true));
                    for (long offset : List.of(0L, 1L)) {
                        parameters.put("offset", offset);
                        try (var rows = execute(db, DccProjectCodeListQuery.page(), parameters)) {
                            assertTrue(rows.next());
                            long expected = direction.equals("asc") ? offset + 1 : 2 - offset;
                            assertEquals(expected, rows.getLong("id"), column + " " + direction + " page " + offset);
                            assertFalse(rows.next());
                        }
                    }
                }
            }
            request.setRouteStatusSort(null);
            request.setQaRegulationStatusSort(null);
            request.setPageSize(-1);
            parameters.put("offset", 0L);
            for (Boolean configured : List.of(true, false)) {
                request.setRouteConfigured(configured);
                parameters.put("ew", DccProjectCodeListQuery.filter(request, null, false, true));
                try (var rows = execute(db, DccProjectCodeListQuery.page(), parameters)) {
                    assertTrue(rows.next());
                    assertEquals(configured ? 1L : 2L, rows.getLong("id"));
                    assertEquals(configured ? "已配置 V3" : "未配置 ", rows.getString("route_display"));
                    assertFalse(rows.next());
                }
                try (var rows = execute(db, DccProjectCodeListQuery.count(), parameters)) {
                    assertTrue(rows.next());
                    assertEquals(1, rows.getInt(1));
                }
            }
            request.setRouteConfigured(null);
            statement.execute("UPDATE dcc_project_code SET project_leader=CASE id WHEN 1 THEN '张三' ELSE '李四' END");
            statement.execute("INSERT INTO dcc_product_catalog VALUES (100,9,0,'PRODUCT-A'),(101,9,0,'PRODUCT-AB'),(102,9,1,'DELETED'),(103,10,0,'FOREIGN'),(104,9,0,'UNCONFIRMED'),(105,9,0,'INCOMPLETE')");
            statement.execute("INSERT INTO dcc_data_relation VALUES (1,9,0,1,100,'CONFIRMED'),(2,9,1,2,100,'CONFIRMED'),(3,9,0,1,102,'CONFIRMED'),(4,10,0,1,103,'CONFIRMED'),(5,9,0,1,104,'PENDING')");
            statement.execute("INSERT INTO dcc_project_product_relation VALUES (10,9,0,2,101,20,'ACTIVE'),(11,9,0,1,105,21,'ACTIVE')");
            statement.execute("INSERT INTO dcc_project_product_create_request VALUES (20,9,0,'COMPLETED',2,101,10),(21,9,0,'WRITING',1,105,11)");
            assertProjectFilters(db, request, parameters, "[{\"key\":\"productCode\",\"operator\":\"contains\",\"value\":\"PRODUCT-A\"}]", List.of(1L, 2L));
            assertProjectFilters(db, request, parameters, "[{\"key\":\"productCode\",\"operator\":\"eq\",\"value\":\"PRODUCT-A\"}]", List.of(1L));
            assertProjectFilters(db, request, parameters, "[{\"key\":\"projectLeader\",\"operator\":\"contains\",\"value\":\"李\"}]", List.of(2L));
            assertProjectFilters(db, request, parameters, "[{\"key\":\"productCode\",\"operator\":\"eq\",\"value\":\"PRODUCT-AB\"},{\"key\":\"projectLeader\",\"operator\":\"eq\",\"value\":\"张三\"}]", List.of());
            for (String unavailable : List.of("DELETED", "FOREIGN", "UNCONFIRMED", "INCOMPLETE", "IDE")) {
                assertProjectFilters(db, request, parameters, "[{\"key\":\"productCode\",\"operator\":\"contains\",\"value\":\"" + unavailable + "\"}]", List.of());
            }
            request.setListFilters(null);
            request.setProductCode("PRODUCT-A");
            request.setProjectLeader("李");
            assertProjectFilters(db, request, parameters, null, List.of(2L));
            request.setProjectLeader(null);
            request.setPageSize(1);
            assertProjectFilters(db, request, parameters, null, List.of(1L), 2);
        }
    }

    private static void assertProjectFilters(java.sql.Connection db, DccProjectCodePageReqVO request,
            Map<String, Object> parameters, String filters, List<Long> expected) throws Exception {
        assertProjectFilters(db, request, parameters, filters, expected, expected.size());
    }

    private static void assertProjectFilters(java.sql.Connection db, DccProjectCodePageReqVO request,
            Map<String, Object> parameters, String filters, List<Long> expected, int total) throws Exception {
        request.setListFilters(filters);
        parameters.put("ew", DccProjectCodeListQuery.filter(request, null, false, true));
        try (var rows = execute(db, DccProjectCodeListQuery.page(), parameters)) {
            var actual = new java.util.ArrayList<Long>();
            while (rows.next()) actual.add(rows.getLong("id"));
            assertEquals(expected, actual, filters);
        }
        try (var rows = execute(db, DccProjectCodeListQuery.count(), parameters)) {
            assertTrue(rows.next());
            assertEquals(total, rows.getInt(1), filters);
        }
    }

    private static java.sql.ResultSet execute(java.sql.Connection db, String script,
                                              Map<String, Object> parameters) throws Exception {
        var configuration = new Configuration();
        var bound = new XMLLanguageDriver().createSqlSource(configuration, script, Map.class).getBoundSql(parameters);
        // MySQL CHAR is unbounded here; H2 defaults it to one character.
        var statement = db.prepareStatement(bound.getSql().replace(" AS CHAR)", " AS VARCHAR)"));
        var values = configuration.newMetaObject(parameters);
        for (int i = 0; i < bound.getParameterMappings().size(); i++) {
            statement.setObject(i + 1, values.getValue(bound.getParameterMappings().get(i).getProperty()));
        }
        return statement.executeQuery();
    }

    private static String bind(Configuration config, String script, Map<String, Object> parameters) {
        var bound = new XMLLanguageDriver().createSqlSource(config, script, Map.class).getBoundSql(parameters);
        bound.getParameterMappings().stream().filter(mapping -> mapping.getProperty().startsWith("ew."))
                .forEach(mapping -> assertNotNull(config.newMetaObject(parameters).getValue(mapping.getProperty())));
        return bound.getSql();
    }
}
