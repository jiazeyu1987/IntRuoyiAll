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

    /** 按设备拆分并比较；不同设备有不同参数时拒绝迁移，而不取并集。 */
    public static Extracted extract(JSONObject config) {
        JSONArray rules = config.getJSONArray("parameterRules");
        if (rules == null) throw invalid("现有工序缺少正式参数配置");
        if (rules.isEmpty()) return new Extracted(new JSONArray(), List.of());
        Map<Long, JSONArray> byDevice = new TreeMap<>();
        for (Object raw : rules) {
            if (!(raw instanceof JSONObject rule) || rule.getLong("deviceId") == null)
                throw invalid("现有参数缺少设备身份");
            JSONObject definition = new JSONObject(true);
            for (String key : FIELDS) definition.put(key, rule.get(key));
            byDevice.computeIfAbsent(rule.getLong("deviceId"), ignored -> new JSONArray()).add(definition);
        }
        JSONArray parameters = null;
        String expected = null;
        for (Map.Entry<Long, JSONArray> entry : byDevice.entrySet()) {
            JSONArray normalized = normalize(entry.getValue());
            String current = fingerprint(config.getLong("processId"), normalized);
            if (expected != null && !expected.equals(current))
                throw invalid("同一工序节点各设备的参数配置不同，不能无损合并为一个工艺");
            expected = current;
            parameters = normalized;
        }
        return new Extracted(parameters, List.copyOf(byDevice.keySet()));
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
        config.put("craftId", craftId);
        config.put("craftCode", craftCode);
        config.put("craftFingerprint", fingerprint(config.getLong("processId"), parameters));
        config.put("craftDeviceIds", new JSONArray(new ArrayList<>(deviceIds)));
        config.put("parameterRules", expand(config, parameters, deviceIds));
    }
    public static String canonical(Object value) { return CANONICAL.canonicalize(JSON.toJSONString(value)); }
    public record Extracted(JSONArray parameters, List<Long> deviceIds) {}
}
