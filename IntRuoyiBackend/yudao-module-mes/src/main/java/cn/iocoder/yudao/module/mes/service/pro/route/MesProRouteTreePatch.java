package cn.iocoder.yudao.module.mes.service.pro.route;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.invalid;

/**
 * 对正式配置应用父级本次变更。以工序库 processId 定位同一工序：
 * 当前路线禁止重复工序，processId 不随发布投影重建而改变。
 * routeProcessId 仅为某版本的引用，不能用作跨路线的工序身份。
 */
public final class MesProRouteTreePatch {
    private static final Set<String> LOCAL_CONFIGS = Set.of("products", "productBoms", "craftChangeId",
            "sharedSourceSnapshots");
    private static final Set<String> REFERENCES = Set.of("routeProcessId", "sourceRouteProcessId",
            "targetRouteProcessId", "predecessorRouteProcessId", "nextRouteProcessId", "clientRouteProcessId");
    private MesProRouteTreePatch() {}

    public static JSONObject inherit(JSONObject template, JSONObject target) {
        JSONObject configs = normalized(template.getJSONObject("configSnapshots"), processReferences(template));
        JSONObject local = target.getJSONObject("configSnapshots");
        for (String key : LOCAL_CONFIGS) {
            if (local.containsKey(key)) configs.put(key, cloneValue(local.get(key)));
            else configs.remove(key);
        }
        Map<String, Long> outgoing = officialReferences(target);
        addSourceReferences(outgoing, template, order(template));
        JSONObject result = copy(target);
        result.put("configSnapshots", restored(configs, outgoing));
        ensureCandidateReferences(result);
        // 车间由新增流程明确选择，继承工艺不能把其岗位改到父模板的车间。
        Map<Long, JSONObject> localNodes = byProcess(local.getJSONObject("flowGraph").getJSONArray("nodes"));
        for (JSONObject node : result.getJSONObject("configSnapshots").getJSONObject("flowGraph")
                .getJSONArray("nodes").toJavaList(JSONObject.class)) {
            JSONObject own = localNodes.get(node.getLong("processId"));
            if (own != null && own.containsKey("routeProcessWorkstationId"))
                node.put("routeProcessWorkstationId", own.get("routeProcessWorkstationId"));
        }
        result.getJSONObject("configSnapshots").remove("craftChangeId");
        return result;
    }

