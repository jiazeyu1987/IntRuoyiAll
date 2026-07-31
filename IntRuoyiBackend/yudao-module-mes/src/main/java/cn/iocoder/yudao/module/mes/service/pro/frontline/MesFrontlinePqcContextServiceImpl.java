package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ACTIVE_ORDER_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ACTIVE_ORDER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ACTIVE_ORDER_ROUTE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_PERSONNEL_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ROUTE_PROCESS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesFrontlinePqcContextServiceImpl implements MesFrontlinePqcContextService {

    private final MesProProcessPoolMapper processPoolMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProRouteMapper routeMapper;
    private final MesProRouteProductMapper routeProductMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProProcessService processService;
    private final MesMdItemService itemService;
    private final MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    private final AdminUserApi adminUserApi;
    private final MesFrontlineTemplateResolver templateResolver;

    public MesFrontlinePqcContextServiceImpl(MesProProcessPoolMapper processPoolMapper,
                                             MesProWorkOrderMapper workOrderMapper,
                                             MesProRouteMapper routeMapper,
                                             MesProRouteProductMapper routeProductMapper,
                                             MesProRouteProcessMapper routeProcessMapper,
                                             MesProProcessService processService,
                                             MesMdItemService itemService,
                                             MesProcessPoolTeamLeaderScopeMapper scopeMapper,
                                             AdminUserApi adminUserApi,
                                             MesFrontlineTemplateResolver templateResolver) {
        this.processPoolMapper = processPoolMapper;
        this.workOrderMapper = workOrderMapper;
        this.routeMapper = routeMapper;
        this.routeProductMapper = routeProductMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.processService = processService;
        this.itemService = itemService;
        this.scopeMapper = scopeMapper;
        this.adminUserApi = adminUserApi;
        this.templateResolver = templateResolver;
    }

    @Override
    public List<MesFrontlineActiveOrderCandidate> listActiveOrders() {
        List<MesProProcessPoolDO> activePools = processPoolMapper.selectActiveList();
        if (CollUtil.isEmpty(activePools)) {
            throw exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_EMPTY);
        }

        Map<ActiveOrderKey, LocalDateTime> latestSubmitTimeByOrder = new LinkedHashMap<>();
        for (MesProProcessPoolDO pool : activePools) {
            requireActivePoolIdentity(pool);
            ActiveOrderKey key = new ActiveOrderKey(pool.getWorkOrderId(), pool.getRouteId());
            LocalDateTime current = latestSubmitTimeByOrder.get(key);
            if (current == null || isAfter(pool.getLatestSubmitTime(), current)) {
                latestSubmitTimeByOrder.put(key, pool.getLatestSubmitTime());
            }
        }

        Set<Long> workOrderIds = latestSubmitTimeByOrder.keySet().stream()
                .map(ActiveOrderKey::workOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> routeIds = latestSubmitTimeByOrder.keySet().stream()
                .map(ActiveOrderKey::routeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProWorkOrderDO> workOrderMap = mapById(workOrderMapper.selectListByIds(workOrderIds),
                MesProWorkOrderDO::getId);
        Map<Long, MesProRouteDO> routeMap = mapById(routeMapper.selectListByIdsIgnoreDeleted(routeIds),
                MesProRouteDO::getId);
        Set<Long> productIds = workOrderMap.values().stream()
                .map(MesProWorkOrderDO::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesMdItemDO> itemMap = productIds.isEmpty() ? Map.of() : itemService.getItemMap(productIds);

        List<MesFrontlineActiveOrderCandidate> candidates = new ArrayList<>();
        for (Map.Entry<ActiveOrderKey, LocalDateTime> entry : latestSubmitTimeByOrder.entrySet()) {
            ActiveOrderKey key = entry.getKey();
            MesProWorkOrderDO workOrder = requireWorkOrder(workOrderMap, key.workOrderId());
            MesProRouteDO route = requireRoute(routeMap, key.routeId());
            requireProductRoute(workOrder, key.routeId());
            MesMdItemDO item = requireProduct(itemMap, workOrder.getProductId());
            candidates.add(new MesFrontlineActiveOrderCandidate(workOrder.getId(), workOrder.getCode(),
                    workOrder.getName(), workOrder.getProductId(), item.getCode(), item.getName(),
                    route.getId(), route.getCode(), route.getName(), entry.getValue()));
        }
        candidates.sort(Comparator
                .comparing(MesFrontlineActiveOrderCandidate::latestSubmitTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MesFrontlineActiveOrderCandidate::workOrderId));
        return candidates;
    }

    @Override
    public List<MesFrontlineRouteProcessCandidate> listProcessesByActiveOrder(Long workOrderId, Long routeId) {
        requireActiveOrder(workOrderId, routeId);
        MesProWorkOrderDO workOrder = requireWorkOrder(workOrderId);
        requireProductRoute(workOrder, routeId);
        MesProRouteDO route = requireRoute(routeId);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        if (CollUtil.isEmpty(routeProcesses)) {
            throw exception(PRO_FRONTLINE_PQC_ROUTE_PROCESS_EMPTY, workOrderId, routeId);
        }
        Set<Long> processIds = routeProcesses.stream()
                .filter(Objects::nonNull)
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(processIds);

        List<MesFrontlineRouteProcessCandidate> candidates = new ArrayList<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            requireRouteProcessIdentity(routeProcess, routeId);
            MesProProcessDO process = processMap.get(routeProcess.getProcessId());
            if (process == null || !CommonStatusEnum.isEnable(process.getStatus())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "processId=" + routeProcess.getProcessId());
            }
            candidates.add(new MesFrontlineRouteProcessCandidate(route.getId(), route.getCode(), route.getName(),
                    routeProcess.getId(), routeProcess.getProcessId(), process.getCode(), process.getName(),
                    routeProcess.getSort(), null, null, null, routeProcess.getWorkstationId(), null, null));
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparing((MesFrontlineRouteProcessCandidate candidate) ->
                                candidate.sort() == null ? Integer.MAX_VALUE : candidate.sort())
                        .thenComparing(MesFrontlineRouteProcessCandidate::routeProcessId))
                .toList();
    }

    @Override
    public List<MesFrontlineEmployeeCandidate> listPqcEmployeeCandidates() {
        List<MesProcessPoolTeamLeaderScopeDO> scopes = scopeMapper.selectActiveScopesByLeaderType(LEADER_TYPE_PQC);
        Set<Long> userIds = new LinkedHashSet<>();
        for (MesProcessPoolTeamLeaderScopeDO scope : scopes) {
            if (scope == null || !Boolean.TRUE.equals(scope.getEnabled())) {
                continue;
            }
            if (scope.getLeaderUserId() != null) {
                userIds.add(scope.getLeaderUserId());
            }
            if (SCOPE_TYPE_EMPLOYEE.equals(scope.getScopeType()) && scope.getEmployeeUserId() != null) {
                userIds.add(scope.getEmployeeUserId());
            }
        }
        if (userIds.isEmpty()) {
            throw exception(PRO_FRONTLINE_PQC_PERSONNEL_EMPTY);
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(userIds);
        if (CollUtil.isEmpty(users)) {
            throw exception(PRO_FRONTLINE_PQC_PERSONNEL_EMPTY);
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
            throw exception(PRO_FRONTLINE_PQC_PERSONNEL_EMPTY);
        }
        return candidateByUserId.values().stream()
                .sorted(Comparator
                        .comparing((MesFrontlineEmployeeCandidate candidate) -> displayName(candidate),
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesFrontlineEmployeeCandidate::userId))
                .toList();
    }

    @Override
    public MesFrontlineRouteProcessCandidate requireActiveOrderProcess(Long workOrderId, Long routeId,
                                                                       Long routeProcessId, Long processId) {
        requireValue(routeProcessId, "routeProcessId");
        requireValue(processId, "processId");
        return listProcessesByActiveOrder(workOrderId, routeId).stream()
                .filter(candidate -> Objects.equals(candidate.routeProcessId(), routeProcessId)
                        && Objects.equals(candidate.processId(), processId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_REQUIRED, workOrderId, routeId));
    }

    @Override
    public MesFrontlineEmployeeCandidate requirePqcEmployee(Long actualEmployeeId) {
        requireValue(actualEmployeeId, "actualEmployeeId");
        return listPqcEmployeeCandidates().stream()
                .filter(candidate -> Objects.equals(candidate.userId(), actualEmployeeId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND, actualEmployeeId));
    }

    @Override
    public MesFrontlineEmployeeSwitchResult switchPqcActualEmployee(Long loginUserId, Long workOrderId, Long routeId,
                                                                    Long routeProcessId, Long processId,
                                                                    Long actualEmployeeId) {
        requireValue(loginUserId, "loginUserId");
        MesFrontlineRouteProcessCandidate process = requireActiveOrderProcess(workOrderId, routeId,
                routeProcessId, processId);
        requirePqcEmployee(actualEmployeeId);
        MesFrontlineTemplateDescriptor template = templateResolver.resolve(new MesFrontlineTemplateRequest(
                loginUserId, actualEmployeeId, process.routeId(), process.routeProcessId(), process.processId()));
        return new MesFrontlineEmployeeSwitchResult(loginUserId, actualEmployeeId, process.routeId(),
                process.routeProcessId(), process.processId(), false, template);
    }

    private void requireActiveOrder(Long workOrderId, Long routeId) {
        requireValue(workOrderId, "workOrderId");
        requireValue(routeId, "routeId");
        if (processPoolMapper.selectActiveByWorkOrderAndRoute(workOrderId, routeId) == null) {
            throw exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_REQUIRED, workOrderId, routeId);
        }
    }

    private MesProWorkOrderDO requireWorkOrder(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null || workOrder.getProductId() == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "workOrderId=" + workOrderId);
        }
        return workOrder;
    }

    private static MesProWorkOrderDO requireWorkOrder(Map<Long, MesProWorkOrderDO> workOrderMap, Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderMap.get(workOrderId);
        if (workOrder == null || workOrder.getProductId() == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "workOrderId=" + workOrderId);
        }
        return workOrder;
    }

    private MesProRouteDO requireRoute(Long routeId) {
        MesProRouteDO route = routeMapper.selectByIdIgnoreDeleted(routeId);
        if (route == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "routeId=" + routeId);
        }
        return route;
    }

    private static MesProRouteDO requireRoute(Map<Long, MesProRouteDO> routeMap, Long routeId) {
        MesProRouteDO route = routeMap.get(routeId);
        if (route == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "routeId=" + routeId);
        }
        return route;
    }

    private void requireProductRoute(MesProWorkOrderDO workOrder, Long routeId) {
        if (workOrder.getProductId() == null
                || routeProductMapper.selectByRouteIdAndItemId(routeId, workOrder.getProductId()) == null) {
            throw exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_ROUTE_REQUIRED,
                    workOrder.getId(), workOrder.getProductId(), routeId);
        }
    }

    private static MesMdItemDO requireProduct(Map<Long, MesMdItemDO> itemMap, Long productId) {
        MesMdItemDO item = itemMap.get(productId);
        if (item == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "productId=" + productId);
        }
        return item;
    }

    private static void requireActivePoolIdentity(MesProProcessPoolDO pool) {
        if (pool == null || pool.getWorkOrderId() == null || pool.getRouteId() == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "active process pool");
        }
    }

    private static void requireRouteProcessIdentity(MesProRouteProcessDO routeProcess, Long routeId) {
        if (routeProcess == null
                || routeProcess.getId() == null
                || routeProcess.getRouteId() == null
                || routeProcess.getProcessId() == null
                || !Objects.equals(routeProcess.getRouteId(), routeId)) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "route process");
        }
    }

    private static void requireValue(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private static boolean isAfter(LocalDateTime candidate, LocalDateTime current) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        return candidate.isAfter(current);
    }

    private static String displayName(MesFrontlineEmployeeCandidate candidate) {
        if (candidate.nickname() != null) {
            return candidate.nickname();
        }
        return candidate.username();
    }

    private static <T> Map<Long, T> mapById(Collection<T> rows, Function<T, Long> idGetter) {
        if (CollUtil.isEmpty(rows)) {
            return Map.of();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> idGetter.apply(row) != null)
                .collect(Collectors.toMap(idGetter, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private record ActiveOrderKey(Long workOrderId, Long routeId) {
    }
}

