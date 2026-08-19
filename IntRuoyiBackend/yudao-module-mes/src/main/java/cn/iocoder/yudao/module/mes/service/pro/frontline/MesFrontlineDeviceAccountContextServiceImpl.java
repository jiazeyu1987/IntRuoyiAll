package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowConfigServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionLifecycleServiceImpl;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ACTUAL_EMPLOYEE_NOT_IN_TEAM;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ACTUAL_EMPLOYEE_LEADER_ASSIGNMENT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_BINDING_SOURCE_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_ROUTE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_PROCESS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_LEADER_EMPLOYEE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ROUTE_PROCESS_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ROUTE_PROCESS_WORKSTATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;

@Service
public class MesFrontlineDeviceAccountContextServiceImpl implements MesFrontlineDeviceAccountContextService {

    public static final String PRESSURE_PUMP_ALL_PROCESS_PERMISSION =
            "mes:pro-feedback:frontline-pressure-pump:all-processes";
    private static final String CANDIDATE_SOURCE_TYPE_USERS = "USERS";
    private static final String CANDIDATE_SOURCE_TYPE_ROLE = "ROLE";

    private final ObjectProvider<MesFrontlineDeviceAccountRouteBindingSource> routeBindingSourceProvider;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProProcessService processService;
    private final MesMdWorkstationWorkerService workstationWorkerService;
    private final AdminUserApi adminUserApi;
    private final PermissionApi permissionApi;
    private final MesProRouteService routeService;
    private final MesMdWorkstationService workstationService;
    private final MesMdWorkstationMachineService workstationMachineService;
    private final MesDvMachineryService machineryService;
    private final MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    private final MesFrontlineActiveOrderProcessService activeOrderProcessService;

    public MesFrontlineDeviceAccountContextServiceImpl(
            ObjectProvider<MesFrontlineDeviceAccountRouteBindingSource> routeBindingSourceProvider,
            MesProRouteProcessMapper routeProcessMapper,
            MesProRouteVersionMapper routeVersionMapper,
            MesProProcessService processService,
            MesMdWorkstationWorkerService workstationWorkerService,
            AdminUserApi adminUserApi,
            PermissionApi permissionApi,
            MesProRouteService routeService,
            MesMdWorkstationService workstationService,
            MesMdWorkstationMachineService workstationMachineService,
            MesDvMachineryService machineryService,
            MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper,
            MesFrontlineActiveOrderProcessService activeOrderProcessService) {
        this.routeBindingSourceProvider = routeBindingSourceProvider;
        this.routeProcessMapper = routeProcessMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.processService = processService;
        this.workstationWorkerService = workstationWorkerService;
        this.adminUserApi = adminUserApi;
        this.permissionApi = permissionApi;
        this.routeService = routeService;
        this.workstationService = workstationService;
        this.workstationMachineService = workstationMachineService;
        this.machineryService = machineryService;
        this.employeeProfileMapper = employeeProfileMapper;
        this.activeOrderProcessService = activeOrderProcessService;
    }

    @Override
    public Long resolveResponsibleLeaderUserId(Long loginUserId) {
        return resolveResponsibleLeaderContext(loginUserId).leaderUserId();
    }

    @Override
    public List<MesFrontlineRouteProcessCandidate> listSwitchableProcesses(Long loginUserId) {
        ResponsibleLeaderContext responsibleLeader = resolveResponsibleLeaderContext(loginUserId);
        List<MesFrontlineRouteProcessCandidate> routeStartLeaderCandidates =
                listRouteStartProductionLeaderSwitchableProcesses(responsibleLeader.leaderUserId());
        if (CollUtil.isNotEmpty(routeStartLeaderCandidates)) {
            return routeStartLeaderCandidates;
        }
        if (responsibleLeader.productionEmployee()) {
            throw exception(PRO_FRONTLINE_ROUTE_PROCESS_NOT_AUTHORIZED, loginUserId);
        }
        return listPostBoundSwitchableProcesses(loginUserId);
    }

