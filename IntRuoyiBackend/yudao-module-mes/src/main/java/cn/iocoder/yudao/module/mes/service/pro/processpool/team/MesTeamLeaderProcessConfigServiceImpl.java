package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderProcessConfigListReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesTeamLeaderProcessConfigServiceImpl implements MesTeamLeaderProcessConfigService {

    static final int STATISTICS_WINDOW_DAYS = 30;

    private final MesTeamLeaderLossReasonService lossReasonService;
    private final MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    private final MesProcessPoolTeamDeviceMapper deviceMapper;
    private final MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    private final MesProProcessPoolEventMapper eventMapper;
    private final Clock clock;

    @Autowired
    public MesTeamLeaderProcessConfigServiceImpl(MesTeamLeaderLossReasonService lossReasonService,
                                                 MesProcessPoolTeamProcessDeviceMapper processDeviceMapper,
                                                 MesProcessPoolTeamDeviceMapper deviceMapper,
                                                 MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper,
                                                 MesProProcessPoolEventMapper eventMapper) {
        this(lossReasonService, processDeviceMapper, deviceMapper, parameterRuleMapper, eventMapper,
                Clock.systemDefaultZone());
    }

    MesTeamLeaderProcessConfigServiceImpl(MesTeamLeaderLossReasonService lossReasonService,
                                          MesProcessPoolTeamProcessDeviceMapper processDeviceMapper,
                                          MesProcessPoolTeamDeviceMapper deviceMapper,
                                          MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper,
                                          MesProProcessPoolEventMapper eventMapper,
                                          Clock clock) {
        this.lossReasonService = lossReasonService;
        this.processDeviceMapper = processDeviceMapper;
        this.deviceMapper = deviceMapper;
        this.parameterRuleMapper = parameterRuleMapper;
        this.eventMapper = eventMapper;
        this.clock = clock;
    }

    @Override
    public List<MesTeamLeaderProcessConfigRow> listProcessConfigs(Long leaderUserId,
                                                                  MesTeamLeaderProcessConfigListReqVO reqVO) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "processConfigLeader");
        }
        if (reqVO == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "processConfigQuery");
        }
        List<MesTeamLeaderLossReasonRow> authorizedRows = lossReasonService.listLossReasonRows(leaderUserId).stream()
                .sorted(Comparator
                        .comparing(MesTeamLeaderLossReasonRow::getRouteId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(MesTeamLeaderLossReasonRow::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesTeamLeaderLossReasonRow::getRouteProcessId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (authorizedRows.isEmpty()) {
            return List.of();
        }
        Set<Long> processIds = authorizedRows.stream()
                .map(MesTeamLeaderLossReasonRow::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> routeProcessIds = authorizedRows.stream()
                .map(MesTeamLeaderLossReasonRow::getRouteProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProcessPoolTeamProcessDeviceDO> processDevices = loadProcessDevices(leaderUserId, processIds);
        Map<Long, MesProcessPoolTeamDeviceDO> deviceMap = loadDeviceMap(leaderUserId, processDevices);
        Map<Long, List<MesProcessPoolTeamProcessDeviceDO>> bindingsByProcess = processDevices.stream()
                .filter(binding -> deviceMap.containsKey(binding.getDeviceId()))
                .collect(Collectors.groupingBy(MesProcessPoolTeamProcessDeviceDO::getProcessId,
                        LinkedHashMap::new, Collectors.toList()));
        Set<Long> deviceIds = deviceMap.keySet();
        Map<RouteDeviceKey, List<MesProcessPoolDeviceParameterRuleDO>> rulesByRouteDevice =
                loadRules(routeProcessIds, deviceIds).stream()
                        .collect(Collectors.groupingBy(rule -> new RouteDeviceKey(rule.getRouteProcessId(), rule.getDeviceId()),
                                LinkedHashMap::new, Collectors.toList()));
        List<MesTeamLeaderProcessConfigRow> configRows = authorizedRows.stream()
                .map(row -> toConfigRow(row, bindingsByProcess, deviceMap, rulesByRouteDevice))
                .toList();
        String routeKeyword = normalizeKeyword(reqVO.getRouteKeyword());
        String processKeyword = normalizeKeyword(reqVO.getProcessKeyword());
        String lossReasonKeyword = normalizeKeyword(reqVO.getLossReasonKeyword());
        String deviceKeyword = normalizeKeyword(reqVO.getDeviceKeyword());
        String parameterKeyword = normalizeKeyword(reqVO.getParameterKeyword());
        return configRows.stream()
                .filter(row -> matchesAny(routeKeyword, row.getRouteCode(), row.getRouteName()))
                .filter(row -> matchesAny(processKeyword, row.getProcessCode(), row.getProcessName()))
                .filter(row -> lossReasonKeyword == null || row.getLossReasons().stream()
                        .anyMatch(reason -> containsKeyword(reason.getReasonName(), lossReasonKeyword)))
                .filter(row -> deviceKeyword == null || row.getDevices().stream()
                        .anyMatch(device -> matchesAny(deviceKeyword, device.getDeviceCode(), device.getDeviceName())))
                .filter(row -> parameterKeyword == null || row.getDevices().stream()
                        .flatMap(device -> device.getParameters().stream())
                        .anyMatch(parameter -> matchesAny(parameterKeyword,
                                parameter.getParameterCode(), parameter.getParameterName())))
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean matchesAny(String keyword, String... candidates) {
        if (keyword == null) {
            return true;
        }
        for (String candidate : candidates) {
            if (containsKeyword(candidate, keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsKeyword(String candidate, String keyword) {
        return candidate != null && candidate.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private List<MesProcessPoolTeamProcessDeviceDO> loadProcessDevices(Long leaderUserId, Set<Long> processIds) {
        if (processIds.isEmpty()) {
            return List.of();
        }
        return processDeviceMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamProcessDeviceDO>()
                .eq(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId, leaderUserId)
                .in(MesProcessPoolTeamProcessDeviceDO::getProcessId, processIds)
                .eq(MesProcessPoolTeamProcessDeviceDO::getEnabled, Boolean.TRUE)
                .orderByAsc(MesProcessPoolTeamProcessDeviceDO::getProcessId)
                .orderByAsc(MesProcessPoolTeamProcessDeviceDO::getId));
    }

    private Map<Long, MesProcessPoolTeamDeviceDO> loadDeviceMap(Long leaderUserId,
                                                                 List<MesProcessPoolTeamProcessDeviceDO> bindings) {
        Set<Long> deviceIds = bindings.stream()
                .map(MesProcessPoolTeamProcessDeviceDO::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deviceIds.isEmpty()) {
            return Map.of();
        }
        return deviceMapper.selectBatchIds(deviceIds).stream()
                .filter(Objects::nonNull)
                .filter(device -> Objects.equals(device.getLeaderUserId(), leaderUserId))
                .collect(Collectors.toMap(MesProcessPoolTeamDeviceDO::getId, Function.identity(),
                        (left, ignored) -> left, LinkedHashMap::new));
    }

    private List<MesProcessPoolDeviceParameterRuleDO> loadRules(Set<Long> routeProcessIds, Set<Long> deviceIds) {
        if (routeProcessIds.isEmpty() || deviceIds.isEmpty()) {
            return List.of();
        }
        return parameterRuleMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolDeviceParameterRuleDO>()
                .in(MesProcessPoolDeviceParameterRuleDO::getRouteProcessId, routeProcessIds)
                .in(MesProcessPoolDeviceParameterRuleDO::getDeviceId, deviceIds)
                .eq(MesProcessPoolDeviceParameterRuleDO::getEnabled, Boolean.TRUE)
                .orderByAsc(MesProcessPoolDeviceParameterRuleDO::getRouteProcessId)
                .orderByAsc(MesProcessPoolDeviceParameterRuleDO::getDeviceId)
                .orderByAsc(MesProcessPoolDeviceParameterRuleDO::getParameterCode)
                .orderByAsc(MesProcessPoolDeviceParameterRuleDO::getId));
    }

    private MesTeamLeaderProcessConfigRow toConfigRow(MesTeamLeaderLossReasonRow source,
                                                      Map<Long, List<MesProcessPoolTeamProcessDeviceDO>> bindingsByProcess,
                                                      Map<Long, MesProcessPoolTeamDeviceDO> deviceMap,
                                                      Map<RouteDeviceKey, List<MesProcessPoolDeviceParameterRuleDO>> rulesByRouteDevice) {
        List<MesTeamLeaderProcessConfigDevice> devices = new ArrayList<>();
        Set<Long> emittedDeviceIds = new LinkedHashSet<>();
        for (MesProcessPoolTeamProcessDeviceDO binding : bindingsByProcess.getOrDefault(source.getProcessId(), List.of())) {
            MesProcessPoolTeamDeviceDO device = deviceMap.get(binding.getDeviceId());
            if (device == null || !emittedDeviceIds.add(device.getId())) {
                continue;
            }
            RouteDeviceKey key = new RouteDeviceKey(source.getRouteProcessId(), device.getId());
            List<MesTeamLeaderProcessConfigParameter> parameters = rulesByRouteDevice.getOrDefault(key, List.of())
                    .stream()
                    .map(this::toParameter)
                    .toList();
            devices.add(new MesTeamLeaderProcessConfigDevice()
                    .setBindingId(binding.getId())
                    .setDeviceId(device.getId())
                    .setDeviceCode(device.getDeviceCode())
                    .setDeviceName(device.getDeviceName())
                    .setDeviceStatus(device.getDeviceStatus())
                    .setMapped(Boolean.TRUE)
                    .setParameters(parameters));
        }
        return new MesTeamLeaderProcessConfigRow()
                .setRouteId(source.getRouteId())
                .setRouteCode(source.getRouteCode())
                .setRouteName(source.getRouteName())
                .setRouteProcessId(source.getRouteProcessId())
                .setProcessId(source.getProcessId())
                .setProcessCode(source.getProcessCode())
                .setProcessName(source.getProcessName())
                .setSort(source.getSort())
                .setLossReasons(source.getReasons() == null ? List.of() : source.getReasons())
                .setDevices(devices);
    }

    private MesTeamLeaderProcessConfigParameter toParameter(MesProcessPoolDeviceParameterRuleDO rule) {
        StatisticResult statistic = calculateStatistic(rule);
        return new MesTeamLeaderProcessConfigParameter()
                .setRuleId(rule.getId())
                .setParameterCode(rule.getParameterCode())
                .setParameterName(rule.getParameterName())
                .setUnit(rule.getUnit())
                .setValueType(rule.getValueType())
                .setStandardText(rule.getStandardText())
                .setLowerLimit(rule.getLowerLimit())
                .setTargetValue(rule.getDefaultValue())
                .setUpperLimit(rule.getUpperLimit())
                .setOptionValues(parseOptionValues(rule.getOptionValuesJson()))
                .setDefaultText(rule.getDefaultText())
                .setDecimalScale(rule.getDecimalScale())
                .setEnabled(rule.getEnabled())
                .setActualAverage(statistic.actualAverage())
                .setSampleCount(statistic.sampleCount())
                .setStatisticsStartTime(statistic.statisticsStartTime())
                .setStatisticsEndTime(statistic.statisticsEndTime())
                .setStatisticsWindowDays(STATISTICS_WINDOW_DAYS);
    }

    private StatisticResult calculateStatistic(MesProcessPoolDeviceParameterRuleDO rule) {
        LocalDateTime statisticsEndTime = LocalDateTime.now(clock);
        LocalDateTime statisticsStartTime = statisticsEndTime.minusDays(STATISTICS_WINDOW_DAYS);
        if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD.equals(rule.getValueType())) {
            return new StatisticResult(null, 0, statisticsStartTime, statisticsEndTime);
        }
        List<MesProProcessPoolEventDO> events = eventMapper.selectList(new LambdaQueryWrapperX<MesProProcessPoolEventDO>()
                .eq(MesProProcessPoolEventDO::getEventType, MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .eq(MesProProcessPoolEventDO::getRouteProcessId, rule.getRouteProcessId())
                .eq(MesProProcessPoolEventDO::getDeviceId, rule.getDeviceId())
                .ge(MesProProcessPoolEventDO::getServerSubmitTime, statisticsStartTime)
                .le(MesProProcessPoolEventDO::getServerSubmitTime, statisticsEndTime)
                .orderByAsc(MesProProcessPoolEventDO::getId));
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (MesProProcessPoolEventDO event : events) {
            if (!matchesStatisticContext(event, rule, statisticsStartTime, statisticsEndTime)) {
                continue;
            }
            BigDecimal value = numericEquipmentParameter(event, rule.getParameterCode());
            if (value == null) {
                continue;
            }
            sum = sum.add(value);
            count++;
        }
        BigDecimal average = count == 0 ? null : sum.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
        return new StatisticResult(average, count, statisticsStartTime, statisticsEndTime);
    }

    private static List<String> parseOptionValues(String optionValuesJson) {
        if (optionValuesJson == null || optionValuesJson.trim().isEmpty()) {
            return List.of();
        }
        return JsonUtils.parseArray(optionValuesJson, String.class);
    }

    private boolean matchesStatisticContext(MesProProcessPoolEventDO event,
                                            MesProcessPoolDeviceParameterRuleDO rule,
                                            LocalDateTime statisticsStartTime,
                                            LocalDateTime statisticsEndTime) {
        if (event == null || !MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
            return false;
        }
        if (!Objects.equals(event.getRouteProcessId(), rule.getRouteProcessId())
                || !Objects.equals(event.getDeviceId(), rule.getDeviceId())) {
            return false;
        }
        LocalDateTime submitTime = event.getServerSubmitTime();
        return submitTime != null && !submitTime.isBefore(statisticsStartTime) && !submitTime.isAfter(statisticsEndTime);
    }

    private BigDecimal numericEquipmentParameter(MesProProcessPoolEventDO event, String parameterCode) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(event.getRawPayload());
            JsonNode parameters = root.path("equipmentParameters");
            if (!parameters.isObject()) {
                return null;
            }
            JsonNode value = parameters.get(parameterCode);
            return value != null && value.isNumber() ? value.decimalValue() : null;
        } catch (Exception ex) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "rawPayload");
        }
    }

    private record RouteDeviceKey(Long routeProcessId, Long deviceId) {
    }

    private record StatisticResult(BigDecimal actualAverage, int sampleCount,
                                   LocalDateTime statisticsStartTime, LocalDateTime statisticsEndTime) {
    }
}
