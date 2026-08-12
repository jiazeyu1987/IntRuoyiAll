package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
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
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
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
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
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
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
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
                                               MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
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
        this.processSnapshotMapper = processSnapshotMapper;
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
        CandidateEligibilityContext context = buildCandidateEligibilityContext(workOrders);
        return workOrders.stream()
                .map(workOrder -> toActiveOrderCandidate(workOrder, context))
                .sorted((left, right) -> Boolean.compare(right.isEligible(), left.isEligible()))
                .limit(ACTIVE_ORDER_CANDIDATE_LIMIT)
                .toList();
    }

    private CandidateEligibilityContext buildCandidateEligibilityContext(List<MesProWorkOrderDO> workOrders) {
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
                    Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap());
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
                    existingRouteIds, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap());
        }
        Map<Long, List<MesProRouteVersionDO>> activeRouteVersionsByRouteId = routeVersionMapper
                .selectListByRouteIds(validRouteIds).stream()
                .filter(version -> Boolean.TRUE.equals(version.getActive()))
                .filter(version -> Objects.equals("ACTIVE", version.getLifecycleStatus()))
                .filter(version -> version.getRouteId() != null)
                .filter(version -> version.getId() != null)
                .collect(Collectors.groupingBy(MesProRouteVersionDO::getRouteId));
        List<MesProRouteProductDO> routeProducts = routeProductMapper.selectListByRouteIds(validRouteIds);
        Map<Long, List<MesProRouteProductDO>> routeProductsByRouteId = routeProducts.stream()
                .filter(binding -> binding.getRouteId() != null)
                .filter(binding -> binding.getItemId() != null)
                .collect(Collectors.groupingBy(MesProRouteProductDO::getRouteId));
        List<Long> routeProductItemIds = routeProducts.stream()
                .map(MesProRouteProductDO::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesMdItemDO> routeItemsById = routeProductItemIds.isEmpty()
                ? Collections.emptyMap()
                : itemMapper.selectBatchIds(routeProductItemIds).stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(MesMdItemDO::getId, Function.identity(), (left, right) -> left));
        Map<Long, List<DccProjectCodeDO>> dccProjectsByRouteId = resolveDccProjectsByRouteId(
                routeProductsByRouteId, routeItemsById, dccProjectCodeMapper.selectEnabledList());
        Set<Long> dccProjectCodeIds = dccProjectsByRouteId.values().stream()
                .flatMap(Collection::stream)
                .map(DccProjectCodeDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesQaInspectionRegulationDO> regulations = dccProjectCodeIds.isEmpty()
                ? Collections.emptyList()
                : inspectionRegulationMapper.selectListByDccProjectCodeIds(dccProjectCodeIds).stream()
                .filter(regulation -> Objects.equals("PUBLISHED", regulation.getLifecycleStatus()))
                .filter(regulation -> regulation.getCurrentVersionId() != null)
                .toList();
        Map<Long, List<MesQaInspectionRegulationDO>> regulationsByDccProjectCodeId = regulations.stream()
                .filter(regulation -> regulation.getDccProjectCodeId() != null)
                .collect(Collectors.groupingBy(MesQaInspectionRegulationDO::getDccProjectCodeId));
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
        Map<Long, List<MesQaInspectionRegulationProcessDO>> processesByVersionId = versionIds.isEmpty()
                ? Collections.emptyMap()
                : inspectionRegulationProcessMapper.selectListByVersionIds(versionIds).stream()
                .filter(process -> process.getRegulationVersionId() != null)
                .collect(Collectors.groupingBy(MesQaInspectionRegulationProcessDO::getRegulationVersionId));
        return new CandidateEligibilityContext(routeBindingsByProductId, activeRouteVersionsByRouteId,
                existingRouteIds, dccProjectsByRouteId, regulationsByDccProjectCodeId, versionsById,
                processesByVersionId, itemsByVersionId);
    }

    private static RouteSourceResolution routeSourceFailure(String reason, RouteSourceFailureType failureType) {
        return new RouteSourceResolution(null, reason, failureType);
    }

    private static CandidateEligibilityContext emptyCandidateEligibilityContext() {
        return new CandidateEligibilityContext(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap());
    }

    private Map<Long, List<DccProjectCodeDO>> resolveDccProjectsByRouteId(
            Map<Long, List<MesProRouteProductDO>> routeProductsByRouteId,
            Map<Long, MesMdItemDO> routeItemsById,
            List<DccProjectCodeDO> enabledProjects) {
        Map<String, List<DccProjectCodeDO>> projectsByCode = enabledProjects.stream()
                .filter(project -> project.getId() != null)
                .filter(project -> project.getProductMasterId() != null)
                .filter(project -> project.getProjectCode() != null && !project.getProjectCode().isBlank())
                .collect(Collectors.groupingBy(DccProjectCodeDO::getProjectCode));
        Map<Long, List<DccProjectCodeDO>> result = new LinkedHashMap<>();
        routeProductsByRouteId.forEach((routeId, bindings) -> {
            Map<Long, DccProjectCodeDO> uniqueProjects = new LinkedHashMap<>();
            bindings.stream()
                    .map(MesProRouteProductDO::getItemId)
                    .map(routeItemsById::get)
                    .filter(Objects::nonNull)
                    .map(MesMdItemDO::getCode)
                    .filter(Objects::nonNull)
                    .flatMap(code -> projectsByCode.getOrDefault(code, Collections.emptyList()).stream())
                    .forEach(project -> uniqueProjects.put(project.getId(), project));
            result.put(routeId, List.copyOf(uniqueProjects.values()));
        });
        return result;
    }

    private static boolean isCancelledWorkOrder(MesProWorkOrderDO workOrder) {
        return workOrder != null && Objects.equals(workOrder.getStatus(),
                MesProWorkOrderStatusEnum.CANCELED.getStatus());
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
        String pqcReason = validateCandidatePqcPrerequisites(workOrder, source, context);
        if (pqcReason != null) {
            return blockedCandidate(pqcReason);
        }
        return new CandidateEligibility(true, null);
    }

    private RouteSourceResolution resolveQaRouteSource(MesProWorkOrderDO workOrder,
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
        List<DccProjectCodeDO> dccProjects = context.dccProjectsByRouteId()
                .getOrDefault(routeId, Collections.emptyList());
        if (dccProjects.isEmpty()) {
            return routeSourceFailure("工艺路线缺少DCC项目代码绑定", RouteSourceFailureType.DCC_PROJECT);
        }
        if (dccProjects.size() > 1) {
            return routeSourceFailure("工艺路线DCC项目代码绑定不唯一", RouteSourceFailureType.DCC_PROJECT);
        }
        DccProjectCodeDO dccProject = dccProjects.get(0);
        List<MesQaInspectionRegulationDO> regulations = context.regulationsByDccProjectCodeId()
                .getOrDefault(dccProject.getId(), Collections.emptyList());
        if (regulations.isEmpty()) {
            return routeSourceFailure("缺少已发布QA规程", RouteSourceFailureType.QA);
        }
        if (regulations.size() > 1) {
            return routeSourceFailure("DCC项目代码已发布QA规程不唯一", RouteSourceFailureType.QA);
        }
        MesQaInspectionRegulationDO regulation = regulations.get(0);
        List<MesProScheduleOrderProcessDO> routeProcesses = toRouteProcessSources(workOrder, activeRouteVersion);
        if (routeProcesses.isEmpty()) {
            return routeSourceFailure("工艺路线快照缺少正式工序", RouteSourceFailureType.ROUTE);
        }
        List<MesQaInspectionRegulationProcessDO> qaProcesses = context.processesByVersionId()
                .getOrDefault(regulation.getCurrentVersionId(), Collections.emptyList());
        if (qaProcesses.isEmpty()) {
            return routeSourceFailure("缺少已发布QA规程", RouteSourceFailureType.QA);
        }
        return new RouteSourceResolution(new ActiveOrderRouteSource(routeId,
                activeRouteVersion.getId(), dccProject.getId(), regulation, routeProcesses, qaProcesses), null, null);
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

    private String validateCandidatePqcPrerequisites(MesProWorkOrderDO workOrder,
                                                     ActiveOrderRouteSource source,
                                                     CandidateEligibilityContext context) {
        MesQaInspectionRegulationDO regulation = source.regulation();
        if (regulation.getCurrentVersionId() == null) {
            return "缺少已发布QA规程";
        }
        MesQaInspectionRegulationVersionDO version = context.versionsById().get(regulation.getCurrentVersionId());
        if (version == null || !Objects.equals(regulation.getId(), version.getRegulationId())
                || !Objects.equals("PUBLISHED", version.getLifecycleStatus())) {
            return "QA规程发布版本不存在或未发布";
        }
        List<MesQaInspectionRegulationItemDO> items = context.itemsByVersionId()
                .getOrDefault(regulation.getCurrentVersionId(), Collections.emptyList());
        if (items == null || items.isEmpty()) {
            return "已发布QA规程缺少检验项目";
        }
        Set<Long> processIds = new LinkedHashSet<>();
        for (MesQaInspectionRegulationProcessDO process : source.qaProcesses()) {
            if (process == null || process.getId() == null
                    || !Objects.equals(regulation.getCurrentVersionId(), process.getRegulationVersionId())
                    || process.getProcessCode() == null || process.getProcessCode().isBlank()
                    || process.getProcessName() == null || process.getProcessName().isBlank()
                    || process.getSort() == null || !processIds.add(process.getId())) {
                return "QA工序身份无效";
            }
            List<MesQaInspectionRegulationItemDO> processItems = items.stream()
                    .filter(item -> Objects.equals(process.getId(), item.getQaProcessId()))
                    .toList();
            if (processItems.isEmpty()) {
                return "QA工序缺少检验项目";
            }
            Set<String> inspectionTypes = processItems.stream()
                    .map(MesQaInspectionRegulationItemDO::getInspectionType)
                    .map(MesTeamLeaderActiveOrderServiceImpl::normalizeInspectionType)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            for (String inspectionType : inspectionTypes) {
                String reason;
                if (Objects.equals(INSPECTION_TYPE_PATROL, inspectionType)) {
                    reason = validatePatrolInspectionQuantity(activeOrderQuantitySnapshot(workOrder), processItems);
                } else if (Objects.equals(INSPECTION_TYPE_FIRST, inspectionType)
                        || Objects.equals("FINAL", inspectionType)) {
                    reason = validateFixedInspectionQuantity(processItems, inspectionType);
                } else {
                    return "QA检验类型无效";
                }
                if (reason != null) {
                    return reason;
                }
            }
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
                .sortOrder(nextSortOrderForLeader(reqBO.getLeaderUserId()))
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
        insertProcessSnapshots(activeOrder, erpFixedQuantity, routeSource.routeProcesses());
        insertPqcInspectionTasks(activeOrder, routeSource.regulation(), routeSource.qaProcesses());
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

    private MesProcessPoolActiveOrderDO selectRemovedActiveOrder(Long workOrderId, Long routeId,
                                                                 Long routeVersionId) {
        return activeOrderMapper.selectRemovedByWorkOrderRouteVersion(workOrderId, routeId, routeVersionId);
    }

    private Long reactivateRemovedActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO,
                                              MesProcessPoolActiveOrderDO removed) {
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
                    .erpFixedQuantitySnapshot(removed.getErpFixedQuantitySnapshot())
                    .activeStatus(STATUS_ACTIVE)
                    .businessStatus(STATUS_ACTIVE)
                    .joinedAt(rejoinedAt)
                    .sortOrder(sortOrder)
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
            List<ProcessIdentity> snapshotProcessIdentities = requireProgressProcessIdentities(activeOrder,
                    snapshotsByActiveOrderId.getOrDefault(activeOrder.getId(), List.of()));
            Map<ProcessIdentity, BigDecimal> targetQuantityByProcess = progressTargetQuantities(activeOrder,
                    snapshotsByActiveOrderId.getOrDefault(activeOrder.getId(), List.of()));
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
                    toProgressPercent(inspectedProcessCount, totalProcessCount)));
        });
        return progressByActiveOrderId;
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
        String pqcReason = validateCandidatePqcPrerequisites(workOrder, source, context);
        if (pqcReason != null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    pqcReason + "，workOrderId=" + workOrder.getId()
                            + "，dccProjectCodeId=" + source.dccProjectCodeId());
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

    private void insertPqcInspectionTasks(MesProcessPoolActiveOrderDO activeOrder,
                                          MesQaInspectionRegulationDO regulation,
                                          List<MesQaInspectionRegulationProcessDO> qaProcesses) {
        MesQaInspectionRegulationVersionDO version = requireRegulationVersion(regulation, activeOrder.getId());
        List<MesQaInspectionRegulationItemDO> items = requireRegulationItems(regulation, activeOrder.getId());
        LocalDate businessDate = resolvePqcBusinessDate(activeOrder);
        List<MesPqcInspectionTaskDO> tasks = new ArrayList<>();
        for (MesQaInspectionRegulationProcessDO qaProcess : qaProcesses) {
            List<MesQaInspectionRegulationItemDO> processItems = items.stream()
                    .filter(item -> Objects.equals(qaProcess.getId(), item.getQaProcessId()))
                    .toList();
            if (processItems.isEmpty()) {
                throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                        "QA工序缺少检验项目，activeOrderId=" + activeOrder.getId()
                                + "，qaProcessId=" + qaProcess.getId());
            }
            List<String> inspectionTypes = processItems.stream()
                    .map(MesQaInspectionRegulationItemDO::getInspectionType)
                    .map(MesTeamLeaderActiveOrderServiceImpl::normalizeInspectionType)
                    .distinct()
                    .sorted(Comparator.comparingInt(MesTeamLeaderActiveOrderServiceImpl::inspectionTypeOrder))
                    .toList();
            for (String inspectionType : inspectionTypes) {
                Integer plannedQuantity = resolveInspectionQuantity(activeOrder, processItems, inspectionType);
                tasks.add(buildPqcTask(activeOrder, qaProcess, version, inspectionType, businessDate,
                        inspectionType, plannedQuantity));
            }
        }
        for (MesPqcInspectionTaskDO task : tasks) {
            insertPqcInspectionTask(task);
        }
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
                                                String inspectionType,
                                                LocalDate businessDate,
                                                String shiftCode,
                                                Integer plannedInspectionQuantity) {
        return MesPqcInspectionTaskDO.builder()
                .activeOrderId(activeOrder.getId())
                .workOrderId(activeOrder.getWorkOrderId())
                .routeId(activeOrder.getRouteId())
                .routeVersionId(activeOrder.getRouteVersionId())
                .qaProcessId(qaProcess.getId())
                .regulationVersionId(version.getId())
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
        MesPqcInspectionTaskDO existing = pqcInspectionTaskMapper.selectByQaIdentity(task.getActiveOrderId(),
                task.getRegulationVersionId(), task.getQaProcessId(), task.getInspectionType(),
                task.getBusinessDate(), task.getShiftCode(), task.getRoundNo());
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

    private record ActiveOrderProcessIdentity(Long activeOrderId, Long routeProcessId, Long processId) {

        private boolean complete() {
            return activeOrderId != null && routeProcessId != null && processId != null;
        }
    }

    private record CandidateEligibility(boolean eligible, String ineligibleReason) {
    }

    private record ActiveOrderRouteSource(Long routeId, Long routeVersionId, Long dccProjectCodeId,
                                          MesQaInspectionRegulationDO regulation,
                                          List<MesProScheduleOrderProcessDO> routeProcesses,
                                          List<MesQaInspectionRegulationProcessDO> qaProcesses) {
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
            Map<Long, List<DccProjectCodeDO>> dccProjectsByRouteId,
            Map<Long, List<MesQaInspectionRegulationDO>> regulationsByDccProjectCodeId,
            Map<Long, MesQaInspectionRegulationVersionDO> versionsById,
            Map<Long, List<MesQaInspectionRegulationProcessDO>> processesByVersionId,
            Map<Long, List<MesQaInspectionRegulationItemDO>> itemsByVersionId) {
    }
}
