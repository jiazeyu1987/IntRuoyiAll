package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MesTeamLeaderActiveOrderReleaseGenerationService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int MAX_APPLY_REMARK_LENGTH = 500;
    private static final String BUSINESS_KEY_PREFIX = "PQC_RELEASE";
    private static final String TASK_TYPE_PQC_RELEASE = "PQC_PRODUCTION_RELEASE";
    private static final String BUSINESS_SCOPE_RELEASE_APPLICATION = "RELEASE_APPLICATION";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistenceService;
    private final MesProductionReleaseRequiredCandidateResolver candidateResolver;
    private final MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher sourceSnapshotHasher;
    private final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort;

    public MesTeamLeaderActiveOrderReleaseGenerationService(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
            MesProcessPoolOrderProcessCompletionMapper completionMapper,
            MesPqcInspectionTaskMapper pqcTaskMapper,
            MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrWorkTaskMapper workTaskMapper,
            MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistenceService,
            MesProductionReleaseRequiredCandidateResolver candidateResolver,
            MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher sourceSnapshotHasher,
            MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.processSnapshotMapper = processSnapshotMapper;
        this.completionMapper = completionMapper;
        this.allocationMapper = allocationMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.aggregateDetailMapper = aggregateDetailMapper;
        this.applicationMapper = applicationMapper;
        this.workTaskMapper = workTaskMapper;
        this.persistenceService = persistenceService;
        this.candidateResolver = candidateResolver;
        this.sourceSnapshotHasher = sourceSnapshotHasher;
        this.completionReceiptPort = completionReceiptPort;
    }

    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseApplicationResult generate(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        String requestKey = validateCommand(leaderUserId, command);
        Long tenantId = requireTenantId();
        MesProcessPoolActiveOrderDO activeOrder = requireActiveOrder(command.getActiveOrderId(), leaderUserId);
        MesProWorkOrderDO workOrder = requireWorkOrder(activeOrder);
        String batchCode = requireBatchCode(workOrder);
        requireCompletionReceipt(activeOrder, workOrder, batchCode, tenantId);
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = requireSnapshots(activeOrder);
        requireNoProductionQuantityConflict(activeOrder, snapshots);
        List<MesProcessPoolOrderProcessCompletionDO> completions =
                requireFormalProductionCompletions(activeOrder, snapshots);
        InspectionEvidence inspectionEvidence = requireFormalInspectionCompletions(activeOrder, snapshots);
        String sourceSnapshotHash = sourceSnapshotHasher.hash(
                new MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.Input(
                        tenantId, activeOrder, workOrder, snapshots, completions,
                        inspectionEvidence.tasks(), inspectionEvidence.details()));
        String businessKey = businessKey(tenantId, activeOrder, workOrder, batchCode);

        MesProcessPoolActiveOrderReleaseApplicationDO requestExisting =
                applicationMapper.selectByRequestIdempotencyKey(activeOrder.getId(), requestKey);
        if (requestExisting != null) {
            if (!sameRequestPayload(requestExisting, activeOrder, workOrder, batchCode,
                    businessKey, sourceSnapshotHash)) {
                throw blocker(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT,
                        requestExisting.getApplicationStatus(), "RELEASE_APPLICATION",
                        String.valueOf(requestExisting.getId()), null,
                        "the request key is already bound to a different authoritative snapshot",
                        "query the existing receipt and submit a new command only for a new business identity");
            }
            return persistenceService.toResult(requireCurrentApplication(requestExisting));
        }

        MesProcessPoolActiveOrderReleaseApplicationDO businessExisting =
                applicationMapper.selectByBusinessIdempotencyKey(activeOrder.getId(), businessKey);
        if (businessExisting != null) {
            return persistenceService.toResult(requireCurrentApplication(businessExisting));
        }

        MesProductionReleaseRoleCandidates candidates = candidateResolver.resolveRequiredCandidates(
                tenantId, MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER);
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                buildApplication(activeOrder, workOrder, batchCode, requestKey, businessKey,
                        sourceSnapshotHash, leaderUserId, command.getApplyRemark());
        return persistenceService.persistPending(application, candidates);
    }

    public MesTeamLeaderActiveOrderReleaseApplicationResult get(Long userId, Long activeOrderId) {
        if (userId == null || activeOrderId == null || activeOrderId <= 0) {
            throw forbidden(activeOrderId);
        }
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                applicationMapper.selectLatestByActiveOrderId(activeOrderId);
        if (application == null) {
            throw blocker(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE, null,
                    "RELEASE_APPLICATION", null, String.valueOf(activeOrderId),
                    "production release application does not exist",
                    "complete the SP-1 production release application first");
        }
        requireCurrentApplication(application);
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
        if (activeOrder == null) {
            throw forbidden(activeOrderId);
        }
        if (!Objects.equals(userId, activeOrder.getLeaderUserId())) {
            MesProEdhrWorkTaskDO task = application.getPqcReleaseWorkTaskId() == null
                    ? null : workTaskMapper.selectById(application.getPqcReleaseWorkTaskId());
            if (!isFrozenPqcCandidate(task, application, userId)) {
                throw forbidden(activeOrderId);
            }
        }
        return persistenceService.toResult(application);
    }

    private String validateCommand(Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        if (leaderUserId == null || command == null || command.getActiveOrderId() == null
                || command.getActiveOrderId() <= 0) {
            throw forbidden(command == null ? null : command.getActiveOrderId());
        }
        String remark = StrUtil.trim(command.getApplyRemark());
        if (remark != null && remark.length() > MAX_APPLY_REMARK_LENGTH) {
            throw blocker(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION, null,
                    "RELEASE_APPLICATION", null, String.valueOf(command.getActiveOrderId()),
                    "applyRemark exceeds 500 characters", "shorten applyRemark to at most 500 characters");
        }
        command.setApplyRemark(remark);
        return MesReleaseFlowIdempotency.requireKey(command.getIdempotencyKey());
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw blocker(MesReleaseFlowBlockerType.FROZEN_ROUTE_SOURCE_REQUIRED, null,
                    "TENANT", null, null, "tenant context is required", "retry in an authenticated tenant context");
        }
        return tenantId;
    }

    private MesProcessPoolActiveOrderDO requireActiveOrder(Long activeOrderId, Long leaderUserId) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(activeOrderId);
        if (activeOrder == null || !Objects.equals(leaderUserId, activeOrder.getLeaderUserId())) {
            throw forbidden(activeOrderId);
        }
        if (!STATUS_ACTIVE.equals(activeOrder.getActiveStatus()) || activeOrder.getWorkOrderId() == null
                || activeOrder.getRouteId() == null || activeOrder.getRouteVersionId() == null) {
            throw blocker(MesReleaseFlowBlockerType.FROZEN_ROUTE_SOURCE_REQUIRED, null,
                    "ACTIVE_ORDER", String.valueOf(activeOrderId), null,
                    "active order lacks an active frozen route identity",
                    "repair the active order and published route version before applying");
        }
        return activeOrder;
    }

    private MesProWorkOrderDO requireWorkOrder(MesProcessPoolActiveOrderDO activeOrder) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectByIdForUpdate(activeOrder.getWorkOrderId());
        if (workOrder == null || workOrder.getProductId() == null) {
            throw blocker(MesReleaseFlowBlockerType.FROZEN_ROUTE_SOURCE_REQUIRED, null,
                    "WORK_ORDER", String.valueOf(activeOrder.getWorkOrderId()), null,
                    "formal work order and product identity are required",
                    "repair the formal work order before applying");
        }
        return workOrder;
    }

    private String requireBatchCode(MesProWorkOrderDO workOrder) {
        String batchCode = StrUtil.trim(workOrder.getBatchCode());
        if (StrUtil.isBlank(batchCode)) {
            throw blocker(MesReleaseFlowBlockerType.FROZEN_ROUTE_SOURCE_REQUIRED, null,
                    "WORK_ORDER", String.valueOf(workOrder.getId()), workOrder.getCode(),
                    "formal batch code is required", "assign the formal production batch code before applying");
        }
        return batchCode;
    }

    private List<MesProcessPoolActiveOrderProcessSnapshotDO> requireSnapshots(
            MesProcessPoolActiveOrderDO activeOrder) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots =
                processSnapshotMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        if (snapshots == null || snapshots.isEmpty()) {
            throw frozenRouteBlocker(activeOrder, null, null, "frozen route process snapshot is missing");
        }
        Set<ProcessIdentity> identities = new LinkedHashSet<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            if (snapshot == null || snapshot.getId() == null
                    || !Objects.equals(activeOrder.getId(), snapshot.getActiveOrderId())
                    || !Objects.equals(activeOrder.getWorkOrderId(), snapshot.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteId(), snapshot.getRouteId())
                    || !Objects.equals(activeOrder.getRouteVersionId(), snapshot.getRouteVersionId())
                    || snapshot.getRouteProcessId() == null || snapshot.getProcessId() == null
                    || !identities.add(new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId()))) {
                throw frozenRouteBlocker(activeOrder,
                        snapshot == null ? null : snapshot.getRouteProcessId(),
                        snapshot == null ? null : snapshot.getProcessId(),
                        "frozen route process snapshot is incomplete or duplicated");
            }
        }
        return List.copyOf(snapshots);
    }

    private List<MesProcessPoolOrderProcessCompletionDO> requireFormalProductionCompletions(
            MesProcessPoolActiveOrderDO activeOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        List<MesProcessPoolOrderProcessCompletionDO> all = list(
                completionMapper.selectListByWorkOrderIds(List.of(activeOrder.getWorkOrderId())));
        List<MesProcessPoolOrderProcessCompletionDO> matched = new ArrayList<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            List<MesProcessPoolOrderProcessCompletionDO> candidates = all.stream()
                    .filter(item -> item != null
                            && Objects.equals(activeOrder.getWorkOrderId(), item.getWorkOrderId())
                            && Objects.equals(snapshot.getRouteProcessId(), item.getRouteProcessId())
                            && Objects.equals(snapshot.getProcessId(), item.getProcessId()))
                    .toList();
            if (candidates.size() != 1 || !isFormalProductionComplete(candidates.get(0))) {
                throw progressBlocker(MesReleaseFlowBlockerType.PRODUCTION_PROGRESS_NOT_COMPLETED,
                        activeOrder, snapshot, "formal production progress is below 100%",
                        "complete and backfill the production confirmation for this frozen process");
            }
            matched.add(candidates.get(0));
        }
        return List.copyOf(matched);
    }

    private boolean isFormalProductionComplete(MesProcessPoolOrderProcessCompletionDO completion) {
        return completion.getId() != null
                && MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED.equals(completion.getCompletionStatus())
                && positive(completion.getTargetQuantity())
                && completion.getConfirmedQuantity() != null
                && completion.getConfirmedQuantity().compareTo(completion.getTargetQuantity()) >= 0
                && completion.getLastEventId() != null && completion.getLastReviewId() != null
                && StrUtil.isNotBlank(completion.getSourceEventIdsJson())
                && StrUtil.isNotBlank(completion.getSourceAllocationIdsJson())
                && StrUtil.isNotBlank(completion.getAggregateHash())
                && StrUtil.isNotBlank(completion.getBackfillIdempotencyKey());
    }

    private void requireCompletionReceipt(MesProcessPoolActiveOrderDO activeOrder,
                                          MesProWorkOrderDO workOrder,
                                          String batchCode,
                                          Long tenantId) {
        MesFlow6CompletionBackfillReceipt receipt = completionReceiptPort.getByActiveOrderId(
                activeOrder.getId(), tenantId);
        if (receipt == null || !MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED
                .equals(receipt.getStatus())
                || !Objects.equals(activeOrder.getId(), receipt.getActiveOrderId())
                || !Objects.equals(activeOrder.getWorkOrderId(), receipt.getWorkOrderId())
                || !Objects.equals(activeOrder.getRouteId(), receipt.getRouteId())
                || !Objects.equals(activeOrder.getRouteVersionId(), receipt.getRouteVersionId())
                || !Objects.equals(workOrder.getBatchCode(), receipt.getBatchCode())
                || !Objects.equals(batchCode, receipt.getBatchCode())
                || receipt.getBatchRecordId() == null || receipt.getProcessInspectionId() == null) {
            throw blocker(MesReleaseFlowBlockerType.FROZEN_ROUTE_SOURCE_REQUIRED, null,
                    "COMPLETION_BACKFILL_RECEIPT", String.valueOf(activeOrder.getId()), null,
                    "Flow-4 completion receipt is required before release application",
                    "complete the active order and retry the release application");
        }
    }

    private void requireNoProductionQuantityConflict(
            MesProcessPoolActiveOrderDO activeOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        Map<ProcessIdentity, BigDecimal> targetQuantityByProcess = new LinkedHashMap<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            targetQuantityByProcess.put(new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId()),
                    snapshot.getPlannedQuantitySnapshot());
        }

        Map<ProcessIdentity, BigDecimal> allocatedQuantityByProcess = new LinkedHashMap<>();
        for (MesProcessPoolReportAllocationDO allocation
                : list(allocationMapper.selectListByActiveOrderIds(List.of(activeOrder.getId())))) {
            if (allocation == null || !Objects.equals(activeOrder.getId(), allocation.getActiveOrderId())
                    || !Objects.equals(activeOrder.getWorkOrderId(), allocation.getWorkOrderId())) {
                throw blocker(MesReleaseFlowBlockerType.PRODUCTION_QUANTITY_CONFLICT, null,
                        "REPORT_ALLOCATION", allocation == null || allocation.getId() == null
                                ? null : String.valueOf(allocation.getId()), null,
                        "formal production allocation is outside the active-order identity",
                        "由组长纠正该工单工序的报工分配后重新申请放行");
            }
            if (allocation.getRouteProcessId() == null || allocation.getProcessId() == null
                    || allocation.getAllocatedQuantity() == null) {
                throw blocker(MesReleaseFlowBlockerType.PRODUCTION_QUANTITY_CONFLICT, null,
                        "REPORT_ALLOCATION", allocation.getId() == null ? null : String.valueOf(allocation.getId()),
                        null, "formal production allocation is incomplete",
                        "由组长补齐或纠正该工单工序的报工分配后重新申请放行");
            }
            ProcessIdentity identity = new ProcessIdentity(allocation.getRouteProcessId(), allocation.getProcessId());
            if (!targetQuantityByProcess.containsKey(identity)) {
                throw blocker(MesReleaseFlowBlockerType.PRODUCTION_QUANTITY_CONFLICT, null,
                        "REPORT_ALLOCATION", allocation.getId() == null ? null : String.valueOf(allocation.getId()),
                        null, "formal production allocation points to an unknown frozen process",
                        "由组长纠正该工单工序的报工分配后重新申请放行");
            }
            allocatedQuantityByProcess.merge(identity, allocation.getAllocatedQuantity(), BigDecimal::add);
        }

        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            ProcessIdentity identity = new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId());
            BigDecimal targetQuantity = targetQuantityByProcess.get(identity);
            BigDecimal allocatedQuantity = allocatedQuantityByProcess.getOrDefault(identity, BigDecimal.ZERO);
            if (!positive(targetQuantity)) {
                throw progressBlocker(MesReleaseFlowBlockerType.PRODUCTION_PROGRESS_NOT_COMPLETED,
                        activeOrder, snapshot, "formal production target is missing",
                        "补齐该冻结工序的正式生产目标后重新申请放行");
            }
            if (allocatedQuantity.compareTo(targetQuantity) > 0) {
                BigDecimal overageQuantity = allocatedQuantity.subtract(targetQuantity);
                throw progressBlocker(MesReleaseFlowBlockerType.PRODUCTION_QUANTITY_CONFLICT,
                        activeOrder, snapshot,
                        "formal production quantity exceeds the work-order process target by "
                                + quantityText(overageQuantity),
                        "由组长纠正该工单工序的报工数量后重新申请放行");
            }
        }
    }

    private InspectionEvidence requireFormalInspectionCompletions(
            MesProcessPoolActiveOrderDO activeOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        List<MesPqcInspectionTaskDO> allTasks = list(pqcTaskMapper.selectListByActiveOrderId(activeOrder.getId()));
        List<MesPqcProcessInspectionAggregateDetailDO> allDetails =
                list(aggregateDetailMapper.selectListByActiveOrderId(activeOrder.getId()));
        List<MesPqcInspectionTaskDO> matchedTasks = new ArrayList<>();
        List<MesPqcProcessInspectionAggregateDetailDO> matchedDetails = new ArrayList<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            List<MesPqcInspectionTaskDO> confirmed = allTasks.stream()
                    .filter(task -> isConfirmedInspectionTask(task, activeOrder, snapshot))
                    .toList();
            MesPqcProcessInspectionAggregateDetailDO detail = confirmed.stream()
                    .flatMap(task -> allDetails.stream()
                            .filter(item -> matchesInspectionDetail(item, task, activeOrder, snapshot)))
                    .findFirst().orElse(null);
            if (confirmed.isEmpty() || detail == null) {
                throw progressBlocker(MesReleaseFlowBlockerType.INSPECTION_PROGRESS_NOT_COMPLETED,
                        activeOrder, snapshot, "formal process inspection progress is below 100%",
                        "confirm the PQC task and aggregate detail for this frozen process");
            }
            matchedTasks.add(confirmed.get(0));
            matchedDetails.add(detail);
        }
        return new InspectionEvidence(List.copyOf(matchedTasks), List.copyOf(matchedDetails));
    }

    private boolean isConfirmedInspectionTask(MesPqcInspectionTaskDO task,
                                               MesProcessPoolActiveOrderDO activeOrder,
                                               MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        return task != null && task.getId() != null
                && MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus())
                && Objects.equals(activeOrder.getId(), task.getActiveOrderId())
                && Objects.equals(activeOrder.getWorkOrderId(), task.getWorkOrderId())
                && Objects.equals(activeOrder.getRouteId(), task.getRouteId())
                && Objects.equals(activeOrder.getRouteVersionId(), task.getRouteVersionId())
                && Objects.equals(snapshot.getRouteProcessId(), task.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), task.getProcessId());
    }

    private boolean matchesInspectionDetail(MesPqcProcessInspectionAggregateDetailDO detail,
                                            MesPqcInspectionTaskDO task,
                                            MesProcessPoolActiveOrderDO activeOrder,
                                            MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        return detail != null && detail.getId() != null && Objects.equals(task.getId(), detail.getPqcTaskId())
                && Objects.equals(activeOrder.getId(), detail.getActiveOrderId())
                && Objects.equals(activeOrder.getWorkOrderId(), detail.getWorkOrderId())
                && Objects.equals(activeOrder.getRouteId(), detail.getRouteId())
                && Objects.equals(activeOrder.getRouteVersionId(), detail.getRouteVersionId())
                && Objects.equals(snapshot.getRouteProcessId(), detail.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), detail.getProcessId());
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO buildApplication(
            MesProcessPoolActiveOrderDO activeOrder, MesProWorkOrderDO workOrder, String batchCode,
            String requestKey, String businessKey, String sourceSnapshotHash,
            Long leaderUserId, String remark) {
        return new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(workOrder.getId())
                .setWorkOrderCode(workOrder.getCode())
                .setRouteId(activeOrder.getRouteId())
                .setRouteVersionId(activeOrder.getRouteVersionId())
                .setProductId(workOrder.getProductId())
                .setBatchCode(batchCode)
                .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setVersion(1)
                .setRequestIdempotencyKey(requestKey)
                .setBusinessIdempotencyKey(businessKey)
                .setAppliedBy(leaderUserId)
                .setAppliedAt(LocalDateTime.now())
                .setRemark(remark);
    }

    private String businessKey(Long tenantId, MesProcessPoolActiveOrderDO activeOrder,
                               MesProWorkOrderDO workOrder, String batchCode) {
        return DigestUtil.sha256Hex(String.join("|", BUSINESS_KEY_PREFIX,
                String.valueOf(tenantId), String.valueOf(activeOrder.getId()),
                String.valueOf(workOrder.getId()), batchCode,
                String.valueOf(activeOrder.getRouteId()), String.valueOf(activeOrder.getRouteVersionId())));
    }

    private boolean sameRequestPayload(MesProcessPoolActiveOrderReleaseApplicationDO existing,
                                       MesProcessPoolActiveOrderDO activeOrder,
                                       MesProWorkOrderDO workOrder,
                                       String batchCode,
                                       String businessKey,
                                       String sourceSnapshotHash) {
        return Objects.equals(activeOrder.getId(), existing.getActiveOrderId())
                && Objects.equals(workOrder.getId(), existing.getWorkOrderId())
                && Objects.equals(batchCode, existing.getBatchCode())
                && Objects.equals(activeOrder.getRouteId(), existing.getRouteId())
                && Objects.equals(activeOrder.getRouteVersionId(), existing.getRouteVersionId())
                && Objects.equals(businessKey, existing.getBusinessIdempotencyKey())
                && Objects.equals(sourceSnapshotHash, existing.getSourceSnapshotHash());
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO requireCurrentApplication(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (application == null || !MesReleaseFlowStatus.isPersistentStatus(application.getApplicationStatus())
                || application.getVersion() == null || application.getVersion() <= 0) {
            throw blocker(MesReleaseFlowBlockerType.LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED,
                    application == null ? null : application.getApplicationStatus(),
                    "RELEASE_APPLICATION", application == null ? null : String.valueOf(application.getId()), null,
                    "legacy release application cannot enter the production release flow",
                    "migrate the legacy application with approved evidence before retrying");
        }
        return application;
    }

    private boolean isFrozenPqcCandidate(MesProEdhrWorkTaskDO task,
                                         MesProcessPoolActiveOrderReleaseApplicationDO application,
                                         Long userId) {
        if (task == null || !Objects.equals(application.getId(), task.getBusinessScopeId())
                || !BUSINESS_SCOPE_RELEASE_APPLICATION.equals(task.getBusinessScopeType())
                || !TASK_TYPE_PQC_RELEASE.equals(task.getTaskType())
                || StrUtil.isBlank(task.getCandidateUserSnapshot())) {
            return false;
        }
        return List.of(task.getCandidateUserSnapshot().split(",")).stream()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .anyMatch(value -> value.equals(String.valueOf(userId)));
    }

    private MesReleaseFlowBlockerException forbidden(Long activeOrderId) {
        return blocker(MesReleaseFlowBlockerType.ACTIVE_ORDER_LEADER_FORBIDDEN, null,
                "ACTIVE_ORDER", activeOrderId == null ? null : String.valueOf(activeOrderId), null,
                "current user is not authorized for this active order release application",
                "use the assigned production leader or a frozen PQC candidate");
    }

    private MesReleaseFlowBlockerException frozenRouteBlocker(
            MesProcessPoolActiveOrderDO activeOrder, Long routeProcessId, Long processId, String reason) {
        return blocker(null, new MesReleaseFlowBlocker()
                .setBlockerType(MesReleaseFlowBlockerType.FROZEN_ROUTE_SOURCE_REQUIRED)
                .setObjectType("ACTIVE_ORDER")
                .setObjectId(String.valueOf(activeOrder.getId()))
                .setRouteProcessId(routeProcessId)
                .setProcessId(processId)
                .setReason(reason)
                .setSuggestion("repair the frozen active-order route snapshot before applying"));
    }

    private MesReleaseFlowBlockerException progressBlocker(
            MesReleaseFlowBlockerType type,
            MesProcessPoolActiveOrderDO activeOrder,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            String reason,
            String suggestion) {
        return blocker(null, new MesReleaseFlowBlocker()
                .setBlockerType(type)
                .setObjectType("ACTIVE_ORDER_PROCESS")
                .setObjectId(String.valueOf(activeOrder.getId()))
                .setRouteProcessId(snapshot.getRouteProcessId())
                .setProcessId(snapshot.getProcessId())
                .setReason(reason)
                .setSuggestion(suggestion));
    }

    private MesReleaseFlowBlockerException blocker(MesReleaseFlowBlockerType type,
                                                   String currentStatus,
                                                   String objectType,
                                                   String objectId,
                                                   String objectCode,
                                                   String reason,
                                                   String suggestion) {
        return blocker(currentStatus, new MesReleaseFlowBlocker()
                .setBlockerType(type)
                .setObjectType(objectType)
                .setObjectId(objectId)
                .setObjectCode(objectCode)
                .setReason(reason)
                .setSuggestion(suggestion));
    }

    private MesReleaseFlowBlockerException blocker(String currentStatus, MesReleaseFlowBlocker flowBlocker) {
        return new MesReleaseFlowBlockerException(flowBlocker.getReason(), new MesReleaseFlowFailureRespVO()
                .setStage(MesReleaseFlowStage.SP_1)
                .setCurrentStatus(currentStatus)
                .setBlockers(List.of(flowBlocker)));
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private String quantityText(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private <T> List<T> list(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record InspectionEvidence(List<MesPqcInspectionTaskDO> tasks,
                                      List<MesPqcProcessInspectionAggregateDetailDO> details) {
    }
}