    private ResponsibleLeaderContext resolveResponsibleLeaderContext(Long loginUserId) {
        requireValue(loginUserId, "loginUserId");
        List<MesProcessPoolTeamEmployeeProfileDO> profiles = employeeProfileMapper.selectList(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>()
                        .eq(MesProcessPoolTeamEmployeeProfileDO::getSystemUserId, loginUserId));
        if (CollUtil.isEmpty(profiles)) {
            return new ResponsibleLeaderContext(loginUserId, false);
        }
        Set<Long> leaderUserIds = profiles.stream()
                .filter(Objects::nonNull)
                .filter(profile -> Boolean.TRUE.equals(profile.getEnabled()))
                .map(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (leaderUserIds.size() != 1) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "productionEmployee leaderUserId loginUserId=" + loginUserId);
        }
        return new ResponsibleLeaderContext(leaderUserIds.iterator().next(), true);
    }

    private List<MesFrontlineRouteProcessCandidate> listPostBoundSwitchableProcesses(Long loginUserId) {
        RouteBindingContext bindingContext = routeBindingContext(loginUserId);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteIds(bindingContext.routeIds());
        if (CollUtil.isEmpty(routeProcesses)) {
            throw exception(PRO_FRONTLINE_ROUTE_PROCESS_NOT_AUTHORIZED, loginUserId);
        }

        Set<Long> processIds = new LinkedHashSet<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            if (routeProcess == null) {
                continue;
            }
            requireRouteProcessIdentity(routeProcess);
            requireRouteProcessWorkstation(routeProcess);
            if (!bindingContext.bindingsByRouteWorkstation().containsKey(routeWorkstationKey(routeProcess))) {
                continue;
            }
            processIds.add(routeProcess.getProcessId());
        }
        if (processIds.isEmpty()) {
            throw exception(PRO_FRONTLINE_ROUTE_PROCESS_NOT_AUTHORIZED, loginUserId);
        }
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(processIds);

        List<MesFrontlineRouteProcessCandidate> candidates = new ArrayList<>();
        Set<CandidateKey> acceptedCandidates = new LinkedHashSet<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            if (routeProcess == null) {
                continue;
            }
            requireRouteProcessIdentity(routeProcess);
            requireRouteProcessWorkstation(routeProcess);
            List<MesFrontlineDeviceRouteBinding> routeBindings =
                    bindingContext.bindingsByRouteWorkstation().get(routeWorkstationKey(routeProcess));
            if (CollUtil.isEmpty(routeBindings)) {
                continue;
            }
            MesProProcessDO process = processMap.get(routeProcess.getProcessId());
            if (process == null || !CommonStatusEnum.isEnable(process.getStatus())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "processId=" + routeProcess.getProcessId());
            }
            for (MesFrontlineDeviceRouteBinding routeBinding : routeBindings) {
                if (!acceptedCandidates.add(new CandidateKey(routeProcess.getId(), routeBinding.deviceId()))) {
                    continue;
                }
                candidates.add(toCandidate(routeBinding, routeProcess, process));
            }
        }
        if (candidates.isEmpty()) {
            throw exception(PRO_FRONTLINE_ROUTE_PROCESS_NOT_AUTHORIZED, loginUserId);
        }
        candidates.sort(Comparator
                .comparing(MesFrontlineRouteProcessCandidate::routeId)
                .thenComparing(candidate -> candidate.sort() == null ? Integer.MAX_VALUE : candidate.sort())
                .thenComparing(MesFrontlineRouteProcessCandidate::routeProcessId)
                .thenComparing(MesFrontlineRouteProcessCandidate::deviceId,
                        Comparator.nullsLast(Long::compareTo)));
        return candidates;
    }

    private List<MesFrontlineRouteProcessCandidate> listRouteStartProductionLeaderSwitchableProcesses(
            Long loginUserId) {
        Map<Long, MesProRouteDO> routeMap = listEnabledRouteMap();
        if (routeMap.isEmpty()) {
            return List.of();
        }
        Set<Long> authorizedRouteIds =
                resolveRouteStartProductionLeaderAuthorizedRouteIds(loginUserId, routeMap);
        if (authorizedRouteIds.isEmpty()) {
            return List.of();
        }
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteIds(authorizedRouteIds);
        if (CollUtil.isEmpty(routeProcesses)) {
            throw exception(PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_PROCESS_EMPTY, authorizedRouteIds);
        }

        Set<Long> workstationIds = new LinkedHashSet<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            if (routeProcess == null || !authorizedRouteIds.contains(routeProcess.getRouteId())) {
                continue;
            }
            requireRouteProcessIdentity(routeProcess);
            requireRouteProcessWorkstation(routeProcess);
            workstationIds.add(routeProcess.getWorkstationId());
        }
        if (workstationIds.isEmpty()) {
            throw exception(PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_PROCESS_EMPTY, authorizedRouteIds);
        }

        Map<Long, MesMdWorkstationDO> workstationMap = workstationService.getWorkstationMap(workstationIds);
        requireWorkstationsPresentAndEnabled(workstationIds, workstationMap);

        List<MesProRouteProcessDO> acceptedRouteProcesses = new ArrayList<>();
        Set<Long> processIds = new LinkedHashSet<>();
        Set<Long> acceptedWorkstationIds = new LinkedHashSet<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            if (routeProcess == null || !authorizedRouteIds.contains(routeProcess.getRouteId())) {
                continue;
            }
            requireRouteProcessIdentity(routeProcess);
            requireRouteProcessWorkstation(routeProcess);
            acceptedRouteProcesses.add(routeProcess);
            processIds.add(routeProcess.getProcessId());
            acceptedWorkstationIds.add(routeProcess.getWorkstationId());
        }
        if (acceptedRouteProcesses.isEmpty()) {
            throw exception(PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_PROCESS_EMPTY, authorizedRouteIds);
        }

        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(processIds);
        Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstation =
                resolveMachinesByWorkstation(acceptedWorkstationIds);
        Map<Long, MesDvMachineryDO> machineryMap = resolveMachineryMap(machinesByWorkstation);

        List<MesFrontlineRouteProcessCandidate> candidates = new ArrayList<>();
        Set<CandidateKey> acceptedCandidates = new LinkedHashSet<>();
        for (MesProRouteProcessDO routeProcess : acceptedRouteProcesses) {
            requireRouteProcessIdentity(routeProcess);
            requireRouteProcessWorkstation(routeProcess);
            MesProProcessDO process = processMap.get(routeProcess.getProcessId());
            if (process == null || !CommonStatusEnum.isEnable(process.getStatus())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "processId=" + routeProcess.getProcessId());
            }
            MesProRouteDO route = routeMap.get(routeProcess.getRouteId());
            MesMdWorkstationDO workstation = workstationMap.get(routeProcess.getWorkstationId());
            List<MesMdWorkstationMachineDO> workstationMachines =
                    machinesByWorkstation.getOrDefault(routeProcess.getWorkstationId(), List.of());
            if (workstationMachines.isEmpty()) {
                if (acceptedCandidates.add(new CandidateKey(routeProcess.getId(), null))) {
                    candidates.add(toCandidate(route, workstation, null, routeProcess, process));
                }
                continue;
            }
            for (MesMdWorkstationMachineDO machine : workstationMachines) {
                MesDvMachineryDO machinery = machineryMap.get(machine.getMachineryId());
                if (!acceptedCandidates.add(new CandidateKey(routeProcess.getId(), machine.getMachineryId()))) {
                    continue;
                }
                candidates.add(toCandidate(route, workstation, machinery, routeProcess, process));
            }
        }
        if (candidates.isEmpty()) {
            throw exception(PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_PROCESS_EMPTY, authorizedRouteIds);
        }
        candidates.sort(Comparator
                .comparing(MesFrontlineRouteProcessCandidate::routeId)
                .thenComparing(candidate -> candidate.sort() == null ? Integer.MAX_VALUE : candidate.sort())
                .thenComparing(MesFrontlineRouteProcessCandidate::routeProcessId)
                .thenComparing(MesFrontlineRouteProcessCandidate::deviceId,
                        Comparator.nullsLast(Long::compareTo)));
        return candidates;
    }

    @Override
    public List<MesFrontlineEmployeeCandidate> listEmployeeCandidates(Long loginUserId, Long activeOrderId,
                                                                      Long routeId, Long routeProcessId,
                                                                      Long processId) {
        Long leaderUserId = resolveResponsibleLeaderUserId(loginUserId);
        MesFrontlineRouteProcessCandidate processCandidate = activeOrderId == null
                ? requireAuthorizedProcess(loginUserId, routeId, routeProcessId, processId)
                : activeOrderProcessService.requireProcess(leaderUserId, activeOrderId, routeId,
                        routeProcessId, processId).toRouteProcessCandidate();
        List<MesFrontlineEmployeeCandidate> candidates = employeeProfileMapper.selectList(
                        new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>()
                                .eq(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId, leaderUserId)
                                .eq(MesProcessPoolTeamEmployeeProfileDO::getEnabled, Boolean.TRUE))
                .stream()
                .filter(Objects::nonNull)
                .filter(profile -> Objects.equals(profile.getLeaderUserId(), leaderUserId))
                .filter(profile -> Boolean.TRUE.equals(profile.getEnabled()))
                .map(MesFrontlineDeviceAccountContextServiceImpl::toEmployeeCandidate)
                .toList();
        if (candidates.isEmpty()) {
            throw exception(PRO_FRONTLINE_LEADER_EMPLOYEE_EMPTY, leaderUserId,
                    processCandidate.processId());
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparing((MesFrontlineEmployeeCandidate candidate) -> displayName(candidate),
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesFrontlineEmployeeCandidate::userId))
                .toList();
    }

    private static MesFrontlineEmployeeCandidate toEmployeeCandidate(
            MesProcessPoolTeamEmployeeProfileDO profile) {
        Long employeeId = profile.getSystemUserId() != null ? profile.getSystemUserId() : profile.getId();
        String displayName = normalizeText(profile.getDisplayName());
        if (displayName == null) {
            displayName = normalizeText(profile.getEmployeeName());
        }
        return new MesFrontlineEmployeeCandidate(employeeId, profile.getEmployeeCode(), displayName);
    }

    @Override
    public MesFrontlineRouteProcessCandidate requireAuthorizedProcess(Long loginUserId, Long routeId,
                                                                      Long routeProcessId, Long processId) {
        requireValue(loginUserId, "loginUserId");
        requireValue(routeId, "routeId");
        requireValue(routeProcessId, "routeProcessId");
        requireValue(processId, "processId");
        return listSwitchableProcesses(loginUserId).stream()
                .filter(candidate -> Objects.equals(candidate.routeId(), routeId)
                        && Objects.equals(candidate.routeProcessId(), routeProcessId)
                        && Objects.equals(candidate.processId(), processId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_ROUTE_PROCESS_NOT_AUTHORIZED, routeId, processId));
    }

    @Override
    public MesFrontlineEmployeeCandidate requireTeamEmployee(Long loginUserId, Long routeId, Long routeProcessId,
                                                             Long processId, Long actualEmployeeId) {
        requireValue(actualEmployeeId, "actualEmployeeId");
        requireUniqueResponsibleLeaderUserId(actualEmployeeId);
        return listEmployeeCandidates(loginUserId, null, routeId, routeProcessId, processId).stream()
                .filter(candidate -> Objects.equals(candidate.userId(), actualEmployeeId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_ACTUAL_EMPLOYEE_NOT_IN_TEAM,
                        actualEmployeeId, processId));
    }

    Long requireUniqueResponsibleLeaderUserId(Long actualEmployeeId) {
        requireValue(actualEmployeeId, "actualEmployeeId");
        List<MesProcessPoolTeamEmployeeProfileDO> profiles = employeeProfileMapper.selectList(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>()
                        .eq(MesProcessPoolTeamEmployeeProfileDO::getEnabled, Boolean.TRUE)
                        .and(wrapper -> wrapper
                                .eq(MesProcessPoolTeamEmployeeProfileDO::getSystemUserId, actualEmployeeId)
                                .or(temporary -> temporary
                                        .isNull(MesProcessPoolTeamEmployeeProfileDO::getSystemUserId)
                                        .eq(MesProcessPoolTeamEmployeeProfileDO::getId, actualEmployeeId))));
        Set<Long> leaderUserIds = profiles.stream()
                .filter(Objects::nonNull)
                .filter(profile -> Boolean.TRUE.equals(profile.getEnabled()))
                .filter(profile -> Objects.equals(profile.getSystemUserId(), actualEmployeeId)
                        || profile.getSystemUserId() == null && Objects.equals(profile.getId(), actualEmployeeId))
                .map(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (leaderUserIds.size() != 1) {
            throw exception(PRO_FRONTLINE_ACTUAL_EMPLOYEE_LEADER_ASSIGNMENT_INVALID, actualEmployeeId);
        }
        return leaderUserIds.iterator().next();
    }

    private RouteBindingContext routeBindingContext(Long loginUserId) {
        requireValue(loginUserId, "loginUserId");
        MesFrontlineDeviceAccountRouteBindingSource routeBindingSource = routeBindingSourceProvider.getIfAvailable();
        if (routeBindingSource == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_BINDING_SOURCE_MISSING);
        }
        List<MesFrontlineDeviceRouteBinding> routeBindings = routeBindingSource.listEnabledRouteBindings(loginUserId);
        if (CollUtil.isEmpty(routeBindings)) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_ROUTE_EMPTY, loginUserId);
        }
        Set<Long> routeIds = new LinkedHashSet<>();
        Map<RouteWorkstationKey, List<MesFrontlineDeviceRouteBinding>> bindingsByRouteWorkstation =
                new LinkedHashMap<>();
        for (MesFrontlineDeviceRouteBinding routeBinding : routeBindings) {
            requireRouteBinding(loginUserId, routeBinding);
            routeIds.add(routeBinding.routeId());
            bindingsByRouteWorkstation
                    .computeIfAbsent(new RouteWorkstationKey(routeBinding.routeId(), routeBinding.workstationId()),
                            ignored -> new ArrayList<>())
                    .add(routeBinding);
        }
        if (bindingsByRouteWorkstation.isEmpty()) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_ROUTE_EMPTY, loginUserId);
        }
        return new RouteBindingContext(routeIds, bindingsByRouteWorkstation);
    }

    private Map<Long, MesProRouteDO> listEnabledRouteMap() {
        List<MesProRouteDO> routes = routeService.getRouteListByStatus(CommonStatusEnum.ENABLE.getStatus());
        Map<Long, MesProRouteDO> routeMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(routes)) {
            return routeMap;
        }
        for (MesProRouteDO route : routes) {
            if (route == null
                    || route.getId() == null) {
                continue;
            }
            routeMap.putIfAbsent(route.getId(), route);
        }
        return routeMap;
    }

    private Set<Long> resolveRouteStartProductionLeaderAuthorizedRouteIds(
            Long loginUserId, Map<Long, MesProRouteDO> routeMap) {
        List<MesProRouteVersionDO> routeVersions = routeVersionMapper.selectListByRouteIds(routeMap.keySet());
        if (CollUtil.isEmpty(routeVersions)) {
            return Set.of();
        }
        Map<Long, MesProRouteVersionDO> activeVersionByRouteId = new LinkedHashMap<>();
        for (MesProRouteVersionDO routeVersion : routeVersions) {
            if (routeVersion == null
                    || routeVersion.getRouteId() == null
                    || !Boolean.TRUE.equals(routeVersion.getActive())
                    || !MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE.equals(
                    routeVersion.getLifecycleStatus())) {
                continue;
            }
            activeVersionByRouteId.putIfAbsent(routeVersion.getRouteId(), routeVersion);
        }
        if (activeVersionByRouteId.isEmpty()) {
            return Set.of();
        }
        boolean allProcessPermissionGranted = permissionApi.hasAnyPermissions(loginUserId,
                PRESSURE_PUMP_ALL_PROCESS_PERMISSION);
        Set<Long> userRoleIds = null;
        Set<Long> authorizedRouteIds = new LinkedHashSet<>();
        for (MesProRouteVersionDO routeVersion : activeVersionByRouteId.values()) {
            List<RouteStartProductionLeaderSnapshot> snapshots =
                    parseRouteStartProductionLeaderSnapshots(routeVersion);
            if (allProcessPermissionGranted
                    && isPressurePumpRoute(routeMap.get(routeVersion.getRouteId()))
                    && !snapshots.isEmpty()) {
                authorizedRouteIds.add(routeVersion.getRouteId());
                continue;
            }
            for (RouteStartProductionLeaderSnapshot snapshot : snapshots) {
                if (CANDIDATE_SOURCE_TYPE_USERS.equals(snapshot.candidateSourceType())
                        && snapshot.candidateSourceIds().contains(loginUserId)) {
                    authorizedRouteIds.add(snapshot.routeId());
                    continue;
                }
                if (CANDIDATE_SOURCE_TYPE_ROLE.equals(snapshot.candidateSourceType())) {
                    if (userRoleIds == null) {
                        Set<Long> roleIds = permissionApi.getUserRoleIdListByUserId(loginUserId);
                        userRoleIds = roleIds == null ? Set.of() : new LinkedHashSet<>(roleIds);
                    }
                    boolean roleMatched = snapshot.candidateSourceIds().stream().anyMatch(userRoleIds::contains);
                    if (roleMatched) {
                        authorizedRouteIds.add(snapshot.routeId());
                    }
                }
            }
        }
        return authorizedRouteIds;
    }

    private boolean isPressurePumpRoute(MesProRouteDO route) {
        return route != null
                && route.getName() != null
                && route.getName().contains("压力泵");
    }

    private List<RouteStartProductionLeaderSnapshot> parseRouteStartProductionLeaderSnapshots(
            MesProRouteVersionDO routeVersion) {
        Object snapshot = resolveRouteVersionConfigSnapshot(routeVersion,
                MesProRouteFlowConfigServiceImpl.ROUTE_START_PRODUCTION_LEADERS_KEY);
        if (snapshot == null) {
            return List.of();
        }
        if (!(snapshot instanceof JSONArray items)) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "routeStartProductionLeaders routeVersionId=" + routeVersion.getId());
        }
        List<RouteStartProductionLeaderSnapshot> result = new ArrayList<>();
        for (Object value : items) {
            JSONObject item = toJsonObject(routeVersion, value);
            Long productionLineId = item.getLong("productionLineId");
            String candidateSourceType = normalizeProductionLeaderCandidateSourceType(
                    item.getString("candidateSourceType"));
            List<Long> candidateSourceIds = parseCandidateSourceIds(item.get("candidateSourceIds"));
            if (productionLineId == null
                    || !Objects.equals(productionLineId, routeVersion.getRouteId())
                    || candidateSourceIds.isEmpty()) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "routeStartProductionLeaders routeVersionId=" + routeVersion.getId());
            }
            result.add(new RouteStartProductionLeaderSnapshot(routeVersion.getRouteId(), productionLineId,
                    candidateSourceType, candidateSourceIds));
        }
        return result;
    }

    private Object resolveRouteVersionConfigSnapshot(MesProRouteVersionDO routeVersion, String configKey) {
        if (routeVersion == null || routeVersion.getRouteSnapshotJson() == null) {
            return null;
        }
        JSONObject snapshot;
        try {
            snapshot = JSON.parseObject(routeVersion.getRouteSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "routeSnapshotJson routeVersionId=" + routeVersion.getId());
        }
        JSONObject configSnapshots = snapshot == null ? null : snapshot.getJSONObject("configSnapshots");
        return configSnapshots == null ? null : configSnapshots.get(configKey);
    }

    private JSONObject toJsonObject(MesProRouteVersionDO routeVersion, Object value) {
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(value));
            if (jsonObject != null) {
                return jsonObject;
            }
        } catch (RuntimeException ex) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "routeStartProductionLeaders routeVersionId=" + routeVersion.getId());
        }
        throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                "routeStartProductionLeaders routeVersionId=" + routeVersion.getId());
    }

    private String normalizeProductionLeaderCandidateSourceType(String candidateSourceType) {
        if ("USER".equals(candidateSourceType) || CANDIDATE_SOURCE_TYPE_USERS.equals(candidateSourceType)) {
            return CANDIDATE_SOURCE_TYPE_USERS;
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE.equals(candidateSourceType)) {
            return CANDIDATE_SOURCE_TYPE_ROLE;
        }
        throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                "routeStartProductionLeaders candidateSourceType=" + candidateSourceType);
    }

    private List<Long> parseCandidateSourceIds(Object rawValue) {
        if (rawValue == null) {
            return List.of();
        }
        if (rawValue instanceof JSONArray array) {
            return array.stream()
                    .map(value -> value == null ? null : Long.valueOf(String.valueOf(value)))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        if (rawValue instanceof List<?> list) {
            return list.stream()
                    .map(value -> value == null ? null : Long.valueOf(String.valueOf(value)))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        String text = String.valueOf(rawValue).trim();
        if (text.isEmpty()) {
            return List.of();
        }
        return java.util.Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .distinct()
                .toList();
    }

    private static void requireWorkstationsPresentAndEnabled(Set<Long> workstationIds,
                                                              Map<Long, MesMdWorkstationDO> workstationMap) {
        for (Long workstationId : workstationIds) {
            MesMdWorkstationDO workstation = workstationMap == null ? null : workstationMap.get(workstationId);
            if (workstation == null || !CommonStatusEnum.isEnable(workstation.getStatus())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "workstationId=" + workstationId);
            }
        }
    }

    private Map<Long, List<MesMdWorkstationMachineDO>> resolveMachinesByWorkstation(Set<Long> workstationIds) {
        List<MesMdWorkstationMachineDO> machines =
                workstationMachineService.getWorkstationMachineListByWorkstationIds(workstationIds);
        Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstation = new LinkedHashMap<>();
        if (CollUtil.isEmpty(machines)) {
            return machinesByWorkstation;
        }
        for (MesMdWorkstationMachineDO machine : machines) {
            if (machine == null
                    || machine.getWorkstationId() == null
                    || machine.getMachineryId() == null
                    || !workstationIds.contains(machine.getWorkstationId())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "workstation machinery");
            }
            machinesByWorkstation.computeIfAbsent(machine.getWorkstationId(), ignored -> new ArrayList<>())
                    .add(machine);
        }
        for (List<MesMdWorkstationMachineDO> workstationMachines : machinesByWorkstation.values()) {
            workstationMachines.sort(Comparator.comparing(MesMdWorkstationMachineDO::getMachineryId));
        }
        return machinesByWorkstation;
    }

    private Map<Long, MesDvMachineryDO> resolveMachineryMap(
            Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstation) {
        Set<Long> machineryIds = new LinkedHashSet<>();
        for (List<MesMdWorkstationMachineDO> machines : machinesByWorkstation.values()) {
            for (MesMdWorkstationMachineDO machine : machines) {
                machineryIds.add(machine.getMachineryId());
            }
        }
        if (machineryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, MesDvMachineryDO> machineryMap = machineryService.getMachineryMap(machineryIds);
        for (Long machineryId : machineryIds) {
            if (machineryMap == null || !machineryMap.containsKey(machineryId)) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "machineryId=" + machineryId);
            }
        }
        return machineryMap;
    }

    private static void requireRouteBinding(Long loginUserId, MesFrontlineDeviceRouteBinding routeBinding) {
        if (routeBinding == null
                || routeBinding.routeId() == null
                || routeBinding.workstationId() == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "route binding");
        }
        if (routeBinding.loginUserId() != null && !Objects.equals(routeBinding.loginUserId(), loginUserId)) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "loginUserId=" + loginUserId);
        }
    }

    private static void requireRouteProcessIdentity(MesProRouteProcessDO routeProcess) {
        if (routeProcess.getId() == null || routeProcess.getRouteId() == null || routeProcess.getProcessId() == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "route process");
        }
    }

    private static void requireRouteProcessWorkstation(MesProRouteProcessDO routeProcess) {
        if (routeProcess.getWorkstationId() == null) {
            throw exception(PRO_FRONTLINE_ROUTE_PROCESS_WORKSTATION_REQUIRED,
                    routeProcess.getRouteId(), routeProcess.getProcessId());
        }
    }

    private static MesFrontlineRouteProcessCandidate toCandidate(MesFrontlineDeviceRouteBinding routeBinding,
                                                                 MesProRouteProcessDO routeProcess,
                                                                 MesProProcessDO process) {
        return new MesFrontlineRouteProcessCandidate(routeBinding.routeId(), routeBinding.routeCode(),
                routeBinding.routeName(), routeProcess.getId(), routeProcess.getProcessId(),
                process.getCode(), process.getName(), routeProcess.getSort(),
                routeBinding.deviceId(), routeBinding.deviceCode(), routeBinding.deviceName(),
                routeProcess.getWorkstationId(),
                routeBinding.workstationCode(), routeBinding.workstationName(),
                MesFrontlineRouteProcessCandidate.CONTEXT_SOURCE_POST_BINDING, routeProcess.getCheckFlag());
    }

    private static MesFrontlineRouteProcessCandidate toCandidate(MesProRouteDO route,
                                                                 MesMdWorkstationDO workstation,
                                                                 MesDvMachineryDO machinery,
                                                                 MesProRouteProcessDO routeProcess,
                                                                 MesProProcessDO process) {
        return new MesFrontlineRouteProcessCandidate(route.getId(), route.getCode(),
                route.getName(), routeProcess.getId(), routeProcess.getProcessId(),
                process.getCode(), process.getName(), routeProcess.getSort(),
                machinery == null ? null : machinery.getId(),
                machinery == null ? null : machinery.getCode(),
                machinery == null ? null : machinery.getName(),
                workstation.getId(), workstation.getCode(), workstation.getName(),
                MesFrontlineRouteProcessCandidate.CONTEXT_SOURCE_ROUTE_START_PRODUCTION_LEADER,
                routeProcess.getCheckFlag());
    }

    private static RouteWorkstationKey routeWorkstationKey(MesProRouteProcessDO routeProcess) {
        return new RouteWorkstationKey(routeProcess.getRouteId(), routeProcess.getWorkstationId());
    }

    private static Set<Long> collectPostIds(Collection<MesMdWorkstationWorkerDO> workers) {
        Set<Long> postIds = new LinkedHashSet<>();
        if (CollUtil.isEmpty(workers)) {
            return postIds;
        }
        for (MesMdWorkstationWorkerDO worker : workers) {
            if (worker != null && worker.getPostId() != null) {
                postIds.add(worker.getPostId());
            }
        }
        return postIds;
    }

    private static String displayName(MesFrontlineEmployeeCandidate candidate) {
        if (candidate.nickname() != null) {
            return candidate.nickname();
        }
        return candidate.username();
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void requireValue(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private record RouteWorkstationKey(Long routeId, Long workstationId) {
    }

    private record CandidateKey(Long routeProcessId, Long deviceId) {
    }

    private record ResponsibleLeaderContext(Long leaderUserId, boolean productionEmployee) {
    }

    private record RouteStartProductionLeaderSnapshot(Long routeId,
                                                       Long productionLineId,
                                                       String candidateSourceType,
                                                       List<Long> candidateSourceIds) {
    }

    private record RouteBindingContext(
            Set<Long> routeIds,
            Map<RouteWorkstationKey, List<MesFrontlineDeviceRouteBinding>> bindingsByRouteWorkstation) {
    }

}
