package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskPreviewRespVO.CompletionSourceMaterialization;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionSnapshotResolver;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteSnapshotCanonicalizer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID;

/** Materializes frozen production quantity facts in Tx-B and preserves traditional task-opening contracts. */
@Service
public class MesProEdhrCompletionSourceBackfillService {

    public static final String AUDIT_REASON_PREFIX = "正式完工来源回填|";
    private final MesProEdhrBatchExecutionOriginMapper originMapper;
    private final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort receiptPort;
    private final MesProRouteVersionSnapshotResolver routeResolver;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProRouteSnapshotCanonicalizer routeCanonicalizer;
    private final MesProEdhrOperationAuditEventMapper operationAuditMapper;
    private final MesProBatchRecordCellLinkRuleMapper ruleMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProBatchRecordExecutionMapper executionMapper;
    private final MesProBatchRecordExecutionFieldAuditBatchMapper auditBatchMapper;
    private final MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper;
    private final MesProBatchRecordExecutionFieldAuditService auditService;
    private final MesTeamLeaderBatchRecordBackfillService backfillService;
    private final MesProEdhrBatchExecutionTaskMapper taskMapper;

    public MesProEdhrCompletionSourceBackfillService(MesProEdhrBatchExecutionOriginMapper originMapper,
            MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort receiptPort,
            MesProRouteVersionSnapshotResolver routeResolver, MesProBatchRecordCellLinkRuleMapper ruleMapper,
            MesProWorkOrderMapper workOrderMapper, MesProBatchRecordExecutionMapper executionMapper,
            MesProBatchRecordExecutionFieldAuditBatchMapper auditBatchMapper,
            MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper,
            MesProBatchRecordExecutionFieldAuditService auditService,
            MesTeamLeaderBatchRecordBackfillService backfillService,
            MesProEdhrBatchExecutionTaskMapper taskMapper,
            MesProRouteVersionMapper routeVersionMapper,
            MesProRouteSnapshotCanonicalizer routeCanonicalizer,
            MesProEdhrOperationAuditEventMapper operationAuditMapper) {
        this.originMapper = originMapper;
        this.receiptPort = receiptPort;
        this.routeResolver = routeResolver;
        this.ruleMapper = ruleMapper;
        this.workOrderMapper = workOrderMapper;
        this.executionMapper = executionMapper;
        this.auditBatchMapper = auditBatchMapper;
        this.auditItemMapper = auditItemMapper;
        this.auditService = auditService;
        this.backfillService = backfillService;
        this.taskMapper = taskMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.routeCanonicalizer = routeCanonicalizer;
        this.operationAuditMapper = operationAuditMapper;
    }

    /** Called inside openTask's transaction, after its authorization and ordinary version governance. */
    public void materialize(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task) {
        Context context = prepare(batch, task);
        if (context == null) return;
        requireMapping(context, task);
        MesProBatchRecordExecutionDO execution = requireExecution(batch, task);
        if (readApplied(context, task) != null) return;
        if (!Integer.valueOf(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_DRAFT).equals(execution.getStatus())) {
            throw sourceRequired("已提交或批准的批记录禁止自动回填，taskId=" + task.getId());
        }
        requireReadableDraftCells(execution);
        write(context, batch, task);
    }

    /** A generated product-information member shares a binding but is not its frozen report. */
    public static List<MesProEdhrBatchExecutionTaskDO> selectProvisionedQuantityTasks(
            MesProEdhrBatchExecutionDO batch, List<MesProEdhrBatchExecutionTaskDO> tasks) {
        List<MesProEdhrBatchExecutionTaskDO> selected = new ArrayList<>();
        Set<DirectReportReference> references = new HashSet<>();
        Set<Long> taskIds = new HashSet<>();
        for (DirectReportReference reference : directQuantityReportReferences(batch)) {
            if (!references.add(reference)) throw sourceRequired("冻结正式主批记录报告重复");
            List<MesProEdhrBatchExecutionTaskDO> matches = tasks.stream()
                    .filter(task -> reference.matches(task)).toList();
            if (matches.size() != 1) {
                throw sourceRequired("冻结正式主批记录必须对应唯一生成任务，routeBindingId="
                        + reference.routeBindingId() + "，reportId=" + reference.reportId());
            }
            MesProEdhrBatchExecutionTaskDO task = matches.get(0);
            if (task.getId() == null || task.getId() <= 0 || !taskIds.add(task.getId())) {
                throw sourceRequired("冻结正式主批记录生成任务身份无效或重复");
            }
            selected.add(task);
        }
        return selected;
    }

    /** Validate the entire locked fanout before the first execution or audit write. */
    public void validateProvisionedQuantityTasks(MesProEdhrBatchExecutionDO batch,
            List<MesProEdhrBatchExecutionTaskDO> tasks, MesBatchExecutionProvisionCommand command) {
        for (MesProEdhrBatchExecutionTaskDO task : selectProvisionedQuantityTasks(batch, tasks)) {
            if (!Objects.equals(task.getMaterialSourceSnapshotHash(), command.getSourceSnapshotHash())) {
                throw sourceRequired("数量来源任务冻结哈希与完工命令不一致，taskId=" + task.getId());
            }
            frozenQuantityBinding(batch, task);
        }
    }

