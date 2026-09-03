package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Synchronizes an explicitly imported recognition JSON into the formal team-leader configuration. */
@Service
public class MesProBatchRecordRecognitionDeviceSyncService {

    private final MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProProcessMapper processMapper;
    private final MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    private final MesProcessPoolTeamDeviceMapper deviceMapper;
    private final MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;

    public MesProBatchRecordRecognitionDeviceSyncService(
            MesRouteDccProjectBindingMapper routeDccProjectBindingMapper,
            MesProRouteProcessMapper routeProcessMapper,
            MesProProcessMapper processMapper,
            MesProcessPoolTeamProcessDeviceMapper processDeviceMapper,
            MesProcessPoolTeamDeviceMapper deviceMapper,
            MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper) {
        this.routeDccProjectBindingMapper = routeDccProjectBindingMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.processMapper = processMapper;
        this.processDeviceMapper = processDeviceMapper;
        this.deviceMapper = deviceMapper;
        this.parameterRuleMapper = parameterRuleMapper;
    }

    public void sync(Long dccProjectCodeId, String totalRecognitionJson) {
        JSONObject root = JSON.parseObject(totalRecognitionJson);
        if (root == null || root.getIntValue("schemaVersion") != 2 || root.getJSONArray("processes") == null) {
            throw new IllegalArgumentException("批记录总识别 JSON 必须是 schemaVersion=2 且包含 processes");
        }
        Long routeId = requireSingleRoute(dccProjectCodeId);
        Map<String, MesProRouteProcessDO> routeProcesses = routeProcessesByName(routeId);
        Long leaderUserId = requireSingleLeader(routeProcesses.values());
        for (Object rawProcess : root.getJSONArray("processes")) {
            JSONObject process = asObject(rawProcess, "processes[]");
            String processName = requireText(process, "name");
            MesProRouteProcessDO routeProcess = routeProcesses.get(processName);
            if (routeProcess == null) {
                throw new IllegalArgumentException("JSON 工序未绑定当前路线：" + processName);
            }
            syncProcess(routeProcess, leaderUserId, process.getJSONArray("equipmentGroups"));
        }
    }

    private Long requireSingleLeader(java.util.Collection<MesProRouteProcessDO> routeProcesses) {
        Set<Long> processIds = routeProcesses.stream().map(MesProRouteProcessDO::getProcessId)
                .filter(id -> id != null).collect(java.util.stream.Collectors.toSet());
        Set<Long> leaderUserIds = processDeviceMapper.selectList(
                        new LambdaQueryWrapperX<MesProcessPoolTeamProcessDeviceDO>()
                                .in(MesProcessPoolTeamProcessDeviceDO::getProcessId, processIds)
                                .eq(MesProcessPoolTeamProcessDeviceDO::getEnabled, Boolean.TRUE))
                .stream().map(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId)
                .filter(id -> id != null).collect(java.util.stream.Collectors.toSet());
        if (leaderUserIds.size() != 1) {
            throw new IllegalArgumentException("项目路线缺少唯一正式生产组长设备作用域，当前数量="
                    + leaderUserIds.size());
        }
        return leaderUserIds.iterator().next();
    }

    private Long requireSingleRoute(Long dccProjectCodeId) {
        List<Long> routeIds = routeDccProjectBindingMapper.selectCurrentListByDccProjectCodeId(dccProjectCodeId).stream()
                .map(binding -> binding.getRouteId()).filter(id -> id != null).distinct().toList();
        if (routeIds.size() != 1) {
            throw new IllegalArgumentException("项目代码必须且只能绑定一条正式工艺路线，当前数量=" + routeIds.size());
        }
        return routeIds.get(0);
    }