    public static JSONObject apply(JSONObject before, JSONObject after, JSONObject target) {
        Map<String, Long> beforeIds = processReferences(before), afterIds = processReferences(after);
        Map<String, Long> targetIds = processReferences(target);
        JSONObject oldConfigs = normalized(before.getJSONObject("configSnapshots"), beforeIds);
        JSONObject newConfigs = normalized(after.getJSONObject("configSnapshots"), afterIds);
        JSONObject local = normalized(target.getJSONObject("configSnapshots"), targetIds);
        List<Long> oldOrder = order(before), newOrder = order(after), childOrder = order(target);
        // 工序删除覆盖该工序自身修改；其余个性化节点保留。
        Set<Long> removed = new HashSet<>(oldOrder); removed.removeAll(newOrder);
        boolean structureChanged = !oldOrder.equals(newOrder);
        Map<Long, List<Long>> extras = new LinkedHashMap<>();
        Long anchor = null;
        for (Long id : childOrder) {
            if (oldOrder.contains(id) || newOrder.contains(id)) { anchor = id; continue; }
            if (anchor != null && removed.contains(anchor) && structureChanged)
                throw invalid("个性化工序 " + id + " 的前置工序将被删除，请先调整其位置");
            extras.computeIfAbsent(anchor, ignored -> new ArrayList<>()).add(id);
        }
        JSONObject merged = copy(local);
        for (String key : newConfigs.keySet()) {
            if (LOCAL_CONFIGS.contains(key)) continue;
            merged.put(key, patch(oldConfigs.get(key), newConfigs.get(key), local.get(key), key));
        }
        for (String key : oldConfigs.keySet()) if (!LOCAL_CONFIGS.contains(key) && !newConfigs.containsKey(key))
            merged.remove(key);
        List<Long> resultOrder = new ArrayList<>();
        if (structureChanged) {
            resultOrder.addAll(extras.getOrDefault(null, List.of()));
            for (Long id : newOrder) {
                resultOrder.add(id);
                resultOrder.addAll(extras.getOrDefault(id, List.of()));
            }
        } else resultOrder.addAll(childOrder);
        JSONObject graph = merged.getJSONObject("flowGraph");
        if (graph == null) throw invalid("缺少工序关系图");
        Map<Long, JSONObject> mergedNodes = byProcess(graph.getJSONArray("nodes"));
        for (Long id : resultOrder) if (!mergedNodes.containsKey(id))
            throw invalid("下属流程缺少对应工序 " + id + "，请先处理工序差异");
        JSONArray orderedNodes = new JSONArray();
        Map<String, Integer> sorts = new HashMap<>();
        for (Long id : resultOrder) {
            JSONObject node = mergedNodes.get(id);
            node.put("sort", orderedNodes.size() + 1);
            sorts.put("P:" + id, orderedNodes.size() + 1);
            orderedNodes.add(node);
        }
        graph.put("nodes", orderedNodes);
        if (structureChanged && linear(before) && linear(after) && linear(target)) {
            JSONArray edges = new JSONArray();
            for (int i = 1; i < resultOrder.size(); i++) {
                JSONObject edge = new JSONObject(true);
                edge.put("sourceRouteProcessId", "P:" + resultOrder.get(i - 1));
                edge.put("targetRouteProcessId", "P:" + resultOrder.get(i));
                edge.put("sourceSort", i); edge.put("targetSort", i + 1);
                edge.put("relationType", "NORMAL"); edge.put("sort", i);
                edges.add(edge);
            }
            graph.put("edges", edges);
            JSONArray boundaries = graph.getJSONArray("boundaryEdges");
            if (boundaries != null && !resultOrder.isEmpty()) {
                Map<String, JSONObject> uniqueBoundaries = new LinkedHashMap<>();
                for (JSONObject boundary : boundaries.toJavaList(JSONObject.class)) {
                    if ("START".equals(boundary.getString("boundaryType")))
                        boundary.put("routeProcessId", "P:" + resultOrder.get(0));
                    else if ("END".equals(boundary.getString("boundaryType")))
                        boundary.put("routeProcessId", "P:" + resultOrder.get(resultOrder.size() - 1));
                    // 线性流程重连后，父级新终点与子流程原终点会汇合为同一条边。
                    uniqueBoundaries.putIfAbsent(identity(boundary, "boundaryType,routeProcessId"), boundary);
                }
                graph.put("boundaryEdges", new JSONArray(new ArrayList<>(uniqueBoundaries.values())));
            }
        }
        // 同一工序的排序和所有引用必须同时更新；不可遗留被删除节点的配置。
        align(merged, sorts, "");
        Map<String, Long> outgoing = officialReferences(target);
        addSourceReferences(outgoing, after, resultOrder);
        JSONObject result = copy(target);
        result.put("configSnapshots", restored(merged, outgoing));
        ensureCandidateReferences(result);
        result.getJSONObject("configSnapshots").remove("craftChangeId");
        return result;
    }

    private static Object patch(Object before, Object after, Object child, String path) {
        if (Objects.equals(before, after)) return cloneValue(child);
        if (after instanceof JSONObject next) {
            JSONObject old = before instanceof JSONObject value ? value : new JSONObject(true);
            if (child == null && !old.isEmpty())
                throw invalid("下属流程缺少待修改配置：" + path);
            JSONObject local = child instanceof JSONObject value ? value : new JSONObject(true);
            JSONObject result = copy(local);
            for (String key : next.keySet()) {
                if (Set.of("id", "routeId", "routeVersionId", "graphVersion", "configVersion", "createTime", "updateTime").contains(key)) continue;
                result.put(key, patch(old.get(key), next.get(key), local.get(key), path + "." + key));
            }
            for (String key : old.keySet()) if (!next.containsKey(key)) result.remove(key);
            return result;
        }
        if (after instanceof JSONArray next) {
            JSONArray old = before instanceof JSONArray value ? value : new JSONArray();
            JSONArray local = child instanceof JSONArray value ? value : new JSONArray();
            String key = arrayKey(path, old, next, local);
            if (key == null) return cloneValue(after);
            Map<String, Object> oldMap = index(old, key), nextMap = index(next, key), localMap = index(local, key);
            JSONArray result = new JSONArray();
            for (Object value : local) {
                String identity = identity((JSONObject) value, key);
                if (oldMap.containsKey(identity) && !nextMap.containsKey(identity)) continue;
                if (nextMap.containsKey(identity))
                    result.add(patch(oldMap.get(identity), nextMap.get(identity), value, path + "[" + identity + "]"));
                else result.add(cloneValue(value));
            }
            for (Map.Entry<String, Object> entry : nextMap.entrySet()) if (!localMap.containsKey(entry.getKey())) {
                if (oldMap.containsKey(entry.getKey())) {
                    if (!Objects.equals(oldMap.get(entry.getKey()), entry.getValue()))
                        throw invalid("下属流程缺少待修改工序或绑定：" + path + " " + entry.getKey());
                } else result.add(cloneValue(entry.getValue()));
            }
            return result;
        }
        return cloneValue(after);
    }