    private static List<DirectReportReference> directQuantityReportReferences(MesProEdhrBatchExecutionDO batch) {
        JsonNode configs = JsonUtils.parseTree(batch.getRouteSnapshotJson()).path("configSnapshots").path("batchUseConfigs");
        if (!configs.isArray()) throw sourceRequired("冻结路线缺少逐工序批记录绑定");
        List<DirectReportReference> references = new ArrayList<>();
        for (JsonNode config : configs) {
            if (!"BATCH".equals(config.path("useType").asText())) continue;
            JsonNode reports = config.path("batchRecordReports");
            if (reports.isMissingNode() || reports.isNull()) continue;
            if (!reports.isArray()) throw sourceRequired("冻结路线正式批记录报告集合类型无效");
            for (JsonNode report : reports) {
                if ("MAIN".equals(report.path("formSlotType").asText())
                        && "PROCESS".equals(report.path("instanceScope").asText())
                        && "BATCH_RECORD".equals(report.path("recordCategory").asText())) {
                    references.add(new DirectReportReference(nullableLong(config.get("routeProcessId")),
                            nullableLong(report.get("routeBindingId")), report.path("batchRecordReportId").asText()));
                }
            }
        }
        return references;
    }

    private record DirectReportReference(Long routeProcessId, Long routeBindingId, String reportId) {
        private boolean matches(MesProEdhrBatchExecutionTaskDO task) {
            return task != null && Objects.equals(routeProcessId, task.getRouteProcessId())
                    && Objects.equals(routeBindingId, task.getRouteBindingId())
                    && Objects.equals(reportId, task.getBatchRecordReportId());
        }
    }

