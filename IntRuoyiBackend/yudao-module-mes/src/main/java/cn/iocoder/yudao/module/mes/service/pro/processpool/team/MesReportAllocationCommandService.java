package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_MODE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_RELEASED_LOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_CONFIRMATION_PRODUCTION_LEADER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED;

@Service
public class MesReportAllocationCommandService {

    private final MesTeamLeaderScopeService scopeService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolReportAllocationStateMapper stateMapper;
    private final MesProcessPoolReportAllocationAdjustmentAuditMapper auditMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesReportAllocationPoolQuantityService poolQuantityService;
    private final MesReportAllocationReleaseStateService releaseStateService;
    private final MesTeamLeaderOrderProcessTargetService targetService;
    private final MesTeamLeaderFifoAllocationService fifoService;
    private final MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService;
    private final MesReportAllocationQuantityFragmentService quantityFragmentService;
    private final MesTeamLeaderOrderProcessCompletionService completionService;
    private final MesProductionReportManagementSummaryService reportManagementSummaryService;

    @Resource
    private MesProBatchRecordExecutionSignatureService signatureService;

    public MesReportAllocationCommandService(
            MesTeamLeaderScopeService scopeService,
            MesProProcessPoolEventMapper eventMapper,
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolReportAllocationStateMapper stateMapper,
            MesProcessPoolReportAllocationAdjustmentAuditMapper auditMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesReportAllocationPoolQuantityService poolQuantityService,
            MesReportAllocationReleaseStateService releaseStateService,
            MesTeamLeaderOrderProcessTargetService targetService,
            MesTeamLeaderFifoAllocationService fifoService,
            MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService,
            MesReportAllocationQuantityFragmentService quantityFragmentService,
            MesTeamLeaderOrderProcessCompletionService completionService,
            MesProductionReportManagementSummaryService reportManagementSummaryService) {
        this.scopeService = scopeService;
        this.eventMapper = eventMapper;
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.allocationMapper = allocationMapper;
        this.stateMapper = stateMapper;
        this.auditMapper = auditMapper;
        this.reviewMapper = reviewMapper;
        this.poolQuantityService = poolQuantityService;
        this.releaseStateService = releaseStateService;
        this.targetService = targetService;
        this.fifoService = fifoService;
        this.routeStartAuthorizationService = routeStartAuthorizationService;
        this.quantityFragmentService = quantityFragmentService;
        this.completionService = completionService;
        this.reportManagementSummaryService = reportManagementSummaryService;
    }

    public MesReportAllocationSnapshot getCurrent(Long eventId, Long leaderUserId, String leaderType) {
        MesProProcessPoolEventDO event = requireEvent(eventId, false);
        assertScope(event, leaderUserId, leaderType);
        BigDecimal pool = poolQuantityService.requirePoolQuantity(event);
        return buildCurrentSnapshot(event, pool, allocationMapper.selectListByEventId(eventId));
    }

