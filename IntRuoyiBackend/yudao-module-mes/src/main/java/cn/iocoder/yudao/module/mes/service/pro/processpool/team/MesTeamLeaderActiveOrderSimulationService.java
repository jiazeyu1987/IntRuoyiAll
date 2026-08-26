package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolQuantityFragmentCreateDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;

@Service
@Validated
public class MesTeamLeaderActiveOrderSimulationService {

    private static final String ACTIVE_STATUS_ACTIVE = "ACTIVE";
    private static final String PQC_INSPECTION_TASK_SOURCE_TYPE = "MES_PQC_INSPECTION_TASK";
    private static final String SIMULATION_TEMPLATE_TYPE_PRODUCTION = "SIMULATED_PRODUCTION_SUBMIT";
    private static final String SIMULATION_TEMPLATE_TYPE_PQC = "SIMULATED_PQC_INSPECTION";
    private static final String SIMULATION_SOURCE_TYPE = "MES_ACTIVE_ORDER_SIMULATION";
    private static final String INSPECTION_TYPE_PATROL = "PATROL";
    private static final String JUDGEMENT_PASS = "SUCCESS";
    private static final BigDecimal PERCENT_DIVISOR = BigDecimal.valueOf(100);
    private static final int PROGRESS_PERCENT_SCALE = 6;

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProcessPoolReportAllocationMapper reportAllocationMapper;
    private final MesProcessPoolSubmissionReviewMapper submissionReviewMapper;
    private final MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    private final MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    private final MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    private final MesProcessPoolEventService processPoolEventService;
    private final MesReportAllocationCommandService reportAllocationCommandService;
    private final MesPqcProcessInspectionAggregationService pqcProcessInspectionAggregationService;
    private final AtomicLong simulationSequence = new AtomicLong();

