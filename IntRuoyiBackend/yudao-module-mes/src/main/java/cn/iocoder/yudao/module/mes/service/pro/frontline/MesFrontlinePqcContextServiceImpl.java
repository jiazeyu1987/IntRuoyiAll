package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateCodes;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateTypes;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
    private static final String PQC_TASK_STATUS_CONFIRMED = "CONFIRMED";
    private static final String PQC_TASK_STATUS_CANCELLED = "CANCELLED";
    private static final Map<String, RuleIdentity> PQC_RULE_IDENTITIES = Map.of(
            "FIRST", new RuleIdentity("FIRST", "FIRST", 1, 10),
            "PATROL_AM", new RuleIdentity("PATROL", "AM", 1, 20),
            "PATROL_PM", new RuleIdentity("PATROL", "PM", 1, 30),
            "FINAL", new RuleIdentity("FINAL", "FINAL", 1, 40));
    private static final Set<String> PQC_TASK_STATUSES = Set.of(
            PQC_TASK_STATUS_PENDING, PQC_TASK_STATUS_SUBMITTED,
            PQC_TASK_STATUS_CONFIRMED, PQC_TASK_STATUS_CANCELLED);

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProProcessPoolEventMapper processPoolEventMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProRouteMapper routeMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final DccProjectCodeMapper dccProjectCodeMapper;
    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationVersionMapper versionMapper;
    private final MesQaInspectionRegulationProcessMapper regulationProcessMapper;
    private final MesQaInspectionRegulationItemMapper regulationItemMapper;
    private final MesQaInspectionRegulationItemEquipmentMapper regulationItemEquipmentMapper;
    private final MesQaInspectionRegulationService regulationService;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    private final MesMdItemService itemService;
    private final MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    private final AdminUserApi adminUserApi;
    private final MesProcessPoolEventService processPoolEventService;
    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private final MesProBatchRecordExecutionSignatureService signatureService;

    public MesFrontlinePqcContextServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                             MesProProcessPoolEventMapper processPoolEventMapper,
                                             MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
                                             MesProWorkOrderMapper workOrderMapper,
                                             MesProRouteMapper routeMapper,
                                             MesProRouteVersionMapper routeVersionMapper,
                                             DccProjectCodeMapper dccProjectCodeMapper,
                                             MesQaInspectionRegulationMapper regulationMapper,
                                             MesQaInspectionRegulationVersionMapper versionMapper,
                                             MesQaInspectionRegulationProcessMapper regulationProcessMapper,
                                             MesQaInspectionRegulationItemMapper regulationItemMapper,
                                             MesQaInspectionRegulationItemEquipmentMapper regulationItemEquipmentMapper,
                                             MesQaInspectionRegulationService regulationService,
                                             MesPqcInspectionTaskMapper pqcTaskMapper,
                                             MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper,
                                             MesMdItemService itemService,
                                             MesProcessPoolTeamLeaderScopeMapper scopeMapper,
                                             AdminUserApi adminUserApi,
                                             MesProcessPoolEventService processPoolEventService,
                                             MesProProcessPoolPqcRecordMapper pqcRecordMapper,
                                             MesProBatchRecordExecutionSignatureService signatureService) {
        this.activeOrderMapper = activeOrderMapper;
        this.processPoolEventMapper = processPoolEventMapper;
        this.processSnapshotMapper = processSnapshotMapper;
        this.workOrderMapper = workOrderMapper;
        this.routeMapper = routeMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.dccProjectCodeMapper = dccProjectCodeMapper;
        this.regulationMapper = regulationMapper;
        this.versionMapper = versionMapper;
        this.regulationProcessMapper = regulationProcessMapper;
        this.regulationItemMapper = regulationItemMapper;
        this.regulationItemEquipmentMapper = regulationItemEquipmentMapper;
        this.regulationService = regulationService;
        this.pqcTaskMapper = pqcTaskMapper;
        this.pqcPieceDetailMapper = pqcPieceDetailMapper;
        this.itemService = itemService;
        this.scopeMapper = scopeMapper;
        this.adminUserApi = adminUserApi;
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

        List<LatestActiveOrderContext> activeOrderContexts = new ArrayList<>();
        for (MesProcessPoolActiveOrderDO activeOrder : activeOrders) {
            requireActiveOrderIdentity(activeOrder);
            activeOrderContexts.add(new LatestActiveOrderContext(activeOrder, activeOrder.getJoinedAt()));
        }
        Set<Long> workOrderIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getWorkOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> routeIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getRouteId)
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
        Map<Long, Set<Long>> productIdsByRouteVersionId = new LinkedHashMap<>();

        List<MesFrontlineActiveOrderCandidate> candidates = new ArrayList<>();
        for (LatestActiveOrderContext context : activeOrderContexts) {
            MesProcessPoolActiveOrderDO activeOrder = context.activeOrder();
            MesProWorkOrderDO workOrder = requireWorkOrder(workOrderMap, activeOrder.getWorkOrderId());
            MesProRouteDO route = requireRoute(routeMap, activeOrder.getRouteId());
            Set<Long> routeVersionProductIds = productIdsByRouteVersionId.computeIfAbsent(
                    activeOrder.getRouteVersionId(), ignored -> resolveRouteVersionProductIds(activeOrder));
            requireProductRoute(workOrder, activeOrder, routeVersionProductIds);
            MesMdItemDO item = requireProduct(itemMap, workOrder.getProductId());
            validateActiveOrderSummary(workOrder, item);
            candidates.add(new MesFrontlineActiveOrderCandidate(activeOrder.getId(),
                    workOrder.getId(), workOrder.getCode(),
                    workOrder.getName(), workOrder.getProductId(), item.getCode(), item.getName(),
                    workOrder.getQuantity(), route.getId(), route.getCode(), route.getName(),
                    context.latestSubmitTime()));
        }
        candidates.sort(Comparator
                .comparing(MesFrontlineActiveOrderCandidate::latestSubmitTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MesFrontlineActiveOrderCandidate::activeOrderId));
        return candidates;
    }

    @Override
    public List<MesFrontlinePqcProcessCandidate> listProcessesByActiveOrder(Long workOrderId, Long routeId) {
        MesProcessPoolActiveOrderDO activeOrder = requireActiveOrder(workOrderId, routeId);
        MesProWorkOrderDO workOrder = requireWorkOrder(workOrderId);
        MesProRouteDO route = requireRoute(routeId);
        Set<Long> routeVersionProductIds = resolveRouteVersionProductIds(activeOrder);
        requireProductRoute(workOrder, activeOrder, routeVersionProductIds);
        QaProjectProcessSource qaSource = resolveQaProjectProcessSource(activeOrder, workOrder,
                routeVersionProductIds);
        Map<Long, List<MesPqcInspectionTaskDO>> tasksByQaProcess = groupTasksByQaProcess(
                pqcTaskMapper.selectListByActiveOrderId(activeOrder.getId()));
        Set<Long> qaProcessIds = qaSource.processes().stream()
                .map(MesQaInspectionRegulationProcessDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        requirePendingTasksBackedByQaProcesses(activeOrder, qaSource.version().getId(), qaProcessIds,
                tasksByQaProcess);

        List<MesFrontlinePqcProcessCandidate> candidates = new ArrayList<>();
        for (MesQaInspectionRegulationProcessDO qaProcess : qaSource.processes()) {
            List<MesFrontlinePqcTaskContext> taskContexts = resolvePendingPqcTaskContexts(activeOrder,
                    qaSource.regulation(), qaSource.version(), qaProcess,
                    tasksByQaProcess.get(qaProcess.getId()));
            MesFrontlinePqcTaskContext taskContext = taskContexts.isEmpty() ? null : taskContexts.get(0);
            List<MesFrontlinePqcTaskOption> taskOptions = taskContexts.stream()
                    .map(MesFrontlinePqcContextServiceImpl::toPqcTaskOption)
                    .toList();
            candidates.add(new MesFrontlinePqcProcessCandidate(route.getId(), route.getCode(), route.getName(),
                    qaSource.project().getId(), qaSource.regulation().getId(), qaSource.version().getId(),
                    qaProcess.getId(), qaProcess.getProcessCode(), qaProcess.getProcessName(), qaProcess.getSort(),
                    activeOrder.getId(), taskContext == null ? null : taskContext.task().getId(),
                    taskContext == null ? null : taskContext.finalInspectionApplicable(),
                    taskContext == null ? null : taskContext.task().getInspectionType(),
                    taskContext == null ? null : taskContext.task().getBusinessDate(),
                    taskContext == null ? null : taskContext.task().getShiftCode(),
                    taskContext == null ? null : taskContext.task().getRoundNo(),
                    taskContext == null ? null : taskContext.task().getPlannedInspectionQuantity(),
                    taskContext == null ? List.of() : taskContext.inspectionItems(),
                    taskOptions));
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparing((MesFrontlinePqcProcessCandidate candidate) ->
                                candidate.qaProcessSort() == null ? Integer.MAX_VALUE : candidate.qaProcessSort())
                        .thenComparing(MesFrontlinePqcProcessCandidate::qaProcessId))
                .toList();
    }

    @Override
    public List<MesFrontlinePqcProcessRespVO> listProcessesByActiveOrder(Long activeOrderId) {
        MesProcessPoolActiveOrderDO activeOrder = requireActiveOrder(activeOrderId);
        requireWorkOrder(activeOrder.getWorkOrderId());
        MesProRouteDO route = requireRoute(activeOrder.getRouteId());
        MesQaInspectionRegulationPublishedVersionRespVO qaSource = regulationService.getLockedVersionForOrder(
                activeOrder.getDccProjectCodeId(), activeOrder.getQaRegulationId(),
                activeOrder.getQaRegulationVersionId());
        requireLockedQaAggregate(activeOrder, qaSource);
        Map<Long, List<MesPqcInspectionTaskDO>> tasksByQaProcess = groupLockedTasksByQaProcess(
                pqcTaskMapper.selectListByActiveOrderId(activeOrder.getId()));
        Set<Long> qaProcessIds = qaSource.getProcesses().stream()
                .map(MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess::getQaProcessId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        requireTasksBackedByLockedQa(activeOrder, qaSource.getPublishedVersionId(), qaProcessIds,
                tasksByQaProcess);
        List<MesFrontlinePqcProcessRespVO.QaInspectionTypeRule> inspectionTypeRules =
                parseLockedInspectionTypeRules(qaSource);
        Map<String, MesFrontlinePqcProcessRespVO.QaInspectionTypeRule> inspectionTypeRuleByKey =
                inspectionTypeRules.stream().collect(Collectors.toMap(
                        MesFrontlinePqcProcessRespVO.QaInspectionTypeRule::getKey,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<MesFrontlineProductionSubmitCandidate> productionSubmitCandidates =
                resolveProductionSubmitCandidates(activeOrder);

        List<MesFrontlinePqcProcessRespVO> processes = new ArrayList<>();
        for (MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess qaProcess : qaSource.getProcesses()) {
            List<MesFrontlinePqcProcessRespVO.PqcInspectionItem> inspectionItems =
                    buildPublishedInspectionItemResponses(qaProcess.getItems());
            if (inspectionItems.isEmpty()) {
                throw exception(PRO_FRONTLINE_PQC_REGULATION_REQUIRED,
                        activeOrder.getId(), qaSource.getPublishedVersionId(), qaProcess.getQaProcessId());
            }
            processes.add(toPqcProcessRespVO(route, activeOrder, qaSource, qaProcess, inspectionItems,
                    tasksByQaProcess.getOrDefault(qaProcess.getQaProcessId(), List.of()),
                    inspectionTypeRules, inspectionTypeRuleByKey, productionSubmitCandidates));
        }
        processes.sort(Comparator
                .comparing((MesFrontlinePqcProcessRespVO respVO) ->
                        respVO.getQaProcessSort() == null ? Integer.MAX_VALUE : respVO.getQaProcessSort())
                .thenComparing(MesFrontlinePqcProcessRespVO::getQaProcessId));
        return processes;
    }

    private MesFrontlinePqcProcessRespVO toPqcProcessRespVO(
            MesProRouteDO route,
            MesProcessPoolActiveOrderDO activeOrder,
            MesQaInspectionRegulationPublishedVersionRespVO qaSource,
            MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess qaProcess,
            List<MesFrontlinePqcProcessRespVO.PqcInspectionItem> inspectionItems,
            List<MesPqcInspectionTaskDO> tasksForProcess,
            List<MesFrontlinePqcProcessRespVO.QaInspectionTypeRule> inspectionTypeRules,
            Map<String, MesFrontlinePqcProcessRespVO.QaInspectionTypeRule> inspectionTypeRuleByKey,
            List<MesFrontlineProductionSubmitCandidate> productionSubmitCandidates) {
        List<MesFrontlinePqcProcessRespVO.PqcTaskOption> taskOptions = buildTaskOptions(
                activeOrder, qaSource.getPublishedVersionId(), qaSource.getFinalInspectionApplicable(),
                qaProcess.getQaProcessId(), inspectionItems, tasksForProcess,
                inspectionTypeRuleByKey);
        MesFrontlinePqcProcessRespVO.PqcTaskOption currentTask = taskOptions.stream()
                .filter(option -> MesFrontlinePqcTaskOverlay.STATUS_PENDING.equals(option.getTaskStatus()))
                .findFirst().orElse(null);
        MesFrontlinePqcProcessRespVO.PqcTaskSummary taskSummary = buildTaskSummary(tasksForProcess);

        MesFrontlinePqcProcessRespVO respVO = new MesFrontlinePqcProcessRespVO();
        respVO.setRouteId(route.getId());
        respVO.setRouteCode(route.getCode());
        respVO.setRouteName(route.getName());
        respVO.setDccProjectCodeId(activeOrder.getDccProjectCodeId());
        respVO.setRegulationId(activeOrder.getQaRegulationId());
        respVO.setRegulationVersionId(qaSource.getPublishedVersionId());
        respVO.setQaProcessId(qaProcess.getQaProcessId());
        respVO.setQaProcessCode(qaProcess.getProcessCode());
        respVO.setQaProcessName(qaProcess.getProcessName());
        respVO.setQaProcessSort(qaProcess.getSort());
        respVO.setActiveOrderId(activeOrder.getId());
        respVO.setPqcTaskId(currentTask == null ? null : currentTask.getPqcTaskId());
        respVO.setInspectionRuleKey(currentTask == null ? null : currentTask.getInspectionRuleKey());
        respVO.setTaskStatus(currentTask == null ? null : currentTask.getTaskStatus());
        respVO.setFinalInspectionApplicable(qaSource.getFinalInspectionApplicable());
        respVO.setInspectionTypeRules(inspectionTypeRules);
        respVO.setInspectionType(currentTask == null ? null : currentTask.getInspectionType());
        respVO.setBusinessDate(currentTask == null ? null : currentTask.getBusinessDate());
        respVO.setShiftCode(currentTask == null ? null : currentTask.getShiftCode());
        respVO.setRoundNo(currentTask == null ? null : currentTask.getRoundNo());
        respVO.setPlannedInspectionQuantity(currentTask == null ? null : currentTask.getPlannedInspectionQuantity());
        respVO.setInspectionItems(inspectionItems);
        respVO.setTaskSummary(taskSummary);
        respVO.setPqcTaskOptions(taskOptions);
        respVO.setProductionSubmitCandidates(productionSubmitCandidates.stream()
                .map(MesFrontlinePqcContextServiceImpl::toProductionSubmitCandidateRespVO)
                .toList());
        return respVO;
    }

    private List<MesFrontlinePqcProcessRespVO.PqcTaskOption> buildTaskOptions(
            MesProcessPoolActiveOrderDO activeOrder,
            Long regulationVersionId,
            Boolean finalInspectionApplicable,
            Long qaProcessId,
            List<MesFrontlinePqcProcessRespVO.PqcInspectionItem> inspectionItems,
            List<MesPqcInspectionTaskDO> tasks,
            Map<String, MesFrontlinePqcProcessRespVO.QaInspectionTypeRule> inspectionTypeRuleByKey) {
        List<MesPqcInspectionTaskDO> pendingTasks = tasks.stream()
                .filter(task -> PQC_TASK_STATUS_PENDING.equals(task.getTaskStatus()))
                .toList();
        List<MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity> expectedTasks = pendingTasks.stream()
                .map(task -> new MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity(
                        activeOrder.getId(), regulationVersionId, qaProcessId,
                        task.getInspectionRuleKey(), task.getInspectionType(), task.getBusinessDate(),
                        task.getShiftCode(), task.getRoundNo(), finalInspectionApplicable,
                        task.getPlannedInspectionQuantity(),
                        toOverlayInspectionItems(inspectionItems, task.getInspectionType())))
                .toList();
        Set<Long> overlaidPendingTaskIds = MesFrontlinePqcTaskOverlay
                .fromExpectedTasks(expectedTasks, pendingTasks).stream()
                .map(MesFrontlinePqcTaskOverlay::pqcTaskOption)
                .map(MesFrontlinePqcTaskOption::pqcTaskId)
                .collect(Collectors.toSet());
        if (overlaidPendingTaskIds.size() != pendingTasks.size()) {
            throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH,
                    "activeOrderId=" + activeOrder.getId() + "，regulationVersionId="
                            + regulationVersionId + "，qaProcessId=" + qaProcessId);
        }
        return tasks.stream()
                .map(task -> toPqcTaskOptionRespVO(task, finalInspectionApplicable,
                        inspectionItems, inspectionTypeRuleByKey.get(task.getInspectionRuleKey())))
                .sorted(Comparator
                        .comparing(MesFrontlinePqcProcessRespVO.PqcTaskOption::getBusinessDate)
                        .thenComparingInt(MesFrontlinePqcProcessRespVO.PqcTaskOption::getRuleSort)
                        .thenComparing(MesFrontlinePqcProcessRespVO.PqcTaskOption::getRoundNo)
                        .thenComparing(MesFrontlinePqcProcessRespVO.PqcTaskOption::getPqcTaskId))
                .toList();
    }

    private static MesFrontlinePqcProcessRespVO.PqcTaskOption toPqcTaskOptionRespVO(
            MesPqcInspectionTaskDO task,
            Boolean finalInspectionApplicable,
            List<MesFrontlinePqcProcessRespVO.PqcInspectionItem> inspectionItems,
            MesFrontlinePqcProcessRespVO.QaInspectionTypeRule inspectionTypeRule) {
        RuleIdentity ruleIdentity = PQC_RULE_IDENTITIES.get(task.getInspectionRuleKey());
        if (ruleIdentity == null || inspectionTypeRule == null
                || !Objects.equals(task.getInspectionRuleKey(), inspectionTypeRule.getKey())) {
            throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, pqcTaskIdentityText(task));
        }
        MesFrontlinePqcProcessRespVO.PqcTaskOption option = new MesFrontlinePqcProcessRespVO.PqcTaskOption();
        option.setPqcTaskId(task.getId());
        option.setRegulationVersionId(task.getRegulationVersionId());
        option.setQaProcessId(task.getQaProcessId());
        option.setInspectionRuleKey(task.getInspectionRuleKey());
        option.setTaskStatus(task.getTaskStatus());
        option.setRuleSort(ruleIdentity.ruleSort());
        option.setInspectionTypeRule(inspectionTypeRule);
        option.setFinalInspectionApplicable(finalInspectionApplicable);
        option.setInspectionType(task.getInspectionType());
        option.setBusinessDate(task.getBusinessDate());
        option.setShiftCode(task.getShiftCode());
        option.setRoundNo(task.getRoundNo());
        option.setPlannedInspectionQuantity(task.getPlannedInspectionQuantity());
        option.setInspectionItems(inspectionItems.stream()
                .filter(item -> item.getApplicableInspectionTypes().contains(task.getInspectionType()))
                .toList());
        if (option.getInspectionItems().isEmpty()) {
            throw exception(PRO_FRONTLINE_PQC_REGULATION_REQUIRED,
                    task.getActiveOrderId(), task.getRegulationVersionId(), task.getQaProcessId());
        }
        return option;
    }

    private static List<MesFrontlinePqcProcessRespVO.QaInspectionTypeRule> parseLockedInspectionTypeRules(
            MesQaInspectionRegulationPublishedVersionRespVO qaSource) {
        List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule> sourceRules =
                qaSource.getInspectionTypeRules();
        if (sourceRules == null || sourceRules.size() != PQC_RULE_IDENTITIES.size()) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "lockedQaInspectionTypeRules regulationVersionId=" + qaSource.getPublishedVersionId());
        }
        Map<String, MesFrontlinePqcProcessRespVO.QaInspectionTypeRule> rulesByKey = new LinkedHashMap<>();
        for (MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule source : sourceRules) {
            RuleIdentity identity = source == null ? null : PQC_RULE_IDENTITIES.get(source.getKey());
            if (identity == null || !Objects.equals(identity.inspectionType(), source.getInspectionType())
                    || rulesByKey.containsKey(source.getKey())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "lockedQaInspectionTypeRules regulationVersionId=" + qaSource.getPublishedVersionId());
            }
            MesFrontlinePqcProcessRespVO.QaInspectionTypeRule rule =
                    new MesFrontlinePqcProcessRespVO.QaInspectionTypeRule();
            rule.setKey(source.getKey());
            rule.setInspectionType(source.getInspectionType());
            rule.setLabel(source.getLabel());
            rule.setRoundLabel(source.getRoundLabel());
            rule.setRequired(source.getRequired());
            rule.setFixedQuantity(source.getFixedQuantity());
            rule.setNotApplicableReason(source.getNotApplicableReason());
            rule.setTaskRule(source.getTaskRule());
            rule.setReleaseGate(source.getReleaseGate());
            rulesByKey.put(rule.getKey(), rule);
        }
        if (!rulesByKey.keySet().equals(PQC_RULE_IDENTITIES.keySet())) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "lockedQaInspectionTypeRules regulationVersionId=" + qaSource.getPublishedVersionId());
        }
        return rulesByKey.values().stream()
                .sorted(Comparator.comparingInt(rule -> PQC_RULE_IDENTITIES.get(rule.getKey()).ruleSort()))
                .toList();
    }

    private static MesFrontlinePqcProcessRespVO.PqcTaskSummary buildTaskSummary(
            List<MesPqcInspectionTaskDO> tasks) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String status : PQC_TASK_STATUSES) {
            counts.put(status, 0);
        }
        for (MesPqcInspectionTaskDO task : tasks) {
            counts.compute(task.getTaskStatus(), (status, count) -> count + 1);
        }
        MesFrontlinePqcProcessRespVO.PqcTaskSummary summary =
                new MesFrontlinePqcProcessRespVO.PqcTaskSummary();
        summary.setTotalCount(tasks.size());
        summary.setPendingCount(counts.get(PQC_TASK_STATUS_PENDING));
        summary.setSubmittedCount(counts.get(PQC_TASK_STATUS_SUBMITTED));
        summary.setConfirmedCount(counts.get(PQC_TASK_STATUS_CONFIRMED));
        summary.setCancelledCount(counts.get(PQC_TASK_STATUS_CANCELLED));
        if (tasks.isEmpty()) {
            summary.setState(MesFrontlinePqcTaskOverlay.STATUS_NOT_CREATED);
        } else if (counts.values().stream().filter(count -> count > 0).count() > 1) {
            summary.setState("MIXED");
        } else {
            summary.setState(tasks.get(0).getTaskStatus());
        }
        return summary;
    }

    private static List<MesFrontlinePqcProcessRespVO.PqcInspectionItem> buildPublishedInspectionItemResponses(
            List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem> items) {
        if (CollUtil.isEmpty(items)) {
            return List.of();
        }
        return items.stream()
                .map(MesFrontlinePqcContextServiceImpl::buildPublishedInspectionItemResponse)
                .sorted(Comparator.comparing(MesFrontlinePqcProcessRespVO.PqcInspectionItem::getItemSort)
                        .thenComparing(MesFrontlinePqcProcessRespVO.PqcInspectionItem::getItemCode))
                .toList();
    }

    private static MesFrontlinePqcProcessRespVO.PqcInspectionItem buildPublishedInspectionItemResponse(
            MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem source) {
        if (source == null || CollUtil.isEmpty(source.getApplicableInspectionTypes())
                || source.getEquipmentOptions() == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "lockedQaInspectionItem");
        }
        List<MesFrontlinePqcProcessRespVO.PqcEquipmentOption> equipmentOptions = source.getEquipmentOptions().stream()
                .sorted(Comparator.comparing(
                                MesQaInspectionRegulationPublishedVersionRespVO.EquipmentOption::getSort,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesQaInspectionRegulationPublishedVersionRespVO.EquipmentOption::getEquipmentId)
                        .thenComparing(MesQaInspectionRegulationPublishedVersionRespVO.EquipmentOption::getEquipmentNumber))
                .map(MesFrontlinePqcContextServiceImpl::toPqcEquipmentOptionRespVO)
                .toList();
        MesFrontlinePqcProcessRespVO.PqcInspectionItem item =
                new MesFrontlinePqcProcessRespVO.PqcInspectionItem();
        item.setItemSort(source.getItemSort());
        item.setItemCode(source.getItemCode());
        item.setItemName(source.getItemName());
        item.setInspectionMethod(source.getInspectionMethod());
        item.setStandardText(source.getStandardText());
        item.setInspectionTool(source.getInspectionTool());
        item.setSamplingPlanText(source.getSamplingPlanText());
        item.setStandardLowerLimit(source.getStandardLowerLimit());
        item.setStandardUpperLimit(source.getStandardUpperLimit());
        item.setStandardUnit(source.getStandardUnit());
        item.setStandardPrecision(source.getStandardPrecision());
        item.setEquipmentRequired(source.getEquipmentRequired());
        item.setResultType(source.getResultType());
        item.setApplicableInspectionTypes(source.getApplicableInspectionTypes());
        item.setFirstInspectionQuantity(source.getFirstInspectionQuantity());
        item.setPatrolInspectionRatio(source.getPatrolInspectionRatio());
        item.setCritical(source.getCritical());
        item.setFailureRule(source.getFailureRule());
        item.setSourceNote(source.getSourceNote());
        item.setSourceOriginalPage(source.getSourceOriginalPage());
        item.setSourceOriginalItem(source.getSourceOriginalItem());
        item.setSourceOriginalExcerpt(source.getSourceOriginalExcerpt());
        item.setSourceOriginalMethod(source.getSourceOriginalMethod());
        item.setEquipmentOptions(equipmentOptions);
        return item;
    }

    private static List<MesFrontlinePqcInspectionItem> toOverlayInspectionItems(
            List<MesFrontlinePqcProcessRespVO.PqcInspectionItem> inspectionItems,
            String inspectionType) {
        return inspectionItems.stream()
                .filter(item -> item.getApplicableInspectionTypes().contains(inspectionType))
                .map(item -> new MesFrontlinePqcInspectionItem(
                        item.getItemCode(), item.getItemName(), item.getInspectionMethod(),
                        item.getStandardText(), item.getInspectionTool(), item.getSamplingPlanText(),
                        item.getStandardLowerLimit(), item.getStandardUpperLimit(), item.getStandardUnit(),
                        item.getStandardPrecision(), item.getEquipmentRequired(), item.getResultType(),
                        item.getEquipmentOptions().stream()
                                .map(option -> new MesFrontlinePqcInspectionItem.EquipmentOption(
                                        option.getEquipmentId(), option.getEquipmentCode(),
                                        option.getEquipmentName(), option.getEquipmentNumber(),
                                        option.getDefaultFlag(), option.getSort()))
                                .toList()))
                .toList();
    }

    private List<MesFrontlineProductionSubmitCandidate> resolveProductionSubmitCandidates(
            MesProcessPoolActiveOrderDO activeOrder) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshotRows =
                processSnapshotMapper.selectListByActiveOrderId(activeOrder.getId());
        if (CollUtil.isEmpty(snapshotRows)) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "activeOrderProcessSnapshots activeOrderId=" + activeOrder.getId());
        }
        Set<ProductionProcessIdentity> identities = new LinkedHashSet<>();
        List<MesFrontlineProductionSubmitCandidate.ActiveOrderProcessSnapshot> snapshots = new ArrayList<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshotRows) {
            if (snapshot == null
                    || !Objects.equals(activeOrder.getId(), snapshot.getActiveOrderId())
                    || !Objects.equals(activeOrder.getWorkOrderId(), snapshot.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteId(), snapshot.getRouteId())
                    || !Objects.equals(activeOrder.getRouteVersionId(), snapshot.getRouteVersionId())
                    || snapshot.getRouteProcessId() == null || snapshot.getProcessId() == null
                    || !identities.add(new ProductionProcessIdentity(
                    snapshot.getRouteProcessId(), snapshot.getProcessId()))) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "activeOrderProcessSnapshot activeOrderId=" + activeOrder.getId());
            }
            snapshots.add(new MesFrontlineProductionSubmitCandidate.ActiveOrderProcessSnapshot(
                    activeOrder.getId(), snapshot.getRouteProcessId(), snapshot.getProcessId()));
        }
        List<MesProProcessPoolEventDO> events = processPoolEventMapper
                .selectProductionSubmitsByWorkOrderAndRoute(activeOrder.getWorkOrderId(), activeOrder.getRouteId());
        if (CollUtil.isEmpty(events)) {
            return List.of();
        }
        List<MesFrontlineProductionSubmitCandidate> candidates = new ArrayList<>();
        for (MesProProcessPoolEventDO event : events) {
            if (event == null || event.getId() == null || event.getServerSubmitTime() == null
                    || !MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())
                    || !Objects.equals(activeOrder.getWorkOrderId(), event.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteId(), event.getRouteId())
                    || event.getRouteProcessId() == null || event.getProcessId() == null) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "productionSubmitEvent activeOrderId=" + activeOrder.getId());
            }
            MesFrontlineProductionSubmitCandidate candidate = new MesFrontlineProductionSubmitCandidate(
                    event.getId(), event.getServerSubmitTime(), activeOrder.getId(),
                    event.getRouteProcessId(), event.getProcessId());
            if (candidate.belongsToSnapshot(snapshots)) {
                candidates.add(MesFrontlineProductionSubmitCandidate.requireActiveOrderProcessSnapshot(
                        event.getId(), event.getServerSubmitTime(), activeOrder.getId(),
                        event.getRouteProcessId(), event.getProcessId(), snapshots));
            }
        }
        candidates.sort(Comparator
                .comparing(MesFrontlineProductionSubmitCandidate::serverSubmitTime).reversed()
                .thenComparing(MesFrontlineProductionSubmitCandidate::eventId, Comparator.reverseOrder()));
        return List.copyOf(candidates);
    }

    private static MesFrontlinePqcProcessRespVO.ProductionSubmitCandidate toProductionSubmitCandidateRespVO(
            MesFrontlineProductionSubmitCandidate candidate) {
        MesFrontlinePqcProcessRespVO.ProductionSubmitCandidate respVO =
                new MesFrontlinePqcProcessRespVO.ProductionSubmitCandidate();
        respVO.setEventId(candidate.eventId());
        respVO.setServerSubmitTime(candidate.serverSubmitTime());
        respVO.setActiveOrderId(candidate.activeOrderId());
        respVO.setRouteProcessId(candidate.routeProcessId());
        respVO.setProcessId(candidate.processId());
        return respVO;
    }

    private static void requireLockedQaAggregate(
            MesProcessPoolActiveOrderDO activeOrder,
            MesQaInspectionRegulationPublishedVersionRespVO qaSource) {
        requirePositive(activeOrder.getDccProjectCodeId(), "activeOrder.dccProjectCodeId");
        requirePositive(activeOrder.getQaRegulationId(), "activeOrder.qaRegulationId");
        requirePositive(activeOrder.getQaRegulationVersionId(), "activeOrder.qaRegulationVersionId");
        if (qaSource == null
                || !Objects.equals(activeOrder.getDccProjectCodeId(), qaSource.getDccProjectCodeId())
                || !Objects.equals(activeOrder.getQaRegulationId(), qaSource.getRegulationId())
                || !Objects.equals(activeOrder.getQaRegulationVersionId(), qaSource.getPublishedVersionId())) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "lockedQaAggregate activeOrderId=" + activeOrder.getId());
        }
        if (CollUtil.isEmpty(qaSource.getProcesses())) {
            throw exception(PRO_FRONTLINE_PQC_ROUTE_PROCESS_EMPTY,
                    activeOrder.getWorkOrderId(), activeOrder.getRouteId());
        }
        Set<Long> processIds = new LinkedHashSet<>();
        for (MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess process : qaSource.getProcesses()) {
            if (process == null || process.getQaProcessId() == null
                    || StrUtil.isBlank(process.getProcessCode()) || StrUtil.isBlank(process.getProcessName())
                    || process.getSort() == null || !processIds.add(process.getQaProcessId())
                    || CollUtil.isEmpty(process.getItems())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "lockedQaProcess regulationVersionId=" + qaSource.getPublishedVersionId());
            }
            for (MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem item : process.getItems()) {
                if (item == null || StrUtil.isBlank(item.getItemCode())
                        || CollUtil.isEmpty(item.getApplicableInspectionTypes())
                        || item.getEquipmentOptions() == null) {
                    throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                            "lockedQaInspectionItems regulationVersionId=" + qaSource.getPublishedVersionId()
                                    + "，qaProcessId=" + process.getQaProcessId());
                }
            }
        }
    }

    private static MesFrontlinePqcProcessRespVO.PqcEquipmentOption toPqcEquipmentOptionRespVO(
            MesFrontlinePqcInspectionItem.EquipmentOption option) {
        MesFrontlinePqcProcessRespVO.PqcEquipmentOption respVO =
                new MesFrontlinePqcProcessRespVO.PqcEquipmentOption();
        respVO.setEquipmentId(option.equipmentId());
        respVO.setEquipmentCode(option.equipmentCode());
        respVO.setEquipmentName(option.equipmentName());
        respVO.setEquipmentNumber(option.equipmentNumber());
        respVO.setDefaultFlag(option.defaultFlag());
        respVO.setSort(option.sort());
        return respVO;
    }

    private static MesFrontlinePqcProcessRespVO.PqcEquipmentOption toPqcEquipmentOptionRespVO(
            MesQaInspectionRegulationPublishedVersionRespVO.EquipmentOption option) {
        MesFrontlinePqcProcessRespVO.PqcEquipmentOption respVO =
                new MesFrontlinePqcProcessRespVO.PqcEquipmentOption();
        respVO.setEquipmentId(option.getEquipmentId());
        respVO.setEquipmentCode(option.getEquipmentCode());
        respVO.setEquipmentName(option.getEquipmentName());
        respVO.setEquipmentNumber(option.getEquipmentNumber());
        respVO.setDefaultFlag(option.getDefaultFlag());
        respVO.setSort(option.getSort());
        return respVO;
    }

    private QaProjectProcessSource resolveQaProjectProcessSource(MesProcessPoolActiveOrderDO activeOrder,
                                                                  MesProWorkOrderDO workOrder,
                                                                  Set<Long> routeItemIds) {
        Long productId = workOrder.getProductId();
        requirePositive(productId, "workOrder.productId");
        Long routeId = activeOrder.getRouteId();
        requirePositive(routeId, "activeOrder.routeId");
        if (routeItemIds.isEmpty()) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "routeProjectCode productId=" + productId + "，routeId=" + routeId);
        }
        DccProjectCodeDO project = resolveRouteDccProject(productId, routeId, routeItemIds);
        MesQaInspectionRegulationDO regulation = regulationMapper.selectByDccProjectCodeId(project.getId());
        if (regulation == null || regulation.getId() == null
                || !Objects.equals(project.getId(), regulation.getDccProjectCodeId())
                || !MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA.equals(regulation.getOwnerModule())
                || !Objects.equals("PUBLISHED", regulation.getLifecycleStatus())
                || regulation.getCurrentVersionId() == null) {
            throw exception(PRO_FRONTLINE_PQC_ROUTE_PROCESS_EMPTY,
                    activeOrder.getWorkOrderId(), activeOrder.getRouteId());
        }
        MesQaInspectionRegulationVersionDO version = versionMapper.selectById(regulation.getCurrentVersionId());
        if (version == null || !Objects.equals(regulation.getId(), version.getRegulationId())
                || !Objects.equals("PUBLISHED", version.getLifecycleStatus())) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "qaRegulationVersion dccProjectCodeId=" + project.getId()
                            + "，regulationVersionId=" + regulation.getCurrentVersionId());
        }
        List<MesQaInspectionRegulationProcessDO> processes =
                regulationProcessMapper.selectListByVersionId(version.getId());
        if (CollUtil.isEmpty(processes)) {
            throw exception(PRO_FRONTLINE_PQC_ROUTE_PROCESS_EMPTY,
                    activeOrder.getWorkOrderId(), activeOrder.getRouteId());
        }
        Set<Long> processIds = new LinkedHashSet<>();
        for (MesQaInspectionRegulationProcessDO process : processes) {
            if (process == null || process.getId() == null
                    || !Objects.equals(version.getId(), process.getRegulationVersionId())
                    || StrUtil.isBlank(process.getProcessCode()) || StrUtil.isBlank(process.getProcessName())
                    || process.getSort() == null || !processIds.add(process.getId())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "qaProcess regulationVersionId=" + version.getId());
            }
        }
        List<MesQaInspectionRegulationItemDO> items = regulationItemMapper.selectListByVersionId(version.getId());
        Set<Long> itemProcessIds = items.stream()
                .filter(Objects::nonNull)
                .map(MesQaInspectionRegulationItemDO::getQaProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!itemProcessIds.containsAll(processIds) || !processIds.containsAll(itemProcessIds)) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "qaInspectionItems regulationVersionId=" + version.getId()
                            + "，processIds=" + processIds + "，itemProcessIds=" + itemProcessIds);
        }
        return new QaProjectProcessSource(project, regulation, version, processes, items);
    }

    private DccProjectCodeDO resolveRouteDccProject(Long productId, Long routeId, Set<Long> routeItemIds) {
        Map<Long, MesMdItemDO> routeItems = itemService.getItemMap(routeItemIds);
        Set<String> routeProjectCodes = routeItems.values().stream()
                .filter(Objects::nonNull)
                .map(MesMdItemDO::getCode)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DccProjectCodeDO> matchedProjects = dccProjectCodeMapper.selectEnabledList().stream()
                .filter(Objects::nonNull)
                .filter(project -> project.getId() != null)
                .filter(project -> project.getProductMasterId() != null)
                .filter(project -> routeProjectCodes.contains(StrUtil.trim(project.getProjectCode())))
                .collect(Collectors.toMap(DccProjectCodeDO::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        if (matchedProjects.size() != 1) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "routeProjectCode productId=" + productId
                            + "，routeId=" + routeId
                            + "，routeItemIds=" + routeItemIds
                            + "，routeProjectCodes=" + routeProjectCodes
                            + "，matchedProjectIds=" + matchedProjects.keySet());
        }
        return matchedProjects.values().iterator().next();
    }

    private Set<Long> resolveRouteVersionProductIds(MesProcessPoolActiveOrderDO activeOrder) {
        requireActiveOrderIdentity(activeOrder);
        Long routeVersionId = activeOrder.getRouteVersionId();
        requirePositive(routeVersionId, "activeOrder.routeVersionId");
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (!isPublishedRouteVersion(activeOrder, routeVersion)) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "routeVersionProducts activeOrderId=" + activeOrder.getId()
                            + "，routeId=" + activeOrder.getRouteId()
                            + "，routeVersionId=" + routeVersionId);
        }
        return parseRouteVersionProductIds(routeVersion);
    }

    private static boolean isPublishedRouteVersion(MesProcessPoolActiveOrderDO activeOrder,
                                                   MesProRouteVersionDO routeVersion) {
        if (routeVersion == null || !Objects.equals(activeOrder.getRouteId(), routeVersion.getRouteId())) {
            return false;
        }
        return (Boolean.TRUE.equals(routeVersion.getActive())
                && "ACTIVE".equals(routeVersion.getLifecycleStatus()))
                || (Boolean.FALSE.equals(routeVersion.getActive())
                && "SUPERSEDED".equals(routeVersion.getLifecycleStatus()));
    }

    private static Set<Long> parseRouteVersionProductIds(MesProRouteVersionDO routeVersion) {
        try {
            JSONObject routeSnapshot = JSONObject.parseObject(routeVersion.getRouteSnapshotJson());
            JSONObject configSnapshots = routeSnapshot == null ? null
                    : routeSnapshot.getJSONObject("configSnapshots");
            Object productsSnapshot = configSnapshots == null ? null : configSnapshots.get("products");
            Collection<?> products;
            if (productsSnapshot instanceof JSONObject productsByKey) {
                products = productsByKey.values();
            } else if (productsSnapshot instanceof JSONArray productsArray) {
                products = productsArray;
            } else {
                throw new IllegalArgumentException("configSnapshots.products is required");
            }
            Set<Long> productIds = new LinkedHashSet<>();
            for (Object value : products) {
                if (!(value instanceof JSONObject productSnapshot)) {
                    throw new IllegalArgumentException("route product itemId is required");
                }
                Long itemId = productSnapshot.getLong("itemId");
                if (itemId == null || itemId <= 0) {
                    throw new IllegalArgumentException("route product itemId is required");
                }
                productIds.add(itemId);
            }
            if (productIds.isEmpty()) {
                throw new IllegalArgumentException("route products are required");
            }
            return productIds;
        } catch (RuntimeException ex) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "routeVersionProducts routeId=" + routeVersion.getRouteId()
                            + "，routeVersionId=" + routeVersion.getId()
                            + "，reason=" + ex.getMessage());
        }
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
    public MesFrontlinePqcProcessCandidate requireActiveOrderProcess(Long workOrderId, Long routeId,
                                                                     Long regulationVersionId, Long qaProcessId) {
        requirePositive(regulationVersionId, "regulationVersionId");
        requirePositive(qaProcessId, "qaProcessId");
        return listProcessesByActiveOrder(workOrderId, routeId).stream()
                .filter(candidate -> Objects.equals(candidate.regulationVersionId(), regulationVersionId)
                        && Objects.equals(candidate.qaProcessId(), qaProcessId))
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
    public MesFrontlinePqcEmployeeSwitchResult switchPqcActualEmployee(Long loginUserId, Long workOrderId,
                                                                       Long routeId, Long regulationVersionId,
                                                                       Long qaProcessId, Long actualEmployeeId) {
        requireValue(loginUserId, "loginUserId");
        requirePqcEmployee(loginUserId, actualEmployeeId);
        MesFrontlinePqcProcessCandidate process = requireActiveOrderProcess(workOrderId, routeId,
                regulationVersionId, qaProcessId);
        MesFrontlinePqcTemplateDescriptor template = new MesFrontlinePqcTemplateDescriptor(
                FrontlineTemplateCodes.PQC_SIMPLIFIED, FrontlineTemplateTypes.PQC,
                process.qaProcessId(), actualEmployeeId);
        return new MesFrontlinePqcEmployeeSwitchResult(loginUserId, actualEmployeeId, process.routeId(),
                process.dccProjectCodeId(), process.regulationVersionId(), process.qaProcessId(), false, template);
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
                .pqcSubmissionIdempotencyKey(command.getPqcSubmissionIdempotencyKey())
                .routeId(command.getRouteId())
                .qaProcessId(command.getQaProcessId())
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
        payload.put("qaProcessId", command.getQaProcessId());
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
                .pqcSubmissionIdempotencyKey(command.getPqcSubmissionIdempotencyKey())
                .routeId(command.getRouteId())
                .qaProcessId(command.getQaProcessId())
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
        if (command.getRegulationVersionId() == null) {
            command.setRegulationVersionId(task.getRegulationVersionId());
        }
        if (command.getQaProcessId() == null) {
            command.setQaProcessId(task.getQaProcessId());
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
        validatePqcTaskSubmissionIdentity(command, task, loginUserId);
    }

    private void validatePqcTaskSubmissionIdentity(MesFrontlinePqcSubmitCommand command,
                                                   MesPqcInspectionTaskDO task,
                                                   Long loginUserId) {
        boolean taskStatusAllowed = PQC_TASK_STATUS_PENDING.equals(task.getTaskStatus())
                || PQC_TASK_STATUS_SUBMITTED.equals(task.getTaskStatus());
        if (!taskStatusAllowed
                || task.getQaProcessId() == null || task.getRegulationVersionId() == null
                || !Objects.equals(command.getActiveOrderId(), task.getActiveOrderId())
                || !Objects.equals(command.getWorkOrderId(), task.getWorkOrderId())
                || !Objects.equals(command.getRouteId(), task.getRouteId())
                || !Objects.equals(command.getRegulationVersionId(), task.getRegulationVersionId())
                || !Objects.equals(command.getQaProcessId(), task.getQaProcessId())
                || !Objects.equals(command.getInspectionType(), task.getInspectionType())
                || !Objects.equals(command.getBusinessDate(), task.getBusinessDate())
                || !Objects.equals(command.getShiftCode(), task.getShiftCode())
                || !Objects.equals(command.getRoundNo(), task.getRoundNo())
                || !Objects.equals(command.getActualEmployeeId(), loginUserId)) {
            throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, pqcTaskIdentityText(task));
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(task.getActiveOrderId());
        if (activeOrder == null || !Objects.equals("ACTIVE", activeOrder.getActiveStatus())
                || !Objects.equals(task.getWorkOrderId(), activeOrder.getWorkOrderId())
                || !Objects.equals(task.getRouteId(), activeOrder.getRouteId())
                || !Objects.equals(task.getRouteVersionId(), activeOrder.getRouteVersionId())) {
            throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, pqcTaskIdentityText(task));
        }
        MesQaInspectionRegulationProcessDO qaProcess = regulationProcessMapper.selectById(task.getQaProcessId());
        MesQaInspectionRegulationVersionDO version = versionMapper.selectById(task.getRegulationVersionId());
        MesQaInspectionRegulationDO regulation = version == null ? null : regulationMapper.selectById(
                version.getRegulationId());
        if (qaProcess == null || version == null || regulation == null
                || !Objects.equals(version.getId(), qaProcess.getRegulationVersionId())
                || !Objects.equals(regulation.getId(), version.getRegulationId())
                || regulation.getDccProjectCodeId() == null
                || dccProjectCodeMapper.selectById(regulation.getDccProjectCodeId()) == null
                || !(Objects.equals("PUBLISHED", version.getLifecycleStatus())
                || Objects.equals("RETIRED", version.getLifecycleStatus()))) {
            throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, pqcTaskIdentityText(task));
        }
        boolean hasTaskItems = regulationItemMapper.selectListByVersionId(version.getId()).stream()
                .anyMatch(item -> Objects.equals(task.getQaProcessId(), item.getQaProcessId())
                        && Objects.equals(task.getInspectionType(), item.getInspectionType()));
        if (!hasTaskItems) {
            throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, pqcTaskIdentityText(task));
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
                .filter(item -> Objects.equals(item.getQaProcessId(), task.getQaProcessId()))
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

    private MesProcessPoolActiveOrderDO requireActiveOrder(Long activeOrderId) {
        requirePositive(activeOrderId, "activeOrderId");
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
        if (activeOrder == null || !Objects.equals("ACTIVE", activeOrder.getActiveStatus())) {
            throw exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_REQUIRED, activeOrderId, null);
        }
        requireActiveOrderIdentity(activeOrder);
        requirePositive(activeOrder.getRouteVersionId(), "activeOrder.routeVersionId");
        return activeOrder;
    }

    private List<MesFrontlinePqcTaskContext> resolvePendingPqcTaskContexts(MesProcessPoolActiveOrderDO activeOrder,
                                                                           MesQaInspectionRegulationDO regulation,
                                                                           MesQaInspectionRegulationVersionDO version,
                                                                           MesQaInspectionRegulationProcessDO qaProcess,
                                                                           List<MesPqcInspectionTaskDO> tasksForProcess) {
        List<MesPqcInspectionTaskDO> pendingTasks = selectPendingTasks(tasksForProcess);
        if (pendingTasks.isEmpty()) {
            return List.of();
        }
        Map<InspectionItemKey, List<MesFrontlinePqcInspectionItem.EquipmentOption>> equipmentOptionsByItem =
                regulationItemEquipmentMapper.selectListByVersionId(version.getId()).stream()
                        .collect(Collectors.groupingBy(
                                row -> new InspectionItemKey(row.getInspectionType(), row.getItemCode()),
                                LinkedHashMap::new,
                                Collectors.mapping(MesFrontlinePqcContextServiceImpl::toEquipmentOption,
                                        Collectors.toList())));
        List<MesFrontlinePqcTaskContext> contexts = new ArrayList<>();
        for (MesPqcInspectionTaskDO task : pendingTasks) {
            if (!Objects.equals(task.getActiveOrderId(), activeOrder.getId())
                    || !Objects.equals(task.getRegulationVersionId(), regulation.getCurrentVersionId())
                    || !Objects.equals(task.getRegulationVersionId(), version.getId())
                    || !Objects.equals(task.getQaProcessId(), qaProcess.getId())) {
                throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, pqcTaskIdentityText(task));
            }
            List<MesFrontlinePqcInspectionItem> items = regulationItemMapper
                    .selectListByVersionId(task.getRegulationVersionId())
                    .stream()
                    .filter(item -> item != null && Objects.equals(item.getQaProcessId(), qaProcess.getId()))
                    .filter(item -> Objects.equals(item.getInspectionType(), task.getInspectionType()))
                    .map(item -> toProcessListInspectionItem(item,
                            equipmentOptionsByItem.getOrDefault(
                                    new InspectionItemKey(item.getInspectionType(), item.getItemCode()), List.of())))
                    .toList();
            if (items.isEmpty()) {
                throw exception(PRO_FRONTLINE_PQC_REGULATION_REQUIRED,
                        activeOrder.getId(), version.getId(), qaProcess.getId());
            }
            contexts.add(new MesFrontlinePqcTaskContext(task, version.getFinalInspectionApplicable(), items));
        }
        return contexts;
    }

    private static Map<Long, List<MesPqcInspectionTaskDO>> groupTasksByQaProcess(
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
                    && (task.getQaProcessId() == null || task.getRegulationVersionId() == null)) {
                throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, pqcTaskIdentityText(task));
            }
            if (task.getQaProcessId() != null && task.getRegulationVersionId() != null) {
                validTasks.add(task);
            }
        }
        return validTasks.stream()
                .collect(Collectors.groupingBy(MesPqcInspectionTaskDO::getQaProcessId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private static Map<Long, List<MesPqcInspectionTaskDO>> groupLockedTasksByQaProcess(
            List<MesPqcInspectionTaskDO> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return Map.of();
        }
        for (MesPqcInspectionTaskDO task : tasks) {
            if (task == null || !PQC_TASK_STATUSES.contains(task.getTaskStatus())
                    || task.getQaProcessId() == null || task.getRegulationVersionId() == null) {
                throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, pqcTaskIdentityText(task));
            }
        }
        return tasks.stream().collect(Collectors.groupingBy(
                MesPqcInspectionTaskDO::getQaProcessId, LinkedHashMap::new, Collectors.toList()));
    }

    private static void requireTasksBackedByLockedQa(
            MesProcessPoolActiveOrderDO activeOrder,
            Long regulationVersionId,
            Set<Long> qaProcessIds,
            Map<Long, List<MesPqcInspectionTaskDO>> tasksByProcess) {
        for (Map.Entry<Long, List<MesPqcInspectionTaskDO>> entry : tasksByProcess.entrySet()) {
            boolean valid = qaProcessIds.contains(entry.getKey()) && entry.getValue().stream()
                    .allMatch(task -> Objects.equals(activeOrder.getId(), task.getActiveOrderId())
                            && Objects.equals(regulationVersionId, task.getRegulationVersionId())
                            && PQC_TASK_STATUSES.contains(task.getTaskStatus())
                            && task.getBusinessDate() != null && task.getRoundNo() != null
                            && task.getId() != null
                            && ruleIdentityMatches(task));
            if (!valid) {
                throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH,
                        "activeOrderId=" + activeOrder.getId() + "，regulationVersionId="
                                + regulationVersionId + "，qaProcessId=" + entry.getKey());
            }
        }
    }

    private static void requirePendingTasksBackedByQaProcesses(
            MesProcessPoolActiveOrderDO activeOrder,
            Long regulationVersionId,
            Set<Long> qaProcessIds,
            Map<Long, List<MesPqcInspectionTaskDO>> tasksByProcess) {
        for (Map.Entry<Long, List<MesPqcInspectionTaskDO>> entry : tasksByProcess.entrySet()) {
            List<MesPqcInspectionTaskDO> pendingTasks = selectPendingTasks(entry.getValue());
            if (pendingTasks.isEmpty()) {
                continue;
            }
            boolean valid = qaProcessIds.contains(entry.getKey()) && pendingTasks.stream()
                    .allMatch(task -> Objects.equals(regulationVersionId, task.getRegulationVersionId()));
            if (!valid) {
                throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH,
                        "activeOrderId=" + activeOrder.getId() + "，regulationVersionId="
                                + regulationVersionId + "，qaProcessId=" + entry.getKey());
            }
        }
    }

    private static boolean ruleIdentityMatches(MesPqcInspectionTaskDO task) {
        RuleIdentity identity = PQC_RULE_IDENTITIES.get(task.getInspectionRuleKey());
        return identity != null
                && Objects.equals(identity.inspectionType(), task.getInspectionType())
                && Objects.equals(identity.shiftCode(), task.getShiftCode())
                && Objects.equals(identity.roundNo(), task.getRoundNo());
    }

    private static String pqcTaskIdentityText(MesPqcInspectionTaskDO task) {
        if (task == null) {
            return "task=null";
        }
        return "taskId=" + task.getId()
                + "，activeOrderId=" + task.getActiveOrderId()
                + "，regulationVersionId=" + task.getRegulationVersionId()
                + "，qaProcessId=" + task.getQaProcessId();
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

    private static MesFrontlinePqcInspectionItem toProcessListInspectionItem(
            MesQaInspectionRegulationItemDO item,
            List<MesFrontlinePqcInspectionItem.EquipmentOption> equipmentOptions) {
        if (item == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "inspectionItem");
        }
        return buildInspectionItem(item, equipmentOptions);
    }

    private static MesFrontlinePqcInspectionItem toInspectionItem(MesQaInspectionRegulationItemDO item,
                                                                  List<MesFrontlinePqcInspectionItem.EquipmentOption>
                                                                          equipmentOptions) {
        if (item == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "inspectionItem");
        }
        if (StrUtil.isBlank(item.getInspectionTool())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "inspectionItem.inspectionTool");
        }
        if (StrUtil.isBlank(item.getSamplingPlanText())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "inspectionItem.samplingPlanText");
        }
        return buildInspectionItem(item, equipmentOptions);
    }

    private static MesFrontlinePqcInspectionItem buildInspectionItem(
            MesQaInspectionRegulationItemDO item,
            List<MesFrontlinePqcInspectionItem.EquipmentOption> equipmentOptions) {
        boolean equipmentRequired = Boolean.TRUE.equals(item.getEquipmentRequired());
        return new MesFrontlinePqcInspectionItem(item.getItemCode(), item.getItemName(),
                item.getInspectionMethod(), item.getStandardText(), item.getInspectionTool(),
                item.getSamplingPlanText(), item.getStandardLowerLimit(),
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
        return new MesFrontlinePqcTaskOption(task.getId(), task.getRegulationVersionId(), task.getQaProcessId(),
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

    private static void requireProductRoute(MesProWorkOrderDO workOrder,
                                            MesProcessPoolActiveOrderDO activeOrder,
                                            Set<Long> routeVersionProductIds) {
        if (workOrder.getProductId() == null || !routeVersionProductIds.contains(workOrder.getProductId())) {
            throw exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_ROUTE_REQUIRED,
                    workOrder.getId(), workOrder.getProductId(), activeOrder.getRouteId());
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

    private record LatestActiveOrderContext(MesProcessPoolActiveOrderDO activeOrder, LocalDateTime latestSubmitTime) {
    }

    private record QaProjectProcessSource(DccProjectCodeDO project,
                                           MesQaInspectionRegulationDO regulation,
                                           MesQaInspectionRegulationVersionDO version,
                                           List<MesQaInspectionRegulationProcessDO> processes,
                                           List<MesQaInspectionRegulationItemDO> items) {
    }

    private record InspectionItemKey(String inspectionType, String itemCode) {
    }

    private record ProductionProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record RuleIdentity(String inspectionType,
                                String shiftCode,
                                Integer roundNo,
                                Integer ruleSort) {
    }

    private record MesFrontlinePqcTaskContext(MesPqcInspectionTaskDO task,
                                              Boolean finalInspectionApplicable,
                                              List<MesFrontlinePqcInspectionItem> inspectionItems) {
    }
}
