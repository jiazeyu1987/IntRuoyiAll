package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditChange;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditValueType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.PqcResultValueValidator;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

@Service
public class MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl
        implements MesTeamLeaderActiveOrderReleaseProcessInspectionWriter {

    private static final String USE_TYPE_BATCH = "BATCH";
    private static final String FORM_SLOT_TYPE = "PROCESS_INSPECTION";
    private static final String RECORD_CATEGORY = "INTERNAL_RECORD";
    private static final String VALIDATION_PROFILE = "INTERNAL_TRACE";
    private static final String OWNER_ROLE = "QUALITY";
    private static final Long PROCESS_INSPECTION_FORM_TEMPLATE_ID = 28L;
    private static final String SOURCE_TYPE = "PQC_AGGREGATE_DETAIL";
    private static final String SCOPE_TYPE_ROUTE_VERSION = "ROUTE_VERSION";
    private static final String PQC_TASK_SOURCE_TYPE = "MES_PQC_INSPECTION_TASK";
    private static final String LEADER_TYPE_PQC = "PQC";
    private static final List<String> DYNAMIC_SUMMARY_FIELDS = List.of(
            "dccProjectCode", "dccProjectName", "qaVersionNo", "itemSummary", "resultSummary",
            "equipmentSummary", "overallJudgement", "inspectorSignedInfo", "reviewerSignedInfo");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MesTeamLeaderActiveOrderReleaseProcessInspectionReader reader;
    private final MesProRouteFlowProcessBatchRecordMapper bindingMapper;
    private final MesProBatchRecordCellLinkRuleMapper ruleMapper;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesProBatchRecordExecutionService executionService;
    private final MesProBatchRecordExecutionMapper executionMapper;
    private final MesProBatchRecordExecutionFieldAuditService fieldAuditService;
    private final MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort dynamicFormPort;

    public MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl(
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader reader,
            MesProRouteFlowProcessBatchRecordMapper bindingMapper,
            MesProBatchRecordCellLinkRuleMapper ruleMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesProBatchRecordExecutionService executionService,
            MesProBatchRecordExecutionMapper executionMapper,
            MesProBatchRecordExecutionFieldAuditService fieldAuditService,
            MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort dynamicFormPort) {
        this.reader = reader;
        this.bindingMapper = bindingMapper;
        this.ruleMapper = ruleMapper;
        this.batchTaskMapper = batchTaskMapper;
        this.executionService = executionService;
        this.executionMapper = executionMapper;
        this.fieldAuditService = fieldAuditService;
        this.dynamicFormPort = dynamicFormPort;
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command) {
        validateCommand(command);
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.SourceBundle bundle = reader.read(command);
        List<MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource> sources =
                bundle == null || bundle.getSources() == null ? List.of() : bundle.getSources().stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(source -> source.getTask() == null ? Long.MAX_VALUE
                                : source.getTask().getId(), Comparator.nullsLast(Long::compareTo)))
                        .toList();
        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = new ArrayList<>();
        List<MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection> prepared = new ArrayList<>();
        LinkedHashSet<Long> sourceObjectIds = new LinkedHashSet<>();
        LinkedHashSet<String> sourceValueHashes = new LinkedHashSet<>();
        List<MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence> signatures =
                new ArrayList<>();
        if (sources.isEmpty()) {
            blockers.add(blocker("PQC_CONFIRMED_AGGREGATE_REQUIRED", "ACTIVE_ORDER", command.getActiveOrderId(),
                    null, "活跃订单缺少正式 PQC 任务和确认汇集", "请完成 PQC 提交、组长复核和结构化汇集"));
        }
        for (MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source : sources) {
            if (!validateCoreSource(command, source, blockers)) {
                continue;
            }
            if (!validateSignatureEvidence(command, source, blockers)) {
                continue;
            }
            if (!validateDccProjectIdentity(command, source, blockers)) {
                continue;
            }
            if (!validateQaDccProvenance(source, blockers)) {
                continue;
            }
            if (!validatePublishedQa(command, source, blockers)) {
                continue;
            }
            MesProRouteFlowProcessBatchRecordDO binding = formalBinding(source.getTask(), blockers);
            if (binding == null) {
                continue;
            }
            MappingResolution mapping = resolveMappings(command, source, binding, blockers);
            if (mapping == null) {
                continue;
            }
            List<String> inspectionHashes = new ArrayList<>(collectEvidence(source, binding, mapping.rules(),
                    sourceObjectIds, signatures));
            for (MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.MappedValue mapped : mapping.values()) {
                inspectionHashes.add(mapped.getSourceValueHash());
            }
            sourceValueHashes.addAll(inspectionHashes);
            prepared.add(new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection()
                    .setSource(source)
                    .setBinding(binding)
                    .setMappedValues(mapping.values())
                    .setDynamicTarget(mapping.dynamicTarget())
                    .setEvidenceHash(sha256(String.join("|", inspectionHashes))));
        }
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan()
                .setCommand(command)
                .setPreparedInspections(List.copyOf(prepared))
                .setSourceObjectIds(List.copyOf(sourceObjectIds))
                .setSourceValueHashes(List.copyOf(sourceValueHashes))
                .setSignatureEvidence(List.copyOf(signatures))
                .setBlockers(List.copyOf(blockers));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult write(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan, Long batchExecutionId) {
        validateWriteInput(plan, batchExecutionId);
        List<MesProEdhrBatchExecutionTaskDO> batchTasks =
                batchTaskMapper.selectListByBatchExecutionId(batchExecutionId);
        List<Long> executionIds = new ArrayList<>();
        List<Long> formCenterInstanceIds = new ArrayList<>();
        List<Long> auditBatchIds = new ArrayList<>();
        List<String> auditHeadHashes = new ArrayList<>();
        for (MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection inspection
                : plan.getPreparedInspections()) {
            MesProEdhrBatchExecutionTaskDO batchTask = requireCurrentBatchTask(
                    batchExecutionId, batchTasks, inspection);
            if (isDynamicBinding(inspection.getBinding())) {
                MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.WriteResult dynamicWrite =
                        dynamicFormPort.write(toDynamicWriteCommand(plan, batchExecutionId, batchTask, inspection));
                if (dynamicWrite == null || dynamicWrite.getFormCenterInstanceId() == null
                        || dynamicWrite.getFieldAuditSnapshotId() == null
                        || StrUtil.isBlank(dynamicWrite.getFieldAuditHeadHash())
                        || !"EFFECTIVE".equals(dynamicWrite.getEffectiveStatus())) {
                    throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                            "过程检验动态 writer 未返回 EFFECTIVE FormCenter instance 与审计快照");
                }
                formCenterInstanceIds.add(dynamicWrite.getFormCenterInstanceId());
                auditBatchIds.add(dynamicWrite.getFieldAuditSnapshotId());
                auditHeadHashes.add(dynamicWrite.getFieldAuditHeadHash());
                continue;
            }
            MesProBatchRecordExecutionOpenOrCreateByContextRespVO opened = executionService.openOrCreateByContext(
                    toOpenRequest(plan.getCommand(), batchExecutionId, batchTask));
            if (opened == null || opened.getId() == null) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                        "过程检验 writer 未创建当前批次传统 execution");
            }
            MesProBatchRecordExecutionDO execution = executionMapper.selectById(opened.getId());
            validateExecutionContext(plan.getCommand(), batchExecutionId, batchTask, execution);
            List<MesProBatchRecordExecutionFieldAuditChange> changes = toAuditChanges(execution, inspection);
            MesProBatchRecordExecutionFieldAuditSaveResult audit = fieldAuditService.saveSystemCellLinkChanges(
                    new MesProBatchRecordExecutionFieldAuditSaveChangesCommand()
                            .setExecutionId(execution.getId())
                            .setIdempotencyKey(auditIdempotencyKey(batchTask, plan.getCommand(), inspection))
                            .setBaseCellValuesHash(execution.getCellValuesHash())
                            .setBaseFieldAuditRevision(execution.getFieldAuditRevision())
                            .setBaseFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                            .setReasonCategory("OTHER")
                            .setReasonText(auditReason(inspection))
                            .setChanges(changes));
            if (audit == null || audit.getAuditBatchId() == null || StrUtil.isBlank(audit.getCellValuesHash())
                    || StrUtil.isBlank(audit.getFieldAuditHeadHash())) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                        "过程检验 writer 未返回字段审计证据，pqcTaskId=" + inspection.getSource().getTask().getId());
            }
            executionIds.add(execution.getId());
            auditBatchIds.add(audit.getAuditBatchId());
            auditHeadHashes.add(audit.getFieldAuditHeadHash());
        }
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult()
                .setDocumentType(FORM_SLOT_TYPE)
                .setBatchRecordExecutionIds(distinct(executionIds))
                .setFormCenterInstanceIds(distinct(formCenterInstanceIds))
                .setFieldAuditIds(distinct(auditBatchIds))
                .setFieldAuditHeadHashes(distinct(auditHeadHashes))
                .setSourceObjectIds(plan.getSourceObjectIds())
                .setSourceValueHashes(plan.getSourceValueHashes())
                .setSignatureEvidence(plan.getSignatureEvidence())
                .setBlockers(List.of());
    }

    private void validateCommand(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command) {
        if (command == null || command.getTenantId() == null || command.getTenantId() <= 0
                || command.getActiveOrderId() == null || command.getActiveOrderId() <= 0
                || command.getWorkOrderId() == null || command.getWorkOrderId() <= 0
                || command.getProductId() == null || command.getProductId() <= 0
                || command.getRouteId() == null || command.getRouteId() <= 0
                || command.getRouteVersionId() == null || command.getRouteVersionId() <= 0
                || StrUtil.isBlank(command.getBatchCode()) || StrUtil.isBlank(command.getSourceSnapshotHash())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseProcessInspectionPlan");
        }
    }

    private void validateWriteInput(MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan,
                                    Long batchExecutionId) {
        if (plan == null || plan.getCommand() == null || plan.getPreparedInspections() == null
                || plan.getBlockers() == null || batchExecutionId == null || batchExecutionId <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseProcessInspectionWrite");
        }
        if (!plan.getBlockers().isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "过程检验 writer plan 存在 blocker，禁止写入");
        }
    }

    private boolean validateCoreSource(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        MesPqcInspectionTaskDO task = source.getTask();
        List<MesPqcProcessInspectionAggregateDetailDO> details = source.getAggregateDetails();
        if (task == null || task.getId() == null || !Objects.equals(command.getTenantId(), task.getTenantId())
                || !Objects.equals(command.getActiveOrderId(), task.getActiveOrderId())
                || !Objects.equals(command.getWorkOrderId(), task.getWorkOrderId())
                || !Objects.equals(command.getRouteId(), task.getRouteId())
                || !Objects.equals(command.getRouteVersionId(), task.getRouteVersionId())
                || task.getRouteProcessId() == null || task.getProcessId() == null
                || task.getRegulationVersionId() == null
                || !MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus())
                || details == null || details.isEmpty()) {
            blockers.add(blocker("PQC_CONFIRMED_AGGREGATE_REQUIRED", "PQC_TASK",
                    task == null ? null : task.getId(), null,
                    "PQC task 必须为 CONFIRMED 且存在结构化汇集明细",
                    "请完成 PQC 组长复核和过程检验汇集"));
            return false;
        }
        boolean valid = details.stream().allMatch(detail -> validAggregateIdentity(command, task, detail));
        Set<Long> eventIds = details.stream().map(MesPqcProcessInspectionAggregateDetailDO::getEventId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> reviewIds = details.stream().map(MesPqcProcessInspectionAggregateDetailDO::getReviewId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> recordIds = details.stream().map(MesPqcProcessInspectionAggregateDetailDO::getSourcePqcRecordId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!valid || eventIds.size() != 1 || reviewIds.size() != 1 || recordIds.size() != 1) {
            blockers.add(blocker("PQC_CONFIRMED_AGGREGATE_REQUIRED", "PQC_TASK", task.getId(), null,
                    "PQC 汇集明细与 task/工单/路线/工序/轮次/租户不一致",
                    "请修复正式 PQC 汇集身份后重新申请"));
            return false;
        }
        return true;
    }

    private boolean validAggregateIdentity(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
                                           MesPqcInspectionTaskDO task,
                                           MesPqcProcessInspectionAggregateDetailDO detail) {
        return detail != null && detail.getId() != null && detail.getSourcePqcRecordId() != null
                && detail.getSourcePieceDetailId() != null && detail.getEventId() != null
                && detail.getReviewId() != null && detail.getPqcTaskId() != null
                && Objects.equals(command.getTenantId(), detail.getTenantId())
                && Objects.equals(task.getId(), detail.getPqcTaskId())
                && Objects.equals(task.getActiveOrderId(), detail.getActiveOrderId())
                && Objects.equals(task.getWorkOrderId(), detail.getWorkOrderId())
                && Objects.equals(task.getRouteId(), detail.getRouteId())
                && Objects.equals(task.getRouteVersionId(), detail.getRouteVersionId())
                && Objects.equals(task.getRouteProcessId(), detail.getRouteProcessId())
                && Objects.equals(task.getProcessId(), detail.getProcessId())
                && Objects.equals(task.getRegulationVersionId(), detail.getRegulationVersionId())
                && Objects.equals(task.getInspectionType(), detail.getInspectionType())
                && Objects.equals(task.getBusinessDate(), detail.getBusinessDate())
                && Objects.equals(task.getShiftCode(), detail.getShiftCode())
                && Objects.equals(task.getRoundNo(), detail.getRoundNo())
                && Objects.equals(task.getActualInspectionQuantity(), detail.getActualInspectionQuantity())
                && detail.getAggregatedAt() != null;
    }

    private boolean validateSignatureEvidence(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        MesPqcInspectionTaskDO task = source.getTask();
        MesPqcProcessInspectionAggregateDetailDO first = source.getAggregateDetails().get(0);
        MesProProcessPoolEventDO event = source.getEvent();
        MesProProcessPoolPqcRecordDO record = source.getPqcRecord();
        MesProcessPoolSubmissionReviewDO review = source.getReview();
        boolean eventValid = event != null && event.getId() != null
                && Objects.equals(command.getTenantId(), event.getTenantId())
                && Objects.equals(first.getEventId(), event.getId())
                && MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())
                && Objects.equals(command.getWorkOrderId(), event.getWorkOrderId())
                && Objects.equals(command.getRouteId(), event.getRouteId())
                && Objects.equals(task.getRouteProcessId(), event.getRouteProcessId())
                && Objects.equals(task.getProcessId(), event.getProcessId())
                && PQC_TASK_SOURCE_TYPE.equals(event.getFeedbackSourceType())
                && Objects.equals(task.getId(), event.getFeedbackSourceId())
                && event.getActualEmployeeId() != null && event.getSignatureId() != null
                && Objects.equals(event.getActualEmployeeId(), event.getSignatureUserId())
                && event.getServerSubmitTime() != null && StrUtil.isNotBlank(event.getSignatureSnapshot());
        boolean recordValid = record != null && record.getId() != null
                && Objects.equals(command.getTenantId(), record.getTenantId())
                && Objects.equals(first.getSourcePqcRecordId(), record.getId())
                && Objects.equals(event == null ? null : event.getId(), record.getEventId())
                && Objects.equals(command.getWorkOrderId(), record.getWorkOrderId())
                && Objects.equals(command.getRouteId(), record.getRouteId())
                && Objects.equals(task.getRouteProcessId(), record.getRouteProcessId())
                && Objects.equals(task.getProcessId(), record.getProcessId())
                && Objects.equals(event == null ? null : event.getActualEmployeeId(), record.getActualEmployeeId())
                && Objects.equals(event == null ? null : event.getSignatureId(), record.getSignatureId())
                && Objects.equals(event == null ? null : event.getSignatureUserId(), record.getSignatureUserId())
                && Objects.equals(event == null ? null : event.getServerSubmitTime(), record.getServerSubmitTime())
                && MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_AGGREGATED
                .equals(record.getProcessInspectionAggregationStatus())
                && Objects.equals(first.getReviewId(), record.getProcessInspectionReviewId());
        boolean reviewValid = review != null && review.getId() != null
                && Objects.equals(command.getTenantId(), review.getTenantId())
                && Objects.equals(first.getReviewId(), review.getId())
                && Objects.equals(first.getEventId(), review.getEventId())
                && LEADER_TYPE_PQC.equals(review.getLeaderType())
                && MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(review.getReviewStatus())
                && review.getLeaderUserId() != null && review.getReviewSignatureId() != null
                && Objects.equals(review.getLeaderUserId(), review.getReviewSignatureUserId())
                && review.getReviewedAt() != null && StrUtil.isNotBlank(review.getReviewSignatureSnapshotJson());
        if (!eventValid || !recordValid || !reviewValid) {
            blockers.add(blocker("PQC_SIGNATURE_REQUIRED", "PQC_TASK", task.getId(), null,
                    "PQC 正式提交或 APPROVED 组长复核缺少一致的签名、人员或服务端时间",
                    "请使用正式 PQC 页面完成提交和组长复核签名"));
            return false;
        }
        return true;
    }

    private boolean validatePublishedQa(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        MesPqcInspectionTaskDO task = source.getTask();
        MesQaInspectionRegulationDO regulation = source.getRegulation();
        MesQaInspectionRegulationVersionDO version = source.getRegulationVersion();
        boolean regulationValid = regulation != null && regulation.getId() != null
                && Objects.equals(command.getTenantId(), regulation.getTenantId())
                && Objects.equals(command.getProductId(), regulation.getProductId())
                && Objects.equals(command.getRouteId(), regulation.getRouteId())
                && Objects.equals(task.getProcessId(), regulation.getProcessId())
                && MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA.equals(regulation.getOwnerModule())
                && "PUBLISHED".equals(regulation.getLifecycleStatus())
                && Objects.equals(task.getRegulationVersionId(), regulation.getCurrentVersionId());
        boolean versionValid = version != null && version.getId() != null
                && Objects.equals(command.getTenantId(), version.getTenantId())
                && Objects.equals(regulation == null ? null : regulation.getId(), version.getRegulationId())
                && Objects.equals(task.getRegulationVersionId(), version.getId())
                && "PUBLISHED".equals(version.getLifecycleStatus())
                && version.getPublishedAt() != null && StrUtil.isNotBlank(version.getSnapshotJson());
        if (!regulationValid || !versionValid) {
            blockers.add(blocker("PQC_QA_REGULATION_REQUIRED", "ROUTE_PROCESS", task.getRouteProcessId(),
                    null, "PQC task 缺少同产品、同路线、同稳定工序的最新 PUBLISHED QA 版本",
                    "请发布并绑定正式 QA 检验规程"));
            return false;
        }
        String mismatchItem = qaMismatchItem(source);
        if (mismatchItem != null) {
            MesPqcProcessInspectionAggregateDetailDO detail = source.getAggregateDetails().stream()
                    .filter(item -> Objects.equals(mismatchItem, item.getItemCode())).findFirst()
                    .orElse(source.getAggregateDetails().get(0));
            blockers.add(blocker("PQC_QA_ITEM_MISMATCH", "PQC_AGGREGATE_DETAIL", detail.getId(),
                    mismatchItem, "PQC 汇集项目、方法、标准、上下限、设备、实测值或判定与发布 QA 不一致",
                    "请按当前发布 QA 项目重新完成 PQC 提交与汇集"));
            return false;
        }
        return true;
    }

    private boolean validateDccProjectIdentity(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        var project = source.getDccProject();
        if (project == null || project.getId() == null
                || !Objects.equals(command.getTenantId(), project.getTenantId())
                || StrUtil.isBlank(source.getRouteProjectCode())
                || !Objects.equals(StrUtil.trim(source.getRouteProjectCode()), StrUtil.trim(project.getProjectCode()))
                || StrUtil.isBlank(project.getProjectCode()) || StrUtil.isBlank(project.getProjectName())
                || !"ENABLE".equals(project.getStatus())) {
            blockers.add(blocker("PQC_DCC_PROJECT_IDENTITY_REQUIRED", "PRODUCT", command.getProductId(), null,
                    "路线项目代码与唯一启用 DCC 项目身份不一致",
                    "请通过正式路线版本产品快照配置唯一 DCC 项目代码；禁止把 MES productId 当作 DCC productMasterId"));
            return false;
        }
        return true;
    }

    private boolean validateQaDccProvenance(
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        var provenance = source.getQaDccProvenance();
        if (provenance == null || !provenance.isVerifiedFor(source.getDccProject(), source.getRegulation(),
                source.getRegulationVersion())) {
            String blockerType = provenance == null || StrUtil.isBlank(provenance.getBlockerType())
                    ? "PQC_DCC_QA_PROVENANCE_REQUIRED" : provenance.getBlockerType();
            String reason = provenance == null || StrUtil.isBlank(provenance.getBlockerMessage())
                    ? "QA 版本缺少可验证的显式 DCC 项目来源关系" : provenance.getBlockerMessage();
            blockers.add(blocker(blockerType, "PQC_TASK", source.getTask().getId(), null, reason,
                    "请建立 QA 发布版本到 DCC 项目的正式来源关系；禁止用 productId/processId 推断"));
            return false;
        }
        return true;
    }

    private String qaMismatchItem(MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source) {
        MesPqcInspectionTaskDO task = source.getTask();
        List<MesQaInspectionRegulationItemDO> items = source.getRegulationItems() == null ? List.of()
                : source.getRegulationItems().stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(task.getInspectionType(), item.getInspectionType()))
                .toList();
        Map<String, MesQaInspectionRegulationItemDO> itemByCode = new LinkedHashMap<>();
        for (MesQaInspectionRegulationItemDO item : items) {
            if (StrUtil.isBlank(item.getItemCode()) || itemByCode.putIfAbsent(item.getItemCode(), item) != null) {
                return StrUtil.blankToDefault(item.getItemCode(), "*");
            }
        }
        Set<String> aggregateItemCodes = source.getAggregateDetails().stream()
                .map(MesPqcProcessInspectionAggregateDetailDO::getItemCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (itemByCode.isEmpty() || !itemByCode.keySet().equals(aggregateItemCodes)) {
            return aggregateItemCodes.stream().findFirst().orElse("*");
        }
        Map<String, List<MesQaInspectionRegulationItemEquipmentDO>> equipmentByItem =
                source.getRegulationItemEquipment() == null ? Map.of()
                        : source.getRegulationItemEquipment().stream()
                        .filter(Objects::nonNull)
                        .filter(item -> Objects.equals(task.getInspectionType(), item.getInspectionType()))
                        .collect(Collectors.groupingBy(MesQaInspectionRegulationItemEquipmentDO::getItemCode));
        for (Map.Entry<String, MesQaInspectionRegulationItemDO> entry : itemByCode.entrySet()) {
            List<MesPqcProcessInspectionAggregateDetailDO> details = source.getAggregateDetails().stream()
                    .filter(detail -> Objects.equals(entry.getKey(), detail.getItemCode()))
                    .sorted(Comparator.comparing(MesPqcProcessInspectionAggregateDetailDO::getSampleNo))
                    .toList();
            if (!sampleNumbersComplete(task, details)) {
                return entry.getKey();
            }
            for (MesPqcProcessInspectionAggregateDetailDO detail : details) {
                if (!matchesQaItem(entry.getValue(), detail)
                        || !matchesQaEquipment(entry.getValue(), detail,
                        equipmentByItem.getOrDefault(entry.getKey(), List.of()))
                        || !matchesResult(entry.getValue(), detail)) {
                    return entry.getKey();
                }
            }
        }
        String expectedInspectionResult = source.getAggregateDetails().stream()
                .allMatch(detail -> MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS
                        .equals(detail.getJudgement()))
                ? MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS
                : MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;
        return Objects.equals(expectedInspectionResult, source.getPqcRecord().getInspectionResult())
                ? null : source.getAggregateDetails().get(0).getItemCode();
    }

    private boolean sampleNumbersComplete(MesPqcInspectionTaskDO task,
                                          List<MesPqcProcessInspectionAggregateDetailDO> details) {
        if (task.getActualInspectionQuantity() == null || task.getActualInspectionQuantity() <= 0) {
            return false;
        }
        List<Integer> expected = IntStream.rangeClosed(1, task.getActualInspectionQuantity()).boxed().toList();
        List<Integer> actual = details.stream().map(MesPqcProcessInspectionAggregateDetailDO::getSampleNo)
                .filter(Objects::nonNull).distinct().sorted().toList();
        return expected.equals(actual);
    }

    private boolean matchesQaItem(MesQaInspectionRegulationItemDO item,
                                  MesPqcProcessInspectionAggregateDetailDO detail) {
        return Objects.equals(item.getItemCode(), detail.getItemCode())
                && Objects.equals(item.getItemName(), detail.getItemName())
                && Objects.equals(item.getInspectionMethod(), detail.getInspectionMethod())
                && Objects.equals(item.getStandardText(), detail.getStandardText())
                && decimalEquals(item.getStandardLowerLimit(), detail.getStandardLowerLimit())
                && decimalEquals(item.getStandardUpperLimit(), detail.getStandardUpperLimit())
                && Objects.equals(item.getStandardUnit(), detail.getStandardUnit())
                && Objects.equals(item.getStandardPrecision(), detail.getStandardPrecision())
                && Objects.equals(item.getResultType(), detail.getResultType());
    }

    private boolean matchesQaEquipment(MesQaInspectionRegulationItemDO item,
                                       MesPqcProcessInspectionAggregateDetailDO detail,
                                       List<MesQaInspectionRegulationItemEquipmentDO> equipment) {
        boolean selected = detail.getSelectedEquipmentId() != null
                || StrUtil.isNotBlank(detail.getSelectedEquipmentNumber());
        if (Boolean.TRUE.equals(item.getEquipmentRequired()) && !selected) {
            return false;
        }
        if (!selected) {
            return true;
        }
        return equipment.stream().anyMatch(option -> Objects.equals(option.getEquipmentId(), detail.getSelectedEquipmentId())
                && Objects.equals(option.getEquipmentCode(), detail.getSelectedEquipmentCode())
                && Objects.equals(option.getEquipmentName(), detail.getSelectedEquipmentName())
                && Objects.equals(option.getEquipmentNumber(), detail.getSelectedEquipmentNumber()));
    }

    private boolean matchesResult(MesQaInspectionRegulationItemDO item,
                                  MesPqcProcessInspectionAggregateDetailDO detail) {
        if (StrUtil.hasBlank(detail.getItemResult(), detail.getMeasuredValue(), detail.getJudgement())
                || !Objects.equals(detail.getItemResult(), detail.getMeasuredValue())) {
            return false;
        }
        String expected = expectedJudgement(item, detail.getMeasuredValue());
        return Objects.equals(expected, detail.getJudgement());
    }

    private String expectedJudgement(MesQaInspectionRegulationItemDO item, String measuredValue) {
        try {
            return PqcResultValueValidator.validate(item.getResultType(), measuredValue,
                    item.getStandardLowerLimit(), item.getStandardUpperLimit(),
                    item.getStandardPrecision()).judgement();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private MesProRouteFlowProcessBatchRecordDO formalBinding(
            MesPqcInspectionTaskDO task, List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        List<MesProRouteFlowProcessBatchRecordDO> bindings = bindingMapper
                .selectListByRouteProcessIdsAndUseType(List.of(task.getRouteProcessId()), USE_TYPE_BATCH);
        bindings = bindings == null ? List.of() : bindings;
        List<MesProRouteFlowProcessBatchRecordDO> matches = bindings.stream()
                .filter(binding -> binding != null && binding.getId() != null
                        && Objects.equals(task.getRouteId(), binding.getRouteId())
                        && Objects.equals(task.getRouteProcessId(), binding.getRouteProcessId())
                        && StrUtil.isNotBlank(binding.getBatchRecordReportId())
                        && binding.getBatchRecordDefinitionId() != null && binding.getBatchRecordVersionId() != null
                        && FORM_SLOT_TYPE.equals(binding.getFormSlotType())
                        && RECORD_CATEGORY.equals(binding.getRecordCategory())
                        && VALIDATION_PROFILE.equals(binding.getValidationProfile())
                        && OWNER_ROLE.equals(binding.getOwnerRoleKey())
                        && binding.getFormTemplateId() == null && StrUtil.isBlank(binding.getFormBindingKey())
                        && StrUtil.isNotBlank(binding.getRecordCategorySnapshotHash())
                        && StrUtil.isNotBlank(binding.getSlotConfigSnapshotHash()))
                .toList();
        List<MesProRouteFlowProcessBatchRecordDO> dynamicMatches = bindings.stream()
                .filter(binding -> binding != null && binding.getId() != null
                        && Objects.equals(task.getRouteId(), binding.getRouteId())
                        && Objects.equals(task.getRouteProcessId(), binding.getRouteProcessId())
                        && StrUtil.isBlank(binding.getBatchRecordReportId())
                        && binding.getBatchRecordDefinitionId() == null && binding.getBatchRecordVersionId() == null
                        && FORM_SLOT_TYPE.equals(binding.getFormSlotType())
                        && RECORD_CATEGORY.equals(binding.getRecordCategory())
                        && VALIDATION_PROFILE.equals(binding.getValidationProfile())
                        && OWNER_ROLE.equals(binding.getOwnerRoleKey())
                        && PROCESS_INSPECTION_FORM_TEMPLATE_ID.equals(binding.getFormTemplateId())
                        && StrUtil.isNotBlank(binding.getFormBindingKey())
                        && binding.getLastPublishedTemplateVersionId() != null
                        && StrUtil.isNotBlank(binding.getLastPublishedTemplateVersionNo())
                        && StrUtil.isNotBlank(binding.getRecordCategorySnapshotHash())
                        && StrUtil.isNotBlank(binding.getSlotConfigSnapshotHash()))
                .toList();
        if (matches.size() + dynamicMatches.size() != 1) {
            blockers.add(blocker("PROCESS_INSPECTION_REPORT_BINDING_REQUIRED", "ROUTE_PROCESS",
                    task.getRouteProcessId(), null,
                    "工序必须存在唯一有效 PROCESS_INSPECTION 目标绑定，传统数量=" + matches.size()
                            + "，动态数量=" + dynamicMatches.size(),
                    "请维护唯一的传统报表绑定或 template 28 已发布动态表单绑定"));
            return null;
        }
        return matches.isEmpty() ? dynamicMatches.get(0) : matches.get(0);
    }

    private MappingResolution resolveMappings(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source,
            MesProRouteFlowProcessBatchRecordDO binding,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        boolean dynamic = isDynamicBinding(binding);
        String scopeType = dynamic ? "FORM_TEMPLATE_VERSION" : SCOPE_TYPE_ROUTE_VERSION;
        Long scopeId = dynamic ? binding.getLastPublishedTemplateVersionId() : binding.getBatchRecordVersionId();
        String targetReportId = dynamic ? "FORMTPL:" + binding.getLastPublishedTemplateVersionId()
                : binding.getBatchRecordReportId();
        List<MesProBatchRecordCellLinkRuleDO> rawRules = ruleMapper.selectEnabledListByScopeAndTargetReport(
                scopeType, scopeId, targetReportId);
        List<MesProBatchRecordCellLinkRuleDO> rules = filterWriterRules(rawRules, dynamic);
        Map<String, SourceValue> available = dynamic ? dynamicSummarySourceValues(command, source)
                : sourceValues(source);
        Set<String> required = dynamic ? dynamicSummarySourceKeys() : requiredSourceKeys(source);
        Map<String, MesProBatchRecordCellLinkRuleDO> ruleByKey = new LinkedHashMap<>();
        Set<String> targetCells = new LinkedHashSet<>();
        String invalidField = null;
        if (rules == null || rules.isEmpty()) {
            invalidField = "*";
        } else {
            for (MesProBatchRecordCellLinkRuleDO rule : rules) {
                String sourceKey = rule == null ? null : rule.getSourceCellKey();
                String targetKey = rule == null ? null : cellKey(rule.getTargetRowIndex(), rule.getTargetColumnIndex());
                if (rule == null || rule.getId() == null || !Boolean.TRUE.equals(rule.getEnabled())
                        || !Objects.equals(scopeType, rule.getScopeType())
                        || !Objects.equals(scopeId, rule.getScopeId())
                        || !SOURCE_TYPE.equals(StrUtil.trim(rule.getSourceType()))
                        || StrUtil.isBlank(rule.getSourceFieldCode()) || StrUtil.isBlank(sourceKey)
                        || !available.containsKey(sourceKey)
                        || !Objects.equals(available.get(sourceKey).fieldCode(), rule.getSourceFieldCode())
                        || !Objects.equals(targetReportId, rule.getTargetReportId())
                        || rule.getTargetRowIndex() == null || rule.getTargetColumnIndex() == null
                        || !Objects.equals(targetKey, rule.getTargetCellKey())
                        || StrUtil.isBlank(rule.getTargetValueType())
                        || StrUtil.isBlank(rule.getTemplateSnapshotHash()) || rule.getRuleVersion() == null
                        || ruleByKey.putIfAbsent(sourceKey, rule) != null || !targetCells.add(targetKey)) {
                    invalidField = rule == null ? "*" : StrUtil.blankToDefault(rule.getSourceFieldCode(), "*");
                    break;
                }
            }
        }
        if (invalidField == null) {
            invalidField = required.stream().filter(key -> !ruleByKey.containsKey(key))
                    .map(key -> available.get(key).fieldCode()).findFirst().orElse(null);
        }
        if (invalidField != null) {
            blockers.add(blocker("PROCESS_INSPECTION_MAPPING_REQUIRED", "ROUTE_PROCESS",
                    source.getTask().getRouteProcessId(), invalidField,
                    "过程检验单缺少完整唯一的 PQC_AGGREGATE_DETAIL 启用映射",
                    "请按发布 QA 项目和签名元数据配置目标单元格映射"));
            return null;
        }
        List<MesProBatchRecordCellLinkRuleDO> orderedRules = rules.stream()
                .sorted(Comparator.comparing(MesProBatchRecordCellLinkRuleDO::getTargetRowIndex)
                        .thenComparing(MesProBatchRecordCellLinkRuleDO::getTargetColumnIndex)
                        .thenComparing(MesProBatchRecordCellLinkRuleDO::getId))
                .toList();
        MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.TargetResolution dynamicTarget = null;
        if (dynamic) {
            dynamicTarget = dynamicFormPort.resolveTarget(binding, orderedRules, source.getRouteProjectCode());
            if (dynamicTarget == null || !dynamicTarget.isValid()) {
                String blockerType = dynamicTarget == null || StrUtil.isBlank(dynamicTarget.getBlockerType())
                        ? "PROCESS_INSPECTION_DYNAMIC_FORM_TEMPLATE_REQUIRED" : dynamicTarget.getBlockerType();
                String message = dynamicTarget == null || StrUtil.isBlank(dynamicTarget.getBlockerMessage())
                        ? "过程检验动态模板目标解析失败" : dynamicTarget.getBlockerMessage();
                blockers.add(blocker(blockerType, "ROUTE_PROCESS_FORM_BINDING", binding.getId(),
                        binding.getFormBindingKey(), message,
                        "请配置精确的 PUBLISHED template 28 版本和稳定 fieldCode 映射"));
                return null;
            }
        }
        MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.TargetResolution resolvedTarget = dynamicTarget;
        List<MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.MappedValue> values = orderedRules.stream()
                .map(rule -> {
                    SourceValue value = available.get(rule.getSourceCellKey());
                    return new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.MappedValue()
                            .setRule(rule).setValue(value.value()).setDisplayValue(displayValue(value.value()))
                            .setTargetFieldCode(resolvedTarget == null ? null
                                    : resolvedTarget.getTargetFieldCodes().get(rule.getId()))
                            .setSourceValueHash(hashMappedValue(rule, value));
                })
                .toList();
        return new MappingResolution(List.copyOf(orderedRules), values, resolvedTarget);
    }

    private Map<String, SourceValue> sourceValues(
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source) {
        Map<String, SourceValue> values = new LinkedHashMap<>();
        for (MesPqcProcessInspectionAggregateDetailDO detail : source.getAggregateDetails().stream()
                .sorted(Comparator.comparing(MesPqcProcessInspectionAggregateDetailDO::getItemCode)
                        .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getSampleNo)
                        .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getId)).toList()) {
            putItemValues(values, detail);
        }
        MesPqcInspectionTaskDO task = source.getTask();
        put(values, headerSourceKey(task.getInspectionType(), "inspectorUserId"), "inspectorUserId",
                source.getEvent().getSignatureUserId());
        put(values, headerSourceKey(task.getInspectionType(), "inspectedAt"), "inspectedAt",
                source.getEvent().getServerSubmitTime());
        put(values, headerSourceKey(task.getInspectionType(), "reviewerUserId"), "reviewerUserId",
                source.getReview().getReviewSignatureUserId());
        put(values, headerSourceKey(task.getInspectionType(), "reviewedAt"), "reviewedAt",
                source.getReview().getReviewedAt());
        put(values, dccSourceKey(task.getInspectionType(), "dccProjectId"), "dccProjectId",
                source.getDccProject().getId());
        put(values, dccSourceKey(task.getInspectionType(), "dccProjectCode"), "dccProjectCode",
                source.getDccProject().getProjectCode());
        put(values, dccSourceKey(task.getInspectionType(), "dccProjectName"), "dccProjectName",
                source.getDccProject().getProjectName());
        return values;
    }

    private List<MesProBatchRecordCellLinkRuleDO> filterWriterRules(List<MesProBatchRecordCellLinkRuleDO> rules,
                                                                   boolean dynamic) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        if (!dynamic) {
            return rules;
        }
        return rules.stream()
                .filter(Objects::nonNull)
                .filter(rule -> SOURCE_TYPE.equals(StrUtil.trim(rule.getSourceType())))
                .toList();
    }

    private Map<String, SourceValue> dynamicSummarySourceValues(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source) {
        Map<String, SourceValue> values = new LinkedHashMap<>();
        MesPqcInspectionTaskDO task = source.getTask();
        put(values, summarySourceKey("dccProjectCode"), "dccProjectCode",
                source.getDccProject().getProjectCode());
        put(values, summarySourceKey("dccProjectName"), "dccProjectName",
                source.getDccProject().getProjectName());
        put(values, summarySourceKey("qaVersionNo"), "qaVersionNo",
                join(source.getRegulation().getRegulationCode(), source.getRegulationVersion().getVersionNo(),
                        source.getRegulationVersion().getPublishedAt()));
        put(values, summarySourceKey("itemSummary"), "itemSummary", itemSummary(source));
        put(values, summarySourceKey("resultSummary"), "resultSummary", resultSummary(source));
        put(values, summarySourceKey("equipmentSummary"), "equipmentSummary", equipmentSummary(source));
        put(values, summarySourceKey("overallJudgement"), "overallJudgement",
                source.getPqcRecord().getInspectionResult());
        put(values, summarySourceKey("inspectorSignedInfo"), "inspectorSignedInfo",
                signatureSummary(task.getId(), source.getEvent().getSignatureUserId(),
                        source.getEvent().getSignatureId(), source.getEvent().getServerSubmitTime()));
        put(values, summarySourceKey("reviewerSignedInfo"), "reviewerSignedInfo",
                signatureSummary(source.getReview().getId(), source.getReview().getReviewSignatureUserId(),
                        source.getReview().getReviewSignatureId(), source.getReview().getReviewedAt()));
        return values;
    }

    private void putItemValues(Map<String, SourceValue> values,
                               MesPqcProcessInspectionAggregateDetailDO detail) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("itemCode", detail.getItemCode());
        fields.put("itemName", detail.getItemName());
        fields.put("inspectionMethod", detail.getInspectionMethod());
        fields.put("standardText", detail.getStandardText());
        fields.put("standardLowerLimit", detail.getStandardLowerLimit());
        fields.put("standardUpperLimit", detail.getStandardUpperLimit());
        fields.put("standardUnit", detail.getStandardUnit());
        fields.put("standardPrecision", detail.getStandardPrecision());
        fields.put("resultType", detail.getResultType());
        fields.put("measuredValue", mappedMeasuredValue(detail));
        fields.put("judgement", detail.getJudgement());
        fields.put("selectedEquipmentId", detail.getSelectedEquipmentId());
        fields.put("selectedEquipmentNumber", detail.getSelectedEquipmentNumber());
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            put(values, itemSourceKey(detail, entry.getKey()), entry.getKey(), entry.getValue());
        }
    }

    private Object mappedMeasuredValue(MesPqcProcessInspectionAggregateDetailDO detail) {
        return switch (detail.getResultType()) {
            case "NUMERIC" -> new BigDecimal(detail.getMeasuredValue());
            case "BOOLEAN", "TEXT" -> detail.getMeasuredValue();
            default -> throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "过程检验实测值类型不受支持，itemCode=" + detail.getItemCode()
                            + "，resultType=" + detail.getResultType());
        };
    }

    private Set<String> requiredSourceKeys(
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source) {
        LinkedHashSet<String> required = new LinkedHashSet<>();
        Map<String, MesQaInspectionRegulationItemDO> itemByCode = source.getRegulationItems().stream()
                .filter(item -> Objects.equals(source.getTask().getInspectionType(), item.getInspectionType()))
                .collect(Collectors.toMap(MesQaInspectionRegulationItemDO::getItemCode, Function.identity()));
        for (MesPqcProcessInspectionAggregateDetailDO detail : source.getAggregateDetails()) {
            for (String field : List.of("itemCode", "itemName", "inspectionMethod", "standardText",
                    "resultType", "measuredValue", "judgement")) {
                required.add(itemSourceKey(detail, field));
            }
            if (detail.getStandardLowerLimit() != null) {
                required.add(itemSourceKey(detail, "standardLowerLimit"));
            }
            if (detail.getStandardUpperLimit() != null) {
                required.add(itemSourceKey(detail, "standardUpperLimit"));
            }
            if (StrUtil.isNotBlank(detail.getStandardUnit())) {
                required.add(itemSourceKey(detail, "standardUnit"));
            }
            if (detail.getStandardPrecision() != null) {
                required.add(itemSourceKey(detail, "standardPrecision"));
            }
            MesQaInspectionRegulationItemDO item = itemByCode.get(detail.getItemCode());
            if (item != null && Boolean.TRUE.equals(item.getEquipmentRequired())) {
                required.add(itemSourceKey(detail, "selectedEquipmentId"));
                required.add(itemSourceKey(detail, "selectedEquipmentNumber"));
            }
        }
        for (String field : List.of("inspectorUserId", "inspectedAt", "reviewerUserId", "reviewedAt")) {
            required.add(headerSourceKey(source.getTask().getInspectionType(), field));
        }
        for (String field : List.of("dccProjectId", "dccProjectCode", "dccProjectName")) {
            required.add(dccSourceKey(source.getTask().getInspectionType(), field));
        }
        return required;
    }

    private Set<String> dynamicSummarySourceKeys() {
        return DYNAMIC_SUMMARY_FIELDS.stream()
                .map(this::summarySourceKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String itemSummary(MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source) {
        return source.getAggregateDetails().stream()
                .sorted(Comparator.comparing(MesPqcProcessInspectionAggregateDetailDO::getItemCode)
                        .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getSampleNo)
                        .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getId))
                .map(detail -> join(detail.getItemCode(), detail.getItemName(), detail.getInspectionMethod(),
                        detail.getStandardText(), mappedMeasuredValue(detail), detail.getJudgement()))
                .collect(Collectors.joining(";"));
    }

    private String resultSummary(MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source) {
        Map<String, Long> counts = source.getAggregateDetails().stream()
                .collect(Collectors.groupingBy(MesPqcProcessInspectionAggregateDetailDO::getJudgement,
                        LinkedHashMap::new, Collectors.counting()));
        return join("quantity=" + source.getTask().getActualInspectionQuantity(),
                counts.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(",")));
    }

    private String equipmentSummary(MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source) {
        String summary = source.getAggregateDetails().stream()
                .filter(detail -> detail.getSelectedEquipmentId() != null
                        || StrUtil.isNotBlank(detail.getSelectedEquipmentNumber()))
                .sorted(Comparator.comparing(MesPqcProcessInspectionAggregateDetailDO::getItemCode)
                        .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getSampleNo)
                        .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getId))
                .map(detail -> join(detail.getItemCode(), detail.getSelectedEquipmentId(),
                        detail.getSelectedEquipmentCode(), detail.getSelectedEquipmentName(),
                        detail.getSelectedEquipmentNumber()))
                .collect(Collectors.joining(";"));
        return StrUtil.blankToDefault(summary, "NO_EQUIPMENT_REQUIRED");
    }

    private String signatureSummary(Long sourceId, Long userId, Long signatureId, LocalDateTime signedAt) {
        return join("sourceId=" + sourceId, "userId=" + userId, "signatureId=" + signatureId,
                "signedAt=" + displayValue(signedAt));
    }

    private List<String> collectEvidence(
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source,
            MesProRouteFlowProcessBatchRecordDO binding,
            List<MesProBatchRecordCellLinkRuleDO> rules,
            Set<Long> sourceObjectIds,
            List<MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence> signatures) {
        List<String> hashes = new ArrayList<>();
        MesPqcInspectionTaskDO task = source.getTask();
        addEvidence(sourceObjectIds, hashes, task.getId(), hashTask(task));
        for (MesPqcProcessInspectionAggregateDetailDO detail : source.getAggregateDetails().stream()
                .sorted(Comparator.comparing(MesPqcProcessInspectionAggregateDetailDO::getId)).toList()) {
            addEvidence(sourceObjectIds, hashes, detail.getId(), hashAggregate(detail));
            sourceObjectIds.add(detail.getSourcePieceDetailId());
        }
        String eventHash = hashEvent(source.getEvent());
        addEvidence(sourceObjectIds, hashes, source.getEvent().getId(), eventHash);
        addEvidence(sourceObjectIds, hashes, source.getPqcRecord().getId(), hashRecord(source.getPqcRecord()));
        String reviewHash = hashReview(source.getReview());
        addEvidence(sourceObjectIds, hashes, source.getReview().getId(), reviewHash);
        addEvidence(sourceObjectIds, hashes, source.getRegulation().getId(), hashRegulation(source.getRegulation()));
        addEvidence(sourceObjectIds, hashes, source.getRegulationVersion().getId(),
                hashRegulationVersion(source.getRegulationVersion()));
        addEvidence(sourceObjectIds, hashes, source.getDccProject().getId(), hashDccProject(source.getDccProject()));
        hashes.add(hashQaDccProvenance(source.getQaDccProvenance()));
        for (MesQaInspectionRegulationItemDO item : source.getRegulationItems().stream()
                .filter(item -> Objects.equals(task.getInspectionType(), item.getInspectionType()))
                .sorted(Comparator.comparing(MesQaInspectionRegulationItemDO::getItemCode)).toList()) {
            addEvidence(sourceObjectIds, hashes, item.getId(), hashQaItem(item));
        }
        for (MesQaInspectionRegulationItemEquipmentDO equipment : source.getRegulationItemEquipment().stream()
                .filter(item -> Objects.equals(task.getInspectionType(), item.getInspectionType()))
                .sorted(Comparator.comparing(MesQaInspectionRegulationItemEquipmentDO::getItemCode)
                        .thenComparing(MesQaInspectionRegulationItemEquipmentDO::getEquipmentId)).toList()) {
            addEvidence(sourceObjectIds, hashes, equipment.getId(), hashEquipment(equipment));
        }
        addEvidence(sourceObjectIds, hashes, binding.getId(), hashBinding(binding));
        for (MesProBatchRecordCellLinkRuleDO rule : rules) {
            addEvidence(sourceObjectIds, hashes, rule.getId(), hashRule(rule));
        }
        signatures.add(signature("FILLER", "PQC_SUBMIT", source.getEvent().getId(),
                source.getEvent().getSignatureId(), source.getEvent().getSignatureUserId(),
                source.getEvent().getServerSubmitTime(), eventHash));
        signatures.add(signature("REVIEWER", "PQC_LEADER_REVIEW", source.getReview().getId(),
                source.getReview().getReviewSignatureId(), source.getReview().getReviewSignatureUserId(),
                source.getReview().getReviewedAt(), reviewHash));
        return List.copyOf(hashes);
    }

    private MesProEdhrBatchExecutionTaskDO requireCurrentBatchTask(
            Long batchExecutionId, List<MesProEdhrBatchExecutionTaskDO> tasks,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection inspection) {
        MesPqcInspectionTaskDO sourceTask = inspection.getSource().getTask();
        MesProRouteFlowProcessBatchRecordDO binding = inspection.getBinding();
        boolean dynamic = isDynamicBinding(binding);
        List<MesProEdhrBatchExecutionTaskDO> matches = tasks == null ? List.of() : tasks.stream()
                .filter(task -> task != null && task.getId() != null
                        && Objects.equals(batchExecutionId, task.getBatchExecutionId())
                        && Objects.equals(sourceTask.getRouteProcessId(), task.getRouteProcessId())
                        && Objects.equals(sourceTask.getProcessId(), task.getProcessId())
                        && Objects.equals(binding.getId(), task.getRouteBindingId())
                        && FORM_SLOT_TYPE.equals(task.getFormSlotType())
                        && RECORD_CATEGORY.equals(task.getRecordCategory())
                        && VALIDATION_PROFILE.equals(task.getValidationProfile())
                        && OWNER_ROLE.equals(task.getOwnerRoleKey())
                        && StrUtil.isNotBlank(task.getRouteBindingSnapshotHash())
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
                    "当前 eDHR 批次缺少唯一 PROCESS_INSPECTION 正式目标任务，batchExecutionId="
                            + batchExecutionId + "，routeProcessId=" + sourceTask.getRouteProcessId());
        }
        return matches.get(0);
    }

    private MesProBatchRecordExecutionOpenOrCreateByContextReqVO toOpenRequest(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
            Long batchExecutionId, MesProEdhrBatchExecutionTaskDO task) {
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
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
            Long batchExecutionId, MesProEdhrBatchExecutionTaskDO task,
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
                    "过程检验 execution 不属于当前批次传统报表任务，batchTaskId=" + task.getId());
        }
    }

    private List<MesProBatchRecordExecutionFieldAuditChange> toAuditChanges(
            MesProBatchRecordExecutionDO execution,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection inspection) {
        Map<String, SnapshotField> fields = snapshotFields(execution.getExecutionSnapshotJson());
        List<MesProBatchRecordExecutionFieldAuditChange> changes = new ArrayList<>();
        for (MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.MappedValue mapped
                : inspection.getMappedValues()) {
            MesProBatchRecordCellLinkRuleDO rule = mapped.getRule();
            SnapshotField field = fields.get(cellKey(rule.getTargetRowIndex(), rule.getTargetColumnIndex()));
            if (field == null || !Objects.equals(rule.getTargetValueType(), field.valueType().name())) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                        "过程检验映射目标字段与 execution 快照不一致，cell=" + rule.getTargetCellKey());
            }
            Object value = auditValue(field.valueType(), mapped.getValue());
            changes.add(new MesProBatchRecordExecutionFieldAuditChange()
                    .setFieldPath(field.fieldPath())
                    .setFieldKey(field.fieldKey())
                    .setRowIndex(rule.getTargetRowIndex())
                    .setColumnIndex(rule.getTargetColumnIndex())
                    .setValueType(field.valueType())
                    .setNewValueJson(value)
                    .setNewValueDisplay(displayValue(value)));
        }
        return List.copyOf(changes);
    }

    private Map<String, SnapshotField> snapshotFields(String snapshotJson) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(snapshotJson);
            JsonNode fields = root.path("fields");
            if (!fields.isArray()) {
                throw new IllegalArgumentException("fields missing");
            }
            Map<String, SnapshotField> result = new LinkedHashMap<>();
            for (JsonNode field : fields) {
                Integer row = integer(field, "rowIndex");
                Integer column = integer(field, "columnIndex");
                String fieldPath = text(field, "fieldPath");
                String fieldKey = text(field, "fieldKey");
                String valueType = text(field, "valueType");
                if (row == null || column == null || StrUtil.hasBlank(fieldPath, fieldKey, valueType)) {
                    continue;
                }
                SnapshotField existing = result.putIfAbsent(cellKey(row, column),
                        new SnapshotField(fieldPath, fieldKey,
                                MesProBatchRecordExecutionFieldAuditValueType.valueOf(valueType)));
                if (existing != null) {
                    throw new IllegalArgumentException("duplicate cell");
                }
            }
            return result;
        } catch (Exception ex) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "过程检验 execution 字段快照无法解析，禁止写入");
        }
    }

    private Object auditValue(MesProBatchRecordExecutionFieldAuditValueType valueType, Object value) {
        if (value == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "过程检验映射来源值为空，禁止写入");
        }
        return switch (valueType) {
            case NUMBER -> value instanceof BigDecimal decimal ? decimal
                    : new BigDecimal(String.valueOf(value));
            case DATETIME -> value instanceof LocalDateTime time ? DATETIME_FORMATTER.format(time)
                    : String.valueOf(value);
            case DATE -> value instanceof LocalDate date ? date.toString() : String.valueOf(value);
            case BOOLEAN -> value instanceof Boolean bool ? bool : Boolean.valueOf(String.valueOf(value));
            case STRING -> String.valueOf(value);
            default -> throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "过程检验映射不支持目标值类型=" + valueType);
        };
    }

    private String auditIdempotencyKey(MesProEdhrBatchExecutionTaskDO batchTask,
                                       MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
                                       MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection inspection) {
        return "AO_RELEASE_PQC_AGGREGATE_DETAIL:" + batchTask.getId() + ":"
                + sha256(command.getSourceSnapshotHash() + "|" + inspection.getEvidenceHash());
    }

    private String auditReason(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection inspection) {
        MesProProcessPoolEventDO event = inspection.getSource().getEvent();
        MesProcessPoolSubmissionReviewDO review = inspection.getSource().getReview();
        return "CONFIRMED PQC 汇集自动生成正式过程检验单；pqcTaskId="
                + inspection.getSource().getTask().getId()
                + "；pqcSignatureId=" + event.getSignatureId()
                + "；pqcReviewSignatureId=" + review.getReviewSignatureId();
    }

    private String hashTask(MesPqcInspectionTaskDO task) {
        return sha256(join("PQC_TASK_V1", task.getTenantId(), task.getId(), task.getActiveOrderId(),
                task.getWorkOrderId(), task.getRouteId(), task.getRouteVersionId(), task.getRouteProcessId(),
                task.getProcessId(), task.getRegulationVersionId(), task.getInspectionType(), task.getBusinessDate(),
                task.getShiftCode(), task.getRoundNo(), task.getPlannedInspectionQuantity(),
                task.getActualInspectionQuantity()));
    }

    private String hashAggregate(MesPqcProcessInspectionAggregateDetailDO detail) {
        return sha256(join("PQC_AGGREGATE_DETAIL_V1", detail.getTenantId(), detail.getId(),
                detail.getSourcePqcRecordId(), detail.getSourcePieceDetailId(), detail.getEventId(),
                detail.getReviewId(), detail.getProductionSubmitEventId(), detail.getPqcTaskId(),
                detail.getActiveOrderId(), detail.getWorkOrderId(), detail.getRouteId(), detail.getRouteVersionId(),
                detail.getRouteProcessId(), detail.getProcessId(), detail.getRegulationVersionId(),
                detail.getInspectionType(), detail.getBusinessDate(), detail.getShiftCode(), detail.getRoundNo(),
                detail.getActualInspectionQuantity(), detail.getSampleNo(), detail.getItemCode(), detail.getItemName(),
                detail.getInspectionMethod(), detail.getStandardText(), detail.getSelectedEquipmentId(),
                detail.getSelectedEquipmentCode(), detail.getSelectedEquipmentName(), detail.getSelectedEquipmentNumber(),
                detail.getStandardLowerLimit(), detail.getStandardUpperLimit(), detail.getStandardUnit(),
                detail.getStandardPrecision(), detail.getResultType(), detail.getItemResult(),
                detail.getMeasuredValue(), detail.getJudgement(), detail.getAggregatedAt()));
    }

    private String hashEvent(MesProProcessPoolEventDO event) {
        return sha256(join("PQC_EVENT_V1", event.getTenantId(), event.getId(), event.getEventType(),
                event.getWorkOrderId(), event.getRouteId(), event.getRouteProcessId(), event.getProcessId(),
                event.getActualEmployeeId(), event.getFeedbackSourceType(), event.getFeedbackSourceId(),
                event.getServerSubmitTime(), event.getSignatureId(), event.getSignatureUserId(),
                event.getSignatureSnapshot()));
    }

    private String hashRecord(MesProProcessPoolPqcRecordDO record) {
        return sha256(join("PQC_RECORD_V1", record.getTenantId(), record.getId(), record.getEventId(),
                record.getProductionSubmitEventId(), record.getWorkOrderId(), record.getRouteId(),
                record.getRouteProcessId(), record.getProcessId(), record.getActualEmployeeId(),
                record.getSignatureId(), record.getSignatureUserId(), record.getInspectionResult(),
                record.getServerSubmitTime(), record.getProcessInspectionReviewId()));
    }

    private String hashReview(MesProcessPoolSubmissionReviewDO review) {
        return sha256(join("PQC_REVIEW_V1", review.getTenantId(), review.getId(), review.getEventId(),
                review.getLeaderUserId(), review.getLeaderType(), review.getReviewedAt(),
                review.getReviewSignatureId(), review.getReviewSignatureUserId(),
                review.getReviewSignatureSnapshotJson()));
    }

    private String hashRegulation(MesQaInspectionRegulationDO regulation) {
        return sha256(join("QA_REGULATION_V1", regulation.getTenantId(), regulation.getId(),
                regulation.getProductId(), regulation.getRouteId(), regulation.getRouteVersionId(),
                regulation.getRouteProcessId(), regulation.getProcessId(), regulation.getOwnerModule(),
                regulation.getRegulationCode(), regulation.getRegulationName(), regulation.getCurrentVersionId()));
    }

    private String hashRegulationVersion(MesQaInspectionRegulationVersionDO version) {
        return sha256(join("QA_REGULATION_VERSION_V1", version.getTenantId(), version.getId(),
                version.getRegulationId(), version.getVersionNo(), version.getPublishedAt(), version.getSnapshotJson()));
    }

    private String hashQaItem(MesQaInspectionRegulationItemDO item) {
        return sha256(join("QA_ITEM_V1", item.getTenantId(), item.getId(), item.getRegulationVersionId(),
                item.getInspectionType(), item.getItemCode(), item.getItemName(), item.getInspectionMethod(),
                item.getStandardText(), item.getStandardLowerLimit(), item.getStandardUpperLimit(),
                item.getStandardUnit(), item.getStandardPrecision(), item.getEquipmentRequired(),
                item.getResultType(), item.getFirstInspectionQuantity(), item.getPatrolInspectionRatio()));
    }

    private String hashEquipment(MesQaInspectionRegulationItemEquipmentDO equipment) {
        return sha256(join("QA_ITEM_EQUIPMENT_V1", equipment.getTenantId(), equipment.getId(),
                equipment.getRegulationVersionId(), equipment.getInspectionType(), equipment.getItemCode(),
                equipment.getEquipmentId(), equipment.getEquipmentCode(), equipment.getEquipmentName(),
                equipment.getEquipmentNumber(), equipment.getDefaultFlag(), equipment.getSort()));
    }

    private String hashBinding(MesProRouteFlowProcessBatchRecordDO binding) {
        return sha256(join("PROCESS_INSPECTION_BINDING_V1", binding.getId(), binding.getRouteId(),
                binding.getRouteProcessId(), binding.getUseType(), binding.getBatchRecordReportId(),
                binding.getBatchRecordDefinitionId(), binding.getBatchRecordVersionId(), binding.getFormSlotType(),
                binding.getFormBindingKey(), binding.getFormTemplateId(), binding.getLastPublishedTemplateVersionId(),
                binding.getLastPublishedTemplateVersionNo(),
                binding.getRecordCategory(), binding.getValidationProfile(), binding.getOwnerRoleKey(),
                binding.getRecordCategorySnapshotHash(), binding.getSlotConfigSnapshotHash()));
    }

    private String hashRule(MesProBatchRecordCellLinkRuleDO rule) {
        return sha256(join("PQC_MAPPING_RULE_V1", rule.getId(), rule.getScopeType(), rule.getScopeId(),
                rule.getRouteId(), rule.getBatchRecordDefinitionId(), rule.getBatchRecordVersionId(),
                rule.getSourceType(), rule.getSourceCellKey(), rule.getSourceFieldCode(), rule.getSourceValueType(),
                rule.getTargetReportId(), rule.getTargetRowIndex(), rule.getTargetColumnIndex(),
                rule.getTargetCellKey(), rule.getTargetValueType(), rule.getAggregationStrategy(),
                rule.getOverwritePolicy(), rule.getTemplateSnapshotHash(), rule.getRuleVersion(), rule.getEnabled()));
    }

    private String hashDccProject(DccProjectCodeDO project) {
        return sha256(join("DCC_PROJECT_PRODUCT_IDENTITY_V1", project.getTenantId(), project.getId(),
                project.getProductMasterId(), project.getProjectCode(), project.getProjectName()));
    }

    private String hashQaDccProvenance(
            MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution provenance) {
        return sha256(join("DCC_QA_PROVENANCE_V1", provenance.getDccProjectCodeId(),
                provenance.getRegulationId(), provenance.getRegulationVersionId(), provenance.getProvenanceType(),
                provenance.getProvenanceId(), provenance.getProvenanceSnapshotHash()));
    }

    private String hashMappedValue(MesProBatchRecordCellLinkRuleDO rule, SourceValue value) {
        return sha256(join("PQC_MAPPED_FIELD_VALUE_V1", rule.getSourceCellKey(), rule.getSourceFieldCode(),
                value.fieldCode(), value.value()));
    }

    private MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.WriteCommand toDynamicWriteCommand(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan,
            Long batchExecutionId,
            MesProEdhrBatchExecutionTaskDO batchTask,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection inspection) {
        List<MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.FieldWrite> fields =
                inspection.getMappedValues().stream()
                        .map(mapped -> new MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.FieldWrite()
                                .setRuleId(mapped.getRule().getId())
                                .setRuleVersion(mapped.getRule().getRuleVersion())
                                .setSourceCellKey(mapped.getRule().getSourceCellKey())
                                .setSourceFieldCode(mapped.getRule().getSourceFieldCode())
                                .setTargetFieldCode(mapped.getTargetFieldCode())
                                .setValue(auditValue(
                                        MesProBatchRecordExecutionFieldAuditValueType.valueOf(
                                                mapped.getRule().getTargetValueType()),
                                        mapped.getValue()))
                                .setDisplayValue(mapped.getDisplayValue())
                                .setSourceValueHash(mapped.getSourceValueHash()))
                        .toList();
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.WriteCommand()
                .setTenantId(plan.getCommand().getTenantId())
                .setBatchExecutionId(batchExecutionId)
                .setBatchTask(batchTask)
                .setBinding(inspection.getBinding())
                .setTarget(inspection.getDynamicTarget())
                .setFields(fields)
                .setSourceSnapshotHash(plan.getCommand().getSourceSnapshotHash())
                .setEvidenceHash(inspection.getEvidenceHash())
                .setSignatureEvidence(plan.getSignatureEvidence());
    }

    private boolean isDynamicBinding(MesProRouteFlowProcessBatchRecordDO binding) {
        return binding != null && StrUtil.isBlank(binding.getBatchRecordReportId())
                && PROCESS_INSPECTION_FORM_TEMPLATE_ID.equals(binding.getFormTemplateId())
                && StrUtil.isNotBlank(binding.getFormBindingKey())
                && binding.getLastPublishedTemplateVersionId() != null;
    }

    private MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence signature(
            String role, String sourceType, Long sourceId, Long signatureId, Long userId,
            LocalDateTime signedAt, String evidenceHash) {
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence()
                .setRole(role).setSourceType(sourceType).setSourceId(sourceId).setSignatureId(signatureId)
                .setUserId(userId).setSignedAt(signedAt).setEvidenceHash(evidenceHash);
    }

    private MesTeamLeaderActiveOrderReleaseBlocker blocker(
            String type, String objectType, Object objectId, String objectCode,
            String reason, String suggestion) {
        return new MesTeamLeaderActiveOrderReleaseBlocker()
                .setBlockerType(type)
                .setObjectType(objectType)
                .setObjectId(objectId == null ? null : String.valueOf(objectId))
                .setObjectCode(objectCode)
                .setReason(reason)
                .setSuggestion(suggestion);
    }

    private void addEvidence(Set<Long> objectIds, List<String> hashes, Long objectId, String hash) {
        if (objectId != null) {
            objectIds.add(objectId);
        }
        hashes.add(hash);
    }

    private void put(Map<String, SourceValue> values, String key, String fieldCode, Object value) {
        SourceValue existing = values.putIfAbsent(key, new SourceValue(fieldCode, value));
        if (existing != null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED,
                    "PQC 汇集来源键重复=" + key);
        }
    }

    private String itemSourceKey(MesPqcProcessInspectionAggregateDetailDO detail, String fieldCode) {
        return detail.getInspectionType() + "|" + detail.getItemCode() + "|" + detail.getSampleNo()
                + "|" + fieldCode;
    }

    private String headerSourceKey(String inspectionType, String fieldCode) {
        return inspectionType + "|" + fieldCode;
    }

    private String dccSourceKey(String inspectionType, String fieldCode) {
        return inspectionType + "|DCC|" + fieldCode;
    }

    private String summarySourceKey(String fieldCode) {
        return "SUMMARY|" + fieldCode;
    }

    private String cellKey(Integer row, Integer column) {
        return row == null || column == null ? null : row + ":" + column;
    }

    private boolean decimalEquals(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private String displayValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof LocalDateTime time) {
            return DATETIME_FORMATTER.format(time);
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

    private <T> List<T> distinct(List<T> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }

    private Integer integer(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value == null || !value.isIntegralNumber() ? null : value.intValue();
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value == null || value.isNull() ? null : value.asText();
    }

    private record SourceValue(String fieldCode, Object value) {
    }

    private record MappingResolution(List<MesProBatchRecordCellLinkRuleDO> rules,
                                     List<MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.MappedValue> values,
                                     MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.TargetResolution
                                             dynamicTarget) {
    }

    private record SnapshotField(String fieldPath, String fieldKey,
                                 MesProBatchRecordExecutionFieldAuditValueType valueType) {
    }
}
