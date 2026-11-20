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
            boolean structurallyComplete = isArrayOrObject(configSnapshots.get(PRODUCTS_KEY))
                    && isArrayOrObject(configSnapshots.get(SCHEDULE_CONFIGS_KEY))
                    && configSnapshots.get(BATCH_USE_CONFIGS_KEY) instanceof JSONArray
                    && configSnapshots.get(SCHEDULE_USE_CONFIGS_KEY) instanceof JSONArray
                    && configSnapshots.getInteger(PRODUCTION_PROCESS_CONFIG_SCHEMA_VERSION_KEY) != null
                    && configSnapshots.get(PRODUCTION_PROCESS_CONFIGS_KEY) instanceof JSONArray;
            if (!structurallyComplete) {
                return false;
            }
            MesProRouteCandidateConfigServiceImpl.validateProductionProcessConfigs(
                    snapshot.getLong("routeVersionId"), configSnapshots);
            return true;
        } catch (RuntimeException ex) {
            return false;
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
