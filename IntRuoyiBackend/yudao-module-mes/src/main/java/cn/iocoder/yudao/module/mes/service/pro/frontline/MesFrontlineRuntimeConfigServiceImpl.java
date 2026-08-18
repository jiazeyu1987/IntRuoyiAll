package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;

@Service
public class MesFrontlineRuntimeConfigServiceImpl implements MesFrontlineRuntimeConfigService {

    private static final String DEVICE_STATUS_ENABLED = "ENABLED";
    private static final String PRODUCTION_CONTEXT_PREFIX = "productionSubmitContext.";

    private final MesFrontlineDeviceAccountContextService contextService;
    private final MesFrontlineActiveOrderProcessService activeOrderProcessService;
    private final MesFrontlineTemplateResolver templateResolver;
    private final MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    private final MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    private final MesProcessPoolTeamDeviceMapper deviceMapper;
    private final MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    private final MesProcessPoolDefectReasonMapper defectReasonMapper;
    private final MesFrontlineSessionSnapshotService sessionSnapshotService;

    public MesFrontlineRuntimeConfigServiceImpl(
            MesFrontlineDeviceAccountContextService contextService,
            MesFrontlineActiveOrderProcessService activeOrderProcessService,
            MesFrontlineTemplateResolver templateResolver,
            MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper,
            MesProcessPoolTeamProcessDeviceMapper processDeviceMapper,
            MesProcessPoolTeamDeviceMapper deviceMapper,
            MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper,
            MesProcessPoolDefectReasonMapper defectReasonMapper,
            MesFrontlineSessionSnapshotService sessionSnapshotService) {
        this.contextService = contextService;
        this.activeOrderProcessService = activeOrderProcessService;
        this.templateResolver = templateResolver;
        this.employeeProfileMapper = employeeProfileMapper;
        this.processDeviceMapper = processDeviceMapper;
        this.deviceMapper = deviceMapper;
        this.parameterRuleMapper = parameterRuleMapper;
        this.defectReasonMapper = defectReasonMapper;
        this.sessionSnapshotService = sessionSnapshotService;
    }

    @Override
    public MesFrontlineRuntimeConfig getRuntimeConfig(Long loginUserId, Long activeOrderId, Long routeId,
                                                      Long routeProcessId, Long processId) {
        Long responsibleLeaderUserId = contextService.resolveResponsibleLeaderUserId(loginUserId);
        MesFrontlineRouteProcessCandidate process = activeOrderProcessService.requireProcess(
                responsibleLeaderUserId, activeOrderId, routeId, routeProcessId, processId)
                .toRouteProcessCandidate();
        List<MesProcessPoolTeamProcessDeviceDO> processDeviceBindings = listProcessDeviceBindings(process.processId());
        Set<Long> leaderUserIds = resolveLeaderUserIds(process, processDeviceBindings, responsibleLeaderUserId);
        processDeviceBindings = filterProcessDeviceBindingsByLeader(processDeviceBindings, leaderUserIds);

        List<MesFrontlineTeamEmployeeOption> employees = toEmployeeOptions(responsibleLeaderUserId);
        List<MesFrontlineTeamDeviceOption> devices = toDeviceOptions(processDeviceBindings, process, leaderUserIds);
        List<MesFrontlineDefectReasonOption> defectReasons = toDefectReasonOptions(process, leaderUserIds);
        MesFrontlineProductionSubmitContext productionSubmitContext =
                resolveProductionSubmitContext(process, responsibleLeaderUserId);
        List<MesFrontlineEmployeeSwitchResult> employeeSwitchSnapshots =
                resolveEmployeeSwitchSnapshots(loginUserId, process, employees);
        MesFrontlineSessionSnapshotReference snapshotReference = sessionSnapshotService.issue(
                new MesFrontlineSessionSnapshotContent(TenantContextHolder.getTenantId(), loginUserId,
                        process.routeId(), process.routeProcessId(), process.processId(), process.workstationId(),
                        employeeSwitchSnapshots, devices, defectReasons, productionSubmitContext));
        return new MesFrontlineRuntimeConfig(process.routeId(), process.routeProcessId(), process.processId(),
                employees, devices, defectReasons, productionSubmitContext, employeeSwitchSnapshots,
                snapshotReference.snapshotId(), snapshotReference.snapshotHash());
    }

    private List<MesFrontlineEmployeeSwitchResult> resolveEmployeeSwitchSnapshots(
            Long loginUserId, MesFrontlineRouteProcessCandidate process,
            List<MesFrontlineTeamEmployeeOption> employees) {
        if (employees == null || employees.isEmpty()) {
            return List.of();
        }
        return employees.stream().map(employee -> {
            Long actualEmployeeId = resolveActualEmployeeId(employee);
            MesFrontlineTemplateDescriptor template = templateResolver.resolve(new MesFrontlineTemplateRequest(
                    loginUserId, actualEmployeeId, process.routeId(), process.routeProcessId(), process.processId()));
            return new MesFrontlineEmployeeSwitchResult(loginUserId, actualEmployeeId,
                    process.routeId(), process.routeProcessId(), process.processId(), false, template);
        }).toList();
    }

    private static Long resolveActualEmployeeId(MesFrontlineTeamEmployeeOption employee) {
        return employee.systemUserId() != null ? employee.systemUserId() : employee.employeeProfileId();
    }

