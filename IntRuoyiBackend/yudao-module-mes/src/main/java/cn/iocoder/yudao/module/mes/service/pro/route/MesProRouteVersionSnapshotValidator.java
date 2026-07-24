package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Set;

final class MesProRouteVersionSnapshotValidator {

    private static final String SNAPSHOT_CONFIGS_KEY = "configSnapshots";
    private static final String FLOW_GRAPH_KEY = "flowGraph";
    private static final String PRODUCTS_KEY = "products";
    private static final String SCHEDULE_CONFIGS_KEY = "scheduleConfigs";
    private static final String BATCH_USE_CONFIGS_KEY = "batchUseConfigs";
    private static final String SCHEDULE_USE_CONFIGS_KEY = "scheduleUseConfigs";
    private static final Set<String> REQUIRED_CONFIG_SNAPSHOT_KEYS = Set.of(
            FLOW_GRAPH_KEY,
            PRODUCTS_KEY,
            SCHEDULE_CONFIGS_KEY,
            BATCH_USE_CONFIGS_KEY,
            SCHEDULE_USE_CONFIGS_KEY);

    private MesProRouteVersionSnapshotValidator() {
    }

    static boolean hasCompleteConfigSnapshot(String routeSnapshotJson) {
        if (StrUtil.isBlank(routeSnapshotJson)) {
            return false;
        }
        try {
            JSONObject snapshot = JSON.parseObject(routeSnapshotJson);
            if (snapshot == null || snapshot.isEmpty()) {
                return false;
            }
            if (snapshot.getLong("routeId") == null
                    || StrUtil.isBlank(snapshot.getString("routeCode"))
                    || StrUtil.isBlank(snapshot.getString("routeName"))) {
                return false;
            }
            JSONObject configSnapshots = snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
            if (configSnapshots == null
                    || !configSnapshots.keySet().containsAll(REQUIRED_CONFIG_SNAPSHOT_KEYS)) {
                return false;
            }
            JSONObject flowGraph = configSnapshots.getJSONObject(FLOW_GRAPH_KEY);
            if (flowGraph == null) {
                return false;
            }
            JSONArray nodes = flowGraph.getJSONArray("nodes");
            if (nodes == null || nodes.isEmpty()) {
                return false;
            }
            return isArrayOrObject(configSnapshots.get(PRODUCTS_KEY))
                    && isArrayOrObject(configSnapshots.get(SCHEDULE_CONFIGS_KEY))
                    && configSnapshots.get(BATCH_USE_CONFIGS_KEY) instanceof JSONArray
                    && configSnapshots.get(SCHEDULE_USE_CONFIGS_KEY) instanceof JSONArray;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean isArrayOrObject(Object value) {
        return value instanceof JSONArray || value instanceof JSONObject;
    }
}
