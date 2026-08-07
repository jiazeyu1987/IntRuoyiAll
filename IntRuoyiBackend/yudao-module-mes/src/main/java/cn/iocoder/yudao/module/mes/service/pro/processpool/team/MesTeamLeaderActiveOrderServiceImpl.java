package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_IDENTITY_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_EFFECTIVE_SCHEDULE_UNIQUE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;

@Service
@Validated
public class MesTeamLeaderActiveOrderServiceImpl implements MesTeamLeaderActiveOrderService {

    static final String STATUS_ACTIVE = "ACTIVE";
    static final String STATUS_REMOVED = "REMOVED";
    private static final String PQC_STATUS_PENDING = "PENDING";
    private static final String INSPECTION_TYPE_FIRST = "FIRST";
    private static final String INSPECTION_TYPE_PATROL = "PATROL";
    private static final String INSPECTION_TYPE_FINAL = "FINAL";
    private static final String SHIFT_FIRST = "FIRST";
    private static final String SHIFT_AM = "AM";
    private static final String SHIFT_PM = "PM";
    private static final String SHIFT_FINAL = "FINAL";
    private static final int DEFAULT_ROUND_NO = 1;

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderService workOrderService;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    private final MesProScheduleOrderMapper scheduleOrderMapper;
    private final MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    private final MesProRouteProductMapper routeProductMapper;
    private final MesProRouteMapper routeMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesQaInspectionRegulationMapper inspectionRegulationMapper;
    private final MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper;
    private final MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    private final MesPqcInspectionTaskMapper pqcInspectionTaskMapper;

    public MesTeamLeaderActiveOrderServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                               MesProWorkOrderService workOrderService,
                                               MesProWorkOrderMapper workOrderMapper,
                                               MesProcessPoolTeamMaintenanceAuditMapper auditMapper,
                                               MesProScheduleOrderMapper scheduleOrderMapper,
                                               MesProScheduleOrderProcessMapper scheduleOrderProcessMapper,
                                               MesProRouteProductMapper routeProductMapper,
                                               MesProRouteMapper routeMapper,
                                               MesProRouteVersionMapper routeVersionMapper,
                                               MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
                                               MesQaInspectionRegulationMapper inspectionRegulationMapper,
                                               MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper,
                                               MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper,
                                               MesPqcInspectionTaskMapper pqcInspectionTaskMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderService = workOrderService;
        this.workOrderMapper = workOrderMapper;
        this.auditMapper = auditMapper;
        this.scheduleOrderMapper = scheduleOrderMapper;
        this.scheduleOrderProcessMapper = scheduleOrderProcessMapper;
        this.routeProductMapper = routeProductMapper;
        this.routeMapper = routeMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.processSnapshotMapper = processSnapshotMapper;
        this.inspectionRegulationMapper = inspectionRegulationMapper;
        this.inspectionRegulationVersionMapper = inspectionRegulationVersionMapper;
        this.inspectionRegulationItemMapper = inspectionRegulationItemMapper;
        this.pqcInspectionTaskMapper = pqcInspectionTaskMapper;
    }

    @Override
    public List<MesTeamLeaderActiveOrderCandidateBO> searchActiveOrderCandidates(String keyword) {
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectConfirmedCandidatesByCode(keyword, 20);
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
        List<Long> workOrderIds = workOrders.stream()
                .map(MesProWorkOrderDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (workOrderIds.isEmpty()) {
            return emptyCandidateEligibilityContext();
        }
        List<MesProScheduleOrderDO> scheduleOrders =
                scheduleOrderMapper.selectEffectiveListByWorkOrderIds(workOrderIds);
        Map<Long, List<MesProScheduleOrderDO>> schedulesByWorkOrderId = scheduleOrders.stream()
                .filter(scheduleOrder -> scheduleOrder.getWorkOrderId() != null)
                .collect(Collectors.groupingBy(MesProScheduleOrderDO::getWorkOrderId));
        List<Long> scheduleOrderIds = scheduleOrders.stream()
                .map(MesProScheduleOrderDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, List<MesProScheduleOrderProcessDO>> processesByScheduleOrderId = scheduleOrderIds.isEmpty()
                ? Collections.emptyMap()
                : scheduleOrderProcessMapper.selectListByScheduleOrderIds(scheduleOrderIds).stream()
                .filter(process -> process.getScheduleOrderId() != null)
                .collect(Collectors.groupingBy(MesProScheduleOrderProcessDO::getScheduleOrderId));
        Map<Long, RouteSourceResolution> unscheduledRouteResolutions =
                buildUnscheduledRouteResolutions(workOrders, schedulesByWorkOrderId);
        List<Long> productIds = Stream.concat(
                        workOrders.stream().map(MesProWorkOrderDO::getProductId),
                        scheduleOrders.stream().map(MesProScheduleOrderDO::getProductId))
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
        return new CandidateEligibilityContext(schedulesByWorkOrderId, processesByScheduleOrderId,
                unscheduledRouteResolutions,
                regulationsByKey, versionsById, itemsByVersionId);
    }

    private Map<Long, RouteSourceResolution> buildUnscheduledRouteResolutions(
            List<MesProWorkOrderDO> workOrders,
            Map<Long, List<MesProScheduleOrderDO>> schedulesByWorkOrderId) {
        List<MesProWorkOrderDO> unscheduledWorkOrders = workOrders.stream()
                .filter(workOrder -> workOrder.getId() != null)
                .filter(workOrder -> schedulesByWorkOrderId
                        .getOrDefault(workOrder.getId(), Collections.emptyList()).isEmpty())
                .toList();
        if (unscheduledWorkOrders.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> productIds = unscheduledWorkOrders.stream()
                .map(MesProWorkOrderDO::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, List<MesProRouteProductDO>> routeProductsByProductId = productIds.isEmpty()
                ? Collections.emptyMap()
                : routeProductMapper.selectListByItemIds(productIds).stream()
                .filter(binding -> binding.getItemId() != null)
                .collect(Collectors.groupingBy(MesProRouteProductDO::getItemId));
        List<Long> routeIds = routeProductsByProductId.values().stream()
                .flatMap(Collection::stream)
                .map(MesProRouteProductDO::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, List<MesProRouteVersionDO>> activeVersionsByRouteId = routeIds.isEmpty()
                ? Collections.emptyMap()
                : routeVersionMapper.selectListByRouteIds(routeIds).stream()
                .filter(version -> version.getRouteId() != null)
                .filter(version -> Boolean.TRUE.equals(version.getActive()))
                .filter(version -> Objects.equals(MesProRouteVersionMapper.STATUS_ACTIVE,
                        version.getLifecycleStatus()))
                .collect(Collectors.groupingBy(MesProRouteVersionDO::getRouteId));
        return unscheduledWorkOrders.stream().collect(Collectors.toMap(
                MesProWorkOrderDO::getId,
                workOrder -> resolveUnscheduledRouteSource(workOrder, routeProductsByProductId,
                        activeVersionsByRouteId),
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private RouteSourceResolution resolveUnscheduledRouteSource(
            MesProWorkOrderDO workOrder,
            Map<Long, List<MesProRouteProductDO>> routeProductsByProductId,
            Map<Long, List<MesProRouteVersionDO>> activeVersionsByRouteId) {
        if (workOrder.getProductId() == null) {
            return routeSourceFailure("缺少产品ID", RouteSourceFailureType.ROUTE);
        }
        List<MesProRouteProductDO> routeProducts = routeProductsByProductId
                .getOrDefault(workOrder.getProductId(), Collections.emptyList());
        if (routeProducts.isEmpty()) {
            return routeSourceFailure("缺少产品正式工艺路线绑定", RouteSourceFailureType.ROUTE);
        }
        if (routeProducts.size() > 1) {
            return routeSourceFailure("产品正式工艺路线绑定不唯一", RouteSourceFailureType.ROUTE);
        }
        Long routeId = routeProducts.get(0).getRouteId();
        if (routeId == null) {
            return routeSourceFailure("产品正式工艺路线绑定缺少路线ID", RouteSourceFailureType.ROUTE);
        }
        List<MesProRouteVersionDO> activeVersions = activeVersionsByRouteId
                .getOrDefault(routeId, Collections.emptyList());
        if (activeVersions.isEmpty()) {
            return routeSourceFailure("产品工艺路线缺少当前ACTIVE版本", RouteSourceFailureType.ROUTE);
        }
        if (activeVersions.size() > 1) {
            return routeSourceFailure("产品工艺路线ACTIVE版本不唯一", RouteSourceFailureType.ROUTE);
        }
        MesProRouteVersionDO activeVersion = activeVersions.get(0);
        if (activeVersion.getId() == null || !Objects.equals(routeId, activeVersion.getRouteId())) {
            return routeSourceFailure("产品工艺路线ACTIVE版本身份无效", RouteSourceFailureType.ROUTE);
        }
        return parseUnscheduledRouteSnapshot(workOrder, routeId, activeVersion);
    }

    private RouteSourceResolution parseUnscheduledRouteSnapshot(MesProWorkOrderDO workOrder, Long routeId,
                                                                 MesProRouteVersionDO activeVersion) {
        JSONObject snapshot;
        try {
            snapshot = JSON.parseObject(activeVersion.getRouteSnapshotJson());
        } catch (RuntimeException ex) {
            return routeSourceFailure("产品工艺路线ACTIVE版本快照格式无效", RouteSourceFailureType.PROCESS);
        }
        if (snapshot == null || !(snapshot.get("configSnapshots") instanceof JSONObject configSnapshots)) {
            return routeSourceFailure("产品工艺路线ACTIVE版本快照缺少配置快照", RouteSourceFailureType.PROCESS);
        }
        if (!(configSnapshots.get("flowGraph") instanceof JSONObject flowGraph)
                || !(flowGraph.get("nodes") instanceof JSONArray nodeArray)
                || nodeArray.isEmpty()) {
            return routeSourceFailure("产品工艺路线ACTIVE版本快照缺少流程工序", RouteSourceFailureType.PROCESS);
        }
        if (!(configSnapshots.get("scheduleUseConfigs") instanceof JSONArray scheduleUseConfigs)
                || scheduleUseConfigs.isEmpty()) {
            return routeSourceFailure("产品工艺路线ACTIVE版本快照缺少排产用途工序配置",
                    RouteSourceFailureType.PROCESS);
        }
        Map<Long, JSONObject> nodesByRouteProcessId = new LinkedHashMap<>();
        List<JSONObject> orderedNodes = new ArrayList<>();
        for (Object value : nodeArray) {
            if (!(value instanceof JSONObject node)) {
                return routeSourceFailure("产品工艺路线ACTIVE版本流程工序格式无效",
                        RouteSourceFailureType.PROCESS);
            }
            Long routeProcessId = node.getLong("routeProcessId");
            Long processId = node.getLong("processId");
            Integer sort = node.getInteger("sort");
            if (routeProcessId == null || processId == null || sort == null) {
                return routeSourceFailure("产品工艺路线ACTIVE版本流程工序身份不完整",
                        RouteSourceFailureType.PROCESS);
            }
            if (nodesByRouteProcessId.put(routeProcessId, node) != null) {
                return routeSourceFailure("产品工艺路线ACTIVE版本流程工序重复",
                        RouteSourceFailureType.PROCESS);
            }
            orderedNodes.add(node);
        }
        Map<Long, JSONObject> configByRouteProcessId = new LinkedHashMap<>();
        for (Object value : scheduleUseConfigs) {
            if (!(value instanceof JSONObject config)) {
                return routeSourceFailure("产品工艺路线ACTIVE版本排产用途工序配置格式无效",
                        RouteSourceFailureType.PROCESS);
            }
            Long configRouteId = config.getLong("routeId");
            Long routeProcessId = config.getLong("routeProcessId");
            String useType = config.getString("useType");
            Boolean enabled = config.getBoolean("enabled");
            if (!Objects.equals(routeId, configRouteId) || routeProcessId == null
                    || !Objects.equals("SCHEDULE", useType) || enabled == null) {
                return routeSourceFailure("产品工艺路线ACTIVE版本排产用途工序配置身份不完整",
                        RouteSourceFailureType.PROCESS);
            }
            if (configByRouteProcessId.put(routeProcessId, config) != null) {
                return routeSourceFailure("产品工艺路线ACTIVE版本排产用途工序配置重复",
                        RouteSourceFailureType.PROCESS);
            }
            if (!nodesByRouteProcessId.containsKey(routeProcessId)) {
                return routeSourceFailure("产品工艺路线ACTIVE版本排产用途工序未匹配流程工序",
                        RouteSourceFailureType.PROCESS);
            }
        }
        if (!configByRouteProcessId.keySet().equals(nodesByRouteProcessId.keySet())) {
            return routeSourceFailure("产品工艺路线ACTIVE版本流程工序缺少排产用途配置",
                    RouteSourceFailureType.PROCESS);
        }
        if (workOrder.getPlannedStartTime() == null) {
            return routeSourceFailure("ERP计划开工时间缺失", RouteSourceFailureType.BUSINESS_DATE);
        }
        orderedNodes.sort(Comparator.comparing(node -> node.getInteger("sort")));
        List<MesProScheduleOrderProcessDO> enabledProcesses = new ArrayList<>();
        for (JSONObject node : orderedNodes) {
            Long routeProcessId = node.getLong("routeProcessId");
            JSONObject config = configByRouteProcessId.get(routeProcessId);
            if (!Boolean.TRUE.equals(config.getBoolean("enabled"))) {
                continue;
            }
            BigDecimal factor = config.getBigDecimal("productionQuantityFactor");
            if (!positive(factor)) {
                return routeSourceFailure("产品工艺路线ACTIVE版本工序数量系数无效",
                        RouteSourceFailureType.PROCESS);
            }
            BigDecimal normalizedFactor = factor.setScale(6, RoundingMode.HALF_UP);
            BigDecimal plannedQuantity = workOrder.getQuantity().multiply(normalizedFactor)
                    .setScale(6, RoundingMode.HALF_UP);
            enabledProcesses.add(MesProScheduleOrderProcessDO.builder()
                    .routeProcessId(routeProcessId)
                    .routeVersionId(activeVersion.getId())
                    .processId(node.getLong("processId"))
                    .sort(node.getInteger("sort"))
                    .enabled(Boolean.TRUE)
                    .productionQuantityFactor(normalizedFactor)
                    .plannedQuantity(plannedQuantity)
                    .planDate(workOrder.getPlannedStartTime().toLocalDate())
                    .build());
        }
        if (enabledProcesses.isEmpty()) {
            return routeSourceFailure("产品工艺路线ACTIVE版本没有启用工序", RouteSourceFailureType.PROCESS);
        }
        return new RouteSourceResolution(new ActiveOrderRouteSource(routeId, activeVersion.getId(),
                workOrder.getProductId(), enabledProcesses, null), null, null);
    }

    private static RouteSourceResolution routeSourceFailure(String reason, RouteSourceFailureType failureType) {
        return new RouteSourceResolution(null, reason, failureType);
    }

    private static CandidateEligibilityContext emptyCandidateEligibilityContext() {
        return new CandidateEligibilityContext(Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
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
        BigDecimal erpFixedQuantity = workOrder.getQuantity();
        if (erpFixedQuantity == null || erpFixedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return blockedCandidate("ERP生产数量无效");
        }
        RouteSourceResolution resolution = resolveCandidateRouteSource(workOrder, context);
        if (resolution.source() == null) {
            return blockedCandidate(resolution.ineligibleReason());
        }
        ActiveOrderRouteSource source = resolution.source();
        for (MesProScheduleOrderProcessDO process : source.enabledProcesses()) {
            String snapshotReason = validateCandidateProcessSnapshot(process, erpFixedQuantity);
            if (snapshotReason != null) {
                return blockedCandidate(snapshotReason);
            }
        }
        for (MesProScheduleOrderProcessDO process : source.enabledProcesses()) {
            String pqcReason = validateCandidatePqcPrerequisites(source.routeId(), source.routeVersionId(),
                    process, source.productId(), context);
            if (pqcReason != null) {
                return blockedCandidate(pqcReason);
            }
        }
        return new CandidateEligibility(true, null);
    }

    private RouteSourceResolution resolveCandidateRouteSource(MesProWorkOrderDO workOrder,
                                                              CandidateEligibilityContext context) {
        List<MesProScheduleOrderDO> scheduleOrders = context.schedulesByWorkOrderId()
                .getOrDefault(workOrder.getId(), Collections.emptyList());
        if (scheduleOrders.size() > 1) {
            return routeSourceFailure("有效排产工单不唯一", RouteSourceFailureType.SCHEDULE_CONFLICT);
        }
        if (scheduleOrders.isEmpty()) {
            return context.unscheduledRouteResolutions().getOrDefault(workOrder.getId(),
                    routeSourceFailure("缺少产品正式工艺路线绑定", RouteSourceFailureType.ROUTE));
        }
        MesProScheduleOrderDO scheduleOrder = scheduleOrders.get(0);
        if (scheduleOrder.getId() == null || scheduleOrder.getRouteId() == null
                || scheduleOrder.getRouteVersionId() == null) {
            return routeSourceFailure("有效排产缺少正式路线/路线版本", RouteSourceFailureType.ROUTE);
        }
        List<MesProScheduleOrderProcessDO> enabledProcesses = context.processesByScheduleOrderId()
                .getOrDefault(scheduleOrder.getId(), Collections.emptyList()).stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .toList();
        if (enabledProcesses.isEmpty()) {
            return routeSourceFailure("缺少启用排产工序", RouteSourceFailureType.PROCESS);
        }
        CandidateProduct product = resolveCandidateProduct(workOrder, scheduleOrder);
        if (product.ineligibleReason() != null) {
            return routeSourceFailure(product.ineligibleReason(), RouteSourceFailureType.ROUTE);
        }
        return new RouteSourceResolution(new ActiveOrderRouteSource(scheduleOrder.getRouteId(),
                scheduleOrder.getRouteVersionId(), product.productId(), enabledProcesses, scheduleOrder), null, null);
    }

    private String validateCandidateProcessSnapshot(MesProScheduleOrderProcessDO process,
                                                    BigDecimal erpFixedQuantity) {
        if (process == null || process.getRouteProcessId() == null || process.getProcessId() == null) {
            return "排产工序缺少路线工序/工序";
        }
        if (!positive(process.getProductionQuantityFactor())) {
            return "排产工序数量系数无效";
        }
        if (!positive(process.getPlannedQuantity())) {
            return "排产工序计划数量无效";
        }
        BigDecimal factor = process.getProductionQuantityFactor().setScale(6, RoundingMode.HALF_UP);
        BigDecimal plannedQuantity = process.getPlannedQuantity().setScale(6, RoundingMode.HALF_UP);
        BigDecimal expectedPlannedQuantity = erpFixedQuantity.multiply(factor).setScale(6, RoundingMode.HALF_UP);
        if (plannedQuantity.compareTo(expectedPlannedQuantity) != 0) {
            return "排产工序计划数量与ERP数量不匹配";
        }
        return null;
    }

    private CandidateProduct resolveCandidateProduct(MesProWorkOrderDO workOrder,
                                                     MesProScheduleOrderDO scheduleOrder) {
        Long workOrderProductId = workOrder.getProductId();
        Long scheduleProductId = scheduleOrder.getProductId();
        if (workOrderProductId != null && scheduleProductId != null
                && !Objects.equals(workOrderProductId, scheduleProductId)) {
            return new CandidateProduct(null, "工单产品与排产产品不一致");
        }
        Long productId = scheduleProductId != null ? scheduleProductId : workOrderProductId;
        if (productId == null) {
            return new CandidateProduct(null, "缺少产品ID");
        }
        return new CandidateProduct(productId, null);
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
        if (version.getFinalInspectionApplicable() == null) {
            return "QA规程发布版本缺少末检适用性配置";
        }
        if (Boolean.FALSE.equals(version.getFinalInspectionApplicable())
                && (version.getFinalInspectionNotApplicableReason() == null
                || version.getFinalInspectionNotApplicableReason().trim().isEmpty())) {
            return "QA规程发布版本缺少末检不适用依据";
        }
        List<MesQaInspectionRegulationItemDO> items = context.itemsByVersionId()
                .getOrDefault(regulation.getCurrentVersionId(), Collections.emptyList());
        if (items == null || items.isEmpty()) {
            return "已发布QA规程缺少检验项目";
        }
        if (process.getPlanDate() == null) {
            return "排产工序缺少计划日期";
        }
        String firstReason = validateFixedInspectionQuantity(items, INSPECTION_TYPE_FIRST);
        if (firstReason != null) {
            return firstReason;
        }
        String patrolReason = validatePatrolInspectionQuantity(process, items);
        if (patrolReason != null) {
            return patrolReason;
        }
        if (Boolean.TRUE.equals(version.getFinalInspectionApplicable())) {
            return validateFixedInspectionQuantity(items, INSPECTION_TYPE_FINAL);
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
            if (!positive(process.getPlannedQuantity())) {
                return "排产工序计划数量无效";
            }
            try {
                process.getPlannedQuantity().multiply(ratio).setScale(0, RoundingMode.CEILING).intValueExact();
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
        MesProWorkOrderDO workOrder = workOrderService.validateWorkOrderConfirmed(reqBO.getWorkOrderId());
        BigDecimal erpFixedQuantity = workOrder.getQuantity();
        if (!positive(erpFixedQuantity)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrder.erpFixedQuantitySnapshot");
        }
        ActiveOrderRouteSource routeSource = requireActiveOrderRouteSourceForAdd(workOrder);
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
        List<MesProScheduleOrderProcessDO> enabledProcesses = routeSource.scheduleOrder() == null
                ? routeSource.enabledProcesses()
                : selectEnabledScheduleProcesses(routeSource.scheduleOrder(), activeOrder.getId());
        insertProcessSnapshots(activeOrder, erpFixedQuantity, enabledProcesses);
        Long productId = routeSource.scheduleOrder() == null
                ? routeSource.productId()
                : requireProductId(workOrder, routeSource.scheduleOrder(), activeOrder.getId());
        insertPqcInspectionTasks(activeOrder, productId, enabledProcesses);
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
        return activeOrders.stream()
                .map(activeOrder -> toActiveOrderRow(activeOrder, routesById, routeVersionsById))
                .toList();
    }

    private void requireFormalRoute(MesProcessPoolActiveOrderDO activeOrder,
                                    Map<Long, MesProRouteDO> routesById) {
        MesProRouteDO route = routesById.get(activeOrder.getRouteId());
        if (route == null || route.getName() == null || route.getName().isBlank()) {
            throw exception(PRO_ROUTE_NOT_EXISTS);
        }
    }

    private MesTeamLeaderActiveOrderRow toActiveOrderRow(
            MesProcessPoolActiveOrderDO activeOrder,
            Map<Long, MesProRouteDO> routesById,
            Map<Long, MesProRouteVersionDO> routeVersionsById) {
        MesProRouteDO route = routesById.get(activeOrder.getRouteId());
        MesProRouteVersionDO routeVersion = routeVersionsById.get(activeOrder.getRouteVersionId());
        if (routeVersion == null
                || !Objects.equals(activeOrder.getRouteId(), routeVersion.getRouteId())
                || routeVersion.getVersionNo() == null
                || routeVersion.getVersionNo().isBlank()) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, activeOrder.getRouteVersionId());
        }
        return new MesTeamLeaderActiveOrderRow()
                .setId(activeOrder.getId())
                .setLeaderUserId(activeOrder.getLeaderUserId())
                .setWorkOrderId(activeOrder.getWorkOrderId())
                .setRouteId(activeOrder.getRouteId())
                .setRouteName(route.getName())
                .setRouteVersionId(activeOrder.getRouteVersionId())
                .setRouteVersionNo(routeVersion.getVersionNo())
                .setErpFixedQuantitySnapshot(activeOrder.getErpFixedQuantitySnapshot())
                .setActiveStatus(activeOrder.getActiveStatus())
                .setBusinessStatus(activeOrder.getBusinessStatus())
                .setJoinedAt(activeOrder.getJoinedAt())
                .setRemovedAt(activeOrder.getRemovedAt())
                .setVersion(activeOrder.getVersion());
    }

    private ActiveOrderRouteSource requireActiveOrderRouteSourceForAdd(MesProWorkOrderDO workOrder) {
        List<MesProScheduleOrderDO> scheduleOrders = scheduleOrderMapper
                .selectEffectiveListByWorkOrderIds(List.of(workOrder.getId()));
        if (scheduleOrders.size() > 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_EFFECTIVE_SCHEDULE_UNIQUE_REQUIRED, workOrder.getId());
        }
        if (scheduleOrders.isEmpty()) {
            RouteSourceResolution resolution = buildUnscheduledRouteResolutions(
                    List.of(workOrder), Collections.emptyMap()).get(workOrder.getId());
            if (resolution == null || resolution.source() == null) {
                throwRouteSourceResolutionFailure(workOrder.getId(), resolution);
            }
            return resolution.source();
        }
        MesProScheduleOrderDO scheduleOrder = scheduleOrders.get(0);
        if (scheduleOrder.getId() == null || scheduleOrder.getRouteId() == null
                || scheduleOrder.getRouteVersionId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED, workOrder.getId());
        }
        Long productId = scheduleOrder.getProductId() != null
                ? scheduleOrder.getProductId() : workOrder.getProductId();
        return new ActiveOrderRouteSource(scheduleOrder.getRouteId(), scheduleOrder.getRouteVersionId(),
                productId, Collections.emptyList(), scheduleOrder);
    }

    private void throwRouteSourceResolutionFailure(Long workOrderId, RouteSourceResolution resolution) {
        if (resolution == null || resolution.failureType() == RouteSourceFailureType.ROUTE) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED, workOrderId);
        }
        if (resolution.failureType() == RouteSourceFailureType.PROCESS) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, workOrderId);
        }
        if (resolution.failureType() == RouteSourceFailureType.SCHEDULE_CONFLICT) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_EFFECTIVE_SCHEDULE_UNIQUE_REQUIRED, workOrderId);
        }
        throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                resolution.ineligibleReason() + "，workOrderId=" + workOrderId);
    }

    private List<MesProScheduleOrderProcessDO> selectEnabledScheduleProcesses(MesProScheduleOrderDO scheduleOrder,
                                                                              Long activeOrderId) {
        List<MesProScheduleOrderProcessDO> enabledProcesses = scheduleOrderProcessMapper
                .selectListByScheduleOrderId(scheduleOrder.getId()).stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .toList();
        if (enabledProcesses.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return enabledProcesses;
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
            MesQaInspectionRegulationVersionDO version = requireRegulationVersion(regulation, activeOrder.getId());
            List<MesQaInspectionRegulationItemDO> items = requireRegulationItems(regulation, activeOrder.getId());
            LocalDate businessDate = requireBusinessDate(process, activeOrder.getId());
            tasks.add(buildPqcTask(activeOrder, process, regulation, INSPECTION_TYPE_FIRST, businessDate,
                    SHIFT_FIRST, resolveFixedInspectionQuantity(items, INSPECTION_TYPE_FIRST, activeOrder.getId())));
            tasks.add(buildPqcTask(activeOrder, process, regulation, INSPECTION_TYPE_PATROL, businessDate,
                    SHIFT_AM, resolvePatrolInspectionQuantity(process, items, activeOrder.getId())));
            tasks.add(buildPqcTask(activeOrder, process, regulation, INSPECTION_TYPE_PATROL, businessDate,
                    SHIFT_PM, resolvePatrolInspectionQuantity(process, items, activeOrder.getId())));
            if (Boolean.TRUE.equals(version.getFinalInspectionApplicable())) {
                tasks.add(buildPqcTask(activeOrder, process, regulation, INSPECTION_TYPE_FINAL, businessDate,
                        SHIFT_FINAL,
                        resolveFixedInspectionQuantity(items, INSPECTION_TYPE_FINAL, activeOrder.getId())));
            } else if (Boolean.FALSE.equals(version.getFinalInspectionApplicable())) {
                requireFinalInspectionNotApplicableReason(version, activeOrder.getId());
            }
        }
        for (MesPqcInspectionTaskDO task : tasks) {
            insertPqcInspectionTask(task);
        }
    }

    private Long requireProductId(MesProWorkOrderDO workOrder, MesProScheduleOrderDO scheduleOrder,
                                  Long activeOrderId) {
        Long workOrderProductId = workOrder.getProductId();
        Long scheduleProductId = scheduleOrder.getProductId();
        if (workOrderProductId != null && scheduleProductId != null
                && !Objects.equals(workOrderProductId, scheduleProductId)) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "工单产品与排产产品不一致，activeOrderId=" + activeOrderId);
        }
        Long productId = scheduleProductId != null ? scheduleProductId : workOrderProductId;
        if (productId == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "缺少产品ID，activeOrderId=" + activeOrderId);
        }
        return productId;
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
        if (version.getFinalInspectionApplicable() == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "QA规程发布版本缺少末检适用性配置，activeOrderId=" + activeOrderId
                            + "，regulationVersionId=" + version.getId());
        }
        if (Boolean.FALSE.equals(version.getFinalInspectionApplicable())) {
            requireFinalInspectionNotApplicableReason(version, activeOrderId);
        }
        return version;
    }

    private void requireFinalInspectionNotApplicableReason(MesQaInspectionRegulationVersionDO version,
                                                           Long activeOrderId) {
        if (version.getFinalInspectionNotApplicableReason() == null
                || version.getFinalInspectionNotApplicableReason().trim().isEmpty()) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "QA规程发布版本缺少末检不适用依据，activeOrderId=" + activeOrderId
                            + "，regulationVersionId=" + version.getId());
        }
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

    private LocalDate requireBusinessDate(MesProScheduleOrderProcessDO process, Long activeOrderId) {
        if (process.getPlanDate() == null) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "排产工序缺少计划日期，activeOrderId=" + activeOrderId
                            + "，routeProcessId=" + process.getRouteProcessId());
        }
        return process.getPlanDate();
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
        if (plannedQuantity == null || plannedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "排产工序计划数量无效，activeOrderId=" + activeOrderId);
        }
        try {
            return plannedQuantity.multiply(ratio).setScale(0, RoundingMode.CEILING).intValueExact();
        } catch (ArithmeticException ex) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "巡检计划数量超出整数范围，activeOrderId=" + activeOrderId);
        }
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
        BigDecimal factor = requirePositive(process.getProductionQuantityFactor(), activeOrder.getId())
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal plannedQuantity = requirePositive(process.getPlannedQuantity(), activeOrder.getId())
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

    private static BigDecimal requirePositive(BigDecimal value, Long activeOrderId) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return value;
    }

    private record CandidateEligibility(boolean eligible, String ineligibleReason) {
    }

    private record CandidateProduct(Long productId, String ineligibleReason) {
    }

    private record ActiveOrderRouteSource(Long routeId, Long routeVersionId, Long productId,
                                          List<MesProScheduleOrderProcessDO> enabledProcesses,
                                          MesProScheduleOrderDO scheduleOrder) {
    }

    private record RouteSourceResolution(ActiveOrderRouteSource source, String ineligibleReason,
                                         RouteSourceFailureType failureType) {
    }

    private enum RouteSourceFailureType {
        SCHEDULE_CONFLICT,
        ROUTE,
        PROCESS,
        BUSINESS_DATE
    }

    private record CandidateRegulationKey(Long productId, Long routeId, Long routeVersionId, Long routeProcessId,
                                          Long processId) {
    }

    private record CandidateEligibilityContext(
            Map<Long, List<MesProScheduleOrderDO>> schedulesByWorkOrderId,
            Map<Long, List<MesProScheduleOrderProcessDO>> processesByScheduleOrderId,
            Map<Long, RouteSourceResolution> unscheduledRouteResolutions,
            Map<CandidateRegulationKey, List<MesQaInspectionRegulationDO>> regulationsByKey,
            Map<Long, MesQaInspectionRegulationVersionDO> versionsById,
            Map<Long, List<MesQaInspectionRegulationItemDO>> itemsByVersionId) {
    }
}
