package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
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
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
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
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_IDENTITY_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_EFFECTIVE_SCHEDULE_UNIQUE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;

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
        this.processSnapshotMapper = processSnapshotMapper;
        this.inspectionRegulationMapper = inspectionRegulationMapper;
        this.inspectionRegulationVersionMapper = inspectionRegulationVersionMapper;
        this.inspectionRegulationItemMapper = inspectionRegulationItemMapper;
        this.pqcInspectionTaskMapper = pqcInspectionTaskMapper;
    }

    @Override
    public List<MesTeamLeaderActiveOrderCandidateBO> searchActiveOrderCandidates(String keyword) {
        return workOrderMapper.selectConfirmedCandidatesByCode(keyword, 20).stream()
                .map(workOrder -> MesTeamLeaderActiveOrderCandidateBO.builder()
                        .workOrderId(workOrder.getId())
                        .workOrderCode(workOrder.getCode())
                        .build())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addActiveOrder(MesTeamLeaderActiveOrderAddReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getWorkOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrder");
        }
        MesProWorkOrderDO workOrder = workOrderService.validateWorkOrderConfirmed(reqBO.getWorkOrderId());
        BigDecimal erpFixedQuantity = workOrder.getQuantity();
        if (erpFixedQuantity == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrder.erpFixedQuantitySnapshot");
        }
        MesProScheduleOrderDO scheduleOrder = requireUniqueEffectiveScheduleOrder(reqBO.getWorkOrderId());
        Long routeId = scheduleOrder.getRouteId();
        Long routeVersionId = scheduleOrder.getRouteVersionId();
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
        List<MesProScheduleOrderProcessDO> enabledProcesses = selectEnabledScheduleProcesses(scheduleOrder,
                activeOrder.getId());
        insertProcessSnapshots(activeOrder, erpFixedQuantity, enabledProcesses);
        insertPqcInspectionTasks(activeOrder, workOrder, scheduleOrder, enabledProcesses);
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
    public List<MesProcessPoolActiveOrderDO> listActiveOrders(Long leaderUserId) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderList");
        }
        return activeOrderMapper.selectActiveListByLeader(leaderUserId);
    }

    private MesProScheduleOrderDO requireUniqueEffectiveScheduleOrder(Long workOrderId) {
        List<MesProScheduleOrderDO> scheduleOrders = scheduleOrderMapper
                .selectEffectiveListByWorkOrderIds(List.of(workOrderId));
        if (scheduleOrders.size() != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_EFFECTIVE_SCHEDULE_UNIQUE_REQUIRED, workOrderId);
        }
        MesProScheduleOrderDO scheduleOrder = scheduleOrders.get(0);
        if (scheduleOrder.getId() == null || scheduleOrder.getRouteId() == null
                || scheduleOrder.getRouteVersionId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED, workOrderId);
        }
        return scheduleOrder;
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

    private void insertPqcInspectionTasks(MesProcessPoolActiveOrderDO activeOrder, MesProWorkOrderDO workOrder,
                                          MesProScheduleOrderDO scheduleOrder,
                                          List<MesProScheduleOrderProcessDO> enabledProcesses) {
        Long productId = requireProductId(workOrder, scheduleOrder, activeOrder.getId());
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
}
