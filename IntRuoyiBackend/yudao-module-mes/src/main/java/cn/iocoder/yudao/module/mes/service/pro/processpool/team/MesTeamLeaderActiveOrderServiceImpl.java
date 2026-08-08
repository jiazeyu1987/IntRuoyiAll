package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_IDENTITY_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;

@Service
@Validated
public class MesTeamLeaderActiveOrderServiceImpl implements MesTeamLeaderActiveOrderService {

    static final String STATUS_ACTIVE = "ACTIVE";
    static final String STATUS_REMOVED = "REMOVED";
    private static final String PQC_STATUS_PENDING = "PENDING";
    private static final String INSPECTION_TYPE_FIRST = "FIRST";
    private static final String INSPECTION_TYPE_PATROL = "PATROL";
    private static final String SHIFT_FIRST = "FIRST";
    private static final String SHIFT_PATROL = "PATROL";
    private static final int DEFAULT_ROUND_NO = 1;
    private static final int PROGRESS_PERCENT_SCALE = 6;
    private static final BigDecimal PERCENT_DIVISOR = BigDecimal.valueOf(100);
    private static final BigDecimal DEFAULT_PRODUCTION_QUANTITY_FACTOR =
            BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderService workOrderService;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesMdItemMapper itemMapper;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    private final MesProScheduleOrderMapper scheduleOrderMapper;
    private final MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    private final MesProRouteProductMapper routeProductMapper;
    private final MesProRouteMapper routeMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesQaInspectionRegulationMapper inspectionRegulationMapper;
    private final MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper;
    private final MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    private final MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    private final MesWorkOrderAbnormalStateService abnormalStateService;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;

    public MesTeamLeaderActiveOrderServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                               MesProWorkOrderService workOrderService,
                                               MesProWorkOrderMapper workOrderMapper,
                                               MesMdItemMapper itemMapper,
                                               MesProcessPoolTeamMaintenanceAuditMapper auditMapper,
                                               MesProScheduleOrderMapper scheduleOrderMapper,
                                               MesProScheduleOrderProcessMapper scheduleOrderProcessMapper,
                                               MesProRouteProductMapper routeProductMapper,
                                               MesProRouteMapper routeMapper,
                                               MesProRouteVersionMapper routeVersionMapper,
                                               MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
                                               MesProcessPoolOrderProcessCompletionMapper completionMapper,
                                               MesQaInspectionRegulationMapper inspectionRegulationMapper,
                                               MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper,
                                               MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper,
                                               MesPqcInspectionTaskMapper pqcInspectionTaskMapper,
                                               MesWorkOrderAbnormalStateService abnormalStateService,
                                               MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderService = workOrderService;
        this.workOrderMapper = workOrderMapper;
        this.itemMapper = itemMapper;
        this.auditMapper = auditMapper;
        this.scheduleOrderMapper = scheduleOrderMapper;
        this.scheduleOrderProcessMapper = scheduleOrderProcessMapper;
        this.routeProductMapper = routeProductMapper;
        this.routeMapper = routeMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.processSnapshotMapper = processSnapshotMapper;
        this.completionMapper = completionMapper;
        this.inspectionRegulationMapper = inspectionRegulationMapper;
        this.inspectionRegulationVersionMapper = inspectionRegulationVersionMapper;
        this.inspectionRegulationItemMapper = inspectionRegulationItemMapper;
        this.pqcInspectionTaskMapper = pqcInspectionTaskMapper;
        this.abnormalStateService = abnormalStateService;
        this.releaseApplicationMapper = releaseApplicationMapper;
    }

    @Override
    public List<MesTeamLeaderActiveOrderCandidateBO> searchActiveOrderCandidates(String keyword) {
        String searchText = keyword == null ? "" : keyword.trim();
        if (searchText.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = itemMapper.selectListByCodeOrNameLike(searchText, 20).stream()
                .map(MesMdItemDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectCandidatesByKeyword(searchText,
                productIds, 20);
        CandidateEligibilityContext context = buildCandidateEligibilityContext(workOrders);
        return workOrders.stream()
                .map(workOrder -> toActiveOrderCandidate(workOrder, context))
                .sorted((left, right) -> Boolean.compare(right.isEligible(), left.isEligible()))
                .toList();
    }

    private CandidateEligibilityContext buildCandidateEligibilityContext(List<MesProWorkOrderDO> workOrders) {
        if (workOrders == null || workOrders.isEmpty()) {
            return emptyCandidateEligibilityContext();
        }
        List<Long> productIds = workOrders.stream()
                .map(MesProWorkOrderDO::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<MesQaInspectionRegulationDO> regulations = productIds.isEmpty()
                ? Collections.emptyList()
                : inspectionRegulationMapper.selectListByProductIds(productIds).stream()
                .filter(regulation -> Objects.equals("PUBLISHED", regulation.getLifecycleStatus()))
                .filter(regulation -> regulation.getCurrentVersionId() != null)
                .toList();
        Map<CandidateRegulationKey, List<MesQaInspectionRegulationDO>> regulationsByKey = regulations.stream()
                .filter(regulation -> regulation.getProductId() != null)
                .filter(regulation -> regulation.getRouteId() != null)
                .filter(regulation -> regulation.getRouteVersionId() != null)
                .filter(regulation -> regulation.getRouteProcessId() != null)
                .filter(regulation -> regulation.getProcessId() != null)
                .collect(Collectors.groupingBy(this::toCandidateRegulationKey));
        List<Long> versionIds = regulations.stream()
                .map(MesQaInspectionRegulationDO::getCurrentVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesQaInspectionRegulationVersionDO> versionsById = versionIds.isEmpty()
                ? Collections.emptyMap()
                : inspectionRegulationVersionMapper.selectBatchIds(versionIds).stream()
                .filter(version -> version.getId() != null)
                .collect(Collectors.toMap(MesQaInspectionRegulationVersionDO::getId, Function.identity(),
                        (left, right) -> left));
        Map<Long, List<MesQaInspectionRegulationItemDO>> itemsByVersionId = versionIds.isEmpty()
                ? Collections.emptyMap()
                : inspectionRegulationItemMapper.selectListByVersionIds(versionIds).stream()
                .filter(item -> item.getRegulationVersionId() != null)
                .collect(Collectors.groupingBy(MesQaInspectionRegulationItemDO::getRegulationVersionId));
        return new CandidateEligibilityContext(regulationsByKey, versionsById, itemsByVersionId);
    }

    private static RouteSourceResolution routeSourceFailure(String reason, RouteSourceFailureType failureType) {
        return new RouteSourceResolution(null, reason, failureType);
    }

    private static CandidateEligibilityContext emptyCandidateEligibilityContext() {
        return new CandidateEligibilityContext(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    private MesTeamLeaderActiveOrderCandidateBO toActiveOrderCandidate(MesProWorkOrderDO workOrder,
                                                                       CandidateEligibilityContext context) {
        CandidateEligibility eligibility = evaluateCandidateEligibility(workOrder, context);
        return MesTeamLeaderActiveOrderCandidateBO.builder()
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .eligible(eligibility.eligible())
                .ineligibleReason(eligibility.ineligibleReason())
                .build();
    }

    private CandidateRegulationKey toCandidateRegulationKey(MesQaInspectionRegulationDO regulation) {
        return new CandidateRegulationKey(regulation.getProductId(), regulation.getRouteId(),
                regulation.getRouteVersionId(), regulation.getRouteProcessId(), regulation.getProcessId());
    }

    private CandidateEligibility evaluateCandidateEligibility(MesProWorkOrderDO workOrder,
                                                              CandidateEligibilityContext context) {
        if (workOrder == null || workOrder.getId() == null) {
            return blockedCandidate("缺少生产工单");
        }
        RouteSourceResolution resolution = resolveQaRouteSource(workOrder, context);
        if (resolution.source() == null) {
            return blockedCandidate(resolution.ineligibleReason());
        }
        ActiveOrderRouteSource source = resolution.source();
        for (MesProScheduleOrderProcessDO process : source.enabledProcesses()) {
            String pqcReason = validateCandidatePqcPrerequisites(source.routeId(), source.routeVersionId(),
                    process, source.productId(), context);
            if (pqcReason != null) {
                return blockedCandidate(pqcReason);
            }
        }
        return new CandidateEligibility(true, null);
    }

    private RouteSourceResolution resolveQaRouteSource(MesProWorkOrderDO workOrder,
                                                       CandidateEligibilityContext context) {
        if (workOrder.getProductId() == null) {
            return routeSourceFailure("缺少产品ID", RouteSourceFailureType.QA);
        }
        List<MesQaInspectionRegulationDO> regulations = listCandidateQaRegulations(workOrder.getProductId(), context);
        if (regulations.isEmpty()) {
            return routeSourceFailure("缺少已发布QA规程", RouteSourceFailureType.QA);
        }
        Map<QaRouteVersionKey, List<MesQaInspectionRegulationDO>> regulationsByRouteVersion = regulations.stream()
                .collect(Collectors.groupingBy(regulation -> new QaRouteVersionKey(regulation.getRouteId(),
                                regulation.getRouteVersionId()),
                        LinkedHashMap::new, Collectors.toList()));
        if (regulationsByRouteVersion.size() > 1) {
            return routeSourceFailure("已发布QA规程路线版本不唯一", RouteSourceFailureType.QA);
        }
        Map.Entry<QaRouteVersionKey, List<MesQaInspectionRegulationDO>> entry =
                regulationsByRouteVersion.entrySet().iterator().next();
        List<MesProScheduleOrderProcessDO> qaProcesses = toQaProcessSources(workOrder, entry.getValue());
        if (qaProcesses.isEmpty()) {
            return routeSourceFailure("缺少已发布QA规程", RouteSourceFailureType.QA);
        }
        return new RouteSourceResolution(new ActiveOrderRouteSource(entry.getKey().routeId(),
                entry.getKey().routeVersionId(), workOrder.getProductId(), qaProcesses), null, null);
    }

    private List<MesQaInspectionRegulationDO> listCandidateQaRegulations(Long productId,
                                                                         CandidateEligibilityContext context) {
        return context.regulationsByKey().entrySet().stream()
                .filter(entry -> Objects.equals(productId, entry.getKey().productId()))
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }

    private List<MesProScheduleOrderProcessDO> toQaProcessSources(MesProWorkOrderDO workOrder,
                                                                   List<MesQaInspectionRegulationDO> regulations) {
        Set<CandidateRegulationKey> processIdentities = new LinkedHashSet<>();
        BigDecimal quantity = activeOrderQuantitySnapshot(workOrder).setScale(6, RoundingMode.HALF_UP);
        List<MesProScheduleOrderProcessDO> processes = new ArrayList<>();
        for (MesQaInspectionRegulationDO regulation : regulations) {
            CandidateRegulationKey key = toCandidateRegulationKey(regulation);
            if (!processIdentities.add(key)) {
                continue;
            }
            processes.add(MesProScheduleOrderProcessDO.builder()
                    .routeProcessId(regulation.getRouteProcessId())
                    .routeVersionId(regulation.getRouteVersionId())
                    .processId(regulation.getProcessId())
                    .enabled(Boolean.TRUE)
                    .productionQuantityFactor(DEFAULT_PRODUCTION_QUANTITY_FACTOR)
                    .plannedQuantity(quantity)
                    .build());
        }
        processes.sort(Comparator
                .comparing(MesProScheduleOrderProcessDO::getRouteProcessId,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MesProScheduleOrderProcessDO::getProcessId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return processes;
    }

    private String validateCandidatePqcPrerequisites(Long routeId,
                                                     Long routeVersionId,
                                                     MesProScheduleOrderProcessDO process,
                                                     Long productId,
                                                     CandidateEligibilityContext context) {
        CandidateRegulationKey key = new CandidateRegulationKey(productId, routeId,
                routeVersionId, process.getRouteProcessId(), process.getProcessId());
        List<MesQaInspectionRegulationDO> regulations = context.regulationsByKey()
                .getOrDefault(key, Collections.emptyList());
        if (regulations.isEmpty()) {
            return "缺少已发布QA规程";
        }
        if (regulations.size() > 1) {
            return "已发布QA规程不唯一";
        }
        MesQaInspectionRegulationDO regulation = regulations.get(0);
        if (regulation.getCurrentVersionId() == null) {
            return "缺少已发布QA规程";
        }
        MesQaInspectionRegulationVersionDO version = context.versionsById().get(regulation.getCurrentVersionId());
        if (version == null || !Objects.equals("PUBLISHED", version.getLifecycleStatus())) {
            return "QA规程发布版本不存在或未发布";
        }
        List<MesQaInspectionRegulationItemDO> items = context.itemsByVersionId()
                .getOrDefault(regulation.getCurrentVersionId(), Collections.emptyList());
        if (items == null || items.isEmpty()) {
            return "已发布QA规程缺少检验项目";
        }
        String firstReason = validateFixedInspectionQuantity(items, INSPECTION_TYPE_FIRST);
        if (firstReason != null) {
            return firstReason;
        }
        String patrolReason = validatePatrolInspectionQuantity(process, items);
        if (patrolReason != null) {
            return patrolReason;
        }
        return null;
    }

    private String validateFixedInspectionQuantity(List<MesQaInspectionRegulationItemDO> items,
                                                   String inspectionType) {
        Integer quantity = null;
        for (MesQaInspectionRegulationItemDO item : items) {
            if (!Objects.equals(inspectionType, normalizeInspectionType(item.getInspectionType()))) {
                continue;
            }
            Integer itemQuantity = item.getFirstInspectionQuantity();
            if (itemQuantity == null || itemQuantity <= 0) {
                return "固定检验数量无效";
            }
            if (quantity != null && !Objects.equals(quantity, itemQuantity)) {
                return "同一检验类型存在不同固定数量";
            }
            quantity = itemQuantity;
        }
        return quantity == null ? "缺少检验类型规则" : null;
    }

    private String validatePatrolInspectionQuantity(MesProScheduleOrderProcessDO process,
                                                    List<MesQaInspectionRegulationItemDO> items) {
        BigDecimal ratio = null;
        Integer fixedQuantity = null;
        for (MesQaInspectionRegulationItemDO item : items) {
            if (!Objects.equals(INSPECTION_TYPE_PATROL, normalizeInspectionType(item.getInspectionType()))) {
                continue;
            }
            if (positive(item.getPatrolInspectionRatio())) {
                if (fixedQuantity != null) {
                    return "巡检规则同时存在固定数量和比例";
                }
                if (ratio != null && ratio.compareTo(item.getPatrolInspectionRatio()) != 0) {
                    return "同一巡检规则存在不同比例";
                }
                ratio = item.getPatrolInspectionRatio();
                continue;
            }
            Integer itemQuantity = item.getFirstInspectionQuantity();
            if (itemQuantity == null || itemQuantity <= 0) {
                return "巡检数量规则无效";
            }
            if (ratio != null) {
                return "巡检规则同时存在固定数量和比例";
            }
            if (fixedQuantity != null && !Objects.equals(fixedQuantity, itemQuantity)) {
                return "同一巡检规则存在不同固定数量";
            }
            fixedQuantity = itemQuantity;
        }
        if (ratio != null) {
            if (process.getPlannedQuantity() == null || process.getPlannedQuantity().compareTo(BigDecimal.ZERO) < 0) {
                return "排产工序计划数量无效";
            }
            try {
                calculatePatrolInspectionQuantity(process.getPlannedQuantity(), ratio);
            } catch (ArithmeticException ex) {
                return "巡检计划数量超出整数范围";
            }
            return null;
        }
        return fixedQuantity == null ? "缺少巡检规则" : null;
    }

    private CandidateEligibility blockedCandidate(String reason) {
        return new CandidateEligibility(false, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getWorkOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrder");
        }
        MesProWorkOrderDO workOrder = workOrderService.validateWorkOrderExists(reqBO.getWorkOrderId());
        BigDecimal erpFixedQuantity = activeOrderQuantitySnapshot(workOrder);
        ActiveOrderRouteSource routeSource = requireQaRouteSourceForAdd(workOrder);
        Long routeId = routeSource.routeId();
        Long routeVersionId = routeSource.routeVersionId();
        MesProcessPoolActiveOrderDO existing = selectExistingActiveOrder(reqBO.getWorkOrderId(), routeId,
                routeVersionId);
        if (existing != null) {
            return existing.getId();
        }
        MesProcessPoolActiveOrderDO removed = selectRemovedActiveOrder(reqBO.getWorkOrderId(), routeId,
                routeVersionId);
        if (removed != null) {
            return reactivateRemovedActiveOrder(reqBO, removed);
        }
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .workOrderId(reqBO.getWorkOrderId())
                .routeId(routeId)
                .routeVersionId(routeVersionId)
                .erpFixedQuantitySnapshot(erpFixedQuantity)
                .activeStatus(STATUS_ACTIVE)
                .businessStatus(STATUS_ACTIVE)
                .joinedAt(LocalDateTime.now())
                .version(0)
                .build();
        try {
            activeOrderMapper.insert(activeOrder);
        } catch (DuplicateKeyException ex) {
            MesProcessPoolActiveOrderDO concurrentlyAdded = selectExistingActiveOrder(reqBO.getWorkOrderId(), routeId,
                    routeVersionId);
            if (concurrentlyAdded != null) {
                return concurrentlyAdded.getId();
            }
            MesProcessPoolActiveOrderDO concurrentlyRemoved = selectRemovedActiveOrder(reqBO.getWorkOrderId(), routeId,
                    routeVersionId);
            if (concurrentlyRemoved != null) {
                return reactivateRemovedActiveOrder(reqBO, concurrentlyRemoved);
            }
            throw ex;
        }
        List<MesProScheduleOrderProcessDO> enabledProcesses = routeSource.enabledProcesses();
        insertProcessSnapshots(activeOrder, erpFixedQuantity, enabledProcesses);
        insertPqcInspectionTasks(activeOrder, routeSource.productId(), enabledProcesses);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "ADD_ACTIVE_ORDER",
                "ACTIVE_ORDER", activeOrder.getId(), null, activeOrder.toString());
        return activeOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getActiveOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "removeActiveOrder");
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(reqBO.getActiveOrderId());
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), reqBO.getLeaderUserId())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, reqBO.getActiveOrderId());
        }
        LocalDateTime removedAt = LocalDateTime.now();
        int updated = activeOrderMapper.removeActiveOrder(activeOrder.getId(), activeOrder.getVersion(), removedAt);
        if (updated <= 0) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, reqBO.getActiveOrderId());
        }
        MesProcessPoolActiveOrderDO update = MesProcessPoolActiveOrderDO.builder()
                .id(activeOrder.getId())
                .activeStatus(STATUS_REMOVED)
                .businessStatus(STATUS_REMOVED)
                .removedAt(removedAt)
                .version(activeOrder.getVersion() == null ? null : activeOrder.getVersion() + 1)
                .build();
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "REMOVE_ACTIVE_ORDER",
                "ACTIVE_ORDER", activeOrder.getId(), activeOrder.toString(), update.toString());
    }

    private MesProcessPoolActiveOrderDO selectExistingActiveOrder(Long workOrderId, Long routeId,
                                                                  Long routeVersionId) {
        return activeOrderMapper.selectActiveByWorkOrderRouteVersion(workOrderId, routeId, routeVersionId);
    }

    private MesProcessPoolActiveOrderDO selectRemovedActiveOrder(Long workOrderId, Long routeId,
                                                                 Long routeVersionId) {
        return activeOrderMapper.selectRemovedByWorkOrderRouteVersion(workOrderId, routeId, routeVersionId);
    }

    private Long reactivateRemovedActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO,
                                              MesProcessPoolActiveOrderDO removed) {
        LocalDateTime rejoinedAt = LocalDateTime.now();
        int updated = activeOrderMapper.reactivateRemovedActiveOrder(removed.getId(), reqBO.getLeaderUserId(),
                removed.getVersion(), rejoinedAt);
        if (updated > 0) {
            MesProcessPoolActiveOrderDO after = MesProcessPoolActiveOrderDO.builder()
                    .id(removed.getId())
                    .leaderUserId(reqBO.getLeaderUserId())
                    .workOrderId(removed.getWorkOrderId())
                    .routeId(removed.getRouteId())
                    .routeVersionId(removed.getRouteVersionId())
                    .erpFixedQuantitySnapshot(removed.getErpFixedQuantitySnapshot())
                    .activeStatus(STATUS_ACTIVE)
                    .businessStatus(STATUS_ACTIVE)
                    .joinedAt(rejoinedAt)
                    .version(removed.getVersion() == null ? null : removed.getVersion() + 1)
                    .build();
            TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "REACTIVATE_ACTIVE_ORDER",
                    "ACTIVE_ORDER", removed.getId(), removed.toString(), after.toString());
            return removed.getId();
        }
        MesProcessPoolActiveOrderDO concurrentlyAdded = selectExistingActiveOrder(removed.getWorkOrderId(),
                removed.getRouteId(), removed.getRouteVersionId());
        if (concurrentlyAdded != null) {
            return concurrentlyAdded.getId();
        }
        throw new IllegalStateException("Failed to reactivate removed active order: " + removed.getId());
    }

    @Override
    public List<MesTeamLeaderActiveOrderRow> listActiveOrders(Long leaderUserId) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderList");
        }
        List<MesProcessPoolActiveOrderDO> activeOrders = activeOrderMapper.selectActiveListByLeader(leaderUserId);
        if (activeOrders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> routeIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesProRouteDO> routesById = routeMapper.selectBatchIds(routeIds).stream()
                .filter(route -> route.getId() != null)
                .collect(Collectors.toMap(MesProRouteDO::getId, Function.identity(), (left, right) -> left));
        activeOrders.forEach(activeOrder -> requireFormalRoute(activeOrder, routesById));

        List<Long> routeVersionIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getRouteVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesProRouteVersionDO> routeVersionsById = routeVersionMapper.selectBatchIds(routeVersionIds).stream()
                .filter(routeVersion -> routeVersion.getId() != null)
                .collect(Collectors.toMap(MesProRouteVersionDO::getId, Function.identity(),
                        (left, right) -> left));
        activeOrders.forEach(activeOrder -> requireFormalRouteVersion(activeOrder, routeVersionsById));
        Map<Long, MesProWorkOrderDO> workOrdersById = loadActiveOrderWorkOrders(activeOrders);
        Map<Long, MesMdItemDO> productsById = loadActiveOrderProducts(workOrdersById.values());
        List<Long> workOrderIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getWorkOrderId)
                .distinct()
                .toList();
        Map<Long, MesProcessPoolWorkOrderAbnormalDO> openAbnormalByWorkOrderId =
                abnormalStateService.findLatestOpenByWorkOrderIds(workOrderIds);
        Map<Long, ActiveOrderProgress> progressByActiveOrderId = loadActiveOrderProgress(activeOrders);
        Map<Long, MesProcessPoolActiveOrderReleaseApplicationDO> latestReleaseApplicationByActiveOrderId =
                loadLatestReleaseApplications(activeOrders);
        return activeOrders.stream()
                .map(activeOrder -> toActiveOrderRow(activeOrder, routesById, routeVersionsById,
                        workOrdersById, productsById, progressByActiveOrderId, openAbnormalByWorkOrderId,
                        latestReleaseApplicationByActiveOrderId))
                .toList();
    }

    private Map<Long, MesProcessPoolActiveOrderReleaseApplicationDO> loadLatestReleaseApplications(
            List<MesProcessPoolActiveOrderDO> activeOrders) {
        List<Long> activeOrderIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesProcessPoolActiveOrderReleaseApplicationDO> latestByActiveOrderId = new LinkedHashMap<>();
        for (MesProcessPoolActiveOrderReleaseApplicationDO application
                : releaseApplicationMapper.selectLatestByActiveOrderIds(activeOrderIds)) {
            if (application.getActiveOrderId() != null) {
                latestByActiveOrderId.putIfAbsent(application.getActiveOrderId(), application);
            }
        }
        return latestByActiveOrderId;
    }

    private void requireFormalRoute(MesProcessPoolActiveOrderDO activeOrder,
                                    Map<Long, MesProRouteDO> routesById) {
        MesProRouteDO route = routesById.get(activeOrder.getRouteId());
        if (route == null || route.getName() == null || route.getName().isBlank()) {
            throw exception(PRO_ROUTE_NOT_EXISTS);
        }
    }

    private void requireFormalRouteVersion(MesProcessPoolActiveOrderDO activeOrder,
                                           Map<Long, MesProRouteVersionDO> routeVersionsById) {
        MesProRouteVersionDO routeVersion = routeVersionsById.get(activeOrder.getRouteVersionId());
        if (routeVersion == null
                || !Objects.equals(activeOrder.getRouteId(), routeVersion.getRouteId())
                || routeVersion.getVersionNo() == null
                || routeVersion.getVersionNo().isBlank()) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, activeOrder.getRouteVersionId());
        }
    }

    private Map<Long, MesProWorkOrderDO> loadActiveOrderWorkOrders(
            List<MesProcessPoolActiveOrderDO> activeOrders) {
        List<Long> workOrderIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getWorkOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesProWorkOrderDO> workOrdersById = workOrderIds.isEmpty()
                ? Collections.emptyMap()
                : workOrderMapper.selectBatchIds(workOrderIds).stream()
                .filter(workOrder -> workOrder.getId() != null)
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, Function.identity(), (left, right) -> left));
        activeOrders.forEach(activeOrder -> {
            if (activeOrder.getWorkOrderId() == null
                    || !workOrdersById.containsKey(activeOrder.getWorkOrderId())) {
                throw exception(PRO_WORK_ORDER_NOT_EXISTS);
            }
        });
        return workOrdersById;
    }

    private Map<Long, MesMdItemDO> loadActiveOrderProducts(Collection<MesProWorkOrderDO> workOrders) {
        List<Long> productIds = workOrders.stream()
                .map(MesProWorkOrderDO::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesMdItemDO> productsById = productIds.isEmpty()
                ? Collections.emptyMap()
                : itemMapper.selectBatchIds(productIds).stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(MesMdItemDO::getId, Function.identity(), (left, right) -> left));
        workOrders.forEach(workOrder -> {
            if (workOrder.getProductId() == null || !productsById.containsKey(workOrder.getProductId())) {
                throw exception(MD_ITEM_NOT_EXISTS);
            }
        });
        return productsById;
    }

    private Map<Long, ActiveOrderProgress> loadActiveOrderProgress(List<MesProcessPoolActiveOrderDO> activeOrders) {
        List<Long> activeOrderIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> workOrderIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getWorkOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, List<MesProcessPoolActiveOrderProcessSnapshotDO>> snapshotsByActiveOrderId =
                processSnapshotMapper.selectListByActiveOrderIds(activeOrderIds).stream()
                        .filter(snapshot -> snapshot.getActiveOrderId() != null)
                        .collect(Collectors.groupingBy(MesProcessPoolActiveOrderProcessSnapshotDO::getActiveOrderId,
                                LinkedHashMap::new, Collectors.toList()));
        Set<WorkOrderProcessIdentity> completedProcesses = completionMapper.selectListByWorkOrderIds(workOrderIds)
                .stream()
                .filter(completion -> MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED
                        .equals(completion.getCompletionStatus()))
                .map(completion -> new WorkOrderProcessIdentity(completion.getWorkOrderId(),
                        completion.getRouteProcessId(), completion.getProcessId()))
                .filter(WorkOrderProcessIdentity::complete)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<ActiveOrderProcessIdentity> inspectedProcesses =
                pqcInspectionTaskMapper.selectListByActiveOrderIds(activeOrderIds).stream()
                        .filter(MesTeamLeaderActiveOrderServiceImpl::isInspectionProgressCompleted)
                        .map(task -> new ActiveOrderProcessIdentity(task.getActiveOrderId(),
                                task.getRouteProcessId(), task.getProcessId()))
                        .filter(ActiveOrderProcessIdentity::complete)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ActiveOrderProgress> progressByActiveOrderId = new LinkedHashMap<>();
        activeOrders.forEach(activeOrder -> {
            List<ProcessIdentity> processIdentities = requireProgressProcessIdentities(activeOrder,
                    snapshotsByActiveOrderId.getOrDefault(activeOrder.getId(), List.of()));
            int totalProcessCount = processIdentities.size();
            long completedProcessCount = processIdentities.stream()
                    .filter(process -> completedProcesses.contains(new WorkOrderProcessIdentity(
                            activeOrder.getWorkOrderId(), process.routeProcessId(), process.processId())))
                    .count();
            long inspectedProcessCount = processIdentities.stream()
                    .filter(process -> inspectedProcesses.contains(new ActiveOrderProcessIdentity(
                            activeOrder.getId(), process.routeProcessId(), process.processId())))
                    .count();
            progressByActiveOrderId.put(activeOrder.getId(), new ActiveOrderProgress(
                    toProgressPercent(completedProcessCount, totalProcessCount),
                    toProgressPercent(inspectedProcessCount, totalProcessCount)));
        });
        return progressByActiveOrderId;
    }

    private static List<ProcessIdentity> requireProgressProcessIdentities(MesProcessPoolActiveOrderDO activeOrder,
                                                                          List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        if (activeOrder.getId() == null || snapshots.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        List<ProcessIdentity> identities = snapshots.stream()
                .map(snapshot -> {
                    if (!Objects.equals(activeOrder.getWorkOrderId(), snapshot.getWorkOrderId())
                            || !Objects.equals(activeOrder.getRouteId(), snapshot.getRouteId())
                            || !Objects.equals(activeOrder.getRouteVersionId(), snapshot.getRouteVersionId())
                            || snapshot.getRouteProcessId() == null
                            || snapshot.getProcessId() == null) {
                        throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
                    }
                    return new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId());
                })
                .toList();
        Set<ProcessIdentity> distinctIdentities = new LinkedHashSet<>(identities);
        if (distinctIdentities.size() != identities.size()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        return List.copyOf(distinctIdentities);
    }

    private static boolean isInspectionProgressCompleted(MesPqcInspectionTaskDO task) {
        return MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED.equals(task.getTaskStatus())
                || MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus());
    }

    private static BigDecimal toProgressPercent(long completedProcessCount, int totalProcessCount) {
        if (totalProcessCount <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, "activeOrderProgress");
        }
        return BigDecimal.valueOf(completedProcessCount)
                .multiply(PERCENT_DIVISOR)
                .divide(BigDecimal.valueOf(totalProcessCount), PROGRESS_PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private MesTeamLeaderActiveOrderRow toActiveOrderRow(
            MesProcessPoolActiveOrderDO activeOrder,
            Map<Long, MesProRouteDO> routesById,
            Map<Long, MesProRouteVersionDO> routeVersionsById,
            Map<Long, MesProWorkOrderDO> workOrdersById,
            Map<Long, MesMdItemDO> productsById,
            Map<Long, ActiveOrderProgress> progressByActiveOrderId,
            Map<Long, MesProcessPoolWorkOrderAbnormalDO> openAbnormalByWorkOrderId,
            Map<Long, MesProcessPoolActiveOrderReleaseApplicationDO> releaseApplicationByActiveOrderId) {
        MesProRouteDO route = routesById.get(activeOrder.getRouteId());
        MesProRouteVersionDO routeVersion = routeVersionsById.get(activeOrder.getRouteVersionId());
        MesProWorkOrderDO workOrder = workOrdersById.get(activeOrder.getWorkOrderId());
        MesMdItemDO product = productsById.get(workOrder.getProductId());
        ActiveOrderProgress progress = progressByActiveOrderId.get(activeOrder.getId());
        if (progress == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        MesProcessPoolWorkOrderAbnormalDO abnormal = openAbnormalByWorkOrderId.get(activeOrder.getWorkOrderId());
        MesProcessPoolActiveOrderReleaseApplicationDO releaseApplication =
                releaseApplicationByActiveOrderId.get(activeOrder.getId());
        return new MesTeamLeaderActiveOrderRow()
                .setId(activeOrder.getId())
                .setLeaderUserId(activeOrder.getLeaderUserId())
                .setWorkOrderId(activeOrder.getWorkOrderId())
                .setWorkOrderCode(workOrder.getCode())
                .setProductName(product.getName())
                .setProductCode(product.getCode())
                .setQuantity(workOrder.getQuantity())
                .setRouteId(activeOrder.getRouteId())
                .setRouteName(route.getName())
                .setRouteVersionId(activeOrder.getRouteVersionId())
                .setRouteVersionNo(routeVersion.getVersionNo())
                .setErpFixedQuantitySnapshot(activeOrder.getErpFixedQuantitySnapshot())
                .setProductionProgressPercent(progress.productionProgressPercent())
                .setInspectionProgressPercent(progress.inspectionProgressPercent())
                .setActiveStatus(activeOrder.getActiveStatus())
                .setBusinessStatus(activeOrder.getBusinessStatus())
                .setJoinedAt(activeOrder.getJoinedAt())
                .setRemovedAt(activeOrder.getRemovedAt())
                .setVersion(activeOrder.getVersion())
                .setAbnormal(abnormal != null)
                .setAbnormalReason(abnormal == null ? null : abnormal.getAbnormalDescription())
                .setAbnormalReportedAt(abnormal == null ? null : abnormal.getReportedAt())
                .setReleaseApplicationStatus(releaseApplication == null ? null : releaseApplication.getApplicationStatus())
                .setReleaseApplicationBlockerSummary(releaseApplication == null ? null : releaseApplication.getBlockerSnapshotJson())
                .setReleaseApprovalWorkTaskId(releaseApplication == null ? null : releaseApplication.getReleaseApprovalWorkTaskId());
    }

    private ActiveOrderRouteSource requireQaRouteSourceForAdd(MesProWorkOrderDO workOrder) {
        CandidateEligibilityContext context = buildCandidateEligibilityContext(List.of(workOrder));
        RouteSourceResolution resolution = resolveQaRouteSource(workOrder, context);
        if (resolution.source() == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    resolution.ineligibleReason() + "，workOrderId=" + workOrder.getId());
        }
        ActiveOrderRouteSource source = resolution.source();
        for (MesProScheduleOrderProcessDO process : source.enabledProcesses()) {
            String pqcReason = validateCandidatePqcPrerequisites(source.routeId(), source.routeVersionId(),
                    process, source.productId(), context);
            if (pqcReason != null) {
                throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                        pqcReason + "，workOrderId=" + workOrder.getId()
                                + "，routeProcessId=" + process.getRouteProcessId()
                                + "，processId=" + process.getProcessId());
            }
        }
        return source;
    }

    private void insertProcessSnapshots(MesProcessPoolActiveOrderDO activeOrder, BigDecimal erpFixedQuantity,
                                        List<MesProScheduleOrderProcessDO> enabledProcesses) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = enabledProcesses.stream()
                .map(process -> toProcessSnapshot(activeOrder, process, erpFixedQuantity))
                .toList();
        if (!Boolean.TRUE.equals(processSnapshotMapper.insertBatch(snapshots))) {
            throw new IllegalStateException("Failed to insert active order process snapshots");
        }
    }

    private void insertPqcInspectionTasks(MesProcessPoolActiveOrderDO activeOrder, Long productId,
                                          List<MesProScheduleOrderProcessDO> enabledProcesses) {
        List<MesPqcInspectionTaskDO> tasks = new ArrayList<>();
        for (MesProScheduleOrderProcessDO process : enabledProcesses) {
            MesQaInspectionRegulationDO regulation = requirePublishedRegulation(activeOrder, process, productId);
            requireRegulationVersion(regulation, activeOrder.getId());
            List<MesQaInspectionRegulationItemDO> items = requireRegulationItems(regulation, activeOrder.getId());
            LocalDate businessDate = resolvePqcBusinessDate(activeOrder, process);
            tasks.add(buildPqcTask(activeOrder, process, regulation, INSPECTION_TYPE_FIRST, businessDate,
                    SHIFT_FIRST, resolveFixedInspectionQuantity(items, INSPECTION_TYPE_FIRST, activeOrder.getId())));
            tasks.add(buildPqcTask(activeOrder, process, regulation, INSPECTION_TYPE_PATROL, businessDate,
                    SHIFT_PATROL, resolvePatrolInspectionQuantity(process, items, activeOrder.getId())));
        }
        for (MesPqcInspectionTaskDO task : tasks) {
            insertPqcInspectionTask(task);
        }
    }

    private MesQaInspectionRegulationDO requirePublishedRegulation(MesProcessPoolActiveOrderDO activeOrder,
                                                                   MesProScheduleOrderProcessDO process,
                                                                   Long productId) {
        MesQaInspectionRegulationDO regulation = inspectionRegulationMapper.selectPublishedByRouteProcess(productId,
                activeOrder.getRouteId(), activeOrder.getRouteVersionId(), process.getRouteProcessId(),
                process.getProcessId());
        if (regulation == null || regulation.getCurrentVersionId() == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "缺少已发布QA规程，activeOrderId=" + activeOrder.getId()
                            + "，routeProcessId=" + process.getRouteProcessId()
                            + "，processId=" + process.getProcessId());
        }
        return regulation;
    }

    private MesQaInspectionRegulationVersionDO requireRegulationVersion(MesQaInspectionRegulationDO regulation,
                                                                        Long activeOrderId) {
        MesQaInspectionRegulationVersionDO version =
                inspectionRegulationVersionMapper.selectById(regulation.getCurrentVersionId());
        if (version == null || !Objects.equals("PUBLISHED", version.getLifecycleStatus())) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "QA规程发布版本不存在或未发布，activeOrderId=" + activeOrderId
                            + "，regulationVersionId=" + regulation.getCurrentVersionId());
        }
        return version;
    }

    private List<MesQaInspectionRegulationItemDO> requireRegulationItems(MesQaInspectionRegulationDO regulation,
                                                                         Long activeOrderId) {
        List<MesQaInspectionRegulationItemDO> items =
                inspectionRegulationItemMapper.selectListByVersionId(regulation.getCurrentVersionId());
        if (items == null || items.isEmpty()) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "已发布QA规程缺少检验项目，activeOrderId=" + activeOrderId
                            + "，regulationVersionId=" + regulation.getCurrentVersionId());
        }
        return items;
    }

    private LocalDate resolvePqcBusinessDate(MesProcessPoolActiveOrderDO activeOrder,
                                             MesProScheduleOrderProcessDO process) {
        if (process.getPlanDate() != null) {
            return process.getPlanDate();
        }
        if (activeOrder.getJoinedAt() == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "活跃订单缺少实际加入时间，activeOrderId=" + activeOrder.getId());
        }
        return activeOrder.getJoinedAt().toLocalDate();
    }

    private MesPqcInspectionTaskDO buildPqcTask(MesProcessPoolActiveOrderDO activeOrder,
                                                MesProScheduleOrderProcessDO process,
                                                MesQaInspectionRegulationDO regulation,
                                                String inspectionType,
                                                LocalDate businessDate,
                                                String shiftCode,
                                                Integer plannedInspectionQuantity) {
        return MesPqcInspectionTaskDO.builder()
                .activeOrderId(activeOrder.getId())
                .workOrderId(activeOrder.getWorkOrderId())
                .routeId(activeOrder.getRouteId())
                .routeVersionId(activeOrder.getRouteVersionId())
                .routeProcessId(process.getRouteProcessId())
                .processId(process.getProcessId())
                .regulationVersionId(regulation.getCurrentVersionId())
                .inspectionType(inspectionType)
                .businessDate(businessDate)
                .shiftCode(shiftCode)
                .roundNo(DEFAULT_ROUND_NO)
                .plannedInspectionQuantity(plannedInspectionQuantity)
                .actualInspectionQuantity(0)
                .taskStatus(PQC_STATUS_PENDING)
                .build();
    }

    private void insertPqcInspectionTask(MesPqcInspectionTaskDO task) {
        MesPqcInspectionTaskDO existing = pqcInspectionTaskMapper.selectByIdentity(task.getActiveOrderId(),
                task.getRouteProcessId(), task.getInspectionType(), task.getBusinessDate(), task.getShiftCode(),
                task.getRoundNo());
        if (existing != null) {
            throw exception(PRO_PQC_INSPECTION_TASK_IDENTITY_CONFLICT, identityText(task));
        }
        try {
            int inserted = pqcInspectionTaskMapper.insert(task);
            if (inserted != 1) {
                throw new IllegalStateException("Failed to insert PQC inspection task: " + identityText(task));
            }
        } catch (DuplicateKeyException ex) {
            throw exception(PRO_PQC_INSPECTION_TASK_IDENTITY_CONFLICT, identityText(task));
        }
    }

    private Integer resolveFixedInspectionQuantity(List<MesQaInspectionRegulationItemDO> items,
                                                   String inspectionType,
                                                   Long activeOrderId) {
        Integer quantity = null;
        for (MesQaInspectionRegulationItemDO item : items) {
            if (!Objects.equals(inspectionType, normalizeInspectionType(item.getInspectionType()))) {
                continue;
            }
            Integer itemQuantity = item.getFirstInspectionQuantity();
            if (itemQuantity == null || itemQuantity <= 0) {
                throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                        "固定检验数量无效，activeOrderId=" + activeOrderId
                                + "，inspectionType=" + inspectionType);
            }
            if (quantity != null && !Objects.equals(quantity, itemQuantity)) {
                throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                        "同一检验类型存在不同固定数量，activeOrderId=" + activeOrderId
                                + "，inspectionType=" + inspectionType);
            }
            quantity = itemQuantity;
        }
        if (quantity == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "缺少检验类型规则，activeOrderId=" + activeOrderId
                            + "，inspectionType=" + inspectionType);
        }
        return quantity;
    }

    private Integer resolvePatrolInspectionQuantity(MesProScheduleOrderProcessDO process,
                                                    List<MesQaInspectionRegulationItemDO> items,
                                                    Long activeOrderId) {
        BigDecimal ratio = null;
        Integer fixedQuantity = null;
        for (MesQaInspectionRegulationItemDO item : items) {
            if (!Objects.equals(INSPECTION_TYPE_PATROL, normalizeInspectionType(item.getInspectionType()))) {
                continue;
            }
            if (positive(item.getPatrolInspectionRatio())) {
                if (fixedQuantity != null) {
                    throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                            "巡检规则同时存在固定数量和比例，activeOrderId=" + activeOrderId);
                }
                if (ratio != null && ratio.compareTo(item.getPatrolInspectionRatio()) != 0) {
                    throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                            "同一巡检规则存在不同比例，activeOrderId=" + activeOrderId);
                }
                ratio = item.getPatrolInspectionRatio();
                continue;
            }
            Integer itemQuantity = item.getFirstInspectionQuantity();
            if (itemQuantity == null || itemQuantity <= 0) {
                throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                        "巡检数量规则无效，activeOrderId=" + activeOrderId);
            }
            if (ratio != null) {
                throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                        "巡检规则同时存在固定数量和比例，activeOrderId=" + activeOrderId);
            }
            if (fixedQuantity != null && !Objects.equals(fixedQuantity, itemQuantity)) {
                throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                        "同一巡检规则存在不同固定数量，activeOrderId=" + activeOrderId);
            }
            fixedQuantity = itemQuantity;
        }
        if (ratio != null) {
            return ceilPatrolInspectionQuantity(process.getPlannedQuantity(), ratio, activeOrderId);
        }
        if (fixedQuantity != null) {
            return fixedQuantity;
        }
        throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                "缺少巡检规则，activeOrderId=" + activeOrderId);
    }

    private Integer ceilPatrolInspectionQuantity(BigDecimal plannedQuantity, BigDecimal ratio, Long activeOrderId) {
        if (plannedQuantity == null || plannedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "排产工序计划数量无效，activeOrderId=" + activeOrderId);
        }
        try {
            return calculatePatrolInspectionQuantity(plannedQuantity, ratio);
        } catch (ArithmeticException ex) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "巡检计划数量超出整数范围，activeOrderId=" + activeOrderId);
        }
    }

    private static Integer calculatePatrolInspectionQuantity(BigDecimal plannedQuantity, BigDecimal ratio) {
        return plannedQuantity.multiply(ratio)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
                .intValueExact();
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String normalizeInspectionType(String inspectionType) {
        if (inspectionType == null) {
            return null;
        }
        String trimmed = inspectionType.trim();
        return trimmed.startsWith(INSPECTION_TYPE_PATROL) ? INSPECTION_TYPE_PATROL : trimmed;
    }

    private static String identityText(MesPqcInspectionTaskDO task) {
        return "activeOrderId=" + task.getActiveOrderId()
                + "，routeProcessId=" + task.getRouteProcessId()
                + "，inspectionType=" + task.getInspectionType()
                + "，businessDate=" + task.getBusinessDate()
                + "，shiftCode=" + task.getShiftCode()
                + "，roundNo=" + task.getRoundNo();
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO toProcessSnapshot(MesProcessPoolActiveOrderDO activeOrder,
                                                                         MesProScheduleOrderProcessDO process,
                                                                         BigDecimal erpFixedQuantity) {
        if (process == null || process.getRouteProcessId() == null || process.getProcessId() == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        BigDecimal factor = requirePositive(productionQuantityFactorOrDefault(
                        process.getProductionQuantityFactor()), activeOrder.getId())
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal plannedQuantity = process.getPlannedQuantity() == null
                ? erpFixedQuantity.multiply(factor).setScale(6, RoundingMode.HALF_UP)
                : requireNonNegative(process.getPlannedQuantity(), activeOrder.getId())
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal expectedPlannedQuantity = erpFixedQuantity.multiply(factor).setScale(6, RoundingMode.HALF_UP);
        if (plannedQuantity.compareTo(expectedPlannedQuantity) != 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(activeOrder.getId())
                .workOrderId(activeOrder.getWorkOrderId())
                .routeId(activeOrder.getRouteId())
                .routeVersionId(activeOrder.getRouteVersionId())
                .routeProcessId(process.getRouteProcessId())
                .processId(process.getProcessId())
                .erpFixedQuantitySnapshot(erpFixedQuantity.setScale(6, RoundingMode.HALF_UP))
                .productionQuantityFactorSnapshot(factor)
                .plannedQuantitySnapshot(plannedQuantity)
                .build();
    }

    private static BigDecimal productionQuantityFactorOrDefault(BigDecimal value) {
        return value == null ? DEFAULT_PRODUCTION_QUANTITY_FACTOR : value;
    }

    private static BigDecimal requirePositive(BigDecimal value, Long activeOrderId) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return value;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, Long activeOrderId) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return value;
    }

    private static BigDecimal activeOrderQuantitySnapshot(MesProWorkOrderDO workOrder) {
        return workOrder.getQuantity() == null ? BigDecimal.ZERO : workOrder.getQuantity();
    }

    private record ActiveOrderProgress(BigDecimal productionProgressPercent, BigDecimal inspectionProgressPercent) {
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record WorkOrderProcessIdentity(Long workOrderId, Long routeProcessId, Long processId) {

        private boolean complete() {
            return workOrderId != null && routeProcessId != null && processId != null;
        }
    }

    private record ActiveOrderProcessIdentity(Long activeOrderId, Long routeProcessId, Long processId) {

        private boolean complete() {
            return activeOrderId != null && routeProcessId != null && processId != null;
        }
    }

    private record CandidateEligibility(boolean eligible, String ineligibleReason) {
    }

    private record ActiveOrderRouteSource(Long routeId, Long routeVersionId, Long productId,
                                          List<MesProScheduleOrderProcessDO> enabledProcesses) {
    }

    private record RouteSourceResolution(ActiveOrderRouteSource source, String ineligibleReason,
                                         RouteSourceFailureType failureType) {
    }

    private enum RouteSourceFailureType {
        QA
    }

    private record CandidateRegulationKey(Long productId, Long routeId, Long routeVersionId, Long routeProcessId,
                                           Long processId) {
    }

    private record QaRouteVersionKey(Long routeId, Long routeVersionId) {
    }

    private record CandidateEligibilityContext(
            Map<CandidateRegulationKey, List<MesQaInspectionRegulationDO>> regulationsByKey,
            Map<Long, MesQaInspectionRegulationVersionDO> versionsById,
            Map<Long, List<MesQaInspectionRegulationItemDO>> itemsByVersionId) {
    }
}
