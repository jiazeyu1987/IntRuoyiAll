package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.route.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import com.alibaba.fastjson.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteResourceTransferService.*;

/** Import the resource graph as one transaction; existing identities must match exactly. */
@Service
public class MesProRouteResourceImportService {
    @Resource private JdbcTemplate jdbc;
    @Resource private MesProRouteResourceTransferService reader;
    @Resource private MesProRouteOwnerPermissionService owners;
    @Resource private MesProRouteControlledContentAdapter platform;
    @Resource private MesProRouteVersionMapper versions;

    @Transactional(rollbackFor = Exception.class)
    public MesProRouteWorkbookImportResult importWorkbook(Workbook workbook) {
        var resources = reader.readResources(workbook);
        validateGraph(resources);
        Map<String, Long> ids = new HashMap<>();
        Set<String> existing = new HashSet<>();
        Long tenant = TenantContextHolder.getRequiredTenantId();
        for (var entry : resources.entrySet()) {
            String table = table(entry.getKey());
            String identity = table.equals("system_users") ? "username" : requireTable(table).identity();
            List<Long> matches = identity.isEmpty() ? List.of() : jdbc.queryForList("SELECT id FROM " + table
                + " WHERE " + identity + "=? AND tenant_id=? AND deleted=0 FOR UPDATE", Long.class,
                entry.getValue().get(identity), tenant);
            if (matches.size() > 1) throw invalidParamException("资源编码不唯一：" + entry.getKey());
            if (table.equals("mes_pro_route") && !matches.isEmpty()) throw invalidParamException("路线编码已存在：" + entry.getValue().getString("code"));
            if (table.equals("system_users") && matches.isEmpty()) throw invalidParamException("目标租户缺少执行人员：" + entry.getValue().getString("username"));
            // These identities remain unique after soft deletion in the current database schema.
            if (matches.isEmpty() && (table.equals("mes_pro_route") || table.equals("dcc_project_code"))) {
                List<Long> deleted = jdbc.queryForList("SELECT id FROM " + table + " WHERE " + identity
                    + "=? AND tenant_id=? AND deleted=1 FOR UPDATE", Long.class, entry.getValue().get(identity), tenant);
                if (!deleted.isEmpty()) throw invalidParamException("已删除的"
                    + (table.equals("mes_pro_route") ? "工艺流程" : "项目") + "仍占用编码："
                    + entry.getValue().getString(identity) + "，请先处理该删除记录");
            }
            ids.put(entry.getKey(), matches.isEmpty() ? IdWorker.getId() : matches.get(0));
            if (!matches.isEmpty()) existing.add(entry.getKey());
        }
        reuseSharedRelations(resources, ids, existing, tenant);
        Map<String, JSONObject> rewritten = new LinkedHashMap<>();
        for (var entry : resources.entrySet()) {
            String table = table(entry.getKey());
            if (table.equals("system_users")) continue;
            JSONObject row = JSON.parseObject(JSON.toJSONString(entry.getValue(), com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue));
            for (String column : new ArrayList<>(row.keySet())) {
                if (column.equals("id")) row.put(column, ids.get(entry.getKey()));
                else if (referenceTable(table, column) != null) row.put(column, remap(referenceTable(table, column), row.get(column), ids));
                else if (column.endsWith("_id") && positive(row.get(column))) throw invalidParamException("不支持的资源引用：" + table + "." + column);
                if (column.endsWith("_json") && row.getString(column) != null && !row.getString(column).isBlank())
                    row.put(column, rewriteJson(row.getString(column), ids));
            }
            if (table.equals("mes_pro_craft")) row.put("fingerprint", cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.fingerprint(row.getLong("process_id"), JSON.parseArray(row.getString("parameters_json"))));
            if (table.equals("mes_pro_route_version")) freezeSnapshot(row);
            rewritten.put(entry.getKey(), row);
            if (existing.contains(entry.getKey())) {
                Map<String, Object> values = jdbc.queryForMap("SELECT " + String.join(",", requireTable(table).columns())
                    + " FROM " + table + " WHERE id=? AND tenant_id=? AND deleted=0 FOR UPDATE", ids.get(entry.getKey()), tenant);
                values.replaceAll((column, value) -> sqlValue(value));
                JSONObject stored = new JSONObject(values);
                for (String column : row.keySet()) if (!equalValue(column, stored.get(column), row.get(column)))
                    throw invalidParamException("目标租户同编码资源内容不一致：" + table + " " + row.getString(requireTable(table).identity()) + " 字段 " + column);
            }
        }
        for (var entry : rewritten.entrySet()) {
            if (existing.contains(entry.getKey())) continue;
            String table = table(entry.getKey()); JSONObject row = entry.getValue();
            List<String> columns = new ArrayList<>(requireTable(table).columns());
            columns.addAll(List.of("tenant_id", "creator", "updater", "deleted"));
            row.put("tenant_id", tenant); row.put("creator", String.valueOf(SecurityFrameworkUtils.getLoginUserId()));
            row.put("updater", String.valueOf(SecurityFrameworkUtils.getLoginUserId())); row.put("deleted", false);
            jdbc.update("INSERT INTO " + table + " (" + String.join(",", columns) + ") VALUES (" + String.join(",", Collections.nCopies(columns.size(), "?")) + ")", columns.stream().map(row::get).toArray());
        }
        List<String> codes = new ArrayList<>();
        for (var entry : rewritten.entrySet()) {
            JSONObject row = entry.getValue();
            if (table(entry.getKey()).equals("mes_pro_route")) { codes.add(row.getString("code")); owners.bindCurrentUserAsOwner(row.getLong("id")); }
            if (table(entry.getKey()).equals("mes_pro_route_version")) platform.recordActiveRegistered(versions.selectById(row.getLong("id")), SecurityFrameworkUtils.getLoginUserId(), "跨租户流程关联资源导入");
        }
        var result = new MesProRouteWorkbookImportResult(); result.setRouteCodes(codes); result.setRouteCount(codes.size());
        result.setRouteProcessCount(count(resources, "mes_pro_route_process")); result.setRouteProductCount(count(resources, "mes_pro_route_product"));
        result.setRouteProductBomCount(count(resources, "mes_pro_route_product_bom")); return result;
    }
    void reuseSharedRelations(Map<String, JSONObject> resources, Map<String, Long> ids, Set<String> existing, Long tenant) {
        Map<String, List<String>> keys = Map.of(
            "mes_md_workstation_machine", List.of("workstation_id", "machinery_id"),
            "mes_dv_machinery_process", List.of("machinery_id", "process_id"),
            "mes_pro_process_pool_team_process_device", List.of("leader_user_id", "process_id", "device_id", "device_group_key"));
        for (var entry : resources.entrySet()) {
            String table = table(entry.getKey());
            List<String> fields = keys.get(table);
            if (fields == null) continue;
            List<Object> values = new ArrayList<>();
            for (String field : fields) {
                String target = referenceTable(table, field);
                values.add(target == null ? entry.getValue().get(field) : remap(target, entry.getValue().get(field), ids));
            }
            values.add(tenant);
            String where = fields.stream().map(field -> field + " <=> ?").collect(java.util.stream.Collectors.joining(" AND "));
            List<Long> matches = jdbc.queryForList("SELECT id FROM " + table + " WHERE " + where
                + " AND tenant_id=? AND deleted=0 FOR UPDATE", Long.class, values.toArray());
            if (matches.size() > 1) throw invalidParamException("目标租户关联资源重复：" + table + " " + entry.getKey());
            if (!matches.isEmpty()) { ids.put(entry.getKey(), matches.get(0)); existing.add(entry.getKey()); }
        }
    }
    private static void validateGraph(Map<String, JSONObject> resources) {
        var routes = resources.entrySet().stream().filter(e -> table(e.getKey()).equals("mes_pro_route")).toList();
        if (routes.isEmpty()) throw invalidParamException("迁移文件中没有工艺流程");
        Set<String> projects = new HashSet<>();
        for (var route : routes) {
            Long id = route.getValue().getLong("id");
            var versions = resources.entrySet().stream().filter(e -> table(e.getKey()).equals("mes_pro_route_version") && Objects.equals(e.getValue().getLong("route_id"), id)).map(Map.Entry::getValue).toList();
            if (versions.size() != 1 || !Boolean.TRUE.equals(versions.get(0).getBoolean("active")) || !"ACTIVE".equals(versions.get(0).getString("lifecycle_status")))
                throw invalidParamException("每条流程必须包含且仅包含一个生效版本：" + route.getValue().getString("code"));
            JSONObject version = versions.get(0);
            var canonicalizer = new MesProRouteSnapshotCanonicalizer();
            if (!canonicalizer.validate(id, version.getString("route_snapshot_json")).ready()
                || !MesProRouteSnapshotCanonicalizer.FORMAT_VERSION.equals(version.getString("route_snapshot_format_version"))
                || !canonicalizer.sha256(version.getString("route_snapshot_json")).equals(version.getString("route_snapshot_sha256")))
                throw invalidParamException("导入源快照身份或内容无效：" + route.getValue().getString("code"));
            var bindings = resources.entrySet().stream().filter(e -> table(e.getKey()).equals("mes_pro_route_dcc_project_binding") && Objects.equals(e.getValue().getLong("route_id"), id)).map(Map.Entry::getValue).toList();
            if (bindings.size() != 1 || !projects.add(bindings.get(0).getString("dcc_project_code_id")))
                throw invalidParamException("流程项目绑定缺失或同一项目关联多条流程");
        }
        for (var entry : resources.entrySet()) {
            JSONObject row = entry.getValue();
            Long routeId = row.getLong("route_id");
            if (routeId != null && !resources.containsKey("mes_pro_route:" + routeId)) throw invalidParamException("资源引用了包外流程：" + entry.getKey());
            for (String field : List.of("route_process_id", "source_route_process_id", "target_route_process_id")) {
                Long process = row.getLong(field);
                if (process == null) continue;
                JSONObject target = resources.get("mes_pro_route_process:" + process);
                if (target == null || (routeId != null && !Objects.equals(routeId, target.getLong("route_id"))))
                    throw invalidParamException("资源引用了其他流程的工序：" + entry.getKey());
            }
        }
    }

