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