    public MesReportAllocationSnapshot previewFifo(Long eventId, Long leaderUserId, String leaderType) {
        MesProProcessPoolEventDO event = requireEvent(eventId, false);
        assertScope(event, leaderUserId, leaderType);
        BigDecimal pool = poolQuantityService.requirePoolQuantity(event);
        List<MesProcessPoolReportAllocationDO> current = allocationMapper.selectListByEventId(eventId);
        List<MesProcessPoolActiveOrderDO> activeOrders = activeOrderMapper.selectActiveListByLeader(leaderUserId)
                .stream().filter(order -> "ACTIVE".equals(order.getActiveStatus())).toList();
        Set<Long> releaseCandidates = activeOrders.stream().map(MesProcessPoolActiveOrderDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        current.stream().map(MesProcessPoolReportAllocationDO::getActiveOrderId).forEach(releaseCandidates::add);
        Set<Long> releasedIds = releaseStateService.findReleasedActiveOrderIds(releaseCandidates);
        List<MesProcessPoolReportAllocationDO> locked = current.stream()
                .filter(row -> releasedIds.contains(row.getActiveOrderId())).toList();
        BigDecimal lockedTotal = sumAllocations(locked);
        BigDecimal editablePool = pool.subtract(lockedTotal);
        if (editablePool.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH, quantityText(pool));
        }
        MesTeamLeaderReportAllocationPreview preview = editablePool.compareTo(BigDecimal.ZERO) == 0
                ? MesTeamLeaderReportAllocationPreview.builder().poolQuantity(BigDecimal.ZERO)
                .totalAllocatedQuantity(BigDecimal.ZERO).unallocatedQuantity(BigDecimal.ZERO).lines(List.of()).build()
                : fifoService.previewFifoAllocation(MesTeamLeaderFifoAllocationReqBO.builder()
                .eventId(eventId).leaderUserId(leaderUserId).routeProcessId(event.getRouteProcessId())
                .processId(event.getProcessId()).confirmQuantity(editablePool).excludedEventId(eventId)
                .excludedActiveOrderIds(releasedIds).build());
        List<MesReportAllocationSnapshotLine> lines = new ArrayList<>(toSnapshotLines(locked, releasedIds,
                calculateCurrentOverage(event, locked)));
        lines.addAll(preview.getLines().stream().map(line -> MesReportAllocationSnapshotLine.builder()
                .activeOrderId(line.getActiveOrderId()).workOrderId(line.getWorkOrderId())
                .workOrderCode(line.getWorkOrderCode()).routeProcessId(line.getRouteProcessId())
                .processId(line.getProcessId()).allocatedQuantity(line.getAllocatedQuantity())
                .overageQuantity(BigDecimal.ZERO).needsAdjustment(false)
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO).released(false).editable(true).build())
                .toList());
        BigDecimal editableAllocated = preview.getTotalAllocatedQuantity();
        return snapshot(eventId, currentVersion(eventId), pool, lockedTotal, editableAllocated, lines);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createInitialAllocation(Long eventId, Long activeOrderId, BigDecimal outputQuantity) {
        if (eventId == null || activeOrderId == null || outputQuantity == null
                || outputQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "frontlineInitialAllocation");
        }
        MesProProcessPoolEventDO event = requireEvent(eventId, true);
        if (event.getDeviceAccountId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "event.deviceAccountId");
        }
        BigDecimal pool = poolQuantityService.requirePoolQuantity(event);
        if (pool.compareTo(outputQuantity) != 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH, quantityText(pool));
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(activeOrderId);
        if (activeOrder == null || !"ACTIVE".equals(activeOrder.getActiveStatus())
                || !Objects.equals(event.getWorkOrderId(), activeOrder.getWorkOrderId())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, activeOrderId);
        }
        MesTeamLeaderOrderProcessTarget target = targetService.requireTarget(activeOrder,
                event.getRouteProcessId(), event.getProcessId());
        MesProcessPoolReportAllocationStateDO state = requireStateForUpdate(event, event.getDeviceAccountId());
        List<MesProcessPoolReportAllocationDO> current = allocationMapper.selectListByEventIdForUpdate(eventId);
        if (!current.isEmpty() || state.getCurrentVersion() == null || state.getCurrentVersion() != 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    eventId, 0, state.getCurrentVersion());
        }

        LocalDateTime now = LocalDateTime.now();
        MesProcessPoolReportAllocationDO allocation = MesProcessPoolReportAllocationDO.builder()
                .eventId(eventId).reviewId(null).leaderUserId(activeOrder.getLeaderUserId())
                .activeOrderId(activeOrderId).workOrderId(activeOrder.getWorkOrderId())
                .routeProcessId(target.routeProcessId()).processId(target.processId())
                .allocatedQuantity(outputQuantity)
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FRONTLINE_SELECTED)
                .lifecycleStatus(MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .createdVersion(1).confirmedAt(now).build();
        if (!Boolean.TRUE.equals(allocationMapper.insertBatch(List.of(allocation)))) {
            throw new IllegalStateException("Failed to insert frontline initial report allocation");
        }
        MesProcessPoolReportAllocationAdjustmentAuditDO audit =
                MesProcessPoolReportAllocationAdjustmentAuditDO.builder()
                        .eventId(eventId).allocationVersion(1).sourceAllocationId(allocation.getId())
                        .activeOrderId(activeOrderId).workOrderId(activeOrder.getWorkOrderId())
                        .routeProcessId(target.routeProcessId()).processId(target.processId())
                        .beforeQuantity(BigDecimal.ZERO).afterQuantity(outputQuantity).deltaQuantity(outputQuantity)
                        .actorUserId(event.getDeviceAccountId())
                        .adjustmentReason("一线生产选择活跃订单后自动分配")
                        .allocationMode(MesProcessPoolReportAllocationDO.MODE_FRONTLINE_SELECTED)
                        .changeSource(MesProcessPoolReportAllocationAdjustmentAuditDO.SOURCE_INITIAL_BASELINE)
                        .occurredAt(now).build();
        if (!Boolean.TRUE.equals(auditMapper.insertBatch(List.of(audit)))) {
            throw new IllegalStateException("Failed to insert frontline initial allocation audit");
        }
        state.setCurrentVersion(1).setLastIdempotencyKey(null)
                .setLastRequestHash(null).setLastChangedBy(event.getDeviceAccountId()).setLastChangedAt(now);
        if (stateMapper.updateById(state) != 1) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT, eventId, 0, 1);
        }
        quantityFragmentService.rebuildForVersion(event, 1, List.of(allocation));
        completionService.reconcileAffectedAllocations(event, List.of(allocation));
        reportManagementSummaryService.refreshProductionEvent(event);
    }

    @Transactional(rollbackFor = Exception.class)
    public MesReportAllocationSnapshot save(MesReportAllocationSaveCommand command) {
        validateCommand(command);
        MesProProcessPoolEventDO event = requireEvent(command.getEventId(), true);
        assertScope(event, command.getLeaderUserId(), command.getLeaderType());
        BigDecimal pool = poolQuantityService.requirePoolQuantity(event);
        MesProcessPoolReportAllocationStateDO state = requireStateForUpdate(event, command.getLeaderUserId());
        List<MesProcessPoolReportAllocationDO> current = allocationMapper.selectListByEventIdForUpdate(event.getId());
        String requestHash = requestHash(command);
        if (StrUtil.isNotBlank(command.getIdempotencyKey())
                && Objects.equals(command.getIdempotencyKey(), state.getLastIdempotencyKey())) {
            if (!Objects.equals(requestHash, state.getLastRequestHash())) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                        event.getId(), command.getExpectedVersion(), state.getCurrentVersion());
            }
            return buildSnapshot(event, pool, state.getCurrentVersion(), current);
        }
        if (command.getExpectedVersion() != null
                && !Objects.equals(command.getExpectedVersion(), state.getCurrentVersion())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    event.getId(), command.getExpectedVersion(), state.getCurrentVersion());
        }

        List<MesProcessPoolActiveOrderDO> activeOrders = activeOrderMapper
                .selectActiveListByLeaderForUpdate(command.getLeaderUserId()).stream()
                .filter(order -> "ACTIVE".equals(order.getActiveStatus())).toList();
        Map<Long, MesProcessPoolActiveOrderDO> activeById = activeOrders.stream().collect(Collectors.toMap(
                MesProcessPoolActiveOrderDO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, BigDecimal> desired = aggregateDesired(command.getAllocations(), activeById, event.getId());
        Set<Long> releaseCandidates = new LinkedHashSet<>(activeById.keySet());
        current.stream().map(MesProcessPoolReportAllocationDO::getActiveOrderId).forEach(releaseCandidates::add);
        Set<Long> releasedIds = releaseStateService.findReleasedActiveOrderIdsForUpdate(releaseCandidates);
        for (Long activeOrderId : desired.keySet()) {
            if (releasedIds.contains(activeOrderId)) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_RELEASED_LOCKED, activeOrderId);
            }
        }
        List<MesProcessPoolReportAllocationDO> locked = current.stream()
                .filter(row -> releasedIds.contains(row.getActiveOrderId())).toList();
        List<MesProcessPoolReportAllocationDO> editableOld = current.stream()
                .filter(row -> !releasedIds.contains(row.getActiveOrderId())).toList();
        BigDecimal lockedTotal = sumAllocations(locked);
        BigDecimal availablePool = pool.subtract(lockedTotal);
        if (availablePool.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH, quantityText(pool));
        }
        Map<Long, MesProWorkOrderDO> workOrders = loadWorkOrders(activeOrders);
        AllocationValidation validation = validateAllocationTargets(event, desired, activeById, workOrders);
        Map<Long, MesTeamLeaderOrderProcessTarget> targets = validation.targets();
        BigDecimal desiredTotal = desired.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lockedTotal.add(desiredTotal).compareTo(pool) > 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH, quantityText(pool));
        }

        Map<Long, BigDecimal> before = aggregateRows(editableOld);
        if (before.equals(desired)) {
            if (!current.isEmpty()) {
                completionService.reconcileAffectedAllocations(event, current);
            }
            return buildSnapshot(event, pool, state.getCurrentVersion(), current,
                    validation.overageByActiveOrderId());
        }

        int newVersion = state.getCurrentVersion() + 1;
        Long reviewId = requireReview(event, command);
        List<Long> oldIds = editableOld.stream().map(MesProcessPoolReportAllocationDO::getId)
                .filter(Objects::nonNull).toList();
        if (!oldIds.isEmpty() && allocationMapper.supersedeCurrentRows(oldIds, newVersion) != oldIds.size()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    event.getId(), command.getExpectedVersion(), state.getCurrentVersion());
        }
        LocalDateTime now = LocalDateTime.now();
        List<MesProcessPoolReportAllocationDO> inserted = desired.entrySet().stream().map(entry -> {
            MesProcessPoolActiveOrderDO order = activeById.get(entry.getKey());
            MesTeamLeaderOrderProcessTarget target = targets.get(entry.getKey());
            return MesProcessPoolReportAllocationDO.builder()
                    .eventId(event.getId()).reviewId(reviewId).leaderUserId(command.getLeaderUserId())
                    .activeOrderId(order.getId()).workOrderId(order.getWorkOrderId())
                    .routeProcessId(target.routeProcessId()).processId(target.processId())
                    .allocatedQuantity(entry.getValue()).allocationMode(command.getAllocationMode())
                    .lifecycleStatus(MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                    .createdVersion(newVersion).confirmedAt(now).build();
        }).toList();
        if (!inserted.isEmpty() && !Boolean.TRUE.equals(allocationMapper.insertBatch(inserted))) {
            throw new IllegalStateException("Failed to insert report allocation version");
        }
        List<MesProcessPoolReportAllocationDO> next = new ArrayList<>(locked);
        next.addAll(inserted);
        quantityFragmentService.rebuildForVersion(event, newVersion, next);
        List<MesProcessPoolReportAllocationDO> affected = new ArrayList<>(editableOld);
        affected.addAll(inserted);
        completionService.reconcileAffectedAllocations(event, affected);
        insertAudits(event, command, newVersion, before, desired, editableOld, activeById, targets, now);
        state.setCurrentVersion(newVersion).setLastIdempotencyKey(command.getIdempotencyKey())
                .setLastRequestHash(requestHash).setLastChangedBy(command.getLeaderUserId()).setLastChangedAt(now);
        if (stateMapper.updateById(state) != 1) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    event.getId(), command.getExpectedVersion(), state.getCurrentVersion());
        }
        reportManagementSummaryService.refreshProductionEvent(event);
        return buildSnapshot(event, pool, newVersion, next, validation.overageByActiveOrderId());
    }

    public List<MesProcessPoolReportAllocationAdjustmentAuditDO> listAudit(
            Long eventId, Long leaderUserId, String leaderType) {
        MesProProcessPoolEventDO event = requireEvent(eventId, false);
        assertScope(event, leaderUserId, leaderType);
        return auditMapper.selectListByEventId(eventId);
    }

    private AllocationValidation validateAllocationTargets(
            MesProProcessPoolEventDO event, Map<Long, BigDecimal> desired,
            Map<Long, MesProcessPoolActiveOrderDO> activeById, Map<Long, MesProWorkOrderDO> workOrders) {
        if (desired.isEmpty()) {
            return new AllocationValidation(Map.of(), Map.of());
        }
        Map<Long, BigDecimal> allocatedElsewhere = allocationMapper
                .selectListByActiveOrderIdsAndProcessForUpdate(desired.keySet(), event.getProcessId()).stream()
                .filter(row -> !Objects.equals(row.getEventId(), event.getId()))
                .collect(Collectors.groupingBy(MesProcessPoolReportAllocationDO::getActiveOrderId,
                        LinkedHashMap::new, Collectors.reducing(BigDecimal.ZERO,
                                MesProcessPoolReportAllocationDO::getAllocatedQuantity, BigDecimal::add)));
        Map<Long, MesTeamLeaderOrderProcessTarget> targets = new LinkedHashMap<>();
        Map<Long, BigDecimal> overageByActiveOrderId = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : desired.entrySet()) {
            MesProcessPoolActiveOrderDO order = activeById.get(entry.getKey());
            MesProWorkOrderDO workOrder = workOrders.get(order.getWorkOrderId());
            if (workOrder == null || workOrder.getQuantity() == null
                    || workOrder.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, order.getWorkOrderId());
            }
            MesTeamLeaderOrderProcessTarget target = targetService.requireUniqueTargetForProcess(order,
                    event.getProcessId());
            targets.put(order.getId(), target);
            BigDecimal totalForOrder = allocatedElsewhere.getOrDefault(order.getId(), BigDecimal.ZERO)
                    .add(entry.getValue());
            BigDecimal overage = totalForOrder.subtract(target.plannedQuantity()).max(BigDecimal.ZERO);
            overageByActiveOrderId.put(order.getId(), overage);
        }
        return new AllocationValidation(targets, overageByActiveOrderId);
    }

    private void insertAudits(MesProProcessPoolEventDO event, MesReportAllocationSaveCommand command,
                              int version, Map<Long, BigDecimal> before, Map<Long, BigDecimal> after,
                              List<MesProcessPoolReportAllocationDO> oldRows,
                              Map<Long, MesProcessPoolActiveOrderDO> activeById,
                              Map<Long, MesTeamLeaderOrderProcessTarget> targets, LocalDateTime occurredAt) {
        Map<Long, MesProcessPoolReportAllocationDO> sourceByActive = oldRows.stream().collect(Collectors.toMap(
                MesProcessPoolReportAllocationDO::getActiveOrderId, Function.identity(), (a, b) -> a,
                LinkedHashMap::new));
        Set<Long> ids = new LinkedHashSet<>(before.keySet());
        ids.addAll(after.keySet());
        List<MesProcessPoolReportAllocationAdjustmentAuditDO> audits = ids.stream()
                .filter(id -> before.getOrDefault(id, BigDecimal.ZERO)
                        .compareTo(after.getOrDefault(id, BigDecimal.ZERO)) != 0)
                .map(id -> {
                    MesProcessPoolReportAllocationDO source = sourceByActive.get(id);
                    MesProcessPoolActiveOrderDO order = activeById.get(id);
                    MesTeamLeaderOrderProcessTarget target = targets.get(id);
                    return MesProcessPoolReportAllocationAdjustmentAuditDO.builder()
                            .eventId(event.getId()).allocationVersion(version)
                            .sourceAllocationId(source == null ? null : source.getId()).activeOrderId(id)
                            .workOrderId(order != null ? order.getWorkOrderId() : source.getWorkOrderId())
                            .routeProcessId(target != null ? target.routeProcessId() : source.getRouteProcessId())
                            .processId(event.getProcessId())
                            .beforeQuantity(before.getOrDefault(id, BigDecimal.ZERO))
                            .afterQuantity(after.getOrDefault(id, BigDecimal.ZERO))
                            .deltaQuantity(after.getOrDefault(id, BigDecimal.ZERO)
                                    .subtract(before.getOrDefault(id, BigDecimal.ZERO)))
                            .actorUserId(command.getLeaderUserId()).adjustmentReason(resolveAdjustmentReason(command))
                            .allocationMode(command.getAllocationMode()).changeSource(command.getAllocationMode())
                            .occurredAt(occurredAt).build();
                }).toList();
        if (!audits.isEmpty() && !Boolean.TRUE.equals(auditMapper.insertBatch(audits))) {
            throw new IllegalStateException("Failed to insert report allocation audits");
        }
    }

    private String resolveAdjustmentReason(MesReportAllocationSaveCommand command) {
        if (StrUtil.isNotBlank(command.getReason())) {
            return command.getReason();
        }
        return MesProcessPoolReportAllocationDO.MODE_FIFO.equals(command.getAllocationMode())
                ? "FIFO自动分配" : "手动分配";
    }

    private Long requireReview(MesProProcessPoolEventDO event, MesReportAllocationSaveCommand command) {
        MesProcessPoolSubmissionReviewDO review = reviewMapper.selectLatestByEventIdForUpdate(event.getId());
        if (review != null) {
            return review.getId();
        }
        Long signatureId = null;
        if (StrUtil.isNotBlank(command.getSignaturePassword())) {
            signatureId = signatureService.recordTeamLeaderReviewSignature(command.getLeaderUserId(),
                    command.getSignaturePassword(), "组长报工分配确认:PRODUCTION:" + event.getId());
        }
        review = MesProcessPoolSubmissionReviewDO.builder().eventId(event.getId())
                .leaderUserId(command.getLeaderUserId()).leaderType(command.getLeaderType())
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark(command.getReason()).reviewedAt(LocalDateTime.now()).reviewSignatureId(signatureId)
                .reviewSignatureUserId(signatureId == null ? null : command.getLeaderUserId()).build();
        reviewMapper.insert(review);
        return review.getId();
    }

    private MesProcessPoolReportAllocationStateDO requireStateForUpdate(
            MesProProcessPoolEventDO event, Long actorUserId) {
        MesProcessPoolReportAllocationStateDO state = stateMapper.selectByEventIdForUpdate(event.getId());
        if (state != null) {
            return state;
        }
        state = MesProcessPoolReportAllocationStateDO.builder().eventId(event.getId()).currentVersion(0)
                .lastChangedBy(actorUserId).lastChangedAt(LocalDateTime.now()).build();
        stateMapper.insert(state);
        return state;
    }

    private MesReportAllocationSnapshot buildCurrentSnapshot(MesProProcessPoolEventDO event, BigDecimal pool,
                                                              List<MesProcessPoolReportAllocationDO> current) {
        return buildSnapshot(event, pool, currentVersion(event.getId()), current);
    }

    private MesReportAllocationSnapshot buildSnapshot(MesProProcessPoolEventDO event, BigDecimal pool,
                                                       int version,
                                                       List<MesProcessPoolReportAllocationDO> current) {
        return buildSnapshot(event, pool, version, current, calculateCurrentOverage(event, current));
    }

    private MesReportAllocationSnapshot buildSnapshot(MesProProcessPoolEventDO event, BigDecimal pool,
                                                       int version,
                                                       List<MesProcessPoolReportAllocationDO> current,
                                                       Map<Long, BigDecimal> overageByActiveOrderId) {
        Set<Long> released = releaseStateService.findReleasedActiveOrderIds(current.stream()
                .map(MesProcessPoolReportAllocationDO::getActiveOrderId).distinct().toList());
        List<MesReportAllocationSnapshotLine> lines = toSnapshotLines(current, released, overageByActiveOrderId);
        BigDecimal releasedTotal = current.stream().filter(row -> released.contains(row.getActiveOrderId()))
                .map(MesProcessPoolReportAllocationDO::getAllocatedQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal editableTotal = sumAllocations(current).subtract(releasedTotal);
        return snapshot(event.getId(), version, pool, releasedTotal, editableTotal, lines);
    }

    private MesReportAllocationSnapshot snapshot(Long eventId, int version, BigDecimal pool,
                                                  BigDecimal releasedTotal, BigDecimal editableTotal,
                                                  List<MesReportAllocationSnapshotLine> lines) {
        BigDecimal total = releasedTotal.add(editableTotal);
        return MesReportAllocationSnapshot.builder().eventId(eventId).version(version).poolQuantity(pool)
                .releasedAllocatedQuantity(releasedTotal).editableAllocatedQuantity(editableTotal)
                .totalAllocatedQuantity(total).unallocatedQuantity(pool.subtract(total))
                .lines(List.copyOf(lines)).build();
    }

    private List<MesReportAllocationSnapshotLine> toSnapshotLines(
            List<MesProcessPoolReportAllocationDO> rows, Set<Long> releasedIds,
            Map<Long, BigDecimal> overageByActiveOrderId) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, MesProWorkOrderDO> workOrders = workOrderMapper.selectListByIds(rows.stream()
                .map(MesProcessPoolReportAllocationDO::getWorkOrderId).distinct().toList()).stream()
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, Function.identity(), (a, b) -> a));
        return rows.stream().sorted(Comparator.comparing(MesProcessPoolReportAllocationDO::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .map(row -> {
                    boolean released = releasedIds.contains(row.getActiveOrderId());
                    BigDecimal overage = overageByActiveOrderId.getOrDefault(
                            row.getActiveOrderId(), BigDecimal.ZERO);
                    MesProWorkOrderDO workOrder = workOrders.get(row.getWorkOrderId());
                    return MesReportAllocationSnapshotLine.builder().allocationId(row.getId())
                            .activeOrderId(row.getActiveOrderId()).workOrderId(row.getWorkOrderId())
                            .workOrderCode(workOrder == null ? null : workOrder.getCode())
                            .routeProcessId(row.getRouteProcessId()).processId(row.getProcessId())
                            .allocatedQuantity(row.getAllocatedQuantity()).allocationMode(row.getAllocationMode())
                            .overageQuantity(overage).needsAdjustment(overage.compareTo(BigDecimal.ZERO) > 0)
                            .released(released).editable(!released).build();
                }).toList();
    }

    private Map<Long, BigDecimal> calculateCurrentOverage(
            MesProProcessPoolEventDO event, List<MesProcessPoolReportAllocationDO> current) {
        if (current.isEmpty()) {
            return Map.of();
        }
        Set<Long> activeOrderIds = current.stream().map(MesProcessPoolReportAllocationDO::getActiveOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, BigDecimal> allocatedElsewhere = allocationMapper
                .selectListByActiveOrderIdsAndProcess(activeOrderIds, event.getProcessId()).stream()
                .filter(row -> !Objects.equals(row.getEventId(), event.getId()))
                .collect(Collectors.groupingBy(MesProcessPoolReportAllocationDO::getActiveOrderId,
                        LinkedHashMap::new, Collectors.reducing(BigDecimal.ZERO,
                                MesProcessPoolReportAllocationDO::getAllocatedQuantity, BigDecimal::add)));
        Map<Long, BigDecimal> currentByActiveOrder = aggregateRows(current);
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (Long activeOrderId : activeOrderIds) {
            MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
            if (activeOrder == null) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, activeOrderId);
            }
            MesTeamLeaderOrderProcessTarget target = targetService.requireUniqueTargetForProcess(
                    activeOrder, event.getProcessId());
            BigDecimal totalForOrder = allocatedElsewhere.getOrDefault(activeOrderId, BigDecimal.ZERO)
                    .add(currentByActiveOrder.getOrDefault(activeOrderId, BigDecimal.ZERO));
            result.put(activeOrderId,
                    totalForOrder.subtract(target.plannedQuantity()).max(BigDecimal.ZERO));
        }
        return result;
    }

    private Map<Long, MesProWorkOrderDO> loadWorkOrders(List<MesProcessPoolActiveOrderDO> orders) {
        List<Long> ids = orders.stream().map(MesProcessPoolActiveOrderDO::getWorkOrderId).distinct().toList();
        return workOrderMapper.selectListByIdsForUpdate(ids).stream().collect(Collectors.toMap(
                MesProWorkOrderDO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, BigDecimal> aggregateDesired(List<MesReportAllocationSaveLine> lines,
                                                   Map<Long, MesProcessPoolActiveOrderDO> activeById,
                                                   Long eventId) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        if (lines == null) {
            return result;
        }
        for (MesReportAllocationSaveLine line : lines) {
            if (line == null || line.getAllocatedQuantity() == null
                    || line.getAllocatedQuantity().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (line.getAllocatedQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, eventId);
            }
            if (!activeById.containsKey(line.getActiveOrderId())) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, line.getActiveOrderId());
            }
            result.merge(line.getActiveOrderId(), line.getAllocatedQuantity(), BigDecimal::add);
        }
        return result;
    }

    private Map<Long, BigDecimal> aggregateRows(Collection<MesProcessPoolReportAllocationDO> rows) {
        return rows.stream().collect(Collectors.groupingBy(MesProcessPoolReportAllocationDO::getActiveOrderId,
                LinkedHashMap::new, Collectors.reducing(BigDecimal.ZERO,
                        MesProcessPoolReportAllocationDO::getAllocatedQuantity, BigDecimal::add)));
    }

    private BigDecimal sumAllocations(Collection<MesProcessPoolReportAllocationDO> rows) {
        return rows.stream().map(MesProcessPoolReportAllocationDO::getAllocatedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int currentVersion(Long eventId) {
        MesProcessPoolReportAllocationStateDO state = stateMapper.selectByEventId(eventId);
        return state == null || state.getCurrentVersion() == null ? 0 : state.getCurrentVersion();
    }

    private MesProProcessPoolEventDO requireEvent(Long eventId, boolean forUpdate) {
        MesProProcessPoolEventDO event = forUpdate ? eventMapper.selectByIdForUpdate(eventId)
                : eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, eventId);
        }
        if (event.getRouteProcessId() == null || event.getProcessId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "event.process");
        }
        return event;
    }

    private void assertScope(MesProProcessPoolEventDO event, Long leaderUserId, String leaderType) {
        if (leaderUserId == null || !MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION.equals(leaderType)) {
            throw exception(PRO_PROCESS_POOL_REPORT_CONFIRMATION_PRODUCTION_LEADER_REQUIRED,
                    event.getId(), leaderType);
        }
        boolean authorized = routeStartAuthorizationService.listAuthorizedRouteProcesses(leaderUserId).stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .anyMatch(event.getProcessId()::equals);
        if (!authorized) {
            throw exception(PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED, "工序报工");
        }
    }

    private void validateCommand(MesReportAllocationSaveCommand command) {
        if (command == null || command.getEventId() == null || command.getLeaderUserId() == null
                || StrUtil.isBlank(command.getLeaderType())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "reportAllocationSave");
        }
        if (!MesProcessPoolReportAllocationDO.MODE_FIFO.equals(command.getAllocationMode())
                && !MesProcessPoolReportAllocationDO.MODE_MANUAL.equals(command.getAllocationMode())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_MODE_INVALID, command.getAllocationMode());
        }
    }

    private String requestHash(MesReportAllocationSaveCommand command) {
        String canonical = command.getAllocationMode() + "\n" + Objects.toString(command.getReason(), "") + "\n"
                + (command.getAllocations() == null ? List.<MesReportAllocationSaveLine>of() : command.getAllocations())
                .stream().sorted(Comparator.comparing(MesReportAllocationSaveLine::getActiveOrderId,
                                Comparator.nullsFirst(Long::compareTo)))
                .map(line -> line.getActiveOrderId() + ":" + quantityText(line.getAllocatedQuantity()))
                .collect(Collectors.joining("|"));
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private String quantityText(BigDecimal quantity) {
        return quantity == null ? "null" : quantity.stripTrailingZeros().toPlainString();
    }

    private record AllocationValidation(Map<Long, MesTeamLeaderOrderProcessTarget> targets,
                                        Map<Long, BigDecimal> overageByActiveOrderId) {
    }
}