    private static String table(String key) { return key.substring(0, key.indexOf(':')); }
    private static int count(Map<String, ?> rows, String table) { return (int) rows.keySet().stream().filter(k -> table(k).equals(table)).count(); }
    private static Object remap(String table, Object id, Map<String, Long> ids) {
        if (!positive(id)) return id;
        Long target = ids.get(table + ":" + id);
        if (target == null) throw invalidParamException("迁移文件缺少关联资源：" + table + ":" + id);
        return target;
    }
    static boolean equalValue(String column, Object a, Object b) {
        if (column.endsWith("_json") && a instanceof String left && b instanceof String right && !left.isBlank() && !right.isBlank())
            return Objects.equals(JSON.parse(left), JSON.parse(right));
        if (a instanceof Number && b instanceof Number) return new java.math.BigDecimal(a.toString()).compareTo(new java.math.BigDecimal(b.toString())) == 0;
        return Objects.equals(a, b);
    }
    static String rewriteJson(String value, Map<String, Long> ids) {
        return JSON.toJSONString(rewrite(JSON.parse(value), ids), com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue);
    }
    static void freezeSnapshot(JSONObject row) {
        MesProRouteSnapshotCanonicalizer canonicalizer = new MesProRouteSnapshotCanonicalizer();
        var validation = canonicalizer.validate(row.getLong("route_id"), row.getString("route_snapshot_json"));
        if (!validation.ready()) throw invalidParamException("导入快照校验失败：" + validation.blockers());
        if (!MesProRouteSnapshotCanonicalizer.FORMAT_VERSION.equals(row.getString("route_snapshot_format_version")))
            throw invalidParamException("导入快照格式版本不受支持");
        row.put("route_snapshot_sha256", canonicalizer.sha256(row.getString("route_snapshot_json")));
    }
    private static final Set<String> PROCESS_KEYED_CONFIGURATIONS = Set.of(
        "scheduleConfigs", "batchUseConfigs", "scheduleUseConfigs", "productionProcessConfigs");

