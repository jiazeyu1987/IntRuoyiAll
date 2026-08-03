package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
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

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ACTUAL_EMPLOYEE_NOT_BOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_BINDING_SOURCE_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_ROUTE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_PROCESS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PROCESS_EMPLOYEE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ROUTE_PROCESS_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ROUTE_PROCESS_WORKSTATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;

@Service
public class MesFrontlineDeviceAccountContextServiceImpl implements MesFrontlineDeviceAccountContextService {

    public static final String PRESSURE_PUMP_ALL_PROCESS_PERMISSION =
            "mes:pro-feedback:frontline-pressure-pump:all-processes";
    private static final String PRESSURE_PUMP_ROUTE_KEYWORD = "压力泵";

    private final ObjectProvider<MesFrontlineDeviceAccountRouteBindingSource> routeBindingSourceProvider;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProProcessService processService;
    private final MesMdWorkstationWorkerService workstationWorkerService;
    private final AdminUserApi adminUserApi;
    private final PermissionApi permissionApi;
    private final MesProRouteService routeService;
    private final MesMdWorkstationService workstationService;
    private final MesMdWorkstationMachineService workstationMachineService;
    private final MesDvMachineryService machineryService;

    public MesFrontlineDeviceAccountContextServiceImpl(
            ObjectProvider<MesFrontlineDeviceAccountRouteBindingSource> routeBindingSourceProvider,
            MesProRouteProcessMapper routeProcessMapper,
            MesProProcessService processService,
            MesMdWorkstationWorkerService workstationWorkerService,
            AdminUserApi adminUserApi,
            PermissionApi permissionApi,
            MesProRouteService routeService,
            MesMdWorkstationService workstationService,
            MesMdWorkstationMachineService workstationMachineService,
            MesDvMachineryService machineryService) {
        this.routeBindingSourceProvider = routeBindingSourceProvider;
        this.routeProcessMapper = routeProcessMapper;
        this.processService = processService;
        this.workstationWorkerService = workstationWorkerService;
        this.adminUserApi = adminUserApi;
        this.permissionApi = permissionApi;
        this.routeService = routeService;
        this.workstationService = workstationService;
        this.workstationMachineService = workstationMachineService;
        this.machineryService = machineryService;
    }

