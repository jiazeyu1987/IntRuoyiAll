package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID;

/**
 * Resolves frontline route access from the login user's formal post and workstation assignments.
 */
@Service
public class MesFrontlineWorkstationPostRouteBindingSource implements MesFrontlineDeviceAccountRouteBindingSource {

    private final AdminUserApi adminUserApi;
    private final MesMdWorkstationWorkerService workstationWorkerService;
    private final MesMdWorkstationService workstationService;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProRouteService routeService;
    private final MesMdWorkstationMachineService workstationMachineService;
    private final MesDvMachineryService machineryService;

    public MesFrontlineWorkstationPostRouteBindingSource(
            AdminUserApi adminUserApi,
            MesMdWorkstationWorkerService workstationWorkerService,
            MesMdWorkstationService workstationService,
            MesProRouteProcessMapper routeProcessMapper,
            MesProRouteService routeService,
            MesMdWorkstationMachineService workstationMachineService,
            MesDvMachineryService machineryService) {
        this.adminUserApi = adminUserApi;
        this.workstationWorkerService = workstationWorkerService;
        this.workstationService = workstationService;
        this.routeProcessMapper = routeProcessMapper;
        this.routeService = routeService;
        this.workstationMachineService = workstationMachineService;
        this.machineryService = machineryService;
    }

    @Override
    public List<MesFrontlineDeviceRouteBinding> listEnabledRouteBindings(Long loginUserId) {
        AdminUserRespDTO loginUser = adminUserApi.getUser(loginUserId);
        if (loginUser == null
                || !CommonStatusEnum.isEnable(loginUser.getStatus())
                || loginUser.getPostIds() == null
                || loginUser.getPostIds().isEmpty()) {
            return List.of();
        }
        Set<Long> postIds = collectPositiveIds(loginUser.getPostIds());
        if (postIds.isEmpty()) {
            return List.of();
        }

        Set<Long> workstationIds = resolveWorkstationIds(postIds,
                workstationWorkerService.getWorkstationWorkerListByPostIds(postIds));
        if (workstationIds.isEmpty()) {
            return List.of();
        }
        Map<Long, MesMdWorkstationDO> workstationMap = workstationService.getWorkstationMap(workstationIds);
        Set<Long> enabledWorkstationIds = requireEnabledWorkstationIds(workstationIds, workstationMap);
        if (enabledWorkstationIds.isEmpty()) {
            return List.of();
        }

        List<MesProRouteProcessDO> routeProcesses =
                routeProcessMapper.selectListByWorkstationIds(enabledWorkstationIds);
        if (routeProcesses == null || routeProcesses.isEmpty()) {
            return List.of();
        }
        Set<Long> routeIds = collectRouteIds(routeProcesses, enabledWorkstationIds);
        Map<Long, MesProRouteDO> routeMap = routeService.getRouteMap(routeIds);
        requireRoutesPresent(routeIds, routeMap);

        Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstation =
                resolveMachinesByWorkstation(enabledWorkstationIds);
        Set<Long> machineryIds = collectMachineryIds(machinesByWorkstation);
        Map<Long, MesDvMachineryDO> machineryMap = machineryIds.isEmpty()
                ? Map.of() : machineryService.getMachineryMap(machineryIds);
        requireMachineryPresent(machineryIds, machineryMap);

        List<MesProRouteProcessDO> sortedRouteProcesses = new ArrayList<>(routeProcesses);
        sortedRouteProcesses.sort(Comparator
                .comparing(MesProRouteProcessDO::getRouteId)
                .thenComparing(process -> process.getSort() == null ? Integer.MAX_VALUE : process.getSort())
                .thenComparing(MesProRouteProcessDO::getId));

        Map<BindingKey, MesFrontlineDeviceRouteBinding> bindings = new LinkedHashMap<>();
        for (MesProRouteProcessDO routeProcess : sortedRouteProcesses) {
            MesProRouteDO route = routeMap.get(routeProcess.getRouteId());
            if (!CommonStatusEnum.isEnable(route.getStatus())) {
                continue;
            }
            MesMdWorkstationDO workstation = workstationMap.get(routeProcess.getWorkstationId());
            List<MesMdWorkstationMachineDO> workstationMachines =
                    machinesByWorkstation.getOrDefault(routeProcess.getWorkstationId(), List.of());
            if (workstationMachines.isEmpty()) {
                putBinding(bindings, loginUserId, route, workstation, null);
                continue;
            }
            for (MesMdWorkstationMachineDO workstationMachine : workstationMachines) {
                putBinding(bindings, loginUserId, route, workstation,
                        machineryMap.get(workstationMachine.getMachineryId()));
            }
        }
        return List.copyOf(bindings.values());
    }

