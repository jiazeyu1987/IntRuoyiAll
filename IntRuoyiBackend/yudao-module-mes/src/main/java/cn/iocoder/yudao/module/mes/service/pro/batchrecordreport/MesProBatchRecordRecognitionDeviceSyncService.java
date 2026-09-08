package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteCandidateConfigService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionLifecycleServiceImpl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Synchronizes an explicitly imported recognition JSON into the candidate route version production configuration. */
@Service
public class MesProBatchRecordRecognitionDeviceSyncService {

    private static final String PRODUCTION_PROCESS_CONFIG_SCHEMA_VERSION_KEY = "productionProcessConfigSchemaVersion";
    private static final String PRODUCTION_PROCESS_CONFIGS_KEY = "productionProcessConfigs";

    private final MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProProcessMapper processMapper;
    private final MesProcessPoolTeamDeviceMapper deviceMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProRouteCandidateConfigService candidateConfigService;

    public MesProBatchRecordRecognitionDeviceSyncService(
            MesRouteDccProjectBindingMapper routeDccProjectBindingMapper,
            MesProRouteProcessMapper routeProcessMapper,
            MesProProcessMapper processMapper,
            MesProcessPoolTeamDeviceMapper deviceMapper,
            MesProRouteVersionMapper routeVersionMapper,
            MesProRouteCandidateConfigService candidateConfigService) {
        this.routeDccProjectBindingMapper = routeDccProjectBindingMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.processMapper = processMapper;
        this.deviceMapper = deviceMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.candidateConfigService = candidateConfigService;
    }

    public void sync(Long dccProjectCodeId, String totalRecognitionJson) {
        JSONObject root = JSON.parseObject(totalRecognitionJson);
        if (root == null || root.getIntValue("schemaVersion") != 3 || root.getJSONArray("processes") == null) {
            throw new IllegalArgumentException("批记录总识别 JSON 必须是 schemaVersion=3 且包含 processes");
        }
        Long routeId = requireSingleRoute(dccProjectCodeId);
        Map<Long, MesProRouteProcessDO> routeProcesses = routeProcessesById(routeId);
        Map<Long, String> processNames = processNames(routeProcesses.values().stream()
                .map(MesProRouteProcessDO::getProcessId).distinct().toList());
        MesProRouteVersionDO candidate = routeVersionMapper.selectOpenCandidateByRouteId(routeId);
        if (candidate == null || !MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(
                candidate.getLifecycleStatus())) {
            throw new IllegalArgumentException("批记录识别设备参数同步需要当前工艺路线存在草稿候选版本：routeId=" + routeId);
        }
        JSONArray productionConfigs = readExistingProductionConfigs(candidate);
        Map<Long, JSONObject> configsByRouteProcessId = productionConfigsByRouteProcessId(productionConfigs);
        Map<Long, JSONObject> importedByRouteProcessId = new LinkedHashMap<>();
        for (Object rawProcess : root.getJSONArray("processes")) {
            JSONObject process = asObject(rawProcess, "processes[]");
            Long routeProcessId = requireLong(process, "routeProcessId");
            MesProRouteProcessDO routeProcess = routeProcesses.get(routeProcessId);
            if (routeProcess == null) {
                throw new IllegalArgumentException("JSON 路线工序未绑定当前路线：routeProcessId=" + routeProcessId);
            }
            if (importedByRouteProcessId.put(routeProcessId, process) != null) {
                throw new IllegalArgumentException("JSON 路线工序重复：routeProcessId=" + routeProcessId);
            }
        }
        if (!importedByRouteProcessId.keySet().equals(routeProcesses.keySet())) {
            Set<Long> missing = new HashSet<>(routeProcesses.keySet());
            missing.removeAll(importedByRouteProcessId.keySet());
            Set<Long> extra = new HashSet<>(importedByRouteProcessId.keySet());
            extra.removeAll(routeProcesses.keySet());
            throw new IllegalArgumentException("JSON 必须覆盖当前路线全部工序且恰好一次：missing="
                    + missing + ", extra=" + extra);
        }
        for (Map.Entry<Long, MesProRouteProcessDO> entry : routeProcesses.entrySet()) {
            MesProRouteProcessDO routeProcess = entry.getValue();
            JSONObject process = importedByRouteProcessId.get(entry.getKey());
            configsByRouteProcessId.put(routeProcess.getId(), buildProductionProcessConfig(
                    routeProcess, processNames.get(routeProcess.getProcessId()), process.getJSONArray("equipmentGroups"),
                    configsByRouteProcessId.get(routeProcess.getId())));
        }
        candidateConfigService.saveConfigSnapshots(candidate.getId(), Map.of(
                PRODUCTION_PROCESS_CONFIG_SCHEMA_VERSION_KEY, 1,
                PRODUCTION_PROCESS_CONFIGS_KEY, new JSONArray(new ArrayList<>(configsByRouteProcessId.values()))));
    }

