package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteTransferSchema.TABLES;

/** Portable, tenant-scoped closure of a route and its execution resources. */
@Service
public class MesProRouteResourceTransferService {
    static final String SHEET = "流程关联资源";
    static final String FORMAT = "MES_ROUTE_RESOURCES_V2_USER_FREE";
    @Resource private JdbcTemplate jdbc;
    private static final Map<String, String> REFERENCES = Map.ofEntries(
        Map.entry("route_id", "mes_pro_route"), Map.entry("process_id", "mes_pro_process"),
        Map.entry("next_process_id", "mes_pro_process"), Map.entry("workstation_id", "mes_md_workstation"),
        Map.entry("route_process_id", "mes_pro_route_process"),
        Map.entry("source_route_process_id", "mes_pro_route_process"), Map.entry("target_route_process_id", "mes_pro_route_process"),
        Map.entry("route_version_id", "mes_pro_route_version"), Map.entry("product_master_id", "mdm_product"),
        Map.entry("dcc_project_code_id", "dcc_project_code"), Map.entry("machinery_id", "mes_dv_machinery"),
        Map.entry("machinery_type_id", "mes_dv_machinery_type"), Map.entry("workshop_id", "mes_md_workshop"),
        Map.entry("production_line_id", "mes_md_production_line"), Map.entry("item_id", "mes_md_item"),
        Map.entry("product_id", "mes_md_item"), Map.entry("unit_measure_id", "mes_md_unit_measure"),
        Map.entry("primary_id", "mes_md_unit_measure"), Map.entry("item_type_id", "mes_md_item_type"),
        Map.entry("warehouse_id", "mes_wm_warehouse"), Map.entry("location_id", "mes_wm_warehouse_location"),
        Map.entry("area_id", "mes_wm_warehouse_area"), Map.entry("calendar_rule_id", "mes_pro_schedule_calendar_rule"),
        Map.entry("route_flow_config_id", "mes_pro_route_flow_config"),
        Map.entry("route_flow_process_config_id", "mes_pro_route_flow_process_config"),
        Map.entry("device_id", "mes_pro_process_pool_team_device")
    );
    static final Map<String, String> JSON_REFERENCES = Map.ofEntries(
        Map.entry("routeId", "mes_pro_route"), Map.entry("routeVersionId", "mes_pro_route_version"),
        Map.entry("processId", "mes_pro_process"), Map.entry("routeProcessId", "mes_pro_route_process"),
        Map.entry("sourceRouteProcessId", "mes_pro_route_process"), Map.entry("targetRouteProcessId", "mes_pro_route_process"),
        Map.entry("workstationId", "mes_md_workstation"), Map.entry("routeProcessWorkstationId", "mes_md_workstation"),
        Map.entry("deviceId", "mes_pro_process_pool_team_device"), Map.entry("deviceIds", "mes_pro_process_pool_team_device"),
        Map.entry("craftDeviceIds", "mes_pro_process_pool_team_device"), Map.entry("craftId", "mes_pro_craft"),
        Map.entry("itemId", "mes_md_item"), Map.entry("inputMaterialIds", "mes_md_item"), Map.entry("outputMaterialIds", "mes_md_item"),
        Map.entry("productionLineId", "mes_pro_route"), Map.entry("routeFlowConfigId", "mes_pro_route_flow_config"),
        Map.entry("calendarRuleId", "mes_pro_schedule_calendar_rule")
    );

    public void writeResources(Workbook workbook, Collection<Long> routeIds) {
        LinkedHashMap<String, JSONObject> resources = new LinkedHashMap<>();
        for (Long id : routeIds) collect("mes_pro_route", id, resources);
        MesProRouteTransferSnapshot.normalize(resources);
        Sheet sheet = workbook.createSheet(SHEET);
        Row header = sheet.createRow(0);
        String[] headers = {FORMAT, "来源编号", "片段序号", "资源JSON"};
        for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
        for (Map.Entry<String, JSONObject> entry : resources.entrySet()) {
            String[] key = entry.getKey().split(":");
            String payload = JSON.toJSONString(entry.getValue(), com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue);
            for (int offset = 0, part = 0; offset < payload.length(); offset += 16000, part++) {
                Row row = sheet.createRow(sheet.getLastRowNum() + 1);
                row.createCell(0).setCellValue(key[0]); row.createCell(1).setCellValue(key[1]);
                row.createCell(2).setCellValue(part);
                row.createCell(3).setCellValue(payload.substring(offset, Math.min(offset + 16000, payload.length())));
            }
        }
    }

