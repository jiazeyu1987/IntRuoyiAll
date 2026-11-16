package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** Snapshot row IDs are historical projection IDs. Bind them by their formal business keys. */
final class MesProRouteTransferSnapshot {
    static void normalize(Map<String, JSONObject> resources) {
        for (var entry : resources.entrySet()) {
            if (!entry.getKey().startsWith("mes_pro_route_version:")) continue;
            JSONObject version = entry.getValue();
            JSONObject snapshot = JSON.parseObject(version.getString("route_snapshot_json"));
            Long route = version.getLong("route_id");
            JSONObject configs = snapshot.getJSONObject("configSnapshots");
            if (configs == null) throw invalidParamException("生效版本缺少完整配置快照");
            bind(configs.getJSONArray("batchUseConfigs"), "mes_pro_route_flow_process_config", route, resources, "routeProcessId", "useType");
            bind(configs.getJSONArray("scheduleUseConfigs"), "mes_pro_route_flow_process_config", route, resources, "routeProcessId", "useType");
            bind(configs.getJSONArray("products"), "mes_pro_route_product", route, resources, "itemId");
            bind(configs.getJSONArray("productBoms"), "mes_pro_route_product_bom", route, resources, "processId", "productId", "itemId");
            bind(configs.getJSONArray("scheduleConfigs"), "mes_pro_route_schedule_config", route, resources, "routeProcessId", "itemId");
            JSONObject graph = configs.getJSONObject("flowGraph");
            if (graph != null) {
                bind(graph.getJSONArray("edges"), "mes_pro_route_process_flow_edge", route, resources, "sourceRouteProcessId", "targetRouteProcessId", "relationType");
                bind(graph.getJSONArray("boundaryEdges"), "mes_pro_route_process_flow_boundary_edge", route, resources, "boundaryType", "routeProcessId");
            }
            normalizeMetadata(snapshot, version.getLong("id"));
            version.put("route_snapshot_json", JSON.toJSONString(snapshot, SerializerFeature.WriteMapNullValue));
            version.put("route_snapshot_sha256", new cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteSnapshotCanonicalizer().sha256(version.getString("route_snapshot_json")));
        }
    }
    private static void bind(JSONArray array, String table, Long route, Map<String, JSONObject> resources, String... fields) {
        if (array == null) return;
        for (Object raw : array) {
            JSONObject node = (JSONObject) raw;
            // Explicit empty binding snapshots contain only routeProcessId, with no projection row identity.
            if (!node.containsKey("id") && !node.containsKey("routeFlowConfigId")) continue;
            var matches = resources.entrySet().stream().filter(e -> e.getKey().startsWith(table + ":"))
                .map(Map.Entry::getValue).filter(row -> {
                    Long rowRoute = row.getLong("route_id");
                    if (table.equals("mes_pro_route_schedule_config")) {
                        JSONObject version = resources.get("mes_pro_route_version:" + row.getLong("route_version_id"));
                        rowRoute = version == null ? null : version.getLong("route_id");
                    }
                    if (!Objects.equals(route, rowRoute)) return false;
                    for (String field : fields) if (!Objects.equals(node.getString(field), row.getString(snake(field)))) return false;
                    return true;
                }).toList();
            if (matches.size() != 1) throw invalidParamException("快照配置无法按业务身份关联：" + table + " " + node.getString("id"));
            JSONObject row = matches.get(0);
            node.put("id", row.getLong("id"));
            if (node.containsKey("routeFlowConfigId")) node.put("routeFlowConfigId", row.getLong("route_flow_config_id"));
        }
    }
    private static String snake(String field) { return field.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT); }
    private static void normalizeMetadata(Object raw, Long version) {
        if (raw instanceof JSONObject object) {
            for (String audit : List.of("creator", "updater", "createTime", "updateTime", "tenantId", "deleted")) object.remove(audit);
            for (String key : object.keySet()) {
                if (key.equals("routeVersionId")) object.put(key, version);
                else normalizeMetadata(object.get(key), version);
            }
        } else if (raw instanceof JSONArray array) for (Object child : array) normalizeMetadata(child, version);
    }
    private MesProRouteTransferSnapshot() {}
}