    public MesTeamLeaderActiveOrderSimulationService(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
            MesProRouteVersionMapper routeVersionMapper,
            MesProcessPoolReportAllocationMapper reportAllocationMapper,
            MesProcessPoolSubmissionReviewMapper submissionReviewMapper,
            MesPqcInspectionTaskMapper pqcInspectionTaskMapper,
            MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper,
            MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper,
            MesProcessPoolEventService processPoolEventService,
            MesReportAllocationCommandService reportAllocationCommandService,
            MesPqcProcessInspectionAggregationService pqcProcessInspectionAggregationService) {
        this.activeOrderMapper = activeOrderMapper;
        this.processSnapshotMapper = processSnapshotMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.reportAllocationMapper = reportAllocationMapper;
        this.submissionReviewMapper = submissionReviewMapper;
        this.pqcInspectionTaskMapper = pqcInspectionTaskMapper;
        this.inspectionRegulationItemMapper = inspectionRegulationItemMapper;
        this.pqcPieceDetailMapper = pqcPieceDetailMapper;
        this.processPoolEventService = processPoolEventService;
        this.reportAllocationCommandService = reportAllocationCommandService;
        this.pqcProcessInspectionAggregationService = pqcProcessInspectionAggregationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderSimulationResult simulateActiveOrderCompletion(Long leaderUserId,
                                                                                  Long activeOrderId) {
        return simulateActiveOrderCompletion(leaderUserId, activeOrderId, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderSimulationResult simulateActiveOrderCompletion(Long leaderUserId,
                                                                                  Long activeOrderId,
                                                                                  String simulationStage,
                                                                                  String simulationRunId) {
        requirePositive(leaderUserId, "leaderUserId");
        requirePositive(activeOrderId, "activeOrderId");
        MesProcessPoolActiveOrderDO activeOrder = requireActiveOrderForLeader(leaderUserId, activeOrderId);
        MesProRouteVersionDO routeVersion = requireRouteVersion(activeOrder);
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots =
                processSnapshotMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        List<ProcessIdentity> snapshotIdentities = requireProgressProcessIdentities(activeOrder, snapshots);
        List<ProcessIdentity> formalIdentities = resolveFormalProgressProcessIdentities(activeOrder, routeVersion,
                snapshotIdentities);
        Set<ProcessIdentity> formalIdentitySet = new LinkedHashSet<>(formalIdentities);
        List<MesPqcInspectionTaskDO> pqcTasks =
                pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        validatePqcTasksBeforeWrite(activeOrder, formalIdentitySet, pqcTasks);

        ProductionSimulationSummary productionSummary =
                simulateProductionSubmissions(activeOrder, snapshots, leaderUserId, simulationStage,
                        simulationRunId);
        PqcSimulationSummary pqcSummary = simulatePqcSubmissions(activeOrder, formalIdentitySet, pqcTasks,
                leaderUserId, simulationStage, simulationRunId);
        ProgressSnapshot progress = calculateProgress(activeOrder, routeVersion, snapshots);
        if (progress.productionProgressPercent().compareTo(PERCENT_DIVISOR) != 0
                || progress.inspectionProgressPercent().compareTo(PERCENT_DIVISOR) != 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderSimulation.progress");
        }
        return MesTeamLeaderActiveOrderSimulationResult.builder()
                .activeOrderId(activeOrder.getId())
                .productionSubmitCount(productionSummary.productionSubmitCount())
                .productionReviewCount(productionSummary.productionReviewCount())
                .pqcSubmitCount(pqcSummary.pqcSubmitCount())
                .pqcReviewCount(pqcSummary.pqcReviewCount())
                .productionProgressPercent(progress.productionProgressPercent())
                .inspectionProgressPercent(progress.inspectionProgressPercent())
                .build();
    }

    private MesProcessPoolActiveOrderDO requireActiveOrderForLeader(Long leaderUserId, Long activeOrderId) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(activeOrderId);
        if (activeOrder == null
                || !Objects.equals(leaderUserId, activeOrder.getLeaderUserId())
                || !ACTIVE_STATUS_ACTIVE.equals(activeOrder.getActiveStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        requirePositive(activeOrder.getTenantId(), "activeOrder.tenantId");
        if (activeOrder.getWorkOrderId() == null || activeOrder.getRouteId() == null
                || activeOrder.getRouteVersionId() == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return activeOrder;
    }

    private MesProRouteVersionDO requireRouteVersion(MesProcessPoolActiveOrderDO activeOrder) {
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(activeOrder.getRouteVersionId());
        if (routeVersion == null || !Objects.equals(activeOrder.getRouteId(), routeVersion.getRouteId())) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        return routeVersion;
    }

    private void validatePqcTasksBeforeWrite(MesProcessPoolActiveOrderDO activeOrder,
                                             Set<ProcessIdentity> formalIdentitySet,
                                             List<MesPqcInspectionTaskDO> pqcTasks) {
        if (pqcTasks == null || pqcTasks.isEmpty()) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "活跃订单缺少固定 PQC 任务，activeOrderId=" + activeOrder.getId());
        }
        for (MesPqcInspectionTaskDO task : pqcTasks) {
            validatePqcTask(activeOrder, formalIdentitySet, task);
        }
    }

    private void validatePqcTask(MesProcessPoolActiveOrderDO activeOrder, Set<ProcessIdentity> formalIdentitySet,
                                 MesPqcInspectionTaskDO task) {
        if (task == null
                || !Objects.equals(activeOrder.getId(), task.getActiveOrderId())
                || !Objects.equals(activeOrder.getWorkOrderId(), task.getWorkOrderId())
                || !Objects.equals(activeOrder.getRouteId(), task.getRouteId())
                || !Objects.equals(activeOrder.getRouteVersionId(), task.getRouteVersionId())
                || !Objects.equals(activeOrder.getTenantId(), task.getTenantId())
                || task.getRouteProcessId() == null
                || task.getProcessId() == null
                || task.getQaProcessId() == null
                || task.getRegulationVersionId() == null
                || StrUtil.isBlank(task.getQaItemCode())
                || StrUtil.isBlank(task.getInspectionType())
                || task.getBusinessDate() == null
                || StrUtil.isBlank(task.getShiftCode())
                || task.getRoundNo() == null
                || !formalIdentitySet.contains(new ProcessIdentity(task.getRouteProcessId(), task.getProcessId()))
                || !(MesPqcInspectionTaskDO.TASK_STATUS_PENDING.equals(task.getTaskStatus())
                || MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED.equals(task.getTaskStatus())
                || MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus()))) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "活跃订单 PQC 任务身份不完整，activeOrderId=" + activeOrder.getId());
        }
        if (!MesPqcInspectionTaskDO.TASK_STATUS_PENDING.equals(task.getTaskStatus())
                && (task.getActualInspectionQuantity() == null || task.getActualInspectionQuantity() <= 0)) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "活跃订单 PQC 任务检验数量不完整，activeOrderId=" + activeOrder.getId());
        }
    }

    private ProductionSimulationSummary simulateProductionSubmissions(
            MesProcessPoolActiveOrderDO activeOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
            Long leaderUserId, String simulationStage, String simulationRunId) {
        Map<ProcessIdentity, BigDecimal> allocatedByProcess = aggregateAllocatedByProcess(activeOrder.getId());
        int submitCount = 0;
        int reviewCount = 0;
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            ProcessIdentity process = new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId());
            BigDecimal plannedQuantity = requirePlannedQuantity(activeOrder, snapshot);
            BigDecimal allocatedQuantity = allocatedByProcess.getOrDefault(process, BigDecimal.ZERO);
            BigDecimal remainingQuantity = plannedQuantity.subtract(allocatedQuantity);
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Long eventId = createProductionSubmitEvent(activeOrder, snapshot, remainingQuantity, leaderUserId,
                    simulationStage, simulationRunId);
            reportAllocationCommandService.createInitialAllocation(eventId, activeOrder.getId(), remainingQuantity);
            markSimulationAllocations(eventId, simulationStage, simulationRunId);
            Long reviewId = insertApprovedReview(eventId, leaderUserId,
                    MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, "模拟生产组长复核",
                    simulationStage, simulationRunId);
            linkAllocationsToReview(eventId, reviewId);
            submitCount++;
            reviewCount++;
        }
        return new ProductionSimulationSummary(submitCount, reviewCount);
    }

    private Long createProductionSubmitEvent(MesProcessPoolActiveOrderDO activeOrder,
                                             MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                             BigDecimal quantity,
                                             Long leaderUserId, String simulationStage,
                                             String simulationRunId) {
        LocalDateTime now = LocalDateTime.now();
        String idempotencyKey = "SIM-AO-PROD-" + activeOrder.getId() + "-" + snapshot.getRouteProcessId()
                + "-" + snapshot.getProcessId();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("simulated", true);
        payload.put("activeOrderId", activeOrder.getId());
        payload.put("routeProcessId", snapshot.getRouteProcessId());
        payload.put("processId", snapshot.getProcessId());
        payload.put("outputQuantity", quantity);
        payload.put("source", "active-order-simulate-completion");
        putSimulationMetadata(payload, simulationStage, simulationRunId);
        if (simulationRunId != null && !simulationRunId.isBlank()) {
            idempotencyKey = idempotencyKey + "-" + simulationRunId;
        }
        return processPoolEventService.createEvent(MesProcessPoolCreateEventReqDTO.builder()
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .eventIdempotencyKey(idempotencyKey)
                .workOrderId(activeOrder.getWorkOrderId())
                .routeId(activeOrder.getRouteId())
                .routeProcessId(snapshot.getRouteProcessId())
                .processId(snapshot.getProcessId())
                .actualEmployeeId(leaderUserId)
                .deviceAccountId(leaderUserId)
                .workstationId(leaderUserId)
                .templateType(SIMULATION_TEMPLATE_TYPE_PRODUCTION)
                .feedbackSourceType(SIMULATION_SOURCE_TYPE)
                .feedbackSourceId(activeOrder.getId())
                .rawPayload(JsonUtils.toJsonString(payload))
                .clientSubmitTime(now)
                .signatureId(nextSimulationSignatureId())
                .signatureUserId(leaderUserId)
                .signatureSnapshot(simulationSignatureSnapshot(leaderUserId, "PRODUCTION_SUBMIT",
                        activeOrder.getId(), now, simulationStage, simulationRunId))
                .simulated(simulationStage != null && !simulationStage.isBlank())
                .simulationStage(simulationStage)
                .simulationRunId(simulationRunId)
                .quantityFragments(List.of(MesProcessPoolQuantityFragmentCreateDTO.builder()
                        .sourceQuantityType(MesProProcessPoolQuantityFragmentDO.SOURCE_QUANTITY_TYPE_OUTPUT)
                        .totalQuantity(quantity)
                        .rawPayload(JsonUtils.toJsonString(payload))
                        .simulated(simulationStage != null && !simulationStage.isBlank())
                        .simulationStage(simulationStage)
                        .simulationRunId(simulationRunId)
                        .build()))
                .build());
    }

    private void markSimulationAllocations(Long eventId, String simulationStage, String simulationRunId) {
        if (simulationStage == null || simulationStage.isBlank()
                || simulationRunId == null || simulationRunId.isBlank()) {
            return;
        }
        for (MesProcessPoolReportAllocationDO allocation :
                reportAllocationMapper.selectListByEventIdForUpdate(eventId)) {
            allocation.setSimulated(Boolean.TRUE)
                    .setSimulationStage(simulationStage)
                    .setSimulationRunId(simulationRunId);
            reportAllocationMapper.updateById(allocation);
        }
    }

    private void linkAllocationsToReview(Long eventId, Long reviewId) {
        List<MesProcessPoolReportAllocationDO> allocations =
                reportAllocationMapper.selectListByEventIdForUpdate(eventId);
        if (allocations.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionAllocation.reviewId");
        }
        for (MesProcessPoolReportAllocationDO allocation : allocations) {
            if (allocation.getReviewId() != null && !Objects.equals(allocation.getReviewId(), reviewId)) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionAllocation.reviewId");
            }
            if (allocation.getReviewId() == null) {
                allocation.setReviewId(reviewId);
                reportAllocationMapper.updateById(allocation);
            }
        }
    }

    private PqcSimulationSummary simulatePqcSubmissions(MesProcessPoolActiveOrderDO activeOrder,
                                                        Set<ProcessIdentity> formalIdentitySet,
                                                        List<MesPqcInspectionTaskDO> lockedTasks,
                                                        Long leaderUserId, String simulationStage,
                                                        String simulationRunId) {
        int submitCount = 0;
        int reviewCount = 0;
        for (MesPqcInspectionTaskDO task : lockedTasks) {
            if (MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus())) {
                continue;
            }
            Long eventId;
            if (MesPqcInspectionTaskDO.TASK_STATUS_PENDING.equals(task.getTaskStatus())) {
                eventId = submitPqcTask(activeOrder, task, leaderUserId, simulationStage, simulationRunId);
                submitCount++;
            } else if (MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED.equals(task.getTaskStatus())) {
                eventId = requirePositive(task.getSubmittedEventId(), "pqcTask.submittedEventId");
            } else {
                throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                        "活跃订单 PQC 任务状态不可模拟，activeOrderId=" + activeOrder.getId());
            }
            Long reviewId = insertApprovedReview(eventId, leaderUserId,
                    MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, "模拟PQC组长复核",
                    simulationStage, simulationRunId);
            pqcProcessInspectionAggregationService.aggregateApprovedPqcSubmission(eventId, reviewId);
            reviewCount++;
        }
        BigDecimal inspectionProgressPercent = calculateInspectionProgressPercent(activeOrder, formalIdentitySet,
                pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId()));
        if (inspectionProgressPercent.compareTo(PERCENT_DIVISOR) != 0) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "活跃订单固定 PQC 任务确认后仍未完成，activeOrderId=" + activeOrder.getId());
        }
        return new PqcSimulationSummary(submitCount, reviewCount);
    }

    private Long submitPqcTask(MesProcessPoolActiveOrderDO activeOrder, MesPqcInspectionTaskDO task,
                               Long leaderUserId, String simulationStage, String simulationRunId) {
        Integer actualInspectionQuantity = requirePositiveInteger(task.getPlannedInspectionQuantity(),
                "pqcTask.plannedInspectionQuantity");
        List<MesPqcInspectionPieceDetailDO> existingDetails = pqcPieceDetailMapper.selectListByTaskId(task.getId());
        if (!existingDetails.isEmpty()) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "待提交 PQC 任务已经存在逐件明细，taskId=" + task.getId());
        }
        List<MesPqcInspectionPieceDetailDO> pieceDetails = buildSimulatedPieceDetails(task,
                actualInspectionQuantity, simulationStage, simulationRunId);
        pieceDetails.forEach(pieceDetail -> pieceDetail.setTenantId(task.getTenantId()));
        if (!Boolean.TRUE.equals(pqcPieceDetailMapper.insertBatch(pieceDetails))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcPieceDetails");
        }
        String contentHash = "SIMULATED:" + task.getId() + ":" + actualInspectionQuantity;
        int updated = pqcInspectionTaskMapper.updateSubmittedIfPending(task.getId(), actualInspectionQuantity,
                contentHash, MesPqcInspectionTaskDO.TASK_STATUS_PENDING,
                MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED);
        if (updated != 1) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "PQC任务提交状态更新失败，taskId=" + task.getId());
        }
        pqcInspectionTaskMapper.updateSimulationMetadata(task.getId(),
                simulationStage != null && !simulationStage.isBlank(), simulationStage, simulationRunId);
        LocalDateTime now = LocalDateTime.now();
        String pqcIdempotencyKey = "SIM-AO-PQC-" + task.getId();
        if (simulationRunId != null && !simulationRunId.isBlank()) {
            pqcIdempotencyKey = pqcIdempotencyKey + "-" + simulationRunId;
        }
        Long eventId = processPoolEventService.createPqcInspectionEvent(MesProcessPoolCreatePqcInspectionReqDTO
                .builder()
                .workOrderId(task.getWorkOrderId())
                .pqcSubmissionIdempotencyKey(pqcIdempotencyKey)
                .routeId(task.getRouteId())
                .qaProcessId(task.getQaProcessId())
                .actualEmployeeId(leaderUserId)
                .templateType(SIMULATION_TEMPLATE_TYPE_PQC)
                .feedbackSourceType(PQC_INSPECTION_TASK_SOURCE_TYPE)
                .feedbackSourceId(task.getId())
                .recordbookSourceType(PQC_INSPECTION_TASK_SOURCE_TYPE)
                .recordbookSourceId(task.getId())
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .rawPayload(buildPqcRawPayload(activeOrder, task, actualInspectionQuantity, simulationStage,
                        simulationRunId))
                .clientSubmitTime(now)
                .signatureId(nextSimulationSignatureId())
                .signatureUserId(leaderUserId)
                .signatureSnapshot(simulationSignatureSnapshot(leaderUserId, "PQC_SUBMIT",
                        activeOrder.getId(), now, simulationStage, simulationRunId))
                .simulated(simulationStage != null && !simulationStage.isBlank())
                .simulationStage(simulationStage)
                .simulationRunId(simulationRunId)
                .build());
        if (pqcInspectionTaskMapper.updateSubmittedEventId(task.getId(), eventId) != 1) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcTask.submittedEventId");
        }
        task.setTaskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED);
        task.setSubmittedEventId(eventId);
        task.setActualInspectionQuantity(actualInspectionQuantity);
        task.setSubmittedContentHash(contentHash);
        return eventId;
    }

    private List<MesPqcInspectionPieceDetailDO> buildSimulatedPieceDetails(MesPqcInspectionTaskDO task,
                                                                           Integer actualInspectionQuantity,
                                                                           String simulationStage,
                                                                           String simulationRunId) {
        List<MesQaInspectionRegulationItemDO> matchedItems = inspectionRegulationItemMapper
                .selectListByVersionId(task.getRegulationVersionId()).stream()
                .filter(item -> inspectionItemBelongsToTask(task, item))
                .sorted(Comparator.comparing(MesQaInspectionRegulationItemDO::getItemSort,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesQaInspectionRegulationItemDO::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (matchedItems.size() != 1) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "PQC任务无法唯一匹配正式检验项目，taskId=" + task.getId());
        }
        MesQaInspectionRegulationItemDO item = matchedItems.get(0);
        validateInspectionItemForSimulation(task, item);
        List<MesPqcInspectionPieceDetailDO> result = new ArrayList<>();
        for (int sampleNo = 1; sampleNo <= actualInspectionQuantity; sampleNo++) {
            String measuredValue = simulatedMeasuredValue(item);
            result.add(MesPqcInspectionPieceDetailDO.builder()
                    .taskId(task.getId())
                    .sampleNo(sampleNo)
                    .itemCode(item.getItemCode().trim())
                    .itemName(item.getItemName().trim())
                    .inspectionMethod(item.getInspectionMethod().trim())
                    .standardText(item.getStandardText().trim())
                    .standardLowerLimit(item.getStandardLowerLimit())
                    .standardUpperLimit(item.getStandardUpperLimit())
                    .standardUnit(item.getStandardUnit())
                    .standardPrecision(item.getStandardPrecision())
                    .resultType(item.getResultType().trim())
                    .itemResult(measuredValue)
                    .measuredValue(measuredValue)
                    .judgement(JUDGEMENT_PASS)
                    .simulated(simulationStage != null && !simulationStage.isBlank())
                    .simulationStage(simulationStage)
                    .simulationRunId(simulationRunId)
                    .build());
        }
        return result;
    }

    private boolean inspectionItemBelongsToTask(MesPqcInspectionTaskDO task,
                                                MesQaInspectionRegulationItemDO item) {
        return item != null
                && Objects.equals(task.getRegulationVersionId(), item.getRegulationVersionId())
                && Objects.equals(task.getQaProcessId(), item.getQaProcessId())
                && Objects.equals(normalizeInspectionType(task.getInspectionType()),
                normalizeInspectionType(item.getInspectionType()))
                && Objects.equals(normalizeQaItemCode(task.getQaItemCode()),
                normalizeQaItemCode(item.getItemCode()));
    }

    private void validateInspectionItemForSimulation(MesPqcInspectionTaskDO task,
                                                     MesQaInspectionRegulationItemDO item) {
        if (StrUtil.isBlank(item.getItemCode())
                || StrUtil.isBlank(item.getItemName())
                || StrUtil.isBlank(item.getInspectionMethod())
                || StrUtil.isBlank(item.getStandardText())
                || StrUtil.isBlank(item.getResultType())) {
            throw exception(PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED,
                    "PQC任务检验项目快照字段不完整，taskId=" + task.getId());
        }
    }

    private String buildPqcRawPayload(MesProcessPoolActiveOrderDO activeOrder, MesPqcInspectionTaskDO task,
                                      Integer actualInspectionQuantity, String simulationStage,
                                      String simulationRunId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("simulated", true);
        payload.put("activeOrderId", activeOrder.getId());
        payload.put("pqcTaskId", task.getId());
        payload.put("regulationVersionId", task.getRegulationVersionId());
        payload.put("workOrderId", task.getWorkOrderId());
        payload.put("routeId", task.getRouteId());
        payload.put("routeVersionId", task.getRouteVersionId());
        payload.put("routeProcessId", task.getRouteProcessId());
        payload.put("processId", task.getProcessId());
        payload.put("qaProcessId", task.getQaProcessId());
        payload.put("qaItemCode", task.getQaItemCode());
        payload.put("inspectionType", task.getInspectionType());
        payload.put("businessDate", task.getBusinessDate());
        payload.put("shiftCode", task.getShiftCode());
        payload.put("roundNo", task.getRoundNo());
        payload.put("actualInspectionQuantity", actualInspectionQuantity);
        payload.put("inspectionResult", MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS);
        payload.put("source", "active-order-simulate-completion");
        putSimulationMetadata(payload, simulationStage, simulationRunId);
        return JsonUtils.toJsonString(payload);
    }

    private Long insertApprovedReview(Long eventId, Long leaderUserId, String leaderType, String remark,
                                      String simulationStage, String simulationRunId) {
        MesProcessPoolSubmissionReviewDO existing = submissionReviewMapper.selectLatestByEventIdForUpdate(eventId);
        if (existing != null) {
            if (MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(existing.getReviewStatus())
                    && Objects.equals(leaderType, existing.getLeaderType())) {
                return existing.getId();
            }
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "submissionReview.status");
        }
        LocalDateTime now = LocalDateTime.now();
        MesProcessPoolSubmissionReviewDO review = MesProcessPoolSubmissionReviewDO.builder()
                .eventId(eventId)
                .leaderUserId(leaderUserId)
                .leaderType(leaderType)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark(withSimulationMetadata(remark, simulationStage, simulationRunId))
                .reviewedAt(now)
                .reviewSignatureId(nextSimulationSignatureId())
                .reviewSignatureUserId(leaderUserId)
                .reviewSignatureSnapshotJson(simulationSignatureSnapshot(leaderUserId, leaderType + "_REVIEW",
                        eventId, now, simulationStage, simulationRunId))
                .simulated(simulationStage != null && !simulationStage.isBlank())
                .simulationStage(simulationStage)
                .simulationRunId(simulationRunId)
                .build();
        submissionReviewMapper.insert(review);
        return review.getId();
    }

    private ProgressSnapshot calculateProgress(MesProcessPoolActiveOrderDO activeOrder,
                                               MesProRouteVersionDO routeVersion,
                                               List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        List<ProcessIdentity> snapshotIdentities = requireProgressProcessIdentities(activeOrder, snapshots);
        List<ProcessIdentity> formalIdentities = resolveFormalProgressProcessIdentities(activeOrder, routeVersion,
                snapshotIdentities);
        Set<ProcessIdentity> formalIdentitySet = new LinkedHashSet<>(formalIdentities);
        Map<ProcessIdentity, BigDecimal> targetQuantityByProcess = progressTargetQuantities(activeOrder, snapshots);
        Map<ProcessIdentity, BigDecimal> allocatedByProcess = aggregateAllocatedByProcess(activeOrder.getId());
        List<MesPqcInspectionTaskDO> pqcTasks = pqcInspectionTaskMapper.selectListByActiveOrderId(
                activeOrder.getId());
        long completedProcessCount = formalIdentities.stream()
                .filter(process -> isProductionProcessFullyAllocated(activeOrder, process, targetQuantityByProcess,
                        allocatedByProcess))
                .count();
        return new ProgressSnapshot(toProgressPercent(completedProcessCount, formalIdentities.size()),
                calculateInspectionProgressPercent(activeOrder, formalIdentitySet, pqcTasks));
    }

    private BigDecimal calculateInspectionProgressPercent(MesProcessPoolActiveOrderDO activeOrder,
                                                          Set<ProcessIdentity> formalIdentitySet,
                                                          List<MesPqcInspectionTaskDO> pqcTasks) {
        if (pqcTasks == null || pqcTasks.isEmpty()) {
            return zeroProgressPercent();
        }
        long confirmedTaskCount = 0;
        for (MesPqcInspectionTaskDO task : pqcTasks) {
            validatePqcTask(activeOrder, formalIdentitySet, task);
            if (MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus())) {
                confirmedTaskCount++;
            }
        }
        return toProgressPercent(confirmedTaskCount, pqcTasks.size());
    }

    private Map<ProcessIdentity, BigDecimal> aggregateAllocatedByProcess(Long activeOrderId) {
        return reportAllocationMapper.selectListByActiveOrderIds(List.of(activeOrderId)).stream()
                .filter(allocation -> allocation.getRouteProcessId() != null && allocation.getProcessId() != null)
                .collect(Collectors.groupingBy(allocation -> new ProcessIdentity(allocation.getRouteProcessId(),
                                allocation.getProcessId()),
                        LinkedHashMap::new, Collectors.reducing(BigDecimal.ZERO,
                                this::requireAllocationQuantity, BigDecimal::add)));
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
            targets.put(new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId()),
                    requirePlannedQuantity(activeOrder, snapshot));
        }
        return targets;
    }

    private static boolean isProductionProcessFullyAllocated(
            MesProcessPoolActiveOrderDO activeOrder,
            ProcessIdentity process,
            Map<ProcessIdentity, BigDecimal> targetQuantityByProcess,
            Map<ProcessIdentity, BigDecimal> allocatedQuantityByProcess) {
        BigDecimal targetQuantity = targetQuantityByProcess.get(process);
        if (targetQuantity == null) {
            BigDecimal erpQuantity = activeOrder.getErpFixedQuantitySnapshot();
            if (erpQuantity == null || erpQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
            }
            targetQuantity = erpQuantity;
        }
        return allocatedQuantityByProcess.getOrDefault(process, BigDecimal.ZERO).compareTo(targetQuantity) >= 0;
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

    private static List<ProcessIdentity> requireProgressProcessIdentities(
            MesProcessPoolActiveOrderDO activeOrder,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        if (activeOrder.getId() == null || snapshots == null || snapshots.isEmpty()) {
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

    private static BigDecimal requirePlannedQuantity(MesProcessPoolActiveOrderDO activeOrder,
                                                     MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        BigDecimal plannedQuantity = snapshot.getPlannedQuantitySnapshot();
        if (plannedQuantity == null || plannedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrder.getId());
        }
        return plannedQuantity;
    }

    private static BigDecimal toProgressPercent(long completedProcessCount, int totalProcessCount) {
        if (totalProcessCount <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, "activeOrderProgress");
        }
        return BigDecimal.valueOf(completedProcessCount)
                .multiply(PERCENT_DIVISOR)
                .divide(BigDecimal.valueOf(totalProcessCount), PROGRESS_PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroProgressPercent() {
        return BigDecimal.ZERO.setScale(PROGRESS_PERCENT_SCALE, RoundingMode.UNNECESSARY);
    }

    private String simulatedMeasuredValue(MesQaInspectionRegulationItemDO item) {
        BigDecimal lower = item.getStandardLowerLimit();
        BigDecimal upper = item.getStandardUpperLimit();
        Integer precision = item.getStandardPrecision() == null ? 2 : Math.max(0, item.getStandardPrecision());
        if (lower != null && upper != null) {
            return lower.add(upper).divide(BigDecimal.valueOf(2), precision, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString();
        }
        if (lower != null) {
            return lower.setScale(precision, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        }
        if (upper != null) {
            return upper.setScale(precision, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        }
        return "OK";
    }

    private Long nextSimulationSignatureId() {
        return System.currentTimeMillis() * 100_000 + simulationSequence.incrementAndGet() % 100_000;
    }

    private String simulationSignatureSnapshot(Long actorId, String actionType, Long objectId,
                                               LocalDateTime occurredAt, String simulationStage,
                                               String simulationRunId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("simulated", true);
        payload.put("actorId", actorId);
        payload.put("actionType", actionType);
        payload.put("objectId", objectId);
        payload.put("occurredAt", occurredAt);
        putSimulationMetadata(payload, simulationStage, simulationRunId);
        return JsonUtils.toJsonString(payload);
    }

    private void putSimulationMetadata(Map<String, Object> payload, String simulationStage,
                                       String simulationRunId) {
        if (simulationStage != null && !simulationStage.isBlank()) {
            payload.put("simulationStage", simulationStage);
        }
        if (simulationRunId != null && !simulationRunId.isBlank()) {
            payload.put("simulationRunId", simulationRunId);
        }
    }

    private String withSimulationMetadata(String remark, String simulationStage, String simulationRunId) {
        if (simulationStage == null || simulationStage.isBlank()
                || simulationRunId == null || simulationRunId.isBlank()) {
            return remark;
        }
        return remark + " [simulationStage=" + simulationStage + "][simulationRunId="
                + simulationRunId + "]";
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        return value;
    }

    private static Integer requirePositiveInteger(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        return value;
    }

    private static String normalizeInspectionType(String inspectionType) {
        if (inspectionType == null) {
            return null;
        }
        String trimmed = inspectionType.trim();
        return trimmed.startsWith(INSPECTION_TYPE_PATROL) ? INSPECTION_TYPE_PATROL : trimmed;
    }

    private static String normalizeQaItemCode(String qaItemCode) {
        if (qaItemCode == null) {
            return null;
        }
        String trimmed = qaItemCode.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record ProgressSnapshot(BigDecimal productionProgressPercent, BigDecimal inspectionProgressPercent) {
    }

    private record ProductionSimulationSummary(Integer productionSubmitCount, Integer productionReviewCount) {
    }

    private record PqcSimulationSummary(Integer pqcSubmitCount, Integer pqcReviewCount) {
    }
}
