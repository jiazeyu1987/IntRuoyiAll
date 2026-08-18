package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesRouteDccProductMasterInvariant;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_MOVE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_HISTORY_AMBIGUOUS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderAddResult.ACTION_ADD;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderAddResult.ACTION_RECOVER;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderAddResult.ACTION_REUSE;

@Service
@Validated
public class MesTeamLeaderActiveOrderServiceImpl implements MesTeamLeaderActiveOrderService {

    static final String STATUS_ACTIVE = "ACTIVE";
    static final String STATUS_REMOVED = "REMOVED";
    private static final String CANDIDATE_STATE_ADDABLE = "ADDABLE";
    private static final String CANDIDATE_STATE_REUSABLE = "REUSABLE";
    private static final String CANDIDATE_STATE_RECOVERABLE = "RECOVERABLE";
    private static final String CANDIDATE_STATE_BLOCKED = "BLOCKED";
    private static final String PQC_STATUS_PENDING = "PENDING";
    private static final String INSPECTION_TYPE_FIRST = "FIRST";
    private static final String INSPECTION_TYPE_PATROL = "PATROL";
    private static final String INSPECTION_TYPE_FINAL = "FINAL";
    private static final String RULE_KEY_FIRST = "FIRST";
    private static final String RULE_KEY_PATROL_AM = "PATROL_AM";
    private static final String RULE_KEY_PATROL_PM = "PATROL_PM";
    private static final String RULE_KEY_FINAL = "FINAL";
    private static final String SHIFT_FIRST = "FIRST";
    private static final String SHIFT_PATROL_AM = "AM";
    private static final String SHIFT_PATROL_PM = "PM";
    private static final String SHIFT_FINAL = "FINAL";
    private static final int DEFAULT_ROUND_NO = 1;
    private static final int ACTIVE_ORDER_CANDIDATE_LIMIT = 20;
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
    private final MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    private final MesProcessPoolReportAllocationMapper reportAllocationMapper;
    private final MesQaInspectionRegulationMapper inspectionRegulationMapper;
    private final MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper;
    private final MesQaInspectionRegulationProcessMapper inspectionRegulationProcessMapper;
    private final MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    private final MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    private final MesWorkOrderAbnormalStateService abnormalStateService;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    private final DccProjectCodeMapper dccProjectCodeMapper;
    private final MesReportAllocationOrderChangeService reportAllocationOrderChangeService;

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
                                               MesRouteDccProjectBindingMapper routeDccProjectBindingMapper,
                                               MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
                                               MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper,
                                               MesProcessPoolReportAllocationMapper reportAllocationMapper,
                                               MesQaInspectionRegulationMapper inspectionRegulationMapper,
                                               MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper,
                                               MesQaInspectionRegulationProcessMapper inspectionRegulationProcessMapper,
                                               MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper,
                                               MesPqcInspectionTaskMapper pqcInspectionTaskMapper,
                                               MesWorkOrderAbnormalStateService abnormalStateService,
                                               MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
                                               DccProjectCodeMapper dccProjectCodeMapper,
                                               MesReportAllocationOrderChangeService reportAllocationOrderChangeService) {
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
        this.routeDccProjectBindingMapper = routeDccProjectBindingMapper;
        this.processSnapshotMapper = processSnapshotMapper;
        this.parameterRuleMapper = parameterRuleMapper;
        this.reportAllocationMapper = reportAllocationMapper;
        this.inspectionRegulationMapper = inspectionRegulationMapper;
        this.inspectionRegulationVersionMapper = inspectionRegulationVersionMapper;
        this.inspectionRegulationProcessMapper = inspectionRegulationProcessMapper;
        this.inspectionRegulationItemMapper = inspectionRegulationItemMapper;
        this.pqcInspectionTaskMapper = pqcInspectionTaskMapper;
        this.abnormalStateService = abnormalStateService;
        this.releaseApplicationMapper = releaseApplicationMapper;
        this.dccProjectCodeMapper = dccProjectCodeMapper;
        this.reportAllocationOrderChangeService = reportAllocationOrderChangeService;
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
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectCandidatesByKeyword(searchText, productIds);
        Map<Long, List<MesProcessPoolActiveOrderDO>> historyByWorkOrderId = loadCandidateHistory(workOrders);
        List<MesProWorkOrderDO> workOrdersWithoutHistory = workOrders.stream()
                .filter(workOrder -> workOrder.getId() != null)
                .filter(workOrder -> historyByWorkOrderId.getOrDefault(workOrder.getId(), List.of()).isEmpty())
                .toList();
        CandidateEligibilityContext context = buildCandidateEligibilityContext(workOrdersWithoutHistory);
        return workOrders.stream()
                .map(workOrder -> toActiveOrderCandidate(workOrder, context, historyByWorkOrderId))
                .sorted((left, right) -> Boolean.compare(right.isEligible(), left.isEligible()))
                .limit(ACTIVE_ORDER_CANDIDATE_LIMIT)
                .toList();
    }

    private Map<Long, List<MesProcessPoolActiveOrderDO>> loadCandidateHistory(List<MesProWorkOrderDO> workOrders) {
        if (workOrders == null || workOrders.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> workOrderIds = workOrders.stream()
                .map(MesProWorkOrderDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (workOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return activeOrderMapper.selectHistoryByWorkOrderIds(workOrderIds).stream()
                .filter(history -> history.getWorkOrderId() != null)
                .collect(Collectors.groupingBy(MesProcessPoolActiveOrderDO::getWorkOrderId,
                        LinkedHashMap::new, Collectors.toList()));
    }

    private CandidateEligibilityContext buildCandidateEligibilityContext(List<MesProWorkOrderDO> workOrders) {
        return buildCandidateEligibilityContext(workOrders, true);
    }

    private CandidateEligibilityContext buildRouteSourceContext(List<MesProWorkOrderDO> workOrders) {
        return buildCandidateEligibilityContext(workOrders, false);
    }

    private CandidateEligibilityContext buildCandidateEligibilityContext(List<MesProWorkOrderDO> workOrders,
                                                                          boolean includeQaEligibility) {
        if (workOrders == null || workOrders.isEmpty()) {
            return emptyCandidateEligibilityContext();
        }
        List<Long> workOrderProductIds = workOrders.stream()
                .filter(workOrder -> !isCancelledWorkOrder(workOrder))
                .map(MesProWorkOrderDO::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (workOrderProductIds.isEmpty()) {
            return emptyCandidateEligibilityContext();
        }
        List<MesProRouteProductDO> workOrderRouteBindings = routeProductMapper
                .selectListByItemIds(workOrderProductIds);
        Map<Long, List<MesProRouteProductDO>> routeBindingsByProductId = workOrderRouteBindings.stream()
                .filter(binding -> binding.getItemId() != null)
                .filter(binding -> binding.getRouteId() != null)
                .collect(Collectors.groupingBy(MesProRouteProductDO::getItemId));
        List<Long> routeIds = workOrderRouteBindings.stream()
                .map(MesProRouteProductDO::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (routeIds.isEmpty()) {
            return new CandidateEligibilityContext(routeBindingsByProductId, Collections.emptyMap(),
                    Collections.emptySet(), Collections.emptyMap());
        }
        Set<Long> existingRouteIds = routeMapper.selectBatchIds(routeIds).stream()
                .map(MesProRouteDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Long> validRouteIds = routeIds.stream()
                .filter(existingRouteIds::contains)
                .toList();
        if (validRouteIds.isEmpty()) {
            return new CandidateEligibilityContext(routeBindingsByProductId, Collections.emptyMap(),
                    existingRouteIds, Collections.emptyMap());
        }
        Map<Long, List<MesProRouteVersionDO>> activeRouteVersionsByRouteId = routeVersionMapper
                .selectListByRouteIds(validRouteIds).stream()
                .filter(version -> Boolean.TRUE.equals(version.getActive()))
                .filter(version -> Objects.equals("ACTIVE", version.getLifecycleStatus()))
                .filter(version -> version.getRouteId() != null)
                .filter(version -> version.getId() != null)
                .collect(Collectors.groupingBy(MesProRouteVersionDO::getRouteId));
        Map<Long, CandidateEligibility> qaEligibility = includeQaEligibility
                ? buildRouteQaEligibility(validRouteIds) : Collections.emptyMap();
        return new CandidateEligibilityContext(routeBindingsByProductId, activeRouteVersionsByRouteId,
                existingRouteIds, qaEligibility);
    }

    private static RouteSourceResolution routeSourceFailure(String reason, RouteSourceFailureType failureType) {
        return new RouteSourceResolution(null, reason, failureType);
    }

    private static CandidateEligibilityContext emptyCandidateEligibilityContext() {
        return new CandidateEligibilityContext(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(),
                Collections.emptyMap());
    }

    private Map<Long, CandidateEligibility> buildRouteQaEligibility(Collection<Long> routeIds) {
        Map<Long, CandidateEligibility> eligibilityByRouteId = new LinkedHashMap<>();
        Map<Long, Long> dccProjectIdByRouteId = new LinkedHashMap<>();
        Map<Long, DccProjectCodeDO> enabledProjectById = new LinkedHashMap<>();
        for (Long routeId : routeIds) {
            MesRouteDccProjectBindingDO binding = routeDccProjectBindingMapper.selectCurrentByRouteId(routeId);
            if (binding == null || binding.getDccProjectCodeId() == null) {
                eligibilityByRouteId.put(routeId, blockedCandidate("工艺路线缺少 DCC 项目代码绑定"));
                continue;
            }
            DccProjectCodeDO project = dccProjectCodeMapper.selectById(binding.getDccProjectCodeId());
            if (project == null || !DccProjectCodeStatusConstants.ENABLE.equals(project.getStatus())) {
                eligibilityByRouteId.put(routeId, blockedCandidate("DCC 项目代码不存在或已停用"));
                continue;
            }
            try {
                requireMatchingCurrentRouteProductMaster(routeId, project);
            } catch (ServiceException ex) {
                eligibilityByRouteId.put(routeId, blockedCandidate(ex.getMessage()));
                continue;
            }
            dccProjectIdByRouteId.put(routeId, project.getId());
            enabledProjectById.put(project.getId(), project);
        }
        if (enabledProjectById.isEmpty()) {
            return eligibilityByRouteId;
        }
        List<Long> dccProjectIds = List.copyOf(enabledProjectById.keySet());
        Map<Long, List<MesQaInspectionRegulationDO>> regulationsByDccProjectId = inspectionRegulationMapper
                .selectListByDccProjectCodeIds(dccProjectIds).stream()
                .filter(regulation -> regulation.getDccProjectCodeId() != null)
                .collect(Collectors.groupingBy(MesQaInspectionRegulationDO::getDccProjectCodeId));
        List<Long> currentVersionIds = regulationsByDccProjectId.values().stream()
                .filter(regulations -> regulations.size() == 1)
                .map(regulations -> regulations.get(0).getCurrentVersionId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesQaInspectionRegulationVersionDO> versionById = currentVersionIds.isEmpty()
                ? Collections.emptyMap()
                : inspectionRegulationVersionMapper.selectBatchIds(currentVersionIds).stream()
                .filter(version -> version.getId() != null)
                .collect(Collectors.toMap(MesQaInspectionRegulationVersionDO::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        for (Map.Entry<Long, Long> entry : dccProjectIdByRouteId.entrySet()) {
            Long routeId = entry.getKey();
            List<MesQaInspectionRegulationDO> regulations = regulationsByDccProjectId
                    .getOrDefault(entry.getValue(), Collections.emptyList());
            if (regulations.size() != 1) {
                eligibilityByRouteId.put(routeId, blockedCandidate("DCC 项目代码缺少唯一 QA 规程"));
                continue;
            }
            MesQaInspectionRegulationDO regulation = regulations.get(0);
            MesQaInspectionRegulationVersionDO version = versionById.get(regulation.getCurrentVersionId());
            if (!isCurrentPublishedVersion(regulation, version)) {
                eligibilityByRouteId.put(routeId, blockedCandidate("QA 规程缺少当前正式发布版本"));
                continue;
            }
            eligibilityByRouteId.put(routeId, eligibleCandidate(CANDIDATE_STATE_ADDABLE));
        }
        return eligibilityByRouteId;
    }

    private static boolean isCurrentPublishedVersion(MesQaInspectionRegulationDO regulation,
                                                     MesQaInspectionRegulationVersionDO version) {
        return regulation != null && version != null
                && MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA.equals(regulation.getOwnerModule())
                && Objects.equals(regulation.getCurrentVersionId(), version.getId())
                && Objects.equals(regulation.getId(), version.getRegulationId())
                && Objects.equals("PUBLISHED", version.getLifecycleStatus());
    }

    private Map<Long, List<DccProjectCodeDO>> resolveDccProjectsByRouteId(Collection<Long> routeIds) {
        Map<Long, List<DccProjectCodeDO>> result = new LinkedHashMap<>();
        for (Long routeId : routeIds) {
            MesRouteDccProjectBindingDO binding = routeDccProjectBindingMapper.selectCurrentByRouteId(routeId);
            if (binding == null || binding.getDccProjectCodeId() == null) {
                result.put(routeId, List.of());
                continue;
            }
            DccProjectCodeDO project = dccProjectCodeMapper.selectById(binding.getDccProjectCodeId());
            if (project == null || !DccProjectCodeStatusConstants.ENABLE.equals(project.getStatus())) {
                result.put(routeId, List.of());
                continue;
            }
            result.put(routeId, List.of(project));
        }
        return result;
    }

    private static boolean isCancelledWorkOrder(MesProWorkOrderDO workOrder) {
        return workOrder != null && Objects.equals(workOrder.getStatus(),
                MesProWorkOrderStatusEnum.CANCELED.getStatus());
    }

    private MesTeamLeaderActiveOrderCandidateBO toActiveOrderCandidate(MesProWorkOrderDO workOrder,
                                                                       CandidateEligibilityContext context,
                                                                       Map<Long, List<MesProcessPoolActiveOrderDO>> historyByWorkOrderId) {
        CandidateEligibility eligibility = evaluateCandidateEligibility(workOrder, context, historyByWorkOrderId);
        return MesTeamLeaderActiveOrderCandidateBO.builder()
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .candidateState(eligibility.candidateState())
                .eligible(eligibility.eligible())
                .ineligibleReason(eligibility.ineligibleReason())
                .build();
    }

    private CandidateEligibility evaluateCandidateEligibility(MesProWorkOrderDO workOrder,
                                                               CandidateEligibilityContext context,
                                                               Map<Long, List<MesProcessPoolActiveOrderDO>> historyByWorkOrderId) {
        if (workOrder == null || workOrder.getId() == null) {
            return blockedCandidate("缺少生产工单");
        }
        List<MesProcessPoolActiveOrderDO> history = historyByWorkOrderId
                .getOrDefault(workOrder.getId(), Collections.emptyList());
        if (!history.isEmpty()) {
            return evaluateCandidateHistory(workOrder.getId(), history);
        }
        RouteSourceResolution resolution = resolveProductionRouteSource(workOrder, context);
        if (resolution.source() == null) {
            return blockedCandidate(resolution.ineligibleReason());
        }
        return context.qaEligibilityByRouteId().getOrDefault(resolution.source().routeId(),
                blockedCandidate("工艺路线缺少 DCC/QA 正式配置"));
    }

    private CandidateEligibility evaluateCandidateHistory(Long workOrderId,
                                                           List<MesProcessPoolActiveOrderDO> history) {
        if (history.size() > 1) {
            return blockedCandidate(exception(PRO_PROCESS_POOL_ACTIVE_ORDER_HISTORY_AMBIGUOUS, workOrderId,
                    history.stream().map(MesProcessPoolActiveOrderDO::getId).toList()).getMessage());
        }
        MesProcessPoolActiveOrderDO historicalOrder = history.get(0);
        if (STATUS_ACTIVE.equals(historicalOrder.getActiveStatus())) {
            return eligibleCandidate(CANDIDATE_STATE_REUSABLE);
        }
        if (STATUS_REMOVED.equals(historicalOrder.getActiveStatus())) {
            try {
                validateRemovedFrozenOrder(historicalOrder);
            } catch (ServiceException ex) {
                return blockedCandidate(ex.getMessage());
            }
            return eligibleCandidate(CANDIDATE_STATE_RECOVERABLE);
        }
        return blockedCandidate("活跃订单历史状态无效：activeOrderId=" + historicalOrder.getId()
                + "，activeStatus=" + historicalOrder.getActiveStatus());
    }

    private RouteSourceResolution resolveProductionRouteSource(MesProWorkOrderDO workOrder,
                                                               CandidateEligibilityContext context) {
        if (isCancelledWorkOrder(workOrder)) {
            return routeSourceFailure("生产工单已取消", RouteSourceFailureType.WORK_ORDER);
        }
        if (workOrder.getProductId() == null) {
            return routeSourceFailure("缺少产品ID", RouteSourceFailureType.ROUTE);
        }
        List<Long> boundRouteIds = context.routeBindingsByProductId()
                .getOrDefault(workOrder.getProductId(), Collections.emptyList()).stream()
                .map(MesProRouteProductDO::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (boundRouteIds.isEmpty()) {
            return routeSourceFailure("缺少产品工艺路线绑定", RouteSourceFailureType.ROUTE);
        }
        List<Long> routeIds = boundRouteIds.stream()
                .filter(context.existingRouteIds()::contains)
                .toList();
        if (routeIds.isEmpty()) {
            return routeSourceFailure("产品工艺路线绑定指向已删除路线", RouteSourceFailureType.ROUTE);
        }
        if (routeIds.size() > 1) {
            return routeSourceFailure("产品工艺路线绑定不唯一", RouteSourceFailureType.ROUTE);
        }
        Long routeId = routeIds.get(0);
        List<MesProRouteVersionDO> activeRouteVersions = context.activeRouteVersionsByRouteId()
                .getOrDefault(routeId, Collections.emptyList());
        if (activeRouteVersions.isEmpty()) {
            return routeSourceFailure("工艺路线缺少ACTIVE版本", RouteSourceFailureType.ROUTE);
        }
        if (activeRouteVersions.size() > 1) {
            return routeSourceFailure("工艺路线ACTIVE版本不唯一", RouteSourceFailureType.ROUTE);
        }
        MesProRouteVersionDO activeRouteVersion = activeRouteVersions.get(0);
        List<MesProScheduleOrderProcessDO> routeProcesses = toRouteProcessSources(workOrder, activeRouteVersion);
        if (routeProcesses.isEmpty()) {
            return routeSourceFailure("工艺路线快照缺少正式工序", RouteSourceFailureType.ROUTE);
        }
        return new RouteSourceResolution(new ActiveOrderRouteSource(routeId,
                activeRouteVersion.getId(), routeProcesses), null, null);
    }

    private List<MesProScheduleOrderProcessDO> toRouteProcessSources(MesProWorkOrderDO workOrder,
                                                                     MesProRouteVersionDO routeVersion) {
        if (routeVersion == null || routeVersion.getRouteSnapshotJson() == null
                || routeVersion.getRouteSnapshotJson().isBlank()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED,
                    workOrder == null ? null : workOrder.getId());
        }
        JSONObject root = JSON.parseObject(routeVersion.getRouteSnapshotJson());
        JSONObject configSnapshots = root == null ? null : root.getJSONObject("configSnapshots");
        JSONObject flowGraph = configSnapshots == null ? null : configSnapshots.getJSONObject("flowGraph");
        JSONArray nodes = flowGraph == null ? null : flowGraph.getJSONArray("nodes");
        if (nodes == null || nodes.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED,
                    workOrder == null ? null : workOrder.getId());
        }
        Map<Long, JSONObject> scheduleConfigByRouteProcessId = scheduleConfigByRouteProcessId(configSnapshots);
        BigDecimal quantity = activeOrderQuantitySnapshot(workOrder).setScale(6, RoundingMode.HALF_UP);
        List<MesProScheduleOrderProcessDO> processes = new ArrayList<>();
        Set<ProcessIdentity> processIdentities = new LinkedHashSet<>();
        for (int index = 0; index < nodes.size(); index++) {
            JSONObject node = nodes.getJSONObject(index);
            Long routeProcessId = node == null ? null : node.getLong("routeProcessId");
            Long processId = node == null ? null : node.getLong("processId");
            if (routeProcessId == null || processId == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED,
                        workOrder == null ? null : workOrder.getId());
            }
            ProcessIdentity identity = new ProcessIdentity(routeProcessId, processId);
            if (!processIdentities.add(identity)) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED,
                        workOrder == null ? null : workOrder.getId());
            }
            JSONObject scheduleConfig = scheduleConfigByRouteProcessId.get(routeProcessId);
            BigDecimal factor = scheduleConfig == null || scheduleConfig.getBigDecimal("productionQuantityFactor") == null
                    ? DEFAULT_PRODUCTION_QUANTITY_FACTOR
                    : scheduleConfig.getBigDecimal("productionQuantityFactor");
            processes.add(MesProScheduleOrderProcessDO.builder()
                    .routeProcessId(routeProcessId)
                    .routeVersionId(routeVersion.getId())
                    .processId(processId)
                    .enabled(Boolean.TRUE)
                    .productionQuantityFactor(factor)
                    .plannedQuantity(quantity.multiply(productionQuantityFactorOrDefault(factor))
                            .setScale(6, RoundingMode.HALF_UP))
                    .build());
        }
        return processes;
    }

    private Map<Long, JSONObject> scheduleConfigByRouteProcessId(JSONObject configSnapshots) {
        JSONArray scheduleUseConfigs = configSnapshots == null ? null : configSnapshots.getJSONArray("scheduleUseConfigs");
        if (scheduleUseConfigs == null || scheduleUseConfigs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, JSONObject> result = new LinkedHashMap<>();
        for (int index = 0; index < scheduleUseConfigs.size(); index++) {
            JSONObject config = scheduleUseConfigs.getJSONObject(index);
            Long routeProcessId = config == null ? null : config.getLong("routeProcessId");
            if (routeProcessId == null || Boolean.FALSE.equals(config.getBoolean("enabled"))) {
                continue;
            }
            result.put(routeProcessId, config);
        }
        return result;
    }

    private static List<PqcInspectionRule> parseCanonicalInspectionRules(MesQaInspectionRegulationVersionDO version) {
        if (version == null || version.getInspectionTypeRulesJson() == null
                || version.getInspectionTypeRulesJson().isBlank()) {
            throw new IllegalArgumentException("QA规程缺少检验类型规则");
        }
        JSONArray rulesJson;
        try {
            rulesJson = JSON.parseArray(version.getInspectionTypeRulesJson());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("QA规程检验类型规则无效");
        }
        if (rulesJson == null || rulesJson.isEmpty()) {
            throw new IllegalArgumentException("QA规程缺少检验类型规则");
        }
        List<PqcInspectionRule> rules = new ArrayList<>();
        Set<String> ruleKeys = new LinkedHashSet<>();
        for (int index = 0; index < rulesJson.size(); index++) {
            JSONObject ruleJson = rulesJson.getJSONObject(index);
            String ruleKey = normalizeRuleKey(ruleJson == null ? null : ruleJson.getString("key"));
            String inspectionType = normalizeInspectionType(ruleJson == null ? null : ruleJson.getString("inspectionType"));
            Boolean required = ruleJson == null ? null : ruleJson.getBoolean("required");
            if (ruleKey == null || inspectionType == null || required == null || !ruleKeys.add(ruleKey)
                    || !Objects.equals(expectedInspectionType(ruleKey), inspectionType)) {
                throw new IllegalArgumentException("QA规程检验类型规则无效");
            }
            rules.add(new PqcInspectionRule(ruleKey, inspectionType, shiftCodeForRuleKey(ruleKey),
                    Boolean.TRUE.equals(required)));
        }
        Set<String> expectedRuleKeys = Set.of(RULE_KEY_FIRST, RULE_KEY_PATROL_AM, RULE_KEY_PATROL_PM, RULE_KEY_FINAL);
        if (!ruleKeys.equals(expectedRuleKeys)) {
            throw new IllegalArgumentException("QA规程检验类型规则无效");
        }
        return rules.stream()
                .sorted(Comparator.comparingInt(rule -> inspectionRuleOrder(rule.ruleKey())))
                .toList();
    }

    private static String validateInspectionRuleTruthTable(List<PqcInspectionRule> rules,
                                                           List<MesQaInspectionRegulationItemDO> items) {
        if (rules == null || rules.isEmpty()) {
            return "QA规程缺少检验类型规则";
        }
        if (rules.stream().noneMatch(PqcInspectionRule::required)) {
            return "QA规程缺少启用检验类型规则";
        }
        Set<String> itemInspectionTypes = new LinkedHashSet<>();
        for (MesQaInspectionRegulationItemDO item : items) {
            String inspectionType = normalizeInspectionType(item.getInspectionType());
            if (inspectionType == null) {
                return "QA检验类型无效";
            }
            itemInspectionTypes.add(inspectionType);
        }
        for (String inspectionType : itemInspectionTypes) {
            List<PqcInspectionRule> typeRules = rules.stream()
                    .filter(rule -> Objects.equals(inspectionType, rule.inspectionType()))
                    .toList();
            if (typeRules.isEmpty()) {
                return "QA检验类型无效";
            }
            if (typeRules.stream().noneMatch(PqcInspectionRule::required)) {
                return "QA规程缺少启用检验类型规则";
            }
        }
        for (PqcInspectionRule rule : rules) {
            if (!rule.required()) {
                continue;
            }
            boolean hasItems = items.stream()
                    .anyMatch(item -> Objects.equals(rule.inspectionType(),
                            normalizeInspectionType(item.getInspectionType())));
            if (!hasItems) {
                return "QA检验规则缺少对应检验项目";
            }
        }
        return null;
    }

    private String validateRuleInspectionQuantity(BigDecimal plannedQuantity,
                                                  List<MesQaInspectionRegulationItemDO> items,
                                                  PqcInspectionRule rule) {
        if (Objects.equals(INSPECTION_TYPE_PATROL, rule.inspectionType())) {
            return validatePatrolInspectionQuantity(plannedQuantity, items);
        }
        if (Objects.equals(INSPECTION_TYPE_FIRST, rule.inspectionType())
                || Objects.equals(INSPECTION_TYPE_FINAL, rule.inspectionType())) {
            return validateFixedInspectionQuantity(items, rule.inspectionType());
        }
        return "QA检验类型无效";
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

    private String validatePatrolInspectionQuantity(BigDecimal plannedQuantity,
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
            if (plannedQuantity == null || plannedQuantity.compareTo(BigDecimal.ZERO) < 0) {
                return "订单计划数量无效";
            }
            try {
                calculatePatrolInspectionQuantity(plannedQuantity, ratio);
            } catch (ArithmeticException ex) {
                return "巡检计划数量超出整数范围";
            }
            return null;
        }
        return fixedQuantity == null ? "缺少巡检规则" : null;
    }

    private CandidateEligibility blockedCandidate(String reason) {
        return new CandidateEligibility(false, CANDIDATE_STATE_BLOCKED, reason);
    }

    private CandidateEligibility eligibleCandidate(String candidateState) {
        return new CandidateEligibility(true, candidateState, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderAddResult addActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getWorkOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrder");
        }
        MesProWorkOrderDO workOrder = workOrderService.validateWorkOrderExists(reqBO.getWorkOrderId());
        MesTeamLeaderActiveOrderAddResult historicalResult = resolveActiveOrderHistory(reqBO);
        if (historicalResult != null) {
            return historicalResult;
        }
        BigDecimal erpFixedQuantity = activeOrderQuantitySnapshot(workOrder);
        ActiveOrderRouteSource routeSource = requireProductionRouteSourceForAdd(workOrder);
        Long routeId = routeSource.routeId();
        Long routeVersionId = routeSource.routeVersionId();
        MesProcessPoolActiveOrderDO existing = selectExistingActiveOrder(reqBO.getWorkOrderId(), routeId,
                routeVersionId);
        if (existing != null) {
            return addResult(existing.getId(), ACTION_REUSE);
        }
        ActiveOrderQaSource qaSource = requireCurrentQaSource(routeId, reqBO.getWorkOrderId());
        LocalDateTime joinedAt = LocalDateTime.now();
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .workOrderId(reqBO.getWorkOrderId())
                .routeId(routeId)
                .routeVersionId(routeVersionId)
                .dccProjectCodeId(qaSource.dccProjectCodeId())
                .qaRegulationId(qaSource.regulation().getId())
                .qaRegulationVersionId(qaSource.version().getId())
                .erpFixedQuantitySnapshot(erpFixedQuantity)
                .activeStatus(STATUS_ACTIVE)
                .businessStatus(STATUS_ACTIVE)
                .joinedAt(joinedAt)
                .sortOrder(nextSortOrderForLeader(reqBO.getLeaderUserId()))
                .version(0)
                .build();
        List<PlannedPqcTask> pqcTaskPlan = preparePqcTaskPlan(activeOrder, qaSource);
        try {
            activeOrderMapper.insert(activeOrder);
        } catch (DuplicateKeyException ex) {
            MesProcessPoolActiveOrderDO concurrentlyAdded = selectExistingActiveOrder(reqBO.getWorkOrderId(), routeId,
                    routeVersionId);
            if (concurrentlyAdded != null) {
                return addResult(concurrentlyAdded.getId(), ACTION_REUSE);
            }
            MesTeamLeaderActiveOrderAddResult concurrentlyResolved = resolveActiveOrderHistory(reqBO);
            if (concurrentlyResolved != null) {
                return concurrentlyResolved;
            }
            throw ex;
        }
        insertProcessSnapshots(activeOrder, erpFixedQuantity, routeSource.routeProcesses());
        insertPqcInspectionTasks(activeOrder, qaSource, pqcTaskPlan);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "ADD_ACTIVE_ORDER",
                "ACTIVE_ORDER", activeOrder.getId(), null, activeOrder.toString());
        return addResult(activeOrder.getId(), ACTION_ADD);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getActiveOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "removeActiveOrder");
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(reqBO.getActiveOrderId());
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), reqBO.getLeaderUserId())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, reqBO.getActiveOrderId());
        }
        reportAllocationOrderChangeService.invalidateActiveOrder(activeOrder.getId(), reqBO.getLeaderUserId(),
                "活跃订单移除");
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveActiveOrder(MesTeamLeaderActiveOrderMoveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getActiveOrderId() == null
                || (!"UP".equals(reqBO.getDirection()) && !"DOWN".equals(reqBO.getDirection()))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "moveActiveOrder");
        }
        List<MesProcessPoolActiveOrderDO> activeOrders =
                activeOrderMapper.selectActiveListByLeaderForUpdate(reqBO.getLeaderUserId());
        int targetIndex = -1;
        for (int index = 0; index < activeOrders.size(); index++) {
            if (Objects.equals(activeOrders.get(index).getId(), reqBO.getActiveOrderId())) {
                targetIndex = index;
                break;
            }
        }
        if (targetIndex < 0) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, reqBO.getActiveOrderId());
        }
        int adjacentIndex = "UP".equals(reqBO.getDirection()) ? targetIndex - 1 : targetIndex + 1;
        if (adjacentIndex < 0 || adjacentIndex >= activeOrders.size()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_MOVE_INVALID,
                    "UP".equals(reqBO.getDirection()) ? "当前订单已经位于首位" : "当前订单已经位于末位");
        }
        MesProcessPoolActiveOrderDO target = activeOrders.get(targetIndex);
        MesProcessPoolActiveOrderDO adjacent = activeOrders.get(adjacentIndex);
        if (target.getSortOrder() == null || adjacent.getSortOrder() == null
                || Objects.equals(target.getSortOrder(), adjacent.getSortOrder())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_MOVE_INVALID, "活跃订单排序值缺失或重复");
        }
        int updated = activeOrderMapper.swapActiveOrderSortOrders(reqBO.getLeaderUserId(),
                target.getId(), target.getSortOrder(), adjacent.getId(), adjacent.getSortOrder());
        if (updated != 2) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_MOVE_INVALID, "排序已被并发修改，请刷新后重试");
        }
        String beforeSnapshot = "targetId=" + target.getId() + ",targetSortOrder=" + target.getSortOrder()
                + ",adjacentId=" + adjacent.getId() + ",adjacentSortOrder=" + adjacent.getSortOrder();
        String afterSnapshot = "targetId=" + target.getId() + ",targetSortOrder=" + adjacent.getSortOrder()
                + ",adjacentId=" + adjacent.getId() + ",adjacentSortOrder=" + target.getSortOrder();
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "MOVE_ACTIVE_ORDER",
                "ACTIVE_ORDER", target.getId(), beforeSnapshot, afterSnapshot);
    }

    private MesProcessPoolActiveOrderDO selectExistingActiveOrder(Long workOrderId, Long routeId,
                                                                  Long routeVersionId) {
        return activeOrderMapper.selectActiveByWorkOrderRouteVersion(workOrderId, routeId, routeVersionId);
    }

    private MesTeamLeaderActiveOrderAddResult resolveActiveOrderHistory(MesTeamLeaderActiveOrderAddReqBO reqBO) {
        List<MesProcessPoolActiveOrderDO> history = activeOrderMapper
                .selectHistoryByWorkOrderIdForUpdate(reqBO.getWorkOrderId());
        if (history == null || history.isEmpty()) {
            return null;
        }
        if (history.size() > 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_HISTORY_AMBIGUOUS, reqBO.getWorkOrderId(),
                    history.stream().map(MesProcessPoolActiveOrderDO::getId).toList());
        }
        MesProcessPoolActiveOrderDO historicalOrder = history.get(0);
        if (STATUS_ACTIVE.equals(historicalOrder.getActiveStatus())) {
            return addResult(historicalOrder.getId(), ACTION_REUSE);
        }
        if (STATUS_REMOVED.equals(historicalOrder.getActiveStatus())) {
            return reactivateRemovedActiveOrder(reqBO, historicalOrder);
        }
        throw new IllegalStateException("Unexpected active order history status: "
                + historicalOrder.getActiveStatus() + ", activeOrderId=" + historicalOrder.getId());
    }

    private MesTeamLeaderActiveOrderAddResult reactivateRemovedActiveOrder(
            MesTeamLeaderActiveOrderAddReqBO reqBO, MesProcessPoolActiveOrderDO removed) {
        validateRemovedFrozenOrder(removed);
        LocalDateTime rejoinedAt = LocalDateTime.now();
        Long sortOrder = nextSortOrderForLeader(reqBO.getLeaderUserId());
        int updated = activeOrderMapper.reactivateRemovedActiveOrder(removed.getId(), reqBO.getLeaderUserId(),
                removed.getVersion(), rejoinedAt, sortOrder);
        if (updated > 0) {
            MesProcessPoolActiveOrderDO after = MesProcessPoolActiveOrderDO.builder()
                    .id(removed.getId())
                    .leaderUserId(reqBO.getLeaderUserId())
                    .workOrderId(removed.getWorkOrderId())
                    .routeId(removed.getRouteId())
                    .routeVersionId(removed.getRouteVersionId())
                    .dccProjectCodeId(removed.getDccProjectCodeId())
                    .qaRegulationId(removed.getQaRegulationId())
                    .qaRegulationVersionId(removed.getQaRegulationVersionId())
                    .erpFixedQuantitySnapshot(removed.getErpFixedQuantitySnapshot())
                    .activeStatus(STATUS_ACTIVE)
                    .businessStatus(STATUS_ACTIVE)
                    .joinedAt(rejoinedAt)
                    .sortOrder(sortOrder)
                    .version(removed.getVersion() == null ? null : removed.getVersion() + 1)
                    .build();
            TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "REACTIVATE_ACTIVE_ORDER",
                    "ACTIVE_ORDER", removed.getId(), removed.toString(), after.toString());
            return addResult(removed.getId(), ACTION_RECOVER);
        }
        MesProcessPoolActiveOrderDO concurrentlyAdded = selectExistingActiveOrder(removed.getWorkOrderId(),
                removed.getRouteId(), removed.getRouteVersionId());
        if (concurrentlyAdded != null) {
            return addResult(concurrentlyAdded.getId(), ACTION_REUSE);
        }
        throw new IllegalStateException("Failed to reactivate removed active order: " + removed.getId());
    }

    private void validateRemovedFrozenOrder(MesProcessPoolActiveOrderDO removed) {
        validateRemovedRouteAndProcessSnapshots(removed);
        ActiveOrderQaSource qaSource = validateRemovedQaLockSnapshot(removed);
        validateRemovedPqcTasks(removed, qaSource);
    }

    private void validateRemovedRouteAndProcessSnapshots(MesProcessPoolActiveOrderDO removed) {
        if (removed.getRouteId() == null || removed.getRouteVersionId() == null) {
            throw frozenOrderBlocked("removed活跃订单缺少冻结路线身份", removed.getId());
        }
        MesProRouteDO route = routeMapper.selectById(removed.getRouteId());
        if (route == null) {
            throw frozenOrderBlocked("removed活跃订单冻结路线不存在", removed.getId());
        }
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(removed.getRouteVersionId());
        if (routeVersion == null || !Objects.equals(removed.getRouteId(), routeVersion.getRouteId())) {
            throw frozenOrderBlocked("removed活跃订单冻结路线版本归属无效", removed.getId());
        }
        Set<ProcessIdentity> frozenRouteProcessIdentities = requireFrozenRouteProcessIdentities(
                routeVersion, removed.getId());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots =
                processSnapshotMapper.selectListByActiveOrderId(removed.getId());
        if (snapshots == null || snapshots.isEmpty()) {
            throw frozenOrderBlocked("removed活跃订单缺少冻结工序快照", removed.getId());
        }
        Set<ProcessIdentity> identities = new LinkedHashSet<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            if (snapshot == null || snapshot.getRouteProcessId() == null || snapshot.getProcessId() == null
                    || !Objects.equals(removed.getId(), snapshot.getActiveOrderId())
                    || !Objects.equals(removed.getWorkOrderId(), snapshot.getWorkOrderId())
                    || !Objects.equals(removed.getRouteId(), snapshot.getRouteId())
                    || !Objects.equals(removed.getRouteVersionId(), snapshot.getRouteVersionId())) {
                throw frozenOrderBlocked("removed活跃订单冻结工序快照身份无效", removed.getId());
            }
            ProcessIdentity identity = new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId());
            if (!identities.add(identity)) {
                throw frozenOrderBlocked("removed活跃订单冻结工序快照身份重复", removed.getId());
            }
            if (removed.getErpFixedQuantitySnapshot() == null
                    || snapshot.getErpFixedQuantitySnapshot() == null
                    || removed.getErpFixedQuantitySnapshot().compareTo(snapshot.getErpFixedQuantitySnapshot()) != 0
                    || !positive(snapshot.getProductionQuantityFactorSnapshot())
                    || snapshot.getPlannedQuantitySnapshot() == null
                    || snapshot.getPlannedQuantitySnapshot().compareTo(snapshot.getErpFixedQuantitySnapshot()
                    .multiply(snapshot.getProductionQuantityFactorSnapshot())) != 0) {
                throw frozenOrderBlocked("removed活跃订单冻结工序数量快照不一致", removed.getId());
            }
        }
        if (!identities.equals(frozenRouteProcessIdentities)) {
            throw frozenOrderBlocked("removed活跃订单冻结工序快照不完整", removed.getId());
        }
    }

    private Set<ProcessIdentity> requireFrozenRouteProcessIdentities(MesProRouteVersionDO routeVersion,
                                                                      Long activeOrderId) {
        JSONArray nodes;
        try {
            JSONObject root = routeVersion.getRouteSnapshotJson() == null
                    ? null : JSON.parseObject(routeVersion.getRouteSnapshotJson());
            JSONObject configSnapshots = root == null ? null : root.getJSONObject("configSnapshots");
            JSONObject flowGraph = configSnapshots == null ? null : configSnapshots.getJSONObject("flowGraph");
            nodes = flowGraph == null ? null : flowGraph.getJSONArray("nodes");
        } catch (RuntimeException ex) {
            throw frozenOrderBlocked("removed活跃订单冻结路线版本快照无效", activeOrderId);
        }
        if (nodes == null || nodes.isEmpty()) {
            throw frozenOrderBlocked("removed活跃订单冻结路线版本快照缺少正式工序", activeOrderId);
        }
        Set<ProcessIdentity> identities = new LinkedHashSet<>();
        Set<Long> routeProcessIds = new LinkedHashSet<>();
        for (int index = 0; index < nodes.size(); index++) {
            JSONObject node = nodes.getJSONObject(index);
            Long routeProcessId = node == null ? null : node.getLong("routeProcessId");
            Long processId = node == null ? null : node.getLong("processId");
            ProcessIdentity identity = new ProcessIdentity(routeProcessId, processId);
            if (routeProcessId == null || processId == null
                    || !routeProcessIds.add(routeProcessId) || !identities.add(identity)) {
                throw frozenOrderBlocked("removed活跃订单冻结路线版本工序身份无效", activeOrderId);
            }
        }
        return identities;
    }

    private ActiveOrderQaSource validateRemovedQaLockSnapshot(MesProcessPoolActiveOrderDO removed) {
        if (removed.getDccProjectCodeId() == null || removed.getQaRegulationId() == null
                || removed.getQaRegulationVersionId() == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "removed活跃订单缺少QA锁定快照，activeOrderId=" + removed.getId());
        }
        DccProjectCodeDO project = dccProjectCodeMapper.selectById(removed.getDccProjectCodeId());
        if (project == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "removed活跃订单冻结DCC不存在，activeOrderId=" + removed.getId()
                            + "，dccProjectCodeId=" + removed.getDccProjectCodeId());
        }
        MesQaInspectionRegulationDO regulation = inspectionRegulationMapper.selectById(removed.getQaRegulationId());
        if (regulation == null
                || !MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA.equals(regulation.getOwnerModule())
                || !Objects.equals(removed.getDccProjectCodeId(), regulation.getDccProjectCodeId())) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "removed活跃订单QA锁定主档无效，activeOrderId=" + removed.getId()
                            + "，qaRegulationId=" + removed.getQaRegulationId());
        }
        MesQaInspectionRegulationVersionDO version =
                inspectionRegulationVersionMapper.selectById(removed.getQaRegulationVersionId());
        if (version == null || !Objects.equals(removed.getQaRegulationId(), version.getRegulationId())
                || (!Objects.equals("PUBLISHED", version.getLifecycleStatus())
                && !Objects.equals("RETIRED", version.getLifecycleStatus()))) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "removed活跃订单QA锁定版本无效，activeOrderId=" + removed.getId()
                            + "，regulationVersionId=" + removed.getQaRegulationVersionId());
        }
        List<MesQaInspectionRegulationProcessDO> processes = inspectionRegulationProcessMapper
                .selectListByVersionIds(List.of(version.getId())).stream()
                .filter(process -> Objects.equals(version.getId(), process.getRegulationVersionId()))
                .filter(process -> process.getId() != null)
                .toList();
        if (processes.isEmpty()) {
            throw frozenOrderBlocked("removed活跃订单冻结QA版本缺少QA工序", removed.getId());
        }
        Set<Long> processIds = processes.stream().map(MesQaInspectionRegulationProcessDO::getId)
                .collect(Collectors.toSet());
        List<MesQaInspectionRegulationItemDO> items = inspectionRegulationItemMapper
                .selectListByVersionId(version.getId());
        if (items == null || items.isEmpty() || items.stream().anyMatch(item ->
                !Objects.equals(version.getId(), item.getRegulationVersionId())
                        || item.getQaProcessId() == null || !processIds.contains(item.getQaProcessId()))) {
            throw frozenOrderBlocked("removed活跃订单冻结QA检验项目身份无效", removed.getId());
        }
        List<PqcInspectionRule> rules;
        try {
            rules = parseCanonicalInspectionRules(version);
        } catch (IllegalArgumentException ex) {
            throw frozenOrderBlocked("removed活跃订单冻结QA规则无效：" + ex.getMessage(), removed.getId());
        }
        String truthTableReason = validateInspectionRuleTruthTable(rules, items);
        if (truthTableReason != null) {
            throw frozenOrderBlocked("removed活跃订单冻结QA规则无效：" + truthTableReason, removed.getId());
        }
        return new ActiveOrderQaSource(project.getId(), regulation, version, processes, items, rules);
    }

    private void validateRemovedPqcTasks(MesProcessPoolActiveOrderDO removed, ActiveOrderQaSource qaSource) {
        List<MesPqcInspectionTaskDO> tasks = pqcInspectionTaskMapper.selectListByActiveOrderId(removed.getId());
        if (tasks == null || tasks.isEmpty()) {
            throw frozenOrderBlocked("removed活跃订单缺少冻结PQC任务", removed.getId());
        }
        Map<FrozenPqcTaskIdentity, PlannedPqcTask> expectedTasks = new LinkedHashMap<>();
        for (PlannedPqcTask plan : preparePqcTaskPlan(removed, qaSource)) {
            FrozenPqcTaskIdentity identity = new FrozenPqcTaskIdentity(
                    plan.qaProcess().getId(), plan.qaItemCode(), plan.rule().ruleKey());
            if (expectedTasks.put(identity, plan) != null) {
                throw frozenOrderBlocked("removed活跃订单冻结QA任务计划多义", removed.getId());
            }
        }
        Set<FrozenPqcTaskIdentity> actualIdentities = new LinkedHashSet<>();
        for (MesPqcInspectionTaskDO task : tasks) {
            FrozenPqcTaskIdentity identity = task == null ? null
                    : new FrozenPqcTaskIdentity(task.getQaProcessId(), normalizeQaItemCode(task.getQaItemCode()),
                    task.getInspectionRuleKey());
            PlannedPqcTask expected = expectedTasks.get(identity);
            if (task == null || identity.qaProcessId() == null || identity.inspectionRuleKey() == null
                    || expected == null || !actualIdentities.add(identity)
                    || !Objects.equals(removed.getId(), task.getActiveOrderId())
                    || !Objects.equals(removed.getWorkOrderId(), task.getWorkOrderId())
                    || !Objects.equals(removed.getRouteId(), task.getRouteId())
                    || !Objects.equals(removed.getRouteVersionId(), task.getRouteVersionId())
                    || !Objects.equals(removed.getQaRegulationVersionId(), task.getRegulationVersionId())
                    || task.getRouteProcessId() != null || task.getProcessId() != null
                    || task.getBusinessDate() == null
                    || !Objects.equals(expected.rule().inspectionType(), task.getInspectionType())
                    || !Objects.equals(expected.rule().shiftCode(), task.getShiftCode())
                    || !Objects.equals(DEFAULT_ROUND_NO, task.getRoundNo())
                    || !Objects.equals(expected.plannedQuantity(), task.getPlannedInspectionQuantity())) {
                throw frozenOrderBlocked("removed活跃订单冻结PQC任务身份无效", removed.getId());
            }
        }
        if (!actualIdentities.equals(expectedTasks.keySet())) {
            throw frozenOrderBlocked("removed活跃订单冻结PQC任务不完整", removed.getId());
        }
    }

    private static ServiceException frozenOrderBlocked(String reason, Long activeOrderId) {
        return exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                reason + "，activeOrderId=" + activeOrderId);
    }

    private static MesTeamLeaderActiveOrderAddResult addResult(Long activeOrderId, String action) {
        return MesTeamLeaderActiveOrderAddResult.builder()
                .activeOrderId(activeOrderId)
                .action(action)
                .build();
    }

    private Long nextSortOrderForLeader(Long leaderUserId) {
        MesProcessPoolActiveOrderDO last = activeOrderMapper.selectLastByLeaderForUpdate(leaderUserId);
        if (last == null) {
            return 1L;
        }
        if (last.getSortOrder() == null || last.getSortOrder() == Long.MAX_VALUE) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_MOVE_INVALID, "无法分配新的活跃订单排序值");
        }
        return last.getSortOrder() + 1L;
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
        Map<Long, ActiveOrderProgress> progressByActiveOrderId = loadActiveOrderProgress(activeOrders,
                routeVersionsById);
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

    private Map<Long, ActiveOrderProgress> loadActiveOrderProgress(
            List<MesProcessPoolActiveOrderDO> activeOrders,
            Map<Long, MesProRouteVersionDO> routeVersionsById) {
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
        Map<ActiveOrderProcessIdentity, BigDecimal> allocatedQuantityByProcess =
                reportAllocationMapper.selectListByActiveOrderIds(activeOrderIds).stream()
                        .filter(allocation -> allocation.getActiveOrderId() != null
                                && allocation.getRouteProcessId() != null
                                && allocation.getProcessId() != null)
                        .collect(Collectors.groupingBy(allocation -> new ActiveOrderProcessIdentity(
                                        allocation.getActiveOrderId(), allocation.getRouteProcessId(),
                                        allocation.getProcessId()),
                                LinkedHashMap::new,
                                Collectors.reducing(BigDecimal.ZERO, this::requireAllocationQuantity, BigDecimal::add)));
        Set<ActiveOrderProcessIdentity> inspectedProcesses =
                pqcInspectionTaskMapper.selectListByActiveOrderIds(activeOrderIds).stream()
                        .filter(MesTeamLeaderActiveOrderServiceImpl::isInspectionProgressCompleted)
                        .map(task -> new ActiveOrderProcessIdentity(task.getActiveOrderId(),
                                task.getRouteProcessId(), task.getProcessId()))
                        .filter(ActiveOrderProcessIdentity::complete)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ActiveOrderProgress> progressByActiveOrderId = new LinkedHashMap<>();
        activeOrders.forEach(activeOrder -> {
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots =
                    snapshotsByActiveOrderId.getOrDefault(activeOrder.getId(), List.of());
            List<ProcessIdentity> snapshotProcessIdentities = requireProgressProcessIdentities(activeOrder, snapshots);
            Map<ProcessIdentity, BigDecimal> targetQuantityByProcess = progressTargetQuantities(activeOrder,
                    snapshots);
            List<MesTeamLeaderActiveOrderRow.ProcessRemainingQuantity> processRemainingQuantities =
                    resolveProcessRemainingQuantities(activeOrder, snapshots, allocatedQuantityByProcess);
            List<ProcessIdentity> processIdentities = resolveFormalProgressProcessIdentities(activeOrder,
                    routeVersionsById.get(activeOrder.getRouteVersionId()), snapshotProcessIdentities);
            int totalProcessCount = processIdentities.size();
            long completedProcessCount = processIdentities.stream()
                    .filter(process -> isProductionProcessFullyAllocated(activeOrder, process,
                            targetQuantityByProcess, allocatedQuantityByProcess))
                    .count();
            long inspectedProcessCount = processIdentities.stream()
                    .filter(process -> inspectedProcesses.contains(new ActiveOrderProcessIdentity(
                            activeOrder.getId(), process.routeProcessId(), process.processId())))
                    .count();
            progressByActiveOrderId.put(activeOrder.getId(), new ActiveOrderProgress(
                    toProgressPercent(completedProcessCount, totalProcessCount),
                    toProgressPercent(inspectedProcessCount, totalProcessCount),
                    processRemainingQuantities));
        });
        return progressByActiveOrderId;
    }

    private static List<MesTeamLeaderActiveOrderRow.ProcessRemainingQuantity> resolveProcessRemainingQuantities(
            MesProcessPoolActiveOrderDO activeOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
            Map<ActiveOrderProcessIdentity, BigDecimal> allocatedQuantityByProcess) {
        return snapshots.stream()
                .map(snapshot -> {
                    BigDecimal plannedQuantity = snapshot.getPlannedQuantitySnapshot();
                    if (plannedQuantity == null || plannedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                        throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
                    }
                    BigDecimal allocatedQuantity = allocatedQuantityByProcess.getOrDefault(
                            new ActiveOrderProcessIdentity(activeOrder.getId(), snapshot.getRouteProcessId(),
                                    snapshot.getProcessId()),
                            BigDecimal.ZERO);
                    BigDecimal remainingQuantity = plannedQuantity.subtract(allocatedQuantity);
                    if (remainingQuantity.compareTo(BigDecimal.ZERO) < 0) {
                        remainingQuantity = BigDecimal.ZERO.setScale(plannedQuantity.scale(), RoundingMode.UNNECESSARY);
                    }
                    return new MesTeamLeaderActiveOrderRow.ProcessRemainingQuantity()
                            .setRouteProcessId(snapshot.getRouteProcessId())
                            .setProcessId(snapshot.getProcessId())
                            .setPlannedQuantity(plannedQuantity)
                            .setAllocatedQuantity(allocatedQuantity)
                            .setRemainingQuantity(remainingQuantity);
                })
                .toList();
    }

    private BigDecimal requireAllocationQuantity(MesProcessPoolReportAllocationDO allocation) {
        if (allocation.getAllocatedQuantity() == null) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, allocation.getWorkOrderId());
        }
        return allocation.getAllocatedQuantity();
    }

    private static Map<ProcessIdentity, BigDecimal> progressTargetQuantities(
            MesProcessPoolActiveOrderDO activeOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        Map<ProcessIdentity, BigDecimal> targets = new LinkedHashMap<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            ProcessIdentity identity = new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId());
            BigDecimal plannedQuantity = snapshot.getPlannedQuantitySnapshot();
            if (plannedQuantity == null || plannedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
            }
            targets.put(identity, plannedQuantity);
        }
        return targets;
    }

    private static boolean isProductionProcessFullyAllocated(
            MesProcessPoolActiveOrderDO activeOrder,
            ProcessIdentity process,
            Map<ProcessIdentity, BigDecimal> targetQuantityByProcess,
            Map<ActiveOrderProcessIdentity, BigDecimal> allocatedQuantityByProcess) {
        BigDecimal targetQuantity = targetQuantityByProcess.get(process);
        if (targetQuantity == null) {
            BigDecimal erpQuantity = activeOrder.getErpFixedQuantitySnapshot();
            if (erpQuantity == null || erpQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
            }
            targetQuantity = erpQuantity;
        }
        BigDecimal allocatedQuantity = allocatedQuantityByProcess.getOrDefault(new ActiveOrderProcessIdentity(
                activeOrder.getId(), process.routeProcessId(), process.processId()), BigDecimal.ZERO);
        return allocatedQuantity.compareTo(targetQuantity) >= 0;
    }

    private static List<ProcessIdentity> resolveFormalProgressProcessIdentities(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProRouteVersionDO routeVersion,
            List<ProcessIdentity> snapshotProcessIdentities) {
        List<ProcessIdentity> routeProcessIdentities = parseRouteSnapshotProcessIdentities(activeOrder,
                routeVersion);
        if (routeProcessIdentities.isEmpty()) {
            return snapshotProcessIdentities;
        }
        Set<ProcessIdentity> formalIdentitySet = new LinkedHashSet<>(routeProcessIdentities);
        boolean snapshotOutsideFormalRoute = snapshotProcessIdentities.stream()
                .anyMatch(snapshot -> !formalIdentitySet.contains(snapshot));
        if (snapshotOutsideFormalRoute) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        return routeProcessIdentities;
    }

    private static List<ProcessIdentity> parseRouteSnapshotProcessIdentities(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProRouteVersionDO routeVersion) {
        if (routeVersion == null || routeVersion.getRouteSnapshotJson() == null
                || routeVersion.getRouteSnapshotJson().isBlank()) {
            return List.of();
        }
        JSONObject root = JSON.parseObject(routeVersion.getRouteSnapshotJson());
        JSONObject configSnapshots = root == null ? null : root.getJSONObject("configSnapshots");
        JSONObject flowGraph = configSnapshots == null ? null : configSnapshots.getJSONObject("flowGraph");
        JSONArray nodes = flowGraph == null ? null : flowGraph.getJSONArray("nodes");
        if (nodes == null || nodes.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        List<ProcessIdentity> identities = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            JSONObject node = nodes.getJSONObject(index);
            Long routeProcessId = node == null ? null : node.getLong("routeProcessId");
            Long processId = node == null ? null : node.getLong("processId");
            if (routeProcessId == null || processId == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
            }
            identities.add(new ProcessIdentity(routeProcessId, processId));
        }
        Set<ProcessIdentity> distinctIdentities = new LinkedHashSet<>(identities);
        if (distinctIdentities.size() != identities.size()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        return List.copyOf(distinctIdentities);
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
        return MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus());
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
                .setProductId(product.getId())
                .setProductName(product.getName())
                .setProductCode(product.getCode())
                .setBatchCode(workOrder.getBatchCode())
                .setQuantity(workOrder.getQuantity())
                .setRouteId(activeOrder.getRouteId())
                .setRouteName(route.getName())
                .setRouteVersionId(activeOrder.getRouteVersionId())
                .setRouteVersionNo(routeVersion.getVersionNo())
                .setErpFixedQuantitySnapshot(activeOrder.getErpFixedQuantitySnapshot())
                .setProcessRemainingQuantities(progress.processRemainingQuantities())
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
                .setReleaseApplicationStatus(releaseApplication == null ? null : releaseApplication.getApplicationStatus());
    }

    private ActiveOrderRouteSource requireProductionRouteSourceForAdd(MesProWorkOrderDO workOrder) {
        CandidateEligibilityContext context = buildRouteSourceContext(List.of(workOrder));
        RouteSourceResolution resolution = resolveProductionRouteSource(workOrder, context);
        if (resolution.source() == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED,
                    resolution.ineligibleReason() + "，workOrderId=" + workOrder.getId());
        }
        return resolution.source();
    }

    private void insertProcessSnapshots(MesProcessPoolActiveOrderDO activeOrder, BigDecimal erpFixedQuantity,
                                        List<MesProScheduleOrderProcessDO> enabledProcesses) {
        Set<Long> processIds = enabledProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProcessPoolDeviceParameterRuleDO> parameterRules = processIds.isEmpty() ? List.of()
                : parameterRuleMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolDeviceParameterRuleDO>()
                        .in(MesProcessPoolDeviceParameterRuleDO::getProcessId, processIds)
                        .eq(MesProcessPoolDeviceParameterRuleDO::getEnabled, Boolean.TRUE));
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = enabledProcesses.stream()
                .map(process -> toProcessSnapshot(activeOrder, process, erpFixedQuantity, parameterRules))
                .toList();
        if (!Boolean.TRUE.equals(processSnapshotMapper.insertBatch(snapshots))) {
            throw new IllegalStateException("Failed to insert active order process snapshots");
        }
    }

    private ActiveOrderQaSource requireCurrentQaSource(Long routeId, Long contextId) {
        MesRouteDccProjectBindingDO binding = routeDccProjectBindingMapper.selectCurrentByRouteId(routeId);
        if (binding == null || binding.getDccProjectCodeId() == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "工艺路线缺少 DCC 项目代码绑定，routeId=" + routeId + "，contextId=" + contextId);
        }
        DccProjectCodeDO project = dccProjectCodeMapper.selectById(binding.getDccProjectCodeId());
        if (project == null || !DccProjectCodeStatusConstants.ENABLE.equals(project.getStatus())) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "DCC 项目代码不存在或已停用，dccProjectCodeId=" + binding.getDccProjectCodeId()
                            + "，contextId=" + contextId);
        }
        requireMatchingCurrentRouteProductMaster(routeId, project);
        List<MesQaInspectionRegulationDO> regulations = inspectionRegulationMapper
                .selectListByDccProjectCodeIds(List.of(project.getId())).stream()
                .filter(regulation -> Objects.equals(project.getId(), regulation.getDccProjectCodeId()))
                .filter(regulation -> MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA
                        .equals(regulation.getOwnerModule()))
                .toList();
        if (regulations.size() != 1) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "DCC 项目代码缺少唯一 QA 规程，dccProjectCodeId=" + project.getId()
                            + "，contextId=" + contextId);
        }
        MesQaInspectionRegulationDO regulation = regulations.get(0);
        MesQaInspectionRegulationVersionDO version = regulation.getCurrentVersionId() == null
                ? null : inspectionRegulationVersionMapper.selectById(regulation.getCurrentVersionId());
        if (!isCurrentPublishedVersion(regulation, version)) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "QA 规程缺少当前正式发布版本，qaRegulationId=" + regulation.getId()
                            + "，contextId=" + contextId);
        }
        List<MesQaInspectionRegulationProcessDO> processes = inspectionRegulationProcessMapper
                .selectListByVersionIds(List.of(version.getId())).stream()
                .filter(process -> Objects.equals(version.getId(), process.getRegulationVersionId()))
                .filter(process -> process.getId() != null)
                .toList();
        if (processes.isEmpty()) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "已发布 QA 规程缺少 QA 工序，regulationVersionId=" + version.getId()
                            + "，contextId=" + contextId);
        }
        Set<Long> processIds = processes.stream().map(MesQaInspectionRegulationProcessDO::getId)
                .collect(Collectors.toSet());
        List<MesQaInspectionRegulationItemDO> items = inspectionRegulationItemMapper
                .selectListByVersionId(version.getId());
        if (items == null || items.isEmpty()
                || items.stream().anyMatch(item -> !Objects.equals(version.getId(), item.getRegulationVersionId())
                || item.getQaProcessId() == null || !processIds.contains(item.getQaProcessId()))) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "已发布 QA 规程的检验项目缺少有效 QA 工序身份，regulationVersionId=" + version.getId()
                            + "，contextId=" + contextId);
        }
        List<PqcInspectionRule> rules;
        try {
            rules = parseCanonicalInspectionRules(version);
        } catch (IllegalArgumentException ex) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    ex.getMessage() + "，contextId=" + contextId
                            + "，regulationVersionId=" + version.getId());
        }
        String truthTableReason = validateInspectionRuleTruthTable(rules, items);
        if (truthTableReason != null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    truthTableReason + "，contextId=" + contextId
                            + "，regulationVersionId=" + version.getId());
        }
        return new ActiveOrderQaSource(project.getId(), regulation, version, processes, items, rules);
    }

    private void requireMatchingCurrentRouteProductMaster(Long routeId, DccProjectCodeDO project) {
        List<MesProRouteProductDO> routeProducts = routeProductMapper.selectListByRouteId(routeId);
        List<Long> itemIds = routeProducts == null ? List.of() : routeProducts.stream()
                .map(MesProRouteProductDO::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesMdItemDO> itemsById = itemIds.isEmpty() ? Collections.emptyMap()
                : itemMapper.selectListByIds(itemIds).stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(MesMdItemDO::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        MesRouteDccProductMasterInvariant.requireMatching(routeId, routeProducts, itemsById, project);
    }

    private List<PlannedPqcTask> preparePqcTaskPlan(MesProcessPoolActiveOrderDO activeOrder,
                                                     ActiveOrderQaSource qaSource) {
        List<PlannedPqcTask> plans = new ArrayList<>();
        for (MesQaInspectionRegulationProcessDO qaProcess : qaSource.processes()) {
            List<MesQaInspectionRegulationItemDO> processItems = qaSource.items().stream()
                    .filter(item -> Objects.equals(qaProcess.getId(), item.getQaProcessId()))
                    .sorted(Comparator
                            .comparing(MesQaInspectionRegulationItemDO::getItemSort,
                                    Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(item -> normalizeQaItemCode(item.getItemCode()),
                                    Comparator.nullsLast(String::compareTo))
                            .thenComparing(MesQaInspectionRegulationItemDO::getId,
                                    Comparator.nullsLast(Long::compareTo)))
                    .toList();
            if (processItems.isEmpty()) {
                throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                        "QA工序缺少检验项目，workOrderId=" + activeOrder.getWorkOrderId()
                                + "，qaProcessId=" + qaProcess.getId());
            }
            for (PqcInspectionRule rule : qaSource.rules()) {
                if (!rule.required() || processItems.stream()
                        .noneMatch(item -> Objects.equals(rule.inspectionType(),
                                normalizeInspectionType(item.getInspectionType())))) {
                    continue;
                }
                if (isItemScopedInspectionType(rule.inspectionType())) {
                    for (MesQaInspectionRegulationItemDO item : processItems) {
                        if (!Objects.equals(rule.inspectionType(), normalizeInspectionType(item.getInspectionType()))) {
                            continue;
                        }
                        String qaItemCode = requireQaItemCode(activeOrder, qaProcess, item);
                        Integer plannedQuantity = resolveInspectionQuantity(activeOrder, List.of(item),
                                rule.inspectionType());
                        plans.add(new PlannedPqcTask(qaProcess, item, qaItemCode, rule, plannedQuantity));
                    }
                    continue;
                }
                Integer plannedQuantity = resolveInspectionQuantity(activeOrder, processItems, rule.inspectionType());
                plans.add(new PlannedPqcTask(qaProcess, null, "", rule, plannedQuantity));
            }
        }
        if (plans.isEmpty()) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "已发布 QA 规程未生成任何检验任务，workOrderId=" + activeOrder.getWorkOrderId()
                            + "，regulationVersionId=" + qaSource.version().getId());
        }
        return plans;
    }

    private void insertPqcInspectionTasks(MesProcessPoolActiveOrderDO activeOrder,
                                          ActiveOrderQaSource qaSource,
                                          List<PlannedPqcTask> plans) {
        LocalDate businessDate = resolvePqcBusinessDate(activeOrder);
        for (PlannedPqcTask plan : plans) {
            MesPqcInspectionTaskDO task = buildPqcTask(activeOrder, plan.qaProcess(), qaSource.version(),
                    plan.qaItemCode(), plan.rule().inspectionType(), plan.rule().ruleKey(), businessDate,
                    plan.rule().shiftCode(), plan.plannedQuantity());
            insertPqcInspectionTask(task);
        }
    }

    private LocalDate resolvePqcBusinessDate(MesProcessPoolActiveOrderDO activeOrder) {
        if (activeOrder.getJoinedAt() == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "活跃订单缺少实际加入时间，activeOrderId=" + activeOrder.getId());
        }
        return activeOrder.getJoinedAt().toLocalDate();
    }

    private MesPqcInspectionTaskDO buildPqcTask(MesProcessPoolActiveOrderDO activeOrder,
                                                MesQaInspectionRegulationProcessDO qaProcess,
                                                MesQaInspectionRegulationVersionDO version,
                                                String qaItemCode,
                                                String inspectionType,
                                                String inspectionRuleKey,
                                                LocalDate businessDate,
                                                String shiftCode,
                                                Integer plannedInspectionQuantity) {
        return MesPqcInspectionTaskDO.builder()
                .activeOrderId(activeOrder.getId())
                .workOrderId(activeOrder.getWorkOrderId())
                .routeId(activeOrder.getRouteId())
                .routeVersionId(activeOrder.getRouteVersionId())
                .qaProcessId(qaProcess.getId())
                .qaItemCode(qaItemCode)
                .regulationVersionId(version.getId())
                .inspectionType(inspectionType)
                .inspectionRuleKey(inspectionRuleKey)
                .businessDate(businessDate)
                .shiftCode(shiftCode)
                .roundNo(DEFAULT_ROUND_NO)
                .plannedInspectionQuantity(plannedInspectionQuantity)
                .actualInspectionQuantity(0)
                .taskStatus(PQC_STATUS_PENDING)
                .build();
    }

    private void insertPqcInspectionTask(MesPqcInspectionTaskDO task) {
        MesPqcInspectionTaskDO existing = pqcInspectionTaskMapper.selectByQaIdentity(task.getActiveOrderId(),
                task.getRegulationVersionId(), task.getQaProcessId(), task.getQaItemCode(), task.getInspectionRuleKey(),
                task.getBusinessDate());
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

    private Integer resolveInspectionQuantity(MesProcessPoolActiveOrderDO activeOrder,
                                              List<MesQaInspectionRegulationItemDO> items,
                                              String inspectionType) {
        if (Objects.equals(INSPECTION_TYPE_PATROL, inspectionType)) {
            return resolvePatrolInspectionQuantity(activeOrder.getErpFixedQuantitySnapshot(), items,
                    activeOrder.getId());
        }
        if (Objects.equals(INSPECTION_TYPE_FIRST, inspectionType) || Objects.equals("FINAL", inspectionType)) {
            return resolveFixedInspectionQuantity(items, inspectionType, activeOrder.getId());
        }
        throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                "QA检验类型无效，activeOrderId=" + activeOrder.getId() + "，inspectionType=" + inspectionType);
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

    private Integer resolvePatrolInspectionQuantity(BigDecimal plannedQuantity,
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
            return ceilPatrolInspectionQuantity(plannedQuantity, ratio, activeOrderId);
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

    private static String normalizeRuleKey(String ruleKey) {
        if (ruleKey == null) {
            return null;
        }
        String trimmed = ruleKey.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isItemScopedInspectionType(String inspectionType) {
        return Objects.equals(INSPECTION_TYPE_FIRST, inspectionType)
                || Objects.equals(INSPECTION_TYPE_PATROL, inspectionType)
                || Objects.equals(INSPECTION_TYPE_FINAL, inspectionType);
    }

    private static String normalizeQaItemCode(String qaItemCode) {
        if (qaItemCode == null) {
            return null;
        }
        String trimmed = qaItemCode.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }

    private static String requireQaItemCode(MesProcessPoolActiveOrderDO activeOrder,
                                            MesQaInspectionRegulationProcessDO qaProcess,
                                            MesQaInspectionRegulationItemDO item) {
        String qaItemCode = normalizeQaItemCode(item == null ? null : item.getItemCode());
        if (qaItemCode == null || qaItemCode.isEmpty()) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "QA检验项目缺少项目编码，activeOrderId=" + activeOrder.getId()
                            + "，qaProcessId=" + qaProcess.getId());
        }
        return qaItemCode;
    }

    private static String expectedInspectionType(String ruleKey) {
        if (Objects.equals(RULE_KEY_FIRST, ruleKey)) {
            return INSPECTION_TYPE_FIRST;
        }
        if (Objects.equals(RULE_KEY_PATROL_AM, ruleKey) || Objects.equals(RULE_KEY_PATROL_PM, ruleKey)) {
            return INSPECTION_TYPE_PATROL;
        }
        if (Objects.equals(RULE_KEY_FINAL, ruleKey)) {
            return INSPECTION_TYPE_FINAL;
        }
        return null;
    }

    private static String shiftCodeForRuleKey(String ruleKey) {
        if (Objects.equals(RULE_KEY_FIRST, ruleKey)) {
            return SHIFT_FIRST;
        }
        if (Objects.equals(RULE_KEY_PATROL_AM, ruleKey)) {
            return SHIFT_PATROL_AM;
        }
        if (Objects.equals(RULE_KEY_PATROL_PM, ruleKey)) {
            return SHIFT_PATROL_PM;
        }
        if (Objects.equals(RULE_KEY_FINAL, ruleKey)) {
            return SHIFT_FINAL;
        }
        return null;
    }

    private static int inspectionRuleOrder(String ruleKey) {
        if (Objects.equals(RULE_KEY_FIRST, ruleKey)) {
            return 1;
        }
        if (Objects.equals(RULE_KEY_PATROL_AM, ruleKey)) {
            return 2;
        }
        if (Objects.equals(RULE_KEY_PATROL_PM, ruleKey)) {
            return 3;
        }
        if (Objects.equals(RULE_KEY_FINAL, ruleKey)) {
            return 4;
        }
        return 99;
    }

    private static int inspectionTypeOrder(String inspectionType) {
        if (Objects.equals(INSPECTION_TYPE_FIRST, inspectionType)) {
            return 1;
        }
        if (Objects.equals(INSPECTION_TYPE_PATROL, inspectionType)) {
            return 2;
        }
        if (Objects.equals("FINAL", inspectionType)) {
            return 3;
        }
        return 99;
    }

    private static String identityText(MesPqcInspectionTaskDO task) {
        return "activeOrderId=" + task.getActiveOrderId()
                + "，regulationVersionId=" + task.getRegulationVersionId()
                + "，qaProcessId=" + task.getQaProcessId()
                + "，qaItemCode=" + task.getQaItemCode()
                + "，inspectionRuleKey=" + task.getInspectionRuleKey()
                + "，inspectionType=" + task.getInspectionType()
                + "，businessDate=" + task.getBusinessDate()
                + "，shiftCode=" + task.getShiftCode()
                + "，roundNo=" + task.getRoundNo();
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO toProcessSnapshot(MesProcessPoolActiveOrderDO activeOrder,
                                                                         MesProScheduleOrderProcessDO process,
                                                                         BigDecimal erpFixedQuantity,
                                                                         List<MesProcessPoolDeviceParameterRuleDO>
                                                                                 parameterRules) {
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
        String parameterSnapshotJson = MesDeviceParameterSnapshotCodec.canonicalize(parameterRules,
                process.getRouteProcessId(), process.getProcessId());
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
                .parameterSnapshotJson(parameterSnapshotJson)
                .parameterSnapshotSha256(MesDeviceParameterSnapshotCodec.sha256(parameterSnapshotJson))
                .parameterSnapshotState(MesDeviceParameterSnapshotCodec.STATE_FROZEN)
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

    private record ActiveOrderProgress(BigDecimal productionProgressPercent, BigDecimal inspectionProgressPercent,
                                       List<MesTeamLeaderActiveOrderRow.ProcessRemainingQuantity>
                                               processRemainingQuantities) {
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record ActiveOrderProcessIdentity(Long activeOrderId, Long routeProcessId, Long processId) {

        private boolean complete() {
            return activeOrderId != null && routeProcessId != null && processId != null;
        }
    }

    private record CandidateEligibility(boolean eligible, String candidateState, String ineligibleReason) {
    }

    private record PqcInspectionRule(String ruleKey, String inspectionType, String shiftCode, boolean required) {
    }

    private record FrozenPqcTaskIdentity(Long qaProcessId, String qaItemCode, String inspectionRuleKey) {
    }

    private record ActiveOrderQaSource(Long dccProjectCodeId,
                                       MesQaInspectionRegulationDO regulation,
                                       MesQaInspectionRegulationVersionDO version,
                                       List<MesQaInspectionRegulationProcessDO> processes,
                                       List<MesQaInspectionRegulationItemDO> items,
                                       List<PqcInspectionRule> rules) {
    }

    private record PlannedPqcTask(MesQaInspectionRegulationProcessDO qaProcess,
                                  MesQaInspectionRegulationItemDO qaItem,
                                  String qaItemCode,
                                  PqcInspectionRule rule,
                                  Integer plannedQuantity) {
    }

    private record ActiveOrderRouteSource(Long routeId, Long routeVersionId,
                                          List<MesProScheduleOrderProcessDO> routeProcesses) {
    }

    private record RouteSourceResolution(ActiveOrderRouteSource source, String ineligibleReason,
                                         RouteSourceFailureType failureType) {
    }

    private enum RouteSourceFailureType {
        WORK_ORDER,
        ROUTE,
        DCC_PROJECT,
        QA
    }

    private record CandidateEligibilityContext(
            Map<Long, List<MesProRouteProductDO>> routeBindingsByProductId,
            Map<Long, List<MesProRouteVersionDO>> activeRouteVersionsByRouteId,
            Set<Long> existingRouteIds,
            Map<Long, CandidateEligibility> qaEligibilityByRouteId) {
    }
}