    private Map<String, MesProRouteProcessDO> routeProcessesByName(Long routeId) {
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        Map<Long, MesProProcessDO> processes = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            processes.put(routeProcess.getProcessId(), processMapper.selectById(routeProcess.getProcessId()));
        }
        Map<String, MesProRouteProcessDO> result = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            MesProProcessDO process = processes.get(routeProcess.getProcessId());
            if (process == null || process.getName() == null || result.put(process.getName(), routeProcess) != null) {
                throw new IllegalArgumentException("当前路线工序名称不唯一或缺失：routeId=" + routeId);
            }
        }
        return result;
    }

    private void syncProcess(MesProRouteProcessDO routeProcess, Long leaderUserId, JSONArray equipmentGroups) {
        Map<Long, MesProcessPoolTeamProcessDeviceDO> bindingsByDevice = processDeviceMapper.selectList(
                        new LambdaQueryWrapperX<MesProcessPoolTeamProcessDeviceDO>()
                                .eq(MesProcessPoolTeamProcessDeviceDO::getProcessId, routeProcess.getProcessId())
                                .eq(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId, leaderUserId))
                .stream().collect(java.util.stream.Collectors.toMap(MesProcessPoolTeamProcessDeviceDO::getDeviceId,
                        binding -> binding, (left, ignored) -> left, LinkedHashMap::new));
        Map<String, MesProcessPoolDeviceParameterRuleDO> rulesByKey = parameterRuleMapper.selectList(
                        new LambdaQueryWrapperX<MesProcessPoolDeviceParameterRuleDO>()
                                .eq(MesProcessPoolDeviceParameterRuleDO::getRouteProcessId, routeProcess.getId())
                                .eq(MesProcessPoolDeviceParameterRuleDO::getLeaderUserId, leaderUserId))
                .stream().collect(java.util.stream.Collectors.toMap(
                        rule -> rule.getDeviceId() + "|" + rule.getParameterCode(),
                        rule -> rule, (left, ignored) -> left, LinkedHashMap::new));
        disableExistingProcessConfiguration(routeProcess, leaderUserId);
        if (equipmentGroups == null) {
            return;
        }
        for (int groupIndex = 0; groupIndex < equipmentGroups.size(); groupIndex++) {
            Object rawGroup = equipmentGroups.get(groupIndex);
            JSONObject group = asObject(rawGroup, "equipmentGroups[]");
            String selectionMode = requireSelectionMode(group);
            String groupKey = "JSON-" + routeProcess.getId() + "-" + (groupIndex + 1);
            List<JSONObject> parameters = arrayObjects(group.getJSONArray("parameters"), "parameters");
            for (JSONObject equipment : arrayObjects(group.getJSONArray("equipmentOptions"), "equipmentOptions")) {
                Long deviceId = resolveOrCreateDevice(leaderUserId, equipment);
                MesProcessPoolTeamProcessDeviceDO binding = bindingsByDevice.get(deviceId);
                if (binding == null) {
                    processDeviceMapper.insert(MesProcessPoolTeamProcessDeviceDO.builder()
                            .leaderUserId(leaderUserId).processId(routeProcess.getProcessId()).deviceId(deviceId)
                            .deviceGroupKey(groupKey).selectionMode(selectionMode)
                            .enabled(true).build());
                } else {
                    processDeviceMapper.updateById(binding.setDeviceGroupKey(groupKey)
                            .setSelectionMode(selectionMode).setEnabled(true).setDisabledAt(null));
                }
                for (JSONObject parameter : parameters) {
                    MesProcessPoolDeviceParameterRuleDO desired = toParameterRule(
                            routeProcess, leaderUserId, deviceId, parameter);
                    MesProcessPoolDeviceParameterRuleDO existing = rulesByKey.get(
                            deviceId + "|" + desired.getParameterCode());
                    if (existing == null) {
                        parameterRuleMapper.insert(desired);
                    } else {
                        desired.setId(existing.getId());
                        parameterRuleMapper.updateById(desired);
                    }
                }
            }
        }
    }

    private void disableExistingProcessConfiguration(MesProRouteProcessDO routeProcess, Long leaderUserId) {
        processDeviceMapper.update(null, new LambdaUpdateWrapper<MesProcessPoolTeamProcessDeviceDO>()
                .set(MesProcessPoolTeamProcessDeviceDO::getEnabled, false)
                .eq(MesProcessPoolTeamProcessDeviceDO::getProcessId, routeProcess.getProcessId())
                .eq(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolTeamProcessDeviceDO::getEnabled, Boolean.TRUE));
        parameterRuleMapper.update(null, new LambdaUpdateWrapper<MesProcessPoolDeviceParameterRuleDO>()
                .set(MesProcessPoolDeviceParameterRuleDO::getEnabled, false)
                .eq(MesProcessPoolDeviceParameterRuleDO::getRouteProcessId, routeProcess.getId())
                .eq(MesProcessPoolDeviceParameterRuleDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolDeviceParameterRuleDO::getEnabled, Boolean.TRUE));
    }

    private Long resolveOrCreateDevice(Long leaderUserId, JSONObject equipment) {
        String code = requireText(equipment, "code");
        String name = requireText(equipment, "name");
        List<MesProcessPoolTeamDeviceDO> matches = deviceMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamDeviceDO>()
                .eq(MesProcessPoolTeamDeviceDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolTeamDeviceDO::getDeviceCode, code));
        if (matches.size() > 1) {
            throw new IllegalArgumentException("正式设备编码不唯一：" + code);
        }
        if (!matches.isEmpty()) {
            MesProcessPoolTeamDeviceDO existing = matches.get(0);
            if (!Boolean.TRUE.equals(existing.getEnabled()) || !"ENABLED".equals(existing.getDeviceStatus())) {
                throw new IllegalArgumentException("正式设备不可用：" + code);
            }
            if (!name.equals(existing.getDeviceName())) {
                deviceMapper.updateById(existing.setDeviceName(name));
            }
            return existing.getId();
        }
        MesProcessPoolTeamDeviceDO device = MesProcessPoolTeamDeviceDO.builder().leaderUserId(leaderUserId)
                .deviceCode(code).deviceName(name).deviceStatus("ENABLED").enabled(true).build();
        deviceMapper.insert(device);
        return device.getId();
    }

    private MesProcessPoolDeviceParameterRuleDO toParameterRule(MesProRouteProcessDO routeProcess, Long leaderUserId,
                                                                  Long deviceId, JSONObject parameter) {
        String name = requireText(parameter, "name");
        String referenceValue = requireText(parameter, "referenceValue");
        JSONObject ui = asObject(parameter.get("ui"), "parameters[].ui");
        String control = requireText(ui, "control");
        MesProcessPoolDeviceParameterRuleDO rule = MesProcessPoolDeviceParameterRuleDO.builder()
                .leaderUserId(leaderUserId).routeProcessId(routeProcess.getId()).processId(routeProcess.getProcessId())
                .deviceId(deviceId).parameterCode(parameterCode(name)).parameterName(ui.getString("displayName") == null
                        ? name : ui.getString("displayName")).unit(ui.getString("unit")).standardText(referenceValue)
                .enabled(true).build();
        if ("number".equals(control)) {
            BigDecimal defaultValue = decimal(ui, "defaultValue", true);
            rule.setDefaultValue(defaultValue).setLowerLimit(decimal(ui, "min", false))
                    .setUpperLimit(decimal(ui, "max", false)).setDecimalScale(scale(ui, "step"))
                    .setValueType(defaultValue.scale() <= 0 ? MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER
                            : MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL);
            return rule;
        }
        if ("select".equals(control)) {
            List<String> options = ui.getJSONArray("options").toJavaList(String.class);
            if (options.isEmpty() || !options.contains(requireText(ui, "defaultValue"))) {
                throw new IllegalArgumentException("下拉参数选项或默认值无效：" + name);
            }
            return rule.setValueType(MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_SELECT)
                    .setOptionValuesJson(JsonUtils.toJsonString(options)).setDefaultText(ui.getString("defaultValue"));
        }
        if ("text".equals(control)) {
            return rule.setValueType(MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD)
                    .setDefaultText(ui.getString("defaultValue"));
        }
        throw new IllegalArgumentException("不支持的参数控件类型：" + control);
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

    private static String parameterCode(String parameterName) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(parameterName.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("JSON_");
            for (int index = 0; index < 8; index++) result.append(String.format("%02X", digest[index]));
            return result.toString();
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