    private void collect(String table, Long id, LinkedHashMap<String, JSONObject> data) {
        if (id == null || id <= 0 || data.containsKey(table + ":" + id)) return;
        String columns = String.join(",", requireTable(table).columns());
        List<Map<String, Object>> found = jdbc.queryForList("SELECT " + columns + " FROM " + table
            + " WHERE id=? AND tenant_id=? AND deleted=0", id, TenantContextHolder.getRequiredTenantId());
        if (found.size() != 1) throw invalidParamException("导出缺少关联资源：" + table + ":" + id);
        Map<String, Object> source = found.get(0);
        source.replaceAll((key, value) -> sqlValue(value));
        JSONObject row = new JSONObject(source);
        MesProRouteTransferPersonnel.remove(row);
        data.put(table + ":" + id, row);
        for (String column : row.keySet()) {
            String target = referenceTable(table, column);
            if (target != null) collect(target, row.getLong(column), data);
            else if (column.endsWith("_id") && !column.equals("id") && positive(row.get(column)))
                throw invalidParamException("暂不支持的关联资源：" + table + "." + column);
            if (column.endsWith("_json") && row.getString(column) != null && !row.getString(column).isBlank())
                collectJson(JSON.parse(row.getString(column)), data, column.equals("route_snapshot_json"));
        }
        if (table.equals("mes_pro_route")) {
            for (String child : TABLES.keySet()) {
                if (TABLES.get(child).columns().contains("route_id")) {
                    String active = child.equals("mes_pro_route_version") ? " AND active=1" : "";
                    collectChildren(child, "route_id", id, active, data);
                }
            }
        } else if (table.equals("mes_pro_route_version")) {
            collectChildren("mes_pro_route_schedule_config", "route_version_id", id, "", data);
            collectChildren("mes_pro_route_process_loss_reason", "route_version_id", id, "", data);
        } else if (table.equals("mes_pro_process")) {
            collectChildren("mes_pro_craft", "process_id", id, "", data);
            collectChildren("mes_pro_process_pool_team_process_device", "process_id", id, "", data);
            collectChildren("mes_dv_machinery_process", "process_id", id, "", data);
            collectChildren("mes_md_workstation", "process_id", id, "", data);
        } else if (table.equals("mes_pro_route_process")) {
            collectChildren("mes_pro_process_pool_device_parameter_rule", "route_process_id", id, "", data);
        } else if (table.equals("mes_md_workstation")) {
            collectChildren("mes_md_workstation_machine", "workstation_id", id, "", data);
        }
    }
    private void collectChildren(String table, String column, Long id, String condition, LinkedHashMap<String, JSONObject> data) {
        for (Long child : jdbc.queryForList("SELECT id FROM " + table + " WHERE " + column
            + "=? AND tenant_id=? AND deleted=0" + condition + " ORDER BY id", Long.class,
            id, TenantContextHolder.getRequiredTenantId())) collect(table, child, data);
    }
    private void collectJson(Object value, LinkedHashMap<String, JSONObject> data, boolean snapshot) {
        if (value instanceof JSONObject object) {
            for (String key : object.keySet()) {
                if (snapshot && Set.of("routeVersionId", "routeFlowConfigId").contains(key)) continue;
                String table = JSON_REFERENCES.get(key);
                if (table != null) {
                    Object ref = object.get(key);
                    if (ref instanceof JSONArray array) for (Object id : array) collect(table, Long.valueOf(id.toString()), data);
                    else if (positive(ref)) collect(table, Long.valueOf(ref.toString()), data);
                } else collectJson(object.get(key), data, snapshot);
            }
        } else if (value instanceof JSONArray array) for (Object child : array) collectJson(child, data, snapshot);
    }
    static String referenceTable(String table, String column) {
        if (column.equals("parent_id")) return table;
        return REFERENCES.get(column);
    }
    static boolean positive(Object value) {
        return value != null && !value.toString().isBlank() && new java.math.BigDecimal(value.toString()).signum() > 0;
    }
    static Object sqlValue(Object value) {
        if (value instanceof java.sql.Timestamp date) return date.toLocalDateTime().toString();
        if (value instanceof java.sql.Date date) return date.toLocalDate().toString();
        if (value instanceof java.sql.Time time) return time.toLocalTime().toString();
        if (value instanceof java.time.temporal.TemporalAccessor temporal) return temporal.toString();
        if (value instanceof java.util.Date) throw invalidParamException("数据库返回了不支持的日期类型：" + value.getClass().getName());
        return value;
    }
    static MesProRouteTransferSchema.Table requireTable(String table) {
        var spec = TABLES.get(table);
        if (spec == null) throw invalidParamException("不支持的流程资源类型：" + table);
        return spec;
    }

    LinkedHashMap<String, JSONObject> readResources(Workbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET);
        DataFormatter formatter = new DataFormatter();
        if (sheet == null || sheet.getRow(0) == null || !FORMAT.equals(formatter.formatCellValue(sheet.getRow(0).getCell(0))))
            throw invalidParamException("请重新导出不含人员配置的工艺流程文件（V2）");
        LinkedHashMap<String, StringBuilder> chunks = new LinkedHashMap<>();
        Map<String, Integer> next = new HashMap<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) throw invalidParamException("流程关联资源存在空行：" + (i + 1));
            String table = formatter.formatCellValue(row.getCell(0));
            requireTable(table);
            String id = formatter.formatCellValue(row.getCell(1));
            if (!id.matches("[1-9][0-9]*")) throw invalidParamException("流程资源编号无效：" + id);
            String key = table + ":" + id;
            int part = Integer.parseInt(formatter.formatCellValue(row.getCell(2)));
            if (part != next.getOrDefault(key, 0)) throw invalidParamException("流程资源片段重复或缺失：" + key);
            next.put(key, part + 1);
            chunks.computeIfAbsent(key, ignored -> new StringBuilder()).append(formatter.formatCellValue(row.getCell(3)));
        }
        LinkedHashMap<String, JSONObject> result = new LinkedHashMap<>();
        chunks.forEach((key, text) -> {
            JSONObject value = JSON.parseObject(text.toString());
            String table = key.substring(0, key.indexOf(':'));
            Set<String> allowed = new HashSet<>(requireTable(table).columns());
            if (!allowed.equals(value.keySet()) || !key.endsWith(":" + value.getString("id")))
                throw invalidParamException("流程资源字段或编号不匹配：" + key);
            JSONObject sanitized = JSON.parseObject(JSON.toJSONString(value, com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue));
            MesProRouteTransferPersonnel.remove(sanitized);
            if (!sanitized.equals(value)) throw invalidParamException("流程资源包含人员配置，请重新导出：" + key);
            result.put(key, value);
        });
        if (result.isEmpty()) throw invalidParamException("流程关联资源为空");
        return result;
    }
}
