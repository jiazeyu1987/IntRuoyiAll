package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateCodes;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateTypes;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ACTIVE_ORDER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ACTIVE_ORDER_ROUTE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_PERSONNEL_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_REGULATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ROUTE_PROCESS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesFrontlinePqcContextServiceImpl implements MesFrontlinePqcContextService {

    private static final String PQC_INSPECTION_TASK_SOURCE_TYPE = "MES_PQC_INSPECTION_TASK";
    private static final String PQC_TASK_STATUS_PENDING = "PENDING";
    private static final String PQC_TASK_STATUS_SUBMITTED = "SUBMITTED";
    private static final String PQC_TASK_STATUS_CANCELLED = "CANCELLED";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper activeOrderProcessSnapshotMapper;
    private final MesProProcessPoolMapper processPoolMapper;
    private final MesProProcessPoolEventMapper processPoolEventMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProRouteMapper routeMapper;
    private final MesProRouteProductMapper routeProductMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationVersionMapper versionMapper;
    private final MesQaInspectionRegulationItemMapper regulationItemMapper;
    private final MesQaInspectionRegulationItemEquipmentMapper regulationItemEquipmentMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    private final MesProProcessService processService;
    private final MesMdItemService itemService;
    private final MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    private final AdminUserApi adminUserApi;
    private final MesFrontlineTemplateResolver templateResolver;
    private final MesProcessPoolEventService processPoolEventService;
    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private final MesProBatchRecordExecutionSignatureService signatureService;

    public MesFrontlinePqcContextServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                             MesProcessPoolActiveOrderProcessSnapshotMapper activeOrderProcessSnapshotMapper,
                                             MesProProcessPoolMapper processPoolMapper,
                                             MesProProcessPoolEventMapper processPoolEventMapper,
                                             MesProWorkOrderMapper workOrderMapper,
                                             MesProRouteMapper routeMapper,
                                             MesProRouteProductMapper routeProductMapper,
                                             MesProRouteProcessMapper routeProcessMapper,
                                             MesQaInspectionRegulationMapper regulationMapper,
                                             MesQaInspectionRegulationVersionMapper versionMapper,
                                             MesQaInspectionRegulationItemMapper regulationItemMapper,
                                             MesQaInspectionRegulationItemEquipmentMapper regulationItemEquipmentMapper,
                                             MesPqcInspectionTaskMapper pqcTaskMapper,
                                             MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper,
                                             MesProProcessService processService,
                                             MesMdItemService itemService,
                                             MesProcessPoolTeamLeaderScopeMapper scopeMapper,
                                             AdminUserApi adminUserApi,
                                             MesFrontlineTemplateResolver templateResolver,
                                             MesProcessPoolEventService processPoolEventService,
                                             MesProProcessPoolPqcRecordMapper pqcRecordMapper,
                                             MesProBatchRecordExecutionSignatureService signatureService) {
        this.activeOrderMapper = activeOrderMapper;
        this.activeOrderProcessSnapshotMapper = activeOrderProcessSnapshotMapper;
        this.processPoolMapper = processPoolMapper;
        this.processPoolEventMapper = processPoolEventMapper;
        this.workOrderMapper = workOrderMapper;
        this.routeMapper = routeMapper;
        this.routeProductMapper = routeProductMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.regulationMapper = regulationMapper;
        this.versionMapper = versionMapper;
        this.regulationItemMapper = regulationItemMapper;
        this.regulationItemEquipmentMapper = regulationItemEquipmentMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.pqcPieceDetailMapper = pqcPieceDetailMapper;
        this.processService = processService;
        this.itemService = itemService;
        this.scopeMapper = scopeMapper;
        this.adminUserApi = adminUserApi;
        this.templateResolver = templateResolver;
        this.processPoolEventService = processPoolEventService;
        this.pqcRecordMapper = pqcRecordMapper;
        this.signatureService = signatureService;
    }

    @Override
    public List<MesFrontlineActiveOrderCandidate> listActiveOrders() {
        List<MesProcessPoolActiveOrderDO> activeOrders = activeOrderMapper.selectActiveList();
        if (CollUtil.isEmpty(activeOrders)) {
            return List.of();
        }

        Map<ActiveOrderKey, LatestActiveOrderContext> latestActiveOrderByOrder = new LinkedHashMap<>();
        for (MesProcessPoolActiveOrderDO activeOrder : activeOrders) {
            requireActiveOrderIdentity(activeOrder);
            ActiveOrderKey key = new ActiveOrderKey(activeOrder.getWorkOrderId(), activeOrder.getRouteId());
            LatestActiveOrderContext current = latestActiveOrderByOrder.get(key);
            if (current == null || isAfter(activeOrder.getJoinedAt(), current.latestSubmitTime())) {
                latestActiveOrderByOrder.put(key,
                        new LatestActiveOrderContext(activeOrder, activeOrder.getJoinedAt()));
            }
        }
        Set<Long> pendingActiveOrderIds = pqcTaskMapper.selectActiveOrderIdsByTaskStatus(
                latestActiveOrderByOrder.values().stream()
                        .map(LatestActiveOrderContext::activeOrder)
                        .map(MesProcessPoolActiveOrderDO::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                PQC_TASK_STATUS_PENDING);
        latestActiveOrderByOrder.entrySet().removeIf(entry ->
                !pendingActiveOrderIds.contains(entry.getValue().activeOrder().getId()));
        if (latestActiveOrderByOrder.isEmpty()) {
            return List.of();
        }

        Set<Long> workOrderIds = latestActiveOrderByOrder.keySet().stream()
                .map(ActiveOrderKey::workOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> routeIds = latestActiveOrderByOrder.keySet().stream()
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
        for (Map.Entry<ActiveOrderKey, LatestActiveOrderContext> entry : latestActiveOrderByOrder.entrySet()) {
            ActiveOrderKey key = entry.getKey();
            MesProWorkOrderDO workOrder = requireWorkOrder(workOrderMap, key.workOrderId());
            MesProRouteDO route = requireRoute(routeMap, key.routeId());
            requireProductRoute(workOrder, key.routeId());
            MesMdItemDO item = requireProduct(itemMap, workOrder.getProductId());
            validateActiveOrderSummary(workOrder, item);
            candidates.add(new MesFrontlineActiveOrderCandidate(workOrder.getId(), workOrder.getCode(),
                    workOrder.getName(), workOrder.getProductId(), item.getCode(), item.getName(),
                    workOrder.getQuantity(), route.getId(), route.getCode(), route.getName(),
                    entry.getValue().latestSubmitTime()));
        }
        candidates.sort(Comparator
                .comparing(MesFrontlineActiveOrderCandidate::latestSubmitTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MesFrontlineActiveOrderCandidate::workOrderId));
        return candidates;
    }

    @Override
    public List<MesFrontlineRouteProcessCandidate> listProcessesByActiveOrder(Long workOrderId, Long routeId) {
        MesProcessPoolActiveOrderDO activeOrder = requireActiveOrder(workOrderId, routeId);
        MesProWorkOrderDO workOrder = requireWorkOrder(workOrderId);
        requireProductRoute(workOrder, routeId);
        MesProRouteDO route = requireRoute(routeId);
        Map<Long, MesProRouteProcessDO> currentRouteProcessById = mapById(routeProcessMapper.selectListByRouteId(routeId),
                MesProRouteProcessDO::getId);
        List<MesProRouteProcessDO> routeProcesses = resolveQaInspectionRouteProcesses(activeOrder, workOrder,
                regulationMapper.selectPublishedListByProductRouteVersion(workOrder.getProductId(), routeId,
                        activeOrder.getRouteVersionId()),
                currentRouteProcessById);
        Set<Long> processIds = routeProcesses.stream()
                .filter(Objects::nonNull)
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(processIds);
        Map<PqcTaskProcessKey, List<MesPqcInspectionTaskDO>> tasksByProcess = groupTasksByProcess(
                pqcTaskMapper.selectListByActiveOrderId(activeOrder.getId()));
        Set<PqcTaskProcessKey> qaProcessKeys = routeProcesses.stream()
                .map(routeProcess -> new PqcTaskProcessKey(routeProcess.getId(), routeProcess.getProcessId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        requirePendingTasksBackedByQaProcesses(activeOrder, qaProcessKeys, tasksByProcess);
        Map<PqcTaskProcessKey, List<MesFrontlineProductionSubmitCandidate>> productionSubmitsByProcess =
                groupProductionSubmitCandidates(workOrderId, routeId,
                        processPoolEventMapper.selectProductionSubmitsByWorkOrderAndRoute(workOrderId, routeId));

        List<MesFrontlineRouteProcessCandidate> candidates = new ArrayList<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            requireRouteProcessIdentity(routeProcess, routeId);
            MesProProcessDO process = processMap.get(routeProcess.getProcessId());
            if (process == null || !CommonStatusEnum.isEnable(process.getStatus())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "processId=" + routeProcess.getProcessId());
            }
            PqcTaskProcessKey processKey = new PqcTaskProcessKey(routeProcess.getId(), routeProcess.getProcessId());
            List<MesFrontlinePqcTaskContext> taskContexts = resolvePendingPqcTaskContexts(activeOrder, workOrder,
                    routeProcess,
                    tasksByProcess.get(new PqcTaskProcessKey(routeProcess.getId(), routeProcess.getProcessId())));
            MesFrontlinePqcTaskContext taskContext = taskContexts.isEmpty() ? null : taskContexts.get(0);
            List<MesFrontlinePqcTaskOption> taskOptions = taskContexts.stream()
                    .map(MesFrontlinePqcContextServiceImpl::toPqcTaskOption)
                    .toList();
            candidates.add(new MesFrontlineRouteProcessCandidate(route.getId(), route.getCode(), route.getName(),
                    routeProcess.getId(), routeProcess.getProcessId(), process.getCode(), process.getName(),
                    routeProcess.getSort(), null, null, null, routeProcess.getWorkstationId(), null, null,
                    activeOrder.getId(), taskContext == null ? null : taskContext.task().getId(),
                    taskContext == null ? null : taskContext.task().getRegulationVersionId(),
                    taskContext == null ? null : taskContext.finalInspectionApplicable(),
                    taskContext == null ? null : taskContext.task().getInspectionType(),
                    taskContext == null ? null : taskContext.task().getBusinessDate(),
                    taskContext == null ? null : taskContext.task().getShiftCode(),
                    taskContext == null ? null : taskContext.task().getRoundNo(),
                    taskContext == null ? null : taskContext.task().getPlannedInspectionQuantity(),
                    taskContext == null ? List.of() : taskContext.inspectionItems(),
                    taskOptions,
                    productionSubmitsByProcess.getOrDefault(processKey, List.of()),
                    MesFrontlineRouteProcessCandidate.CONTEXT_SOURCE_PQC_ACTIVE_ORDER));
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparing((MesFrontlineRouteProcessCandidate candidate) ->
                                candidate.sort() == null ? Integer.MAX_VALUE : candidate.sort())
                        .thenComparing(MesFrontlineRouteProcessCandidate::routeProcessId))
                .toList();
    }

    private List<MesProRouteProcessDO> resolveQaInspectionRouteProcesses(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            List<MesQaInspectionRegulationDO> publishedRegulations,
            Map<Long, MesProRouteProcessDO> currentRouteProcessById) {
        if (CollUtil.isEmpty(publishedRegulations)) {
            throw exception(PRO_FRONTLINE_PQC_ROUTE_PROCESS_EMPTY,
                    activeOrder.getWorkOrderId(), activeOrder.getRouteId());
        }
        List<MesProRouteProcessDO> routeProcesses = new ArrayList<>();
        Set<PqcTaskProcessKey> processKeys = new LinkedHashSet<>();
        int qaProcessSort = 1;
        for (MesQaInspectionRegulationDO regulation : publishedRegulations) {
            if (regulation == null) {
                continue;
            }
            requireQaInspectionRegulationProcessIdentity(activeOrder, workOrder, regulation);
            PqcTaskProcessKey key = new PqcTaskProcessKey(regulation.getRouteProcessId(), regulation.getProcessId());
            if (!processKeys.add(key)) {
                continue;
            }
            routeProcesses.add(toQaInspectionRouteProcess(regulation,
                    currentRouteProcessById.get(regulation.getRouteProcessId()), qaProcessSort++));
        }
        if (routeProcesses.isEmpty()) {
            throw exception(PRO_FRONTLINE_PQC_ROUTE_PROCESS_EMPTY,
                    activeOrder.getWorkOrderId(), activeOrder.getRouteId());
        }
        return routeProcesses;
    }

    private static MesProRouteProcessDO toQaInspectionRouteProcess(MesQaInspectionRegulationDO regulation,
                                                                   MesProRouteProcessDO currentRouteProcess,
                                                                   int qaProcessSort) {
        return MesProRouteProcessDO.builder()
                .id(regulation.getRouteProcessId())
                .routeId(regulation.getRouteId())
                .processId(regulation.getProcessId())
                .workstationId(currentRouteProcess == null ? null : currentRouteProcess.getWorkstationId())
                .sort(currentRouteProcess == null ? qaProcessSort : currentRouteProcess.getSort())
                .build();
    }

    @Override
    public List<MesFrontlineEmployeeCandidate> listPqcEmployeeCandidates(Long loginUserId) {
        requireValue(loginUserId, "loginUserId");
        List<MesProcessPoolTeamLeaderScopeDO> scopes = scopeMapper.selectActiveScopesByLeaderType(LEADER_TYPE_PQC);
        Set<Long> userIds = new LinkedHashSet<>();
        boolean loginUserInPqcScope = false;
        for (MesProcessPoolTeamLeaderScopeDO scope : scopes) {
            if (scope == null || !Boolean.TRUE.equals(scope.getEnabled())) {
                continue;
            }
            boolean leaderMatchesLogin = Objects.equals(scope.getLeaderUserId(), loginUserId);
            boolean employeeMatchesLogin = SCOPE_TYPE_EMPLOYEE.equals(scope.getScopeType())
                    && Objects.equals(scope.getEmployeeUserId(), loginUserId);
            loginUserInPqcScope = loginUserInPqcScope || leaderMatchesLogin || employeeMatchesLogin;
            if (scope.getLeaderUserId() != null) {
                userIds.add(scope.getLeaderUserId());
            }
            if (SCOPE_TYPE_EMPLOYEE.equals(scope.getScopeType()) && scope.getEmployeeUserId() != null) {
                userIds.add(scope.getEmployeeUserId());
            }
        }
        if (!loginUserInPqcScope || userIds.isEmpty()) {
            throw exception(PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND, loginUserId);
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
            if (Objects.equals(user.getId(), loginUserId)) {
                candidateByUserId.putIfAbsent(user.getId(),
                        new MesFrontlineEmployeeCandidate(user.getId(), user.getUsername(), user.getNickname()));
            }
        }
        if (candidateByUserId.isEmpty()) {
            throw exception(PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND, loginUserId);
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
    public MesFrontlineEmployeeCandidate requirePqcEmployee(Long loginUserId, Long actualEmployeeId) {
        requireValue(loginUserId, "loginUserId");
        requireValue(actualEmployeeId, "actualEmployeeId");
        if (!Objects.equals(loginUserId, actualEmployeeId)) {
            throw exception(PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND, actualEmployeeId);
        }
        return listPqcEmployeeCandidates(loginUserId).stream()
                .filter(candidate -> Objects.equals(candidate.userId(), actualEmployeeId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND, actualEmployeeId));
    }

    @Override
    public MesFrontlineEmployeeSwitchResult switchPqcActualEmployee(Long loginUserId, Long workOrderId, Long routeId,
                                                                    Long routeProcessId, Long processId,
                                                                    Long actualEmployeeId) {
        requireValue(loginUserId, "loginUserId");
        requirePqcEmployee(loginUserId, actualEmployeeId);
        MesFrontlineRouteProcessCandidate process = requireActiveOrderProcess(workOrderId, routeId,
                routeProcessId, processId);
        MesFrontlineTemplateDescriptor template = new MesFrontlineTemplateDescriptor(
                FrontlineTemplateCodes.PQC_SIMPLIFIED, FrontlineTemplateTypes.PQC,
                process.routeProcessId(), process.processId(), actualEmployeeId);
        return new MesFrontlineEmployeeSwitchResult(loginUserId, actualEmployeeId, process.routeId(),
                process.routeProcessId(), process.processId(), false, template);
    }

    @Override
    public Optional<MesFrontlinePqcSubmitResult> getSubmittedPqcInspection(Long loginUserId, Long pqcTaskId) {
        requireValue(loginUserId, "loginUserId");
        requirePositive(pqcTaskId, "pqcTaskId");
        MesProProcessPoolEventDO event = processPoolEventMapper.selectLatestPqcByTaskId(
                PQC_INSPECTION_TASK_SOURCE_TYPE, pqcTaskId);
        if (event == null) {
            return Optional.empty();
        }
        if (!Objects.equals(loginUserId, event.getActualEmployeeId())) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "pqcSubmitReceipt.pqcTaskId=" + pqcTaskId);
        }
        return Optional.of(loadPqcSubmitResult(event.getId(), pqcTaskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesFrontlinePqcSubmitResult submitPqcInspection(Long loginUserId, MesFrontlinePqcSubmitCommand command) {
        requireValue(loginUserId, "loginUserId");
        requirePqcSubmitCommand(command);
        MesPqcInspectionTaskDO task = pqcTaskMapper.selectById(command.getPqcTaskId());
        applyPqcTaskContext(command, task, loginUserId);
        String nonconformanceDescription = normalizeNonconformanceDescription(
                command.getNonconformanceDescription());
        Optional<Long> existingPqcEventId = processPoolEventService.findExistingPqcInspectionEventId(
                buildPqcInspectionLookup(command));
        if (existingPqcEventId.isPresent()) {
            return loadPqcSubmitResult(existingPqcEventId.get(), command.getPqcTaskId());
        }
        List<MesFrontlinePqcInspectionItem> inspectionItems = resolveSubmittedInspectionItems(task, command);
        List<MesPqcInspectionPieceDetailDO> pieceDetails = buildPieceDetails(task.getId(), command,
                inspectionItems);
        String inspectionResult = resolvePqcInspectionResult(command.getScrapQuantity(), pieceDetails);
        String rawPayload = buildPqcInspectionEventRawPayload(command, pieceDetails,
                inspectionResult, nonconformanceDescription);
        Long signatureId = signatureService.recordPqcSubmitSignature(command.getSignaturePassword(),
                "PQC任务" + task.getId() + "正式提交");
        requirePositive(signatureId, "signatureId");
        Map<String, Object> signatureSnapshot = new LinkedHashMap<>();
        signatureSnapshot.put("signatureId", signatureId);
        signatureSnapshot.put("actorId", loginUserId);
        signatureSnapshot.put("actionType", MesProBatchRecordExecutionSignatureService.ACTION_PQC_SUBMIT);
        signatureSnapshot.put("pqcTaskId", task.getId());

        pqcTaskMapper.updateSubmittedIfPending(task.getId(), command.getActualInspectionQuantity(),
                PQC_TASK_STATUS_PENDING, PQC_TASK_STATUS_SUBMITTED);
        if (CollUtil.isNotEmpty(pieceDetails)) {
            pqcPieceDetailMapper.insertBatch(pieceDetails);
        }
        Long eventId = processPoolEventService.createPqcInspectionEvent(MesProcessPoolCreatePqcInspectionReqDTO.builder()
                .workOrderId(command.getWorkOrderId())
                .productionSubmitEventId(command.getProductionSubmitEventId())
                .pqcSubmissionIdempotencyKey(command.getPqcSubmissionIdempotencyKey())
                .routeId(command.getRouteId())
                .routeProcessId(command.getRouteProcessId())
                .processId(command.getProcessId())
                .actualEmployeeId(command.getActualEmployeeId())
                .deviceAccountId(command.getDeviceAccountId())
                .deviceId(command.getDeviceId())
                .workstationId(command.getWorkstationId())
                .templateType(command.getTemplateType())
                .feedbackSourceType(PQC_INSPECTION_TASK_SOURCE_TYPE)
                .feedbackSourceId(task.getId())
                .recordbookSourceType(PQC_INSPECTION_TASK_SOURCE_TYPE)
                .recordbookSourceId(task.getId())
                .inspectionResult(inspectionResult)
                .rawPayload(rawPayload)
                .clientSubmitTime(command.getClientSubmitTime())
                .signatureId(signatureId)
                .signatureUserId(loginUserId)
                .signatureSnapshot(JsonUtils.toJsonString(signatureSnapshot))
                .build());
        return loadPqcSubmitResult(eventId, task.getId());
    }

    private String buildPqcInspectionEventRawPayload(MesFrontlinePqcSubmitCommand command,
                                                     List<MesPqcInspectionPieceDetailDO> pieceDetails,
                                                     String inspectionResult,
                                                     String nonconformanceDescription) {
        Map<String, Object> payload = new LinkedHashMap<>(command.getRawPayload());
        payload.put("activeOrderId", command.getActiveOrderId());
        payload.put("pqcTaskId", command.getPqcTaskId());
        payload.put("regulationVersionId", command.getRegulationVersionId());
        payload.put("workOrderId", command.getWorkOrderId());
        payload.put("routeId", command.getRouteId());
        payload.put("routeProcessId", command.getRouteProcessId());
        payload.put("processId", command.getProcessId());
        payload.put("inspectionType", command.getInspectionType());
        payload.put("businessDate", command.getBusinessDate());
        payload.put("shiftCode", command.getShiftCode());
        payload.put("roundNo", command.getRoundNo());
        payload.put("actualInspectionQuantity", command.getActualInspectionQuantity());
        payload.put("scrapQuantity", command.getScrapQuantity());
        payload.put("inspectionResult", inspectionResult);
        if (nonconformanceDescription != null) {
            payload.put("nonconformanceDescription", nonconformanceDescription);
        }
        payload.put("pqcItemDetails", buildPqcItemDetailsSnapshot(pieceDetails));
        payload.put("pieceDetailCount", pieceDetails.size());
        return JsonUtils.toJsonString(payload);
    }

    private List<Map<String, Object>> buildPqcItemDetailsSnapshot(List<MesPqcInspectionPieceDetailDO> pieceDetails) {
        Map<String, Map<String, Object>> snapshotByItem = new LinkedHashMap<>();
        for (MesPqcInspectionPieceDetailDO detail : pieceDetails) {
            String itemCode = detail.getItemCode();
            Map<String, Object> item = snapshotByItem.computeIfAbsent(itemCode, key -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("itemCode", detail.getItemCode());
                value.put("itemName", detail.getItemName());
                value.put("selectedEquipmentId", detail.getSelectedEquipmentId());
                value.put("selectedEquipmentCode", detail.getSelectedEquipmentCode());
                value.put("selectedEquipmentName", detail.getSelectedEquipmentName());
                value.put("selectedEquipmentNumber", detail.getSelectedEquipmentNumber());
                value.put("standardText", detail.getStandardText());
                value.put("standardLowerLimit", detail.getStandardLowerLimit());
                value.put("standardUpperLimit", detail.getStandardUpperLimit());
                value.put("standardUnit", detail.getStandardUnit());
                value.put("standardPrecision", detail.getStandardPrecision());
                value.put("inspectionMethod", detail.getInspectionMethod());
                value.put("resultType", detail.getResultType());
                value.put("sampleValues", new ArrayList<String>());
                value.put("judgement", detail.getJudgement());
                return value;
            });
            @SuppressWarnings("unchecked")
            List<String> sampleValues = (List<String>) item.get("sampleValues");
            sampleValues.add(detail.getMeasuredValue());
        }
        return new ArrayList<>(snapshotByItem.values());
    }

    private MesProcessPoolCreatePqcInspectionReqDTO buildPqcInspectionLookup(
            MesFrontlinePqcSubmitCommand command) {
        return MesProcessPoolCreatePqcInspectionReqDTO.builder()
                .workOrderId(command.getWorkOrderId())
                .productionSubmitEventId(command.getProductionSubmitEventId())
                .pqcSubmissionIdempotencyKey(command.getPqcSubmissionIdempotencyKey())
                .routeId(command.getRouteId())
                .routeProcessId(command.getRouteProcessId())
                .processId(command.getProcessId())
                .actualEmployeeId(command.getActualEmployeeId())
                .deviceAccountId(command.getDeviceAccountId())
                .deviceId(command.getDeviceId())
                .workstationId(command.getWorkstationId())
                .templateType(command.getTemplateType())
                .feedbackSourceType(PQC_INSPECTION_TASK_SOURCE_TYPE)
                .feedbackSourceId(command.getPqcTaskId())
                .recordbookSourceType(PQC_INSPECTION_TASK_SOURCE_TYPE)
                .recordbookSourceId(command.getPqcTaskId())
                .clientSubmitTime(command.getClientSubmitTime())
                .build();
    }

    private void requirePqcSubmitCommand(MesFrontlinePqcSubmitCommand command) {
        if (command == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "request");
        }
        requirePositive(command.getPqcTaskId(), "pqcTaskId");
        if (command.getActualInspectionQuantity() == null || command.getActualInspectionQuantity() <= 0) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "actualInspectionQuantity");
        }
        requireText(command.getSignaturePassword(), "signaturePassword");
    }

    private void applyPqcTaskContext(MesFrontlinePqcSubmitCommand command, MesPqcInspectionTaskDO task,
                                     Long loginUserId) {
        if (task == null || task.getId() == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "pqcTaskId");
        }
        if (command.getActiveOrderId() == null) {
            command.setActiveOrderId(task.getActiveOrderId());
        }
        if (command.getWorkOrderId() == null) {
            command.setWorkOrderId(task.getWorkOrderId());
        }
        if (command.getRouteId() == null) {
            command.setRouteId(task.getRouteId());
        }
        if (command.getRouteProcessId() == null) {
            command.setRouteProcessId(task.getRouteProcessId());
        }
        if (command.getProcessId() == null) {
            command.setProcessId(task.getProcessId());
        }
        if (command.getRegulationVersionId() == null) {
            command.setRegulationVersionId(task.getRegulationVersionId());
        }
        if (StrUtil.isBlank(command.getInspectionType())) {
            command.setInspectionType(task.getInspectionType());
        }
        if (command.getBusinessDate() == null) {
            command.setBusinessDate(task.getBusinessDate());
        }
        if (StrUtil.isBlank(command.getShiftCode())) {
            command.setShiftCode(task.getShiftCode());
        }
        if (command.getRoundNo() == null) {
            command.setRoundNo(task.getRoundNo());
        }
        if (command.getActualEmployeeId() == null) {
            command.setActualEmployeeId(loginUserId);
        }
        if (StrUtil.isBlank(command.getPqcSubmissionIdempotencyKey())) {
            command.setPqcSubmissionIdempotencyKey("pqc-task-" + task.getId());
        }
        if (StrUtil.isBlank(command.getTemplateType())) {
            command.setTemplateType(FrontlineTemplateCodes.PQC_SIMPLIFIED);
        }
        if (command.getScrapQuantity() == null) {
            command.setScrapQuantity(0);
        }
        if (command.getItemResults() == null) {
            command.setItemResults(List.of());
        }
        if (command.getRawPayload() == null) {
            command.setRawPayload(new LinkedHashMap<>());
        }
    }

    private List<MesFrontlinePqcInspectionItem> resolveSubmittedInspectionItems(
            MesPqcInspectionTaskDO task, MesFrontlinePqcSubmitCommand command) {
        if (task == null || task.getRegulationVersionId() == null || CollUtil.isEmpty(command.getItemResults())) {
            return List.of();
        }
        Set<String> submittedItemCodes = command.getItemResults().stream()
                .filter(Objects::nonNull)
                .map(MesFrontlinePqcSubmitCommand.ItemResult::getItemCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (submittedItemCodes.isEmpty()) {
            return List.of();
        }
        return regulationItemMapper.selectListByVersionId(task.getRegulationVersionId()).stream()
                .filter(item -> item != null && submittedItemCodes.contains(item.getItemCode()))
                .filter(item -> StrUtil.isBlank(task.getInspectionType())
                        || Objects.equals(item.getInspectionType(), task.getInspectionType()))
                .map(item -> toInspectionItem(item, List.of()))
                .toList();
    }

    private String resolvePqcInspectionResult(Integer scrapQuantity,
                                              List<MesPqcInspectionPieceDetailDO> pieceDetails) {
        int safeScrapQuantity = scrapQuantity == null ? 0 : scrapQuantity;
        if (safeScrapQuantity > 0 || pieceDetails.stream().anyMatch(detail ->
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(detail.getJudgement()))) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;
        }
        return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS;
    }

    private MesFrontlinePqcSubmitResult loadPqcSubmitResult(Long eventId, Long expectedTaskId) {
        requirePositive(eventId, "pqcEventId");
        MesProProcessPoolEventDO event = processPoolEventMapper.selectById(eventId);
        MesProProcessPoolPqcRecordDO record = pqcRecordMapper.selectByEventId(eventId);
        if (event == null || record == null
                || !MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())
                || !PQC_INSPECTION_TASK_SOURCE_TYPE.equals(event.getFeedbackSourceType())
                || !Objects.equals(expectedTaskId, event.getFeedbackSourceId())
                || !Objects.equals(event.getId(), record.getEventId())
                || !Objects.equals(event.getSignatureId(), record.getSignatureId())
                || !Objects.equals(event.getServerSubmitTime(), record.getServerSubmitTime())
                || !Objects.equals(record.getInspectionResult(),
                        MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                        && !Objects.equals(record.getInspectionResult(),
                        MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE)) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "pqcSubmitReceipt.eventId=" + eventId);
        }
        requirePositive(record.getId(), "pqcRecordId");
        requirePositive(record.getSignatureId(), "pqcSubmitReceipt.signatureId");
        requireValue(record.getServerSubmitTime(), "pqcSubmitReceipt.serverSubmitTime");
        return new MesFrontlinePqcSubmitResult(expectedTaskId, eventId, record.getId(), record.getSignatureId(),
                record.getInspectionResult(), record.getServerSubmitTime());
    }

    private String normalizeNonconformanceDescription(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private MesProcessPoolActiveOrderDO requireActiveOrder(Long workOrderId, Long routeId) {
        requireValue(workOrderId, "workOrderId");
        requireValue(routeId, "routeId");
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectActiveByWorkOrderAndRoute(workOrderId, routeId);
        if (activeOrder == null) {
            throw exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_REQUIRED, workOrderId, routeId);
        }
        requirePositive(activeOrder.getId(), "activeOrderId");
        requirePositive(activeOrder.getRouteVersionId(), "activeOrder.routeVersionId");
        return activeOrder;
    }

    private List<MesFrontlinePqcTaskContext> resolvePendingPqcTaskContexts(MesProcessPoolActiveOrderDO activeOrder,
                                                                           MesProWorkOrderDO workOrder,
                                                                           MesProRouteProcessDO routeProcess,
                                                                           List<MesPqcInspectionTaskDO> tasksForProcess) {
        List<MesPqcInspectionTaskDO> pendingTasks = selectPendingTasks(tasksForProcess);
        if (pendingTasks.isEmpty()) {
            return List.of();
        }
        MesQaInspectionRegulationDO regulation = regulationMapper.selectPublishedByRouteProcess(
                workOrder.getProductId(), activeOrder.getRouteId(), activeOrder.getRouteVersionId(),
                routeProcess.getId(), routeProcess.getProcessId());
        if (regulation == null || regulation.getCurrentVersionId() == null) {
            throw exception(PRO_FRONTLINE_PQC_REGULATION_REQUIRED,
                    activeOrder.getId(), routeProcess.getId(), routeProcess.getProcessId());
        }
        List<MesFrontlinePqcTaskContext> contexts = new ArrayList<>();
        for (MesPqcInspectionTaskDO task : pendingTasks) {
            if (!Objects.equals(task.getRegulationVersionId(), regulation.getCurrentVersionId())) {
                throw exception(PRO_FRONTLINE_PQC_TASK_REQUIRED,
                        activeOrder.getId(), routeProcess.getId(), routeProcess.getProcessId());
            }
            MesQaInspectionRegulationVersionDO version = versionMapper.selectById(task.getRegulationVersionId());
            if (version == null || !Objects.equals("PUBLISHED", version.getLifecycleStatus())) {
                throw exception(PRO_FRONTLINE_PQC_REGULATION_REQUIRED,
                        activeOrder.getId(), routeProcess.getId(), routeProcess.getProcessId());
            }
            Map<InspectionItemKey, List<MesFrontlinePqcInspectionItem.EquipmentOption>> equipmentOptionsByItem =
                    regulationItemEquipmentMapper.selectListByVersionId(task.getRegulationVersionId()).stream()
                            .collect(Collectors.groupingBy(
                                    row -> new InspectionItemKey(row.getInspectionType(), row.getItemCode()),
                                    LinkedHashMap::new,
                                    Collectors.mapping(MesFrontlinePqcContextServiceImpl::toEquipmentOption,
                                            Collectors.toList())));
            List<MesFrontlinePqcInspectionItem> items = regulationItemMapper
                    .selectListByVersionId(task.getRegulationVersionId())
                    .stream()
                    .filter(item -> item != null && Objects.equals(item.getInspectionType(), task.getInspectionType()))
                    .map(item -> toInspectionItem(item,
                            equipmentOptionsByItem.getOrDefault(
                                    new InspectionItemKey(item.getInspectionType(), item.getItemCode()), List.of())))
                    .toList();
            if (items.isEmpty()) {
                throw exception(PRO_FRONTLINE_PQC_REGULATION_REQUIRED,
                        activeOrder.getId(), routeProcess.getId(), routeProcess.getProcessId());
            }
            contexts.add(new MesFrontlinePqcTaskContext(task, version.getFinalInspectionApplicable(), items));
        }
        return contexts;
    }

    private Map<PqcTaskProcessKey, List<MesFrontlineProductionSubmitCandidate>> groupProductionSubmitCandidates(
            Long workOrderId, Long routeId, List<MesProProcessPoolEventDO> events) {
        if (CollUtil.isEmpty(events)) {
            return Map.of();
        }
        Map<PqcTaskProcessKey, List<MesFrontlineProductionSubmitCandidate>> result = new LinkedHashMap<>();
        for (MesProProcessPoolEventDO event : events) {
            if (event == null
                    || !MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())
                    || !Objects.equals(workOrderId, event.getWorkOrderId())
                    || !Objects.equals(routeId, event.getRouteId())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "productionSubmitCandidate");
            }
            requirePositive(event.getId(), "productionSubmitCandidate.eventId");
            requirePositive(event.getRouteProcessId(), "productionSubmitCandidate.routeProcessId");
            requirePositive(event.getProcessId(), "productionSubmitCandidate.processId");
            requirePositive(event.getDeviceAccountId(), "productionSubmitCandidate.deviceAccountId");
            requirePositive(event.getDeviceId(), "productionSubmitCandidate.deviceId");
            requirePositive(event.getWorkstationId(), "productionSubmitCandidate.workstationId");
            requireValue(event.getServerSubmitTime(), "productionSubmitCandidate.serverSubmitTime");
            PqcTaskProcessKey key = new PqcTaskProcessKey(event.getRouteProcessId(), event.getProcessId());
            result.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new MesFrontlineProductionSubmitCandidate(event.getId(), event.getServerSubmitTime()));
        }
        return result;
    }

    private static Map<PqcTaskProcessKey, List<MesPqcInspectionTaskDO>> groupTasksByProcess(
            List<MesPqcInspectionTaskDO> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return Map.of();
        }
        List<MesPqcInspectionTaskDO> validTasks = new ArrayList<>();
        for (MesPqcInspectionTaskDO task : tasks) {
            if (task == null) {
                continue;
            }
            if (!PQC_TASK_STATUS_CANCELLED.equals(task.getTaskStatus())
                    && (task.getRouteProcessId() == null || task.getProcessId() == null)) {
                throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, pqcTaskIdentityText(task));
            }
            if (task.getRouteProcessId() != null && task.getProcessId() != null) {
                validTasks.add(task);
            }
        }
        return validTasks.stream()
                .collect(Collectors.groupingBy(task -> new PqcTaskProcessKey(task.getRouteProcessId(),
                                task.getProcessId()),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private static void requirePendingTasksBackedByQaProcesses(
            MesProcessPoolActiveOrderDO activeOrder,
            Set<PqcTaskProcessKey> qaProcessKeys,
            Map<PqcTaskProcessKey, List<MesPqcInspectionTaskDO>> tasksByProcess) {
        for (Map.Entry<PqcTaskProcessKey, List<MesPqcInspectionTaskDO>> entry : tasksByProcess.entrySet()) {
            if (selectPendingTasks(entry.getValue()).isEmpty() || qaProcessKeys.contains(entry.getKey())) {
                continue;
            }
            PqcTaskProcessKey key = entry.getKey();
            throw exception(PRO_FRONTLINE_PQC_TASK_REQUIRED,
                    activeOrder.getId(), key.routeProcessId(), key.processId());
        }
    }

    private static String pqcTaskIdentityText(MesPqcInspectionTaskDO task) {
        return "taskId=" + task.getId()
                + "，activeOrderId=" + task.getActiveOrderId()
                + "，routeProcessId=" + task.getRouteProcessId()
                + "，processId=" + task.getProcessId();
    }

    private static boolean hasSubmittedTask(List<MesPqcInspectionTaskDO> tasks) {
        return CollUtil.isNotEmpty(tasks) && tasks.stream()
                .anyMatch(task -> task != null && PQC_TASK_STATUS_SUBMITTED.equals(task.getTaskStatus()));
    }

    private static List<MesPqcInspectionTaskDO> selectPendingTasks(List<MesPqcInspectionTaskDO> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return List.of();
        }
        return tasks.stream()
                .filter(task -> task != null && PQC_TASK_STATUS_PENDING.equals(task.getTaskStatus()))
                .sorted(Comparator
                        .comparing(MesPqcInspectionTaskDO::getBusinessDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(task -> inspectionTypeOrder(task.getInspectionType()))
                        .thenComparing(MesPqcInspectionTaskDO::getRoundNo,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MesPqcInspectionTaskDO::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static int inspectionTypeOrder(String inspectionType) {
        if ("FIRST".equals(inspectionType)) {
            return 1;
        }
        if ("PATROL".equals(inspectionType)) {
            return 2;
        }
        if ("FINAL".equals(inspectionType)) {
            return 3;
        }
        return 99;
    }

    private static MesFrontlinePqcInspectionItem toInspectionItem(MesQaInspectionRegulationItemDO item,
                                                                  List<MesFrontlinePqcInspectionItem.EquipmentOption>
                                                                          equipmentOptions) {
        if (item == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "inspectionItem");
        }
        boolean equipmentRequired = Boolean.TRUE.equals(item.getEquipmentRequired());
        return new MesFrontlinePqcInspectionItem(item.getItemCode(), item.getItemName(),
                item.getInspectionMethod(), item.getStandardText(), item.getStandardLowerLimit(),
                item.getStandardUpperLimit(), item.getStandardUnit(), item.getStandardPrecision(),
                equipmentRequired, item.getResultType(), List.copyOf(equipmentOptions));
    }

    private static MesFrontlinePqcInspectionItem.EquipmentOption toEquipmentOption(
            MesQaInspectionRegulationItemEquipmentDO row) {
        if (row == null || row.getEquipmentId() == null || StrUtil.isBlank(row.getEquipmentCode())
                || StrUtil.isBlank(row.getEquipmentName()) || StrUtil.isBlank(row.getEquipmentNumber())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "inspectionItem.equipmentOption");
        }
        return new MesFrontlinePqcInspectionItem.EquipmentOption(row.getEquipmentId(), row.getEquipmentCode(),
                row.getEquipmentName(), row.getEquipmentNumber(), row.getDefaultFlag(), row.getSort());
    }

    private static MesFrontlinePqcTaskOption toPqcTaskOption(MesFrontlinePqcTaskContext context) {
        MesPqcInspectionTaskDO task = context.task();
        return new MesFrontlinePqcTaskOption(task.getId(), task.getRegulationVersionId(),
                context.finalInspectionApplicable(), task.getInspectionType(), task.getBusinessDate(),
                task.getShiftCode(), task.getRoundNo(), task.getPlannedInspectionQuantity(),
                context.inspectionItems());
    }

    private List<MesPqcInspectionPieceDetailDO> buildPieceDetails(Long taskId, MesFrontlinePqcSubmitCommand command,
                                                                  List<MesFrontlinePqcInspectionItem> inspectionItems) {
        if (CollUtil.isEmpty(command.getItemResults())) {
            return List.of();
        }
        Map<String, MesFrontlinePqcInspectionItem> itemByCode = inspectionItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> StrUtil.isNotBlank(item.itemCode()))
                .collect(Collectors.toMap(MesFrontlinePqcInspectionItem::itemCode,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<MesPqcInspectionPieceDetailDO> details = new ArrayList<>();
        for (MesFrontlinePqcSubmitCommand.ItemResult itemResult : command.getItemResults()) {
            if (itemResult == null || StrUtil.isBlank(itemResult.getItemCode())) {
                continue;
            }
            MesFrontlinePqcInspectionItem item = itemByCode.get(itemResult.getItemCode());
            MesFrontlinePqcInspectionItem.EquipmentOption selectedEquipment =
                    resolveSelectedEquipment(item, itemResult);
            List<String> values = itemResult.getSampleValues();
            if (CollUtil.isEmpty(values)) {
                continue;
            }
            int sampleLimit = Math.min(values.size(), command.getActualInspectionQuantity());
            for (int sampleIndex = 0; sampleIndex < sampleLimit; sampleIndex += 1) {
                String value = Objects.toString(values.get(sampleIndex), "").trim();
                if (StrUtil.isBlank(value)) {
                    continue;
                }
                details.add(MesPqcInspectionPieceDetailDO.builder()
                        .taskId(taskId)
                        .sampleNo(sampleIndex + 1)
                        .itemCode(itemResult.getItemCode())
                        .itemName(item == null ? null : item.itemName())
                        .inspectionMethod(item == null ? null : item.inspectionMethod())
                        .standardText(item == null ? null : item.standardText())
                        .selectedEquipmentId(selectedEquipment == null ? null : selectedEquipment.equipmentId())
                        .selectedEquipmentCode(selectedEquipment == null ? null : selectedEquipment.equipmentCode())
                        .selectedEquipmentName(selectedEquipment == null ? null : selectedEquipment.equipmentName())
                        .selectedEquipmentNumber(selectedEquipment == null ? null : selectedEquipment.equipmentNumber())
                        .standardLowerLimit(item == null ? null : item.standardLowerLimit())
                        .standardUpperLimit(item == null ? null : item.standardUpperLimit())
                        .standardUnit(item == null ? null : item.standardUnit())
                        .standardPrecision(item == null ? null : item.standardPrecision())
                        .resultType(item == null ? null : item.resultType())
                        .itemResult(value)
                        .measuredValue(value)
                        .judgement(resolvePieceJudgement(item, value))
                        .build());
            }
        }
        return details;
    }

    private MesFrontlinePqcInspectionItem.EquipmentOption resolveSelectedEquipment(
            MesFrontlinePqcInspectionItem item, MesFrontlinePqcSubmitCommand.ItemResult itemResult) {
        boolean hasSelectedEquipment = itemResult.getSelectedEquipmentId() != null
                || StrUtil.isNotBlank(itemResult.getSelectedEquipmentNumber());
        if (!hasSelectedEquipment) {
            return null;
        }
        if (item != null && CollUtil.isNotEmpty(item.equipmentOptions())) {
            Optional<MesFrontlinePqcInspectionItem.EquipmentOption> configuredEquipment =
                    item.equipmentOptions().stream()
                            .filter(option -> Objects.equals(option.equipmentId(), itemResult.getSelectedEquipmentId())
                                    && Objects.equals(option.equipmentNumber(), itemResult.getSelectedEquipmentNumber()))
                            .findFirst();
            if (configuredEquipment.isPresent()) {
                return configuredEquipment.get();
            }
        }
        return new MesFrontlinePqcInspectionItem.EquipmentOption(itemResult.getSelectedEquipmentId(), null, null,
                itemResult.getSelectedEquipmentNumber(), null, null);
    }

    private String resolvePieceJudgement(MesFrontlinePqcInspectionItem item, String value) {
        if (item != null && "NUMBER".equals(item.resultType())) {
            try {
                BigDecimal measuredValue = new BigDecimal(value);
                boolean belowLowerLimit = item.standardLowerLimit() != null
                        && measuredValue.compareTo(item.standardLowerLimit()) < 0;
                boolean aboveUpperLimit = item.standardUpperLimit() != null
                        && measuredValue.compareTo(item.standardUpperLimit()) > 0;
                return belowLowerLimit || aboveUpperLimit
                        ? MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE
                        : MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS;
            } catch (NumberFormatException ex) {
                return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;
            }
        }
        if ((item == null || "BOOLEAN".equals(item.resultType()) || "CHOICE".equals(item.resultType()))
                && "不合格".equals(value)) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;
        }
        if ((item == null || "BOOLEAN".equals(item.resultType()) || "CHOICE".equals(item.resultType()))
                && "合格".equals(value)) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS;
        }
        return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS;
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

    private static void validateActiveOrderSummary(MesProWorkOrderDO workOrder, MesMdItemDO item) {
        if (StrUtil.isBlank(item.getName())) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "productName productId=" + workOrder.getProductId());
        }
        if (workOrder.getQuantity() == null || workOrder.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "quantity workOrderId=" + workOrder.getId());
        }
    }

    private static void requireActiveOrderIdentity(MesProcessPoolActiveOrderDO activeOrder) {
        if (activeOrder == null || activeOrder.getId() == null
                || activeOrder.getWorkOrderId() == null || activeOrder.getRouteId() == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "active order");
        }
    }

    private static void requireQaInspectionRegulationProcessIdentity(MesProcessPoolActiveOrderDO activeOrder,
                                                                     MesProWorkOrderDO workOrder,
                                                                     MesQaInspectionRegulationDO regulation) {
        if (regulation.getProductId() == null
                || regulation.getRouteId() == null
                || regulation.getRouteVersionId() == null
                || regulation.getRouteProcessId() == null
                || regulation.getProcessId() == null
                || regulation.getCurrentVersionId() == null
                || !"PUBLISHED".equals(regulation.getLifecycleStatus())
                || !Objects.equals(regulation.getProductId(), workOrder.getProductId())
                || !Objects.equals(regulation.getRouteId(), activeOrder.getRouteId())
                || !Objects.equals(regulation.getRouteVersionId(), activeOrder.getRouteVersionId())) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "qaInspectionRegulation activeOrderId=" + activeOrder.getId()
                            + "，regulationId=" + regulation.getId());
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

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private static void requireText(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
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

    private record LatestActiveOrderContext(MesProcessPoolActiveOrderDO activeOrder, LocalDateTime latestSubmitTime) {
    }

    private record PqcTaskProcessKey(Long routeProcessId, Long processId) {
    }

    private record InspectionItemKey(String inspectionType, String itemCode) {
    }

    private record MesFrontlinePqcTaskContext(MesPqcInspectionTaskDO task,
                                              Boolean finalInspectionApplicable,
                                              List<MesFrontlinePqcInspectionItem> inspectionItems) {
    }
}
