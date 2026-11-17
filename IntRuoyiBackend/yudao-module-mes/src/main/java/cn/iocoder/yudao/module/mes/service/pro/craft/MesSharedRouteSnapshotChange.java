package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftDO;
import com.alibaba.fastjson.*;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.*;

/** 只修改候选快照中的明确引用，不读取或改写订单、执行记录。 */
public final class MesSharedRouteSnapshotChange {
    private MesSharedRouteSnapshotChange() {}

    public static List<JSONObject> configurations(JSONObject root) {
        JSONObject data = root.getJSONObject("configSnapshots");
        JSONArray configs = data == null ? null : data.getJSONArray("productionProcessConfigs");
        return configs == null ? List.of() : configs.toJavaList(JSONObject.class);
    }

    public static List<JSONObject> nodes(JSONObject root) {
        JSONObject data = root.getJSONObject("configSnapshots");
        JSONObject graph = data == null ? null : data.getJSONObject("flowGraph");
        JSONArray nodes = graph == null ? null : graph.getJSONArray("nodes");
        if (nodes == null) throw invalid("流程缺少工序节点快照");
        return nodes.toJavaList(JSONObject.class);
    }

    public static boolean uses(JSONObject root, String type, Long sourceId, Set<Long> deviceIds) {
        return switch (type) {
            case "PROCESS" -> nodes(root).stream().anyMatch(n -> Objects.equals(sourceId, n.getLong("processId")));
            case "CRAFT" -> configurations(root).stream().anyMatch(c -> usesCraft(c, sourceId));
            case "DEVICE" -> configurations(root).stream().anyMatch(c -> devices(c).stream().anyMatch(deviceIds::contains));
            default -> throw invalid("变更对象类型无效");
        };
    }

    /** 保持节点身份、连线、逐工序批记录和表单槽位；只更新工序身份及其展示字段。 */
    public static void process(JSONObject root, Long sourceId, JSONObject target) {
        rewriteProcess(root, sourceId, target);
    }

    private static void rewriteProcess(Object value, Long sourceId, JSONObject target) {
        if (value instanceof JSONObject object) {
            if (Objects.equals(sourceId, object.getLong("processId"))) {
                object.put("processId", target.getLong("id"));
                if (object.containsKey("processName")) object.put("processName", target.getString("name"));
                if (object.containsKey("processCode")) object.put("processCode", target.getString("code"));
                if (object.containsKey("attention")) object.put("attention", target.get("attention"));
                if (object.containsKey("sopFileUrl")) object.put("sopFileUrl", target.get("sopFileUrl"));
            }
            for (Object child : object.values()) rewriteProcess(child, sourceId, target);
        } else if (value instanceof JSONArray array) {
            for (Object child : array) rewriteProcess(child, sourceId, target);
        }
    }

    public static void craft(JSONObject config, Long sourceId, MesProCraftDO target) {
        if (!Objects.equals(config.getLong("processId"), target.getProcessId()))
            throw invalid("替换工艺必须属于当前工序");
        if (!Objects.equals(sourceId, target.getId()) && usesCraft(config, target.getId()))
            throw invalid("该节点已使用目标工艺，请先合并工艺设备关联");
        JSONObject binding = bindings(config).stream().filter(b -> Objects.equals(sourceId, b.getLong("craftId")))
                .findFirst().orElseThrow(() -> invalid("工艺引用已变化"));
        replaceCraft(config, sourceId, target.getCode(), JSON.parseArray(target.getParametersJson()));
        binding.put("craftId", target.getId());
    }

    /** 更换工序时，调用方提供用户确认的完整工艺设备关联，不能沿用原工序的参数归属。 */
    public static void processBindings(JSONObject config, List<Binding> selected) {
        Set<Long> requiredDevices = new HashSet<>();
        for (Object raw : config.getJSONArray("parameterRules"))
            requiredDevices.add(((JSONObject) raw).getLong("deviceId"));
        Set<Long> selectedDevices = new HashSet<>();
        selected.forEach(b -> selectedDevices.addAll(b.deviceIds()));
        if (!selectedDevices.equals(requiredDevices)) throw invalid("请为原有参数的全部设备选择新工序工艺");
        if (!selected.isEmpty()) bindAll(config, selected);
        else {
            if (!bindings(config).isEmpty()) throw invalid("原工艺关联缺少参数，无法替换工序");
            config.put("parameterRules", new JSONArray());
        }
    }