    private MesFrontlineProductionSubmitContext resolveProductionSubmitContext(
            MesFrontlineRouteProcessCandidate process, Long responsibleLeaderUserId) {
        requirePositive(responsibleLeaderUserId, "approveUserId");
        return new MesFrontlineProductionSubmitContext(
                null,
                null,
                null,
                null,
                process.routeId(),
                process.routeProcessId(),
                process.processId(),
                process.workstationId(),
                null,
                responsibleLeaderUserId,
                null,
                null,
                null);
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, PRODUCTION_CONTEXT_PREFIX + field);
        }
    }

    private List<MesProcessPoolTeamProcessDeviceDO> listProcessDeviceBindings(Long processId) {
        return processDeviceMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamProcessDeviceDO>()
                .eq(MesProcessPoolTeamProcessDeviceDO::getProcessId, processId)
                .eq(MesProcessPoolTeamProcessDeviceDO::getEnabled, Boolean.TRUE));
    }

    private static Set<Long> resolveLeaderUserIds(MesFrontlineRouteProcessCandidate process,
                                                  List<MesProcessPoolTeamProcessDeviceDO> processDeviceBindings,
                                                  Long responsibleLeaderUserId) {
        Set<Long> leaderUserIds = new LinkedHashSet<>();
        if (isRouteStartProductionLeaderContext(process)) {
            if (responsibleLeaderUserId == null) {
                throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                        "frontline runtime routeStartProductionLeader");
            }
            leaderUserIds.add(responsibleLeaderUserId);
            return leaderUserIds;
        }
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
        processDeviceBindings.stream().map(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId)
                .filter(Objects::nonNull).forEach(leaderUserIds::add);
        if (leaderUserIds.size() > 1) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                    "frontline runtime processId=" + process.processId());
        }
        if (leaderUserIds.isEmpty() && responsibleLeaderUserId != null) {
            leaderUserIds.add(responsibleLeaderUserId);
        }
        return leaderUserIds;
    }

    private static boolean isRouteStartProductionLeaderContext(MesFrontlineRouteProcessCandidate process) {
        return MesFrontlineRouteProcessCandidate.CONTEXT_SOURCE_ROUTE_START_PRODUCTION_LEADER.equals(
                process.contextSource());
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

    private List<MesFrontlineTeamEmployeeOption> toEmployeeOptions(Long leaderUserId) {
        Objects.requireNonNull(leaderUserId, "leaderUserId");
        List<MesFrontlineTeamEmployeeOption> employees = new ArrayList<>();
        Set<Long> emittedProfileIds = new LinkedHashSet<>();
        List<MesProcessPoolTeamEmployeeProfileDO> profiles = employeeProfileMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>()
                        .eq(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId, leaderUserId)
                        .eq(MesProcessPoolTeamEmployeeProfileDO::getEnabled, Boolean.TRUE));
        for (MesProcessPoolTeamEmployeeProfileDO profile : profiles) {
            if (profile == null || !Objects.equals(profile.getLeaderUserId(), leaderUserId)
                    || !Boolean.TRUE.equals(profile.getEnabled())
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
                .collect(Collectors.toMap(MesProcessPoolTeamDeviceDO::getId, device -> device,
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
                                 resolveParameterDefaultValue(rule), rule.getValueType(), rule.getStandardText(),
                                 parseOptionValues(rule.getOptionValuesJson()), rule.getDefaultText(),
                                 rule.getDecimalScale()),
                                 Collectors.toList())));
    }

    private static List<String> parseOptionValues(String optionValuesJson) {
        if (optionValuesJson == null || optionValuesJson.trim().isEmpty()) {
            return List.of();
        }
        return JsonUtils.parseArray(optionValuesJson, String.class);
    }

    private static BigDecimal resolveParameterDefaultValue(MesProcessPoolDeviceParameterRuleDO rule) {
        if (rule.getDefaultValue() != null) {
            return rule.getDefaultValue();
        }
        if (!MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER.equals(rule.getValueType())
                && !MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL.equals(rule.getValueType())) {
            return null;
        }
        if (rule.getLowerLimit() == null || rule.getUpperLimit() == null) {
            return null;
        }
        return rule.getLowerLimit().add(rule.getUpperLimit()).divide(BigDecimal.valueOf(2));
    }

    private List<MesFrontlineDefectReasonOption> toDefectReasonOptions(
            MesFrontlineRouteProcessCandidate process, Set<Long> leaderUserIds) {
        return defectReasonMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolDefectReasonDO>()
                        .eq(MesProcessPoolDefectReasonDO::getRouteProcessId, process.routeProcessId())
                        .eq(MesProcessPoolDefectReasonDO::getReasonType,
                                MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS)
                        .eq(MesProcessPoolDefectReasonDO::getEnabled, Boolean.TRUE))
                .stream()
                .filter(reason -> routeProcessMatches(reason.getRouteProcessId(), process.routeProcessId()))
                .filter(reason -> MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS.equals(reason.getReasonType()))
                .filter(reason -> Boolean.TRUE.equals(reason.getEnabled()))
                .sorted(Comparator
                        .comparing(MesProcessPoolDefectReasonDO::getReasonCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesProcessPoolDefectReasonDO::getId))
                .map(reason -> new MesFrontlineDefectReasonOption(reason.getId(), reason.getReasonType(),
                        reason.getReasonCode(), reason.getReasonName()))
                .toList();
    }

    private static boolean routeProcessMatches(Long configuredRouteProcessId, Long routeProcessId) {
        return configuredRouteProcessId != null && Objects.equals(configuredRouteProcessId, routeProcessId);
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
