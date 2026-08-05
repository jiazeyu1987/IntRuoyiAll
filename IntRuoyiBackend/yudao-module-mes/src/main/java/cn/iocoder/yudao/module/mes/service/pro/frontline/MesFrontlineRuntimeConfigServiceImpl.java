package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED;

@Service
public class MesFrontlineRuntimeConfigServiceImpl implements MesFrontlineRuntimeConfigService {

    private static final String DEVICE_STATUS_ENABLED = "ENABLED";

    private final MesFrontlineDeviceAccountContextService contextService;
    private final MesProcessPoolTeamEmployeeBindingMapper employeeBindingMapper;
    private final MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    private final MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    private final MesProcessPoolTeamDeviceMapper deviceMapper;
    private final MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    private final MesProcessPoolDefectReasonMapper defectReasonMapper;

    public MesFrontlineRuntimeConfigServiceImpl(
            MesFrontlineDeviceAccountContextService contextService,
            MesProcessPoolTeamEmployeeBindingMapper employeeBindingMapper,
            MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper,
            MesProcessPoolTeamProcessDeviceMapper processDeviceMapper,
            MesProcessPoolTeamDeviceMapper deviceMapper,
            MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper,
            MesProcessPoolDefectReasonMapper defectReasonMapper) {
        this.contextService = contextService;
        this.employeeBindingMapper = employeeBindingMapper;
        this.employeeProfileMapper = employeeProfileMapper;
        this.processDeviceMapper = processDeviceMapper;
        this.deviceMapper = deviceMapper;
        this.parameterRuleMapper = parameterRuleMapper;
        this.defectReasonMapper = defectReasonMapper;
    }

    @Override
    public MesFrontlineRuntimeConfig getRuntimeConfig(Long loginUserId, Long routeId, Long routeProcessId,
                                                      Long processId) {
        MesFrontlineRouteProcessCandidate process = contextService.requireAuthorizedProcess(loginUserId,
                routeId, routeProcessId, processId);
        List<MesProcessPoolTeamEmployeeBindingDO> employeeBindings = listEmployeeBindings(process.processId());
        List<MesProcessPoolTeamProcessDeviceDO> processDeviceBindings = listProcessDeviceBindings(process.processId());
        Set<Long> leaderUserIds = resolveLeaderUserIds(process, employeeBindings, processDeviceBindings);
        employeeBindings = filterEmployeeBindingsByLeader(employeeBindings, leaderUserIds);
        processDeviceBindings = filterProcessDeviceBindingsByLeader(processDeviceBindings, leaderUserIds);

        List<MesFrontlineTeamEmployeeOption> employees = toEmployeeOptions(employeeBindings);
        List<MesFrontlineTeamDeviceOption> devices = toDeviceOptions(processDeviceBindings, process, leaderUserIds);
        List<MesFrontlineDefectReasonOption> defectReasons = toDefectReasonOptions(process, leaderUserIds);
        return new MesFrontlineRuntimeConfig(process.routeId(), process.routeProcessId(), process.processId(),
                employees, devices, defectReasons);
    }