    @Override
    public List<MesFrontlineRouteProcessCandidate> listSwitchableProcesses(Long loginUserId) {
        requireValue(loginUserId, "loginUserId");
        if (hasPressurePumpAllProcessRolePermission(loginUserId)) {
            return listPressurePumpSwitchableProcesses(loginUserId);
        }
        return listPostBoundSwitchableProcesses(loginUserId);
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

    private List<MesFrontlineRouteProcessCandidate> listPressurePumpSwitchableProcesses(Long loginUserId) {
        Map<Long, MesProRouteDO> routeMap = listEnabledPressurePumpRouteMap();
        if (routeMap.isEmpty()) {
            throw exception(PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_EMPTY, loginUserId);
        }

        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteIds(routeMap.keySet());
        if (CollUtil.isEmpty(routeProcesses)) {
            throw exception(PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_PROCESS_EMPTY, routeMap.keySet());
        }

        Set<Long> processIds = new LinkedHashSet<>();
        Set<Long> workstationIds = new LinkedHashSet<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            if (routeProcess == null || !routeMap.containsKey(routeProcess.getRouteId())) {
                continue;
            }
            requireRouteProcessIdentity(routeProcess);
            requireRouteProcessWorkstation(routeProcess);
            processIds.add(routeProcess.getProcessId());
            workstationIds.add(routeProcess.getWorkstationId());
        }
        if (processIds.isEmpty()) {
            throw exception(PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_PROCESS_EMPTY, routeMap.keySet());
        }

        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(processIds);
        Map<Long, MesMdWorkstationDO> workstationMap = workstationService.getWorkstationMap(workstationIds);
        requireWorkstationsPresentAndEnabled(workstationIds, workstationMap);
        Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstation =
                resolveMachinesByWorkstation(workstationIds);
        Map<Long, MesDvMachineryDO> machineryMap = resolveMachineryMap(machinesByWorkstation);

        List<MesFrontlineRouteProcessCandidate> candidates = new ArrayList<>();
        Set<CandidateKey> acceptedCandidates = new LinkedHashSet<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            if (routeProcess == null || !routeMap.containsKey(routeProcess.getRouteId())) {
                continue;
            }
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
            throw exception(PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_PROCESS_EMPTY, routeMap.keySet());
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
    public List<MesFrontlineEmployeeCandidate> listEmployeeCandidates(Long loginUserId, Long routeId,
                                                                      Long routeProcessId, Long processId) {
        MesFrontlineRouteProcessCandidate processCandidate = requireAuthorizedProcess(loginUserId, routeId,
                routeProcessId, processId);
        List<MesMdWorkstationWorkerDO> workers = workstationWorkerService.getWorkstationWorkerListByWorkstationId(
                processCandidate.workstationId());
        Set<Long> postIds = collectPostIds(workers);
        if (postIds.isEmpty()) {
            throw exception(PRO_FRONTLINE_PROCESS_EMPLOYEE_EMPTY, processCandidate.workstationId(),
                    processCandidate.processId());
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserListByPostIds(postIds);
        if (CollUtil.isEmpty(users)) {
            throw exception(PRO_FRONTLINE_PROCESS_EMPLOYEE_EMPTY, processCandidate.workstationId(),
                    processCandidate.processId());
        }
        Map<Long, MesFrontlineEmployeeCandidate> candidateByUserId = new LinkedHashMap<>();
        for (AdminUserRespDTO user : users) {
            if (user == null || user.getId() == null || !CommonStatusEnum.isEnable(user.getStatus())) {
                continue;
            }
            candidateByUserId.putIfAbsent(user.getId(),
                    new MesFrontlineEmployeeCandidate(user.getId(), user.getUsername(), user.getNickname()));
        }
        if (candidateByUserId.isEmpty()) {
            throw exception(PRO_FRONTLINE_PROCESS_EMPLOYEE_EMPTY, processCandidate.workstationId(),
                    processCandidate.processId());
        }
        return candidateByUserId.values().stream()
                .sorted(Comparator
                        .comparing((MesFrontlineEmployeeCandidate candidate) -> displayName(candidate),
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesFrontlineEmployeeCandidate::userId))
                .toList();
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
    public MesFrontlineEmployeeCandidate requireBoundEmployee(Long loginUserId, Long routeId, Long routeProcessId,
                                                              Long processId, Long actualEmployeeId) {
        requireValue(actualEmployeeId, "actualEmployeeId");
        return listEmployeeCandidates(loginUserId, routeId, routeProcessId, processId).stream()
                .filter(candidate -> Objects.equals(candidate.userId(), actualEmployeeId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_ACTUAL_EMPLOYEE_NOT_BOUND, actualEmployeeId, processId));
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

    private boolean hasPressurePumpAllProcessRolePermission(Long loginUserId) {
        Set<Long> roleIds = permissionApi.getUserRoleIdListByUserId(loginUserId);
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }
        return permissionApi.hasAnyPermissionsInRoles(roleIds, PRESSURE_PUMP_ALL_PROCESS_PERMISSION);
    }

    private Map<Long, MesProRouteDO> listEnabledPressurePumpRouteMap() {
        List<MesProRouteDO> routes = routeService.getRouteListByStatus(CommonStatusEnum.ENABLE.getStatus());
        Map<Long, MesProRouteDO> routeMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(routes)) {
            return routeMap;
        }
        for (MesProRouteDO route : routes) {
            if (route == null
                    || route.getId() == null
                    || route.getName() == null
                    || !route.getName().contains(PRESSURE_PUMP_ROUTE_KEYWORD)) {
                continue;
            }
            routeMap.putIfAbsent(route.getId(), route);
        }
        return routeMap;
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
                routeBinding.workstationCode(), routeBinding.workstationName());
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
                workstation.getId(), workstation.getCode(), workstation.getName());
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

    private static void requireValue(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private record RouteWorkstationKey(Long routeId, Long workstationId) {
    }

    private record CandidateKey(Long routeProcessId, Long deviceId) {
    }

    private record RouteBindingContext(
            Set<Long> routeIds,
            Map<RouteWorkstationKey, List<MesFrontlineDeviceRouteBinding>> bindingsByRouteWorkstation) {
    }

}