    private static JSONObject rewriteProcessKeys(JSONObject configs, Map<String, Long> ids) {
        JSONObject result = new JSONObject(true);
        for (String key : configs.keySet()) {
            JSONObject config = configs.getJSONObject(key);
            if (config == null || !key.equals(config.getString("routeProcessId")))
                throw invalidParamException("快照工序索引与内容不一致：" + key);
            String mapped = String.valueOf(remap("mes_pro_route_process", config.get("routeProcessId"), ids));
            if (result.containsKey(mapped)) throw invalidParamException("快照工序索引映射冲突：" + key);
            result.put(mapped, rewrite(config, ids));
        }
        return result;
    }
    static Object rewrite(Object value, Map<String, Long> ids) {
        if (value instanceof JSONObject object) {
            for (String key : new ArrayList<>(object.keySet())) {
                String target = JSON_REFERENCES.get(key);
                if (key.equals("candidateSourceIds")) target = candidateTable(object.getString("candidateSourceType"));
                Object current = object.get(key);
                if (PROCESS_KEYED_CONFIGURATIONS.contains(key) && current instanceof JSONObject configs) {
                    object.put(key, rewriteProcessKeys(configs, ids));
                } else if (target != null) {
                    if (current instanceof JSONArray array) for (int i = 0; i < array.size(); i++) array.set(i, remap(target, array.get(i), ids));
                    else object.put(key, remap(target, current, ids));
                } else if (key.equals("id") && positive(current)) {
                    var candidates = ids.entrySet().stream().filter(e -> e.getKey().endsWith(":" + current)).map(Map.Entry::getValue).distinct().toList();
                    if (candidates.size() != 1) throw invalidParamException("快照编号映射不唯一：" + current);
                    object.put(key, candidates.get(0));
                } else rewrite(current, ids);
            }
            refreshCraftFingerprints(object);
        } else if (value instanceof JSONArray array) for (Object item : array) rewrite(item, ids);
        return value;
    }
    private static void refreshCraftFingerprints(JSONObject config) {
        if (config.getLong("craftId") == null || config.getJSONArray("parameterRules") == null) return;
        for (JSONObject binding : cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.bindings(config)) {
            JSONArray parameters = new JSONArray(); Set<String> codes = new HashSet<>();
            JSONArray devices = binding.getJSONArray("craftDeviceIds");
            for (Object raw : config.getJSONArray("parameterRules")) {
                JSONObject rule = (JSONObject) raw;
                if (devices.contains(rule.getLong("deviceId")) && codes.add(rule.getString("parameterCode"))) {
                    JSONObject parameter = JSON.parseObject(JSON.toJSONString(rule, com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue));
                    parameter.remove("deviceId"); parameter.remove("processId"); parameter.remove("routeProcessId"); parameters.add(parameter);
                }
            }
            binding.put("craftFingerprint", cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.fingerprint(config.getLong("processId"), parameters));
        }
    }
}
