package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
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
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_OVERAGE_LIMIT_EXCEEDED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_RELEASED_LOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_CONFIRMATION_PRODUCTION_LEADER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_REJECT_REMARK_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_SIGNATURE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED;

@Service
public class MesReportAllocationCommandService {
    @Resource
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper revisionMapper;

    private final MesTeamLeaderScopeService scopeService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper activeOrderProcessSnapshotMapper;
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
    @Resource
    private GxpAuditService gxpAuditService;
    @Resource
    private cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService handoffService;
    @Resource
    private cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver returnCorrectionResolver;
    @Resource
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;
    @Resource
    private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService nonconformanceReviewService;

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
            MesProductionReportManagementSummaryService reportManagementSummaryService,
            MesProcessPoolActiveOrderProcessSnapshotMapper activeOrderProcessSnapshotMapper) {
        this.scopeService = scopeService;
        this.eventMapper = eventMapper;
        this.activeOrderMapper = activeOrderMapper;
        this.activeOrderProcessSnapshotMapper = activeOrderProcessSnapshotMapper;
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
        Set<Long> releasedIds = allocationLockedIds(releaseCandidates, activeOrders.stream().collect(Collectors.toMap(
                MesProcessPoolActiveOrderDO::getId, Function.identity())), false);
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
        gxpAuditService.acquireLedgerLock();
        MesProProcessPoolEventDO event = requireEvent(eventId, true);
        assertSubmissionNotRejected(event.getId());
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
        assertActiveOrdersOpenForProduction(List.of(activeOrder));
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
                .routeProcessId(event.getRouteProcessId()).processId(event.getProcessId())
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
                        .routeProcessId(event.getRouteProcessId()).processId(event.getProcessId())
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
        reportManagementSummaryService.refreshProductionEvent(event);
        appendInitialAllocationGxpAudit(event, activeOrder, allocation, state);
    }

    private void appendInitialAllocationGxpAudit(MesProProcessPoolEventDO event,
                                                 MesProcessPoolActiveOrderDO activeOrder,
                                                 MesProcessPoolReportAllocationDO allocation,
                                                 MesProcessPoolReportAllocationStateDO state) {
        Long parentSignatureId = event.getSignatureId();
        if (parentSignatureId == null) {
            throw new IllegalStateException("Production submit signature is required for initial allocation audit");
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("profile", "ALLOCATION");
        after.put("eventId", event.getId());
        after.put("allocationVersion", state.getCurrentVersion());
        after.put("poolQuantity", event.getReportOutputQuantity());
        Map<String, Object> allocationSnapshot = new LinkedHashMap<>();
        allocationSnapshot.put("id", allocation.getId());
        allocationSnapshot.put("activeOrderId", allocation.getActiveOrderId());
        allocationSnapshot.put("workOrderId", allocation.getWorkOrderId());
        allocationSnapshot.put("routeProcessId", allocation.getRouteProcessId());
        allocationSnapshot.put("processId", allocation.getProcessId());
        allocationSnapshot.put("allocatedQuantity", allocation.getAllocatedQuantity());
        allocationSnapshot.put("status", allocation.getLifecycleStatus());
        after.put("allocation", allocationSnapshot);
        Map<String, Object> affectedOrder = new LinkedHashMap<>();
        affectedOrder.put("activeOrderId", activeOrder.getId());
        affectedOrder.put("workOrderId", activeOrder.getWorkOrderId());
        affectedOrder.put("quantity", allocation.getAllocatedQuantity());
        affectedOrder.put("status", allocation.getLifecycleStatus());
        after.put("affectedOrders", List.of(affectedOrder));
        gxpAuditService.append(GxpAuditCommand.builder()
                .eventSchemaVersion(2)
                .operationId("mes.production.allocation.initial")
                .subjectId("MES_PROCESS_POOL_EVENT:" + event.getId())
                .subjectVersion(String.valueOf(state.getCurrentVersion()))
                .reason("一线生产初始分配")
                .reasonCode("MES_PRODUCTION_ALLOCATION_INITIAL")
                .reasonSource("SYSTEM")
                .beforeState(GxpAuditStateEnvelope.builder()
                        .state("ABSENT")
                        .canonicalJson("{\"eventId\":" + event.getId() + ",\"allocationVersion\":0}")
                        .build())
                .afterState(GxpAuditStateEnvelope.builder()
                        .state("PRESENT")
                        .objectVersion(String.valueOf(state.getCurrentVersion()))
                        .canonicalJson(JsonUtils.toJsonString(after))
                        .build())
                .idempotencyKey("ALLOC_INITIAL:" + event.getId() + ":" + state.getCurrentVersion())
                .requestId("MES-ALLOC-INITIAL:" + event.getId() + ":" + state.getCurrentVersion())
                .resultStatus("SUCCESS")
                .sourceType("SERVICE_METHOD")
                .sourceLocator("cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationCommandService#createInitialAllocation")
                .signatureRecordId(String.valueOf(parentSignatureId))
                .links(List.of(
                        new GxpAuditRelation("SUBJECT", "PROCESS_POOL_EVENT", String.valueOf(event.getId()),
                                String.valueOf(state.getCurrentVersion()), null),
                        new GxpAuditRelation("SOURCE", "ACTIVE_ORDER", String.valueOf(activeOrder.getId()),
                                String.valueOf(activeOrder.getVersion()), null),
                        new GxpAuditRelation("PARENT_SIGNATURE", "SIGNATURE", String.valueOf(parentSignatureId),
                                null, null)))
                .evidences(List.of(new GxpAuditEvidence("PARENT_PRODUCTION_SUBMIT", String.valueOf(event.getId()),
                        null, null, "PARENT")))
                .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public MesReportAllocationSnapshot save(MesReportAllocationSaveCommand command) {
        validateCommand(command);
        gxpAuditService.acquireLedgerLock();
        MesProProcessPoolEventDO event = requireEvent(command.getEventId(), true);
        assertScope(event, command.getLeaderUserId(), command.getLeaderType());
        assertSubmissionNotRejected(event.getId());
        BigDecimal pool = poolQuantityService.requirePoolQuantity(event);
        MesProcessPoolReportAllocationStateDO state = readStateForReview(event);
        List<MesProcessPoolReportAllocationDO> current = allocationMapper.selectListByEventIdForUpdate(event.getId());
        int currentVersion = state.getCurrentVersion() == null ? 0 : state.getCurrentVersion();
        List<Map<String, Object>> beforeAllocationSnapshot = allocationAuditSnapshot(current);
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

        validateDisplayedProductionContext(event, state, command.getExpectedReview());
        List<MesProcessPoolActiveOrderDO> activeOrders = activeOrderMapper
                .selectActiveListByLeaderForUpdate(command.getLeaderUserId()).stream()
                .filter(order -> "ACTIVE".equals(order.getActiveStatus())).toList();
        Map<Long, MesProcessPoolActiveOrderDO> activeById = activeOrders.stream().collect(Collectors.toMap(
                MesProcessPoolActiveOrderDO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Set<Long> currentIds = current.stream().map(MesProcessPoolReportAllocationDO::getActiveOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long id : currentIds) {
            MesProcessPoolActiveOrderDO order = activeById.get(id);
            if (order == null) order = activeOrderMapper.selectByIdForUpdate(id);
            if (order == null) throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, id);
            activeById.put(id, order);
        }
        Map<Long, BigDecimal> desired = aggregateDesired(command.getAllocations(), activeById, event.getId());
        Map<Long, BigDecimal> previous = aggregateRows(current);
        Set<Long> lockedIds = allocationLockedIds(currentIds, activeById, true);
        for (Long id : lockedIds) {
            boolean explicitlyRequested = command.getAllocations() != null && command.getAllocations().stream()
                    .anyMatch(line -> line != null && Objects.equals(line.getActiveOrderId(), id));
            if (explicitlyRequested && (!previous.containsKey(id) || !desired.containsKey(id) || desired.get(id).compareTo(previous.get(id)) != 0)) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_RELEASED_LOCKED, id);
            }
            // Closed-cycle rows are retained even when omitted from the editable request.
            desired.remove(id);
        }
        Set<Long> retainedIds = new LinkedHashSet<>(lockedIds);
        previous.forEach((id, quantity) -> {
            if (desired.containsKey(id) && quantity.compareTo(desired.get(id)) == 0) retainedIds.add(id);
        });
        retainedIds.forEach(desired::remove);
        List<MesProcessPoolReportAllocationDO> locked = current.stream()
                .filter(row -> retainedIds.contains(row.getActiveOrderId())).toList();
        List<MesProcessPoolReportAllocationDO> editableOld = current.stream()
                .filter(row -> !retainedIds.contains(row.getActiveOrderId())).toList();
        Set<Long> changedIds = new LinkedHashSet<>(desired.keySet());
        editableOld.stream().map(MesProcessPoolReportAllocationDO::getActiveOrderId).forEach(changedIds::add);
        assertActiveOrdersOpenForProduction(changedIds, activeById);
        for (Long id : changedIds) {
            nonconformanceReviewService.ensureWorkOrderNotFrozen(activeById.get(id).getWorkOrderId(), "报工分配调整");
        }
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
        var latestReview = reviewMapper.selectLatestByEventIdForUpdate(event.getId());
        ReviewEvidenceRequirement evidenceRequirement = before.equals(desired) ? reviewEvidenceRequirement(event, current)
                : new ReviewEvidenceRequirement(latestReview == null || !hasApprovedReviewEvidence(latestReview), null);
        if (evidenceRequirement.required()) {
            nonconformanceReviewService.ensureWorkOrderNotFrozen(event.getWorkOrderId(), "生产报工复核");
            Set<Long> evidenceOrders = current.stream()
                    .filter(row -> row.getAllocatedQuantity() != null && row.getAllocatedQuantity().signum() > 0)
                    .filter(row -> evidenceRequirement.required() || !lockedIds.contains(row.getActiveOrderId()))
                    .map(MesProcessPoolReportAllocationDO::getActiveOrderId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            for (Long id : evidenceOrders) {
                nonconformanceReviewService.ensureWorkOrderNotFrozen(activeById.get(id).getWorkOrderId(), "生产报工复核");
            }
        }
        if (state.getId() == null && (evidenceRequirement.required() || !before.equals(desired))) {
            state.setLastChangedBy(command.getLeaderUserId()).setLastChangedAt(LocalDateTime.now());
            if (stateMapper.insert(state) != 1) throw new IllegalStateException("生产分配状态写入失败");
        }
        if (before.equals(desired)) {
            ReviewEvidenceRequirement reviewRequirement = evidenceRequirement;
            MesProcessPoolSubmissionReviewDO auditReview = null;
            if (reviewRequirement.required()) {
                if (locked.stream().anyMatch(row -> lockedIds.contains(row.getActiveOrderId())) &&
                        (locked.stream().anyMatch(row -> row.getReviewId() == null)
                        || reviewRequirement.reviewToBackfill() != null)) {
                    throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lockedAllocation.reviewEvidence");
                }
                MesProcessPoolSubmissionReviewDO review = requireReview(event, command,
                        reviewRequirement.reviewToBackfill());
                auditReview = review;
                Long reviewId = review.getId();
                long missingReviewCount = current.stream()
                        .filter(row -> row != null && row.getReviewId() == null)
                        .count();
                if (missingReviewCount > 0) {
                    int attached = allocationMapper.attachReviewToCurrentRowsByEventId(
                            event.getId(), reviewId, command.getLeaderUserId(), review.getReviewedAt());
                    if (attached != missingReviewCount) {
                        throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                                event.getId(), command.getExpectedVersion(), state.getCurrentVersion());
                    }
                }
                if (reviewRequirement.reviewToBackfill() != null) {
                    long linkedReviewCount = current.stream()
                            .filter(row -> row != null && Objects.equals(row.getReviewId(), reviewId))
                            .count();
                    int refreshed = allocationMapper.refreshReviewEvidenceForCurrentRowsByReviewId(
                            event.getId(), reviewId, command.getLeaderUserId(), review.getReviewedAt());
                    if (refreshed != linkedReviewCount) {
                        throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                                event.getId(), command.getExpectedVersion(), state.getCurrentVersion());
                    }
                }
                current = allocationMapper.selectListByEventIdForUpdate(event.getId());
                state.setLastIdempotencyKey(command.getIdempotencyKey())
                        .setLastRequestHash(requestHash)
                        .setLastChangedBy(command.getLeaderUserId())
                        .setLastChangedAt(review.getReviewedAt());
                if (stateMapper.updateById(state) != 1) {
                    throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                            event.getId(), command.getExpectedVersion(), state.getCurrentVersion());
                }
            }
            if (!current.isEmpty()) {
                reportManagementSummaryService.refreshProductionEvent(event);
                if (auditReview != null) {
                    appendAllocationGxpAudit("mes.production.allocation.save", event, currentVersion,
                            beforeAllocationSnapshot, state.getCurrentVersion(),
                            current, current.stream().map(MesProcessPoolReportAllocationDO::getActiveOrderId)
                                    .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new)),
                            auditReview.getReviewSignatureId(),
                            "分配复核证据补写",
                            "REVIEW:" + auditReview.getId());
                }
            }
            if (!current.isEmpty()) {
                completionService.reconcileAffectedAllocations(event, current.stream()
                        .filter(row -> !lockedIds.contains(row.getActiveOrderId())).toList());
            }
            if (auditReview != null) handoffService.allocationReviewed(event.getId(), auditReview.getId(), current.stream()
                    .filter(row -> !lockedIds.contains(row.getActiveOrderId())).toList());
            return buildSnapshot(event, pool, state.getCurrentVersion(), current,
                    validation.overageByActiveOrderId());
        }

        int newVersion = currentVersion + 1;
        MesProcessPoolSubmissionReviewDO review = requireReview(event, command);
        Long reviewId = review.getId();
        List<Long> oldIds = editableOld.stream().map(MesProcessPoolReportAllocationDO::getId)
                .filter(Objects::nonNull).toList();
        if (!oldIds.isEmpty() && allocationMapper.supersedeCurrentRows(oldIds, newVersion) != oldIds.size()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    event.getId(), command.getExpectedVersion(), state.getCurrentVersion());
        }
        LocalDateTime now = review.getReviewedAt();
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
        if (locked.isEmpty()) quantityFragmentService.rebuildForVersion(event, newVersion, inserted);
        else quantityFragmentService.rebuildPreservingAllocations(event, newVersion, inserted, locked);
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
        Set<Long> allocationAuditActiveOrderIds = new LinkedHashSet<>();
        current.stream().map(MesProcessPoolReportAllocationDO::getActiveOrderId)
                .filter(Objects::nonNull).forEach(allocationAuditActiveOrderIds::add);
        next.stream().map(MesProcessPoolReportAllocationDO::getActiveOrderId)
                .filter(Objects::nonNull).forEach(allocationAuditActiveOrderIds::add);
        appendAllocationGxpAudit("mes.production.allocation.save", event, currentVersion,
                beforeAllocationSnapshot, newVersion,
                next, allocationAuditActiveOrderIds,
                review.getReviewSignatureId(), "分配保存", null);
        handoffService.allocationReviewed(event.getId(), review.getId(), inserted);
        return buildSnapshot(event, pool, newVersion, next, validation.overageByActiveOrderId());
    }

    /**
     * 驳回生产报工并回滚该事件已经形成的共享分配事实。
     *
     * <p>生产报工的正式进度来自当前有效分配，因此这里通过生命周期替换、数量片段重建和
     * 订单工序完成量回算恢复派生状态，不直接覆盖任何进度百分比。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Long rejectProductionSubmission(Long eventId, Long leaderUserId,
                                           String rejectReason, String signaturePassword, MesSubmissionReviewExpectedContext expectedReview) {
        validateRejectCommand(eventId, leaderUserId, rejectReason, signaturePassword);
        gxpAuditService.acquireLedgerLock();
        MesProProcessPoolEventDO event = requireEvent(eventId, true);
        if (!MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionSubmitEvent");
        }
        assertScope(event, leaderUserId, MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION);
        MesProcessPoolReportAllocationStateDO state = readStateForReview(event);
        List<MesProcessPoolReportAllocationDO> current = allocationMapper.selectListByEventIdForUpdate(eventId);
        int currentVersion = state.getCurrentVersion() == null ? 0 : state.getCurrentVersion();
        List<Map<String, Object>> beforeAllocationSnapshot = allocationAuditSnapshot(current);
        MesProcessPoolSubmissionReviewDO existingReview = reviewMapper.selectLatestByEventIdForUpdate(eventId);
        var returnedCorrection = returnCorrectionResolver.find(event, existingReview);
        boolean sameRejectedRequest = existingReview != null
                && MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(existingReview.getReviewStatus())
                && isSameRejection(existingReview, leaderUserId, rejectReason);
        if (existingReview != null
                && MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(existingReview.getReviewStatus())
                && returnedCorrection == null) {
            if (sameRejectedRequest && current.isEmpty()) {
                var signed = JsonUtils.parseTree(existingReview.getReviewSignatureSnapshotJson()).path("expectedReview");
                if (expectedReview == null || !signed.equals(JsonUtils.parseTree(JsonUtils.toJsonString(expectedReview)))) {
                    throw new IllegalStateException("生产退回重放上下文不一致");
                }
                return existingReview.getId();
            }
            if (!sameRejectedRequest) {
                throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS,
                        eventId, existingReview.getReviewStatus());
            }
        }
        if (existingReview != null
                && !MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(existingReview.getReviewStatus())
                && !sameRejectedRequest && returnedCorrection == null) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS,
                    eventId, existingReview.getReviewStatus());
        }

        validateDisplayedProductionContext(event, state, expectedReview);
        nonconformanceReviewService.ensureWorkOrderNotFrozen(event.getWorkOrderId(), "生产报工退回");
        Set<Long> activeOrderIds = current.stream()
                .filter(line -> line.getAllocatedQuantity() != null && line.getAllocatedQuantity().signum() > 0)
                .map(MesProcessPoolReportAllocationDO::getActiveOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertActiveOrdersOpenForProduction(activeOrderIds, Map.of());
        for (Long activeOrderId : activeOrderIds.stream().sorted().toList()) {
            var order = activeOrderMapper.selectByIdForUpdate(activeOrderId);
            if (order == null || !Objects.equals(order.getTenantId(), event.getTenantId())
                    || order.getWorkOrderId() == null) {
                throw new IllegalStateException("生产退回目标缺少正式同租户工单");
            }
            nonconformanceReviewService.ensureWorkOrderNotFrozen(order.getWorkOrderId(), "生产报工退回");
        }
        Set<Long> releasedActiveOrderIds = releaseStateService.findReleasedActiveOrderIdsForUpdate(activeOrderIds);
        if (!releasedActiveOrderIds.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_RELEASED_LOCKED,
                    releasedActiveOrderIds.iterator().next());
        }

        if (state.getId() == null) {
            state.setLastChangedBy(leaderUserId).setLastChangedAt(LocalDateTime.now());
            if (stateMapper.insert(state) != 1) throw new IllegalStateException("生产分配状态写入失败");
        }
        int rejectedVersion = Math.max(currentVersion + 1, 1);
        List<Long> currentAllocationIds = current.stream()
                .map(MesProcessPoolReportAllocationDO::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!currentAllocationIds.isEmpty()
                && allocationMapper.supersedeCurrentRows(currentAllocationIds, rejectedVersion)
                != currentAllocationIds.size()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    eventId, currentVersion, rejectedVersion);
        }

        // Rebuild with an empty current allocation set: this supersedes FIFO lines and restores fragment balances.
        quantityFragmentService.rebuildForVersion(event, rejectedVersion, List.of());
        if (!current.isEmpty()) {
            completionService.reconcileAffectedAllocations(event, current);
            insertRejectionAudits(event, leaderUserId, rejectReason, rejectedVersion, current);
        }

        ReviewSignaturePayload signature = recordRejectionSignature(event, leaderUserId, signaturePassword, expectedReview);
        MesProcessPoolSubmissionReviewDO rejectedReview = MesProcessPoolSubmissionReviewDO.builder()
                .eventId(eventId)
                .leaderUserId(leaderUserId)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_REJECTED)
                .reviewRemark(rejectReason.trim())
                .reviewedAt(LocalDateTime.now())
                .reviewSignatureId(signature.reviewSignatureId())
                .reviewSignatureUserId(signature.reviewSignatureUserId())
                .reviewSignatureSnapshotJson(signature.reviewSignatureSnapshotJson())
                .reviewRound(existingReview == null ? 0 : requiredReviewRound(existingReview) + 1)
                .sourceRevisionId(returnedCorrection == null ? null : returnedCorrection.getId())
                .supersededReviewId(existingReview == null ? null : existingReview.getId())
                .build();
        if (reviewMapper.insert(rejectedReview) != 1) {
            throw new IllegalStateException("Failed to insert production rejection review: " + eventId);
        }

        state.setCurrentVersion(rejectedVersion)
                .setLastIdempotencyKey(null)
                .setLastRequestHash(null)
                .setLastChangedBy(leaderUserId)
                .setLastChangedAt(LocalDateTime.now());
        if (stateMapper.updateById(state) != 1) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    eventId, currentVersion, rejectedVersion);
        }
        reportManagementSummaryService.refreshProductionEvent(event);
        appendAllocationGxpAudit("mes.production.reject", event, currentVersion,
                beforeAllocationSnapshot, rejectedVersion, List.of(),
                activeOrderIds, signature.reviewSignatureId(), rejectReason, null);
        handoffService.reviewed(eventId, rejectedReview.getId());
        return rejectedReview.getId();
    }

    private void appendAllocationGxpAudit(String operationId, MesProProcessPoolEventDO event,
                                          Integer beforeVersion, List<Map<String, Object>> beforeAllocations,
                                          Integer afterVersion, List<MesProcessPoolReportAllocationDO> allocations,
                                          Collection<Long> relationActiveOrderIds,
                                          Long signatureId, String reason, String auditSuffix) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("profile", "mes.production.reject".equals(operationId) ? "PRODUCTION_REVIEW" : "ALLOCATION");
        after.put("eventId", event.getId());
        after.put("allocationVersion", afterVersion);
        after.put("poolQuantity", event.getReportOutputQuantity());
        after.put("allocations", allocationAuditSnapshot(allocations));
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("profile", "mes.production.reject".equals(operationId) ? "PRODUCTION_REVIEW" : "ALLOCATION");
        before.put("eventId", event.getId());
        before.put("allocationVersion", beforeVersion);
        before.put("poolQuantity", event.getReportOutputQuantity());
        before.put("allocations", beforeAllocations);
        String auditIdentity = "ALLOC:" + operationId + ":" + event.getId() + ":" + afterVersion
                + (StrUtil.isBlank(auditSuffix) ? "" : ":" + auditSuffix);
        List<GxpAuditRelation> relations = new ArrayList<>();
        relations.add(new GxpAuditRelation("SUBJECT", "PROCESS_POOL_EVENT",
                String.valueOf(event.getId()), String.valueOf(afterVersion), null));
        if (relationActiveOrderIds != null) {
            relationActiveOrderIds.stream().filter(Objects::nonNull).distinct().forEach(activeOrderId ->
                    relations.add(new GxpAuditRelation("SUBJECT", "ACTIVE_ORDER",
                            String.valueOf(activeOrderId), null, null)));
        }
        gxpAuditService.append(GxpAuditCommand.builder()
                .eventSchemaVersion(2)
                .operationId(operationId)
                .subjectId("MES_PROCESS_POOL_EVENT:" + event.getId())
                .subjectVersion(String.valueOf(afterVersion))
                .reason(reason)
                .reasonCode("mes.production.reject".equals(operationId)
                        ? "MES_PRODUCTION_REJECT" : "MES_PRODUCTION_ALLOCATION_SAVE")
                .reasonSource("mes.production.reject".equals(operationId) ? "USER" : "SYSTEM")
                .beforeState(GxpAuditStateEnvelope.builder().state("PRESENT")
                        .objectVersion(String.valueOf(beforeVersion))
                        .canonicalJson(JsonUtils.toJsonString(before)).build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT")
                        .objectVersion(String.valueOf(afterVersion))
                        .canonicalJson(JsonUtils.toJsonString(after)).build())
                .idempotencyKey(auditIdentity)
                .requestId("MES-ALLOC:" + auditIdentity)
                .resultStatus("SUCCESS")
                .sourceType("SERVICE_METHOD")
                .sourceLocator("mes.production.reject".equals(operationId)
                        ? "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationCommandService#rejectProductionSubmission"
                        : "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationCommandService#save")
                .signatureRecordId(signatureId == null ? null : String.valueOf(signatureId))
                .links(relations)
                .build());
    }

    private List<Map<String, Object>> allocationAuditSnapshot(
            Collection<MesProcessPoolReportAllocationDO> allocations) {
        if (allocations == null) {
            throw new IllegalStateException("Allocation audit snapshot is required");
        }
        return allocations.stream().map(row -> {
            if (row == null) {
                throw new IllegalStateException("Allocation audit snapshot contains null row");
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getId());
            item.put("eventId", row.getEventId());
            item.put("reviewId", row.getReviewId());
            item.put("activeOrderId", row.getActiveOrderId());
            item.put("workOrderId", row.getWorkOrderId());
            item.put("routeProcessId", row.getRouteProcessId());
            item.put("processId", row.getProcessId());
            item.put("allocatedQuantity", row.getAllocatedQuantity());
            item.put("allocationMode", row.getAllocationMode());
            item.put("status", row.getLifecycleStatus());
            item.put("createdVersion", row.getCreatedVersion());
            item.put("supersededVersion", row.getSupersededVersion());
            item.put("confirmedAt", row.getConfirmedAt());
            return item;
        }).toList();
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
        List<MesProcessPoolReportAllocationDO> allocatedElsewhere = allocationMapper
                .selectListByActiveOrderIdsAndProcessForUpdate(desired.keySet(), event.getProcessId()).stream()
                .filter(row -> !Objects.equals(row.getEventId(), event.getId())).toList();
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
            BigDecimal totalForOrder = calculateAllocatedMaterialMaximum(
                    order, target, event, entry.getValue(), allocatedElsewhere, true);
            assertWithinFrozenOverageLimit(order.getId(), target.routeProcessId(), target.processId(),
                    target.plannedQuantity(), totalForOrder, true);
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

    private ReviewEvidenceRequirement reviewEvidenceRequirement(MesProProcessPoolEventDO event,
                                                                List<MesProcessPoolReportAllocationDO> current) {
        if (current == null || current.isEmpty()) {
            return new ReviewEvidenceRequirement(false, null);
        }
        boolean missingReviewId = current.stream()
                .filter(Objects::nonNull)
                .anyMatch(allocation -> allocation.getReviewId() == null);
        Set<Long> reviewIds = current.stream()
                .filter(Objects::nonNull)
                .map(MesProcessPoolReportAllocationDO::getReviewId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        MesProcessPoolSubmissionReviewDO reviewToBackfill = selectReviewRequiringSignatureBackfill(event, reviewIds);
        return new ReviewEvidenceRequirement(missingReviewId || reviewToBackfill != null, reviewToBackfill);
    }

    private MesProcessPoolSubmissionReviewDO selectReviewRequiringSignatureBackfill(
            MesProProcessPoolEventDO event, Set<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return null;
        }
        Map<Long, MesProcessPoolSubmissionReviewDO> reviewsById = reviewMapper
                .selectListByEventIdForUpdate(event.getId()).stream()
                .filter(review -> review != null && review.getId() != null && reviewIds.contains(review.getId()))
                .collect(Collectors.toMap(MesProcessPoolSubmissionReviewDO::getId, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
        MesProcessPoolSubmissionReviewDO reviewToBackfill = null;
        for (Long reviewId : reviewIds) {
            MesProcessPoolSubmissionReviewDO review = reviewsById.get(reviewId);
            if (review == null) {
                throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_SIGNATURE_REQUIRED, event.getId());
            }
            if (hasApprovedReviewEvidence(review)) {
                continue;
            }
            if (reviewToBackfill != null) {
                throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_SIGNATURE_REQUIRED, event.getId());
            }
            reviewToBackfill = review;
        }
        return reviewToBackfill;
    }

    private MesProcessPoolSubmissionReviewDO requireReview(MesProProcessPoolEventDO event,
                                                           MesReportAllocationSaveCommand command) {
        return requireReview(event, command, null);
    }

    private MesProcessPoolSubmissionReviewDO requireReview(MesProProcessPoolEventDO event,
                                                           MesReportAllocationSaveCommand command,
                                                           MesProcessPoolSubmissionReviewDO reviewToBackfill) {
        if (StrUtil.isBlank(command.getSignaturePassword())) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_SIGNATURE_REQUIRED, event.getId());
        }
        MesProcessPoolSubmissionReviewDO review = reviewToBackfill == null
                ? reviewMapper.selectLatestByEventIdForUpdate(event.getId()) : reviewToBackfill;
        MesProcessPoolSubmissionReviewDO superseded = null;
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO correction = null;
        if (review != null && MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(review.getReviewStatus())) {
            correction = returnCorrectionResolver.find(event, review);
            if (correction == null) throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS, event.getId(), review.getReviewStatus());
            superseded = review;
            review = null;
        }
        if (review != null) {
            if (MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(review.getReviewStatus())) {
                throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS,
                        event.getId(), review.getReviewStatus());
            }
            if (!MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(review.getReviewStatus())) {
                throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS,
                        event.getId(), review.getReviewStatus());
            }
            if (hasApprovedReviewEvidence(review)) {
                requireApprovedReviewEvidence(event, review);
                return review;
            }
            LocalDateTime reviewedAt = LocalDateTime.now();
            ReviewSignaturePayload signature = recordApprovedReviewSignature(event, command, reviewedAt);
            review.setLeaderUserId(command.getLeaderUserId())
                    .setLeaderType(command.getLeaderType())
                    .setReviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                    .setReviewRemark(command.getReason())
                    .setReviewedAt(reviewedAt)
                    .setReviewSignatureId(signature.reviewSignatureId())
                    .setReviewSignatureUserId(signature.reviewSignatureUserId())
                    .setReviewSignatureSnapshotJson(signature.reviewSignatureSnapshotJson());
            if (reviewMapper.updateById(review) != 1) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                        event.getId(), command.getExpectedVersion(), null);
            }
            requireApprovedReviewEvidence(event, review);
            return review;
        }
        LocalDateTime reviewedAt = LocalDateTime.now();
        ReviewSignaturePayload signature = recordApprovedReviewSignature(event, command, reviewedAt);
        review = MesProcessPoolSubmissionReviewDO.builder().eventId(event.getId())
                .leaderUserId(command.getLeaderUserId()).leaderType(command.getLeaderType())
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark(command.getReason()).reviewedAt(reviewedAt)
                .reviewSignatureId(signature.reviewSignatureId())
                .reviewSignatureUserId(signature.reviewSignatureUserId())
                .reviewSignatureSnapshotJson(signature.reviewSignatureSnapshotJson())
                .reviewRound(superseded == null ? 0 : requiredReviewRound(superseded) + 1)
                .sourceRevisionId(correction == null ? null : correction.getId())
                .supersededReviewId(superseded == null ? null : superseded.getId()).build();
        if (reviewMapper.insert(review) != 1) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    event.getId(), command.getExpectedVersion(), null);
        }
        requireApprovedReviewEvidence(event, review);
        return review;
    }

    private void requireApprovedReviewEvidence(MesProProcessPoolEventDO event,
                                               MesProcessPoolSubmissionReviewDO review) {
        if (!hasApprovedReviewEvidence(review)) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_SIGNATURE_REQUIRED, event.getId());
        }
    }

    private boolean hasApprovedReviewEvidence(MesProcessPoolSubmissionReviewDO review) {
        return review != null
                && review.getId() != null
                && MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(review.getReviewStatus())
                && review.getReviewedAt() != null
                && review.getReviewSignatureId() != null
                && review.getReviewSignatureUserId() != null
                && StrUtil.isNotBlank(review.getReviewSignatureSnapshotJson());
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

    private MesProcessPoolReportAllocationStateDO readStateForReview(MesProProcessPoolEventDO event) {
        var state = stateMapper.selectByEventIdForUpdate(event.getId());
        return state == null ? MesProcessPoolReportAllocationStateDO.builder().eventId(event.getId()).currentVersion(0).build() : state;
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
        Set<Long> released = allocationLockedIds(current.stream()
                .map(MesProcessPoolReportAllocationDO::getActiveOrderId).distinct().toList(), Map.of(), false);
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
        Set<Long> actuallyReleased = releaseStateService.findReleasedActiveOrderIds(rows.stream()
                .map(MesProcessPoolReportAllocationDO::getActiveOrderId).distinct().toList());
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
                            .released(actuallyReleased.contains(row.getActiveOrderId())).editable(!released).build();
                }).toList();
    }

    private Map<Long, BigDecimal> calculateCurrentOverage(
            MesProProcessPoolEventDO event, List<MesProcessPoolReportAllocationDO> current) {
        if (current.isEmpty()) {
            return Map.of();
        }
        Set<Long> activeOrderIds = current.stream().map(MesProcessPoolReportAllocationDO::getActiveOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProcessPoolReportAllocationDO> allocatedElsewhere = allocationMapper
                .selectListByActiveOrderIdsAndProcess(activeOrderIds, event.getProcessId()).stream()
                .filter(row -> !Objects.equals(row.getEventId(), event.getId())).toList();
        Map<Long, BigDecimal> currentByActiveOrder = aggregateRows(current);
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (Long activeOrderId : activeOrderIds) {
            MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
            if (activeOrder == null) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, activeOrderId);
            }
            MesTeamLeaderOrderProcessTarget target = targetService.requireUniqueTargetForProcess(
                    activeOrder, event.getProcessId());
            BigDecimal totalForOrder = calculateAllocatedMaterialMaximum(activeOrder, target, event,
                    currentByActiveOrder.getOrDefault(activeOrderId, BigDecimal.ZERO), allocatedElsewhere, false);
            assertWithinFrozenOverageLimit(activeOrderId, target.routeProcessId(), target.processId(),
                    target.plannedQuantity(), totalForOrder, false);
            result.put(activeOrderId,
                    totalForOrder.subtract(target.plannedQuantity()).max(BigDecimal.ZERO));
        }
        return result;
    }

    private BigDecimal calculateAllocatedMaterialMaximum(MesProcessPoolActiveOrderDO order,
            MesTeamLeaderOrderProcessTarget target, MesProProcessPoolEventDO currentEvent,
            BigDecimal currentQuantity, List<MesProcessPoolReportAllocationDO> otherAllocations,
            boolean forUpdate) {
        MesProcessPoolActiveOrderProcessSnapshotDO frozen = requireFrozenProcessSnapshot(
                order.getId(), target.routeProcessId(), target.processId(), forUpdate);
        List<MesProcessPoolReportAllocationDO> projected = new ArrayList<>(otherAllocations.stream()
                .filter(row -> Objects.equals(row.getActiveOrderId(), order.getId())).toList());
        projected.add(MesProcessPoolReportAllocationDO.builder().eventId(currentEvent.getId())
                .activeOrderId(order.getId()).workOrderId(order.getWorkOrderId())
                .routeProcessId(target.routeProcessId()).processId(target.processId())
                .allocatedQuantity(currentQuantity).build());
        Set<Long> eventIds = projected.stream().map(MesProcessPoolReportAllocationDO::getEventId)
                .filter(id -> !Objects.equals(id, currentEvent.getId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProProcessPoolEventDO> events = new ArrayList<>();
        if (!eventIds.isEmpty()) {
            events.addAll(eventMapper.selectBatchIds(eventIds));
        }
        events.add(currentEvent);
        return MesOutputMaterialProgressCalculator.calculateMaximumProcessQuantity(order, frozen, events, projected);
    }

    private void assertWithinFrozenOverageLimit(Long activeOrderId, Long routeProcessId, Long processId,
                                                BigDecimal plannedQuantity, BigDecimal submittedQuantity,
                                                boolean forUpdate) {
        BigDecimal percent = requireFrozenOveragePercent(activeOrderId, routeProcessId, processId, forUpdate);
        BigDecimal limit = plannedQuantity.multiply(BigDecimal.ONE.add(percent.movePointLeft(2)));
        if (submittedQuantity.compareTo(limit) > 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_OVERAGE_LIMIT_EXCEEDED,
                    submittedQuantity, limit, routeProcessId, processId);
        }
    }

    private BigDecimal requireFrozenOveragePercent(Long activeOrderId, Long routeProcessId, Long processId,
                                                   boolean forUpdate) {
        return requireFrozenProcessSnapshot(activeOrderId, routeProcessId, processId, forUpdate)
                .getOveragePercentSnapshot();
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO requireFrozenProcessSnapshot(
            Long activeOrderId, Long routeProcessId, Long processId, boolean forUpdate) {
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = forUpdate
                ? activeOrderProcessSnapshotMapper.selectListByActiveOrderAndProcessForUpdate(activeOrderId, processId)
                .stream()
                .filter(row -> Objects.equals(row.getRouteProcessId(), routeProcessId))
                .findFirst()
                .orElse(null)
                : activeOrderProcessSnapshotMapper.selectByActiveOrderAndProcess(activeOrderId, routeProcessId, processId);
        if (snapshot == null || snapshot.getOveragePercentSnapshot() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                    "reportAllocation.overagePercentSnapshot activeOrderId=" + activeOrderId
                            + ", routeProcessId=" + routeProcessId + ", processId=" + processId);
        }
        return snapshot;
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

    private void assertActiveOrdersOpenForProduction(Collection<MesProcessPoolActiveOrderDO> activeOrders) {
        if (activeOrders == null || activeOrders.isEmpty()) {
            return;
        }
        Map<Long, MesProcessPoolActiveOrderDO> activeById = activeOrders.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(MesProcessPoolActiveOrderDO::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        assertActiveOrdersOpenForProduction(activeById.keySet(), activeById);
    }

    private void assertActiveOrdersOpenForProduction(Collection<Long> activeOrderIds,
                                                     Map<Long, MesProcessPoolActiveOrderDO> activeById) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()) {
            return;
        }
        for (Long activeOrderId : activeOrderIds) {
            MesProcessPoolActiveOrderDO activeOrder = activeById.get(activeOrderId);
            if (activeOrder == null) {
                activeOrder = activeOrderMapper.selectByIdForUpdate(activeOrderId);
            }
            if (activeOrder == null) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, activeOrderId);
            }
            if (!"ACTIVE".equals(activeOrder.getBusinessStatus())
                    || !"ACTIVE".equals(activeOrder.getActiveStatus())
                    || completionReceiptMapper.selectByActiveOrderIdForUpdate(activeOrderId) != null) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_RELEASED_LOCKED, activeOrderId);
            }
        }
        Set<Long> lockedIds = releaseStateService.findReleaseApplicationLockedActiveOrderIdsForUpdate(activeOrderIds);
        if (!lockedIds.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_RELEASED_LOCKED, lockedIds.iterator().next());
        }
    }

    private void assertSubmissionNotRejected(Long eventId) {
        MesProcessPoolSubmissionReviewDO review = reviewMapper.selectLatestByEventIdForUpdate(eventId);
        if (review != null && MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(review.getReviewStatus())
                && returnCorrectionResolver.find(requireEvent(eventId, true), review) == null) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS,
                    eventId, review.getReviewStatus());
        }
    }

    private Set<Long> allocationLockedIds(Collection<Long> ids,
            Map<Long, MesProcessPoolActiveOrderDO> knownOrders, boolean forUpdate) {
        Set<Long> locked = new LinkedHashSet<>(forUpdate
                ? releaseStateService.findReleaseApplicationLockedActiveOrderIdsForUpdate(ids)
                : releaseStateService.findReleaseApplicationLockedActiveOrderIds(ids));
        locked.addAll(forUpdate ? releaseStateService.findReleasedActiveOrderIdsForUpdate(ids)
                : releaseStateService.findReleasedActiveOrderIds(ids));
        for (Long id : ids) {
            MesProcessPoolActiveOrderDO order = knownOrders.get(id);
            if (order == null) order = forUpdate ? activeOrderMapper.selectByIdForUpdate(id) : activeOrderMapper.selectById(id);
            if (order == null) throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, id);
            if (!"ACTIVE".equals(order.getBusinessStatus()) || !"ACTIVE".equals(order.getActiveStatus())
                    || (forUpdate ? completionReceiptMapper.selectByActiveOrderIdForUpdate(id)
                        : completionReceiptMapper.selectByActiveOrderId(id)) != null) locked.add(id);
        }
        return locked;
    }
    private int requiredReviewRound(MesProcessPoolSubmissionReviewDO review) {
        cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.require(
                review.getReviewRound() != null && review.getReviewRound() >= 0, "正式复核轮次缺失，请核对迁移");
        return review.getReviewRound();
    }

    private void validateRejectCommand(Long eventId, Long leaderUserId,
                                       String rejectReason, String signaturePassword) {
        if (eventId == null || leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionReject");
        }
        if (StrUtil.isBlank(rejectReason)) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_REJECT_REMARK_REQUIRED, eventId);
        }
        if (StrUtil.isBlank(signaturePassword)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                    "productionReject.signaturePassword");
        }
    }

    private boolean isSameRejection(MesProcessPoolSubmissionReviewDO review,
                                    Long leaderUserId, String rejectReason) {
        return Objects.equals(review.getLeaderUserId(), leaderUserId)
                && Objects.equals(normalizeReason(review.getReviewRemark()), normalizeReason(rejectReason));
    }

    private String normalizeReason(String reason) {
        return StrUtil.isBlank(reason) ? null : StrUtil.trim(reason);
    }

    private void insertRejectionAudits(MesProProcessPoolEventDO event, Long leaderUserId,
                                       String rejectReason, int rejectedVersion,
                                       List<MesProcessPoolReportAllocationDO> current) {
        List<MesProcessPoolReportAllocationAdjustmentAuditDO> audits = current.stream()
                .map(allocation -> MesProcessPoolReportAllocationAdjustmentAuditDO.builder()
                        .eventId(event.getId())
                        .allocationVersion(rejectedVersion)
                        .sourceAllocationId(allocation.getId())
                        .activeOrderId(allocation.getActiveOrderId())
                        .workOrderId(allocation.getWorkOrderId())
                        .routeProcessId(allocation.getRouteProcessId())
                        .processId(allocation.getProcessId())
                        .beforeQuantity(allocation.getAllocatedQuantity())
                        .afterQuantity(BigDecimal.ZERO)
                        .deltaQuantity(allocation.getAllocatedQuantity().negate())
                        .actorUserId(leaderUserId)
                        .adjustmentReason(rejectReason.trim())
                        .allocationMode(MesProcessPoolReportAllocationDO.MODE_SYSTEM)
                        .changeSource(MesProcessPoolReportAllocationAdjustmentAuditDO.SOURCE_SUBMISSION_REJECTED)
                        .occurredAt(LocalDateTime.now())
                        .build())
                .toList();
        if (!Boolean.TRUE.equals(auditMapper.insertBatch(audits))) {
            throw new IllegalStateException("Failed to insert production rejection allocation audits: " + event.getId());
        }
    }

    private ReviewSignaturePayload recordRejectionSignature(MesProProcessPoolEventDO event,
                                                               Long leaderUserId,
                                                               String signaturePassword, MesSubmissionReviewExpectedContext expectedReview) {
        var correction = returnCorrectionResolver.find(event, reviewMapper.selectLatestByEventIdForUpdate(event.getId()));
        Long signatureId = correction == null ? signatureService.recordTeamLeaderReviewSignature(
                leaderUserId, signaturePassword, "组长驳回生产报工:PRODUCTION:" + event.getId(),
                "PROCESS_POOL_EVENT", event.getId(), "生产报工组长驳回")
                : signatureService.recordTeamLeaderReviewSignature(leaderUserId, signaturePassword,
                    "组长驳回本人更正报工:PRODUCTION:" + event.getId(), reviewSignatureContext(event, "REJECTED", correction));
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("signatureId", signatureId);
        snapshot.put("actorId", leaderUserId);
        snapshot.put("actionType", MesProBatchRecordExecutionSignatureService.ACTION_TEAM_LEADER_REVIEW);
        snapshot.put("processPoolEventId", event.getId());
        snapshot.put("eventType", event.getEventType());
        snapshot.put("leaderType", MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION);
        snapshot.put("reviewStatus", MesProcessPoolSubmissionReviewDO.STATUS_REJECTED);
        snapshot.put("expectedReview", expectedReview);
        addCorrectionEvidence(snapshot, event, correction);
        return new ReviewSignaturePayload(signatureId, leaderUserId, JsonUtils.toJsonString(snapshot));
    }

    private ReviewSignaturePayload recordApprovedReviewSignature(MesProProcessPoolEventDO event,
                                                                  MesReportAllocationSaveCommand command,
                                                                  LocalDateTime reviewedAt) {
        var correction = returnCorrectionResolver.find(event, reviewMapper.selectLatestByEventIdForUpdate(event.getId()));
        Long signatureId = correction == null ? signatureService.recordTeamLeaderReviewSignature(command.getLeaderUserId(),
                command.getSignaturePassword(), "组长报工分配确认:PRODUCTION:" + event.getId(),
                "PROCESS_POOL_EVENT", event.getId(), "生产报工组长复核")
                : signatureService.recordTeamLeaderReviewSignature(command.getLeaderUserId(), command.getSignaturePassword(),
                    "组长复核本人更正报工:PRODUCTION:" + event.getId(), reviewSignatureContext(event, "APPROVED", correction));
        return new ReviewSignaturePayload(signatureId, command.getLeaderUserId(),
                buildApprovedReviewSignatureSnapshot(event, command, signatureId, reviewedAt));
    }

    private String buildApprovedReviewSignatureSnapshot(MesProProcessPoolEventDO event,
                                                        MesReportAllocationSaveCommand command,
                                                        Long signatureId,
                                                        LocalDateTime reviewedAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("signatureId", signatureId);
        snapshot.put("actorId", command.getLeaderUserId());
        snapshot.put("expectedReview", command.getExpectedReview());
        snapshot.put("actionType", MesProBatchRecordExecutionSignatureService.ACTION_TEAM_LEADER_REVIEW);
        snapshot.put("processPoolEventId", event.getId());
        snapshot.put("eventType", event.getEventType());
        snapshot.put("leaderType", command.getLeaderType());
        snapshot.put("reviewStatus", MesProcessPoolSubmissionReviewDO.STATUS_APPROVED);
        snapshot.put("reviewedAt", reviewedAt);
        addCorrectionEvidence(snapshot, event, returnCorrectionResolver.find(event, reviewMapper.selectLatestByEventIdForUpdate(event.getId())));
        return JsonUtils.toJsonString(snapshot);
    }
    private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesTeamLeaderReviewSignatureContext reviewSignatureContext(
            MesProProcessPoolEventDO event, String decision,
            cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO correction) {
        return new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesTeamLeaderReviewSignatureContext(event.getId(), decision,
                cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                correction.getId(), correction.getRevisionSignatureId(), JsonUtils.parseTree(correction.getAfterPayload()).path("supersededReviewId").longValue());
    }
    private void addCorrectionEvidence(Map<String,Object> snapshot, MesProProcessPoolEventDO event,
            cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO correction) {
        snapshot.put("payloadHash", cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()));
        snapshot.put("originalSubmissionSignatureId", event.getSignatureId());
        if (correction != null) {
            snapshot.put("revisionId", correction.getId()); snapshot.put("revisionSignatureId", correction.getRevisionSignatureId());
            snapshot.put("supersededReviewId", JsonUtils.parseTree(correction.getAfterPayload()).path("supersededReviewId").longValue());
        }
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

    private void validateDisplayedProductionContext(MesProProcessPoolEventDO event,
            MesProcessPoolReportAllocationStateDO state, MesSubmissionReviewExpectedContext expected) {
        var review = reviewMapper.selectLatestByEventIdForUpdate(event.getId());
        var revisions = revisionMapper.selectListByEventIdForUpdate(event.getId());
        if (revisions == null) throw new IllegalStateException("生产更正版本读取失败");
        Long revisionId = revisions.isEmpty() ? 0L : revisions.get(0).getId();
        if (expected == null || !Objects.equals(event.getId(), expected.getEventId())
                || !Objects.equals(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher.sha256(event.getRawPayload()), expected.getPayloadHash())
                || !Objects.equals(revisionId, expected.getRevisionId())
                || !Objects.equals(review == null ? 0L : review.getId(), expected.getReviewId())
                || !Objects.equals(review == null ? 0 : review.getReviewRound(), expected.getReviewRound())
                || !Objects.equals(review == null ? "PENDING" : review.getReviewStatus(), expected.getReviewStatus())
                || !Objects.equals(state.getCurrentVersion(), expected.getAllocationVersion())) {
            throw new IllegalStateException("生产正文、修订或复核轮次已变化，请刷新后重新复核");
        }
    }

    private String requestHash(MesReportAllocationSaveCommand command) {
        String canonical = command.getAllocationMode() + "\n" + Objects.toString(command.getReason(), "") + "\n"
                + JsonUtils.toJsonString(command.getExpectedReview()) + "\n"
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

    private record ReviewEvidenceRequirement(boolean required,
                                             MesProcessPoolSubmissionReviewDO reviewToBackfill) {
    }

    private record ReviewSignaturePayload(Long reviewSignatureId,
                                          Long reviewSignatureUserId,
                                          String reviewSignatureSnapshotJson) {
    }
}