    /** Tx-B's canonical receipt is available before Tx-C creates the trace origin. */
    public ProvisionedResult materializeProvisioned(MesProEdhrBatchExecutionDO batch,
            MesProEdhrBatchExecutionTaskDO task, MesBatchExecutionProvisionCommand command) {
        if (!Objects.equals(batch.getId(), task.getBatchExecutionId())
                || !"MAIN".equals(task.getFormSlotType()) || !"PROCESS".equals(task.getInstanceScope())
                || !"BATCH_RECORD".equals(task.getRecordCategory())) {
            throw sourceRequired("完工来源物化仅允许精确正式逐工序主批记录任务");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (command == null || !("ACTIVE_ORDER_COMPLETION".equals(command.getEntryType())
                || "ACTIVE_ORDER_PQC".equals(command.getEntryType()))
                || !Objects.equals(tenantId, command.getTenantId())
                || command.getCompletionBackfillReceiptId() == null) {
            throw sourceRequired("完工来源物化缺少当前租户权威回执");
        }
        MesFlow6CompletionBackfillReceipt receipt = receiptPort.getByReceiptId(
                Long.valueOf(command.getCompletionBackfillReceiptId()), tenantId);
        requireReceipt(batch, receipt);
        if (!Objects.equals(command.getSourceCredentialId(), String.valueOf(receipt.getReceiptId()))
                || !Objects.equals(command.getActiveOrderId(), receipt.getActiveOrderId())
                || !Objects.equals(command.getWorkOrderId(), receipt.getWorkOrderId())
                || !Objects.equals(command.getRouteId(), receipt.getRouteId())
                || !Objects.equals(command.getRouteVersionId(), receipt.getRouteVersionId())
                || !Objects.equals(command.getBatchCode(), receipt.getBatchCode())
                || !Objects.equals(command.getCompletionBackfillReceiptHash(), receipt.getReceiptHash())
                || !Objects.equals(command.getSourceSnapshotHash(), receipt.getSourceSnapshotHash())
                || !Objects.equals(task.getMaterialSourceSnapshotHash(), receipt.getSourceSnapshotHash())) {
            throw sourceRequired("完工物化权威回执、任务冻结来源不一致");
        }
        Context context = prepareQuantityContext(batch, task, receipt);
        if (context.rules().isEmpty()) {
            return new ProvisionedResult(task.getId(), "SKIPPED_NO_SOURCE_RULES", null, 0);
        }
        requireMapping(context, task);
        for (var rule : context.rules()) {
            if (!"PROCESS_POOL_REPORT".equals(rule.getSourceType())
                    || !Set.of("outputQuantity", "lossQuantity").contains(rule.getSourceFieldCode())
                    || !"SUM".equals(rule.getAggregationStrategy()) || !"NUMBER".equals(rule.getTargetValueType())
                    || !"ONLY_WHEN_EMPTY".equals(rule.getOverwritePolicy())) {
                throw sourceRequired("自动完工物化仅支持已配置的生产数量SUM空格写入，ruleId=" + rule.getId());
            }
        }
        if (task.getExecutionId() != null) {
            MesProBatchRecordExecutionDO execution = requireExecution(batch, task);
            ReadResult applied = readApplied(context, task);
            if (applied != null) return new ProvisionedResult(task.getId(), "APPLIED", task.getExecutionId(),
                    applied.evidence().getAppliedFieldCount());
            if (!Integer.valueOf(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_DRAFT).equals(execution.getStatus())) {
                throw sourceRequired("已提交或批准的批记录禁止自动回填，taskId=" + task.getId());
            }
            requireReadableDraftCells(execution);
        }
        write(context, batch, task);
        return new ProvisionedResult(task.getId(), "APPLIED", task.getExecutionId(), context.rules().size());
    }

    private void write(Context context, MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task) {
        JsonNode frozen = JsonUtils.parseTree(context.receipt().getFormalSourceSnapshotJson());
        List<MesProcessPoolReportAllocationDO> allocations = rows(frozen.path("allocations"),
                MesProcessPoolReportAllocationDO.class).stream()
                .filter(row -> Objects.equals(row.getWorkOrderId(), batch.getWorkOrderId())
                        && Objects.equals(row.getRouteProcessId(), task.getRouteProcessId())
                        && Objects.equals(row.getProcessId(), task.getProcessId()))
                .sorted(Comparator.comparing(MesProcessPoolReportAllocationDO::getConfirmedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(MesProcessPoolReportAllocationDO::getId)).toList();
        List<MesProcessPoolOrderProcessCompletionDO> completions = rows(frozen.path("completions"),
                MesProcessPoolOrderProcessCompletionDO.class).stream()
                .filter(row -> Objects.equals(row.getWorkOrderId(), batch.getWorkOrderId())
                        && Objects.equals(row.getRouteProcessId(), task.getRouteProcessId())
                        && Objects.equals(row.getProcessId(), task.getProcessId())).toList();
        if (allocations.isEmpty() || completions.size() != 1) {
            throw sourceRequired("冻结来源缺少唯一工序完工及分配，taskId=" + task.getId());
        }
        MesProcessPoolOrderProcessCompletionDO completion = completions.get(0);
        if (!"COMPLETED".equals(completion.getCompletionStatus()) || completion.getLastEventId() == null) {
            throw sourceRequired("冻结工序未正式完工，taskId=" + task.getId());
        }
        Set<Long> eventIds = new LinkedHashSet<>();
        Set<Long> reviewIds = new LinkedHashSet<>();
        for (MesProcessPoolReportAllocationDO allocation : allocations) {
            if (allocation.getId() == null || allocation.getEventId() == null || allocation.getReviewId() == null
                    || !Objects.equals(allocation.getTenantId(), batch.getTenantId())
                    || !Objects.equals(allocation.getActiveOrderId(), context.receipt().getActiveOrderId())
                    || !MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT.equals(allocation.getLifecycleStatus())) {
                throw sourceRequired("冻结生产分配身份不完整，taskId=" + task.getId());
            }
            eventIds.add(allocation.getEventId());
            reviewIds.add(allocation.getReviewId());
        }
        List<MesProProcessPoolEventDO> events = rows(frozen.path("productionFacts").path("events"),
                MesProProcessPoolEventDO.class).stream().filter(row -> eventIds.contains(row.getId()))
                .sorted(Comparator.comparing(MesProProcessPoolEventDO::getServerSubmitTime,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(MesProProcessPoolEventDO::getId)).toList();
        List<MesProcessPoolSubmissionReviewDO> reviews = rows(frozen.path("productionFacts").path("reviews"),
                MesProcessPoolSubmissionReviewDO.class).stream().filter(row -> reviewIds.contains(row.getId())).toList();
        if (events.size() != eventIds.size() || reviews.size() != reviewIds.size()
                || events.stream().anyMatch(row -> !"PRODUCTION_SUBMIT".equals(row.getEventType())
                || !Objects.equals(batch.getTenantId(), row.getTenantId()) || row.getActualEmployeeId() == null
                || row.getSignatureId() == null || row.getSignatureUserId() == null || row.getRawPayload() == null)
                || reviews.stream().anyMatch(row -> !"PRODUCTION".equals(row.getLeaderType())
                || !"APPROVED".equals(row.getReviewStatus()) || !Objects.equals(batch.getTenantId(), row.getTenantId())
                || row.getReviewSignatureId() == null || row.getReviewSignatureUserId() == null)) {
            throw sourceRequired("冻结生产事件或生产复核身份不完整，PQC不能替代生产复核，taskId=" + task.getId());
        }
        for (MesProcessPoolReportAllocationDO allocation : allocations) {
            if (reviews.stream().noneMatch(review -> Objects.equals(review.getId(), allocation.getReviewId())
                    && Objects.equals(review.getEventId(), allocation.getEventId()))) {
                throw sourceRequired("冻结复核与生产分配不匹配，allocationId=" + allocation.getId());
            }
        }
        MesProProcessPoolEventDO event = events.stream()
                .filter(row -> Objects.equals(completion.getLastEventId(), row.getId())).findFirst()
                .orElseThrow(() -> sourceRequired("冻结末次生产事件缺失，taskId=" + task.getId()));
        List<MesProcessPoolReportAllocationDO> lastAllocations = allocations.stream()
                .filter(row -> Objects.equals(event.getId(), row.getEventId())).toList();
        if (lastAllocations.size() != 1) throw sourceRequired("冻结末次分配不唯一，taskId=" + task.getId());
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(batch.getWorkOrderId());
        if (workOrder == null || !Objects.equals(workOrder.getProductId(), batch.getProductId())) {
            throw sourceRequired("正式生产工单身份不匹配，taskId=" + task.getId());
        }
        workOrder.setBatchCode(batch.getBatchCode());
        MesTeamLeaderBatchRecordBackfillResult result = backfillService.backfillCompletedProcess(
                new MesTeamLeaderBatchRecordBackfillCommand().setEvent(event).setAllocation(lastAllocations.get(0))
                        .setSourceEvents(events).setAllocations(allocations).setReviews(reviews)
                        .setAggregateHash(context.receipt().getSourceSnapshotHash()).setIdempotencyKey(context.key())
                        .setWorkOrder(workOrder).setRouteBinding(context.binding()).setMappingRules(context.rules())
                        .setBatchExecutionId(batch.getId()).setBatchExecutionTaskId(task.getId())
                        .setSourceAuditReasonText(context.reason()).setRequireEmptyTargets(true));
        if (result == null || result.getExecutionId() == null || result.getAuditBatchId() == null) {
            throw sourceRequired("回填未形成正式执行实例与来源审计，taskId=" + task.getId());
        }
        if (task.getExecutionId() == null) {
            task.setExecutionId(result.getExecutionId());
            requireExecution(batch, task);
            if (taskMapper.updateById(new MesProEdhrBatchExecutionTaskDO().setId(task.getId())
                    .setExecutionId(result.getExecutionId())) != 1) {
                throw sourceRequired("来源执行实例绑定保存失败，taskId=" + task.getId());
            }
        }
        if (!Objects.equals(result.getExecutionId(), task.getExecutionId()) || readApplied(context, task) == null) {
            throw sourceRequired("回填未形成当前执行实例的来源字段审计，taskId=" + task.getId());
        }
    }

    /** Read-only evidence. Its cells contain only this source audit, never unrelated manual draft cells. */
    public ReadResult preview(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task) {
        Context context = preparePreview(batch, task);
        if (context == null) return null;
        if (context.problem() != null) return new ReadResult(evidence(context, "CONFIGURATION_MISSING")
                .setMessage(context.problem()), "[]");
        MesProBatchRecordExecutionDO execution = task.getExecutionId() == null ? null : requireExecution(batch, task);
        ReadResult applied = readApplied(context, task);
        if (applied != null) applied.evidence().setExecutionStatus(execution.getStatus());
        return applied == null ? new ReadResult(evidence(context, "PENDING")
                .setMessage("正式生产来源尚未回填"), "[]") : applied;
    }

    private Context prepare(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task) {
        MesFlow6CompletionBackfillReceipt receipt = originReceipt(batch, task);
        if (receipt == null) return null;
        if (hasSourceAudit(task, AUDIT_REASON_PREFIX + "scope=QUANTITY|")) {
            return prepareAppliedQuantityContext(batch, task, receipt);
        }
        return prepareContext(batch, task, receipt, frozenBinding(batch, task));
    }

    private Context preparePreview(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task) {
        MesFlow6CompletionBackfillReceipt receipt = originReceipt(batch, task);
        if (receipt == null) return null;
        if (!"MAIN".equals(task.getFormSlotType())) {
            return prepareContext(batch, task, receipt, frozenBinding(batch, task));
        }
        if (hasSourceAudit(task, AUDIT_REASON_PREFIX + "scope=QUANTITY|")) {
            return prepareAppliedQuantityContext(batch, task, receipt);
        }
        if (hasSourceAudit(task, AUDIT_REASON_PREFIX)) {
            return prepareContext(batch, task, receipt, frozenBinding(batch, task));
        }
        boolean directQuantityReport = directQuantityReportReferences(batch).stream().anyMatch(ref -> ref.matches(task));
        if (directQuantityReport && hasExplicitQuantitySkip(batch, task, receipt)) {
            Context quantities = prepareQuantityContext(batch, task, receipt);
            if (!quantities.rules().isEmpty()) throw sourceRequired("已跳过报告的数量来源规则已变化");
            return quantities;
        }
        return prepareContext(batch, task, receipt, frozenBinding(batch, task));
    }

    private boolean hasExplicitQuantitySkip(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task,
                                            MesFlow6CompletionBackfillReceipt receipt) {
        List<MesProEdhrOperationAuditEventDO> events = operationAuditMapper.selectByMap(Map.of(
                "tenant_id", TenantContextHolder.getRequiredTenantId(), "batch_execution_id", batch.getId(),
                "operation_type", "OPEN", "result_status", "SUCCESS"));
        boolean skipped = false;
        boolean initialProvisionEvidence = false;
        for (var event : events) {
            if (event.getMetadataJson() == null) continue;
            JsonNode metadata = JsonUtils.parseTree(routeCanonicalizer.canonicalize(event.getMetadataJson()));
            JsonNode results = metadata.get("completionSourceMaterializations");
            if (results == null) continue;
            if (!results.isArray() || !Objects.equals(event.getBatchExecutionId(), batch.getId())
                    || !"BATCH_EXECUTION".equals(event.getObjectType())
                    || !Objects.equals(event.getObjectId(), String.valueOf(batch.getId()))
                    || !Objects.equals(event.getRouteId(), batch.getRouteId())
                    || !"OPEN".equals(event.getOperationType()) || !"SUCCESS".equals(event.getResultStatus())
                    || !"ALLOW".equals(event.getPermissionDecision())
                    || !("ACTIVE_ORDER_COMPLETION".equals(metadata.path("entryType").asText())
                    || "ACTIVE_ORDER_PQC".equals(metadata.path("entryType").asText()))
                    || !Objects.equals(receipt.getActiveOrderId(), nullableLong(metadata.get("activeOrderId")))
                    || !Objects.equals(batch.getWorkOrderId(), nullableLong(metadata.get("workOrderId")))
                    || !Objects.equals(batch.getRouteId(), nullableLong(metadata.get("routeId")))
                    || !Objects.equals(batch.getRouteVersionId(), nullableLong(metadata.get("routeVersionId")))
                    || !Objects.equals(batch.getBatchCode(), metadata.path("batchCode").asText())
                    || !Objects.equals(receipt.getReceiptId(), nullableLong(metadata.get("completionBackfillReceiptId")))
                    || !Objects.equals(receipt.getReceiptHash(), metadata.path("completionBackfillReceiptHash").asText())
                    || !Objects.equals(receipt.getSourceSnapshotHash(), metadata.path("sourceSnapshotHash").asText())
                    || !Objects.equals(String.valueOf(receipt.getReceiptId()), metadata.path("sourceCredentialId").asText())) {
                throw sourceRequired("数量来源OPEN审计与当前冻结批次回执不一致");
            }
            List<JsonNode> matches = new ArrayList<>();
            results.forEach(result -> {
                if (Objects.equals(task.getId(), nullableLong(result.get("taskId")))) matches.add(result);
            });
            if (matches.size() != 1 || !"SKIPPED_NO_SOURCE_RULES".equals(matches.get(0).path("status").asText())
                    || !(matches.get(0).path("executionId").isMissingNode() || matches.get(0).path("executionId").isNull())
                    || !matches.get(0).path("appliedFieldCount").isIntegralNumber()
                    || !matches.get(0).path("appliedFieldCount").canConvertToInt()
                    || matches.get(0).path("appliedFieldCount").intValue() != 0) {
                throw sourceRequired("数量来源OPEN审计缺少唯一无规则结论，APPLIED必须有实际字段审计");
            }
            Long provisioningId = nullableLong(metadata.get("batchProvisionReceiptId"));
            if (provisioningId != null && provisioningId <= 0) throw sourceRequired("数量来源初始provision回执身份无效");
            initialProvisionEvidence |= provisioningId != null;
            skipped = true;
        }
        if (skipped && !initialProvisionEvidence) throw sourceRequired("数量来源缺少初始provision成功审计");
        return skipped;
    }

    private boolean hasSourceAudit(MesProEdhrBatchExecutionTaskDO task, String prefix) {
        return task.getExecutionId() != null && auditBatchMapper.selectListByExecutionId(task.getExecutionId()).stream()
                .anyMatch(row -> row.getReasonText() != null && row.getReasonText().startsWith(prefix));
    }

    private Context prepareAppliedQuantityContext(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task,
                                                  MesFlow6CompletionBackfillReceipt receipt) {
        Context quantities = prepareQuantityContext(batch, task, receipt);
        requireExecution(batch, task);
        if (readApplied(quantities, task) == null) {
            throw sourceRequired("数量来源合同必须对应完整且有效的正式来源审计");
        }
        return quantities;
    }

    private MesFlow6CompletionBackfillReceipt originReceipt(MesProEdhrBatchExecutionDO batch,
                                                           MesProEdhrBatchExecutionTaskDO task) {
        if (!Objects.equals(batch.getId(), task.getBatchExecutionId())) {
            throw sourceRequired("回填任务与批次身份不匹配");
        }
        if (!"PROCESS".equals(task.getInstanceScope()) || !"BATCH_RECORD".equals(task.getRecordCategory())
                || task.getBatchRecordReportId() == null) return null;
        List<MesProEdhrBatchExecutionOriginDO> origins = originMapper.selectListByBatchExecutionId(batch.getId())
                .stream().filter(row -> "ACTIVE_ORDER_COMPLETION".equals(row.getEntryType())
                        || "ACTIVE_ORDER_PQC".equals(row.getEntryType())).toList();
        if (origins.isEmpty()) return null;
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Set<Long> receipts = new LinkedHashSet<>();
        origins.forEach(row -> receipts.add(row.getCompletionBackfillReceiptId()));
        if (!Objects.equals(batch.getTenantId(), tenantId) || receipts.size() != 1 || receipts.contains(null)
                || origins.stream().anyMatch(row -> !Objects.equals(row.getTenantId(), tenantId)
                || !Objects.equals(row.getBatchExecutionId(), batch.getId())
                || !Objects.equals(row.getWorkOrderId(), batch.getWorkOrderId()))) {
            throw sourceRequired("当前批次完工来源身份不唯一或跨租户，batchExecutionId=" + batch.getId());
        }
        MesFlow6CompletionBackfillReceipt receipt = receiptPort.getByReceiptId(receipts.iterator().next(), tenantId);
        requireReceipt(batch, receipt);
        if (!Objects.equals(receipt.getWorkOrderId(), batch.getWorkOrderId())
                || !Objects.equals(receipt.getRouteId(), batch.getRouteId())
                || !Objects.equals(receipt.getRouteVersionId(), batch.getRouteVersionId())
                || !Objects.equals(receipt.getBatchCode(), batch.getBatchCode())
                || origins.stream().anyMatch(row -> !Objects.equals(row.getActiveOrderId(), receipt.getActiveOrderId())
                || !Objects.equals(row.getSourceSnapshotHash(), receipt.getSourceSnapshotHash())
                || !Objects.equals(row.getCompletionBackfillReceiptHash(), receipt.getReceiptHash()))) {
            throw sourceRequired("当前批次与冻结完工回执不匹配，batchExecutionId=" + batch.getId());
        }
        return receipt;
    }

    private void requireReceipt(MesProEdhrBatchExecutionDO batch, MesFlow6CompletionBackfillReceipt receipt) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (receipt == null || !Objects.equals(tenantId, batch.getTenantId())
                || !Objects.equals(tenantId, receipt.getTenantId())
                || !Objects.equals(receipt.getWorkOrderId(), batch.getWorkOrderId())
                || !Objects.equals(receipt.getRouteId(), batch.getRouteId())
                || !Objects.equals(receipt.getRouteVersionId(), batch.getRouteVersionId())
                || !Objects.equals(receipt.getBatchCode(), batch.getBatchCode())
                || receipt.getReceiptHash() == null || receipt.getReceiptHash().isBlank()
                || !Objects.equals(receipt.getSourceSnapshotHash(),
                        MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.sourceSnapshotHash(
                                receipt.getFormalSourceSnapshotJson(), receipt.getLossConditionFactsJson()))) {
            throw sourceRequired("当前批次与正式冻结完工回执或来源哈希不匹配");
        }
    }

    private Context prepareQuantityContext(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task,
                                           MesFlow6CompletionBackfillReceipt receipt) {
        if (!Objects.equals(task.getMaterialSourceSnapshotHash(), receipt.getSourceSnapshotHash())) {
            throw sourceRequired("数量来源任务冻结哈希与正式回执不一致");
        }
        return quantityContext(batch, task, prepareContext(batch, task, receipt, frozenQuantityBinding(batch, task)));
    }

    private Context prepareContext(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task,
            MesFlow6CompletionBackfillReceipt receipt, MesProRouteFlowProcessBatchRecordDO binding) {
        List<MesProBatchRecordCellLinkRuleDO> rules = ruleMapper.selectEnabledListByScopeAndTargetReport(
                        "ROUTE_VERSION", binding.getBatchRecordVersionId(), binding.getBatchRecordReportId(),
                        task.getRouteProcessId()).stream()
                .filter(row -> "PROCESS_POOL_REPORT".equals(row.getSourceType())
                        || "PRODUCTION_PICK_LIST".equals(row.getSourceType()))
                .sorted(Comparator.comparing(MesProBatchRecordCellLinkRuleDO::getId)).toList();
        String problem = null;
        if (rules.isEmpty()) problem = "当前工序正式批记录缺少生产来源单元格映射，routeProcessId="
                + task.getRouteProcessId() + "，reportId=" + task.getBatchRecordReportId();
        Set<String> targets = new HashSet<>();
        for (MesProBatchRecordCellLinkRuleDO rule : rules) {
            if (!"ROUTE_VERSION".equals(rule.getScopeType())
                    || !Objects.equals(binding.getBatchRecordVersionId(), rule.getScopeId())
                    || !Objects.equals(binding.getBatchRecordReportId(), rule.getTargetReportId())
                    || !Objects.equals(task.getRouteProcessId(), rule.getRouteProcessId())
                    || rule.getSourceFieldCode() == null || rule.getTargetRowIndex() == null
                    || rule.getTargetColumnIndex() == null
                    || !targets.add(rule.getTargetRowIndex() + ":" + rule.getTargetColumnIndex())) {
                throw sourceRequired("正式生产映射身份或目标格冲突，ruleId=" + rule.getId());
            }
            if ("PRODUCTION_PICK_LIST".equals(rule.getSourceType())) {
                problem = "领料映射缺少受支持的冻结项目与来源选择合同，ruleId=" + rule.getId()
                        + "；不得任选领料单或使用当前配置替代冻结来源";
            }
        }
        return contextForRules(batch, task, receipt, binding, rules, problem, "");
    }

    private Context quantityContext(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task,
                                    Context fullContext) {
        List<MesProBatchRecordCellLinkRuleDO> quantities = fullContext.rules().stream()
                .filter(rule -> "PROCESS_POOL_REPORT".equals(rule.getSourceType())
                        && Set.of("outputQuantity", "lossQuantity").contains(rule.getSourceFieldCode())).toList();
        String problem = quantities.isEmpty() ? "当前正式报告无已配置生产数量来源规则" : null;
        return contextForRules(batch, task, fullContext.receipt(), fullContext.binding(), quantities,
                problem, "scope=QUANTITY|");
    }

    private Context contextForRules(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task,
            MesFlow6CompletionBackfillReceipt receipt, MesProRouteFlowProcessBatchRecordDO binding,
            List<MesProBatchRecordCellLinkRuleDO> rules, String problem, String auditScope) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String mappingHash = DigestUtil.sha256Hex(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer
                .canonicalize(JsonUtils.toJsonString(rules)));
        String key = DigestUtil.sha256Hex(tenantId + "|" + batch.getId() + "|" + task.getId() + "|"
                + binding.getId() + "|" + binding.getBatchRecordReportId() + "|" + binding.getBatchRecordVersionId()
                + "|" + receipt.getReceiptHash() + "|" + receipt.getSourceSnapshotHash() + "|" + auditScope + mappingHash);
        String reason = AUDIT_REASON_PREFIX + auditScope + "receipt=" + receipt.getReceiptId() + "|task=" + task.getId()
                + "|source=" + receipt.getSourceSnapshotHash() + "|mapping=" + mappingHash;
        return new Context(receipt, binding, List.copyOf(rules), key, reason, mappingHash, problem);
    }

    private MesProRouteFlowProcessBatchRecordDO frozenBinding(MesProEdhrBatchExecutionDO batch,
                                                             MesProEdhrBatchExecutionTaskDO task) {
        if (task.getRouteBindingId() == null) throw sourceRequired("任务缺少正式冻结批记录绑定身份");
        var route = routeResolver.resolveVersion(batch.getRouteVersionId());
        if (!Objects.equals(batch.getRouteId(), route.routeId())
                || !MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(batch.getRouteSnapshotJson())
                .equals(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(route.routeSnapshotJson()))) {
            throw sourceRequired("当前批次冻结路线快照不匹配");
        }
        return bindingFromSnapshot(batch, task, route.routeSnapshotJson());
    }

    /** New quantity receipts require the exact published identity, independently of legacy migration readiness. */
    private MesProRouteFlowProcessBatchRecordDO frozenQuantityBinding(MesProEdhrBatchExecutionDO batch,
                                                                     MesProEdhrBatchExecutionTaskDO task) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (batch.getRouteVersionId() == null || batch.getRouteVersionId() <= 0
                || batch.getRouteId() == null || batch.getRouteId() <= 0 || task.getRouteBindingId() == null
                || !Objects.equals(batch.getId(), task.getBatchExecutionId())
                || !"MAIN".equals(task.getFormSlotType()) || !"PROCESS".equals(task.getInstanceScope())
                || !"BATCH_RECORD".equals(task.getRecordCategory())) {
            throw sourceRequired("数量来源缺少精确正式逐工序主批记录身份");
        }
        List<MesProRouteVersionDO> versions = routeVersionMapper.selectByMap(Map.of(
                "id", batch.getRouteVersionId(), "tenant_id", tenantId));
        if (versions.size() != 1) throw sourceRequired("数量来源当前租户发布路线版本不唯一或不存在");
        MesProRouteVersionDO version = versions.get(0);
        if (version == null || !Objects.equals(version.getId(), batch.getRouteVersionId())
                || !Objects.equals(batch.getTenantId(), tenantId)
                || !Objects.equals(version.getRouteId(), batch.getRouteId()) || !Boolean.FALSE.equals(version.getDeleted())
                || !("ACTIVE".equals(version.getLifecycleStatus()) || "SUPERSEDED".equals(version.getLifecycleStatus()))
                || version.getActive() == null
                || "ACTIVE".equals(version.getLifecycleStatus()) != Boolean.TRUE.equals(version.getActive())
                || !MesProRouteSnapshotCanonicalizer.FORMAT_VERSION.equals(version.getRouteSnapshotFormatVersion())
                || version.getRouteSnapshotSha256() == null
                || !version.getRouteSnapshotSha256().matches("[0-9a-f]{64}")) {
            throw sourceRequired("数量来源精确发布路线身份缺失或不一致");
        }
        if (!routeCanonicalizer.sha256(version.getRouteSnapshotJson()).equals(version.getRouteSnapshotSha256())
                || !routeCanonicalizer.validate(version.getRouteId(), version.getRouteSnapshotJson()).ready()
                || !routeCanonicalizer.canonicalize(batch.getRouteSnapshotJson())
                .equals(routeCanonicalizer.canonicalize(version.getRouteSnapshotJson()))) {
            throw sourceRequired("数量来源发布路线哈希、正式结构或批次冻结快照不一致");
        }
        JsonNode nodes = JsonUtils.parseTree(version.getRouteSnapshotJson()).path("configSnapshots").path("flowGraph").path("nodes");
        List<JsonNode> matches = new ArrayList<>();
        nodes.forEach(node -> {
            if (Objects.equals(task.getRouteProcessId(), nullableLong(node.get("routeProcessId")))) matches.add(node);
        });
        if (matches.size() != 1 || !Objects.equals(task.getProcessId(), nullableLong(matches.get(0).get("processId")))) {
            throw sourceRequired("数量来源任务与冻结正式工序身份不一致");
        }
        MesProRouteFlowProcessBatchRecordDO binding = bindingFromSnapshot(batch, task, version.getRouteSnapshotJson());
        if (!"MAIN".equals(binding.getFormSlotType()) || binding.getBatchRecordDefinitionId() == null
                || binding.getBatchRecordDefinitionId() <= 0 || binding.getBatchRecordVersionId() <= 0
                || binding.getId() <= 0 || task.getRouteProcessId() <= 0 || task.getProcessId() == null
                || task.getProcessId() <= 0 || binding.getBatchRecordReportId() == null
                || binding.getBatchRecordReportId().isBlank()) {
            throw sourceRequired("数量来源正式主批记录绑定身份不完整");
        }
        return binding;
    }

