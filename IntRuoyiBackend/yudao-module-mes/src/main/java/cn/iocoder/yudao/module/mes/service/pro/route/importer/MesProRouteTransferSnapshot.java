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
        // Schedule row IDs occur only as snapshot projection identities, and may collide with route-process IDs.
        for (var entry : new ArrayList<>(resources.entrySet())) {
            if (!entry.getKey().startsWith("mes_pro_route_schedule_config:")) continue;
            long id = com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
            entry.getValue().put("id", id);
            resources.remove(entry.getKey());
            resources.put("mes_pro_route_schedule_config:" + id, entry.getValue());
        }
        for (var entry : new ArrayList<>(resources.entrySet())) {
            if (!entry.getKey().startsWith("mes_pro_route_version:")) continue;
            JSONObject version = entry.getValue();
            JSONObject snapshot = JSON.parseObject(version.getString("route_snapshot_json"));
            Long route = version.getLong("route_id");
            JSONObject configs = snapshot.getJSONObject("configSnapshots");
            if (configs == null) throw invalidParamException("生效版本缺少完整配置快照");
            bind(configs.getJSONArray("batchUseConfigs"), "mes_pro_route_flow_process_config", route, resources, "routeProcessId", "useType");
            bind(configs.getJSONArray("scheduleUseConfigs"), "mes_pro_route_flow_process_config", route, resources, "routeProcessId", "useType");
            bind(configs.get("products"), "mes_pro_route_product", route, resources, "itemId");
            bind(configs.get("productBoms"), "mes_pro_route_product_bom", route, resources, "processId", "productId", "itemId");
            bind(configs.get("scheduleConfigs"), "mes_pro_route_schedule_config", route, resources, "routeProcessId", "itemId");
            JSONObject graph = configs.getJSONObject("flowGraph");
            if (graph != null) {
                bind(graph.getJSONArray("edges"), "mes_pro_route_process_flow_edge", route, resources, "sourceRouteProcessId", "targetRouteProcessId", "relationType");
                bind(graph.getJSONArray("boundaryEdges"), "mes_pro_route_process_flow_boundary_edge", route, resources, "boundaryType", "routeProcessId");
            }
            projectLossReasons(configs, version.getLong("id"), route, resources);
            normalizeMetadata(snapshot, version.getLong("id"));
            version.put("route_snapshot_json", JSON.toJSONString(snapshot, SerializerFeature.WriteMapNullValue));
            version.put("route_snapshot_sha256", new cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteSnapshotCanonicalizer().sha256(version.getString("route_snapshot_json")));
        }
    }
    /** Production execution reads reasons from the active snapshot, whose catalogue IDs are not version-row IDs. */
    private static void projectLossReasons(JSONObject configs, Long version, Long route, Map<String, JSONObject> resources) {
        String table = "mes_pro_route_process_loss_reason";
        resources.entrySet().removeIf(entry -> entry.getKey().startsWith(table + ":")
                && Objects.equals(version, entry.getValue().getLong("route_version_id")));
        Set<String> identities = new HashSet<>();
        JSONArray processes = configs.getJSONArray("productionProcessConfigs");
        if (processes == null) return;
        for (Object raw : processes) {
            if (!(raw instanceof JSONObject process)) throw invalidParamException("生产工序快照格式无效");
            JSONArray reasons = process.getJSONArray("lossReasons");
            if (reasons == null || reasons.isEmpty()) continue;
            Long routeProcess = process.getLong("routeProcessId");
            JSONObject formalProcess = resources.get("mes_pro_route_process:" + routeProcess);
            if (formalProcess == null || !Objects.equals(route, formalProcess.getLong("route_id"))
                    || !Objects.equals(process.getLong("processId"), formalProcess.getLong("process_id")))
                throw invalidParamException("损耗原因快照的流程工序引用不一致：" + routeProcess);
            int sort = 0;
            for (Object item : reasons) {
                if (!(item instanceof JSONObject reason)
                        || reason.getString("reasonCode") == null || reason.getString("reasonCode").isBlank()
                        || reason.getString("reasonName") == null || reason.getString("reasonName").isBlank()
                        || !(reason.get("enabled") instanceof Boolean))
                    throw invalidParamException("损耗原因快照条目不完整：" + routeProcess);
                if (!identities.add(routeProcess + ":" + reason.getString("reasonCode")))
                    throw invalidParamException("损耗原因快照编码重复：" + routeProcess + " " + reason.getString("reasonCode"));
                JSONObject row = new JSONObject(true);
                for (String column : MesProRouteTransferSchema.TABLES.get(table).columns()) row.put(column, null);
                long id = com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
                row.put("id", id); row.put("route_version_id", version);
                row.put("route_process_id", routeProcess); row.put("process_id", process.getLong("processId"));
                row.put("reason_code", reason.getString("reasonCode")); row.put("reason_name", reason.getString("reasonName"));
                row.put("enabled", reason.getBoolean("enabled")); row.put("remark", reason.getString("remark"));
                row.put("sort", reason.containsKey("sort") ? reason.getInteger("sort") : sort);
                resources.put(table + ":" + id, row);
                reason.put("id", id);
                sort++;
            }
        }
    }
    private static void bind(Object collection, String table, Long route, Map<String, JSONObject> resources, String... fields) {
        if (collection == null) return;
        // Publication accepts both arrays and business-keyed objects for these snapshots.
        Collection<?> values;
        if (collection instanceof JSONArray array) values = array;
        else if (collection instanceof JSONObject object) values = object.values();
        else throw invalidParamException("快照配置集合格式无效：" + table);
        for (Object raw : values) {
            if (!(raw instanceof JSONObject node)) throw invalidParamException("快照配置条目格式无效：" + table);
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
