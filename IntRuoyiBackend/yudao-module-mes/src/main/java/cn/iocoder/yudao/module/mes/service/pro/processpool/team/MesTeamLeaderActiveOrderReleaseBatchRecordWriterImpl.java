package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.MesProductionPickListSourceService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

@Service
public class MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl
        implements MesTeamLeaderActiveOrderReleaseBatchRecordWriter {

    private static final String USE_TYPE_BATCH = "BATCH";
    private static final String RECORD_CATEGORY_BATCH_RECORD = "BATCH_RECORD";
    private static final String PROCESS_INSPECTION = "PROCESS_INSPECTION";
    private static final String LOSS_REPORT = "LOSS_REPORT";
    private static final String SOURCE_TYPE_PROCESS_POOL_REPORT = "PROCESS_POOL_REPORT";
    private static final String SOURCE_TYPE_PRODUCTION_PICK_LIST = MesProductionPickListSourceService.SOURCE_TYPE;
    private static final String SCOPE_TYPE_ROUTE_VERSION = "ROUTE_VERSION";
    private static final String LEADER_TYPE_PRODUCTION = "PRODUCTION";

    private final MesProRouteFlowProcessBatchRecordMapper bindingMapper;
    private final MesProBatchRecordCellLinkRuleMapper ruleMapper;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesTeamLeaderBatchRecordBackfillService backfillService;
    private final MesProductionPickListSourceService productionPickListSourceService;

    public MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl(
            MesProRouteFlowProcessBatchRecordMapper bindingMapper,
            MesProBatchRecordCellLinkRuleMapper ruleMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesTeamLeaderBatchRecordBackfillService backfillService,
            MesProductionPickListSourceService productionPickListSourceService) {
        this.bindingMapper = bindingMapper;
        this.ruleMapper = ruleMapper;
        this.batchTaskMapper = batchTaskMapper;
        this.backfillService = backfillService;
        this.productionPickListSourceService = productionPickListSourceService;
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan(
            MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command) {
        validateCommand(command);
        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = new ArrayList<>();
        List<MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess> preparedProcesses = new ArrayList<>();
        LinkedHashSet<Long> sourceObjectIds = new LinkedHashSet<>();
        LinkedHashSet<String> sourceValueHashes = new LinkedHashSet<>();
        List<MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult.SignatureEvidence> signatures = new ArrayList<>();
        Set<ProcessIdentity> identities = new LinkedHashSet<>();

        for (MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource source
                : command.getProcessSources()) {
            int blockerCountBefore = blockers.size();
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot = source == null ? null : source.getSnapshot();
            if (!validateProcessIdentity(command, source, blockers)) {
                continue;
            }
            ProcessIdentity identity = new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId());
            if (!identities.add(identity)) {
                blockers.add(blocker("PRODUCTION_HISTORY_REQUIRED", "ROUTE_PROCESS", snapshot.getRouteProcessId(),
                        "同一工序存在重复的生产完成来源", "请修复活跃订单工序完成快照后重新申请"));
                continue;
            }

            MesProRouteFlowProcessBatchRecordDO binding = formalBinding(snapshot, blockers);
            List<MesProBatchRecordCellLinkRuleDO> rules = binding == null
                    ? List.of() : formalRules(snapshot, binding, blockers);
            validateHistorySources(command, source, rules, blockers, sourceObjectIds, sourceValueHashes, signatures);
            if (blockers.size() == blockerCountBefore) {
                preparedProcesses.add(new MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess()
                        .setSource(source)
                        .setBinding(binding)
                        .setRules(List.copyOf(rules)));
            }
        }
        preparedProcesses.sort(Comparator.comparing(item -> item.getSource().getSnapshot().getRouteProcessId()));
        sourceObjectIds.addAll(preparedProcesses.stream()
                .map(item -> item.getBinding().getId())
                .filter(Objects::nonNull)
                .toList());
        return new MesTeamLeaderActiveOrderReleaseBatchRecordPlan()
                .setCommand(command)
                .setPreparedProcesses(List.copyOf(preparedProcesses))
                .setSourceObjectIds(List.copyOf(sourceObjectIds))
                .setSourceValueHashes(List.copyOf(sourceValueHashes))
                .setSignatureEvidence(List.copyOf(signatures))
                .setBlockers(List.copyOf(blockers));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult write(
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan, Long batchExecutionId) {
        if (plan == null || plan.getCommand() == null || plan.getPreparedProcesses() == null
                || plan.getBlockers() == null || batchExecutionId == null || batchExecutionId <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseBatchRecordWrite");
        }
        if (!plan.getBlockers().isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "批记录 writer plan 存在 blocker，禁止写入");
        }
        List<MesProEdhrBatchExecutionTaskDO> batchTasks =
                batchTaskMapper.selectListByBatchExecutionId(batchExecutionId);
        List<Long> executionIds = new ArrayList<>();
        List<Long> auditBatchIds = new ArrayList<>();
        List<String> auditHeadHashes = new ArrayList<>();
        for (MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess prepared
                : plan.getPreparedProcesses()) {
            MesProEdhrBatchExecutionTaskDO task = requireCurrentBatchTask(batchExecutionId, batchTasks, prepared);
            MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource source = prepared.getSource();
            MesProcessPoolOrderProcessCompletionDO completion = source.getCompletion();
            List<MesProProcessPoolEventDO> sourceEvents = orderedEvents(source.getSourceEvents());
            List<MesProcessPoolReportAllocationDO> allocations = orderedAllocations(source.getAllocations());
            MesProProcessPoolEventDO event = sourceEvents.stream()
                    .filter(item -> Objects.equals(completion.getLastEventId(), item.getId()))
                    .findFirst()
                    .orElseThrow(() -> exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                            "批记录 writer 缺少 completion 末次生产提交，eventId="
                                    + completion.getLastEventId()));
            MesProcessPoolReportAllocationDO allocation = allocations.stream()
                    .filter(item -> Objects.equals(item.getEventId(), event.getId()))
                    .reduce((first, second) -> second)
                    .orElseThrow(() -> exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                            "批记录 writer 缺少末次生产提交对应分配，eventId=" + event.getId()));
            MesTeamLeaderBatchRecordBackfillResult backfill = backfillService.backfillCompletedProcess(
                    new MesTeamLeaderBatchRecordBackfillCommand()
                            .setEvent(event)
                            .setAllocation(allocation)
                            .setSourceEvents(sourceEvents)
                            .setAllocations(allocations)
                            .setAggregateHash(completion.getAggregateHash())
                            .setIdempotencyKey(completion.getBackfillIdempotencyKey())
                            .setWorkOrder(plan.getCommand().getWorkOrder())
                            .setDccProjectCodeId(plan.getCommand().getDccProjectCodeId())
                            .setBatchExecutionId(batchExecutionId)
                            .setBatchExecutionTaskId(task.getId()));
            if (backfill == null || backfill.getExecutionId() == null || backfill.getAuditBatchId() == null) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                        "批记录 writer 未返回当前批次 execution 或字段审计，routeProcessId="
                                + source.getSnapshot().getRouteProcessId());
            }
            executionIds.add(backfill.getExecutionId());
            auditBatchIds.add(backfill.getAuditBatchId());
            if (StrUtil.isNotBlank(backfill.getFieldAuditHeadHash())) {
                auditHeadHashes.add(backfill.getFieldAuditHeadHash());
            }
        }
        return new MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult()
                .setDocumentType(RECORD_CATEGORY_BATCH_RECORD)
                .setBatchRecordExecutionIds(distinct(executionIds))
                .setFieldAuditIds(distinct(auditBatchIds))
                .setFieldAuditHeadHashes(List.copyOf(new LinkedHashSet<>(auditHeadHashes)))
                .setSourceObjectIds(plan.getSourceObjectIds())
                .setSourceValueHashes(plan.getSourceValueHashes())
                .setSignatureEvidence(plan.getSignatureEvidence())
                .setBlockers(List.of());
    }

    private void validateCommand(MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command) {
        if (command == null || command.getTenantId() == null || command.getActiveOrderId() == null
                || command.getWorkOrderId() == null
                || command.getRouteId() == null || command.getRouteVersionId() == null
                || command.getDccProjectCodeId() == null
                || command.getProductId() == null || StrUtil.isBlank(command.getBatchCode())
                || command.getApplicantUserId() == null
                || command.getWorkOrder() == null || command.getWorkOrder().getId() == null
                || StrUtil.isBlank(command.getWorkOrder().getBatchCode())
                || StrUtil.isBlank(command.getSourceSnapshotHash())
                || command.getProcessSources() == null || command.getProcessSources().isEmpty()
                || !Objects.equals(command.getWorkOrderId(), command.getWorkOrder().getId())
                || !Objects.equals(command.getProductId(), command.getWorkOrder().getProductId())
                || !Objects.equals(StrUtil.trim(command.getBatchCode()),
                        StrUtil.trim(command.getWorkOrder().getBatchCode()))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseBatchRecordPlan");
        }
    }

    private boolean validateProcessIdentity(MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command,
                                            MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource source,
                                            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = source == null ? null : source.getSnapshot();
        MesProcessPoolOrderProcessCompletionDO completion = source == null ? null : source.getCompletion();
        if (snapshot == null || completion == null || snapshot.getRouteProcessId() == null
                || snapshot.getProcessId() == null) {
            blockers.add(blocker("PRODUCTION_HISTORY_REQUIRED", "ACTIVE_ORDER", command.getActiveOrderId(),
                    "活跃订单工序缺少正式完成来源", "请完成生产组长确认并形成工序完成记录"));
            return false;
        }
        boolean snapshotMatches = Objects.equals(command.getActiveOrderId(), snapshot.getActiveOrderId())
                && Objects.equals(command.getWorkOrder().getId(), snapshot.getWorkOrderId())
                && Objects.equals(command.getRouteId(), snapshot.getRouteId())
                && Objects.equals(command.getRouteVersionId(), snapshot.getRouteVersionId());
        boolean completionMatches = Objects.equals(snapshot.getWorkOrderId(), completion.getWorkOrderId())
                && Objects.equals(snapshot.getRouteProcessId(), completion.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), completion.getProcessId())
                && MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED.equals(completion.getCompletionStatus())
                && MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS.equals(completion.getBackfillStatus())
                && completion.getBackfillExecutionId() != null
                && completion.getTargetQuantity() != null && completion.getTargetQuantity().signum() > 0
                && completion.getConfirmedQuantity() != null
                && completion.getConfirmedQuantity().compareTo(completion.getTargetQuantity()) >= 0
                && StrUtil.isNotBlank(completion.getAggregateHash())
                && StrUtil.isNotBlank(completion.getBackfillIdempotencyKey());
        if (!snapshotMatches || !completionMatches) {
            blockers.add(blocker("PRODUCTION_HISTORY_REQUIRED", "ROUTE_PROCESS", snapshot.getRouteProcessId(),
                    "工序完成记录与活跃订单正式快照不一致或回填未成功", "请修复生产完成和历史回填证据后重新申请"));
            return false;
        }
        return true;
    }

    private MesProRouteFlowProcessBatchRecordDO formalBinding(
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        List<MesProRouteFlowProcessBatchRecordDO> formal = bindingMapper
                .selectListByRouteProcessIdsAndUseType(List.of(snapshot.getRouteProcessId()), USE_TYPE_BATCH)
                .stream()
                .filter(binding -> binding != null
                        && binding.getId() != null
                        && Objects.equals(snapshot.getRouteId(), binding.getRouteId())
                        && Objects.equals(snapshot.getRouteProcessId(), binding.getRouteProcessId())
                        && StrUtil.isNotBlank(binding.getBatchRecordReportId())
                        && binding.getBatchRecordDefinitionId() != null
                        && binding.getBatchRecordVersionId() != null
                        && RECORD_CATEGORY_BATCH_RECORD.equals(binding.getRecordCategory())
                        && !PROCESS_INSPECTION.equals(binding.getFormSlotType())
                        && !LOSS_REPORT.equals(binding.getFormSlotType()))
                .toList();
        if (formal.size() != 1) {
            blockers.add(blocker("BATCH_RECORD_BINDING_REQUIRED", "ROUTE_PROCESS", snapshot.getRouteProcessId(),
                    "工序必须存在唯一逐工序正式批记录绑定，当前数量=" + formal.size(),
                    "请在工序设置中维护唯一正式批记录表单"));
            return null;
        }
        return formal.get(0);
    }

    private List<MesProBatchRecordCellLinkRuleDO> formalRules(
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            MesProRouteFlowProcessBatchRecordDO binding,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        List<MesProBatchRecordCellLinkRuleDO> rules = ruleMapper.selectEnabledListByScopeAndTargetReport(
                SCOPE_TYPE_ROUTE_VERSION, binding.getBatchRecordVersionId(), binding.getBatchRecordReportId());
        Set<String> targetCells = new LinkedHashSet<>();
        boolean valid = rules != null && !rules.isEmpty();
        if (valid) {
            for (MesProBatchRecordCellLinkRuleDO rule : rules) {
                String targetCell = rule == null ? null : rule.getTargetRowIndex() + ":" + rule.getTargetColumnIndex();
                if (rule == null || rule.getId() == null || !Boolean.TRUE.equals(rule.getEnabled())
                        || !Set.of(SOURCE_TYPE_PROCESS_POOL_REPORT, SOURCE_TYPE_PRODUCTION_PICK_LIST)
                                .contains(StrUtil.trim(rule.getSourceType()))
                        || StrUtil.isBlank(rule.getSourceFieldCode())
                        || !Objects.equals(binding.getBatchRecordReportId(), rule.getTargetReportId())
                        || rule.getTargetRowIndex() == null || rule.getTargetColumnIndex() == null
                        || !targetCells.add(targetCell)) {
                    valid = false;
                    break;
                }
            }
        }
        if (!valid) {
            blockers.add(blocker("BATCH_RECORD_MAPPING_REQUIRED", "ROUTE_PROCESS", snapshot.getRouteProcessId(),
                    "正式批记录缺少完整且唯一的报工/领料单字段映射",
                    "请配置生产提交或领料单字段到当前批记录版本的启用映射"));
            return List.of();
        }
        return List.copyOf(rules);
    }

    private void validateHistorySources(
            MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command,
            MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource source,
            List<MesProBatchRecordCellLinkRuleDO> rules,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers,
            Set<Long> sourceObjectIds,
            Set<String> sourceValueHashes,
            List<MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult.SignatureEvidence> signatures) {
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = source.getSnapshot();
        MesProcessPoolOrderProcessCompletionDO completion = source.getCompletion();
        List<MesProProcessPoolEventDO> events = source.getSourceEvents();
        List<MesProcessPoolReportAllocationDO> allocations = source.getAllocations();
        List<MesProcessPoolSubmissionReviewDO> reviews = source.getReviews();
        if (events == null || events.isEmpty() || allocations == null || allocations.isEmpty()
                || reviews == null || reviews.isEmpty()) {
            blockers.add(blocker("PRODUCTION_HISTORY_REQUIRED", "ROUTE_PROCESS", snapshot.getRouteProcessId(),
                    "工序缺少生产提交、分配或生产组长确认记录", "请补齐正式生产历史后重新申请"));
            return;
        }
        List<Long> eventIds = orderedEvents(events).stream().map(MesProProcessPoolEventDO::getId).toList();
        List<Long> allocationIds = orderedAllocations(allocations).stream()
                .map(MesProcessPoolReportAllocationDO::getId).toList();
        if (!eventIds.equals(sortedIds(parseIds(completion.getSourceEventIdsJson())))
                || !allocationIds.equals(sortedIds(parseIds(completion.getSourceAllocationIdsJson())))
                || !eventIds.contains(completion.getLastEventId())) {
            blockers.add(blocker("PRODUCTION_HISTORY_REQUIRED", "ROUTE_PROCESS", snapshot.getRouteProcessId(),
                    "工序完成记录的来源事件/分配快照与正式记录不一致", "请修复生产完成来源追溯后重新申请"));
            return;
        }

        Map<Long, MesProProcessPoolEventDO> eventById = new LinkedHashMap<>();
        for (MesProProcessPoolEventDO event : events) {
            if (!validEventContext(command, snapshot, event)) {
                blockers.add(blocker("PRODUCTION_HISTORY_REQUIRED", "PRODUCTION_EVENT",
                        event == null ? null : event.getId(), "生产提交不属于当前工单、工艺路线或工序",
                        "请修复当前工单工序的正式生产提交来源"));
                return;
            }
            if (!validEventSignature(event)) {
                blockers.add(blocker("PRODUCTION_SIGNATURE_REQUIRED", "PRODUCTION_EVENT",
                        event == null ? null : event.getId(), "生产提交缺少真实填写人、签名或提交时间",
                        "请使用正式一线生产提交并完成电子签名"));
                return;
            }
            eventById.put(event.getId(), event);
            sourceObjectIds.add(event.getId());
            String evidenceHash = hashEvent(event);
            sourceValueHashes.add(evidenceHash);
            signatures.add(signature("FILLER", "PRODUCTION_SUBMIT", event.getId(), event.getSignatureId(),
                    event.getSignatureUserId(), event.getServerSubmitTime(), evidenceHash));
        }
        Map<Long, MesProcessPoolSubmissionReviewDO> reviewById = new LinkedHashMap<>();
        for (MesProcessPoolSubmissionReviewDO review : reviews) {
            if (!validReview(eventById, review)) {
                blockers.add(blocker("PRODUCTION_SIGNATURE_REQUIRED", "PRODUCTION_REVIEW",
                        review == null ? null : review.getId(), "生产组长确认缺少 APPROVED 复核签名或确认时间",
                        "请由正式生产组长确认并完成电子签名"));
                return;
            }
            reviewById.put(review.getId(), review);
            sourceObjectIds.add(review.getId());
            String evidenceHash = hashReview(review);
            sourceValueHashes.add(evidenceHash);
            signatures.add(signature("REVIEWER", "PRODUCTION_LEADER_CONFIRM", review.getId(),
                    review.getReviewSignatureId(), review.getReviewSignatureUserId(), review.getReviewedAt(),
                    evidenceHash));
        }
        for (MesProcessPoolReportAllocationDO allocation : allocations) {
            MesProProcessPoolEventDO event = allocation == null ? null : eventById.get(allocation.getEventId());
            MesProcessPoolSubmissionReviewDO review = allocation == null ? null : reviewById.get(allocation.getReviewId());
            if (!validAllocation(command, snapshot, allocation, event, review)) {
                blockers.add(blocker("PRODUCTION_HISTORY_REQUIRED", "REPORT_ALLOCATION",
                        allocation == null ? null : allocation.getId(),
                        "报工分配与活跃订单、生产提交或组长确认不一致", "请修复正式报工分配追溯后重新申请"));
                return;
            }
            sourceObjectIds.add(allocation.getId());
            sourceValueHashes.add(hashAllocation(allocation));
        }
        if (!reviewById.containsKey(completion.getLastReviewId())) {
            blockers.add(blocker("PRODUCTION_HISTORY_REQUIRED", "ROUTE_PROCESS", snapshot.getRouteProcessId(),
                    "工序完成记录的末次组长确认不存在", "请修复生产完成确认追溯后重新申请"));
            return;
        }
        List<MesProBatchRecordCellLinkRuleDO> reportRules = rules.stream()
                .filter(rule -> SOURCE_TYPE_PROCESS_POOL_REPORT.equals(StrUtil.trim(rule.getSourceType())))
                .toList();
        if (!reportRules.isEmpty() && !validateRuleSourceValues(events, allocations, reportRules)) {
            blockers.add(blocker("BATCH_RECORD_MAPPING_REQUIRED", "ROUTE_PROCESS", snapshot.getRouteProcessId(),
                    "批记录字段映射缺少对应生产来源值或多来源聚合策略", "请补齐生产参数及正式字段映射"));
            return;
        }
        if (!validatePickListSourceValues(command, snapshot, rules, blockers, sourceObjectIds, sourceValueHashes)) {
            return;
        }
        sourceObjectIds.add(completion.getId());
        sourceObjectIds.add(completion.getBackfillExecutionId());
        sourceValueHashes.add(hashCompletion(completion));
    }

    private boolean validatePickListSourceValues(
            MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            List<MesProBatchRecordCellLinkRuleDO> rules,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers,
            Set<Long> sourceObjectIds,
            Set<String> sourceValueHashes) {
        for (MesProBatchRecordCellLinkRuleDO rule : rules) {
            if (!SOURCE_TYPE_PRODUCTION_PICK_LIST.equals(StrUtil.trim(rule.getSourceType()))) {
                continue;
            }
            try {
                MesProductionPickListSourceService.ResolvedValue resolved = productionPickListSourceService.resolveValue(
                        new MesProductionPickListSourceService.ResolveCommand(command.getRouteId(),
                                snapshot.getRouteProcessId(), command.getProductId(), command.getDccProjectCodeId(),
                                command.getWorkOrder().getCode(), rule.getSourceFieldCode()));
                sourceObjectIds.add(resolved.pickListId());
                sourceObjectIds.add(resolved.pickListItemId());
                sourceValueHashes.add(resolved.evidenceHash());
            } catch (ServiceException ex) {
                blockers.add(blocker("PRODUCTION_PICK_LIST_REQUIRED", "ROUTE_PROCESS",
                        snapshot.getRouteProcessId(), ex.getMessage(),
                        "请核对活跃订单工单、DCC 产品、已审核领料单及物料分录后重新申请"));
                return false;
            }
        }
        return true;
    }

    private boolean validEventContext(MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command,
                                      MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                      MesProProcessPoolEventDO event) {
        return event != null && event.getId() != null
                && MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())
                && Objects.equals(command.getWorkOrder().getId(), event.getWorkOrderId())
                && Objects.equals(snapshot.getRouteId(), event.getRouteId())
                && Objects.equals(snapshot.getRouteProcessId(), event.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), event.getProcessId());
    }

    private boolean validEventSignature(MesProProcessPoolEventDO event) {
        return event.getActualEmployeeId() != null && event.getSignatureId() != null
                && Objects.equals(event.getActualEmployeeId(), event.getSignatureUserId())
                && event.getServerSubmitTime() != null && StrUtil.isNotBlank(event.getSignatureSnapshot())
                && StrUtil.isNotBlank(event.getRawPayload());
    }

    private boolean validReview(Map<Long, MesProProcessPoolEventDO> eventById,
                                MesProcessPoolSubmissionReviewDO review) {
        return review != null && review.getId() != null && eventById.containsKey(review.getEventId())
                && LEADER_TYPE_PRODUCTION.equals(review.getLeaderType())
                && MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(review.getReviewStatus())
                && review.getLeaderUserId() != null && review.getReviewSignatureId() != null
                && Objects.equals(review.getLeaderUserId(), review.getReviewSignatureUserId())
                && review.getReviewedAt() != null && StrUtil.isNotBlank(review.getReviewSignatureSnapshotJson());
    }

    private boolean validAllocation(MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command,
                                    MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                    MesProcessPoolReportAllocationDO allocation,
                                    MesProProcessPoolEventDO event,
                                    MesProcessPoolSubmissionReviewDO review) {
        return allocation != null && allocation.getId() != null && event != null && review != null
                && Objects.equals(command.getActiveOrderId(), allocation.getActiveOrderId())
                && Objects.equals(command.getWorkOrder().getId(), allocation.getWorkOrderId())
                && Objects.equals(snapshot.getRouteProcessId(), allocation.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), allocation.getProcessId())
                && Objects.equals(event.getId(), allocation.getEventId())
                && Objects.equals(review.getId(), allocation.getReviewId())
                && Objects.equals(review.getLeaderUserId(), allocation.getLeaderUserId())
                && allocation.getAllocatedQuantity() != null && allocation.getAllocatedQuantity().signum() > 0
                && allocation.getConfirmedAt() != null
                && Objects.equals(review.getReviewedAt(), allocation.getConfirmedAt());
    }

    private boolean validateRuleSourceValues(List<MesProProcessPoolEventDO> events,
                                             List<MesProcessPoolReportAllocationDO> allocations,
                                             List<MesProBatchRecordCellLinkRuleDO> rules) {
        if (rules.isEmpty()) {
            return false;
        }
        Map<Long, JsonNode> payloads = new LinkedHashMap<>();
        try {
            for (MesProProcessPoolEventDO event : events) {
                payloads.put(event.getId(), JsonUtils.getObjectMapper().readTree(event.getRawPayload()));
            }
        } catch (Exception ex) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "生产提交 JSON 无法解析，禁止生成批记录");
        }
        for (MesProBatchRecordCellLinkRuleDO rule : rules) {
            if (allocations.size() > 1 && StrUtil.isBlank(rule.getAggregationStrategy())) {
                return false;
            }
            if ("allocatedQuantity".equals(rule.getSourceFieldCode())) {
                if (allocations.stream().anyMatch(item -> item.getAllocatedQuantity() == null)) {
                    return false;
                }
                continue;
            }
            for (MesProProcessPoolEventDO event : events) {
                JsonNode value = payloads.get(event.getId()).get(rule.getSourceFieldCode());
                if (value == null || value.isNull() || value.isContainerNode()
                        || value.isTextual() && StrUtil.isBlank(value.asText())) {
                    return false;
                }
            }
        }
        return true;
    }

    private MesProEdhrBatchExecutionTaskDO requireCurrentBatchTask(
            Long batchExecutionId,
            List<MesProEdhrBatchExecutionTaskDO> tasks,
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess prepared) {
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = prepared.getSource().getSnapshot();
        MesProRouteFlowProcessBatchRecordDO binding = prepared.getBinding();
        List<MesProEdhrBatchExecutionTaskDO> matches = tasks == null ? List.of() : tasks.stream()
                .filter(task -> task != null && task.getId() != null
                        && Objects.equals(batchExecutionId, task.getBatchExecutionId())
                        && Objects.equals(snapshot.getRouteProcessId(), task.getRouteProcessId())
                        && Objects.equals(snapshot.getProcessId(), task.getProcessId())
                        && Objects.equals(binding.getBatchRecordReportId(), task.getBatchRecordReportId())
                        && Objects.equals(binding.getBatchRecordDefinitionId(), task.getBatchRecordDefinitionId())
                        && Objects.equals(binding.getBatchRecordVersionId(), task.getBatchRecordVersionId())
                        && Objects.equals(binding.getId(), task.getRouteBindingId())
                        && RECORD_CATEGORY_BATCH_RECORD.equals(task.getRecordCategory())
                        && !PROCESS_INSPECTION.equals(task.getFormSlotType())
                        && !LOSS_REPORT.equals(task.getFormSlotType()))
                .toList();
        if (matches.size() != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "当前 eDHR 批次缺少唯一正式批记录任务，batchExecutionId=" + batchExecutionId
                            + "，routeProcessId=" + snapshot.getRouteProcessId()
                            + "，reportId=" + binding.getBatchRecordReportId());
        }
        return matches.get(0);
    }

    private List<Long> parseIds(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        List<Long> ids = JsonUtils.parseArray(json, Long.class);
        if (ids == null || ids.stream().anyMatch(Objects::isNull)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "生产完成来源 ID 快照无法解析");
        }
        return ids;
    }

    private List<Long> sortedIds(List<Long> ids) {
        return ids.stream().sorted().toList();
    }

    private List<MesProProcessPoolEventDO> orderedEvents(List<MesProProcessPoolEventDO> events) {
        return events.stream().sorted(Comparator.comparing(MesProProcessPoolEventDO::getId)).toList();
    }

    private List<MesProcessPoolReportAllocationDO> orderedAllocations(
            List<MesProcessPoolReportAllocationDO> allocations) {
        return allocations.stream().sorted(Comparator.comparing(MesProcessPoolReportAllocationDO::getId)).toList();
    }

    private MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult.SignatureEvidence signature(
            String role, String sourceType, Long sourceId, Long signatureId, Long userId,
            LocalDateTime signedAt, String evidenceHash) {
        return new MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult.SignatureEvidence()
                .setRole(role)
                .setSourceType(sourceType)
                .setSourceId(sourceId)
                .setSignatureId(signatureId)
                .setUserId(userId)
                .setSignedAt(signedAt)
                .setEvidenceHash(evidenceHash);
    }

    private MesTeamLeaderActiveOrderReleaseBlocker blocker(
            String type, String objectType, Object objectId, String reason, String suggestion) {
        return new MesTeamLeaderActiveOrderReleaseBlocker()
                .setBlockerType(type)
                .setObjectType(objectType)
                .setObjectId(objectId == null ? null : String.valueOf(objectId))
                .setReason(reason)
                .setSuggestion(suggestion);
    }

    private String hashEvent(MesProProcessPoolEventDO event) {
        return sha256(String.join("|", "PRODUCTION_EVENT_V1", value(event.getId()), value(event.getEventType()),
                value(event.getWorkOrderId()), value(event.getRouteId()), value(event.getRouteProcessId()),
                value(event.getProcessId()), value(event.getActualEmployeeId()), value(event.getServerSubmitTime()),
                value(event.getSignatureId()), value(event.getSignatureUserId()), value(event.getSignatureSnapshot()),
                value(event.getRawPayload())));
    }

    private String hashAllocation(MesProcessPoolReportAllocationDO allocation) {
        return sha256(String.join("|", "PRODUCTION_ALLOCATION_V1", value(allocation.getId()),
                value(allocation.getEventId()), value(allocation.getReviewId()), value(allocation.getLeaderUserId()),
                value(allocation.getActiveOrderId()), value(allocation.getWorkOrderId()),
                value(allocation.getRouteProcessId()), value(allocation.getProcessId()),
                value(allocation.getAllocatedQuantity()), value(allocation.getConfirmedAt())));
    }

    private String hashReview(MesProcessPoolSubmissionReviewDO review) {
        return sha256(String.join("|", "PRODUCTION_REVIEW_V1", value(review.getId()), value(review.getEventId()),
                value(review.getLeaderUserId()), value(review.getLeaderType()), value(review.getReviewStatus()),
                value(review.getReviewedAt()), value(review.getReviewSignatureId()),
                value(review.getReviewSignatureUserId()), value(review.getReviewSignatureSnapshotJson())));
    }

    private String hashCompletion(MesProcessPoolOrderProcessCompletionDO completion) {
        return sha256(String.join("|", "PRODUCTION_COMPLETION_V1", value(completion.getId()),
                value(completion.getWorkOrderId()), value(completion.getRouteProcessId()),
                value(completion.getProcessId()), value(completion.getTargetQuantity()),
                value(completion.getConfirmedQuantity()), value(completion.getCompletionStatus()),
                value(completion.getBackfillStatus()), value(completion.getBackfillExecutionId()),
                value(completion.getSourceEventIdsJson()), value(completion.getSourceAllocationIdsJson()),
                value(completion.getAggregateHash()), value(completion.getBackfillIdempotencyKey())));
    }

    private String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private <T> List<T> distinct(List<T> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }
}