    private MesProRouteFlowProcessBatchRecordDO bindingFromSnapshot(MesProEdhrBatchExecutionDO batch,
            MesProEdhrBatchExecutionTaskDO task, String routeSnapshotJson) {
        List<MesProRouteFlowProcessBatchRecordDO> bindings = new ArrayList<>();
        JsonNode configs = JsonUtils.parseTree(routeSnapshotJson).path("configSnapshots").path("batchUseConfigs");
        if (!configs.isArray()) throw sourceRequired("冻结路线缺少逐工序批记录绑定");
        for (JsonNode config : configs) {
            if (!Objects.equals(task.getRouteProcessId(), nullableLong(config.get("routeProcessId")))
                    || !"BATCH".equals(config.path("useType").asText())) continue;
            JsonNode reports = config.path("batchRecordReports");
            if (!reports.isArray()) throw sourceRequired("冻结路线缺少正式批记录报告集合");
            for (JsonNode report : reports) {
                if (!Objects.equals(task.getRouteBindingId(), nullableLong(report.get("routeBindingId")))) continue;
                MesProRouteFlowProcessBatchRecordDO binding = JsonUtils.parseObject(report.toString(),
                        MesProRouteFlowProcessBatchRecordDO.class);
                binding.setId(task.getRouteBindingId());
                binding.setRouteId(batch.getRouteId());
                binding.setRouteProcessId(task.getRouteProcessId());
                binding.setUseType("BATCH");
                bindings.add(binding);
            }
        }
        if (bindings.size() != 1) throw sourceRequired("正式冻结批记录绑定不唯一，taskId=" + task.getId());
        MesProRouteFlowProcessBatchRecordDO binding = bindings.get(0);
        if (!"PROCESS".equals(binding.getInstanceScope()) || !"BATCH_RECORD".equals(binding.getRecordCategory())
                || !Objects.equals(task.getBatchRecordReportId(), binding.getBatchRecordReportId())
                || !Objects.equals(task.getBatchRecordDefinitionId(), binding.getBatchRecordDefinitionId())
                || !Objects.equals(task.getBatchRecordVersionId(), binding.getBatchRecordVersionId())
                || binding.getBatchRecordVersionId() == null) {
            throw sourceRequired("当前任务与冻结正式绑定身份不匹配，taskId=" + task.getId());
        }
        return binding;
    }

