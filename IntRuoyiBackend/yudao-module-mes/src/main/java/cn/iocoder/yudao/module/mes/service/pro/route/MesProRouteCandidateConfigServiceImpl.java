package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE;

/**
 * 工艺路线候选版本配置快照服务实现。
 */
@Service
@Validated
public class MesProRouteCandidateConfigServiceImpl implements MesProRouteCandidateConfigService {

    private static final String SNAPSHOT_CONFIGS_KEY = "configSnapshots";
    private static final String DEVICE_STATUS_ENABLED = "ENABLED";
    private static final Set<String> NUMERIC_PARAMETER_VALUE_TYPES = Set.of(
            MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER,
            MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL);

    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProcessPoolTeamDeviceMapper teamDeviceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfigSnapshot(Long candidateRouteVersionId, String configKey, Object configSnapshot) {
        if (StrUtil.isBlank(configKey) || configSnapshot == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidateRouteVersionId);
        }
        saveConfigSnapshots(candidateRouteVersionId, Map.of(configKey, configSnapshot));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfigSnapshots(Long candidateRouteVersionId, Map<String, Object> incomingConfigSnapshots) {
        saveConfigSnapshotsInternal(candidateRouteVersionId, null, incomingConfigSnapshots);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfigSnapshots(Long candidateRouteVersionId, String expectedRouteSnapshotSha256,
                                    Map<String, Object> incomingConfigSnapshots) {
        if (StrUtil.isBlank(expectedRouteSnapshotSha256)) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT, candidateRouteVersionId, null, null);
        }
        saveConfigSnapshotsInternal(candidateRouteVersionId, expectedRouteSnapshotSha256, incomingConfigSnapshots);
    }

    private void saveConfigSnapshotsInternal(Long candidateRouteVersionId, String expectedRouteSnapshotSha256,
                                             Map<String, Object> incomingConfigSnapshots) {
        MesProRouteVersionDO candidate = routeVersionMapper.selectById(candidateRouteVersionId);
        if (candidate == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, candidateRouteVersionId);
        }
        if (Boolean.TRUE.equals(candidate.getActive())
                || !MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        validateSourceActiveVersionStillCurrent(candidate);
        if (expectedRouteSnapshotSha256 != null
                && !Objects.equals(expectedRouteSnapshotSha256, candidate.getRouteSnapshotSha256())) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT, candidate.getRouteId(),
                    expectedRouteSnapshotSha256, candidate.getRouteSnapshotSha256());
        }
        if (incomingConfigSnapshots == null || incomingConfigSnapshots.isEmpty()
                || StrUtil.isBlank(candidate.getRouteSnapshotJson())) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidate.getId());
        }

        JSONObject snapshot = JSON.parseObject(candidate.getRouteSnapshotJson());
        if (snapshot == null || snapshot.isEmpty()) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidate.getId());
        }
        JSONObject configSnapshots = snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        if (configSnapshots == null) {
            configSnapshots = new JSONObject(true);
            snapshot.put(SNAPSHOT_CONFIGS_KEY, configSnapshots);
        }
        for (Map.Entry<String, Object> entry : incomingConfigSnapshots.entrySet()) {
            if (StrUtil.isBlank(entry.getKey()) || entry.getValue() == null) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidate.getId());
            }
            Object jsonSnapshot = JSON.parse(JSON.toJSONString(entry.getValue()));
            if (jsonSnapshot == null) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidate.getId());
            }
            configSnapshots.put(entry.getKey(), jsonSnapshot);
        }
        validateProductionProcessConfigs(candidate.getId(), configSnapshots);
        validateProductionDevicesExist(candidate.getId(), configSnapshots);

        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setActive(Boolean.FALSE);
        update.setLifecycleStatus(candidate.getLifecycleStatus());
        MesProRouteVersionSnapshotIdentityWriter.apply(update, snapshot.toJSONString());
        LambdaUpdateWrapper<MesProRouteVersionDO> updateWrapper = new LambdaUpdateWrapper<MesProRouteVersionDO>()
                .eq(MesProRouteVersionDO::getId, candidate.getId())
                .eq(MesProRouteVersionDO::getLifecycleStatus,
                        MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .eq(MesProRouteVersionDO::getActive, Boolean.FALSE);
        if (candidate.getRouteSnapshotSha256() == null) {
            updateWrapper.isNull(MesProRouteVersionDO::getRouteSnapshotSha256);
        } else {
            updateWrapper.eq(MesProRouteVersionDO::getRouteSnapshotSha256, candidate.getRouteSnapshotSha256());
        }
        if (routeVersionMapper.update(update, updateWrapper) != 1) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT, candidate.getRouteId(),
                    candidate.getRouteSnapshotSha256(), "changed concurrently");
        }
    }

    public static void validateProductionProcessConfigs(Long routeVersionId, JSONObject configSnapshots) {
        if (!configSnapshots.containsKey("productionProcessConfigSchemaVersion")
                && !configSnapshots.containsKey("productionProcessConfigs")) {
            return;
        }
        if (!Objects.equals(1, configSnapshots.getInteger("productionProcessConfigSchemaVersion"))) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
        }
        JSONObject flowGraph = configSnapshots.getJSONObject("flowGraph");
        JSONArray nodes = flowGraph == null ? null : flowGraph.getJSONArray("nodes");
        JSONArray configs = configSnapshots.getJSONArray("productionProcessConfigs");
        if (nodes == null || nodes.isEmpty() || configs == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
        }
        Map<Long, Long> expectedProcesses = new java.util.LinkedHashMap<>();
        for (Object rawNode : nodes) {
            JSONObject node = requireObject(rawNode, routeVersionId);
            Long routeProcessId = node.getLong("routeProcessId");
            Long processId = node.getLong("processId");
            if (routeProcessId == null || processId == null
                    || expectedProcesses.putIfAbsent(routeProcessId, processId) != null) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
        }
        Set<Long> actualRouteProcessIds = new LinkedHashSet<>();
        for (Object rawConfig : configs) {
            JSONObject config = requireObject(rawConfig, routeVersionId);
            Long routeProcessId = config.getLong("routeProcessId");
            Long processId = config.getLong("processId");
            BigDecimal overagePercent = config.getBigDecimal("overagePercent");
            if (!Objects.equals(expectedProcesses.get(routeProcessId), processId)
                    || !actualRouteProcessIds.add(routeProcessId)
                    || overagePercent == null
                    || overagePercent.compareTo(BigDecimal.ZERO) < 0
                    || overagePercent.compareTo(BigDecimal.valueOf(100)) > 0
                    || config.getJSONArray("lossReasons") == null
                    || config.getJSONArray("deviceSelectionGroups") == null
                    || config.getJSONArray("parameterRules") == null) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
            validateDeviceConfiguration(routeVersionId, config);
        }
        if (!actualRouteProcessIds.equals(expectedProcesses.keySet())) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
        }
    }

    private static void validateDeviceConfiguration(Long routeVersionId, JSONObject config) {
        Set<Long> deviceIds = new LinkedHashSet<>();
        Set<String> groupKeys = new LinkedHashSet<>();
        for (Object rawGroup : config.getJSONArray("deviceSelectionGroups")) {
            JSONObject group = requireObject(rawGroup, routeVersionId);
            String groupKey = group.getString("deviceGroupKey");
            String selectionMode = group.getString("selectionMode");
            JSONArray groupDeviceIds = group.getJSONArray("deviceIds");
            if (StrUtil.isBlank(groupKey) || !groupKeys.add(groupKey)
                    || (!"SINGLE".equals(selectionMode) && !"MULTIPLE".equals(selectionMode))
                    || groupDeviceIds == null || groupDeviceIds.isEmpty()) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
            for (Object rawDeviceId : groupDeviceIds) {
                Long deviceId;
                try {
                    deviceId = rawDeviceId == null ? null : Long.valueOf(String.valueOf(rawDeviceId));
                } catch (NumberFormatException ex) {
                    throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
                }
                if (deviceId == null || deviceId <= 0 || !deviceIds.add(deviceId)) {
                    throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
                }
            }
        }
        Set<String> parameterKeys = new LinkedHashSet<>();
        for (Object rawRule : config.getJSONArray("parameterRules")) {
            JSONObject rule = requireObject(rawRule, routeVersionId);
            Long deviceId = rule.getLong("deviceId");
            String parameterCode = cn.iocoder.yudao.module.mes.service.pro.processpool.team
                    .MesDeviceParameterSnapshotCodec.normalizeCode(rule.getString("parameterCode"));
            if (!deviceIds.contains(deviceId) || parameterCode == null
                    || !parameterKeys.add(deviceId + "|" + parameterCode)) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
            validateProductionParameterRule(routeVersionId, config, rule, deviceIds);
        }
    }

    private static void validateProductionParameterRule(Long routeVersionId, JSONObject config,
                                                        JSONObject rule, Set<Long> deviceIds) {
        Long deviceId = rule.getLong("deviceId");
        String valueType = rule.getString("valueType");
        BigDecimal lowerLimit = rule.getBigDecimal("lowerLimit");
        BigDecimal upperLimit = rule.getBigDecimal("upperLimit");
        BigDecimal defaultValue = rule.getBigDecimal("defaultValue");
        Integer decimalScale = rule.getInteger("decimalScale");
        String defaultText = StrUtil.trim(rule.getString("defaultText"));
        List<String> optionValues = parseParameterOptions(routeVersionId, rule.getString("optionValuesJson"));
        if (!deviceIds.contains(deviceId)
                || !Objects.equals(config.getLong("routeProcessId"), rule.getLong("routeProcessId"))
                || !Objects.equals(config.getLong("processId"), rule.getLong("processId"))
                || StrUtil.isBlank(rule.getString("parameterName"))
                || StrUtil.isBlank(valueType)
                || StrUtil.isBlank(rule.getString("standardText"))) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
        }
        if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD.equals(valueType)) {
            requireNoNumericContract(routeVersionId, lowerLimit, upperLimit, defaultValue, decimalScale, optionValues);
            return;
        }
        if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_SELECT.equals(valueType)) {
            if (lowerLimit != null || upperLimit != null || defaultValue != null || decimalScale != null
                    || optionValues.isEmpty() || StrUtil.isBlank(defaultText) || !optionValues.contains(defaultText)) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
            return;
        }
        if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_BOOLEAN.equals(valueType)) {
            boolean validDefault = defaultValue != null
                    && (defaultValue.compareTo(BigDecimal.ZERO) == 0
                    || defaultValue.compareTo(BigDecimal.ONE) == 0);
            if (!validDefault || lowerLimit != null || upperLimit != null || decimalScale != null
                    || !optionValues.isEmpty() || StrUtil.isNotBlank(defaultText)) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
            return;
        }
        if (!NUMERIC_PARAMETER_VALUE_TYPES.contains(valueType)
                || !optionValues.isEmpty() || StrUtil.isNotBlank(defaultText)
                || decimalScale != null && (decimalScale < 0 || decimalScale > 6)
                || MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER.equals(valueType)
                && decimalScale != null && decimalScale != 0
                || lowerLimit != null && upperLimit != null && lowerLimit.compareTo(upperLimit) > 0
                || defaultValue != null && lowerLimit != null && defaultValue.compareTo(lowerLimit) < 0
                || defaultValue != null && upperLimit != null && defaultValue.compareTo(upperLimit) > 0) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
        }
    }

    private static void requireNoNumericContract(Long routeVersionId, BigDecimal lowerLimit,
                                                 BigDecimal upperLimit, BigDecimal defaultValue,
                                                 Integer decimalScale, List<String> optionValues) {
        if (lowerLimit != null || upperLimit != null || defaultValue != null || decimalScale != null
                || !optionValues.isEmpty()) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
        }
    }

    private static List<String> parseParameterOptions(Long routeVersionId, String optionValuesJson) {
        if (StrUtil.isBlank(optionValuesJson)) {
            return List.of();
        }
        try {
            List<String> values = JSON.parseArray(optionValuesJson, String.class);
            if (values == null || values.stream().anyMatch(StrUtil::isBlank)) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
            List<String> normalized = values.stream().map(StrUtil::trim).toList();
            if (new LinkedHashSet<>(normalized).size() != normalized.size()) {
                throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
            }
            return normalized;
        } catch (RuntimeException ex) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
        }
    }

    private void validateProductionDevicesExist(Long routeVersionId, JSONObject configSnapshots) {
        JSONArray configs = configSnapshots.getJSONArray("productionProcessConfigs");
        if (configs == null) {
            return;
        }
        Set<Long> deviceIds = new LinkedHashSet<>();
        for (Object rawConfig : configs) {
            JSONObject config = requireObject(rawConfig, routeVersionId);
            for (Object rawGroup : config.getJSONArray("deviceSelectionGroups")) {
                JSONObject group = requireObject(rawGroup, routeVersionId);
                for (Object rawDeviceId : group.getJSONArray("deviceIds")) {
                    deviceIds.add(Long.valueOf(String.valueOf(rawDeviceId)));
                }
            }
        }
        if (deviceIds.isEmpty()) {
            return;
        }
        Map<Long, MesProcessPoolTeamDeviceDO> devices = teamDeviceMapper.selectBatchIds(deviceIds).stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(MesProcessPoolTeamDeviceDO::getId, device -> device));
        boolean invalid = devices.size() != deviceIds.size() || deviceIds.stream().anyMatch(deviceId -> {
            MesProcessPoolTeamDeviceDO device = devices.get(deviceId);
            return device == null || !Boolean.TRUE.equals(device.getEnabled())
                    || !DEVICE_STATUS_ENABLED.equals(device.getDeviceStatus());
        });
        if (invalid) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
        }
        for (Object rawConfig : configs) {
            JSONObject config = requireObject(rawConfig, routeVersionId);
            Set<String> processDeviceCodes = new LinkedHashSet<>();
            for (Object rawGroup : config.getJSONArray("deviceSelectionGroups")) {
                JSONObject group = requireObject(rawGroup, routeVersionId);
                for (Object rawDeviceId : group.getJSONArray("deviceIds")) {
                    Long deviceId = Long.valueOf(String.valueOf(rawDeviceId));
                    MesProcessPoolTeamDeviceDO device = devices.get(deviceId);
                    String deviceCode = device == null || StrUtil.isBlank(device.getDeviceCode()) ? null
                            : StrUtil.trim(device.getDeviceCode()).toUpperCase(Locale.ROOT);
                    if (deviceCode == null || !processDeviceCodes.add(deviceCode)) {
                        throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
                    }
                }
            }
        }
    }

    private static JSONObject requireObject(Object raw, Long routeVersionId) {
        if (raw instanceof JSONObject object) {
            return object;
        }
        throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, routeVersionId);
    }

    private void validateSourceActiveVersionStillCurrent(MesProRouteVersionDO candidate) {
        if (candidate.getSourceRouteVersionId() == null) {
            return;
        }
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(candidate.getRouteId());
        Long activeVersionId = activeVersion == null ? null : activeVersion.getId();
        if (!candidate.getSourceRouteVersionId().equals(activeVersionId)) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    candidate.getRouteId(), candidate.getSourceRouteVersionId(), activeVersionId);
        }
    }
}