    private JSONArray readExistingProductionConfigs(MesProRouteVersionDO candidate) {
        JSONObject routeSnapshot = JSON.parseObject(candidate.getRouteSnapshotJson());
        JSONObject configSnapshots = routeSnapshot == null ? null : routeSnapshot.getJSONObject("configSnapshots");
        JSONArray productionConfigs = configSnapshots == null ? null
                : configSnapshots.getJSONArray(PRODUCTION_PROCESS_CONFIGS_KEY);
        return productionConfigs == null ? new JSONArray() : productionConfigs;
    }

    private Map<Long, JSONObject> productionConfigsByRouteProcessId(JSONArray productionConfigs) {
        Map<Long, JSONObject> result = new LinkedHashMap<>();
        for (Object rawConfig : productionConfigs) {
            JSONObject config = asObject(rawConfig, PRODUCTION_PROCESS_CONFIGS_KEY + "[]");
            Long routeProcessId = config.getLong("routeProcessId");
            if (routeProcessId == null || result.put(routeProcessId, config) != null) {
                throw new IllegalArgumentException("路线候选版本生产配置 routeProcessId 缺失或重复：" + routeProcessId);
            }
        }
        return result;
    }

    private JSONObject buildProductionProcessConfig(MesProRouteProcessDO routeProcess, String processName,
                                                    JSONArray equipmentGroups, JSONObject existingConfig) {
        JSONObject config = existingConfig == null ? new JSONObject(true) : new JSONObject(existingConfig);
        config.put("routeProcessId", routeProcess.getId());
        config.put("processId", routeProcess.getProcessId());
        config.put("sort", routeProcess.getSort());
        config.put("processName", processName);
        if (!config.containsKey("lossReasons")) {
            config.put("lossReasons", new JSONArray());
        }
        if (!config.containsKey("overagePercent")) {
            config.put("overagePercent", null);
        }
        JSONArray deviceSelectionGroups = new JSONArray();
        JSONArray parameterRules = new JSONArray();
        if (equipmentGroups != null) {
            Set<String> groupKeys = new HashSet<>();
            for (int groupIndex = 0; groupIndex < equipmentGroups.size(); groupIndex++) {
                JSONObject group = asObject(equipmentGroups.get(groupIndex), "equipmentGroups[]");
                String selectionMode = requireSelectionMode(group);
                String groupKey = requireText(group, "deviceGroupKey");
                if (!groupKeys.add(groupKey)) {
                    throw new IllegalArgumentException("JSON 设备组键重复：" + groupKey);
                }
                Integer groupSort = requireInteger(group, "sort");
                JSONArray deviceIds = new JSONArray();
                List<JSONObject> parameters = arrayObjects(group.getJSONArray("parameters"), "parameters");
                for (JSONObject equipment : arrayObjects(group.getJSONArray("equipmentOptions"), "equipmentOptions")) {
                    Long deviceId = requireExistingDeviceId(equipment);
                    deviceIds.add(deviceId);
                    for (JSONObject parameter : parameters) {
                        parameterRules.add(toParameterRuleSnapshot(routeProcess, deviceId, parameter));
                    }
                }
                JSONObject selectionGroup = new JSONObject(true);
                selectionGroup.put("deviceGroupKey", groupKey);
                selectionGroup.put("selectionMode", selectionMode);
                selectionGroup.put("deviceIds", deviceIds);
                selectionGroup.put("sort", groupSort);
                deviceSelectionGroups.add(selectionGroup);
            }
        }
        config.put("deviceSelectionGroups", deviceSelectionGroups);
        config.put("parameterRules", parameterRules);
        return config;
    }