    private MesProBatchRecordExecutionDO requireExecution(MesProEdhrBatchExecutionDO batch,
                                                         MesProEdhrBatchExecutionTaskDO task) {
        MesProBatchRecordExecutionDO execution = task.getExecutionId() == null ? null
                : executionMapper.selectById(task.getExecutionId());
        if (execution == null || !Objects.equals(execution.getBatchExecutionId(), batch.getId())
                || !Objects.equals(execution.getTaskId(), task.getId())
                || !Objects.equals(execution.getWorkOrderId(), batch.getWorkOrderId())
                || !Objects.equals(execution.getRouteId(), batch.getRouteId())
                || !Objects.equals(execution.getBatchCode(), batch.getBatchCode())
                || !Objects.equals(execution.getRouteProcessId(), task.getRouteProcessId())
                || !Objects.equals(execution.getRouteBindingId(), task.getRouteBindingId())
                || !"PROCESS".equals(execution.getInstanceScope())
                || !"BATCH_RECORD".equals(execution.getRecordCategory())
                || !Objects.equals(execution.getBatchRecordReportId(), task.getBatchRecordReportId())
                || !Objects.equals(execution.getBatchRecordDefinitionId(), task.getBatchRecordDefinitionId())
                || !Objects.equals(execution.getBatchRecordVersionId(), task.getBatchRecordVersionId())) {
            throw sourceRequired("正式回填执行实例与批次任务不匹配，taskId=" + task.getId());
        }
        return execution;
    }

