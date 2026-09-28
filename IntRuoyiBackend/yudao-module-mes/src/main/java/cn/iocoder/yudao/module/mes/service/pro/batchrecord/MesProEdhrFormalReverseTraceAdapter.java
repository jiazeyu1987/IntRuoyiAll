package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CatalogItem;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Condition;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceItem;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Reads one formal persisted source category; it never creates production facts. */
final class MesProEdhrFormalReverseTraceAdapter implements MesProEdhrReverseTraceSourceAdapter {
    private static final String SOURCE_VIEW = "RECORDED";
    private static final List<String> PARAMETER_SEMANTIC_IDENTITY_KEYS = List.of(
            "routeVersionId", "routeProcessId", "processId", "unit");
    private static final List<DateTimeFormatter> SOURCE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    private final Category category;
    private final MesProBatchRecordExecutionMapper executionMapper;
    private final MesProEdhrBatchExecutionOriginMapper originMapper;
    private final MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper;
    private final MesProEdhrBatchExecutionTaskMapper taskMapper;
    private final FormActionInstanceMapper formValueMapper;
    private final MesProBatchRecordExecutionFieldAuditItemMapper fieldAuditMapper;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    private final MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;

    MesProEdhrFormalReverseTraceAdapter(Category category, MesProBatchRecordExecutionMapper executionMapper,
                                        MesProEdhrBatchExecutionOriginMapper originMapper,
                                        MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper,
                                        MesProEdhrBatchExecutionTaskMapper taskMapper,
                                        FormActionInstanceMapper formValueMapper,
                                        MesProBatchRecordExecutionFieldAuditItemMapper fieldAuditMapper,
                                        MesProProcessPoolEventMapper eventMapper,
                                        MesProProcessPoolPqcRecordMapper pqcRecordMapper,
                                        MesProcessPoolSubmissionReviewMapper reviewMapper,
                                        MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
                                        MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
                                        MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper) {
        this.category = Objects.requireNonNull(category);
        this.executionMapper = executionMapper;
        this.originMapper = originMapper;
        this.traceLinkMapper = traceLinkMapper;
        this.taskMapper = taskMapper;
        this.formValueMapper = formValueMapper;
        this.fieldAuditMapper = fieldAuditMapper;
        this.eventMapper = eventMapper;
        this.pqcRecordMapper = pqcRecordMapper;
        this.reviewMapper = reviewMapper;
        this.releaseApplicationMapper = releaseApplicationMapper;
        this.releaseTransactionMapper = releaseTransactionMapper;
        this.completionReceiptMapper = completionReceiptMapper;
    }

    @Override public Category category() { return category; }
    @Override
    public boolean supports(String evidenceKey) {
        if (evidenceKey == null) return false;
        if (category != Category.FIELD) return evidenceKey.startsWith(category.name() + ":");
        return evidenceKey.startsWith("FIELD:") || evidenceKey.startsWith("FIELD_DYNAMIC:")
                || evidenceKey.startsWith("FIELD_CHANGE:");
    }

    @Override
    public CatalogResult readCatalog(MesProEdhrBatchExecutionDO batch) {
        if (batch == null || batch.getId() == null) {
            return new CatalogResult(null, null, "SOURCE_MISSING", "SOURCE_MISSING", "批次来源身份缺失", List.of());
        }
        if ((category == Category.INSPECTION || category == Category.MATERIAL)
                && resolveActiveOrderId(batch) == null) {
            return new CatalogResult(sourceVersion(batch), sourceIdentity(batch), "SOURCE_MISSING", "SOURCE_MISSING",
                    "批次正式来源关联缺失", List.of());
        }
        List<Fact> facts = readFacts(batch);
        if (facts.isEmpty()) {
            return new CatalogResult(sourceVersion(facts, batch), sourceIdentity(batch), "NO_RECORDED_FACT",
                    "NO_RECORDED_FACT", "当前批次没有已保存的正式记录", List.of());
        }
        return new CatalogResult(sourceVersion(facts, batch), sourceIdentity(batch), "AVAILABLE", null, null,
                facts.stream().map(Fact::item).toList());
    }

    @Override
    public EvaluationResult evaluate(MesProEdhrBatchExecutionDO batch, List<Condition> conditions) {
        List<Fact> facts = readFacts(batch);
        List<ConditionMatch> matches = conditions.stream().map(condition -> {
            Fact fact = facts.stream().filter(item -> Objects.equals(item.item().getEvidenceKey(), condition.getEvidenceKey()))
                    .filter(item -> matches(condition, item.item())).findFirst().orElse(null);
            return new ConditionMatch(condition.getConditionId(), fact != null,
                    fact == null ? null : fact.item().getLabel(), fact == null ? null : fact.item().getSourceRef());
        }).toList();
        return new EvaluationResult(sourceVersion(facts, batch), sourceIdentity(batch), "COMPLETE", null, null, matches);
    }

    @Override
    public EvidenceResult readEvidence(MesProEdhrBatchExecutionDO batch, List<Condition> conditions) {
        List<Fact> facts = readFacts(batch);
        List<EvidenceItem> items = new ArrayList<>();
        for (Condition condition : conditions) {
            facts.stream().filter(item -> Objects.equals(item.item().getEvidenceKey(), condition.getEvidenceKey()))
                    .filter(item -> matches(condition, item.item())).forEach(fact -> items.add(
                            new EvidenceItem().setConditionId(condition.getConditionId())
                                    .setTargetBatchExecutionId(String.valueOf(batch.getId()))
                                    .setSourceIdentity(sourceIdentity(batch)).setSourceStage(fact.stage())
                                    .setSourceAction(fact.action()).setSourceRef(fact.item().getSourceRef())
                                    .setSourceContext(fact.sourceContext())
                                    .setActualValue(fact.item().getSavedValue()).setRecordStatus(fact.item().getRecordStatus())
                                    .setRecordedStandard(fact.item().getRecordedStandard())
                                    .setRecordedAt(fact.item().getRecordedAt())));
        }
        return new EvidenceResult(sourceVersion(facts, batch), sourceIdentity(batch), String.valueOf(batch.getId()), items);
    }

    @Override
    public ValidationResult validateCondition(CatalogItem item, Condition condition) {
        if (item == null || condition == null || !Objects.equals(item.getEvidenceKey(), condition.getEvidenceKey())
                || !Objects.equals(item.getCategory(), category) || !Objects.equals(item.getSourceView(), condition.getSourceView())
                || item.getAllowedOperators() == null || StrUtil.isBlank(condition.getOperator())
                || !item.getAllowedOperators().contains(condition.getOperator().toUpperCase(Locale.ROOT))) {
            return new ValidationResult(false, "CONDITION_INVALID", "条件与正式目录项不匹配");
        }
        String operator = condition.getOperator().toUpperCase(Locale.ROOT);
        if (Set.of("GT", "GE", "LT", "LE", "BETWEEN", "OUT_OF_LIMIT").contains(operator)
                && !"number".equalsIgnoreCase(item.getValueType())) {
            return new ValidationResult(false, "CONDITION_INVALID", "非数值目录项不支持数值比较");
        }
        if ("OUT_OF_LIMIT".equals(operator) && !hasRecordedBounds(item)) {
            return new ValidationResult(false, "CONDITION_INVALID", "正式记录缺少有效数值标准");
        }
        if ("number".equalsIgnoreCase(item.getValueType()) && !Set.of("OUT_OF_LIMIT", "JUDGEMENT_EQ").contains(operator)
                && !isNumericConditionValue(condition.getValue(), operator)) {
            return new ValidationResult(false, "CONDITION_INVALID", "数值条件格式不正确");
        }
        if (category == Category.PARAMETER
                && (!hasCompleteParameterSemanticIdentity(item.getQualifiers())
                || !hasCompleteParameterSemanticIdentity(condition.getQualifiers()))) {
            return new ValidationResult(false, "CONDITION_INVALID", "参数条件必须包含完整路线版本、路线工序、工序和单位限定");
        }
        if (condition.getQualifiers() != null && !condition.getQualifiers().isEmpty()
                && (item.getQualifiers() == null || condition.getQualifiers().entrySet().stream()
                .anyMatch(entry -> !Objects.equals(entry.getValue(), item.getQualifiers().get(entry.getKey()))))) {
            return new ValidationResult(false, "CONDITION_INVALID", "条件限定与正式来源不一致");
        }
        return new ValidationResult(true, null, null);
    }

    private List<Fact> readFacts(MesProEdhrBatchExecutionDO batch) {
        return switch (category) {
            case FIELD -> readFieldFacts(batch);
            case PARAMETER, EQUIPMENT, PERSON -> readEventFacts(batch);
            case INSPECTION -> readInspectionFacts(batch);
            case MATERIAL -> readMaterialFacts(batch);
        };
    }