    /** B 的参数必须显式选择；允许明确确认无参数。其他设备参数及设备组原样保留。 */
    public static void device(JSONObject config, Set<Long> sourceIds, Long targetId,
                              MesProCraftDO targetCraft, boolean withoutParameters) {
        List<Long> previousDevices = devices(config);
        Set<Long> replaced = new HashSet<>(previousDevices);
        replaced.retainAll(sourceIds);
        if (replaced.isEmpty()) return;
        if (replaced.size() != 1) throw invalid("同一工序存在多个原设备身份，请先消除重复设备");
        Long sourceId = replaced.iterator().next();
        if (previousDevices.contains(targetId)) throw invalid("目标设备已在该工序使用");
        if (targetCraft == null && !withoutParameters) throw invalid("请选择新设备的工艺，或明确确认无参数");
        if (targetCraft != null && withoutParameters) throw invalid("新设备的参数选择冲突");
        if (targetCraft != null && !Objects.equals(targetCraft.getProcessId(), config.getLong("processId")))
            throw invalid("新设备所用工艺不属于当前工序");

        List<JSONObject> previousBindings = new ArrayList<>(bindings(config));
        JSONArray remainingRules = new JSONArray();
        for (Object raw : config.getJSONArray("parameterRules")) {
            JSONObject rule = (JSONObject) raw;
            if (!Objects.equals(sourceId, rule.getLong("deviceId"))) remainingRules.add(raw);
        }
        for (Object raw : config.getJSONArray("deviceSelectionGroups")) {
            JSONArray ids = ((JSONObject) raw).getJSONArray("deviceIds");
            for (int i = 0; i < ids.size(); i++) if (Objects.equals(sourceId, ids.getLong(i))) ids.set(i, targetId);
        }

        JSONArray metadata = new JSONArray();
        for (JSONObject previous : previousBindings) {
            List<Long> ids = new ArrayList<>(previous.getJSONArray("craftDeviceIds").toJavaList(Long.class));
            ids.remove(sourceId);
            if (ids.isEmpty()) continue;
            JSONObject binding = new JSONObject(true);
            for (String key : List.of("craftId", "craftCode", "craftFingerprint")) binding.put(key, previous.get(key));
            binding.put("craftDeviceIds", new JSONArray(new ArrayList<>(ids)));
            metadata.add(binding);
        }
        if (targetCraft != null) {
            JSONArray parameters = JSON.parseArray(targetCraft.getParametersJson());
            JSONObject binding = metadata.toJavaList(JSONObject.class).stream()
                    .filter(b -> Objects.equals(targetCraft.getId(), b.getLong("craftId"))).findFirst().orElse(null);
            if (binding == null) {
                binding = new JSONObject(true);
                binding.put("craftId", targetCraft.getId()); binding.put("craftCode", targetCraft.getCode());
                binding.put("craftFingerprint", fingerprint(config.getLong("processId"), parameters));
                binding.put("craftDeviceIds", new JSONArray()); metadata.add(binding);
            } else if (!Objects.equals(binding.getString("craftFingerprint"), fingerprint(config.getLong("processId"), parameters))) {
                throw invalid("新设备所选工艺与节点现有工艺版本不一致");
            }
            binding.getJSONArray("craftDeviceIds").add(targetId);
            remainingRules.addAll(expand(config, parameters, List.of(targetId)));
        }
        for (String key : List.of("craftId", "craftCode", "craftFingerprint", "craftDeviceIds", "additionalCraftBindings")) config.remove(key);
        if (!metadata.isEmpty()) {
            config.putAll(metadata.getJSONObject(0)); metadata.remove(0);
            if (!metadata.isEmpty()) config.put("additionalCraftBindings", metadata);
        }
        config.put("parameterRules", remainingRules);
    }
}
