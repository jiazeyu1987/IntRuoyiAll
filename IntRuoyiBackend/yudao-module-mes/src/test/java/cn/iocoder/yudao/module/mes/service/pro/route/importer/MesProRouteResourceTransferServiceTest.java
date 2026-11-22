package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.alibaba.fastjson.*;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MesProRouteResourceTransferServiceTest {
    private final MesProRouteResourceTransferService service = new MesProRouteResourceTransferService();
    @Test void personnelFreeWorkbookImportsIntoTenantWithoutSourceUsers() throws Exception {
        Map<String, JSONObject> source = new LinkedHashMap<>();
        source.put("mes_pro_route:101", resource("mes_pro_route", "{\"id\":101,\"code\":\"ROUTE\",\"remark\":\"[owner]source-person[/owner]工艺备注\"}"));
        source.put("mes_pro_process:301", resource("mes_pro_process", "{\"id\":301,\"code\":\"PROCESS\",\"name\":\"清洗\"}"));
        source.put("mes_pro_route_process:401", resource("mes_pro_route_process", "{\"id\":401,\"route_id\":101,\"process_id\":301,\"sort\":1}"));
        source.put("mes_pro_route_dcc_project_binding:701", resource("mes_pro_route_dcc_project_binding", "{\"id\":701,\"route_id\":101,\"dcc_project_code_id\":801}"));
        source.put("dcc_project_code:801", resource("dcc_project_code", "{\"id\":801,\"project_code\":\"PROJECT\"}"));
        source.put("mes_pro_process_pool_team_device:501", resource("mes_pro_process_pool_team_device", "{\"id\":501,\"device_code\":\"DEVICE\",\"device_name\":\"清洗机\",\"enabled\":true}"));
        source.put("mes_pro_craft:601", resource("mes_pro_craft", "{\"id\":601,\"code\":\"CRAFT\",\"name\":\"清洗工艺\",\"process_id\":301,\"parameters_json\":\"[]\"}"));
        source.get("mes_pro_craft:601").put("parameters_json", "[{\"parameterCode\":\"TEMP\",\"parameterName\":\"温度\",\"standardText\":\"保持温度\",\"valueType\":\"DECIMAL\",\"lowerLimit\":10,\"upperLimit\":50}]");
        JSONObject version = resource("mes_pro_route_version", "{\"id\":201,\"route_id\":101,\"active\":true,\"lifecycle_status\":\"ACTIVE\",\"route_snapshot_format_version\":\"MES_ROUTE_SNAPSHOT_CANONICAL_V1\"}");
        version.put("route_snapshot_json", "{\"routeId\":101,\"routeVersionId\":201,\"remark\":\"[owner]source-person[/owner]备注\",\"configSnapshots\":{\"flowGraph\":{\"nodes\":[{\"routeProcessId\":401,\"processId\":301,\"sort\":1}]},\"routeStartProductionLeaders\":[{\"productionLineId\":101,\"candidateSourceType\":\"USERS\",\"candidateSourceIds\":[999],\"candidateSourceNames\":[\"source-person\"]}],\"batchUseConfigs\":[],\"productionProcessConfigs\":[{\"routeProcessId\":401,\"processId\":301,\"deviceIds\":[501],\"parameterRules\":[]}]}}");
        source.put("mes_pro_route_version:201", version);
        var exportJdbc = org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class, call -> {
            if (!call.getMethod().getName().equals("queryForList")) return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
            String sql = call.getArgument(0);
            assertFalse(sql.contains("system_users"));
            String table = sql.substring(sql.indexOf(" FROM ") + 6, sql.indexOf(" WHERE "));
            Object[] args = (Object[]) call.getRawArguments()[sql.contains("WHERE id=?") ? 1 : 2];
            if (sql.contains("WHERE id=?")) return List.of(new LinkedHashMap<>(source.get(table + ":" + args[0])));
            String field = sql.substring(sql.indexOf(" WHERE ") + 7, sql.indexOf("=?"));
            return source.entrySet().stream().filter(e -> e.getKey().startsWith(table + ":")
                    && Objects.equals(e.getValue().getLong(field), args[0])).map(e -> e.getValue().getLong("id")).toList();
        });
        org.springframework.test.util.ReflectionTestUtils.setField(service, "jdbc", exportJdbc);
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(186L);
        try (Workbook workbook = new XSSFWorkbook(); var bytes = new java.io.ByteArrayOutputStream()) {
            service.writeResources(workbook, List.of(101L)); workbook.write(bytes);
            try (Workbook reopened = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes.toByteArray()));
                 var security = org.mockito.Mockito.mockStatic(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.class)) {
                var restored = service.readResources(reopened);
                String payload = JSON.toJSONString(restored);
                for (String forbidden : List.of("source-person", "candidateSource", "system_users", "leader_user_id", "[owner]")) assertFalse(payload.contains(forbidden), forbidden);
                assertEquals("清洗工艺", restored.get("mes_pro_craft:601").getString("name"));
                assertEquals("清洗机", restored.get("mes_pro_process_pool_team_device:501").getString("device_name"));
                assertNotNull(restored.get("mes_pro_route_version:201").getString("route_snapshot_sha256"));
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(203L);
                security.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserId).thenReturn(12345L);
                List<String> inserted = new ArrayList<>();
                var targetJdbc = org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class, call -> {
                    String method = call.getMethod().getName();
                    if (method.equals("queryForList")) { assertFalse(((String) call.getArgument(0)).contains("system_users")); return List.of(); }
                    if (method.equals("update")) {
                        String sql = call.getArgument(0); inserted.add(sql);
                        if (sql.startsWith("INSERT INTO mes_pro_process_pool_team_device ")) {
                            List<String> columns = List.of(sql.substring(sql.indexOf('(') + 1, sql.indexOf(')')).split(","));
                            assertEquals(12345L, ((Object[]) call.getRawArguments()[1])[columns.indexOf("leader_user_id")]);
                        }
                        return 1;
                    }
                    return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
                });
                var importer = new MesProRouteResourceImportService();
                org.springframework.test.util.ReflectionTestUtils.setField(importer, "reader", service);
                org.springframework.test.util.ReflectionTestUtils.setField(importer, "jdbc", targetJdbc);
                org.springframework.test.util.ReflectionTestUtils.setField(importer, "owners", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionService.class));
                org.springframework.test.util.ReflectionTestUtils.setField(importer, "platform", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteControlledContentAdapter.class));
                org.springframework.test.util.ReflectionTestUtils.setField(importer, "versions", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper.class));
                assertEquals(1, importer.importWorkbook(reopened).getRouteCount());
                assertEquals(source.size(), inserted.size());
            }
        } finally { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }
    }
    private static JSONObject resource(String table, String values) {
        JSONObject row = new JSONObject(true);
        MesProRouteTransferSchema.TABLES.get(table).columns().forEach(column -> row.put(column, null));
        row.putAll(JSON.parseObject(values));
        return row;
    }
    @Test void clearsPersonnelInsideEncodedJsonAndKeepsTechnicalValues() {
        JSONObject row = JSON.parseObject("{\"default_attributes_json\":\"{\\\"projectLeaderUserId\\\":999,\\\"model\\\":\\\"M1\\\"}\",\"config\":{\"candidateSourceType\":\"ROLE\",\"candidateSourceIds\":[999],\"candidateSourceNames\":[\"source-person\"],\"fillableScopeJson\":\"{\\\"userId\\\":999}\",\"permissionScopeId\":999,\"ownerRoleKey\":\"PRODUCTION\",\"parameterRules\":[{\"defaultValue\":999}]}}");
        MesProRouteTransferPersonnel.remove(row);
        assertEquals("{\"model\":\"M1\"}", row.getString("default_attributes_json"));
        JSONObject config = row.getJSONObject("config");
        assertEquals(Set.of("ownerRoleKey", "parameterRules"), config.keySet());
        assertEquals(999, config.getJSONArray("parameterRules").getJSONObject(0).getIntValue("defaultValue"));
    }
    @Test void rejectsOldPersonnelBearingFormatAndInjectedPersonnel() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = sheet(workbook);
            sheet.getRow(0).getCell(0).setCellValue("MES_ROUTE_RESOURCES_V1");
            assertTrue(assertThrows(ServiceException.class, () -> service.readResources(workbook)).getMessage().contains("重新导出"));
            sheet.getRow(0).getCell(0).setCellValue(MesProRouteResourceTransferService.FORMAT);
            JSONObject row = process(); row.put("remark", "[owner]source-person[/owner]");
            fragment(sheet, 1, 0, JSON.toJSONString(row, SerializerFeature.WriteMapNullValue));
            assertThrows(ServiceException.class, () -> service.readResources(workbook));
        }
    }
    @Test void importedProcessKeyedConfigurationsRemapBothKeysAndValues() {
        for (String collection : List.of("scheduleConfigs", "batchUseConfigs", "scheduleUseConfigs", "productionProcessConfigs")) {
            String source = "{\"configSnapshots\":{\"" + collection + "\":{\"11\":{\"routeProcessId\":11,\"capacityMode\":\"RESOURCE_CALCULATED\",\"remark\":null}}}}";
            JSONObject result = JSON.parseObject(MesProRouteResourceImportService.rewriteJson(source,
                Map.of("mes_pro_route_process:11", 111L)));
            JSONObject configs = result.getJSONObject("configSnapshots").getJSONObject(collection);
            assertEquals(Set.of("111"), configs.keySet(), collection);
            assertEquals(111L, configs.getJSONObject("111").getLong("routeProcessId"));
            assertTrue(configs.getJSONObject("111").containsKey("remark"));
        }
    }
    @Test void inconsistentOrUnmappedProcessKeysFailRatherThanFreezeAnInvalidSnapshot() {
        assertThrows(ServiceException.class, () -> MesProRouteResourceImportService.rewriteJson(
            "{\"scheduleConfigs\":{\"11\":{\"routeProcessId\":12}}}",
            Map.of("mes_pro_route_process:11",111L,"mes_pro_route_process:12",112L)));
        assertThrows(ServiceException.class, () -> MesProRouteResourceImportService.rewriteJson(
            "{\"scheduleConfigs\":{\"12\":{\"routeProcessId\":12}}}", Map.of("mes_pro_route_process:11",111L)));
        assertThrows(ServiceException.class, () -> MesProRouteResourceImportService.rewriteJson(
            "{\"scheduleConfigs\":{\"11\":{\"routeProcessId\":11},\"12\":{\"routeProcessId\":12}}}",
            Map.of("mes_pro_route_process:11",111L,"mes_pro_route_process:12",111L)));
    }
    @Test void arrayConfigurationsAndBusinessCodeObjectKeysKeepTheirDeclaredMeaning() {
        JSONObject result = JSON.parseObject(MesProRouteResourceImportService.rewriteJson(
            "{\"scheduleConfigs\":[{\"routeProcessId\":11}],\"labels\":{\"11\":\"business label\"}}",
            Map.of("mes_pro_route_process:11",111L)));
        assertEquals(111L, result.getJSONArray("scheduleConfigs").getJSONObject(0).getLong("routeProcessId"));
        assertEquals("business label", result.getJSONObject("labels").getString("11"));
    }
    @Test void existingJsonResourcesCompareByContentWhileRetainingExplicitNulls() {
        assertTrue(MesProRouteResourceImportService.equalValue("parameters_json", "{\"a\":1,\"b\":null}", "{\"b\":null,\"a\":1}"));
        assertFalse(MesProRouteResourceImportService.equalValue("parameters_json", "{\"a\":1}", "{\"a\":1,\"b\":null}"));
        assertFalse(MesProRouteResourceImportService.equalValue("description", "{\"a\":1,\"b\":2}", "{\"b\":2,\"a\":1}"));
    }
    @Test void jsonRewritePreservesExplicitNullAttributesAndNestedValues() {
        String source = "{\"processId\":11,\"otherMarket\":null,\"nested\":{\"transferTo\":null},\"items\":[{\"libraryScope\":null}]}";
        JSONObject result = JSON.parseObject(MesProRouteResourceImportService.rewriteJson(source, Map.of("mes_pro_process:11", 111L)));
        assertEquals(111L, result.getLong("processId"));
        assertTrue(result.containsKey("otherMarket")); assertNull(result.get("otherMarket"));
        assertTrue(result.getJSONObject("nested").containsKey("transferTo"));
        assertTrue(result.getJSONArray("items").getJSONObject(0).containsKey("libraryScope"));
    }
    @Test void jdbcDateTimesRemainSqlDateTimesInsteadOfEpochNumbersInJson() {
        var date = java.time.LocalDateTime.of(2026, 11, 12, 14, 38, 49);
        JSONObject row = new JSONObject();
        row.put("status_changed_at", MesProRouteResourceTransferService.sqlValue(date));
        row.put("last_check_time", MesProRouteResourceTransferService.sqlValue(java.sql.Timestamp.valueOf(date)));
        JSONObject read = JSON.parseObject(JSON.toJSONString(row));
        assertEquals("2026-11-12T14:38:49", read.getString("status_changed_at"));
        assertEquals(read.getString("status_changed_at"), read.getString("last_check_time"));
        assertEquals("2026-11-12", MesProRouteResourceTransferService.sqlValue(java.sql.Date.valueOf(date.toLocalDate())));
    }
    @Test void databaseNullColumnsSurviveActualExportAndWorkbookReadback() throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        for (String column : MesProRouteTransferSchema.TABLES.get("mes_pro_route").columns()) row.put(column, null);
        row.put("id", 101L); row.put("code", "ROUTE-001"); row.put("name", "流程"); row.put("status", 0);
        var jdbc = org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class, call -> {
            if (call.getMethod().getName().equals("queryForList"))
                return ((String) call.getArgument(0)).contains("WHERE id=?") ? List.of(row) : List.of();
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
        });
        org.springframework.test.util.ReflectionTestUtils.setField(service, "jdbc", jdbc);
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(186L);
        try (Workbook workbook = new XSSFWorkbook(); var bytes = new java.io.ByteArrayOutputStream()) {
            service.writeResources(workbook, List.of(101L)); workbook.write(bytes);
            try (Workbook reopened = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
                var restored = service.readResources(reopened).get("mes_pro_route:101");
                assertEquals(row.keySet(), restored.keySet()); assertTrue(restored.containsKey("description"));
                assertNull(restored.get("description")); assertEquals("ROUTE-001", restored.getString("code"));
            }
        } finally { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }
    }
    @Test void objectSnapshotCollectionsSurviveActualExportAndWorkbookReadback() throws Exception {
        Map<String, Object> route = new LinkedHashMap<>();
        for (String column : MesProRouteTransferSchema.TABLES.get("mes_pro_route").columns()) route.put(column, null);
        route.put("id", 101L); route.put("code", "ROUTE-001");
        Map<String, Object> version = new LinkedHashMap<>();
        for (String column : MesProRouteTransferSchema.TABLES.get("mes_pro_route_version").columns()) version.put(column, null);
        version.put("id", 201L); version.put("route_id", 101L);
        version.put("route_snapshot_json", "{\"configSnapshots\":{\"products\":{},\"productBoms\":{},\"scheduleConfigs\":{}}}");
        var jdbc = org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class, call -> {
            if (call.getMethod().getName().equals("queryForList")) {
                String sql = call.getArgument(0);
                if (sql.contains("WHERE id=?")) return List.of(sql.contains("FROM mes_pro_route_version ") ? version : route);
                if (sql.contains("FROM mes_pro_route_version ")) return List.of(201L);
                return List.of();
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
        });
        org.springframework.test.util.ReflectionTestUtils.setField(service, "jdbc", jdbc);
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(186L);
        try (Workbook workbook = new XSSFWorkbook(); var bytes = new java.io.ByteArrayOutputStream()) {
            service.writeResources(workbook, List.of(101L));
            workbook.write(bytes);
            try (Workbook reopened = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
                JSONObject restored = service.readResources(reopened).get("mes_pro_route_version:201");
                JSONObject configs = JSON.parseObject(restored.getString("route_snapshot_json")).getJSONObject("configSnapshots");
                for (String key : List.of("products", "productBoms", "scheduleConfigs")) assertTrue(configs.getJSONObject(key).isEmpty());
                assertNotNull(restored.getString("route_snapshot_sha256"));
            }
        } finally { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }
    }
    private static JSONObject process() {
        JSONObject row = new JSONObject(true);
        for (String column : MesProRouteTransferSchema.TABLES.get("mes_pro_process").columns()) row.put(column, null);
        row.put("id", 101L); row.put("code", "GX-001"); row.put("name", "清洗"); return row;
    }
    private static Sheet sheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet(MesProRouteResourceTransferService.SHEET);
        sheet.createRow(0).createCell(0).setCellValue(MesProRouteResourceTransferService.FORMAT); return sheet;
    }
    private static void fragment(Sheet sheet, int row, int part, String text) {
        Row value = sheet.createRow(row); value.createCell(0).setCellValue("mes_pro_process");
        value.createCell(1).setCellValue("101"); value.createCell(2).setCellValue(part); value.createCell(3).setCellValue(text);
    }
    @Test void preservesNullsAndLongPayloadAcrossExcelFragments() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            JSONObject source = process(); source.put("attention", "工艺要求".repeat(12000));
            String json = JSON.toJSONString(source, SerializerFeature.WriteMapNullValue); Sheet sheet = sheet(workbook);
            for (int i = 0, part = 0; i < json.length(); i += 16000, part++) fragment(sheet, part + 1, part, json.substring(i, Math.min(i + 16000, json.length())));
            var result = service.readResources(workbook); assertEquals(JSON.parseObject(json), result.get("mes_pro_process:101"));
            assertTrue(result.get("mes_pro_process:101").containsKey("sop_file_url"));
        }
    }
    @Test void rejectsMissingFragmentBeforeImport() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) { fragment(sheet(workbook), 1, 1, "{}"); assertThrows(ServiceException.class, () -> service.readResources(workbook)); }
    }
    @Test void rejectsTenantInjectionInPayload() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            JSONObject row = process(); row.put("tenant_id", 186); fragment(sheet(workbook), 1, 0, JSON.toJSONString(row, SerializerFeature.WriteMapNullValue));
            assertThrows(ServiceException.class, () -> service.readResources(workbook));
        }
    }
    @Test void rewritesProcessWorkstationDeviceAndMaterialReferencesIndependently() {
        JSONObject config = JSON.parseObject("{\"processId\":11,\"routeProcessId\":12,\"workstationId\":13,\"deviceIds\":[14],\"inputMaterialIds\":[15],\"parameterRules\":[{\"deviceId\":14,\"processId\":11,\"routeProcessId\":12,\"defaultValue\":14}]}");
        MesProRouteResourceImportService.rewrite(config, Map.of("mes_pro_process:11",111L,"mes_pro_route_process:12",112L,"mes_md_workstation:13",113L,"mes_pro_process_pool_team_device:14",114L,"mes_md_item:15",115L));
        assertEquals(111L, config.getLong("processId")); assertEquals(113L, config.getLong("workstationId")); assertEquals(114L, config.getJSONArray("deviceIds").getLong(0));
        assertEquals(115L, config.getJSONArray("inputMaterialIds").getLong(0)); assertEquals(14, config.getJSONArray("parameterRules").getJSONObject(0).getIntValue("defaultValue"));
    }
    @Test void missingReferencedResourceFailsInsteadOfRetainingSourceId() {
        assertThrows(ServiceException.class, () -> MesProRouteResourceImportService.rewrite(JSON.parseObject("{\"deviceId\":14}"), Map.of()));
    }
}
