package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;

final class MesProRouteVersionSnapshotValidator {

    private static final String SNAPSHOT_CONFIGS_KEY = "configSnapshots";
    private static final String FLOW_GRAPH_KEY = "flowGraph";
    private static final String PRODUCTS_KEY = "products";
    private static final String SCHEDULE_CONFIGS_KEY = "scheduleConfigs";
    private static final String BATCH_USE_CONFIGS_KEY = "batchUseConfigs";
    private static final String SCHEDULE_USE_CONFIGS_KEY = "scheduleUseConfigs";
    private static final String PRODUCTION_PROCESS_CONFIG_SCHEMA_VERSION_KEY = "productionProcessConfigSchemaVersion";
    private static final String PRODUCTION_PROCESS_CONFIGS_KEY = "productionProcessConfigs";
    private static final String ROUTE_START_PRODUCTION_LEADERS_KEY = "routeStartProductionLeaders";
    private static final Set<String> REQUIRED_CONFIG_SNAPSHOT_KEYS = Set.of(
            FLOW_GRAPH_KEY,
            PRODUCTS_KEY,
            SCHEDULE_CONFIGS_KEY,
            BATCH_USE_CONFIGS_KEY,
            SCHEDULE_USE_CONFIGS_KEY,
            PRODUCTION_PROCESS_CONFIG_SCHEMA_VERSION_KEY,
            PRODUCTION_PROCESS_CONFIGS_KEY);

    private MesProRouteVersionSnapshotValidator() {
    }

    static boolean hasCompleteConfigSnapshot(String routeSnapshotJson) {
        return configSnapshotProblem(routeSnapshotJson) == null;
    }

    static String configSnapshotProblem(String routeSnapshotJson) {
        if (StrUtil.isBlank(routeSnapshotJson)) {
            return "快照为空";
        }
        try {
            JSONObject snapshot = JSON.parseObject(routeSnapshotJson);
            if (snapshot == null || snapshot.isEmpty()) {
                return "快照没有内容";
            }
            if (snapshot.getLong("routeId") == null
                    || StrUtil.isBlank(snapshot.getString("routeCode"))
                    || StrUtil.isBlank(snapshot.getString("routeName"))) {
                return "缺少 routeId、routeCode 或 routeName";
            }
            JSONObject configSnapshots = snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
            if (configSnapshots == null
                    || !configSnapshots.keySet().containsAll(REQUIRED_CONFIG_SNAPSHOT_KEYS)) {
                return "configSnapshots 缺少必要配置字段：" + (configSnapshots == null
                        ? "configSnapshots" : REQUIRED_CONFIG_SNAPSHOT_KEYS.stream()
                        .filter(key -> !configSnapshots.containsKey(key)).sorted().toList());
            }
            JSONObject flowGraph = configSnapshots.getJSONObject(FLOW_GRAPH_KEY);
            if (flowGraph == null) {
                return "缺少 flowGraph";
            }
            JSONArray nodes = flowGraph.getJSONArray("nodes");
            if (nodes == null || nodes.isEmpty()) {
                return "flowGraph.nodes 为空";
            }
            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
                Object value = nodes.get(nodeIndex);
                if (!(value instanceof JSONObject node) || !node.containsKey("sopFileUrl")) {
                    return "flowGraph.nodes[" + nodeIndex + "] 缺少 sopFileUrl 字段";
                }
                Object sopFileUrl = node.get("sopFileUrl");
                // null 表示明确未绑定；字段缺失或其他类型不能进入生产冻结版本。
                if (sopFileUrl != null && !(sopFileUrl instanceof String)) {
                    return "flowGraph.nodes[" + nodeIndex + "] sopFileUrl 类型错误";
                }
            }
            for (String key : java.util.List.of(PRODUCTS_KEY, SCHEDULE_CONFIGS_KEY)) {
                if (!isArrayOrObject(configSnapshots.get(key))) return "configSnapshots." + key + " 必须为数组或对象";
            }
            for (String key : java.util.List.of(BATCH_USE_CONFIGS_KEY, SCHEDULE_USE_CONFIGS_KEY, PRODUCTION_PROCESS_CONFIGS_KEY)) {
                if (!(configSnapshots.get(key) instanceof JSONArray)) return "configSnapshots." + key + " 必须为数组";
            }
            if (configSnapshots.getInteger(PRODUCTION_PROCESS_CONFIG_SCHEMA_VERSION_KEY) == null) {
                return "configSnapshots." + PRODUCTION_PROCESS_CONFIG_SCHEMA_VERSION_KEY + " 缺少有效版本号";
            }
            MesProRouteCandidateConfigServiceImpl.validateProductionProcessConfigs(
                    snapshot.getLong("routeVersionId"), configSnapshots);
            return null;
        } catch (RuntimeException ex) {
            return "配置校验失败：" + ex.getMessage();
        }
    }

    static void requireProductionLeaderAssignments(String routeSnapshotJson) {
        JSONObject snapshot = JSON.parseObject(routeSnapshotJson);
        JSONObject configs = snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        if (configs == null || !hasProductionLeaderAssignments(snapshot.getLong("routeId"),
                configs.getJSONArray(ROUTE_START_PRODUCTION_LEADERS_KEY))) {
            throw exception0(BAD_REQUEST.getCode(), "工艺路线未配置有效的生产组长，请在生产组长配置中分配后再发布");
        }
    }

    private static boolean hasProductionLeaderAssignments(Long routeId, JSONArray assignments) {
        if (routeId == null || assignments == null || assignments.isEmpty()) return false;
        for (Object value : assignments) {
            if (!(value instanceof JSONObject assignment)
                    || !routeId.equals(assignment.getLong("productionLineId"))) return false;
            String sourceType = assignment.getString("candidateSourceType");
            if (!"USER".equals(sourceType) && !"USERS".equals(sourceType) && !"ROLE".equals(sourceType)) {
                return false;
            }
            JSONArray sourceIds = assignment.getJSONArray("candidateSourceIds");
            if (sourceIds == null || sourceIds.isEmpty()
                    || sourceIds.stream().anyMatch(id -> id == null || parsePositiveId(id) == null)) return false;
        }
        return true;
    }

    private static Long parsePositiveId(Object value) {
        try {
            long id = Long.parseLong(String.valueOf(value));
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isArrayOrObject(Object value) {
        return value instanceof JSONArray || value instanceof JSONObject;
    }
}