    private ReadResult readApplied(Context context, MesProEdhrBatchExecutionTaskDO task) {
        if (task.getExecutionId() == null) return null;
        List<MesProBatchRecordExecutionFieldAuditBatchDO> audits = auditBatchMapper
                .selectListByExecutionId(task.getExecutionId()).stream()
                .filter(row -> row.getReasonText() != null && row.getReasonText().startsWith(AUDIT_REASON_PREFIX)).toList();
        if (audits.isEmpty()) return null;
        if (audits.size() != 1 || !Objects.equals(audits.get(0).getIdempotencyKey(), context.key())
                || !Objects.equals(audits.get(0).getReasonText(), context.reason())
                || !Objects.equals(audits.get(0).getTenantId(), context.receipt().getTenantId())) {
            throw sourceRequired("正式回填映射或来源已变化，禁止重用旧审计，taskId=" + task.getId());
        }
        MesProBatchRecordExecutionFieldAuditBatchDO audit = audits.get(0);
        var verification = auditService.verifyChain(task.getExecutionId());
        if (verification == null || verification.getStatus() != MesProBatchRecordExecutionFieldAuditHashVerificationStatus.VALID) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
        List<MesProBatchRecordExecutionFieldAuditItemDO> items = auditItemMapper.selectListByBatchId(audit.getId());
        if (audit.getId() == null || items.isEmpty() || !Objects.equals(audit.getFieldCount(), items.size())
                || items.stream().anyMatch(item -> !Objects.equals(item.getAuditBatchId(), audit.getId())
                || !Objects.equals(item.getExecutionId(), task.getExecutionId())
                || !Objects.equals(item.getReasonText(), context.reason()))) {
            throw sourceRequired("正式回填字段审计不完整，taskId=" + task.getId());
        }
        List<Map<String, Object>> cells = new ArrayList<>();
        for (var item : items) {
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("rowIndex", item.getRowIndex());
            cell.put("columnIndex", item.getColumnIndex());
            cell.put("fieldPath", item.getFieldPath());
            cell.put("fieldKey", item.getFieldKey());
            cell.put("value", JsonUtils.parseTree(item.getNewValueJson()));
            cells.add(cell);
        }
        return new ReadResult(evidence(context, "APPLIED").setMessage("生产事实已回填，表单状态按正式流程办理")
                .setExecutionId(String.valueOf(task.getExecutionId())).setAuditBatchId(String.valueOf(audit.getId()))
                .setFieldAuditHeadHash(audit.getNewHeadHash()).setAppliedFieldCount(items.size()), JsonUtils.toJsonString(cells));
    }