    private List<MesProcessPoolTeamEmployeeBindingDO> listEmployeeBindings(Long processId) {
        return employeeBindingMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamEmployeeBindingDO>()
                .eq(MesProcessPoolTeamEmployeeBindingDO::getProcessId, processId)
                .eq(MesProcessPoolTeamEmployeeBindingDO::getEnabled, Boolean.TRUE));
    }

    private List<MesProcessPoolTeamProcessDeviceDO> listProcessDeviceBindings(Long processId) {
        return processDeviceMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamProcessDeviceDO>()
                .eq(MesProcessPoolTeamProcessDeviceDO::getProcessId, processId)
                .eq(MesProcessPoolTeamProcessDeviceDO::getEnabled, Boolean.TRUE));
    }

    private static Set<Long> resolveLeaderUserIds(MesFrontlineRouteProcessCandidate process,
                                                  List<MesProcessPoolTeamEmployeeBindingDO> employeeBindings,
                                                  List<MesProcessPoolTeamProcessDeviceDO> processDeviceBindings) {
        Set<Long> leaderUserIds = new LinkedHashSet<>();
        if (process.deviceId() != null) {
            processDeviceBindings.stream()
                    .filter(binding -> Objects.equals(binding.getDeviceId(), process.deviceId()))
                    .map(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId)
                    .filter(Objects::nonNull)
                    .forEach(leaderUserIds::add);
            if (leaderUserIds.isEmpty()) {
                throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                        "frontline runtime deviceId=" + process.deviceId());
            }
            return leaderUserIds;
        }
        employeeBindings.stream().map(MesProcessPoolTeamEmployeeBindingDO::getLeaderUserId)
                .filter(Objects::nonNull).forEach(leaderUserIds::add);
        processDeviceBindings.stream().map(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId)
                .filter(Objects::nonNull).forEach(leaderUserIds::add);
        if (leaderUserIds.size() > 1) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                    "frontline runtime processId=" + process.processId());
        }
        return leaderUserIds;
    }

    private static List<MesProcessPoolTeamEmployeeBindingDO> filterEmployeeBindingsByLeader(
            List<MesProcessPoolTeamEmployeeBindingDO> employeeBindings, Set<Long> leaderUserIds) {
        if (leaderUserIds.isEmpty()) {
            return List.of();
        }
        return employeeBindings.stream()
                .filter(binding -> leaderUserIds.contains(binding.getLeaderUserId()))
                .toList();
    }

    private static List<MesProcessPoolTeamProcessDeviceDO> filterProcessDeviceBindingsByLeader(
            List<MesProcessPoolTeamProcessDeviceDO> processDeviceBindings, Set<Long> leaderUserIds) {
        if (leaderUserIds.isEmpty()) {
            return List.of();
        }
        return processDeviceBindings.stream()
                .filter(binding -> leaderUserIds.contains(binding.getLeaderUserId()))
                .toList();
    }

    private List<MesFrontlineTeamEmployeeOption> toEmployeeOptions(
            List<MesProcessPoolTeamEmployeeBindingDO> employeeBindings) {
        Set<Long> profileIds = employeeBindings.stream()
                .map(MesProcessPoolTeamEmployeeBindingDO::getEmployeeProfileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (profileIds.isEmpty()) {
            return List.of();
        }
        Map<Long, MesProcessPoolTeamEmployeeProfileDO> profiles = employeeProfileMapper.selectBatchIds(profileIds)
                .stream()
                .filter(profile -> profile != null && Boolean.TRUE.equals(profile.getEnabled()))
                .collect(Collectors.toMap(MesProcessPoolTeamEmployeeProfileDO::getId, Function.identity(),
                        (left, ignored) -> left, LinkedHashMap::new));
        List<MesFrontlineTeamEmployeeOption> employees = new ArrayList<>();
        Set<Long> emittedProfileIds = new LinkedHashSet<>();
        for (MesProcessPoolTeamEmployeeBindingDO binding : employeeBindings) {
            MesProcessPoolTeamEmployeeProfileDO profile = profiles.get(binding.getEmployeeProfileId());
            if (profile == null || !Objects.equals(profile.getLeaderUserId(), binding.getLeaderUserId())
                    || !emittedProfileIds.add(profile.getId())) {
                continue;
            }
            String displayName = resolveProfileDisplayName(profile);
            employees.add(new MesFrontlineTeamEmployeeOption(profile.getId(), profile.getSystemUserId(),
                    profile.getEmployeeCode(), displayName, displayName, profile.getEmployeeType()));
        }
        employees.sort(Comparator
                .comparing(MesFrontlineTeamEmployeeOption::employeeName, Comparator.nullsLast(String::compareTo))
                .thenComparing(MesFrontlineTeamEmployeeOption::employeeProfileId));
        return employees;
    }

    private List<MesFrontlineTeamDeviceOption> toDeviceOptions(
            List<MesProcessPoolTeamProcessDeviceDO> processDeviceBindings,
            MesFrontlineRouteProcessCandidate process,
            Set<Long> leaderUserIds) {
        Set<Long> deviceIds = processDeviceBindings.stream()
                .map(MesProcessPoolTeamProcessDeviceDO::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deviceIds.isEmpty()) {
            return List.of();
        }
        Map<Long, MesProcessPoolTeamDeviceDO> devices = deviceMapper.selectBatchIds(deviceIds).stream()
                .filter(device -> device != null
                        && Boolean.TRUE.equals(device.getEnabled())
                        && DEVICE_STATUS_ENABLED.equals(device.getDeviceStatus()))
                .collect(Collectors.toMap(MesProcessPoolTeamDeviceDO::getId, Function.identity(),
                        (left, ignored) -> left, LinkedHashMap::new));
        Map<Long, List<MesFrontlineDeviceParameterOption>> parametersByDevice =
                listParameterOptions(process, deviceIds, leaderUserIds);
        List<MesFrontlineTeamDeviceOption> options = new ArrayList<>();
        Set<Long> emittedDeviceIds = new LinkedHashSet<>();
        for (MesProcessPoolTeamProcessDeviceDO binding : processDeviceBindings) {
            MesProcessPoolTeamDeviceDO device = devices.get(binding.getDeviceId());
            if (device == null || !Objects.equals(device.getLeaderUserId(), binding.getLeaderUserId())
                    || !emittedDeviceIds.add(device.getId())) {
                continue;
            }
            options.add(new MesFrontlineTeamDeviceOption(device.getId(), device.getDeviceCode(),
                    device.getDeviceName(), device.getDeviceStatus(),
                    parametersByDevice.getOrDefault(device.getId(), List.of())));
        }
        options.sort(Comparator
                .comparing(MesFrontlineTeamDeviceOption::deviceName, Comparator.nullsLast(String::compareTo))
                .thenComparing(MesFrontlineTeamDeviceOption::deviceId));
        return options;
    }

    private Map<Long, List<MesFrontlineDeviceParameterOption>> listParameterOptions(
            MesFrontlineRouteProcessCandidate process, Set<Long> deviceIds, Set<Long> leaderUserIds) {
        if (deviceIds.isEmpty() || leaderUserIds.isEmpty()) {
            return Map.of();
        }
        List<MesProcessPoolDeviceParameterRuleDO> rules = parameterRuleMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolDeviceParameterRuleDO>()
                        .eq(MesProcessPoolDeviceParameterRuleDO::getProcessId, process.processId())
                        .in(MesProcessPoolDeviceParameterRuleDO::getDeviceId, deviceIds)
                        .in(MesProcessPoolDeviceParameterRuleDO::getLeaderUserId, leaderUserIds)
                        .eq(MesProcessPoolDeviceParameterRuleDO::getEnabled, Boolean.TRUE));
        return rules.stream()
                .filter(rule -> routeProcessMatches(rule.getRouteProcessId(), process.routeProcessId()))
                .sorted(Comparator
                        .comparing(MesProcessPoolDeviceParameterRuleDO::getParameterCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesProcessPoolDeviceParameterRuleDO::getId))
                .collect(Collectors.groupingBy(MesProcessPoolDeviceParameterRuleDO::getDeviceId,
                        LinkedHashMap::new,
                        Collectors.mapping(rule -> new MesFrontlineDeviceParameterOption(rule.getParameterCode(),
                                rule.getParameterName(), rule.getUnit(), rule.getLowerLimit(), rule.getUpperLimit(),
                                rule.getDefaultValue(), rule.getValueType()), Collectors.toList())));
    }

    private List<MesFrontlineDefectReasonOption> toDefectReasonOptions(
            MesFrontlineRouteProcessCandidate process, Set<Long> leaderUserIds) {
        if (leaderUserIds.isEmpty()) {
            return List.of();
        }
        return defectReasonMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolDefectReasonDO>()
                        .eq(MesProcessPoolDefectReasonDO::getProcessId, process.processId())
                        .in(MesProcessPoolDefectReasonDO::getLeaderUserId, leaderUserIds)
                        .eq(MesProcessPoolDefectReasonDO::getEnabled, Boolean.TRUE))
                .stream()
                .filter(reason -> routeProcessMatches(reason.getRouteProcessId(), process.routeProcessId()))
                .sorted(Comparator
                        .comparing(MesProcessPoolDefectReasonDO::getReasonCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesProcessPoolDefectReasonDO::getId))
                .map(reason -> new MesFrontlineDefectReasonOption(reason.getId(), reason.getReasonType(),
                        reason.getReasonCode(), reason.getReasonName()))
                .toList();
    }

    private static boolean routeProcessMatches(Long configuredRouteProcessId, Long routeProcessId) {
        return configuredRouteProcessId == null || Objects.equals(configuredRouteProcessId, routeProcessId);
    }

    private static String resolveProfileDisplayName(MesProcessPoolTeamEmployeeProfileDO profile) {
        String displayName = normalizeText(profile.getDisplayName());
        return displayName != null ? displayName : normalizeText(profile.getEmployeeName());
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
