package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditChange;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditValueType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

@Service
public class MesTeamLeaderActiveOrderReleaseLossReportWriterImpl
        implements MesTeamLeaderActiveOrderReleaseLossReportWriter {

    private static final String USE_TYPE_BATCH = "BATCH";
    private static final String FORM_SLOT_TYPE = "LOSS_REPORT";
    private static final String RECORD_CATEGORY = "INTERNAL_RECORD";
    private static final String VALIDATION_PROFILE = "INTERNAL_TRACE";
    private static final String OWNER_ROLE = "PRODUCTION";
    private static final Long LOSS_REPORT_FORM_TEMPLATE_ID = 25L;
    private static final String SOURCE_TYPE = "PRODUCTION_LOSS";
    private static final String SCOPE_TYPE_ROUTE_VERSION = "ROUTE_VERSION";
    private static final String FEEDBACK_SOURCE_TYPE = "MES_PRO_FEEDBACK";
    private static final String LEADER_TYPE_PRODUCTION = "PRODUCTION";
    private static final List<String> DYNAMIC_SUMMARY_FIELDS = List.of(
            "productLabel", "productSpec", "productionSummary", "lossDetailsSummary", "approvalSummary");
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> REQUIRED_FIELDS = List.of(
            "productId", "batchCode", "routeProcessId", "processId", "outputQuantity",
            "qualifiedQuantity", "lossQuantity", "laborScrapQuantity", "materialScrapQuantity",
            "otherScrapQuantity", "lossDetails", "fillerUserId", "fillerSignedAt",
            "reviewerUserId", "reviewerSignedAt");

    private final MesTeamLeaderActiveOrderReleaseLossSourceReader sourceReader;
    private final MesProRouteFlowProcessBatchRecordMapper bindingMapper;
    private final MesProBatchRecordCellLinkRuleMapper ruleMapper;
    private final MesProBatchRecordReportMapper reportMapper;
    private final MesProBatchRecordVersionMapper versionMapper;
    private final MesMdItemMapper itemMapper;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesProBatchRecordExecutionService executionService;
    private final MesProBatchRecordExecutionMapper executionMapper;
    private final MesProBatchRecordExecutionFieldAuditService fieldAuditService;
    private final MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort dynamicFormPort;

    public MesTeamLeaderActiveOrderReleaseLossReportWriterImpl(
            MesTeamLeaderActiveOrderReleaseLossSourceReader sourceReader,
            MesProRouteFlowProcessBatchRecordMapper bindingMapper,
            MesProBatchRecordCellLinkRuleMapper ruleMapper,
            MesProBatchRecordReportMapper reportMapper,
            MesProBatchRecordVersionMapper versionMapper,
            MesMdItemMapper itemMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesProBatchRecordExecutionService executionService,
            MesProBatchRecordExecutionMapper executionMapper,
            MesProBatchRecordExecutionFieldAuditService fieldAuditService,
            MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort dynamicFormPort) {
        this.sourceReader = sourceReader;
        this.bindingMapper = bindingMapper;
        this.ruleMapper = ruleMapper;
        this.reportMapper = reportMapper;
        this.versionMapper = versionMapper;
        this.itemMapper = itemMapper;
        this.batchTaskMapper = batchTaskMapper;
        this.executionService = executionService;
        this.executionMapper = executionMapper;
        this.fieldAuditService = fieldAuditService;
        this.dynamicFormPort = dynamicFormPort;
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseLossReportPlan plan(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command) {
        validateCommand(command);
        MesTeamLeaderActiveOrderReleaseLossSourceReadResult readResult = sourceReader.read(command);
        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = new ArrayList<>();
        List<MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport> prepared = new ArrayList<>();
        LinkedHashSet<Long> sourceObjectIds = new LinkedHashSet<>();
        LinkedHashSet<String> sourceValueHashes = new LinkedHashSet<>();
        List<MesTeamLeaderActiveOrderReleaseSignatureEvidence> signatures = new ArrayList<>();
        if (readResult == null) {
            blockers.add(blocker("LOSS_SOURCE_REQUIRED", null, null, "ACTIVE_ORDER", command.getActiveOrderId(),
                    null, null, "正式损耗来源 reader 未返回结果", "请修复正式损耗来源链路"));
            return planResult(command, prepared, sourceObjectIds, sourceValueHashes, signatures, blockers);
        }
        if (readResult.getBlockers() != null) {
            blockers.addAll(readResult.getBlockers());
        }
        List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource> sources =
                readResult.getProcessSources() == null ? List.of() : readResult.getProcessSources().stream()
                        .filter(Objects::nonNull)
                        .sorted(sourceComparator())
                        .toList();
        if (sources.isEmpty() && blockers.isEmpty()) {
            blockers.add(blocker("LOSS_SOURCE_REQUIRED", null, null, "ACTIVE_ORDER", command.getActiveOrderId(),
                    null, null, "当前激活批次没有正式生产损耗来源", "请完成生产反馈和生产组长复核"));
        }
        if (!blockers.isEmpty()) {
            return planResult(command, prepared, sourceObjectIds, sourceValueHashes, signatures, blockers);
        }

        Map<ProcessKey, List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource>> grouped =
                sources.stream().collect(Collectors.groupingBy(source -> new ProcessKey(
                                source.getSnapshot() == null ? null : source.getSnapshot().getRouteProcessId(),
                                source.getSnapshot() == null ? null : source.getSnapshot().getProcessId()),
                        LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<ProcessKey, List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource>> entry
                : grouped.entrySet()) {
            int blockerCount = blockers.size();
            for (MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource source : entry.getValue()) {
                validateFormalSource(command, source, blockers);
            }
            if (blockers.size() != blockerCount) {
                continue;
            }
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot = entry.getValue().get(0).getSnapshot();
            MesProRouteFlowProcessBatchRecordDO binding = formalBinding(snapshot, blockers);
            boolean dynamic = isDynamicBinding(binding);
            TargetMetadata target = binding == null ? null : (dynamic
                    ? dynamicTargetMetadata(binding) : formalTarget(command, snapshot, binding, blockers));
            MappingResolution mapping = target == null ? null
                    : formalMappings(command, snapshot, binding, entry.getValue(), blockers);
            if (mapping == null || blockers.size() != blockerCount) {
                continue;
            }
            List<String> targetSnapshotHashes = new ArrayList<>(target.snapshotHashes());
            if (mapping.dynamicTarget() != null) {
                targetSnapshotHashes.add(mapping.dynamicTarget().getTemplateSnapshotHash());
            }
            List<String> evidenceHashes = collectEvidence(entry.getValue(), binding, target, mapping.rules(),
                    mapping.dynamicTarget(), sourceObjectIds, sourceValueHashes, signatures);
            for (MesProBatchRecordCellLinkRuleDO rule : mapping.rules()) {
                Object value = mapping.values().get(rule.getSourceFieldCode());
                if (value != null) {
                    String hash = hashMappedValue(rule, value);
                    evidenceHashes.add(hash);
                    sourceValueHashes.add(hash);
                }
            }
            prepared.add(new MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport()
                    .setSources(List.copyOf(entry.getValue()))
                    .setBinding(binding)
                    .setRules(mapping.rules())
                    .setDynamicTarget(mapping.dynamicTarget())
                    .setMappedValues(mapping.values())
                    .setTargetSnapshotHashes(List.copyOf(targetSnapshotHashes))
                    .setEvidenceHash(sha256(String.join("|", evidenceHashes))));
        }
        prepared.sort(Comparator.comparing(item -> item.getSources().get(0).getSnapshot().getRouteProcessId()));
        return planResult(command, prepared, sourceObjectIds, sourceValueHashes, signatures, blockers);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseLossReportWriteResult write(
            MesTeamLeaderActiveOrderReleaseLossReportPlan plan, Long batchExecutionId) {
        validateWriteInput(plan, batchExecutionId);
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(batchExecutionId);
        List<Long> executionIds = new ArrayList<>();
        List<Long> formCenterInstanceIds = new ArrayList<>();
        List<Long> auditIds = new ArrayList<>();
        List<String> auditHeadHashes = new ArrayList<>();
        List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> documents = new ArrayList<>();
        for (MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport prepared
                : plan.getPreparedReports()) {
            MesProEdhrBatchExecutionTaskDO task = requireCurrentBatchTask(batchExecutionId, tasks, prepared);
            if (isDynamicBinding(prepared.getBinding())) {
                MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.WriteResult dynamicWrite =
                        dynamicFormPort.write(toDynamicWriteCommand(plan, batchExecutionId, task, prepared));
                if (dynamicWrite == null || dynamicWrite.getFormCenterInstanceId() == null
                        || dynamicWrite.getFieldAuditSnapshotId() == null
                        || StrUtil.isBlank(dynamicWrite.getFieldAuditHeadHash())
                        || !"EFFECTIVE".equals(dynamicWrite.getEffectiveStatus())) {
                    throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                            "损耗报告动态 writer 未返回 EFFECTIVE FormCenter instance 与审计快照");
                }
                formCenterInstanceIds.add(dynamicWrite.getFormCenterInstanceId());
                auditIds.add(dynamicWrite.getFieldAuditSnapshotId());
                auditHeadHashes.add(dynamicWrite.getFieldAuditHeadHash());
                documents.add(new MesTeamLeaderActiveOrderReleaseDocumentEvidence()
                        .setDocumentType(FORM_SLOT_TYPE)
                        .setBatchExecutionId(batchExecutionId)
                        .setBatchExecutionTaskId(task.getId())
                        .setBatchRecordExecutionIds(List.of())
                        .setFormCenterInstanceIds(List.of(dynamicWrite.getFormCenterInstanceId()))
                        .setTargetReportIds(List.of("FORMTPL:" + prepared.getDynamicTarget().getTemplateVersionId()))
                        .setTargetFormTemplateIds(List.of(prepared.getBinding().getFormTemplateId()))
                        .setTargetDefinitionIds(List.of())
                        .setTargetVersionIds(List.of(prepared.getDynamicTarget().getTemplateVersionId()))
                        .setTargetSnapshotHashes(prepared.getTargetSnapshotHashes())
                        .setFieldAuditIds(List.of(dynamicWrite.getFieldAuditSnapshotId()))
                        .setRequiredFieldCount(prepared.getRules().size())
                        .setAuditedRequiredFieldCount(prepared.getRules().size())
                        .setSourceObjectIds(plan.getSourceObjectIds())
                        .setSourceValueHashes(plan.getSourceValueHashes())
                        .setSignatureEvidence(plan.getSignatureEvidence())
                        .setSourceSnapshotHash(plan.getCommand().getSourceSnapshotHash())
                        .setSourceConsistent(true));
                continue;
            }
            MesProBatchRecordExecutionOpenOrCreateByContextRespVO opened = executionService.openOrCreateByContext(
                    toOpenRequest(plan.getCommand(), batchExecutionId, task));
            if (opened == null || opened.getId() == null) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                        "损耗报告 writer 未创建当前批次传统 execution");
            }
            MesProBatchRecordExecutionDO execution = executionMapper.selectById(opened.getId());
            validateExecutionContext(plan.getCommand(), batchExecutionId, task, execution);
            List<MesProBatchRecordExecutionFieldAuditChange> changes = toAuditChanges(execution, prepared);
            MesProBatchRecordExecutionFieldAuditSaveResult audit = fieldAuditService.saveSystemCellLinkChanges(
                    new MesProBatchRecordExecutionFieldAuditSaveChangesCommand()
                            .setExecutionId(execution.getId())
                            .setIdempotencyKey(auditIdempotencyKey(plan.getCommand(), task, prepared))
                            .setBaseCellValuesHash(execution.getCellValuesHash())
                            .setBaseFieldAuditRevision(execution.getFieldAuditRevision())
                            .setBaseFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                            .setReasonCategory("OTHER")
                            .setReasonText(auditReason(prepared))
                            .setChanges(changes));
            if (audit == null || audit.getAuditBatchId() == null
                    || StrUtil.isBlank(audit.getFieldAuditHeadHash())
                    || StrUtil.isBlank(audit.getCellValuesHash())) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                        "损耗报告 writer 未返回字段审计证据，batchTaskId=" + task.getId());
            }
            executionIds.add(execution.getId());
            auditIds.add(audit.getAuditBatchId());
            auditHeadHashes.add(audit.getFieldAuditHeadHash());
            documents.add(new MesTeamLeaderActiveOrderReleaseDocumentEvidence()
                    .setDocumentType(FORM_SLOT_TYPE)
                    .setBatchExecutionId(batchExecutionId)
                    .setBatchExecutionTaskId(task.getId())
                    .setBatchRecordExecutionIds(List.of(execution.getId()))
                    .setTargetReportIds(List.of(task.getBatchRecordReportId()))
                    .setTargetDefinitionIds(List.of(task.getBatchRecordDefinitionId()))
                    .setTargetVersionIds(List.of(task.getBatchRecordVersionId()))
                    .setTargetSnapshotHashes(prepared.getTargetSnapshotHashes())
                    .setFieldAuditIds(List.of(audit.getAuditBatchId()))
                    .setRequiredFieldCount(prepared.getRules().size())
                    .setAuditedRequiredFieldCount(changes.size())
                    .setSourceObjectIds(plan.getSourceObjectIds())
                    .setSourceValueHashes(plan.getSourceValueHashes())
                    .setSignatureEvidence(plan.getSignatureEvidence())
                    .setSourceSnapshotHash(plan.getCommand().getSourceSnapshotHash())
                    .setSourceConsistent(true));
        }
        return new MesTeamLeaderActiveOrderReleaseLossReportWriteResult()
                .setDocumentType(FORM_SLOT_TYPE)
                .setBatchRecordExecutionIds(distinct(executionIds))
                .setFormCenterInstanceIds(distinct(formCenterInstanceIds))
                .setFieldAuditIds(distinct(auditIds))
                .setFieldAuditHeadHashes(distinct(auditHeadHashes))
                .setSourceObjectIds(plan.getSourceObjectIds())
                .setSourceValueHashes(plan.getSourceValueHashes())
                .setSignatureEvidence(plan.getSignatureEvidence())
                .setDocumentEvidence(List.copyOf(documents))
                .setBlockers(List.of());
    }

    private MesTeamLeaderActiveOrderReleaseLossReportPlan planResult(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            List<MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport> prepared,
            Set<Long> sourceObjectIds,
            Set<String> sourceValueHashes,
            List<MesTeamLeaderActiveOrderReleaseSignatureEvidence> signatures,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        return new MesTeamLeaderActiveOrderReleaseLossReportPlan()
                .setCommand(command)
                .setPreparedReports(List.copyOf(prepared))
                .setSourceObjectIds(List.copyOf(sourceObjectIds))
                .setSourceValueHashes(List.copyOf(sourceValueHashes))
                .setSignatureEvidence(List.copyOf(signatures))
                .setBlockers(List.copyOf(blockers));
    }

    private void validateCommand(MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command) {
        if (command == null || command.getTenantId() == null || command.getTenantId() <= 0
                || command.getActiveOrderId() == null || command.getActiveOrderId() <= 0
                || command.getWorkOrderId() == null || command.getWorkOrderId() <= 0
                || command.getRouteId() == null || command.getRouteId() <= 0
                || command.getRouteVersionId() == null || command.getRouteVersionId() <= 0
                || command.getProductId() == null || command.getProductId() <= 0
                || StrUtil.isBlank(command.getBatchCode()) || StrUtil.isBlank(command.getSourceSnapshotHash())
                || command.getProcessSnapshots() == null || command.getProcessSnapshots().isEmpty()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseLossReportPlan");
        }
    }

    private void validateWriteInput(MesTeamLeaderActiveOrderReleaseLossReportPlan plan, Long batchExecutionId) {
        if (plan == null || plan.getCommand() == null || plan.getPreparedReports() == null
                || plan.getBlockers() == null || batchExecutionId == null || batchExecutionId <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseLossReportWrite");
        }
        if (!plan.getBlockers().isEmpty() || plan.getPreparedReports().isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "损耗报告 writer plan 存在 blocker 或没有正式写入计划，禁止写入");
        }
    }

    private void validateFormalSource(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource source,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = source.getSnapshot();
        MesProFeedbackDO feedback = source.getFeedback();
        MesProProcessPoolEventDO event = source.getEvent();
        MesProcessPoolReportAllocationDO allocation = source.getAllocation();
        MesProcessPoolSubmissionReviewDO review = source.getReview();
        if (!validIdentity(command, snapshot, feedback, event, allocation, review)) {
            blockers.add(blocker("LOSS_SOURCE_REQUIRED", snapshot, null, "PRODUCTION_FEEDBACK",
                    feedback == null ? null : feedback.getId(), null, null,
                    "损耗来源与当前活跃订单、工单、路线、工序或正式反馈闭环不一致",
                    "请修复当前批次生产反馈追溯"));
            return;
        }
        if (!validSignatures(feedback, event, allocation, review)) {
            blockers.add(blocker("PRODUCTION_SIGNATURE_REQUIRED", snapshot, null, "PRODUCTION_REVIEW",
                    review.getId(), null, null,
                    "生产反馈填写签名或生产组长 APPROVED 复核签名不完整",
                    "请由正式填写人和生产组长完成电子签名"));
            return;
        }
        if (!validNonNegativeQuantities(feedback)
                || !decimalEquals(feedback.getFeedbackQuantity(),
                feedback.getQualifiedQuantity().add(feedback.getUnqualifiedQuantity()))
                || !decimalEquals(feedback.getFeedbackQuantity(), allocation.getAllocatedQuantity())) {
            blockers.add(blocker("LOSS_SOURCE_REQUIRED", snapshot, null, "PRODUCTION_FEEDBACK", feedback.getId(),
                    "lossQuantity", null,
                    "报工总量、合格量、损耗总量和当前活跃订单分配数量不一致",
                    "请修复正式生产反馈数量后重新申请"));
            return;
        }
        BigDecimal categoryTotal = feedback.getLaborScrapQuantity()
                .add(feedback.getMaterialScrapQuantity()).add(feedback.getOtherScrapQuantity());
        if (!decimalEquals(feedback.getUnqualifiedQuantity(), categoryTotal)) {
            blockers.add(blocker("LOSS_SOURCE_REQUIRED", snapshot, null, "PRODUCTION_FEEDBACK", feedback.getId(),
                    "lossQuantity", null, "损耗分类数量之和与损耗总量不一致",
                    "请修复人工、物料和其它损耗分类数量"));
            return;
        }
        if (feedback.getUnqualifiedQuantity().signum() == 0) {
            blockers.add(blocker("ZERO_LOSS_CONFIRMATION_UNSUPPORTED", snapshot, null,
                    "PRODUCTION_FEEDBACK", feedback.getId(), "lossQuantity", null,
                    "当前正式损耗模板没有可证明的零损耗确认字段",
                    "请先补齐正式零损耗确认字段及映射"));
            return;
        }
        List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail> details = source.getLossDetails();
        boolean detailShapeValid = details != null && !details.isEmpty()
                && details.stream().allMatch(detail -> detail != null && detail.getReasonId() != null
                && StrUtil.isNotBlank(detail.getReasonCode()) && StrUtil.isNotBlank(detail.getReasonName())
                && detail.getQuantity() != null && detail.getQuantity().signum() > 0);
        BigDecimal detailTotal = detailShapeValid ? details.stream()
                .map(MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.valueOf(-1);
        boolean allReasonsMatched = detailShapeValid && details.stream().allMatch(detail ->
                Objects.equals(feedback.getLossReasonId(), detail.getReasonId())
                        && Objects.equals(feedback.getLossReasonCodeSnapshot(), detail.getReasonCode())
                        && Objects.equals(feedback.getLossReasonNameSnapshot(), detail.getReasonName()));
        if (!detailShapeValid || !decimalEquals(feedback.getUnqualifiedQuantity(), detailTotal)
                || !allReasonsMatched) {
            blockers.add(blocker("LOSS_SOURCE_REQUIRED", snapshot, null, "PRODUCTION_EVENT", event.getId(),
                    "lossDetails", null,
                    "结构化损耗明细、损耗总量或反馈原因快照不一致",
                    "请通过正式生产反馈重新保存结构化 lossDetails"));
        }
    }

    private boolean validIdentity(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            MesProFeedbackDO feedback,
            MesProProcessPoolEventDO event,
            MesProcessPoolReportAllocationDO allocation,
            MesProcessPoolSubmissionReviewDO review) {
        return snapshot != null && snapshot.getRouteProcessId() != null && snapshot.getProcessId() != null
                && Objects.equals(command.getActiveOrderId(), snapshot.getActiveOrderId())
                && Objects.equals(command.getWorkOrderId(), snapshot.getWorkOrderId())
                && Objects.equals(command.getRouteId(), snapshot.getRouteId())
                && Objects.equals(command.getRouteVersionId(), snapshot.getRouteVersionId())
                && feedback != null && feedback.getId() != null
                && Objects.equals(command.getWorkOrderId(), feedback.getWorkOrderId())
                && Objects.equals(command.getRouteId(), feedback.getRouteId())
                && Objects.equals(snapshot.getProcessId(), feedback.getProcessId())
                && event != null && event.getId() != null
                && MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())
                && FEEDBACK_SOURCE_TYPE.equals(event.getFeedbackSourceType())
                && Objects.equals(feedback.getId(), event.getFeedbackSourceId())
                && Objects.equals(command.getWorkOrderId(), event.getWorkOrderId())
                && Objects.equals(command.getRouteId(), event.getRouteId())
                && Objects.equals(snapshot.getRouteProcessId(), event.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), event.getProcessId())
                && allocation != null && allocation.getId() != null
                && Objects.equals(event.getId(), allocation.getEventId())
                && Objects.equals(command.getActiveOrderId(), allocation.getActiveOrderId())
                && Objects.equals(command.getWorkOrderId(), allocation.getWorkOrderId())
                && Objects.equals(snapshot.getRouteProcessId(), allocation.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), allocation.getProcessId())
                && review != null && review.getId() != null
                && Objects.equals(event.getId(), review.getEventId())
                && Objects.equals(allocation.getReviewId(), review.getId());
    }

    private boolean validSignatures(MesProFeedbackDO feedback,
                                    MesProProcessPoolEventDO event,
                                    MesProcessPoolReportAllocationDO allocation,
                                    MesProcessPoolSubmissionReviewDO review) {
        return event.getActualEmployeeId() != null && event.getSignatureId() != null
                && event.getSignatureUserId() != null && event.getServerSubmitTime() != null
                && StrUtil.isNotBlank(event.getSignatureSnapshot())
                && Objects.equals(event.getActualEmployeeId(), event.getSignatureUserId())
                && Objects.equals(feedback.getFeedbackUserId(), event.getSignatureUserId())
                && LEADER_TYPE_PRODUCTION.equals(review.getLeaderType())
                && MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(review.getReviewStatus())
                && review.getLeaderUserId() != null && review.getReviewSignatureId() != null
                && review.getReviewSignatureUserId() != null && review.getReviewedAt() != null
                && StrUtil.isNotBlank(review.getReviewSignatureSnapshotJson())
                && Objects.equals(review.getLeaderUserId(), review.getReviewSignatureUserId())
                && Objects.equals(feedback.getApproveUserId(), review.getReviewSignatureUserId())
                && Objects.equals(allocation.getLeaderUserId(), review.getLeaderUserId())
                && Objects.equals(allocation.getConfirmedAt(), review.getReviewedAt());
    }

    private boolean validNonNegativeQuantities(MesProFeedbackDO feedback) {
        return nonNegative(feedback.getFeedbackQuantity()) && feedback.getFeedbackQuantity().signum() > 0
                && nonNegative(feedback.getQualifiedQuantity())
                && nonNegative(feedback.getUnqualifiedQuantity())
                && nonNegative(feedback.getLaborScrapQuantity())
                && nonNegative(feedback.getMaterialScrapQuantity())
                && nonNegative(feedback.getOtherScrapQuantity());
    }

    private MesProRouteFlowProcessBatchRecordDO formalBinding(
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        List<MesProRouteFlowProcessBatchRecordDO> bindings = bindingMapper
                .selectListByRouteProcessIdsAndUseType(List.of(snapshot.getRouteProcessId()), USE_TYPE_BATCH);
        List<MesProRouteFlowProcessBatchRecordDO> formal = bindings == null ? List.of() : bindings.stream()
                .filter(Objects::nonNull)
                .filter(binding -> binding.getId() != null
                        && Objects.equals(snapshot.getRouteId(), binding.getRouteId())
                        && Objects.equals(snapshot.getRouteProcessId(), binding.getRouteProcessId())
                        && USE_TYPE_BATCH.equals(binding.getUseType())
                        && StrUtil.isNotBlank(binding.getBatchRecordReportId())
                        && binding.getBatchRecordDefinitionId() != null
                        && binding.getBatchRecordVersionId() != null
                        && FORM_SLOT_TYPE.equals(binding.getFormSlotType())
                        && RECORD_CATEGORY.equals(binding.getRecordCategory())
                        && VALIDATION_PROFILE.equals(binding.getValidationProfile())
                        && OWNER_ROLE.equals(binding.getOwnerRoleKey())
                        && binding.getPermissionScopeId() != null
                        && StrUtil.isNotBlank(binding.getRecordCategorySnapshotHash())
                        && StrUtil.isNotBlank(binding.getSlotConfigSnapshotHash()))
                .toList();
        List<MesProRouteFlowProcessBatchRecordDO> dynamicFormal = bindings == null ? List.of() : bindings.stream()
                .filter(Objects::nonNull)
                .filter(binding -> binding.getId() != null
                        && Objects.equals(snapshot.getRouteId(), binding.getRouteId())
                        && Objects.equals(snapshot.getRouteProcessId(), binding.getRouteProcessId())
                        && USE_TYPE_BATCH.equals(binding.getUseType())
                        && StrUtil.isBlank(binding.getBatchRecordReportId())
                        && binding.getBatchRecordDefinitionId() == null
                        && binding.getBatchRecordVersionId() == null
                        && FORM_SLOT_TYPE.equals(binding.getFormSlotType())
                        && RECORD_CATEGORY.equals(binding.getRecordCategory())
                        && VALIDATION_PROFILE.equals(binding.getValidationProfile())
                        && OWNER_ROLE.equals(binding.getOwnerRoleKey())
                        && LOSS_REPORT_FORM_TEMPLATE_ID.equals(binding.getFormTemplateId())
                        && StrUtil.isNotBlank(binding.getFormBindingKey())
                        && binding.getLastPublishedTemplateVersionId() != null
                        && StrUtil.isNotBlank(binding.getLastPublishedTemplateVersionNo())
                        && binding.getPermissionScopeId() != null
                        && StrUtil.isNotBlank(binding.getRecordCategorySnapshotHash())
                        && StrUtil.isNotBlank(binding.getSlotConfigSnapshotHash()))
                .toList();
        if (formal.isEmpty() && dynamicFormal.size() == 1) {
            return dynamicFormal.get(0);
        }
        if (formal.size() + dynamicFormal.size() != 1) {
            blockers.add(blocker("LOSS_REPORT_BINDING_REQUIRED", snapshot, null, "ROUTE_PROCESS",
                    snapshot.getRouteProcessId(), null, null,
                    "工序必须存在唯一有效 LOSS_REPORT 目标绑定，传统数量=" + formal.size()
                            + "，动态数量=" + dynamicFormal.size(),
                    "请维护唯一的传统损耗报表绑定或 template 25 已发布动态表单绑定"));
            return null;
        }
        return formal.get(0);
    }

    private TargetMetadata dynamicTargetMetadata(MesProRouteFlowProcessBatchRecordDO binding) {
        return new TargetMetadata(null, null, List.of(binding.getRecordCategorySnapshotHash(),
                binding.getSlotConfigSnapshotHash()));
    }

    private TargetMetadata formalTarget(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            MesProRouteFlowProcessBatchRecordDO binding,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        MesProBatchRecordReportDO report = reportMapper.selectByReportId(binding.getBatchRecordReportId());
        MesProBatchRecordVersionDO version = versionMapper.selectById(binding.getBatchRecordVersionId());
        boolean valid = report != null && report.getId() != null
                && Objects.equals(binding.getBatchRecordReportId(), report.getReportId())
                && Objects.equals(binding.getBatchRecordDefinitionId(), report.getBatchRecordDefinitionId())
                && Objects.equals(binding.getBatchRecordVersionId(), report.getBatchRecordVersionId())
                && FORM_SLOT_TYPE.equals(report.getFormSlotType())
                && StrUtil.isNotBlank(report.getSourceFileSha256())
                && version != null && version.getId() != null
                && Objects.equals(binding.getBatchRecordVersionId(), version.getId())
                && Objects.equals(binding.getBatchRecordDefinitionId(), version.getDefinitionId())
                && "APPROVED".equals(version.getStatus())
                && Objects.equals(command.getRouteId(), version.getRouteId())
                && Objects.equals(report.getSourceFileSha256(), version.getSourceFileSha256());
        if (!valid) {
            blockers.add(blocker("LOSS_REPORT_BINDING_REQUIRED", snapshot, null, "LOSS_REPORT",
                    binding.getBatchRecordReportId(), null, null,
                    "LOSS_REPORT 绑定未指向当前路线已批准且模板哈希一致的传统报表版本",
                    "请修复正式损耗报告元数据和批准版本"));
            return null;
        }
        List<String> snapshotHashes = List.of(binding.getRecordCategorySnapshotHash(),
                binding.getSlotConfigSnapshotHash(), report.getSourceFileSha256());
        return new TargetMetadata(report, version, snapshotHashes);
    }

    private MappingResolution formalMappings(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            MesProRouteFlowProcessBatchRecordDO binding,
            List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource> sources,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        boolean dynamic = isDynamicBinding(binding);
        String scopeType = dynamic ? "FORM_TEMPLATE_VERSION" : SCOPE_TYPE_ROUTE_VERSION;
        Long scopeId = dynamic ? binding.getLastPublishedTemplateVersionId() : binding.getBatchRecordVersionId();
        String targetReportId = dynamic ? "FORMTPL:" + binding.getLastPublishedTemplateVersionId()
                : binding.getBatchRecordReportId();
        List<MesProBatchRecordCellLinkRuleDO> rawRules = ruleMapper.selectEnabledListByScopeAndTargetReport(
                scopeType, scopeId, targetReportId);
        List<MesProBatchRecordCellLinkRuleDO> rules = rawRules == null ? List.of() : rawRules.stream()
                .filter(Objects::nonNull)
                .filter(rule -> !dynamic || SOURCE_TYPE.equals(StrUtil.trim(rule.getSourceType())))
                .sorted(Comparator.comparing(MesProBatchRecordCellLinkRuleDO::getTargetRowIndex,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesProBatchRecordCellLinkRuleDO::getTargetColumnIndex,
                                Comparator.nullsLast(Integer::compareTo)))
                .toList();
        MesMdItemDO product = dynamic ? formalProduct(command, snapshot, blockers) : null;
        if (dynamic && product == null) {
            return null;
        }
        List<String> requiredFields = dynamic ? DYNAMIC_SUMMARY_FIELDS : REQUIRED_FIELDS;
        Map<String, List<MesProBatchRecordCellLinkRuleDO>> byField = rules.stream()
                .filter(rule -> StrUtil.isNotBlank(rule.getSourceFieldCode()))
                .collect(Collectors.groupingBy(MesProBatchRecordCellLinkRuleDO::getSourceFieldCode,
                        LinkedHashMap::new, Collectors.toList()));
        for (String requiredField : requiredFields) {
            if (byField.getOrDefault(requiredField, List.of()).size() != 1) {
                blockers.add(blocker("LOSS_REPORT_MAPPING_REQUIRED", snapshot, null, "LOSS_REPORT",
                        targetReportId, requiredField, null,
                        "正式损耗报告必填字段缺少唯一 PRODUCTION_LOSS 映射",
                        "请为当前批准模板配置唯一必填字段映射"));
            }
        }
        Set<String> cells = new LinkedHashSet<>();
        for (MesProBatchRecordCellLinkRuleDO rule : rules) {
            String cell = cellKey(rule.getTargetRowIndex(), rule.getTargetColumnIndex());
            boolean valid = rule.getId() != null && Boolean.TRUE.equals(rule.getEnabled())
                    && scopeType.equals(rule.getScopeType())
                    && Objects.equals(scopeId, rule.getScopeId())
                    && SOURCE_TYPE.equals(StrUtil.trim(rule.getSourceType()))
                    && requiredFields.contains(rule.getSourceFieldCode())
                    && Objects.equals(targetReportId, rule.getTargetReportId())
                    && rule.getTargetRowIndex() != null && rule.getTargetColumnIndex() != null
                    && StrUtil.isNotBlank(rule.getTargetCellKey())
                    && StrUtil.isNotBlank(rule.getTargetValueType())
                    && (!dynamic || (rule.getRuleVersion() != null
                    && Objects.equals(summarySourceKey(rule.getSourceFieldCode()), rule.getSourceCellKey())
                    && StrUtil.isNotBlank(rule.getTemplateSnapshotHash())))
                    && cells.add(cell);
            if (!valid) {
                blockers.add(blocker("LOSS_REPORT_MAPPING_REQUIRED", snapshot, null, "LOSS_REPORT",
                        targetReportId, rule.getSourceFieldCode(), rule.getTargetCellKey(),
                        "损耗报告映射来源、目标、值类型或目标单元格不合法或重复",
                        "请修复当前批准模板的 PRODUCTION_LOSS 映射"));
            }
        }
        if (!blockers.isEmpty()) {
            return null;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        if (dynamic) {
            values.putAll(dynamicSummaryValues(command, snapshot, sources, product));
        } else {
            for (MesProBatchRecordCellLinkRuleDO rule : rules) {
                List<Object> sourceValues = sourceValues(rule.getSourceFieldCode(), command, sources);
                Object value = aggregate(rule, sourceValues, snapshot, binding, blockers);
                if (value != null) {
                    values.put(rule.getSourceFieldCode(), value);
                }
            }
        }
        if (!blockers.isEmpty()) {
            return null;
        }
        MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.TargetResolution dynamicTarget = null;
        if (dynamic) {
            dynamicTarget = dynamicFormPort.resolveTarget(binding, rules);
            if (dynamicTarget == null || !dynamicTarget.isValid()) {
                String blockerType = dynamicTarget == null || StrUtil.isBlank(dynamicTarget.getBlockerType())
                        ? "LOSS_REPORT_DYNAMIC_FORM_TEMPLATE_REQUIRED" : dynamicTarget.getBlockerType();
                String message = dynamicTarget == null || StrUtil.isBlank(dynamicTarget.getBlockerMessage())
                        ? "损耗单动态模板目标解析失败" : dynamicTarget.getBlockerMessage();
                blockers.add(blocker(blockerType, snapshot, null, "ROUTE_PROCESS_FORM_BINDING",
                        binding.getId(), binding.getFormBindingKey(), null, message,
                        "请配置精确的 PUBLISHED template 25 版本和稳定 fieldCode 映射"));
                return null;
            }
        }
        return new MappingResolution(List.copyOf(rules), Map.copyOf(values), dynamicTarget);
    }

    private List<Object> sourceValues(
            String field,
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource> sources) {
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = sources.get(0).getSnapshot();
        if ("productId".equals(field)) {
            return List.of(command.getProductId());
        }
        if ("batchCode".equals(field)) {
            return List.of(command.getBatchCode());
        }
        if ("routeProcessId".equals(field)) {
            return List.of(snapshot.getRouteProcessId());
        }
        if ("processId".equals(field)) {
            return List.of(snapshot.getProcessId());
        }
        if ("lossDetails".equals(field)) {
            return List.of(canonicalLossDetails(sources));
        }
        List<Object> values = new ArrayList<>();
        for (MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource source : sources) {
            MesProFeedbackDO feedback = source.getFeedback();
            MesProProcessPoolEventDO event = source.getEvent();
            MesProcessPoolSubmissionReviewDO review = source.getReview();
            values.add(switch (field) {
                case "outputQuantity" -> feedback.getFeedbackQuantity();
                case "qualifiedQuantity" -> feedback.getQualifiedQuantity();
                case "lossQuantity" -> feedback.getUnqualifiedQuantity();
                case "laborScrapQuantity" -> feedback.getLaborScrapQuantity();
                case "materialScrapQuantity" -> feedback.getMaterialScrapQuantity();
                case "otherScrapQuantity" -> feedback.getOtherScrapQuantity();
                case "fillerUserId" -> event.getSignatureUserId();
                case "fillerSignedAt" -> event.getServerSubmitTime();
                case "reviewerUserId" -> review.getReviewSignatureUserId();
                case "reviewerSignedAt" -> review.getReviewedAt();
                default -> null;
            });
        }
        return values;
    }

    private MesMdItemDO formalProduct(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        MesMdItemDO product = itemMapper.selectById(command.getProductId());
        if (product == null || product.getId() == null || !Objects.equals(command.getProductId(), product.getId())
                || !CommonStatusEnum.ENABLE.getStatus().equals(product.getStatus())
                || StrUtil.hasBlank(product.getCode(), product.getName(), product.getSpecification())) {
            blockers.add(blocker("LOSS_PRODUCT_IDENTITY_REQUIRED", snapshot, null, "PRODUCT",
                    command.getProductId(), "productLabel", null,
                    "损耗单摘要缺少正式启用产品主数据名称、编码或规格",
                    "请补齐 mes_md_item 正式产品名称/规格；禁止用 productId 作为名称 fallback"));
            return null;
        }
        return product;
    }

    private Map<String, Object> dynamicSummaryValues(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource> sources,
            MesMdItemDO product) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("productLabel", product.getName());
        values.put("productSpec", join(product.getCode(), product.getSpecification()));
        values.put("productionSummary", join("routeProcessId=" + snapshot.getRouteProcessId(),
                "processId=" + snapshot.getProcessId(), "output=" + decimalText(sum(sources, "outputQuantity")),
                "qualified=" + decimalText(sum(sources, "qualifiedQuantity"))));
        String lossSummary = join("loss=" + decimalText(sum(sources, "lossQuantity")),
                "labor=" + decimalText(sum(sources, "laborScrapQuantity")),
                "material=" + decimalText(sum(sources, "materialScrapQuantity")),
                "other=" + decimalText(sum(sources, "otherScrapQuantity")));
        String fillerSignedInfo = sources.stream()
                .map(source -> signatureSummary(source.getEvent().getId(), source.getEvent().getSignatureUserId(),
                        source.getEvent().getSignatureId(), source.getEvent().getServerSubmitTime()))
                .collect(Collectors.joining(";"));
        String reviewerSignedInfo = sources.stream()
                .map(source -> signatureSummary(source.getReview().getId(),
                        source.getReview().getReviewSignatureUserId(), source.getReview().getReviewSignatureId(),
                        source.getReview().getReviewedAt()))
                .collect(Collectors.joining(";"));
        values.put("lossDetailsSummary", join(lossSummary, "details=" + canonicalLossDetails(sources),
                "filler=" + fillerSignedInfo, "reviewer=" + reviewerSignedInfo));
        values.put("approvalSummary", reviewerSignedInfo);
        return values;
    }

    private BigDecimal sum(List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource> sources,
                           String field) {
        BigDecimal total = BigDecimal.ZERO;
        for (MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource source : sources) {
            total = total.add(switch (field) {
                case "outputQuantity" -> source.getFeedback().getFeedbackQuantity();
                case "qualifiedQuantity" -> source.getFeedback().getQualifiedQuantity();
                case "lossQuantity" -> source.getFeedback().getUnqualifiedQuantity();
                case "laborScrapQuantity" -> source.getFeedback().getLaborScrapQuantity();
                case "materialScrapQuantity" -> source.getFeedback().getMaterialScrapQuantity();
                case "otherScrapQuantity" -> source.getFeedback().getOtherScrapQuantity();
                default -> throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                        "损耗单摘要字段不受支持，field=" + field);
            });
        }
        return total;
    }

    private String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String signatureSummary(Long sourceId, Long userId, Long signatureId, LocalDateTime signedAt) {
        return join("sourceId=" + sourceId, "userId=" + userId, "signatureId=" + signatureId,
                "signedAt=" + displayValue(signedAt));
    }

    private Object aggregate(
            MesProBatchRecordCellLinkRuleDO rule,
            List<Object> values,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            MesProRouteFlowProcessBatchRecordDO binding,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        if (values.isEmpty() || values.stream().anyMatch(Objects::isNull)) {
            blockers.add(blocker("LOSS_REPORT_MAPPING_REQUIRED", snapshot, null, "LOSS_REPORT",
                    targetReportId(binding), rule.getSourceFieldCode(), rule.getTargetCellKey(),
                    "损耗报告映射缺少正式来源值", "请补齐正式生产反馈字段"));
            return null;
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        String strategy = StrUtil.trim(rule.getAggregationStrategy());
        if (StrUtil.isBlank(strategy)) {
            blockers.add(blocker("LOSS_REPORT_MAPPING_REQUIRED", snapshot, null, "LOSS_REPORT",
                    targetReportId(binding), rule.getSourceFieldCode(), rule.getTargetCellKey(),
                    "多个生产反馈来源缺少显式聚合策略", "请配置正式聚合策略"));
            return null;
        }
        try {
            return switch (strategy.toUpperCase()) {
                case "SUM" -> values.stream().map(this::toDecimal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                case "LIST" -> values.stream().map(this::displayValue).collect(Collectors.joining(","));
                case "DISTINCT_LIST" -> String.join(",", values.stream().map(this::displayValue)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
                case "FIRST" -> values.get(0);
                case "LAST" -> values.get(values.size() - 1);
                case "MIN" -> values.stream().map(this::toDecimal).min(BigDecimal::compareTo).orElseThrow();
                case "MAX" -> values.stream().map(this::toDecimal).max(BigDecimal::compareTo).orElseThrow();
                default -> throw new IllegalArgumentException("unsupported aggregation");
            };
        } catch (RuntimeException ex) {
            blockers.add(blocker("LOSS_REPORT_MAPPING_REQUIRED", snapshot, null, "LOSS_REPORT",
                    targetReportId(binding), rule.getSourceFieldCode(), rule.getTargetCellKey(),
                    "损耗报告聚合策略与正式来源值不兼容", "请修复字段值类型或聚合策略"));
            return null;
        }
    }

    private String canonicalLossDetails(
            List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource> sources) {
        Map<LossDetailKey, BigDecimal> totals = new LinkedHashMap<>();
        sources.stream().flatMap(source -> source.getLossDetails().stream())
                .sorted(Comparator.comparing(MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail::getReasonId)
                        .thenComparing(MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail::getReasonCode)
                        .thenComparing(MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail::getReasonName))
                .forEach(detail -> totals.merge(new LossDetailKey(detail.getReasonId(), detail.getReasonCode(),
                        detail.getReasonName()), detail.getQuantity(), BigDecimal::add));
        List<Map<String, Object>> canonical = new ArrayList<>();
        for (Map.Entry<LossDetailKey, BigDecimal> entry : totals.entrySet()) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("reasonId", entry.getKey().reasonId());
            detail.put("reasonCode", entry.getKey().reasonCode());
            detail.put("reasonName", entry.getKey().reasonName());
            detail.put("quantity", entry.getValue().stripTrailingZeros());
            canonical.add(detail);
        }
        return JsonUtils.toJsonString(canonical);
    }

    private List<String> collectEvidence(
            List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource> sources,
            MesProRouteFlowProcessBatchRecordDO binding,
            TargetMetadata target,
            List<MesProBatchRecordCellLinkRuleDO> rules,
            MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.TargetResolution dynamicTarget,
            Set<Long> sourceObjectIds,
            Set<String> sourceValueHashes,
            List<MesTeamLeaderActiveOrderReleaseSignatureEvidence> signatures) {
        List<String> hashes = new ArrayList<>();
        for (MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource source : sources) {
            String feedbackHash = hashFeedback(source.getFeedback());
            String detailsHash = sha256("LOSS_DETAILS_V1|" + canonicalLossDetails(List.of(source)));
            String eventHash = hashEvent(source.getEvent(), detailsHash);
            String allocationHash = hashAllocation(source.getAllocation());
            String reviewHash = hashReview(source.getReview());
            addEvidence(sourceObjectIds, sourceValueHashes, hashes, source.getFeedback().getId(), feedbackHash);
            addEvidence(sourceObjectIds, sourceValueHashes, hashes, source.getEvent().getId(), eventHash);
            addEvidence(sourceObjectIds, sourceValueHashes, hashes, source.getAllocation().getId(), allocationHash);
            addEvidence(sourceObjectIds, sourceValueHashes, hashes, source.getReview().getId(), reviewHash);
            sourceValueHashes.add(detailsHash);
            hashes.add(detailsHash);
            signatures.add(signature("FILLER", "PRODUCTION_SUBMIT", source.getEvent().getId(),
                    source.getEvent().getSignatureId(), source.getEvent().getSignatureUserId(),
                    source.getEvent().getServerSubmitTime(), eventHash));
            signatures.add(signature("REVIEWER", "PRODUCTION_REVIEW", source.getReview().getId(),
                    source.getReview().getReviewSignatureId(), source.getReview().getReviewSignatureUserId(),
                    source.getReview().getReviewedAt(), reviewHash));
        }
        String bindingHash = hashBinding(binding);
        addEvidence(sourceObjectIds, sourceValueHashes, hashes, binding.getId(), bindingHash);
        if (target.report() != null && target.version() != null) {
            addEvidence(sourceObjectIds, sourceValueHashes, hashes, target.report().getId(), hashReport(target.report()));
            addEvidence(sourceObjectIds, sourceValueHashes, hashes, target.version().getId(),
                    hashVersion(target.version()));
        }
        if (dynamicTarget != null) {
            hashes.add(hashDynamicTarget(dynamicTarget));
            sourceValueHashes.add(hashDynamicTarget(dynamicTarget));
        }
        for (MesProBatchRecordCellLinkRuleDO rule : rules) {
            addEvidence(sourceObjectIds, sourceValueHashes, hashes, rule.getId(), hashRule(rule));
        }
        return hashes;
    }

    private MesProEdhrBatchExecutionTaskDO requireCurrentBatchTask(
            Long batchExecutionId,
            List<MesProEdhrBatchExecutionTaskDO> tasks,
            MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport prepared) {
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = prepared.getSources().get(0).getSnapshot();
        MesProRouteFlowProcessBatchRecordDO binding = prepared.getBinding();
        boolean dynamic = isDynamicBinding(binding);
        List<MesProEdhrBatchExecutionTaskDO> matches = tasks == null ? List.of() : tasks.stream()
                .filter(Objects::nonNull)
                .filter(task -> task.getId() != null
                        && Objects.equals(batchExecutionId, task.getBatchExecutionId())
                        && Objects.equals(snapshot.getRouteProcessId(), task.getRouteProcessId())
                        && Objects.equals(snapshot.getProcessId(), task.getProcessId())
                        && Objects.equals(binding.getId(), task.getRouteBindingId())
                        && FORM_SLOT_TYPE.equals(task.getFormSlotType())
                        && RECORD_CATEGORY.equals(task.getRecordCategory())
                        && VALIDATION_PROFILE.equals(task.getValidationProfile())
                        && OWNER_ROLE.equals(task.getOwnerRoleKey())
                        && Objects.equals(binding.getRecordCategorySnapshotHash(),
                        task.getRouteBindingSnapshotHash())
                        && Objects.equals(binding.getSlotConfigSnapshotHash(), task.getSlotConfigSnapshotHash())
                        && (dynamic
                        ? Objects.equals(binding.getFormBindingKey(), task.getFormBindingKey())
                        && Objects.equals(binding.getFormTemplateId(), task.getFormTemplateId())
                        && Objects.equals(binding.getLastPublishedTemplateVersionId(), task.getFormTemplateVersionId())
                        && Objects.equals(binding.getLastPublishedTemplateVersionNo(), task.getFormTemplateVersionNo())
                        && task.getFormCenterInstanceId() != null && StrUtil.isBlank(task.getBatchRecordReportId())
                        : Objects.equals(binding.getBatchRecordReportId(), task.getBatchRecordReportId())
                        && Objects.equals(binding.getBatchRecordDefinitionId(), task.getBatchRecordDefinitionId())
                        && Objects.equals(binding.getBatchRecordVersionId(), task.getBatchRecordVersionId())
                        && task.getFormTemplateId() == null && StrUtil.isBlank(task.getFormBindingKey())))
                .toList();
        if (matches.size() != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "当前 eDHR 批次缺少唯一 LOSS_REPORT 正式目标任务，batchExecutionId=" + batchExecutionId
                            + "，routeProcessId=" + snapshot.getRouteProcessId());
        }
        return matches.get(0);
    }

    private MesProBatchRecordExecutionOpenOrCreateByContextReqVO toOpenRequest(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            Long batchExecutionId,
            MesProEdhrBatchExecutionTaskDO task) {
        return new MesProBatchRecordExecutionOpenOrCreateByContextReqVO()
                .setWorkOrderId(command.getWorkOrderId())
                .setRouteId(command.getRouteId())
                .setBatchExecutionId(batchExecutionId)
                .setProcessId(task.getProcessId())
                .setRouteProcessId(task.getRouteProcessId())
                .setTaskId(task.getId())
                .setBatchRecordReportId(task.getBatchRecordReportId())
                .setInstanceScope(task.getInstanceScope())
                .setSharedFormKey(task.getSharedFormKey())
                .setFormSlotType(FORM_SLOT_TYPE)
                .setRecordCategory(RECORD_CATEGORY)
                .setValidationProfile(VALIDATION_PROFILE)
                .setRecordbookEnabled(task.getRecordbookEnabled())
                .setPermissionScopeId(task.getPermissionScopeId())
                .setRouteBindingId(task.getRouteBindingId())
                .setRouteBindingSnapshotHash(task.getRouteBindingSnapshotHash())
                .setArchiveVisibility(task.getArchiveVisibility())
                .setSlotConfigSnapshotHash(task.getSlotConfigSnapshotHash())
                .setBatchCode(command.getBatchCode());
    }

    private void validateExecutionContext(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            Long batchExecutionId,
            MesProEdhrBatchExecutionTaskDO task,
            MesProBatchRecordExecutionDO execution) {
        if (execution == null || execution.getId() == null
                || !Objects.equals(command.getWorkOrderId(), execution.getWorkOrderId())
                || !Objects.equals(command.getRouteId(), execution.getRouteId())
                || !Objects.equals(batchExecutionId, execution.getBatchExecutionId())
                || !Objects.equals(task.getId(), execution.getTaskId())
                || !Objects.equals(task.getRouteProcessId(), execution.getRouteProcessId())
                || !Objects.equals(task.getBatchRecordReportId(), execution.getBatchRecordReportId())
                || !Objects.equals(task.getBatchRecordDefinitionId(), execution.getBatchRecordDefinitionId())
                || !Objects.equals(task.getBatchRecordVersionId(), execution.getBatchRecordVersionId())
                || !Objects.equals(task.getRouteBindingId(), execution.getRouteBindingId())
                || !Objects.equals(task.getRouteBindingSnapshotHash(), execution.getRouteBindingSnapshotHash())
                || !Objects.equals(task.getSlotConfigSnapshotHash(), execution.getSlotConfigSnapshotHash())
                || !Objects.equals(command.getBatchCode(), execution.getBatchCode())
                || !FORM_SLOT_TYPE.equals(execution.getFormSlotType())
                || !RECORD_CATEGORY.equals(execution.getRecordCategory())
                || !VALIDATION_PROFILE.equals(execution.getValidationProfile())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "损耗报告 execution 不属于当前批次传统报表任务，batchTaskId=" + task.getId());
        }
    }

    private List<MesProBatchRecordExecutionFieldAuditChange> toAuditChanges(
            MesProBatchRecordExecutionDO execution,
            MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport prepared) {
        Map<String, SnapshotField> fields = snapshotFields(execution.getExecutionSnapshotJson());
        Map<String, JsonNode> currentValues = currentValues(execution.getCellValuesJson());
        List<MesProBatchRecordExecutionFieldAuditChange> changes = new ArrayList<>();
        for (MesProBatchRecordCellLinkRuleDO rule : prepared.getRules()) {
            String cell = cellKey(rule.getTargetRowIndex(), rule.getTargetColumnIndex());
            SnapshotField field = fields.get(cell);
            MesProBatchRecordExecutionFieldAuditValueType valueType = parseValueType(rule.getTargetValueType());
            if (field == null || valueType == null || field.valueType() != valueType) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                        "损耗报告映射目标字段与 execution 快照不一致，cell=" + rule.getTargetCellKey());
            }
            Object value = auditValue(valueType, prepared.getMappedValues().get(rule.getSourceFieldCode()));
            changes.add(new MesProBatchRecordExecutionFieldAuditChange()
                    .setFieldPath(field.fieldPath())
                    .setFieldKey(field.fieldKey())
                    .setRowIndex(rule.getTargetRowIndex())
                    .setColumnIndex(rule.getTargetColumnIndex())
                    .setValueType(valueType)
                    .setNewValueJson(value)
                    .setNewValueDisplay(displayValue(value))
                    .setExpectedOldValueHash(oldValueHash(valueType,
                            currentValues.get(cell), field.defaultValue())));
        }
        return List.copyOf(changes);
    }

    private Map<String, SnapshotField> snapshotFields(String snapshotJson) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(snapshotJson);
            JsonNode fieldsNode = root == null ? null : root.get("fields");
            if (fieldsNode == null || !fieldsNode.isArray()) {
                throw new IllegalArgumentException("fields missing");
            }
            Map<String, SnapshotField> result = new LinkedHashMap<>();
            for (JsonNode field : fieldsNode) {
                Integer row = integer(field, "rowIndex");
                Integer column = integer(field, "columnIndex");
                String fieldPath = text(field, "fieldPath");
                String fieldKey = text(field, "fieldKey");
                MesProBatchRecordExecutionFieldAuditValueType valueType = parseValueType(text(field, "valueType"));
                if (row == null || column == null || StrUtil.hasBlank(fieldPath, fieldKey) || valueType == null) {
                    throw new IllegalArgumentException("invalid field");
                }
                JsonNode defaultValue = field.get("defaultValue");
                if (defaultValue == null || defaultValue.isMissingNode()) {
                    defaultValue = NullNode.instance;
                }
                if (result.putIfAbsent(cellKey(row, column),
                        new SnapshotField(fieldPath, fieldKey, valueType, defaultValue.deepCopy())) != null) {
                    throw new IllegalArgumentException("duplicate cell");
                }
            }
            return result;
        } catch (Exception ex) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "损耗报告 execution 字段快照无法解析，禁止写入");
        }
    }

    private Map<String, JsonNode> currentValues(String cellValuesJson) {
        if (StrUtil.isBlank(cellValuesJson)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "损耗报告 execution 缺少当前单元格值快照");
        }
        try {
            JsonNode cells = JsonUtils.getObjectMapper().readTree(cellValuesJson);
            if (cells == null || !cells.isArray()) {
                throw new IllegalArgumentException("cell values not array");
            }
            Map<String, JsonNode> result = new LinkedHashMap<>();
            for (JsonNode cell : cells) {
                Integer row = integer(cell, "rowIndex");
                Integer column = integer(cell, "columnIndex");
                if (row == null || column == null) {
                    throw new IllegalArgumentException("invalid cell");
                }
                JsonNode value = cell.get("value");
                if (result.putIfAbsent(cellKey(row, column),
                        value == null ? NullNode.instance : value.deepCopy()) != null) {
                    throw new IllegalArgumentException("duplicate cell");
                }
            }
            return result;
        } catch (Exception ex) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "损耗报告 execution 当前单元格值无法解析，禁止写入");
        }
    }

    private String oldValueHash(MesProBatchRecordExecutionFieldAuditValueType valueType,
                                JsonNode currentValue,
                                JsonNode defaultValue) {
        JsonNode oldValue = normalizeOldValue(valueType, currentValue == null ? defaultValue : currentValue);
        String canonical = oldValue == null || oldValue.isNull() || oldValue.isMissingNode()
                ? "null" : MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(valueType, oldValue);
        return MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue(canonical);
    }

    private JsonNode normalizeOldValue(MesProBatchRecordExecutionFieldAuditValueType valueType, JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return NullNode.instance;
        }
        if (!value.isTextual()) {
            return value;
        }
        String text = value.textValue();
        if (StrUtil.isBlank(text) && valueType != MesProBatchRecordExecutionFieldAuditValueType.STRING) {
            return valueType == MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN
                    ? BooleanNode.FALSE : NullNode.instance;
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.NUMBER) {
            return DecimalNode.valueOf(MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(
                    new BigDecimal(text)));
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN) {
            return BooleanNode.valueOf(Boolean.parseBoolean(text));
        }
        return TextNode.valueOf(text);
    }

    private Object auditValue(MesProBatchRecordExecutionFieldAuditValueType valueType, Object value) {
        if (value == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "损耗报告映射来源值为空，禁止写入");
        }
        try {
            return switch (valueType) {
                case NUMBER -> value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
                case DATETIME -> value instanceof LocalDateTime time ? DATETIME_FORMATTER.format(time)
                        : String.valueOf(value);
                case DATE -> value instanceof LocalDate date ? date.toString() : String.valueOf(value);
                case BOOLEAN -> value instanceof Boolean bool ? bool : Boolean.valueOf(String.valueOf(value));
                case STRING -> value instanceof LocalDateTime time ? DATETIME_FORMATTER.format(time)
                        : String.valueOf(value);
                default -> throw new IllegalArgumentException("unsupported target type");
            };
        } catch (RuntimeException ex) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "损耗报告映射来源值与目标类型不兼容，valueType=" + valueType);
        }
    }

    private String auditIdempotencyKey(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            MesProEdhrBatchExecutionTaskDO task,
            MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport prepared) {
        return "AO_RELEASE_LOSS_REPORT:" + sha256(join(command.getSourceSnapshotHash(), task.getId(),
                task.getRouteProcessId(), task.getProcessId(), prepared.getEvidenceHash()));
    }

    private String auditReason(MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport prepared) {
        String feedbackIds = prepared.getSources().stream().map(source -> String.valueOf(source.getFeedback().getId()))
                .collect(Collectors.joining(","));
        String signatureIds = prepared.getSources().stream().map(source -> String.valueOf(source.getEvent().getSignatureId()))
                .collect(Collectors.joining(","));
        String reviewSignatureIds = prepared.getSources().stream()
                .map(source -> String.valueOf(source.getReview().getReviewSignatureId()))
                .collect(Collectors.joining(","));
        return "正式生产反馈自动生成损耗报告；feedbackIds=" + feedbackIds
                + "；productionSignatureIds=" + signatureIds
                + "；reviewSignatureIds=" + reviewSignatureIds;
    }

    private Comparator<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource> sourceComparator() {
        return Comparator
                .comparing((MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource source) ->
                                source.getSnapshot() == null ? null : source.getSnapshot().getRouteProcessId(),
                        Comparator.nullsFirst(Long::compareTo))
                .thenComparing(source -> source.getSnapshot() == null ? null : source.getSnapshot().getProcessId(),
                        Comparator.nullsFirst(Long::compareTo))
                .thenComparing(source -> source.getEvent() == null ? null : source.getEvent().getId(),
                        Comparator.nullsFirst(Long::compareTo));
    }

    private String hashFeedback(MesProFeedbackDO feedback) {
        return sha256(join("PRODUCTION_LOSS_FEEDBACK_V1", feedback.getId(), feedback.getCode(),
                feedback.getWorkOrderId(), feedback.getRouteId(), feedback.getProcessId(), feedback.getFeedbackTime(),
                feedback.getFeedbackQuantity(), feedback.getQualifiedQuantity(), feedback.getUnqualifiedQuantity(),
                feedback.getLaborScrapQuantity(), feedback.getMaterialScrapQuantity(),
                feedback.getOtherScrapQuantity(), feedback.getLossReasonId(),
                feedback.getLossReasonCodeSnapshot(), feedback.getLossReasonNameSnapshot(),
                feedback.getFeedbackUserId(), feedback.getApproveUserId()));
    }

    private String hashEvent(MesProProcessPoolEventDO event, String detailsHash) {
        return sha256(join("PRODUCTION_LOSS_EVENT_V1", event.getId(), event.getEventType(), event.getWorkOrderId(),
                event.getRouteId(), event.getRouteProcessId(), event.getProcessId(), event.getActualEmployeeId(),
                event.getFeedbackSourceType(), event.getFeedbackSourceId(), event.getServerSubmitTime(),
                event.getSignatureId(), event.getSignatureUserId(), event.getSignatureSnapshot(), detailsHash));
    }

    private String hashAllocation(MesProcessPoolReportAllocationDO allocation) {
        return sha256(join("PRODUCTION_LOSS_ALLOCATION_V1", allocation.getId(), allocation.getEventId(),
                allocation.getReviewId(), allocation.getLeaderUserId(), allocation.getActiveOrderId(),
                allocation.getWorkOrderId(), allocation.getRouteProcessId(), allocation.getProcessId(),
                allocation.getAllocatedQuantity(), allocation.getConfirmedAt()));
    }

    private String hashReview(MesProcessPoolSubmissionReviewDO review) {
        return sha256(join("PRODUCTION_LOSS_REVIEW_V1", review.getId(), review.getEventId(),
                review.getLeaderUserId(), review.getLeaderType(), review.getReviewStatus(), review.getReviewedAt(),
                review.getReviewSignatureId(), review.getReviewSignatureUserId(),
                review.getReviewSignatureSnapshotJson()));
    }

    private String hashBinding(MesProRouteFlowProcessBatchRecordDO binding) {
        return sha256(join("PRODUCTION_LOSS_BINDING_V1", binding.getId(), binding.getRouteId(),
                binding.getRouteProcessId(), binding.getUseType(), binding.getBatchRecordReportId(),
                binding.getBatchRecordDefinitionId(), binding.getBatchRecordVersionId(), binding.getFormSlotType(),
                binding.getFormBindingKey(), binding.getFormTemplateId(), binding.getLastPublishedTemplateVersionId(),
                binding.getLastPublishedTemplateVersionNo(), binding.getRecordCategory(),
                binding.getValidationProfile(), binding.getOwnerRoleKey(),
                binding.getPermissionScopeId(), binding.getRecordCategorySnapshotHash(),
                binding.getSlotConfigSnapshotHash()));
    }

    private String hashReport(MesProBatchRecordReportDO report) {
        return sha256(join("PRODUCTION_LOSS_REPORT_META_V1", report.getId(), report.getReportId(),
                report.getReportCode(), report.getReportName(), report.getFormSlotType(),
                report.getBatchRecordDefinitionId(), report.getBatchRecordVersionId(), report.getSourceFileSha256()));
    }

    private String hashVersion(MesProBatchRecordVersionDO version) {
        return sha256(join("PRODUCTION_LOSS_VERSION_V1", version.getId(), version.getDefinitionId(),
                version.getVersionNo(), version.getStatus(), version.getRouteId(), version.getSourceFileSha256()));
    }

    private String hashRule(MesProBatchRecordCellLinkRuleDO rule) {
        return sha256(join("PRODUCTION_LOSS_RULE_V1", rule.getId(), rule.getRuleVersion(), rule.getScopeType(),
                rule.getScopeId(), rule.getSourceType(), rule.getSourceCellKey(), rule.getSourceFieldCode(),
                rule.getSourceValueType(), rule.getTargetReportId(),
                rule.getTargetRowIndex(), rule.getTargetColumnIndex(), rule.getTargetCellKey(),
                rule.getTargetValueType(), rule.getAggregationStrategy(), rule.getTemplateSnapshotHash(),
                rule.getEnabled()));
    }

    private String hashDynamicTarget(MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.TargetResolution target) {
        return sha256(join("PRODUCTION_LOSS_DYNAMIC_TARGET_V1", target.getTemplateVersionId(),
                target.getTemplateSnapshotHash(), target.getTargetFieldCodes()));
    }

    private String hashMappedValue(MesProBatchRecordCellLinkRuleDO rule, Object value) {
        return sha256(join("PRODUCTION_LOSS_MAPPED_FIELD_VALUE_V1", rule.getSourceCellKey(),
                rule.getSourceFieldCode(), value));
    }

    private void addEvidence(Set<Long> objectIds, Set<String> allHashes, List<String> localHashes,
                             Long objectId, String hash) {
        if (objectId != null) {
            objectIds.add(objectId);
        }
        allHashes.add(hash);
        localHashes.add(hash);
    }

    private MesTeamLeaderActiveOrderReleaseSignatureEvidence signature(
            String role, String sourceType, Long sourceId, Long signatureId, Long userId,
            LocalDateTime signedAt, String evidenceHash) {
        return new MesTeamLeaderActiveOrderReleaseSignatureEvidence()
                .setRole(role)
                .setSourceType(sourceType)
                .setSourceId(sourceId)
                .setSignatureId(signatureId)
                .setUserId(userId)
                .setSignedAt(signedAt)
                .setEvidenceHash(evidenceHash);
    }

    private MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.WriteCommand toDynamicWriteCommand(
            MesTeamLeaderActiveOrderReleaseLossReportPlan plan,
            Long batchExecutionId,
            MesProEdhrBatchExecutionTaskDO batchTask,
            MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport prepared) {
        List<MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.FieldWrite> fields =
                prepared.getRules().stream()
                        .map(rule -> {
                            Object rawValue = prepared.getMappedValues().get(rule.getSourceFieldCode());
                            Object value = auditValue(MesProBatchRecordExecutionFieldAuditValueType.valueOf(
                                    rule.getTargetValueType()), rawValue);
                            return new MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.FieldWrite()
                                    .setRuleId(rule.getId())
                                    .setRuleVersion(rule.getRuleVersion())
                                    .setSourceCellKey(rule.getSourceCellKey())
                                    .setSourceFieldCode(rule.getSourceFieldCode())
                                    .setTargetFieldCode(prepared.getDynamicTarget().getTargetFieldCodes()
                                            .get(rule.getId()))
                                    .setValue(value)
                                    .setDisplayValue(displayValue(value))
                                    .setSourceValueHash(hashMappedValue(rule, rawValue));
                        })
                        .toList();
        return new MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.WriteCommand()
                .setTenantId(plan.getCommand().getTenantId())
                .setBatchExecutionId(batchExecutionId)
                .setBatchTask(batchTask)
                .setBinding(prepared.getBinding())
                .setTarget(prepared.getDynamicTarget())
                .setFields(fields)
                .setSourceSnapshotHash(plan.getCommand().getSourceSnapshotHash())
                .setEvidenceHash(prepared.getEvidenceHash())
                .setSignatureEvidence(plan.getSignatureEvidence());
    }

    private boolean isDynamicBinding(MesProRouteFlowProcessBatchRecordDO binding) {
        return binding != null && StrUtil.isBlank(binding.getBatchRecordReportId())
                && LOSS_REPORT_FORM_TEMPLATE_ID.equals(binding.getFormTemplateId())
                && StrUtil.isNotBlank(binding.getFormBindingKey())
                && binding.getLastPublishedTemplateVersionId() != null;
    }

    private String targetReportId(MesProRouteFlowProcessBatchRecordDO binding) {
        return isDynamicBinding(binding) ? "FORMTPL:" + binding.getLastPublishedTemplateVersionId()
                : binding.getBatchRecordReportId();
    }

    private String summarySourceKey(String fieldCode) {
        return "SUMMARY|" + fieldCode;
    }

    private MesTeamLeaderActiveOrderReleaseBlocker blocker(
            String type,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            Long routeProcessId,
            String objectType,
            Object objectId,
            String fieldCode,
            String cellKey,
            String reason,
            String suggestion) {
        return new MesTeamLeaderActiveOrderReleaseBlocker()
                .setBlockerType(type)
                .setObjectType(objectType)
                .setObjectId(objectId == null ? null : String.valueOf(objectId))
                .setRouteProcessId(snapshot == null ? routeProcessId : snapshot.getRouteProcessId())
                .setProcessId(snapshot == null ? null : snapshot.getProcessId())
                .setFieldCode(fieldCode)
                .setCellKey(cellKey)
                .setReason(reason)
                .setSuggestion(suggestion);
    }

    private boolean nonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0;
    }

    private boolean decimalEquals(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private BigDecimal toDecimal(Object value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }

    private MesProBatchRecordExecutionFieldAuditValueType parseValueType(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return MesProBatchRecordExecutionFieldAuditValueType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String displayValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private String join(Object... values) {
        List<String> normalized = new ArrayList<>(values.length);
        for (Object value : values) {
            if (value == null) {
                normalized.add("null");
            } else if (value instanceof BigDecimal decimal) {
                normalized.add(decimal.stripTrailingZeros().toPlainString());
            } else {
                normalized.add(String.valueOf(value));
            }
        }
        return String.join("|", normalized);
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

    private String cellKey(Integer row, Integer column) {
        return row == null || column == null ? null : row + ":" + column;
    }

    private Integer integer(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value == null || !value.isIntegralNumber() ? null : value.intValue();
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value == null || value.isNull() ? null : value.asText();
    }

    private <T> List<T> distinct(List<T> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }

    private record ProcessKey(Long routeProcessId, Long processId) {
    }

    private record LossDetailKey(Long reasonId, String reasonCode, String reasonName) {
    }

    private record TargetMetadata(MesProBatchRecordReportDO report,
                                  MesProBatchRecordVersionDO version,
                                  List<String> snapshotHashes) {
    }

    private record MappingResolution(List<MesProBatchRecordCellLinkRuleDO> rules,
                                     Map<String, Object> values,
                                     MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.TargetResolution dynamicTarget) {
    }

    private record SnapshotField(String fieldPath, String fieldKey,
                                 MesProBatchRecordExecutionFieldAuditValueType valueType,
                                 JsonNode defaultValue) {
    }
}