    private static String arrayKey(String path, JSONArray... arrays) {
        if (path.endsWith(".nodes")) return "processId";
        if (path.endsWith(".edges")) return "sourceRouteProcessId,targetRouteProcessId";
        if (path.endsWith(".layouts")) return "routeProcessId";
        if (path.endsWith(".boundaryEdges")) return "boundaryType,routeProcessId";
        if (path.endsWith(".deviceSelectionGroups")) return "deviceGroupKey";
        if (path.endsWith(".parameterRules")) return "deviceId,parameterCode";
        if (path.equals("productionProcessConfigs") || path.equals("batchUseConfigs")
                || path.equals("scheduleUseConfigs")) return "routeProcessId";
        for (String key : List.of("bindingKey", "formBindingKey", "parameterKey", "parameterId", "id")) {
            boolean present = false, valid = true;
            for (JSONArray array : arrays) for (Object value : array) {
                present = true;
                if (!(value instanceof JSONObject object) || object.get(key) == null) valid = false;
            }
            if (present && valid) return key;
        }
        return null;
    }
    private static String identity(JSONObject object, String keys) {
        List<String> result = new ArrayList<>();
        for (String key : keys.split(",")) {
            if (object.get(key) == null) throw invalid("配置缺少稳定标识：" + key);
            result.add(String.valueOf(object.get(key)));
        }
        return String.join("/", result);
    }
    private static Map<String, Object> index(JSONArray array, String key) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Object value : array) {
            String identity = identity((JSONObject) value, key);
            if (result.put(identity, value) != null) throw invalid("配置标识重复：" + identity);
        }
        return result;
    }
    public static List<Long> order(JSONObject root) {
        return root.getJSONObject("configSnapshots").getJSONObject("flowGraph").getJSONArray("nodes")
                .toJavaList(JSONObject.class).stream()
                .sorted(Comparator.comparing(node -> node.getInteger("sort"))).map(node -> node.getLong("processId")).toList();
    }
    private static Map<Long, JSONObject> byProcess(JSONArray nodes) {
        Map<Long, JSONObject> result = new LinkedHashMap<>();
        for (JSONObject node : nodes.toJavaList(JSONObject.class))
            if (result.put(node.getLong("processId"), node) != null) throw invalid("同一流程不允许重复工序");
        return result;
    }
    private static Map<String, Long> processReferences(JSONObject root) {
        Map<String, Long> result = new HashMap<>();
        for (JSONObject node : root.getJSONObject("configSnapshots").getJSONObject("flowGraph")
                .getJSONArray("nodes").toJavaList(JSONObject.class)) {
            Long processId = node.getLong("processId");
            if (processId == null || node.get("routeProcessId") == null) throw invalid("工序身份缺失");
            result.put(node.getString("routeProcessId"), processId);
            if (node.get("clientRouteProcessId") != null) result.put(node.getString("clientRouteProcessId"), processId);
        }
        return result;
    }
    private static Map<String, Long> officialReferences(JSONObject root) {
        Map<String, Long> result = new HashMap<>();
        for (JSONObject node : root.getJSONObject("configSnapshots").getJSONObject("flowGraph")
                .getJSONArray("nodes").toJavaList(JSONObject.class))
            result.put("P:" + node.getLong("processId"), node.getLong("routeProcessId"));
        return result;
    }
    private static void addSourceReferences(Map<String, Long> target, JSONObject source, List<Long> processes) {
        Map<String, Long> sourceReferences = officialReferences(source);
        for (Long process : processes) {
            String key = "P:" + process;
            if (target.containsKey(key)) continue;
            Long reference = sourceReferences.get(key);
            // 新工序可以来自草稿候选：负数 routeProcessId 是编辑器分配的稳定候选引用，
            // 发布投影会将其映射为目标路线的新正式记录。正式来源仍必须保持正数引用。
            if (reference == null || reference == 0 || target.containsValue(reference))
                throw invalid("新增工序缺少唯一的候选快照引用：" + process);
            target.put(key, reference);
        }
    }

    private static void ensureCandidateReferences(JSONObject root) {
        JSONArray nodes = root.getJSONObject("configSnapshots").getJSONObject("flowGraph").getJSONArray("nodes");
        for (JSONObject node : nodes.toJavaList(JSONObject.class)) {
            Long routeProcessId = node.getLong("routeProcessId");
            if (routeProcessId != null && routeProcessId < 0 && !node.containsKey("clientRouteProcessId")) {
                node.put("clientRouteProcessId", routeProcessId);
            }
        }
    }
    private static JSONObject normalized(JSONObject value, Map<String, Long> references) {
        JSONObject businessConfigs = copy(value);
        JSONObject graph = businessConfigs.getJSONObject("flowGraph");
        if (graph != null) {
            // 编辑器操作列表不是发布后的业务配置；其 client 引用在正式节点生成后已失效。
            for (String key : List.of("routeProcessCreates", "routeProcessUpdates", "routeProcessDeletes"))
                graph.remove(key);
        }
        return (JSONObject) normalize(businessConfigs, references, false);
    }
    private static Object normalize(Object value, Map<String, Long> references, boolean restoring) {
        if (value instanceof JSONObject object) {
            JSONObject result = new JSONObject(true);
            for (String key : object.keySet()) {
                Object item = object.get(key);
                String outputKey = key;
                if (!restoring && references.containsKey(key)) outputKey = "P:" + references.get(key);
                if (restoring && key.startsWith("P:")) {
                    if (!references.containsKey(key)) throw invalid("配置引用了已删除工序：" + key);
                    outputKey = String.valueOf(references.get(key));
                }
                if (REFERENCES.contains(key) && item != null) {
                    if (!references.containsKey(String.valueOf(item))) throw invalid("工序引用无法匹配：" + item);
                    item = restoring ? references.get(String.valueOf(item)) : "P:" + references.get(String.valueOf(item));
                } else item = normalize(item, references, restoring);
                result.put(outputKey, item);
            }
            return result;
        }
        if (value instanceof JSONArray array) {
            JSONArray result = new JSONArray();
            for (Object item : array) result.add(normalize(item, references, restoring));
            return result;
        }
        return value;
    }
    private static JSONObject restored(JSONObject value, Map<String, Long> references) {
        return (JSONObject) normalize(value, references, true);
    }
    private static void align(Object value, Map<String, Integer> sorts, String path) {
        if (value instanceof JSONArray array) {
            array.removeIf(item -> item instanceof JSONObject object && object.getString("routeProcessId") != null
                    && !sorts.containsKey(object.getString("routeProcessId")));
            array.forEach(item -> align(item, sorts, path + "[]"));
        } else if (value instanceof JSONObject object) {
            String reference = object.getString("routeProcessId");
            boolean processSort = Set.of("flowGraph.nodes[]", "productionProcessConfigs[]",
                    "batchUseConfigs[]", "scheduleUseConfigs[]", "scheduleConfigs[]").contains(path)
                    || path.startsWith("scheduleConfigs.P:") && !path.substring("scheduleConfigs.P:".length()).contains(".");
            if (processSort && reference != null && sorts.containsKey(reference) && object.containsKey("sort"))
                object.put("sort", sorts.get(reference));
            if (reference != null && sorts.containsKey(reference) && object.containsKey("routeProcessSort"))
                object.put("routeProcessSort", sorts.get(reference));
            if (object.containsKey("sourceSort")) object.put("sourceSort", sorts.get(object.getString("sourceRouteProcessId")));
            if (object.containsKey("targetSort")) object.put("targetSort", sorts.get(object.getString("targetRouteProcessId")));
            for (String key : new ArrayList<>(object.keySet())) {
                if (key.startsWith("P:") && !sorts.containsKey(key)) object.remove(key);
                else align(object.get(key), sorts, path.isEmpty() ? key : path + "." + key);
            }
        }
    }
    private static boolean linear(JSONObject root) {
        List<Long> ids = order(root);
        JSONArray nodes = root.getJSONObject("configSnapshots").getJSONObject("flowGraph").getJSONArray("nodes");
        JSONArray edges = root.getJSONObject("configSnapshots").getJSONObject("flowGraph").getJSONArray("edges");
        if (edges == null || edges.size() != ids.size() - 1) return ids.size() == 1 && (edges == null || edges.isEmpty());
        Map<String, Long> references = processReferences(root);
        Set<String> actual = new HashSet<>();
        for (JSONObject edge : edges.toJavaList(JSONObject.class)) {
            if (!"NORMAL".equals(edge.getString("relationType"))) return false;
            actual.add(references.get(edge.getString("sourceRouteProcessId")) + "/" + references.get(edge.getString("targetRouteProcessId")));
        }
        for (int i = 1; i < ids.size(); i++) if (!actual.contains(ids.get(i - 1) + "/" + ids.get(i))) return false;
        return true;
    }
    private static Object cloneValue(Object value) { return value == null ? null : JSON.parse(JSON.toJSONString(value)); }
    private static JSONObject copy(JSONObject value) { return JSON.parseObject(value.toJSONString()); }
}
