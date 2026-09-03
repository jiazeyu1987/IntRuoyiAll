package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmProductIssueStatusEnum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

/** Materializes the three formal completion documents in Tx-A without a batch execution id. */
@Service
public class MesTeamLeaderActiveOrderCompletionBackfillPortImpl
        implements MesTeamLeaderActiveOrderCompletionBackfillPort {

    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesTeamLeaderActiveOrderReleaseLossSourceReader lossSourceReader;
    private final MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper;
    private final MesWmProductIssueMapper productIssueMapper;
    private final MesWmProductIssueDetailMapper productIssueDetailMapper;
    private final MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;
    private final MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper;

    public MesTeamLeaderActiveOrderCompletionBackfillPortImpl(
            MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolOrderProcessCompletionMapper completionMapper,
            MesPqcInspectionTaskMapper pqcTaskMapper,
            MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesTeamLeaderActiveOrderReleaseLossSourceReader lossSourceReader,
            MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper,
            MesWmProductIssueMapper productIssueMapper,
            MesWmProductIssueDetailMapper productIssueDetailMapper,
            MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper,
            MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper) {
        this.snapshotMapper = snapshotMapper;
        this.allocationMapper = allocationMapper;
        this.completionMapper = completionMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.aggregateDetailMapper = aggregateDetailMapper;
        this.workOrderMapper = workOrderMapper;
        this.lossSourceReader = lossSourceReader;
        this.backfillMapper = backfillMapper;
        this.productIssueMapper = productIssueMapper;
        this.productIssueDetailMapper = productIssueDetailMapper;
        this.pickListBindingMapper = pickListBindingMapper;
        this.pickListBindingItemMapper = pickListBindingItemMapper;
    }

    @Override
    public String readSourceSnapshotHash(Long leaderUserId, MesProcessPoolActiveOrderDO activeOrder,
                                         MesTeamLeaderActiveOrderCompletionCommand command) {
        return prepare(leaderUserId, activeOrder, command).getSourceSnapshotHash();
    }

    @Override
    public MesTeamLeaderActiveOrderCompletionBackfillDraft prepare(
            Long leaderUserId, MesProcessPoolActiveOrderDO activeOrder,
            MesTeamLeaderActiveOrderCompletionCommand command) {
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)) {
            throw sourceMissing(activeOrder, "ACTIVE_ORDER_OWNER");
        }
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = lockedSnapshots(activeOrder);
        List<MesProcessPoolReportAllocationDO> allocations = lockedAllocations(activeOrder);
        List<MesProcessPoolOrderProcessCompletionDO> completions = completionMapper
                .selectListByWorkOrderIdsForUpdate(List.of(activeOrder.getWorkOrderId()));
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        List<MesPqcProcessInspectionAggregateDetailDO> details = aggregateDetailMapper
                .selectListByActiveOrderIdForUpdate(activeOrder.getId());
        if (completions == null || completions.isEmpty() || tasks == null || tasks.isEmpty()
                || details == null || details.isEmpty()) {
            throw sourceMissing(activeOrder, "FORMAL_BACKFILL_SOURCE");
        }
        List<Long> batchSourceIds = new ArrayList<>(
                validateProductionSources(activeOrder, snapshots, allocations, completions));
        List<Long> inspectionSourceIds = validateInspectionSources(activeOrder, snapshots, tasks, details);

        MesProWorkOrderDO workOrder = workOrderMapper.selectByIdForUpdate(activeOrder.getWorkOrderId());
        if (workOrder == null || workOrder.getProductId() == null || StrUtil.isBlank(workOrder.getBatchCode())
                || activeOrder.getRouteId() == null || activeOrder.getRouteVersionId() == null) {
            throw sourceMissing(activeOrder, "WORK_ORDER_BATCH_ROUTE");
        }
        FormalProductIssue formalProductIssue = lockedFormalProductIssue(activeOrder);
        List<MesProcessPoolActiveOrderPickListBindingDO> pickListBindings = pickListBindingMapper
                .selectListByActiveOrderId(activeOrder.getId());
        if (pickListBindings == null || pickListBindings.isEmpty()) {
            throw sourceMissing(activeOrder, "FORMAL_PICK_LIST_BINDINGS_REQUIRED");
        }
        Map<Long, List<MesProcessPoolActiveOrderPickListBindingItemDO>> pickListItems = new LinkedHashMap<>();
        for (MesProcessPoolActiveOrderPickListBindingDO binding : pickListBindings) {
            List<MesProcessPoolActiveOrderPickListBindingItemDO> items =
                    pickListBindingItemMapper.selectListByBindingId(binding.getId());
            if (items == null || items.isEmpty()) {
                throw sourceMissing(activeOrder, "FORMAL_PICK_LIST_BINDING_ITEMS_REQUIRED:" + binding.getId());
            }
            pickListItems.put(binding.getId(), items);
            batchSourceIds.add(binding.getId());
            items.stream().map(MesProcessPoolActiveOrderPickListBindingItemDO::getId)
                    .filter(Objects::nonNull).forEach(batchSourceIds::add);
        }
        batchSourceIds.add(formalProductIssue.issue().getId());
        formalProductIssue.details().stream().map(MesWmProductIssueDetailDO::getId)
                .filter(Objects::nonNull).forEach(batchSourceIds::add);
        String sourceSeed = canonicalSourceSeed(activeOrder, workOrder, formalProductIssue, pickListBindings,
                pickListItems,
                snapshots, allocations, completions, tasks, details);
        String sourceSeedHash = sha256(sourceSeed);
        MesTeamLeaderActiveOrderReleaseLossReportPlanCommand lossCommand =
                new MesTeamLeaderActiveOrderReleaseLossReportPlanCommand()
                        .setTenantId(activeOrder.getTenantId())
                        .setActiveOrderId(activeOrder.getId())
                        .setWorkOrderId(activeOrder.getWorkOrderId())
                        .setRouteId(activeOrder.getRouteId())
                        .setRouteVersionId(activeOrder.getRouteVersionId())
                        .setProductId(workOrder.getProductId())
                        .setBatchCode(workOrder.getBatchCode())
                        .setSourceSnapshotHash(sourceSeedHash)
                        .setProcessSnapshots(snapshots);
        MesTeamLeaderActiveOrderReleaseLossSourceReadResult lossSources = lossSourceReader.read(lossCommand);
        if (lossSources == null || lossSources.getBlockers() == null || !lossSources.getBlockers().isEmpty()
                || lossSources.getProcessSources() == null
                || lossSources.getProcessSources().size() != snapshots.size()) {
            throw sourceMissing(activeOrder, "LOSS_CONDITION_FACTS");
        }
        List<MesTeamLeaderActiveOrderCompletionLossCondition> conditions = lossSources.getProcessSources().stream()
                .sorted(Comparator.comparing(source -> source.getSnapshot().getRouteProcessId()))
                .map(this::toLossCondition)
                .toList();
        String lossConditionFactsJson = JsonUtils.toJsonString(conditions);
        String signatureSnapshotJson = signatureSnapshot(completions, tasks, details, lossSources);
        boolean hasActualLoss = conditions.stream().anyMatch(item -> Boolean.TRUE.equals(item.getHasActualLoss()));
        BigDecimal lossQuantity = conditions.stream().map(MesTeamLeaderActiveOrderCompletionLossCondition::getLossQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long lossRecordId = conditions.stream().map(MesTeamLeaderActiveOrderCompletionLossCondition::getLossRecordId)
                .filter(Objects::nonNull).findFirst().orElse(null);
        String sourceSnapshotHash = sha256(sourceSeedHash + "|" + lossConditionFactsJson);
        String batchIdsJson = JsonUtils.toJsonString(batchSourceIds);
        String inspectionIdsJson = JsonUtils.toJsonString(inspectionSourceIds);
        return new MesTeamLeaderActiveOrderCompletionBackfillDraft()
                .setTenantId(activeOrder.getTenantId())
                .setWorkOrderId(activeOrder.getWorkOrderId())
                .setBatchCode(workOrder.getBatchCode())
                .setRouteId(activeOrder.getRouteId())
                .setRouteVersionId(activeOrder.getRouteVersionId())
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setFormalSourceSnapshotJson(sourceSeed)
                .setSignatureSnapshotJson(signatureSnapshotJson)
                .setBatchRecordSourceIdsJson(batchIdsJson)
                .setProcessInspectionSourceIdsJson(inspectionIdsJson)
                .setLossSourceHash(sha256(lossConditionFactsJson))
                .setBatchRecordStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setProcessInspectionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setLossReportStatus(hasActualLoss
                        ? MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_SUCCESS
                        : MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_NOT_REQUIRED)
                .setHasActualLoss(hasActualLoss)
                .setLossQuantity(lossQuantity)
                .setLossRecordId(lossRecordId)
                .setZeroLossConfirmationSnapshot(hasActualLoss ? null
                        : JsonUtils.toJsonString(Map.of("activeOrderId", activeOrder.getId(),
                        "sourceSnapshotHash", sourceSnapshotHash, "status", "NO_LOSS")))
                .setLossConditionFactsJson(lossConditionFactsJson);
    }

    @Override
    public void write(MesTeamLeaderActiveOrderCompletionBackfillDraft draft, Long activeOrderId) {
        if (draft == null || activeOrderId == null || draft.getMaterializedBy() == null) {
            throw sourceMissing(null, "BACKFILL_WRITE_CONTEXT");
        }
        validateWriteDraft(activeOrderId, draft);
        Long workOrderId = draft.getWorkOrderId();
        draft.setBatchRecordId(insert(activeOrderId, workOrderId, MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD,
                draft.getBatchRecordSourceIdsJson(), draft.getSourceSnapshotHash(),
                materializedPayload(MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD, draft),
                draft.getMaterializedBy(), draft.getTenantId()));
        draft.setProcessInspectionId(insert(activeOrderId, workOrderId, MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_PROCESS_INSPECTION,
                draft.getProcessInspectionSourceIdsJson(), draft.getSourceSnapshotHash(),
                materializedPayload(MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_PROCESS_INSPECTION, draft),
                draft.getMaterializedBy(), draft.getTenantId()));
        if (Boolean.TRUE.equals(draft.getHasActualLoss())) {
            Long sourceLossRecordId = draft.getLossRecordId();
            Long formalLossRecordId = insert(activeOrderId, workOrderId,
                    MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_LOSS_REPORT,
                    JsonUtils.toJsonString(List.of(sourceLossRecordId)), draft.getLossSourceHash(),
                    draft.getLossConditionFactsJson(), draft.getMaterializedBy(), draft.getTenantId());
            draft.setLossRecordId(formalLossRecordId);
        }
        List<MesProcessPoolOrderProcessCompletionDO> completions = completionMapper
                .selectListByWorkOrderIdsForUpdate(List.of(workOrderId));
        if (completions == null || completions.isEmpty()
                || completions.stream().anyMatch(item -> item == null || item.getId() == null
                || !MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED.equals(item.getCompletionStatus()))) {
            throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_COMPLETION_REQUIRED");
        }
        for (MesProcessPoolOrderProcessCompletionDO completion : completions) {
            completion.setBackfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                    .setBackfillExecutionId(draft.getBatchRecordId())
                    .setBackfillError(null);
            if (completionMapper.updateById(completion) != 1) {
                throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_COMPLETION_UPDATE_FAILED");
            }
        }
    }

    private static String materializedPayload(String type,
                                              MesTeamLeaderActiveOrderCompletionBackfillDraft draft) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("status", MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS);
        payload.put("sourceSnapshotHash", draft.getSourceSnapshotHash());
        payload.put("formalSourceSnapshot", draft.getFormalSourceSnapshotJson());
        payload.put("signatureSnapshot", draft.getSignatureSnapshotJson());
        payload.put("sourceIds", MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD.equals(type)
                ? draft.getBatchRecordSourceIdsJson() : draft.getProcessInspectionSourceIdsJson());
        return JsonUtils.toJsonString(payload);
    }

    private static void validateWriteDraft(Long activeOrderId,
                                           MesTeamLeaderActiveOrderCompletionBackfillDraft draft) {
        if (!MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS
                .equals(draft.getBatchRecordStatus())
                || !MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS
                .equals(draft.getProcessInspectionStatus())
                || StrUtil.isBlank(draft.getSourceSnapshotHash())
                || StrUtil.isBlank(draft.getBatchRecordSourceIdsJson())
                || StrUtil.isBlank(draft.getProcessInspectionSourceIdsJson())
                || StrUtil.isBlank(draft.getFormalSourceSnapshotJson())
                || StrUtil.isBlank(draft.getSignatureSnapshotJson())
                || StrUtil.isBlank(draft.getLossConditionFactsJson())
                || draft.getWorkOrderId() == null || StrUtil.isBlank(draft.getBatchCode())
                || draft.getRouteId() == null || draft.getRouteVersionId() == null
                || draft.getHasActualLoss() == null || draft.getLossQuantity() == null
                || draft.getLossQuantity().signum() < 0) {
            throw sourceMissing(null, "BACKFILL_WRITE_DRAFT_INCOMPLETE");
        }
        final List<MesTeamLeaderActiveOrderCompletionLossCondition> conditions;
        try {
            conditions = JsonUtils.parseArray(draft.getLossConditionFactsJson(),
                    MesTeamLeaderActiveOrderCompletionLossCondition.class);
        } catch (RuntimeException parseException) {
            throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_LOSS_CONDITIONS_INVALID");
        }
        if (conditions == null || conditions.isEmpty()) {
            throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_LOSS_CONDITIONS_REQUIRED");
        }
        boolean hasActualLoss = false;
        BigDecimal totalLoss = BigDecimal.ZERO;
        for (MesTeamLeaderActiveOrderCompletionLossCondition condition : conditions) {
            if (condition == null || condition.getProcessId() == null || StrUtil.isBlank(condition.getStatus())
                    || condition.getHasActualLoss() == null || condition.getLossQuantity() == null
                    || condition.getLossQuantity().signum() < 0 || StrUtil.isBlank(condition.getSourceHash())) {
                throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_LOSS_CONDITION_INCOMPLETE");
            }
            if (MesTeamLeaderActiveOrderCompletionLossCondition.BLOCKED.equals(condition.getStatus())) {
                throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_LOSS_CONDITION_BLOCKED");
            }
            if (MesTeamLeaderActiveOrderCompletionLossCondition.REQUIRED.equals(condition.getStatus())) {
                if (!Boolean.TRUE.equals(condition.getHasActualLoss())
                        || condition.getLossQuantity().signum() <= 0 || condition.getLossRecordId() == null) {
                    throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_LOSS_CONDITION_REQUIRED_INVALID");
                }
                hasActualLoss = true;
                totalLoss = totalLoss.add(condition.getLossQuantity());
            } else if (MesTeamLeaderActiveOrderCompletionLossCondition.NO_LOSS.equals(condition.getStatus())) {
                if (Boolean.TRUE.equals(condition.getHasActualLoss())
                        || condition.getLossQuantity().signum() != 0 || condition.getLossRecordId() != null
                        || StrUtil.isBlank(condition.getZeroLossConfirmationSnapshot())) {
                    throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_LOSS_CONDITION_NO_LOSS_INVALID");
                }
            } else {
                throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_LOSS_CONDITION_STATUS_INVALID");
            }
        }
        if (!Objects.equals(draft.getHasActualLoss(), hasActualLoss)
                || draft.getLossQuantity().compareTo(totalLoss) != 0) {
            throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_LOSS_SUMMARY_MISMATCH");
        }
        if (Boolean.TRUE.equals(draft.getHasActualLoss())) {
            if (!MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_SUCCESS
                    .equals(draft.getLossReportStatus())
                    || draft.getLossQuantity().signum() <= 0
                    || draft.getLossRecordId() == null || StrUtil.isBlank(draft.getLossSourceHash())) {
                throw sourceMissing(null, "BACKFILL_WRITE_LOSS_RECORD_REQUIRED");
            }
            return;
        }
        if (!MesProcessPoolActiveOrderCompletionReceiptDO.LOSS_REPORT_STATUS_NOT_REQUIRED
                .equals(draft.getLossReportStatus())
                || draft.getLossQuantity().signum() != 0 || draft.getLossRecordId() != null
                || StrUtil.isBlank(draft.getZeroLossConfirmationSnapshot())) {
            throw sourceMissing(null, "BACKFILL_WRITE_ZERO_LOSS_CONFIRMATION_REQUIRED");
        }
    }

    private Long insert(Long activeOrderId, Long workOrderId, String type, String sourceIds,
                        String sourceHash, String payload, Long materializedBy, Long tenantId) {
        MesProcessPoolActiveOrderCompletionBackfillDO row = MesProcessPoolActiveOrderCompletionBackfillDO.builder()
                .activeOrderId(activeOrderId).workOrderId(workOrderId).backfillType(type).status("SUCCESS")
                .sourceIdsJson(sourceIds).sourceSnapshotHash(sourceHash).payloadJson(payload)
                .materializedAt(java.time.LocalDateTime.now()).materializedBy(materializedBy).build();
        row.setTenantId(tenantId);
        if (backfillMapper.insert(row) <= 0) {
            throw sourceMissing(null, "BACKFILL_WRITE_" + type);
        }
        if (row.getId() == null) {
            throw sourceMissingById(activeOrderId, "BACKFILL_WRITE_ID_" + type);
        }
        return row.getId();
    }

    private List<MesProcessPoolActiveOrderProcessSnapshotDO> lockedSnapshots(MesProcessPoolActiveOrderDO order) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> result = snapshotMapper
                .selectListByActiveOrderIdForUpdate(order.getId());
        if (result == null || result.isEmpty()) {
            throw sourceMissing(order, "PROCESS_SNAPSHOT");
        }
        return result;
    }

    private List<MesProcessPoolReportAllocationDO> lockedAllocations(MesProcessPoolActiveOrderDO order) {
        List<MesProcessPoolReportAllocationDO> result = allocationMapper.selectListByActiveOrderIdForUpdate(order.getId());
        if (result == null || result.isEmpty()) {
            throw sourceMissing(order, "REPORT_ALLOCATION");
        }
        return result;
    }

    private List<Long> validateProductionSources(MesProcessPoolActiveOrderDO order,
                                                   List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
                                                   List<MesProcessPoolReportAllocationDO> allocations,
                                                   List<MesProcessPoolOrderProcessCompletionDO> completions) {
        List<Long> ids = new ArrayList<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            List<MesProcessPoolOrderProcessCompletionDO> completionMatches = completions.stream()
                    .filter(item -> item != null && Objects.equals(snapshot.getRouteProcessId(), item.getRouteProcessId())
                            && Objects.equals(snapshot.getProcessId(), item.getProcessId())
                            && Objects.equals(order.getWorkOrderId(), item.getWorkOrderId()))
                    .toList();
            if (completionMatches.size() != 1) {
                throw sourceMissing(order, "PRODUCTION_COMPLETION");
            }
            MesProcessPoolOrderProcessCompletionDO completion = completionMatches.get(0);
            if (completion.getId() == null
                    || !MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED.equals(completion.getCompletionStatus())
                    || completion.getTargetQuantity() == null || completion.getConfirmedQuantity() == null
                    || completion.getConfirmedQuantity().compareTo(completion.getTargetQuantity()) < 0
                    || completion.getLastEventId() == null || completion.getLastReviewId() == null
                    || StrUtil.isBlank(completion.getSourceEventIdsJson())
                    || StrUtil.isBlank(completion.getSourceAllocationIdsJson())
                    || StrUtil.isBlank(completion.getAggregateHash())) {
                throw sourceMissing(order, "PRODUCTION_COMPLETION");
            }
            List<Long> declaredAllocationIds;
            try {
                declaredAllocationIds = JsonUtils.parseArray(completion.getSourceAllocationIdsJson(), Long.class);
            } catch (RuntimeException ex) {
                throw sourceMissing(order, "PRODUCTION_COMPLETION_SOURCE_ALLOCATIONS");
            }
            Set<Long> expectedAllocationIds = allocations.stream()
                    .filter(allocation -> allocation != null
                            && Objects.equals(snapshot.getRouteProcessId(), allocation.getRouteProcessId())
                            && Objects.equals(snapshot.getProcessId(), allocation.getProcessId())
                            && Objects.equals(order.getWorkOrderId(), allocation.getWorkOrderId())
                            && allocation.getId() != null)
                    .map(MesProcessPoolReportAllocationDO::getId)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            if (declaredAllocationIds == null
                    || !expectedAllocationIds.equals(new java.util.LinkedHashSet<>(declaredAllocationIds))) {
                throw sourceMissing(order, "PRODUCTION_COMPLETION_SOURCE_ALLOCATIONS");
            }
            ids.add(completion.getId());
        }
        allocations.stream().map(MesProcessPoolReportAllocationDO::getId).filter(Objects::nonNull).forEach(ids::add);
        return ids.stream().distinct().sorted().toList();
    }

    private List<Long> validateInspectionSources(MesProcessPoolActiveOrderDO order,
                                                   List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
                                                   List<MesPqcInspectionTaskDO> tasks,
                                                   List<MesPqcProcessInspectionAggregateDetailDO> details) {
        List<Long> ids = new ArrayList<>();
        for (MesPqcInspectionTaskDO task : tasks) {
            if (task == null || task.getId() == null
                    || !MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus())
                    || task.getActualInspectionQuantity() == null || task.getActualInspectionQuantity() <= 0
                    || task.getSubmittedEventId() == null) {
                throw sourceMissing(order, "PROCESS_INSPECTION");
            }
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshotMatches = snapshots.stream().filter(item -> item != null
                    && Objects.equals(item.getRouteProcessId(), task.getRouteProcessId())
                    && Objects.equals(item.getProcessId(), task.getProcessId()))
                    .toList();
            if (snapshotMatches.size() != 1) {
                throw sourceMissing(order, "PROCESS_INSPECTION");
            }
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot = snapshotMatches.get(0);
            List<MesPqcProcessInspectionAggregateDetailDO> detailMatches = details.stream()
                    .filter(item -> item != null && Objects.equals(task.getId(), item.getPqcTaskId())
                            && Objects.equals(order.getId(), item.getActiveOrderId())
                            && Objects.equals(order.getWorkOrderId(), item.getWorkOrderId())
                            && Objects.equals(order.getRouteId(), item.getRouteId())
                            && Objects.equals(order.getRouteVersionId(), item.getRouteVersionId())
                            && Objects.equals(task.getRouteProcessId(), item.getRouteProcessId())
                            && Objects.equals(task.getProcessId(), item.getProcessId()))
                    .toList();
            if (detailMatches.isEmpty()
                    || !Objects.equals(task.getActualInspectionQuantity(), detailMatches.size())) {
                throw sourceMissing(order, "PROCESS_INSPECTION");
            }
            Set<Long> detailIds = new java.util.LinkedHashSet<>();
            Set<Long> sourcePieceIds = new java.util.LinkedHashSet<>();
            for (MesPqcProcessInspectionAggregateDetailDO detail : detailMatches) {
                if (detail.getId() == null
                        || detail.getSourcePqcRecordId() == null
                        || detail.getSourcePieceDetailId() == null
                        || detail.getEventId() == null
                        || detail.getReviewId() == null
                        || !Objects.equals(task.getSubmittedEventId(), detail.getEventId())
                        || !Objects.equals(task.getActualInspectionQuantity(), detail.getActualInspectionQuantity())
                        || !detailIds.add(detail.getId())
                        || !sourcePieceIds.add(detail.getSourcePieceDetailId())) {
                    throw sourceMissing(order, "PROCESS_INSPECTION");
                }
            }
            ids.add(task.getId());
            ids.addAll(detailIds);
        }
        if (ids.isEmpty()) {
            throw sourceMissing(order, "PROCESS_INSPECTION");
        }
        return ids.stream().distinct().sorted().toList();
    }

    private MesTeamLeaderActiveOrderCompletionLossCondition toLossCondition(
            MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource source) {
        MesProFeedbackDO feedback = source.getFeedback();
        BigDecimal quantity = feedback.getUnqualifiedQuantity() == null ? BigDecimal.ZERO
                : feedback.getUnqualifiedQuantity();
        boolean actual = quantity.signum() > 0;
        Map<String, Object> lossSource = new LinkedHashMap<>();
        lossSource.put("event", source.getEvent());
        lossSource.put("feedback", source.getFeedback());
        lossSource.put("allocation", source.getAllocation());
        lossSource.put("review", source.getReview());
        lossSource.put("lossDetails", source.getLossDetails());
        String sourceHash = sha256(JsonUtils.toJsonString(lossSource));
        return new MesTeamLeaderActiveOrderCompletionLossCondition()
                .setProcessId(source.getSnapshot().getProcessId())
                .setStatus(actual ? MesTeamLeaderActiveOrderCompletionLossCondition.REQUIRED
                        : MesTeamLeaderActiveOrderCompletionLossCondition.NO_LOSS)
                .setHasActualLoss(actual).setLossQuantity(quantity)
                .setLossRecordId(actual ? feedback.getId() : null)
                .setZeroLossConfirmationSnapshot(actual ? null
                        : JsonUtils.toJsonString(Map.of("eventId", source.getEvent().getId(),
                        "feedbackId", feedback.getId(), "status", "NO_LOSS")))
                .setSourceHash(sourceHash);
    }

    private static String canonicalSourceSeed(MesProcessPoolActiveOrderDO order,
                                               MesProWorkOrderDO workOrder,
                                               FormalProductIssue formalProductIssue,
                                               List<MesProcessPoolActiveOrderPickListBindingDO> pickListBindings,
                                               Map<Long, List<MesProcessPoolActiveOrderPickListBindingItemDO>> pickListItems,
                                               List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
                                               List<MesProcessPoolReportAllocationDO> allocations,
                                               List<MesProcessPoolOrderProcessCompletionDO> completions,
                                               List<MesPqcInspectionTaskDO> tasks,
                                               List<MesPqcProcessInspectionAggregateDetailDO> details) {
        Map<String, Object> seed = new LinkedHashMap<>();
        Map<String, Object> orderBinding = new LinkedHashMap<>();
        orderBinding.put("id", order.getId());
        orderBinding.put("tenantId", order.getTenantId());
        orderBinding.put("workOrderId", order.getWorkOrderId());
        orderBinding.put("routeId", order.getRouteId());
        orderBinding.put("routeVersionId", order.getRouteVersionId());
        orderBinding.put("leaderUserId", order.getLeaderUserId());
        seed.put("activeOrderBinding", orderBinding);
        Map<String, Object> workOrderBinding = new LinkedHashMap<>();
        workOrderBinding.put("id", workOrder.getId());
        workOrderBinding.put("productId", workOrder.getProductId());
        workOrderBinding.put("batchCode", workOrder.getBatchCode());
        seed.put("workOrderBinding", workOrderBinding);
        seed.put("formalProductIssue", formalProductIssue.issue());
        seed.put("formalProductIssueDetails", formalProductIssue.details());
        seed.put("pickListBindings", pickListBindings);
        seed.put("pickListBindingItems", pickListItems);
        seed.put("snapshots", snapshots);
        seed.put("allocations", allocations);
        seed.put("completions", completions);
        seed.put("pqcTasks", tasks);
        seed.put("pqcDetails", details);
        return JsonUtils.toJsonString(seed);
    }

    private static String signatureSnapshot(
            List<MesProcessPoolOrderProcessCompletionDO> completions,
            List<MesPqcInspectionTaskDO> tasks,
            List<MesPqcProcessInspectionAggregateDetailDO> details,
            MesTeamLeaderActiveOrderReleaseLossSourceReadResult lossSources) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("productionCompletionSignatures", completions);
        snapshot.put("pqcTaskSignatures", tasks);
        snapshot.put("pqcDetailSignatures", details);
        snapshot.put("lossSignatures", lossSources.getProcessSources());
        return JsonUtils.toJsonString(snapshot);
    }

    private FormalProductIssue lockedFormalProductIssue(MesProcessPoolActiveOrderDO activeOrder) {
        List<MesWmProductIssueDO> issues = productIssueMapper
                .selectListByWorkOrderIdForUpdate(activeOrder.getWorkOrderId());
        List<MesWmProductIssueDO> finished = issues == null ? List.of() : issues.stream()
                .filter(Objects::nonNull)
                .filter(issue -> Objects.equals(activeOrder.getWorkOrderId(), issue.getWorkOrderId())
                        && Objects.equals(MesWmProductIssueStatusEnum.FINISHED.getStatus(), issue.getStatus()))
                .toList();
        if (finished.size() != 1 || finished.get(0).getId() == null) {
            throw sourceMissing(activeOrder, "FORMAL_PRODUCT_ISSUE_UNIQUE_FINISHED");
        }
        MesWmProductIssueDO issue = finished.get(0);
        List<MesWmProductIssueDetailDO> details = productIssueDetailMapper
                .selectListByIssueIdForUpdate(issue.getId());
        if (details == null || details.isEmpty() || details.stream().anyMatch(detail ->
                detail == null || detail.getId() == null || !Objects.equals(issue.getId(), detail.getIssueId())
                        || detail.getLineId() == null || detail.getMaterialStockId() == null
                        || detail.getItemId() == null || detail.getQuantity() == null
                        || detail.getQuantity().signum() <= 0 || detail.getBatchId() == null
                        || StrUtil.isBlank(detail.getBatchCode()))) {
            throw sourceMissing(activeOrder, "FORMAL_PRODUCT_ISSUE_DETAIL");
        }
        return new FormalProductIssue(issue, details.stream()
                .sorted(Comparator.comparing(MesWmProductIssueDetailDO::getId))
                .toList());
    }

    private record FormalProductIssue(MesWmProductIssueDO issue, List<MesWmProductIssueDetailDO> details) {
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private static RuntimeException sourceMissing(MesProcessPoolActiveOrderDO order, String field) {
        return exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                order == null ? null : order.getId(), field);
    }

    private static RuntimeException sourceMissingById(Long activeOrderId, String field) {
        return exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, activeOrderId, field);
    }
}
