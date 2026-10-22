package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteSnapshotCanonicalizer;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.*;

/** 工艺仅保存参数定义；设备身份始终属于路线执行配置。 */
public final class MesProCraftParameters {
    private MesProCraftParameters() {}
    private static final Set<String> FIELDS = Set.of("parameterCode", "parameterName", "unit",
            "valueType", "standardText", "lowerLimit", "upperLimit", "defaultValue",
            "optionValuesJson", "defaultText", "decimalScale", "sort");
    private static final Set<String> TYPES = Set.of("INTEGER", "DECIMAL", "BOOLEAN", "SELECT", "TEXT_STANDARD");
    private static final MesProRouteSnapshotCanonicalizer CANONICAL = new MesProRouteSnapshotCanonicalizer();

    public static cn.iocoder.yudao.framework.common.exception.ServiceException invalid(String message) {
        return ServiceExceptionUtil.exception(new cn.iocoder.yudao.framework.common.exception.ErrorCode(1_017_990_001, "{}"), message);
    }
    public static JSONArray normalize(JSONArray source) {
        if (source == null || source.isEmpty()) throw invalid("工艺至少需要一个参数");
        JSONArray result = new JSONArray();
        Set<String> codes = new HashSet<>();
        for (Object raw : source) {
            // Spring/Jackson 将请求中的 JSON 对象解析为 Map，统一转成内部 JSON 对象。
            if (!(raw instanceof Map<?, ?>)) throw invalid("工艺参数格式无效");
            JSONObject p = (JSONObject) JSON.toJSON(raw);
            if (!FIELDS.containsAll(p.keySet())) throw invalid("工艺参数包含设备身份或未知字段");
            JSONObject copy = JSON.parseObject(p.toJSONString());
            String code = copy.getString("parameterCode");
            String identityCode = cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotCodec.normalizeCode(code);
            if (identityCode == null || !codes.add(identityCode)) throw invalid("参数编码不能为空或重复");
            if (blank(copy.getString("parameterName")) || blank(copy.getString("standardText"))
                    || copy.getString("valueType") == null || !TYPES.contains(copy.getString("valueType")))
                throw invalid("参数名称、标准说明或类型无效：" + code);
            // 缺少的可空字段统一为 null，保持配置比较稳定。
            for (String field : FIELDS) copy.putIfAbsent(field, null);
            String options = copy.getString("optionValuesJson");
            if (options != null) copy.put("optionValuesJson", JSON.parseArray(options).toJSONString());
            result.add(copy);
        }
        result.sort(Comparator.comparing(raw -> ((JSONObject) raw).getString("parameterCode")));
        return result;
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    public static String fingerprint(Long processId, JSONArray parameters) {
        JSONObject identity = new JSONObject(true);
        identity.put("processId", processId);
        identity.put("parameters", normalize(parameters));
        return CANONICAL.sha256(identity.toJSONString());
    }

    /** 参数身份用于去重；行身份同时包含名称，用于阻止覆盖其他人的重命名。 */
    public static String rowFingerprint(String name, Long processId, JSONArray parameters) {
        return CANONICAL.sha256(canonical(Map.of("name", name, "parameters", fingerprint(processId, parameters))));
    }

    /** 每种设备参数方案独立提取；相同方案复用，绝不合并不同标准。 */
    public static List<Extracted> extractSchemes(JSONObject config) {
        JSONArray rules = config.getJSONArray("parameterRules");
        if (rules == null) throw invalid("现有工序缺少正式参数配置");
        if (rules.isEmpty()) return List.of();
        Map<Long, JSONArray> byDevice = new TreeMap<>();
        for (Object raw : rules) {
            if (!(raw instanceof JSONObject rule) || rule.getLong("deviceId") == null)
                throw invalid("现有参数缺少设备身份");
            JSONObject definition = new JSONObject(true);
            for (String key : FIELDS) definition.put(key, rule.get(key));
            byDevice.computeIfAbsent(rule.getLong("deviceId"), ignored -> new JSONArray()).add(definition);
        }
        Map<String, JSONArray> parameters = new LinkedHashMap<>();
        Map<String, List<Long>> devices = new LinkedHashMap<>();
        for (Map.Entry<Long, JSONArray> entry : byDevice.entrySet()) {
            JSONArray normalized = normalize(entry.getValue());
            String current = fingerprint(config.getLong("processId"), normalized);
            parameters.putIfAbsent(current, normalized);
            devices.computeIfAbsent(current, ignored -> new ArrayList<>()).add(entry.getKey());
        }
        return parameters.entrySet().stream()
                .map(entry -> new Extracted(entry.getValue(), List.copyOf(devices.get(entry.getKey())))).toList();
    }

    /** 绑定单个工艺时仍要求原设备参数一致，不扩大已有填写范围。 */
    public static Extracted extract(JSONObject config) {
        List<Extracted> schemes = extractSchemes(config);
        if (schemes.isEmpty()) return new Extracted(new JSONArray(), List.of());
        if (schemes.size() != 1)
            throw invalid("同一工序节点各设备的参数配置不同，不能无损合并为一个工艺");
        return schemes.get(0);
    }

    public static List<Long> devices(JSONObject config) {
        JSONArray groups = config.getJSONArray("deviceSelectionGroups");
        if (groups == null) throw invalid("工序缺少正式设备配置");
        Set<Long> ids = new TreeSet<>();
        for (Object raw : groups) {
            JSONObject group = (JSONObject) raw;
            JSONArray members = group.getJSONArray("deviceIds");
            if (members == null) throw invalid("设备组缺少设备身份");
            for (Object value : members) {
                Long id = Long.valueOf(value.toString());
                if (id <= 0 || !ids.add(id)) throw invalid("工序设备身份无效或重复");
            }
        }
        return List.copyOf(ids);
    }

    public static JSONArray expand(JSONObject config, JSONArray parameters, Collection<Long> deviceIds) {
        Set<Long> available = new HashSet<>(devices(config));
        if (deviceIds.isEmpty() || !available.containsAll(deviceIds)) throw invalid("工艺缺少有效的执行设备关联");
        JSONArray result = new JSONArray();
        for (Long deviceId : deviceIds) {
            for (Object raw : normalize(parameters)) {
                JSONObject rule = JSON.parseObject(((JSONObject) raw).toJSONString());
                rule.put("routeProcessId", config.getLong("routeProcessId"));
                rule.put("processId", config.getLong("processId"));
                rule.put("deviceId", deviceId);
                result.add(rule);
            }
        }
        return result;
    }
    public static void bind(JSONObject config, Long craftId, String craftCode, JSONArray parameters,
                            Collection<Long> deviceIds) {
        bindAll(config, List.of(new Binding(craftId, craftCode, parameters, List.copyOf(deviceIds))));
    }
    /** 原主工艺字段继续表示第一项，附加工艺逐项保存自己的执行设备；工艺库仍无设备信息。 */
    public static List<JSONObject> bindings(JSONObject config) {
        List<JSONObject> result = new ArrayList<>();
        if (config.getLong("craftId") != null) result.add(config);
        JSONArray additional = config.getJSONArray("additionalCraftBindings");
        if (additional != null && !additional.isEmpty()) {
            if (result.isEmpty()) throw invalid("附加工艺缺少主工艺关联");
            for (Object raw : additional) {
                if (!(raw instanceof JSONObject binding) || binding.getLong("craftId") == null)
                    throw invalid("附加工艺关联格式无效");
                result.add(binding);
            }
        }
        return result;
    }
    public static boolean usesCraft(JSONObject config, Long id) {
        return bindings(config).stream().anyMatch(b -> Objects.equals(id, b.getLong("craftId")));
    }
    public static void bindAll(JSONObject config, List<Binding> values) {
        if (values == null || values.isEmpty()) throw invalid("至少选择一个工艺");
        Set<Long> craftIds = new HashSet<>(), deviceIds = new HashSet<>();
        JSONArray rules = new JSONArray(), metadata = new JSONArray();
        for (Binding binding : values) {
            if (binding.craftId() == null || !craftIds.add(binding.craftId())) throw invalid("工艺不能重复选择");
            for (Long deviceId : binding.deviceIds()) {
                if (!deviceIds.add(deviceId)) throw invalid("同一设备不能重复分配到多个工艺");
            }
            rules.addAll(expand(config, binding.parameters(), binding.deviceIds()));
            JSONObject item = new JSONObject(true);
            item.put("craftId", binding.craftId()); item.put("craftCode", binding.craftCode());
            item.put("craftFingerprint", fingerprint(config.getLong("processId"), binding.parameters()));
            item.put("craftDeviceIds", new JSONArray(new ArrayList<>(binding.deviceIds())));
            metadata.add(item);
        }
        config.putAll(metadata.getJSONObject(0));
        metadata.remove(0);
        if (metadata.isEmpty()) config.remove("additionalCraftBindings");
        else config.put("additionalCraftBindings", metadata);
        config.put("parameterRules", rules);
    }
    /** 升级某一工艺时只重展该工艺原设备的参数，其他工艺及设备保持原样。 */
    public static void replaceCraft(JSONObject config, Long craftId, String code, JSONArray parameters) {
        JSONObject binding = bindings(config).stream().filter(b -> Objects.equals(craftId, b.getLong("craftId")))
                .findFirst().orElseThrow(() -> invalid("流程未关联目标工艺"));
        List<Long> ids = binding.getJSONArray("craftDeviceIds").toJavaList(Long.class);
        JSONArray rules = new JSONArray();
        for (Object raw : config.getJSONArray("parameterRules")) {
            if (!ids.contains(((JSONObject) raw).getLong("deviceId"))) rules.add(raw);
        }
        rules.addAll(expand(config, parameters, ids));
        binding.put("craftCode", code);
        binding.put("craftFingerprint", fingerprint(config.getLong("processId"), parameters));
        config.put("parameterRules", rules);
    }
    public record Binding(Long craftId, String craftCode, JSONArray parameters, List<Long> deviceIds) {}
    public static String canonical(Object value) { return CANONICAL.canonicalize(JSON.toJSONString(value)); }
    public record Extracted(JSONArray parameters, List<Long> deviceIds) {}
}