    private Long requireExistingDeviceId(JSONObject equipment) {
        String code = requireText(equipment, "code");
        List<MesProcessPoolTeamDeviceDO> matches = deviceMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolTeamDeviceDO>()
                        .eq(MesProcessPoolTeamDeviceDO::getDeviceCode, code));
        if (matches.size() != 1) {
            throw new IllegalArgumentException("JSON 设备必须匹配唯一现有设备：" + code + "，当前数量=" + matches.size());
        }
        MesProcessPoolTeamDeviceDO device = matches.get(0);
        if (!Boolean.TRUE.equals(device.getEnabled()) || !"ENABLED".equals(device.getDeviceStatus())) {
            throw new IllegalArgumentException("JSON 设备不是启用状态：" + code);
        }
        return device.getId();
    }

    private JSONObject toParameterRuleSnapshot(MesProRouteProcessDO routeProcess, Long deviceId,
                                               JSONObject parameter) {
        String name = requireText(parameter, "name");
        String referenceValue = requireText(parameter, "referenceValue");
        JSONObject ui = asObject(parameter.get("ui"), "parameters[].ui");
        String control = requireText(ui, "control");
        JSONObject rule = new JSONObject(true);
        rule.put("routeProcessId", routeProcess.getId());
        rule.put("processId", routeProcess.getProcessId());
        rule.put("deviceId", deviceId);
        rule.put("parameterCode", requireText(parameter, "parameterCode"));
        rule.put("parameterName", ui.getString("displayName") == null ? name : ui.getString("displayName"));
        rule.put("unit", ui.getString("unit"));
        rule.put("standardText", referenceValue);
        rule.put("sort", requireInteger(parameter, "sort"));
        if ("number".equals(control)) {
            BigDecimal defaultValue = decimal(ui, "defaultValue", true);
            rule.put("defaultValue", defaultValue);
            rule.put("lowerLimit", decimal(ui, "min", false));
            rule.put("upperLimit", decimal(ui, "max", false));
            rule.put("decimalScale", scale(ui, "step"));
            rule.put("valueType", defaultValue.scale() <= 0
                    ? MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER
                    : MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL);
            return rule;
        }
        if ("select".equals(control)) {
            List<String> options = ui.getJSONArray("options").toJavaList(String.class);
            if (options.isEmpty() || !options.contains(requireText(ui, "defaultValue"))) {
                throw new IllegalArgumentException("下拉参数选项或默认值无效：" + name);
            }
            rule.put("valueType", MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_SELECT);
            rule.put("optionValuesJson", JsonUtils.toJsonString(options));
            rule.put("defaultText", ui.getString("defaultValue"));
            return rule;
        }
        if ("text".equals(control)) {
            rule.put("valueType", MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD);
            rule.put("defaultText", ui.getString("defaultValue"));
            return rule;
        }
        throw new IllegalArgumentException("不支持的参数控件类型：" + control);
    }

    private Long requireSingleRoute(Long dccProjectCodeId) {
        List<Long> routeIds = routeDccProjectBindingMapper.selectCurrentListByDccProjectCodeId(dccProjectCodeId).stream()
                .map(binding -> binding.getRouteId()).filter(id -> id != null).distinct().toList();
        if (routeIds.size() != 1) {
            throw new IllegalArgumentException("项目代码必须且只能绑定一条正式工艺路线，当前数量=" + routeIds.size());
        }
        return routeIds.get(0);
    }

    private Map<Long, MesProRouteProcessDO> routeProcessesById(Long routeId) {
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        Map<Long, MesProRouteProcessDO> result = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            if (routeProcess.getId() == null || routeProcess.getProcessId() == null
                    || result.put(routeProcess.getId(), routeProcess) != null) {
                throw new IllegalArgumentException("当前路线工序身份不唯一或缺失：routeId=" + routeId);
            }
        }
        return result;
    }

    private Map<Long, String> processNames(List<Long> processIds) {
        Map<Long, String> result = new LinkedHashMap<>();
        for (Long processId : processIds) {
            MesProProcessDO process = processMapper.selectById(processId);
            if (process == null || process.getName() == null || process.getName().isBlank()) {
                throw new IllegalArgumentException("当前路线工序基础工序名称缺失：processId=" + processId);
            }
            result.put(processId, process.getName().trim());
        }
        return result;
    }

    private static JSONObject asObject(Object value, String field) {
        if (!(value instanceof JSONObject object)) {
            throw new IllegalArgumentException("JSON 字段必须为对象：" + field);
        }
        return object;
    }

    private static List<JSONObject> arrayObjects(JSONArray values, String field) {
        if (values == null) return List.of();
        List<JSONObject> result = new ArrayList<>();
        for (Object value : values) result.add(asObject(value, field));
        return result;
    }

    private static String requireText(JSONObject value, String field) {
        String text = value.getString(field);
        if (text == null || text.isBlank()) throw new IllegalArgumentException("JSON 缺少字段：" + field);
        return text.trim();
    }

    private static Long requireLong(JSONObject value, String field) {
        Long number = value.getLong(field);
        if (number == null || number <= 0) throw new IllegalArgumentException("JSON 缺少正整数字段：" + field);
        return number;
    }

    private static Integer requireInteger(JSONObject value, String field) {
        Integer number = value.getInteger(field);
        if (number == null || number <= 0) throw new IllegalArgumentException("JSON 缺少正整数字段：" + field);
        return number;
    }

    private static String requireSelectionMode(JSONObject group) {
        String selectionMode = requireText(group, "selectionMode");
        if (!"SINGLE".equals(selectionMode) && !"MULTIPLE".equals(selectionMode)) {
            throw new IllegalArgumentException("不支持的设备选择模式：" + selectionMode);
        }
        return selectionMode;
    }

    private static BigDecimal decimal(JSONObject value, String field, boolean required) {
        Object raw = value.get(field);
        if (raw == null && !required) return null;
        if (raw == null) throw new IllegalArgumentException("JSON 缺少数值字段：" + field);
        try { return new BigDecimal(String.valueOf(raw)); } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("JSON 数值字段无效：" + field, ex);
        }
    }

    private static int scale(JSONObject value, String field) { return Math.max(0, decimal(value, field, true).stripTrailingZeros().scale()); }
}