    private static void requireMapping(Context context, MesProEdhrBatchExecutionTaskDO task) {
        if (context.problem() != null) throw exception(PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED,
                task.getRouteProcessId(), task.getBatchRecordReportId(), context.problem());
    }

    private static void requireReadableDraftCells(MesProBatchRecordExecutionDO execution) {
        if (execution.getCellValuesJson() == null || execution.getCellValuesJson().isBlank()) return;
        JsonNode cells;
        try {
            cells = JsonUtils.parseTree(execution.getCellValuesJson());
        } catch (RuntimeException invalid) {
            throw sourceRequired("现有草稿单元格数据无效，禁止视为空白后回填");
        }
        if (!cells.isArray()) throw sourceRequired("现有草稿单元格必须为数组，禁止视为空白后回填");
        Set<String> positions = new HashSet<>();
        for (JsonNode cell : cells) {
            if (!cell.path("rowIndex").canConvertToInt() || !cell.path("columnIndex").canConvertToInt()
                    || !positions.add(cell.path("rowIndex").asInt() + ":" + cell.path("columnIndex").asInt())) {
                throw sourceRequired("现有草稿单元格位置缺失或重复，禁止自动回填");
            }
        }
    }

    private static CompletionSourceMaterialization evidence(Context context, String status) {
        return new CompletionSourceMaterialization().setStatus(status)
                .setReceiptId(String.valueOf(context.receipt().getReceiptId()))
                .setSourceSnapshotHash(context.receipt().getSourceSnapshotHash()).setMappingHash(context.mappingHash());
    }

    private static <T> List<T> rows(JsonNode value, Class<T> type) {
        if (!value.isArray()) throw sourceRequired("冻结正式来源集合缺失：" + type.getSimpleName());
        List<T> rows = new ArrayList<>();
        value.forEach(row -> rows.add(JsonUtils.parseObject(row.toString(), type)));
        return rows;
    }

    private static Long nullableLong(JsonNode value) {
        return value == null || value.isNull() ? null : Long.valueOf(value.asText());
    }

    private static RuntimeException sourceRequired(String message) {
        return exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED, message);
    }

    private record Context(MesFlow6CompletionBackfillReceipt receipt, MesProRouteFlowProcessBatchRecordDO binding,
                           List<MesProBatchRecordCellLinkRuleDO> rules, String key, String reason,
                           String mappingHash, String problem) { }

    public record ReadResult(CompletionSourceMaterialization evidence, String cellValuesJson) { }

    public record ProvisionedResult(Long taskId, String status, Long executionId, Integer appliedFieldCount) { }
}