    private List<Fact> readFieldFacts(MesProEdhrBatchExecutionDO batch) {
        MesProBatchRecordExecutionPageReqVO request = new MesProBatchRecordExecutionPageReqVO();
        request.setBatchExecutionId(batch.getId());
        request.setPageSize(200);
        List<MesProBatchRecordExecutionDO> executions = new ArrayList<>();
        Long expectedTotal = null;
        for (int pageNo = 1; ; pageNo++) {
            request.setPageNo(pageNo);
            PageResult<MesProBatchRecordExecutionDO> page = executionMapper.selectPage(request);
            if (page == null || page.getList() == null || page.getTotal() == null) {
                throw new IllegalStateException("SOURCE_MISSING: 批记录字段来源未完整读取");
            }
            if (expectedTotal == null) {
                expectedTotal = page.getTotal();
            } else if (!expectedTotal.equals(page.getTotal())) {
                throw new IllegalStateException("SOURCE_MISSING: 批记录字段来源总数不一致");
            }
            executions.addAll(page.getList());
            if (executions.size() == expectedTotal) break;
            if (executions.size() > expectedTotal || page.getList().isEmpty()) {
                throw new IllegalStateException("SOURCE_MISSING: 批记录字段来源未完整读取");
            }
        }
        List<Fact> facts = new ArrayList<>();
        for (MesProBatchRecordExecutionDO execution : executions) {
            if (execution.getId() == null || !Objects.equals(batch.getId(), execution.getBatchExecutionId())) {
                throw sourceConflict("批记录字段执行身份不一致");
            }
            if (execution.getBatchRecordVersionId() == null) throw sourceMissing("批记录冻结模板版本缺失");
            addTraditionalFieldFacts(facts, execution);
            for (MesProBatchRecordExecutionFieldAuditItemDO audit : fieldAuditMapper.selectListByExecutionId(execution.getId())) {
                String identity = "FIELD_CHANGE:" + execution.getBatchRecordVersionId() + ":" + audit.getFieldPath();
                String beforeKey = identity + ":BEFORE";
                String afterKey = identity + ":AFTER";
                facts.add(fact(beforeKey, audit.getFieldLabel(), auditValue(audit.getOldValueJson(), audit.getValueType()), "FLOW-04", "FIELD_CHANGE_BEFORE",
                        "field-audit:" + audit.getId() + ":before", audit.getChangedAt(), "FIELD_CHANGE"));
                facts.add(fact(afterKey, audit.getFieldLabel(), auditValue(audit.getNewValueJson(), audit.getValueType()), "FLOW-04", "FIELD_CHANGE_AFTER",
                        "field-audit:" + audit.getId() + ":after", audit.getChangedAt(), "FIELD_CHANGE"));
            }
        }
        List<MesProEdhrBatchExecutionTaskDO> tasks = taskMapper.selectListByBatchExecutionId(batch.getId());
        if (tasks == null) throw sourceMissing("批次表单任务未完整读取");
        Set<Long> readInstances = new java.util.HashSet<>();
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            if (task.getFormCenterInstanceId() == null) continue;
            FormActionInstanceDO instance = formValueMapper.selectById(task.getFormCenterInstanceId());
            if (instance == null || task.getFormTemplateId() == null || task.getFormTemplateVersionId() == null
                    || batch.getRouteVersionId() == null || StrUtil.isBlank(task.getFormBindingKey())
                    || StrUtil.isBlank(instance.getFormDataJson())) throw sourceMissing("动态表单正式实例或版本绑定缺失");
            JSONObject values = parseJsonObject(instance.getFormDataJson(), "动态表单保存字段JSON");
            if (values == null || !Objects.equals(batch.getId(), task.getBatchExecutionId())
                    || !Objects.equals(batch.getTenantId(), instance.getTenantId())
                    || !Objects.equals(task.getFormCenterInstanceId(), instance.getId())
                    || !"MES".equals(instance.getDataDomain()) || !"MES".equals(instance.getSystemCode())
                    || !"EDHR_ROUTE_FORM".equals(instance.getObjectType())
                    || !String.valueOf(batch.getRouteVersionId()).equals(instance.getObjectVersion())
                    || !cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionPublishProjectionServiceImpl
                        .routeFormActionCode(batch.getRouteVersionId(), task.getFormBindingKey()).equals(instance.getActionCode())
                    || !Objects.equals(batch.getId(), values.getLong("batchExecutionId"))
                    || !Objects.equals(task.getFormTemplateId(), values.getLong("formTemplateId"))
                    || !Objects.equals(task.getFormTemplateVersionId(), values.getLong("formTemplateVersionId"))
                    || tasks.stream().noneMatch(bound -> Objects.equals(bound.getFormCenterInstanceId(), instance.getId())
                        && Objects.equals(bound.getBatchExecutionId(), batch.getId())
                        && Objects.equals(bound.getFormTemplateVersionId(), task.getFormTemplateVersionId())
                        && Objects.equals(bound.getId(), values.getLong("batchTaskId"))
                        && String.valueOf(bound.getId()).equals(instance.getObjectId()))) {
                throw sourceConflict("动态表单实例与批次、租户或模板版本绑定不一致");
            }
            if (!readInstances.add(instance.getId())) continue;
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (Set.of("batchExecutionId", "batchTaskId", "routeProcessId", "formBindingKey", "formTemplateId",
                        "formTemplateVersionId", "formTemplateVersionNo").contains(entry.getKey())) continue;
                addDynamicFieldFacts(facts, task, instance, entry.getKey(), entry.getValue());
            }
        }
        return facts;
    }

    private void addTraditionalFieldFacts(List<Fact> facts, MesProBatchRecordExecutionDO execution) {
        List<?> cells = traditionalArray(execution.getCellValuesJson(), "批记录保存单元格");
        // An empty persisted array is not permission to materialize snapshot default values.
        if (cells.isEmpty()) return;
        if (StrUtil.isBlank(execution.getExecutionSnapshotJson()) || StrUtil.isBlank(execution.getBatchRecordReportId())) {
            throw sourceMissing("批记录冻结字段定义或报表身份缺失");
        }
        JSONObject snapshot = parseJsonObject(execution.getExecutionSnapshotJson(), "批记录冻结字段定义");
        if (!(snapshot.get("fields") instanceof List<?> fields)) throw sourceMissing("批记录冻结fields数组缺失");
        Map<String, JSONObject> definitions = new LinkedHashMap<>();
        Map<String, Integer> keyCounts = new LinkedHashMap<>();
        for (Object raw : fields) {
            if (!(raw instanceof JSONObject field)) throw sourceConflict("批记录冻结字段不是对象");
            String coordinate = traditionalCoordinate(field);
            if (StrUtil.isBlank(field.getString("fieldKey")) || StrUtil.isBlank(field.getString("fieldPath"))
                    || StrUtil.isBlank(field.getString("valueType"))) throw sourceMissing("批记录冻结字段身份或类型缺失");
            if (definitions.putIfAbsent(coordinate, field) != null) throw sourceConflict("批记录冻结字段坐标重复");
            keyCounts.merge(field.getString("fieldKey"), 1, Integer::sum);
        }
        Set<String> seen = new java.util.HashSet<>();
        for (Object raw : cells) {
            if (!(raw instanceof JSONObject cell)) throw sourceConflict("批记录保存单元格不是对象");
            String coordinate = traditionalCoordinate(cell);
            if (!seen.add(coordinate)) throw sourceConflict("批记录保存单元格坐标重复");
            JSONObject field = definitions.get(coordinate);
            if (field == null) throw sourceConflict("批记录保存单元格没有冻结字段定义");
            String type = field.getString("valueType");
            if (cell.containsKey("valueType") && !Objects.equals(type, cell.getString("valueType"))) {
                throw sourceConflict("批记录保存类型与冻结字段类型不一致");
            }
            if (!cell.containsKey("value")) throw sourceMissing("批记录保存单元格值缺失");
            Object value;
            try {
                MesProBatchRecordExecutionFieldAuditValueType declaredType =
                        MesProBatchRecordExecutionFieldAuditValueType.valueOf(type);
                // The writer persists explicit null for a cleared cell, independently of its declared type.
                value = cell.get("value") == null ? null : JSON.parse(
                        MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(
                                declaredType, cell.get("value")));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("SOURCE_CONFLICT:批记录保存值与冻结字段类型不一致", exception);
            }
            String path = field.getString("fieldPath");
            String identity = JSON.toJSONString(List.of(execution.getBatchRecordReportId(), path, field.getString("fieldKey")));
            if (keyCounts.get(field.getString("fieldKey")) > 1) {
                // No stable repeated-row key is persisted: bind position to this exact saved source revision.
                if (execution.getFieldAuditRevision() == null) throw sourceMissing("批记录重复行保存修订缺失");
                identity += ":" + execution.getId() + ":" + execution.getFieldAuditRevision()
                        + ":" + DigestUtil.sha256Hex(execution.getCellValuesJson()) + ":" + coordinate;
            }
            Fact saved = fact("FIELD:" + execution.getBatchRecordVersionId() + ":" + identity,
                    field.getString("label"), value, "FLOW-04", "BATCH_FIELD_SAVED",
                    "execution:" + execution.getId() + "#" + identity, execution.getUpdateTime(), "SAVED_RECORD");
            saved.item().setValueType(switch (type) {
                case "NUMBER" -> "number";
                case "BOOLEAN" -> "boolean";
                case "JSON", "SIGNATURE" -> "json";
                case "NULL" -> "null";
                default -> "string";
            });
            facts.add(saved);
        }
    }

    private List<?> traditionalArray(String json, String name) {
        if (StrUtil.isBlank(json)) throw sourceMissing(name + "缺失");
        Object parsed;
        try {
            parsed = JSON.parse(json);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("SOURCE_CONFLICT:" + name + "无法解析", exception);
        }
        if (!(parsed instanceof List<?> rows)) throw sourceConflict(name + "必须为数组");
        return rows;
    }

    private String traditionalCoordinate(JSONObject value) {
        if (!(value.get("rowIndex") instanceof Integer row) || row < 0
                || !(value.get("columnIndex") instanceof Integer column) || column < 0) {
            throw sourceConflict("批记录单元格坐标缺失或不是非负整数");
        }
        return row + ":" + column;
    }

    private Object auditValue(String json, String type) {
        if (StrUtil.isBlank(json) || StrUtil.isBlank(type)) throw sourceMissing("字段审计类型或正式JSON值缺失");
        try {
            Object value = JSON.parse(json);
            // The audit producer permits an explicit null old value for a first assignment.
            if (value == null) return null;
            String canonical = MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(
                    MesProBatchRecordExecutionFieldAuditValueType.valueOf(type), value);
            return JSON.parse(canonical);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("SOURCE_CONFLICT:字段审计类型与正式JSON值不一致", exception);
        }
    }

    private void addDynamicFieldFacts(List<Fact> facts, MesProEdhrBatchExecutionTaskDO task,
                                      FormActionInstanceDO instance, String path, Object value) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, child) -> addDynamicFieldFacts(facts, task, instance, path + "." + key, child));
        } else if (value instanceof List<?> rows) {
            // Without a formal row key, bind the position to this saved revision, never to another revision's row.
            String revision = DigestUtil.sha256Hex(instance.getFormDataJson());
            for (int i = 0; i < rows.size(); i++) {
                addDynamicFieldFacts(facts, task, instance, path + "[" + revision + ":" + i + "]", rows.get(i));
            }
        } else if (value != null) {
            facts.add(fact("FIELD_DYNAMIC:" + task.getFormTemplateVersionId() + ":" + path, path, value,
                    "FLOW-04", "DYNAMIC_FORM_SAVED", "bpm-form-instance:" + instance.getId() + "#" + path,
                    instance.getUpdateTime(), "SAVED_RECORD"));
        }
    }

    private List<Fact> readEventFacts(MesProEdhrBatchExecutionDO batch) {
        JSONObject snapshot = readFormalSnapshot(batch);
        JSONObject activeOrderBinding = requiredSnapshotObject(snapshot, "activeOrderBinding", "完工正式活跃订单快照缺失");
        JSONObject workOrderBinding = requiredSnapshotObject(snapshot, "workOrderBinding", "完工正式工单快照缺失");
        Long targetActiveOrderId = activeOrderBinding.getLong("id");
        Long targetTenantId = activeOrderBinding.getLong("tenantId");
        Long targetWorkOrderId = activeOrderBinding.getLong("workOrderId");
        Long targetRouteId = activeOrderBinding.getLong("routeId");
        Long routeVersionId = activeOrderBinding.getLong("routeVersionId");
        if (targetActiveOrderId == null || targetTenantId == null || targetWorkOrderId == null || targetRouteId == null
                || routeVersionId == null || workOrderBinding.getLong("id") == null) {
            throw sourceMissing("完工正式目标绑定身份缺失");
        }
        if (!Objects.equals(targetActiveOrderId, resolveActiveOrderId(batch))
                || !Objects.equals(targetTenantId, batch.getTenantId())
                || !Objects.equals(targetWorkOrderId, batch.getWorkOrderId())
                || !Objects.equals(targetRouteId, batch.getRouteId())
                || !Objects.equals(workOrderBinding.getLong("id"), batch.getWorkOrderId())) {
            throw sourceConflict("完工正式目标绑定身份不一致");
        }
        Object allocationRows = snapshot.get("allocations");
        if (!(allocationRows instanceof Collection<?> allocations)) throw sourceMissing("完工正式分配来源清单缺失");
        Set<Long> allocationEventIds = new java.util.LinkedHashSet<>();
        Set<Long> allocationIds = new java.util.HashSet<>();
        Map<Long, List<JSONObject>> allocationsByEventId = new LinkedHashMap<>();
        for (Object row : allocations) {
            JSONObject allocation = parseJsonObject(JSON.toJSONString(row), "完工分配来源");
            if (allocation == null || allocation.getLong("eventId") == null) throw sourceMissing("完工分配事件身份缺失");
            Long allocationId = allocation.getLong("id");
            if (allocationId != null && !allocationIds.add(allocationId)) throw sourceConflict("完工正式分配身份重复");
            Long allocationTenantId = allocation.getLong("tenantId");
            Long allocationActiveOrderId = allocation.getLong("activeOrderId");
            Long allocationWorkOrderId = allocation.getLong("workOrderId");
            Long allocationRouteProcessId = allocation.getLong("routeProcessId");
            Long allocationProcessId = allocation.getLong("processId");
            if (allocationTenantId == null || allocationActiveOrderId == null || allocationWorkOrderId == null
                    || allocationRouteProcessId == null || allocationProcessId == null) {
                throw sourceMissing("完工正式分配目标身份或工序缺失");
            }
            if (!Objects.equals(allocationTenantId, targetTenantId)
                    || !Objects.equals(allocationActiveOrderId, targetActiveOrderId)
                    || !Objects.equals(allocationWorkOrderId, targetWorkOrderId)) {
                throw sourceConflict("完工正式分配目标归属不一致");
            }
            // 同一事件可对应不同分配行；事件事实只读取一次，分配身份不得重复。
            allocationEventIds.add(allocation.getLong("eventId"));
            allocationsByEventId.computeIfAbsent(allocation.getLong("eventId"), ignored -> new ArrayList<>())
                    .add(allocation);
        }
        if (!allocationEventIds.isEmpty()) {
            Object processRows = snapshot.get("snapshots");
            if (!(processRows instanceof Collection<?> processes)) throw sourceMissing("完工正式工序快照缺失");
            Set<String> formalProcessKeys = new java.util.HashSet<>();
            for (Object row : processes) {
                JSONObject process = parseJsonObject(JSON.toJSONString(row), "完工工序快照");
                if (process == null || process.getLong("routeProcessId") == null || process.getLong("processId") == null) {
                    throw sourceMissing("完工正式工序快照身份缺失");
                }
                formalProcessKeys.add(process.getLong("routeProcessId") + ":" + process.getLong("processId"));
            }
            for (List<JSONObject> eventAllocations : allocationsByEventId.values()) {
                for (JSONObject allocation : eventAllocations) {
                    String processKey = allocation.getLong("routeProcessId") + ":" + allocation.getLong("processId");
                    if (!formalProcessKeys.contains(processKey)) throw sourceConflict("完工正式分配工序不在目标快照");
                }
            }
        }
        List<MesProEdhrBatchExecutionTraceLinkDO> links = traceLinkMapper.selectListByBatchExecutionId(batch.getId());
        if (links == null) throw sourceMissing("正式追溯清单读取失败");
        List<MesProEdhrBatchExecutionTraceLinkDO> productionLinks = links.stream()
                .filter(link -> "PRODUCTION_SUBMIT".equals(link.getLinkType())).toList();
        if (!allocationEventIds.isEmpty() && productionLinks.isEmpty()) throw sourceMissing("生产提交正式追溯关联缺失");
        Set<Long> linkedEventIds = new java.util.HashSet<>();
        boolean receiptWitness = false;
        for (MesProEdhrBatchExecutionTraceLinkDO link : productionLinks) {
            if (!Objects.equals(batch.getId(), link.getBatchExecutionId()) || !Objects.equals(batch.getTenantId(), link.getTenantId())
                    || !"BOUND".equals(link.getRelationStatus())) throw sourceConflict("生产提交追溯关联身份或状态不一致");
            if (StrUtil.isBlank(link.getSnapshotJson()) || StrUtil.isBlank(link.getSnapshotHash())) throw sourceMissing("生产提交追溯快照或哈希缺失");
            if (!MesProEdhrBatchTraceSourceHash.isValid(link.getLinkType(), link.getSnapshotJson(), link.getSnapshotHash())) {
                throw sourceConflict("生产提交追溯快照哈希不一致");
            }
            if (link.getSourceEventId() != null) {
                if (!allocationEventIds.contains(link.getSourceEventId())) throw sourceConflict("追溯事件不在完工正式分配清单");
                linkedEventIds.add(link.getSourceEventId());
            } else {
                // The formal producer writes a receipt witness here, not an event ID.
                var origin = resolveOrigin(batch);
                var receipt = completionReceiptMapper.selectByIdAndTenantId(origin.getCompletionBackfillReceiptId(), batch.getTenantId());
                JSONObject witness = parseJsonObject(link.getSnapshotJson(), "生产提交回执见证");
                if (!"PRODUCTION_SUBMIT".equals(witness.getString("sourceType"))
                        || !Objects.equals(receipt.getBatchRecordId(), link.getSourceObjectId())
                        || !Objects.equals(receipt.getBatchRecordId(), witness.getLong("sourceId"))
                        || !Objects.equals(receipt.getReceiptHash(), witness.getString("witnessHash"))) {
                    throw sourceConflict("生产提交追溯见证与完工回执不一致");
                }
                receiptWitness = true;
            }
        }
        if (!receiptWitness && !linkedEventIds.equals(allocationEventIds)) throw sourceMissing("生产提交追溯事件集合不完整");
        List<Long> eventIds = allocationEventIds.stream().sorted().toList();
        JSONObject productionFacts = requiredSnapshotObject(snapshot, "productionFacts", "正式冻结生产事实缺失");
        if (!Integer.valueOf(1).equals(productionFacts.getInteger("formatVersion"))) {
            throw sourceConflict("正式冻结生产事实版本不支持");
        }
        if (!(productionFacts.get("events") instanceof Collection<?>)) throw sourceMissing("冻结生产事件清单缺失");
        List<MesProProcessPoolEventDO> events = new ArrayList<>();
        for (JSONObject row : snapshotRows(productionFacts.get("events"))) {
            String payload = row.getString("rawPayload");
            if (StrUtil.isBlank(payload) || StrUtil.isBlank(row.getString("payloadContentHash"))) {
                throw sourceMissing("冻结事件内容版本缺失");
            }
            if (!Objects.equals(DigestUtil.sha256Hex(payload), row.getString("payloadContentHash"))) {
                throw sourceConflict("冻结事件内容版本冲突");
            }
            parseJsonObject(payload, "冻结生产事件payload");
            events.add(JsonUtils.parseObject(row.toJSONString(), MesProProcessPoolEventDO.class));
        }
        if (events == null || events.size() != eventIds.size()) throw sourceMissing("生产提交事件未完整读取");
        Set<Long> readIds = new java.util.HashSet<>();
        for (MesProProcessPoolEventDO event : events) {
            if (event == null || !allocationEventIds.contains(event.getId()) || !readIds.add(event.getId())
                    || !Objects.equals(batch.getTenantId(), event.getTenantId())
                    || !"PRODUCTION_SUBMIT".equals(event.getEventType())) {
                throw sourceConflict("生产提交事件集合或租户事件类型不一致");
            }
            List<JSONObject> eventAllocations = allocationsByEventId.get(event.getId());
            if (eventAllocations == null || eventAllocations.isEmpty()) throw sourceConflict("生产提交事件缺少正式分配关系");
            for (JSONObject allocation : eventAllocations) {
                if (!Objects.equals(event.getTenantId(), allocation.getLong("tenantId"))
                        || !Objects.equals(event.getProcessId(), allocation.getLong("processId"))) {
                    throw sourceConflict("生产提交事件与正式分配工序或租户不一致");
                }
            }
        }
        List<Fact> facts = new ArrayList<>();
        for (MesProProcessPoolEventDO event : events) {
            Map<String, JSONObject> targetProcesses = new LinkedHashMap<>();
            for (JSONObject allocation : allocationsByEventId.get(event.getId())) {
                targetProcesses.putIfAbsent(allocation.getLong("routeProcessId") + ":" + allocation.getLong("processId"), allocation);
            }
            if (category == Category.EQUIPMENT) {
                if (StrUtil.isNotBlank(event.getRawPayload())) {
                    JSONObject raw = parseJsonObject(event.getRawPayload(), "生产提交设备JSON");
                    addSelectedEquipment(facts, event, raw.get("selectedDevices"), null);
                    if (raw.get("materialDetails") instanceof Collection<?> materials) {
                        for (Object value : materials) {
                            JSONObject material = parseJsonObject(JSON.toJSONString(value), "生产物料明细");
                            addSelectedEquipment(facts, event, material.get("selectedDevices"), material.getString("materialId"));
                        }
                    }
                }
            } else if (category == Category.PERSON) {
                if (event.getActualEmployeeId() != null) facts.add(fact("PERSON:EMPLOYEE:actualEmployeeId:" + event.getActualEmployeeId(), "实际操作人", event.getActualEmployeeId(), "FLOW-04", "PRODUCTION_OPERATOR", "event:" + event.getId(), event.getServerSubmitTime()));
                if (event.getSignatureUserId() != null) facts.add(fact("PERSON:SYSTEM_USER:signatureUserId:" + event.getSignatureUserId(), "提交签名人", event.getSignatureUserId(), "FLOW-04", "PRODUCTION_SIGNATURE", "event:" + event.getId(), event.getServerSubmitTime()));
            } else if (category == Category.PARAMETER && StrUtil.isNotBlank(event.getRawPayload())) {
                JSONObject raw = parseJsonObject(event.getRawPayload(), "生产提交参数JSON");
                Object materialDetails = raw.get("materialDetails");
                if (materialDetails != null && !(materialDetails instanceof Collection<?>)) throw sourceConflict("生产物料明细不是正式数组");
                if (materialDetails instanceof Collection<?> materials) {
                    for (Object material : materials) {
                        JSONObject materialObject = parseJsonObject(JSON.toJSONString(material), "生产物料明细");
                        Object readings = materialObject.get("deviceParameterReadings");
                        if (readings instanceof Collection<?> collection) {
                            for (Object value : collection) {
                                for (JSONObject allocation : targetProcesses.values()) {
                                    addParameterFact(facts, event, value, materialObject.getString("materialId"),
                                            routeVersionId, allocation.getLong("routeProcessId"), allocation.getLong("processId"));
                                }
                            }
                        } else if (readings != null) throw sourceConflict("物料参数明细不是正式数组");
                    }
                }
                Object readings = raw.get("deviceParameterReadings");
                if (readings instanceof Collection<?> collection) {
                    for (Object value : collection) {
                        for (JSONObject allocation : targetProcesses.values()) {
                            addParameterFact(facts, event, value, null, routeVersionId,
                                    allocation.getLong("routeProcessId"), allocation.getLong("processId"));
                        }
                    }
                }
                else if (readings != null) throw sourceConflict("生产参数明细不是正式数组");
            }
        }
        if (category == Category.PERSON) {
            facts.addAll(readFormalPqcPersonFacts(snapshot, targetTenantId, targetActiveOrderId, targetWorkOrderId,
                    targetRouteId, routeVersionId));
        }
        List<MesProcessPoolSubmissionReviewDO> frozenReviews = readFrozenReviews(productionFacts, allocationsByEventId, targetTenantId);
        if (category == Category.PERSON) facts.addAll(readReviewAndReleasePersonFacts(batch, frozenReviews));
        if (category == Category.EQUIPMENT) facts.addAll(readPqcEquipmentFacts(batch));
        return facts;
    }

    private List<MesProcessPoolSubmissionReviewDO> readFrozenReviews(JSONObject productionFacts,
            Map<Long, List<JSONObject>> allocationsByEventId, Long tenantId) {
        if (!(productionFacts.get("reviews") instanceof Collection<?>)) throw sourceMissing("冻结生产审核清单缺失");
        Map<Long, MesProcessPoolSubmissionReviewDO> reviews = new LinkedHashMap<>();
        for (JSONObject row : snapshotRows(productionFacts.get("reviews"))) {
            MesProcessPoolSubmissionReviewDO review = JsonUtils.parseObject(row.toJSONString(), MesProcessPoolSubmissionReviewDO.class);
            if (review.getId() == null || review.getEventId() == null || review.getLeaderUserId() == null
                    || review.getReviewSignatureId() == null || review.getReviewedAt() == null
                    || StrUtil.isBlank(review.getReviewSignatureSnapshotJson())) throw sourceMissing("冻结审核身份或签名缺失");
            if (!Objects.equals(tenantId, review.getTenantId()) || !"PRODUCTION".equals(review.getLeaderType())
                    || !"APPROVED".equals(review.getReviewStatus())
                    || !Objects.equals(review.getLeaderUserId(), review.getReviewSignatureUserId())
                    || reviews.put(review.getId(), review) != null) throw sourceConflict("冻结审核归属或状态冲突");
            JSONObject signature = parseJsonObject(review.getReviewSignatureSnapshotJson(), "冻结审核签名");
            if (!Objects.equals(String.valueOf(review.getReviewSignatureId()), signature.getString("signatureId"))
                    || !Objects.equals(String.valueOf(review.getLeaderUserId()), signature.getString("actorId"))
                    || !Objects.equals(String.valueOf(review.getEventId()), signature.getString("processPoolEventId"))
                    || !"TEAM_LEADER_REVIEW".equals(signature.getString("actionType"))
                    || !"PRODUCTION_SUBMIT".equals(signature.getString("eventType"))
                    || !"PRODUCTION".equals(signature.getString("leaderType"))
                    || !"APPROVED".equals(signature.getString("reviewStatus"))) throw sourceConflict("冻结审核签名绑定冲突");
        }
        Set<Long> boundIds = new java.util.LinkedHashSet<>();
        for (var entry : allocationsByEventId.entrySet()) {
            for (JSONObject allocation : entry.getValue()) {
                Long reviewId = allocation.getLong("reviewId");
                if (reviewId == null || allocation.getLong("leaderUserId") == null || !reviews.containsKey(reviewId)) {
                    throw sourceMissing("正式分配精确冻结审核缺失");
                }
                var review = reviews.get(reviewId);
                if (!Objects.equals(entry.getKey(), review.getEventId())) {
                    throw sourceConflict("冻结审核与正式分配绑定冲突");
                }
                boundIds.add(reviewId);
            }
        }
        if (!boundIds.equals(reviews.keySet())) throw sourceConflict("冻结审核存在未绑定来源");
        return List.copyOf(reviews.values());
    }

    private List<Fact> readFormalPqcPersonFacts(JSONObject snapshot, Long targetTenantId, Long targetActiveOrderId,
                                                 Long targetWorkOrderId, Long targetRouteId, Long routeVersionId) {
        Object rawDetails = snapshot.get("pqcDetails");
        if (!(rawDetails instanceof Collection<?>)) throw sourceMissing("正式PQC快照明细缺失");
        List<JSONObject> details = snapshotRows(rawDetails);
        if (details.isEmpty()) return List.of();
        Object rawTasks = snapshot.get("pqcTasks");
        if (!(rawTasks instanceof Collection<?>)) throw sourceMissing("正式PQC任务快照缺失");
        Map<Long, JSONObject> tasksById = new LinkedHashMap<>();
        for (JSONObject task : snapshotRows(rawTasks)) {
            Long taskId = task.getLong("id");
            if (taskId == null) throw sourceMissing("正式PQC任务身份缺失");
            if (tasksById.put(taskId, task) != null) throw sourceConflict("正式PQC任务身份重复");
        }

        Map<Long, Long> taskByEventId = new LinkedHashMap<>();
        Map<Long, Long> recordByEventId = new LinkedHashMap<>();
        Map<Long, Long> reviewByEventId = new LinkedHashMap<>();
        List<Fact> facts = new ArrayList<>();
        for (JSONObject detail : details) {
            Long detailId = detail.getLong("id");
            Long taskId = detail.getLong("pqcTaskId");
            Long eventId = detail.getLong("eventId");
            Long recordId = detail.getLong("sourcePqcRecordId");
            Long reviewId = exactPqcReviewId(detail.get("reviewId"));
            if (detailId == null || taskId == null || eventId == null || recordId == null || reviewId == null) {
                throw sourceMissing("正式PQC聚合明细关联身份缺失");
            }
            JSONObject task = tasksById.get(taskId);
            if (task == null) throw sourceMissing("正式PQC聚合明细对应任务缺失");
            validateFormalPqcTask(task, targetTenantId, targetActiveOrderId, targetWorkOrderId, targetRouteId,
                    routeVersionId, eventId);
            validateFormalPqcDetail(detail, task, targetTenantId, targetActiveOrderId, targetWorkOrderId,
                    targetRouteId, routeVersionId);
            Long previousTaskId = taskByEventId.putIfAbsent(eventId, taskId);
            if (previousTaskId != null && !Objects.equals(previousTaskId, taskId)) {
                throw sourceConflict("正式PQC提交事件关联多个任务");
            }
            // Check every frozen detail before event deduplication, including later samples/items.
            Long previousReviewId = reviewByEventId.putIfAbsent(eventId, reviewId);
            if (previousReviewId != null && !Objects.equals(previousReviewId, reviewId)) {
                throw sourceConflict("正式PQC提交事件对应复核冲突");
            }
            Long previousRecordId = recordByEventId.putIfAbsent(eventId, recordId);
            if (previousRecordId != null) {
                if (!Objects.equals(previousRecordId, recordId)) throw sourceConflict("正式PQC提交事件对应记录冲突");
                continue;
            }

            MesProProcessPoolEventDO event = eventMapper.selectById(eventId);
            if (event == null) throw sourceMissing("正式PQC提交事件未完整读取");
            validateFormalPqcEvent(event, targetTenantId, targetWorkOrderId, targetRouteId, taskId, task);
            MesProProcessPoolPqcRecordDO pqcRecord = pqcRecordMapper.selectByEventId(eventId);
            if (pqcRecord == null) throw sourceMissing("正式PQC记录未完整读取");
            validateFormalPqcRecord(pqcRecord, recordId, event, targetTenantId, targetWorkOrderId, targetRouteId, task);
            MesProcessPoolSubmissionReviewDO review = readFormalPqcReview(reviewId, eventId, targetTenantId);
            if (pqcRecord.getActualEmployeeId() != null) {
                facts.add(fact("PERSON:EMPLOYEE:PQC_OPERATOR:" + pqcRecord.getActualEmployeeId(), "PQC检验人",
                        pqcRecord.getActualEmployeeId(), "FLOW-05", "PQC_OPERATOR", "pqc-record:" + pqcRecord.getId(),
                        pqcRecord.getServerSubmitTime()));
            }
            if (pqcRecord.getSignatureUserId() != null) {
                facts.add(fact("PERSON:SYSTEM_USER:PQC_SIGNATURE:" + pqcRecord.getSignatureUserId(), "PQC提交签名人",
                        pqcRecord.getSignatureUserId(), "FLOW-05", "PQC_SIGNATURE", "pqc-record:" + pqcRecord.getId(),
                        pqcRecord.getServerSubmitTime()));
            }
            facts.add(fact("PERSON:SYSTEM_USER:PQC_REVIEW:" + review.getLeaderUserId(), "PQC复核人",
                    review.getLeaderUserId(), "FLOW-05", "PQC_REVIEW", "review:" + review.getId(), review.getReviewedAt()));
            facts.add(fact("PERSON:SYSTEM_USER:PQC_REVIEW_SIGNATURE:" + review.getReviewSignatureUserId(), "PQC复核签名人",
                    review.getReviewSignatureUserId(), "FLOW-05", "PQC_REVIEW_SIGNATURE", "review:" + review.getId(), review.getReviewedAt()));
        }
        return facts;
    }

    private static Long exactPqcReviewId(Object value) {
        if (value == null) throw sourceMissing("正式PQC明细复核身份缺失");
        if (!(value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof java.math.BigInteger || value instanceof String)) {
            throw sourceConflict("正式PQC明细复核身份不是精确整数");
        }
        String decimal = value.toString();
        if (!decimal.matches("[1-9][0-9]*")) throw sourceConflict("正式PQC明细复核身份不是规范正整数");
        try {
            return Long.valueOf(decimal);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("SOURCE_CONFLICT:正式PQC明细复核身份超出Long范围", exception);
        }
    }

    private MesProcessPoolSubmissionReviewDO readFormalPqcReview(Long reviewId, Long eventId, Long tenantId) {
        // PQC reviews are terminal records; corrections re-aggregate with this same review ID.
        // Never substitute a latest review or collect all reviews of the event.
        MesProcessPoolSubmissionReviewDO review = reviewMapper.selectById(reviewId);
        if (review == null || review.getLeaderUserId() == null || review.getReviewedAt() == null
                || review.getReviewSignatureId() == null || review.getReviewSignatureUserId() == null
                || StrUtil.isBlank(review.getReviewSignatureSnapshotJson())) {
            throw sourceMissing("正式PQC精确复核记录或签名缺失");
        }
        if (!Objects.equals(reviewId, review.getId()) || !Objects.equals(tenantId, review.getTenantId())
                || !Objects.equals(eventId, review.getEventId()) || !"PQC".equals(review.getLeaderType())
                || !"APPROVED".equals(review.getReviewStatus())
                || !Objects.equals(review.getLeaderUserId(), review.getReviewSignatureUserId())) {
            throw sourceConflict("正式PQC复核归属或状态冲突");
        }
        JSONObject signature = parseJsonObject(review.getReviewSignatureSnapshotJson(), "正式PQC复核签名");
        if (!Objects.equals(String.valueOf(review.getReviewSignatureId()), signature.getString("signatureId"))
                || !Objects.equals(String.valueOf(review.getLeaderUserId()), signature.getString("actorId"))
                || !Objects.equals(String.valueOf(eventId), signature.getString("processPoolEventId"))
                || !"TEAM_LEADER_REVIEW".equals(signature.getString("actionType"))
                || !"PQC_INSPECTION".equals(signature.getString("eventType"))
                || !"PQC".equals(signature.getString("leaderType"))
                || !"APPROVED".equals(signature.getString("reviewStatus"))) {
            throw sourceConflict("正式PQC复核签名绑定冲突");
        }
        return review;
    }

    private void validateFormalPqcTask(JSONObject task, Long targetTenantId, Long targetActiveOrderId,
                                       Long targetWorkOrderId, Long targetRouteId, Long routeVersionId,
                                       Long eventId) {
        if (!Objects.equals(task.getLong("tenantId"), targetTenantId)
                || !Objects.equals(task.getLong("activeOrderId"), targetActiveOrderId)
                || !Objects.equals(task.getLong("workOrderId"), targetWorkOrderId)
                || !Objects.equals(task.getLong("routeId"), targetRouteId)
                || !Objects.equals(task.getLong("routeVersionId"), routeVersionId)
                || task.getLong("routeProcessId") == null || task.getLong("processId") == null
                || task.getLong("qaProcessId") == null
                || !Objects.equals(task.getLong("submittedEventId"), eventId)) {
            throw sourceConflict("正式PQC任务与批次或提交事件身份不一致");
        }
    }

    private void validateFormalPqcDetail(JSONObject detail, JSONObject task, Long targetTenantId,
                                         Long targetActiveOrderId, Long targetWorkOrderId, Long targetRouteId,
                                         Long routeVersionId) {
        if (!Objects.equals(detail.getLong("tenantId"), targetTenantId)
                || !Objects.equals(detail.getLong("activeOrderId"), targetActiveOrderId)
                || !Objects.equals(detail.getLong("workOrderId"), targetWorkOrderId)
                || !Objects.equals(detail.getLong("routeId"), targetRouteId)
                || !Objects.equals(detail.getLong("routeVersionId"), routeVersionId)
                || !Objects.equals(detail.getLong("routeProcessId"), task.getLong("routeProcessId"))
                || !Objects.equals(detail.getLong("processId"), task.getLong("processId"))
                || !Objects.equals(detail.getLong("pqcTaskId"), task.getLong("id"))
                || !Objects.equals(detail.getLong("eventId"), task.getLong("submittedEventId"))) {
            throw sourceConflict("正式PQC聚合明细与任务或批次身份不一致");
        }
    }

    private void validateFormalPqcEvent(MesProProcessPoolEventDO event, Long targetTenantId,
                                       Long targetWorkOrderId, Long targetRouteId, Long taskId, JSONObject task) {
        if (!Objects.equals(event.getTenantId(), targetTenantId)
                || !Objects.equals(event.getEventType(), MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                || !Objects.equals(event.getWorkOrderId(), targetWorkOrderId)
                || !Objects.equals(event.getRouteId(), targetRouteId)
                || !Objects.equals(event.getFeedbackSourceType(), "MES_PQC_INSPECTION_TASK")
                || !Objects.equals(event.getFeedbackSourceId(), taskId)
                || !Objects.equals(event.getRecordbookSourceType(), "MES_PQC_INSPECTION_TASK")
                || !Objects.equals(event.getRecordbookSourceId(), taskId)
                || (task.getLong("qaProcessId") != null
                && !Objects.equals(event.getQaProcessId(), task.getLong("qaProcessId")))) {
            throw sourceConflict("正式PQC提交事件与任务或批次身份不一致");
        }
    }

    private void validateFormalPqcRecord(MesProProcessPoolPqcRecordDO record, Long recordId,
                                         MesProProcessPoolEventDO event, Long targetTenantId,
                                         Long targetWorkOrderId, Long targetRouteId, JSONObject task) {
        if (!Objects.equals(record.getId(), recordId)
                || !Objects.equals(record.getTenantId(), targetTenantId)
                || !Objects.equals(record.getEventId(), event.getId())
                || !Objects.equals(record.getWorkOrderId(), targetWorkOrderId)
                || !Objects.equals(record.getRouteId(), targetRouteId)
                || (task.getLong("qaProcessId") != null
                && !Objects.equals(record.getQaProcessId(), task.getLong("qaProcessId")))
                || !Objects.equals(record.getActualEmployeeId(), event.getActualEmployeeId())
                || !Objects.equals(record.getSignatureUserId(), event.getSignatureUserId())
                || !Objects.equals(record.getServerSubmitTime(), event.getServerSubmitTime())) {
            throw sourceConflict("正式PQC记录与提交事件或任务身份不一致");
        }
    }

    private List<Fact> readReviewAndReleasePersonFacts(MesProEdhrBatchExecutionDO batch,
                                                     List<MesProcessPoolSubmissionReviewDO> reviews) {
        List<Fact> facts = new ArrayList<>();
            for (MesProcessPoolSubmissionReviewDO review : reviews) {
                if (review.getLeaderUserId() != null) {
                    facts.add(fact("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:" + review.getLeaderUserId(), "生产复核人",
                            review.getLeaderUserId(), "FLOW-04", "PRODUCTION_REVIEW", "review:" + review.getId(), review.getReviewedAt()));
                }
                if (review.getReviewSignatureUserId() != null) {
                    facts.add(fact("PERSON:SYSTEM_USER:PRODUCTION_REVIEW_SIGNATURE:" + review.getReviewSignatureUserId(), "生产复核签名人",
                            review.getReviewSignatureUserId(), "FLOW-04", "PRODUCTION_REVIEW_SIGNATURE", "review:" + review.getId(), review.getReviewedAt()));
                }
            }
        List<MesProcessPoolActiveOrderReleaseApplicationDO> applications = releaseApplicationMapper
                .selectListByBatchExecutionIds(List.of(batch.getId()));
        if (applications == null) throw sourceMissing("放行申请人员来源未完整读取");
        for (MesProcessPoolActiveOrderReleaseApplicationDO application : applications) {
            if (application.getPqcDecidedBy() != null) {
                facts.add(fact("PERSON:SYSTEM_USER:PQC_RELEASE:" + application.getPqcDecidedBy(), "PQC生产放行人",
                        application.getPqcDecidedBy(), "FLOW-08", "PQC_RELEASE_APPROVAL", "release-application:" + application.getId(), application.getPqcDecidedAt()));
            }
            if (application.getAppliedBy() != null) {
                facts.add(fact("PERSON:SYSTEM_USER:PQC_RELEASE_APPLICANT:" + application.getAppliedBy(), "PQC生产放行申请人",
                        application.getAppliedBy(), "FLOW-07", "PQC_RELEASE_APPLICATION", "release-application:" + application.getId(), application.getAppliedAt()));
            }
        }
        MesProEdhrReleaseTransactionDO transaction = releaseTransactionMapper.selectByBatchExecutionId(batch.getId());
        if (transaction != null) {
            if (transaction.getSubmittedBy() != null) {
                facts.add(fact("PERSON:SYSTEM_USER:LISTING_RELEASE_SUBMIT:" + transaction.getSubmittedBy(), "上市放行提交人",
                        transaction.getSubmittedBy(), "FLOW-09", "LISTING_RELEASE_SUBMIT", "release-transaction:" + transaction.getId(), transaction.getSubmittedAt()));
            }
            if (transaction.getApprovedBy() != null) {
                facts.add(fact("PERSON:SYSTEM_USER:LISTING_RELEASE_APPROVER:" + transaction.getApprovedBy(), "上市放行批准人",
                        transaction.getApprovedBy(), "FLOW-09", "LISTING_RELEASE_APPROVAL", "release-transaction:" + transaction.getId(), transaction.getApprovedAt()));
            }
        }
        return facts;
    }

    private void addParameterFact(List<Fact> facts, MesProProcessPoolEventDO event, Object value, String materialId,
                                  Long routeVersionId, Long routeProcessId, Long processId) {
        JSONObject reading = parseJsonObject(JSON.toJSONString(value), "生产参数明细");
        String code = StrUtil.blankToDefault(reading.getString("parameterCode"), reading.getString("code"));
        if (StrUtil.isBlank(code)) throw sourceMissing("正式参数编码缺失");
        if (routeVersionId == null || routeProcessId == null || processId == null) {
            throw sourceMissing("正式参数工序或路线版本身份缺失: " + code);
        }
        Object actual = reading.get("value") != null ? reading.get("value") : reading.get("textValue");
        if (actual == null || actual instanceof String text && StrUtil.isBlank(text)) {
            throw sourceMissing("正式参数记录值缺失: " + code);
        }
        Map<String, String> qualifiers = new LinkedHashMap<>();
        qualifiers.put("routeVersionId", String.valueOf(routeVersionId));
        qualifiers.put("routeProcessId", String.valueOf(routeProcessId));
        qualifiers.put("processId", String.valueOf(processId));
        qualifiers.put("unit", StrUtil.blankToDefault(reading.getString("unit"), ""));
        putQualifier(qualifiers, "materialId", materialId);
        putQualifier(qualifiers, "deviceId", reading.getString("deviceId"));
        putQualifier(qualifiers, "deviceCode", reading.getString("deviceCode"));
        Fact fact = fact("PARAMETER:" + code, code, actual, "FLOW-04", "PARAMETER_RECORDED",
                "event:" + event.getId() + "#material:" + materialId + "#device:" + reading.getString("deviceId") + "#parameter:" + code,
                event.getServerSubmitTime(), SOURCE_VIEW, qualifiers,
                Map.of("sourceEventId", String.valueOf(event.getId()),
                        "sourceWorkOrderId", String.valueOf(event.getWorkOrderId()),
                        "sourceRouteId", String.valueOf(event.getRouteId()),
                        "sourceRouteProcessId", String.valueOf(event.getRouteProcessId()),
                        "sourceProcessId", String.valueOf(event.getProcessId())));
        fact.item().setSemanticIdentity(parameterSemanticIdentity(code, qualifiers));
        fact.item().setUnit(reading.getString("unit")).setParameterStatus(reading.getString("parameterStatus"));
        setRecordedStandard(fact.item(), reading.getString("lowerLimit"), reading.getString("upperLimit"), reading.getString("standard"));
        facts.add(fact);
    }

    private void addSelectedEquipment(List<Fact> facts, MesProProcessPoolEventDO event, Object selected, String materialId) {
        if (selected == null) return;
        if (!(selected instanceof Collection<?> devices)) throw sourceConflict("已选生产设备不是正式数组");
        Set<Long> seen = new java.util.HashSet<>();
        for (Object value : devices) {
            JSONObject device = parseJsonObject(JSON.toJSONString(value), "已选生产设备");
            if (device == null || device.getLong("deviceId") == null) throw sourceMissing("已选生产设备身份缺失");
            Long deviceId = device.getLong("deviceId");
            if (!seen.add(deviceId)) throw sourceConflict("同一物料已选生产设备重复");
            facts.add(fact("EQUIPMENT:PRODUCTION:deviceId:" + deviceId, "生产设备", deviceId, "FLOW-04", "PRODUCTION_DEVICE_USED",
                    "event:" + event.getId() + "#material:" + materialId + "#device:" + deviceId,
                    event.getServerSubmitTime(), SOURCE_VIEW, Map.of(), productionEquipmentContext(device, materialId)));
        }
    }

    private List<Fact> readInspectionFacts(MesProEdhrBatchExecutionDO batch) {
        return readFormalPqcDetails(batch).stream()
                .map(detail -> {
                    if (("NUMBER".equalsIgnoreCase(detail.getResultType()) || "NUMERIC".equalsIgnoreCase(detail.getResultType()))
                            && StrUtil.isBlank(detail.getMeasuredValue())) throw sourceMissing("数值检验实测值缺失");
                    Object value = StrUtil.isNotBlank(detail.getMeasuredValue())
                            ? parsePqcValue(detail.getMeasuredValue(), detail.getResultType())
                            : detail.getJudgement() == null ? detail.getItemResult() : detail.getJudgement();
                    String evidenceKey = "INSPECTION:" + detail.getRegulationVersionId() + ":"
                            + detail.getRouteProcessId() + ":" + detail.getSampleNo() + ":" + detail.getItemCode();
                    Fact fact = fact(evidenceKey, detail.getItemName(), value, "FLOW-05", "PQC_ITEM_RESULT",
                            "pqc-aggregate-snapshot:" + detail.getId(), detail.getAggregatedAt(), SOURCE_VIEW,
                            pqcQualifiers(detail));
                    fact.item().setResultType(detail.getResultType()).setJudgement(detail.getJudgement())
                            .setUnit(detail.getStandardUnit()).setStandardPrecision(detail.getStandardPrecision());
                    setRecordedStandard(fact.item(), detail.getStandardLowerLimit() == null ? null : detail.getStandardLowerLimit().toPlainString(),
                            detail.getStandardUpperLimit() == null ? null : detail.getStandardUpperLimit().toPlainString(), detail.getStandardText());
                    return fact;
                }).toList();
    }

    private List<Fact> readMaterialFacts(MesProEdhrBatchExecutionDO batch) {
        com.alibaba.fastjson.JSONObject snapshot = readFormalSnapshot(batch);
        if (!snapshot.containsKey("pickListBindingItems") || snapshot.get("pickListBindingItems") == null) throw sourceMissing("正式领料快照明细缺失");
        List<Fact> facts = new ArrayList<>();
        for (com.alibaba.fastjson.JSONObject item : snapshotRows(snapshot.get("pickListBindingItems"))) {
            String materialNumber = item.getString("materialNumber");
            String lotNumber = item.getString("lotNumber");
            if (StrUtil.isBlank(materialNumber) || StrUtil.isBlank(lotNumber)) throw sourceMissing("正式领料明细物料或批号缺失");
            facts.add(fact("MATERIAL:" + materialNumber + ":" + lotNumber, item.getString("materialName"), lotNumber,
                    "FLOW-07", "FORMAL_MATERIAL_LOT", "pick-list-item-snapshot:" + item.getString("id"),
                    parseTime(item.getString("sourceModifyTime"))));
        }
        return facts;
    }

    private List<Fact> readPqcEquipmentFacts(MesProEdhrBatchExecutionDO batch) {
        return readFormalPqcDetails(batch).stream()
                .filter(detail -> detail.getSelectedEquipmentId() != null)
                .map(detail -> fact("EQUIPMENT:PQC:selectedEquipmentId:" + detail.getSelectedEquipmentId(),
                        StrUtil.blankToDefault(detail.getSelectedEquipmentName(), "检验设备"), detail.getSelectedEquipmentId(),
                        "FLOW-05", "PQC_EQUIPMENT_USED", "pqc-aggregate-snapshot:" + detail.getId(), detail.getAggregatedAt(),
                        SOURCE_VIEW, Map.of(), pqcEquipmentContext(detail)))
                .toList();
    }

    private List<MesPqcProcessInspectionAggregateDetailDO> readFormalPqcDetails(MesProEdhrBatchExecutionDO batch) {
        com.alibaba.fastjson.JSONObject snapshot = readFormalSnapshot(batch);
        if (!(snapshot.get("pqcDetails") instanceof Collection<?>)) throw sourceMissing("正式PQC快照明细缺失");
        List<MesPqcProcessInspectionAggregateDetailDO> details = new ArrayList<>();
        for (com.alibaba.fastjson.JSONObject row : snapshotRows(snapshot.get("pqcDetails"))) {
            MesPqcProcessInspectionAggregateDetailDO detail = new MesPqcProcessInspectionAggregateDetailDO()
                    .setId(row.getLong("id")).setPqcTaskId(row.getLong("pqcTaskId"))
                    .setRouteProcessId(row.getLong("routeProcessId"))
                    .setRegulationVersionId(row.getLong("regulationVersionId")).setSampleNo(row.getInteger("sampleNo"))
                     .setItemCode(row.getString("itemCode")).setItemName(row.getString("itemName"))
                     .setStandardText(row.getString("standardText")).setStandardLowerLimit(row.getBigDecimal("standardLowerLimit"))
                     .setStandardUpperLimit(row.getBigDecimal("standardUpperLimit")).setStandardUnit(row.getString("standardUnit"))
                     .setStandardPrecision(row.getInteger("standardPrecision")).setResultType(row.getString("resultType"))
                     .setMeasuredValue(row.getString("measuredValue")).setJudgement(row.getString("judgement"))
                     .setItemResult(row.getString("itemResult"))
                     .setSelectedEquipmentId(row.getLong("selectedEquipmentId"))
                     .setSelectedEquipmentCode(row.getString("selectedEquipmentCode"))
                     .setSelectedEquipmentName(row.getString("selectedEquipmentName"))
                     .setSelectedEquipmentNumber(row.getString("selectedEquipmentNumber"))
                     .setAggregatedAt(parseTime(row.getString("aggregatedAt")));
            if (detail.getId() == null || detail.getRegulationVersionId() == null || detail.getSampleNo() == null || detail.getRouteProcessId() == null
                    || StrUtil.isBlank(detail.getItemCode()) || StrUtil.isBlank(detail.getResultType())) throw sourceMissing("正式PQC明细身份或类型缺失");
            if (StrUtil.hasBlank(detail.getItemResult(), detail.getMeasuredValue(), detail.getJudgement())) {
                throw sourceMissing("正式PQC结果值缺失");
            }
            if (!Objects.equals(detail.getItemResult(), detail.getMeasuredValue())) {
                throw sourceConflict("正式PQC结果与实测值不一致");
            }
            details.add(detail);
        }
        return details;
    }

    private Map<String, String> pqcQualifiers(MesPqcProcessInspectionAggregateDetailDO detail) {
        Map<String, String> qualifiers = new LinkedHashMap<>();
        putQualifier(qualifiers, "regulationVersionId", detail.getRegulationVersionId() == null ? null : String.valueOf(detail.getRegulationVersionId()));
        putQualifier(qualifiers, "routeProcessId", detail.getRouteProcessId() == null ? null : String.valueOf(detail.getRouteProcessId()));
        putQualifier(qualifiers, "sampleNo", detail.getSampleNo() == null ? null : String.valueOf(detail.getSampleNo()));
        putQualifier(qualifiers, "itemCode", detail.getItemCode());
        putQualifier(qualifiers, "selectedEquipmentId", detail.getSelectedEquipmentId() == null ? null : String.valueOf(detail.getSelectedEquipmentId()));
        putQualifier(qualifiers, "selectedEquipmentCode", detail.getSelectedEquipmentCode());
        putQualifier(qualifiers, "selectedEquipmentNumber", detail.getSelectedEquipmentNumber());
        return qualifiers;
    }

    private Map<String, String> productionEquipmentContext(JSONObject device, String materialId) {
        Map<String, String> context = new LinkedHashMap<>();
        putQualifier(context, "equipmentNamespace", "PRODUCTION");
        putQualifier(context, "deviceId", device.getLong("deviceId") == null ? null : String.valueOf(device.getLong("deviceId")));
        putQualifier(context, "deviceCode", device.getString("deviceCode"));
        putQualifier(context, "materialId", materialId);
        return context;
    }

    private Map<String, String> pqcEquipmentContext(MesPqcProcessInspectionAggregateDetailDO detail) {
        Map<String, String> context = new LinkedHashMap<>(pqcQualifiers(detail));
        putQualifier(context, "equipmentNamespace", "PQC");
        return context;
    }

    private Object parsePqcValue(String value, String resultType) {
        if (StrUtil.isBlank(value)) return null;
        if ("NUMERIC".equalsIgnoreCase(resultType) || "NUMBER".equalsIgnoreCase(resultType)) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException ignored) {
                throw new IllegalStateException("SOURCE_CONFLICT:PQC数值结果无法解析");
            }
        }
        return value;
    }

    private com.alibaba.fastjson.JSONObject readFormalSnapshot(MesProEdhrBatchExecutionDO batch) {
        if (batch == null || batch.getId() == null || batch.getTenantId() == null || batch.getWorkOrderId() == null) {
            throw sourceMissing("批次或租户身份缺失");
        }
        MesProEdhrBatchExecutionOriginDO origin = resolveOrigin(batch);
        if (origin == null || origin.getCompletionBackfillReceiptId() == null) {
            throw sourceMissing("完工正式来源回执缺失");
        }
        if (origin.getWorkOrderId() == null) {
            throw sourceMissing("完工正式来源工单身份缺失");
        }
        if (!Objects.equals(origin.getWorkOrderId(), batch.getWorkOrderId())) {
            throw sourceConflict("完工正式来源工单身份冲突");
        }
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = completionReceiptMapper
                .selectByIdAndTenantId(origin.getCompletionBackfillReceiptId(), batch.getTenantId());
        MesProEdhrReverseTraceReceiptValidator.validate(batch, origin, receipt);
        return parseJsonObject(receipt.getFormalSourceSnapshotJson(), "完工正式来源快照JSON");
    }

    private static JSONObject requiredSnapshotObject(JSONObject snapshot, String key, String missingMessage) {
        Object value = snapshot.get(key);
        if (value == null) throw sourceMissing(missingMessage);
        JSONObject object = parseJsonObject(JSON.toJSONString(value), key);
        if (object == null) throw sourceMissing(missingMessage);
        return object;
    }

    private static IllegalStateException sourceMissing(String reason) {
        return new IllegalStateException("SOURCE_MISSING:" + reason);
    }

    private static IllegalStateException sourceConflict(String reason) {
        return new IllegalStateException("SOURCE_CONFLICT:" + reason);
    }

    private static JSONObject parseJsonObject(String json, String sourceName) {
        try {
            JSONObject value = JSON.parseObject(json);
            if (value == null) throw sourceConflict(sourceName + "不是JSON对象");
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("SOURCE_CONFLICT:" + sourceName + "无法解析", exception);
        }
    }

    private static List<com.alibaba.fastjson.JSONObject> snapshotRows(Object raw) {
        List<com.alibaba.fastjson.JSONObject> result = new ArrayList<>();
        if (raw instanceof Map<?, ?> groups) {
            for (Object value : groups.values()) {
                if (!(value instanceof Collection<?>)) throw sourceConflict("正式快照分组明细不是数组");
                result.addAll(snapshotRows(value));
            }
        } else if (raw instanceof Collection<?> collection) {
            for (Object value : collection) {
                if (!(value instanceof JSONObject row) || row.isEmpty()) throw sourceConflict("正式快照包含无效明细行");
                result.add(row);
            }
        } else throw sourceConflict("正式快照明细格式不正确");
        return result;
    }

    private static LocalDateTime parseTime(String value) {
        if (StrUtil.isBlank(value)) return null;
        String normalized = value.trim();
        if (normalized.matches("^-?\\d+$")) {
            try {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(normalized)), ZoneId.systemDefault());
            } catch (NumberFormatException exception) {
                throw new IllegalStateException("SOURCE_CONFLICT:正式来源时间戳超出范围", exception);
            }
        }
        for (DateTimeFormatter formatter : SOURCE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next format defined by the project's timestamp protocol.
            }
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("SOURCE_CONFLICT:正式来源时间格式不正确", exception);
        }
    }

    private MesProEdhrBatchExecutionOriginDO resolveOrigin(MesProEdhrBatchExecutionDO batch) {
        List<MesProEdhrBatchExecutionOriginDO> origins = originMapper.selectListByBatchExecutionId(batch.getId());
        if (origins == null) {
            throw new IllegalStateException("SOURCE_MISSING: 批次正式来源查询返回空结果");
        }
        if (origins.isEmpty()) return null;
        MesProEdhrBatchExecutionOriginDO first = origins.get(0);
        for (MesProEdhrBatchExecutionOriginDO origin : origins) {
            if (origin == null || origin.getActiveOrderId() == null || origin.getCompletionBackfillReceiptId() == null) throw sourceMissing("批次正式来源身份缺失");
            if (!Objects.equals(origin.getBatchExecutionId(), batch.getId()) || !Objects.equals(origin.getTenantId(), batch.getTenantId())
                    || !Objects.equals(origin.getWorkOrderId(), batch.getWorkOrderId())
                    || !Objects.equals(first.getActiveOrderId(), origin.getActiveOrderId())
                    || !Objects.equals(first.getCompletionBackfillReceiptId(), origin.getCompletionBackfillReceiptId())) {
                throw sourceConflict("批次正式来源绑定冲突");
            }
        }
        return first;
    }

    private Long resolveActiveOrderId(MesProEdhrBatchExecutionDO batch) {
        MesProEdhrBatchExecutionOriginDO origin = resolveOrigin(batch);
        return origin == null ? null : origin.getActiveOrderId();
    }

    private Fact fact(String evidenceKey, String label, Object value, String stage, String action, String sourceRef, LocalDateTime recordedAt) {
        return fact(evidenceKey, label, value, stage, action, sourceRef, recordedAt, SOURCE_VIEW);
    }

    private Fact fact(String evidenceKey, String label, Object value, String stage, String action,
                      String sourceRef, LocalDateTime recordedAt, String sourceView) {
        return fact(evidenceKey, label, value, stage, action, sourceRef, recordedAt, sourceView, Map.of());
    }

    private Fact fact(String evidenceKey, String label, Object value, String stage, String action,
                      String sourceRef, LocalDateTime recordedAt, String sourceView,
                      Map<String, String> qualifiers) {
        return fact(evidenceKey, label, value, stage, action, sourceRef, recordedAt, sourceView, qualifiers, Map.of());
    }

    private Fact fact(String evidenceKey, String label, Object value, String stage, String action,
                      String sourceRef, LocalDateTime recordedAt, String sourceView,
                      Map<String, String> qualifiers, Map<String, String> sourceContext) {
        String savedValue = value == null ? null : String.valueOf(value);
        CatalogItem item = new CatalogItem().setEvidenceKey(evidenceKey).setCategory(category).setSourceView(sourceView)
                .setSourceRef(sourceRef).setSemanticIdentity(evidenceKey).setLabel(label).setValueType(value instanceof Number ? "number" : "string")
                .setSavedValue(savedValue).setRecordedAt(recordedAt).setRecordStatus("RECORDED")
                .setQualifiers(qualifiers == null ? Map.of() : Map.copyOf(qualifiers))
                .setAllowedOperators(category == Category.INSPECTION
                        ? List.of("EQ", "NE", "GT", "GE", "LT", "LE", "BETWEEN", "JUDGEMENT_EQ")
                        : List.of("EQ", "NE", "GT", "GE", "LT", "LE", "BETWEEN", "OUT_OF_LIMIT"));
        return new Fact(item, stage, action, sourceContext == null ? Map.of() : Map.copyOf(sourceContext));
    }

    private boolean matches(Condition condition, CatalogItem item) {
        if (item == null) return false;
        if (!qualifiersMatch(condition, item)) return false;
        String operator = condition.getOperator().toUpperCase(Locale.ROOT);
        if ("JUDGEMENT_EQ".equals(operator)) return Objects.equals(item.getJudgement(), String.valueOf(condition.getValue()));
        if (item.getSavedValue() == null) return false;
        String actual = item.getSavedValue();
        String expected = condition.getValue() instanceof Map<?, ?> map && map.get("decimal") != null ? String.valueOf(map.get("decimal")) : String.valueOf(condition.getValue());
        boolean numeric = "number".equals(item.getValueType());
        if ("EQ".equals(operator) || "NE".equals(operator)) {
            boolean equal = numeric ? new BigDecimal(actual).compareTo(new BigDecimal(expected)) == 0 : actual.equals(expected);
            return "EQ".equals(operator) ? equal : !equal;
        }
        if (!numeric) return false;
        if ("OUT_OF_LIMIT".equals(operator)) {
            if (!hasRecordedBounds(item)) throw sourceMissing("数值事实缺少当时标准，不能判断超限");
            BigDecimal number = new BigDecimal(actual);
            return item.getLowerLimit() != null && number.compareTo(new BigDecimal(item.getLowerLimit())) < 0
                    || item.getUpperLimit() != null && number.compareTo(new BigDecimal(item.getUpperLimit())) > 0;
        }
        if ("BETWEEN".equals(operator)) {
            BigDecimal[] bounds = numericBounds(condition.getValue());
            if (bounds == null) throw new IllegalArgumentException("CONDITION_INVALID:数值区间无效");
            BigDecimal numericActual = new BigDecimal(actual);
            return numericActual.compareTo(bounds[0]) >= 0 && numericActual.compareTo(bounds[1]) <= 0;
        }
        BigDecimal left = new BigDecimal(actual), right = new BigDecimal(expected);
        return switch (operator) { case "GT" -> left.compareTo(right) > 0; case "GE" -> left.compareTo(right) >= 0; case "LT" -> left.compareTo(right) < 0; case "LE" -> left.compareTo(right) <= 0; default -> false; };
    }

    private void setRecordedStandard(CatalogItem item, String lower, String upper, String text) {
        String lowerValue = StrUtil.isBlank(lower) ? null : lower;
        String upperValue = StrUtil.isBlank(upper) ? null : upper;
        try {
            BigDecimal min = lowerValue == null ? null : new BigDecimal(lowerValue);
            BigDecimal max = upperValue == null ? null : new BigDecimal(upperValue);
            if (min != null && max != null && min.compareTo(max) > 0) throw sourceConflict("正式数值标准上下限倒置");
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("SOURCE_CONFLICT:正式数值标准无效", exception);
        }
        item.setLowerLimit(lowerValue).setUpperLimit(upperValue);
        item.setRecordedStandard(StrUtil.isNotBlank(text) ? text : lowerValue != null && upperValue != null
                ? lowerValue + " ~ " + upperValue : lowerValue != null ? ">= " + lowerValue : upperValue != null ? "<= " + upperValue : null);
    }

    private boolean hasRecordedBounds(CatalogItem item) {
        return item.getLowerLimit() != null || item.getUpperLimit() != null;
    }

    private static void putQualifier(Map<String, String> qualifiers, String key, String value) {
        if (StrUtil.isNotBlank(value)) qualifiers.put(key, value);
    }

    private boolean qualifiersMatch(Condition condition, CatalogItem item) {
        if (category == Category.PARAMETER
                && (!hasCompleteParameterSemanticIdentity(condition.getQualifiers())
                || !hasCompleteParameterSemanticIdentity(item.getQualifiers()))) {
            return false;
        }
        if (condition.getQualifiers() == null || condition.getQualifiers().isEmpty()) return true;
        if (item.getQualifiers() == null) return false;
        return condition.getQualifiers().entrySet().stream()
                .allMatch(entry -> Objects.equals(entry.getValue(), item.getQualifiers().get(entry.getKey())));
    }

    private static boolean hasCompleteParameterSemanticIdentity(Map<String, String> qualifiers) {
        return qualifiers != null
                && StrUtil.isNotBlank(qualifiers.get("routeVersionId"))
                && StrUtil.isNotBlank(qualifiers.get("routeProcessId"))
                && StrUtil.isNotBlank(qualifiers.get("processId"))
                && qualifiers.containsKey("unit");
    }

    private static String parameterSemanticIdentity(String code, Map<String, String> qualifiers) {
        if (!hasCompleteParameterSemanticIdentity(qualifiers)) {
            throw sourceMissing("正式参数语义身份缺失: " + code);
        }
        return "PARAMETER|" + code + PARAMETER_SEMANTIC_IDENTITY_KEYS.stream()
                .map(key -> "|" + StrUtil.blankToDefault(qualifiers.get(key), ""))
                .collect(java.util.stream.Collectors.joining());
    }

    private boolean isNumericConditionValue(Object value, String operator) {
        if ("BETWEEN".equals(operator)) return numericBounds(value) != null;
        try {
            if (value instanceof Map<?, ?> map && map.get("decimal") != null) {
                new BigDecimal(String.valueOf(map.get("decimal")));
            } else {
                new BigDecimal(String.valueOf(value));
            }
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private BigDecimal[] numericBounds(Object value) {
        try {
            if (value instanceof Map<?, ?> map) {
                Object lower = map.containsKey("lower") ? map.get("lower") : map.get("min");
                Object upper = map.containsKey("upper") ? map.get("upper") : map.get("max");
                if (lower != null && upper != null) {
                    BigDecimal min = new BigDecimal(String.valueOf(lower)), max = new BigDecimal(String.valueOf(upper));
                    return min.compareTo(max) <= 0 ? new BigDecimal[]{min, max} : null;
                }
            }
            String[] parts = String.valueOf(value).split(",", -1);
            if (parts.length == 2) {
                BigDecimal min = new BigDecimal(parts[0].trim()), max = new BigDecimal(parts[1].trim());
                return min.compareTo(max) <= 0 ? new BigDecimal[]{min, max} : null;
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private String sourceVersion(MesProEdhrBatchExecutionDO batch) { return category.name() + ":formal-v1"; }
    private String sourceVersion(List<Fact> facts, MesProEdhrBatchExecutionDO batch) {
        String canonical = facts.stream().map(fact -> MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(JSON.toJSONString(fact.item())
                        + "|" + JSON.toJSONString(fact.sourceContext())))
                .sorted().collect(java.util.stream.Collectors.joining("\n"));
        return category.name() + ":formal-v2:" + DigestUtil.sha256Hex(canonical);
    }
    private String sourceIdentity(MesProEdhrBatchExecutionDO batch) { return category.name() + ":batch:" + batch.getId(); }
    private record Fact(CatalogItem item, String stage, String action, Map<String, String> sourceContext) { }
}