    private static Set<Long> resolveWorkstationIds(Set<Long> postIds,
                                                    List<MesMdWorkstationWorkerDO> workers) {
        Set<Long> workstationIds = new LinkedHashSet<>();
        if (workers == null) {
            return workstationIds;
        }
        for (MesMdWorkstationWorkerDO worker : workers) {
            if (worker == null
                    || worker.getPostId() == null
                    || worker.getWorkstationId() == null
                    || !postIds.contains(worker.getPostId())) {
                throw invalidContext("workstation worker");
            }
            workstationIds.add(worker.getWorkstationId());
        }
        return workstationIds;
    }

    private static Set<Long> requireEnabledWorkstationIds(Set<Long> workstationIds,
                                                           Map<Long, MesMdWorkstationDO> workstationMap) {
        Set<Long> enabledWorkstationIds = new LinkedHashSet<>();
        for (Long workstationId : workstationIds) {
            MesMdWorkstationDO workstation = workstationMap.get(workstationId);
            if (workstation == null) {
                throw invalidContext("workstationId=" + workstationId);
            }
            if (CommonStatusEnum.isEnable(workstation.getStatus())) {
                enabledWorkstationIds.add(workstationId);
            }
        }
        return enabledWorkstationIds;
    }

    private static Set<Long> collectRouteIds(List<MesProRouteProcessDO> routeProcesses,
                                             Set<Long> enabledWorkstationIds) {
        Set<Long> routeIds = new LinkedHashSet<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            if (routeProcess == null
                    || routeProcess.getId() == null
                    || routeProcess.getRouteId() == null
                    || routeProcess.getProcessId() == null
                    || routeProcess.getWorkstationId() == null
                    || !enabledWorkstationIds.contains(routeProcess.getWorkstationId())) {
                throw invalidContext("route process");
            }
            routeIds.add(routeProcess.getRouteId());
        }
        return routeIds;
    }

    private static void requireRoutesPresent(Set<Long> routeIds, Map<Long, MesProRouteDO> routeMap) {
        for (Long routeId : routeIds) {
            if (!routeMap.containsKey(routeId)) {
                throw invalidContext("routeId=" + routeId);
            }
        }
    }

    private Map<Long, List<MesMdWorkstationMachineDO>> resolveMachinesByWorkstation(
            Set<Long> enabledWorkstationIds) {
        List<MesMdWorkstationMachineDO> machines =
                workstationMachineService.getWorkstationMachineListByWorkstationIds(enabledWorkstationIds);
        Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstation = new LinkedHashMap<>();
        if (machines == null) {
            return machinesByWorkstation;
        }
        for (MesMdWorkstationMachineDO machine : machines) {
            if (machine == null
                    || machine.getWorkstationId() == null
                    || machine.getMachineryId() == null
                    || !enabledWorkstationIds.contains(machine.getWorkstationId())) {
                throw invalidContext("workstation machinery");
            }
            machinesByWorkstation.computeIfAbsent(machine.getWorkstationId(), ignored -> new ArrayList<>())
                    .add(machine);
        }
        for (List<MesMdWorkstationMachineDO> workstationMachines : machinesByWorkstation.values()) {
            workstationMachines.sort(Comparator.comparing(MesMdWorkstationMachineDO::getMachineryId));
        }
        return machinesByWorkstation;
    }

    private static Set<Long> collectMachineryIds(
            Map<Long, List<MesMdWorkstationMachineDO>> machinesByWorkstation) {
        Set<Long> machineryIds = new LinkedHashSet<>();
        for (List<MesMdWorkstationMachineDO> machines : machinesByWorkstation.values()) {
            for (MesMdWorkstationMachineDO machine : machines) {
                machineryIds.add(machine.getMachineryId());
            }
        }
        return machineryIds;
    }

    private static void requireMachineryPresent(Set<Long> machineryIds,
                                                Map<Long, MesDvMachineryDO> machineryMap) {
        for (Long machineryId : machineryIds) {
            if (!machineryMap.containsKey(machineryId)) {
                throw invalidContext("machineryId=" + machineryId);
            }
        }
    }

    private static void putBinding(Map<BindingKey, MesFrontlineDeviceRouteBinding> bindings,
                                   Long loginUserId,
                                   MesProRouteDO route,
                                   MesMdWorkstationDO workstation,
                                   MesDvMachineryDO machinery) {
        Long machineryId = machinery == null ? null : machinery.getId();
        BindingKey key = new BindingKey(route.getId(), workstation.getId(), machineryId);
        bindings.putIfAbsent(key, new MesFrontlineDeviceRouteBinding(
                loginUserId,
                route.getId(),
                route.getCode(),
                route.getName(),
                machineryId,
                machinery == null ? null : machinery.getCode(),
                machinery == null ? null : machinery.getName(),
                workstation.getId(),
                workstation.getCode(),
                workstation.getName()));
    }

    private static Set<Long> collectPositiveIds(Set<Long> ids) {
        Set<Long> result = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) {
                result.add(id);
            }
        }
        return result;
    }

    private static RuntimeException invalidContext(String detail) {
        return exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, detail);
    }

    private record BindingKey(Long routeId, Long workstationId, Long machineryId) {
    }

}
