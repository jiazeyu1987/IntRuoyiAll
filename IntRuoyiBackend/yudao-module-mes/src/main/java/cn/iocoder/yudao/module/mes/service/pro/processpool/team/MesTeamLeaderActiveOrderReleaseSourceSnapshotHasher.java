package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Component
public class MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher {

    public static final String VERSION = "AO_RELEASE_SOURCE_V1";

    public String hash(Input input) {
        Objects.requireNonNull(input, "source snapshot input is required");
        Map<String, Object> root = map();
        root.put("version", VERSION);
        root.put("tenantId", input.tenantId());
        root.put("activeOrder", activeOrder(input.activeOrder()));
        root.put("workOrder", workOrder(input.workOrder()));
        root.put("product", product(input.product()));
        root.put("route", route(input.route()));
        root.put("routeVersion", routeVersion(input.routeVersion()));
        root.put("processSnapshots", processSnapshots(input.processSnapshots()));
        root.put("productionCompletions", productionCompletions(input.completions()));
        root.put("writerPlans", writerPlans(input));
        root.put("releaseApproval", releaseApproval(input.releaseApprovalRule(),
                input.releaseCandidateSourceType(), input.releaseCandidateSourceId(),
                input.releaseOwnerCandidateUserIds()));
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(root));
    }

    private Map<String, Object> activeOrder(MesProcessPoolActiveOrderDO activeOrder) {
        Map<String, Object> value = map();
        value.put("id", required(activeOrder).getId());
        value.put("leaderUserId", activeOrder.getLeaderUserId());
        value.put("workOrderId", activeOrder.getWorkOrderId());
        value.put("routeId", activeOrder.getRouteId());
        value.put("routeVersionId", activeOrder.getRouteVersionId());
        value.put("activeStatus", activeOrder.getActiveStatus());
        value.put("businessStatus", activeOrder.getBusinessStatus());
        value.put("erpFixedQuantity", decimal(activeOrder.getErpFixedQuantitySnapshot()));
        return value;
    }

    private Map<String, Object> workOrder(MesProWorkOrderDO workOrder) {
        Map<String, Object> value = map();
        value.put("id", required(workOrder).getId());
        value.put("code", workOrder.getCode());
        value.put("productId", workOrder.getProductId());
        value.put("batchCode", workOrder.getBatchCode());
        value.put("quantity", decimal(workOrder.getQuantity()));
        return value;
    }

    private Map<String, Object> product(MesMdItemDO product) {
        Map<String, Object> value = map();
        value.put("id", required(product).getId());
        value.put("code", product.getCode());
        value.put("specification", product.getSpecification());
        value.put("unitMeasureId", product.getUnitMeasureId());
        value.put("itemTypeId", product.getItemTypeId());
        value.put("status", product.getStatus());
        value.put("batchFlag", product.getBatchFlag());
        value.put("updatedAt", time(product.getUpdateTime()));
        return value;
    }

    private Map<String, Object> route(MesProRouteDO route) {
        Map<String, Object> value = map();
        value.put("id", required(route).getId());
        value.put("code", route.getCode());
        value.put("status", route.getStatus());
        value.put("updatedAt", time(route.getUpdateTime()));
        return value;
    }

    private Map<String, Object> routeVersion(MesProRouteVersionDO routeVersion) {
        Map<String, Object> value = map();
        value.put("id", required(routeVersion).getId());
        value.put("routeId", routeVersion.getRouteId());
        value.put("versionNo", routeVersion.getVersionNo());
        value.put("active", routeVersion.getActive());
        value.put("lifecycleStatus", routeVersion.getLifecycleStatus());
        value.put("sourceRouteVersionId", routeVersion.getSourceRouteVersionId());
        value.put("routeSnapshotJson", routeVersion.getRouteSnapshotJson());
        value.put("publishedBy", routeVersion.getPublishedBy());
        value.put("publishedTime", time(routeVersion.getPublishedTime()));
        value.put("updatedAt", time(routeVersion.getUpdateTime()));
        return value;
    }

    private List<Map<String, Object>> processSnapshots(
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        return list(snapshots).stream()
                .sorted(Comparator.comparing(MesProcessPoolActiveOrderProcessSnapshotDO::getRouteProcessId)
                        .thenComparing(MesProcessPoolActiveOrderProcessSnapshotDO::getProcessId)
                        .thenComparing(MesProcessPoolActiveOrderProcessSnapshotDO::getId))
                .map(snapshot -> {
                    Map<String, Object> value = map();
                    value.put("id", snapshot.getId());
                    value.put("activeOrderId", snapshot.getActiveOrderId());
                    value.put("workOrderId", snapshot.getWorkOrderId());
                    value.put("routeId", snapshot.getRouteId());
                    value.put("routeVersionId", snapshot.getRouteVersionId());
                    value.put("routeProcessId", snapshot.getRouteProcessId());
                    value.put("processId", snapshot.getProcessId());
                    return value;
                }).toList();
    }

    private List<Map<String, Object>> productionCompletions(
            List<MesProcessPoolOrderProcessCompletionDO> completions) {
        return list(completions).stream()
                .sorted(Comparator.comparing(MesProcessPoolOrderProcessCompletionDO::getRouteProcessId)
                        .thenComparing(MesProcessPoolOrderProcessCompletionDO::getProcessId)
                        .thenComparing(MesProcessPoolOrderProcessCompletionDO::getId))
                .map(completion -> {
                    Map<String, Object> value = map();
                    value.put("id", completion.getId());
                    value.put("workOrderId", completion.getWorkOrderId());
                    value.put("routeProcessId", completion.getRouteProcessId());
                    value.put("processId", completion.getProcessId());
                    value.put("targetQuantity", decimal(completion.getTargetQuantity()));
                    value.put("confirmedQuantity", decimal(completion.getConfirmedQuantity()));
                    value.put("completionStatus", completion.getCompletionStatus());
                    value.put("completedAt", time(completion.getCompletedAt()));
                    value.put("backfillStatus", completion.getBackfillStatus());
                    value.put("backfillExecutionId", completion.getBackfillExecutionId());
                    value.put("lastEventId", completion.getLastEventId());
                    value.put("lastReviewId", completion.getLastReviewId());
                    value.put("aggregateHash", completion.getAggregateHash());
                    value.put("backfillIdempotencyKey", completion.getBackfillIdempotencyKey());
                    return value;
                }).toList();
    }

    private List<Map<String, Object>> writerPlans(Input input) {
        List<Map<String, Object>> plans = new ArrayList<>();
        plans.add(batchPlan(input.batchRecordPlan()));
        plans.add(inspectionPlan(input.processInspectionPlan()));
        plans.add(lossPlan(input.lossReportPlan()));
        return plans.stream().sorted(Comparator.comparing(item -> String.valueOf(item.get("documentType")))).toList();
    }

    private Map<String, Object> batchPlan(MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan) {
        Map<String, Object> value = writerEvidence("BATCH_RECORD", plan.getSourceObjectIds(),
                plan.getSourceValueHashes(), batchSignatures(plan));
        value.put("targets", list(plan.getPreparedProcesses()).stream().map(prepared -> target(
                prepared.getSource().getSnapshot().getRouteProcessId(),
                prepared.getSource().getSnapshot().getProcessId(), prepared.getBinding(), prepared.getRules(), null))
                .sorted(Comparator.comparing(JsonUtils::toJsonString))
                .toList());
        return value;
    }

    private Map<String, Object> inspectionPlan(MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan) {
        Map<String, Object> value = writerEvidence("PROCESS_INSPECTION", plan.getSourceObjectIds(),
                plan.getSourceValueHashes(), inspectionSignatures(plan));
        value.put("targets", list(plan.getPreparedInspections()).stream()
                .map(this::inspectionTarget)
                .sorted(Comparator.comparing(JsonUtils::toJsonString))
                .toList());
        return value;
    }

    private Map<String, Object> inspectionTarget(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection prepared) {
        Map<String, Object> value = map();
        value.put("routeProcessId", prepared.getSource().getTask().getRouteProcessId());
        value.put("processId", prepared.getSource().getTask().getProcessId());
        value.put("binding", binding(prepared.getBinding()));
        value.put("mappedRules", list(prepared.getMappedValues()).stream().map(mapped -> {
            Map<String, Object> pair = map();
            pair.put("mappingRule", rule(mapped.getRule()));
            pair.put("mappedValue", normalize(mapped.getValue()));
            return pair;
        }).sorted(Comparator.comparing(JsonUtils::toJsonString)).toList());
        return value;
    }

    private Map<String, Object> lossPlan(MesTeamLeaderActiveOrderReleaseLossReportPlan plan) {
        Map<String, Object> value = writerEvidence("LOSS_REPORT", plan.getSourceObjectIds(),
                plan.getSourceValueHashes(), lossSignatures(plan));
        value.put("targets", list(plan.getPreparedReports()).stream().map(prepared -> target(
                prepared.getSources().get(0).getSnapshot().getRouteProcessId(),
                prepared.getSources().get(0).getSnapshot().getProcessId(), prepared.getBinding(),
                prepared.getRules(), prepared.getMappedValues().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> entry.getKey() + "=" + normalize(entry.getValue())).toList()))
                .sorted(Comparator.comparing(JsonUtils::toJsonString))
                .toList());
        return value;
    }

    private Map<String, Object> writerEvidence(String documentType, List<Long> sourceObjectIds,
                                               List<String> sourceValueHashes,
                                               List<Map<String, Object>> signatures) {
        Map<String, Object> value = map();
        value.put("documentType", documentType);
        value.put("sourceObjectIds", list(sourceObjectIds).stream().sorted().toList());
        value.put("sourceValueHashes", list(sourceValueHashes).stream().sorted().toList());
        value.put("signatureEvidence", signatures.stream()
                .sorted(Comparator.comparing(item -> JsonUtils.toJsonString(item))).toList());
        return value;
    }

    private Map<String, Object> target(Long routeProcessId, Long processId,
                                       MesProRouteFlowProcessBatchRecordDO binding,
                                       List<MesProBatchRecordCellLinkRuleDO> rules,
                                       List<?> mappedValues) {
        Map<String, Object> value = map();
        value.put("routeProcessId", routeProcessId);
        value.put("processId", processId);
        value.put("binding", binding(binding));
        value.put("mappingRules", list(rules).stream()
                .sorted(Comparator.comparing(MesProBatchRecordCellLinkRuleDO::getId))
                .map(this::rule).toList());
        value.put("mappedValues", mappedValues == null ? List.of() : mappedValues);
        return value;
    }

    private Map<String, Object> binding(MesProRouteFlowProcessBatchRecordDO binding) {
        Map<String, Object> value = map();
        value.put("id", binding.getId());
        value.put("routeFlowProcessConfigId", binding.getRouteFlowProcessConfigId());
        value.put("routeId", binding.getRouteId());
        value.put("routeProcessId", binding.getRouteProcessId());
        value.put("useType", binding.getUseType());
        value.put("reportId", binding.getBatchRecordReportId());
        value.put("definitionId", binding.getBatchRecordDefinitionId());
        value.put("versionId", binding.getBatchRecordVersionId());
        value.put("recordCategory", binding.getRecordCategory());
        value.put("formSlotType", binding.getFormSlotType());
        value.put("formBindingKey", binding.getFormBindingKey());
        value.put("formTemplateId", binding.getFormTemplateId());
        value.put("lastPublishedTemplateVersionId", binding.getLastPublishedTemplateVersionId());
        value.put("instanceScope", binding.getInstanceScope());
        value.put("sharedFormKey", binding.getSharedFormKey());
        value.put("fillableScopeJson", binding.getFillableScopeJson());
        value.put("validationProfile", binding.getValidationProfile());
        value.put("recordbookEnabled", binding.getRecordbookEnabled());
        value.put("permissionScopeId", binding.getPermissionScopeId());
        value.put("requiredPolicy", binding.getRequiredPolicy());
        value.put("requiredConditionJson", binding.getRequiredConditionJson());
        value.put("ownerRoleKey", binding.getOwnerRoleKey());
        value.put("archiveVisibility", binding.getArchiveVisibility());
        value.put("recordCategorySnapshotHash", binding.getRecordCategorySnapshotHash());
        value.put("slotConfigSnapshotHash", binding.getSlotConfigSnapshotHash());
        value.put("updatedAt", time(binding.getUpdateTime()));
        return value;
    }

    private Map<String, Object> rule(MesProBatchRecordCellLinkRuleDO rule) {
        Map<String, Object> value = map();
        value.put("id", rule.getId());
        value.put("ruleVersion", rule.getRuleVersion());
        value.put("scopeType", rule.getScopeType());
        value.put("scopeId", rule.getScopeId());
        value.put("routeId", rule.getRouteId());
        value.put("definitionId", rule.getBatchRecordDefinitionId());
        value.put("versionId", rule.getBatchRecordVersionId());
        value.put("sourceType", rule.getSourceType());
        value.put("sourceReportId", rule.getSourceReportId());
        value.put("sourceRowIndex", rule.getSourceRowIndex());
        value.put("sourceColumnIndex", rule.getSourceColumnIndex());
        value.put("sourceCellKey", rule.getSourceCellKey());
        value.put("sourceFieldCode", rule.getSourceFieldCode());
        value.put("sourceValueType", rule.getSourceValueType());
        value.put("targetReportId", rule.getTargetReportId());
        value.put("targetRowIndex", rule.getTargetRowIndex());
        value.put("targetColumnIndex", rule.getTargetColumnIndex());
        value.put("targetCellKey", rule.getTargetCellKey());
        value.put("targetValueType", rule.getTargetValueType());
        value.put("aggregationStrategy", rule.getAggregationStrategy());
        value.put("overwritePolicy", rule.getOverwritePolicy());
        value.put("templateSnapshotHash", rule.getTemplateSnapshotHash());
        value.put("enabled", rule.getEnabled());
        value.put("updatedAt", time(rule.getUpdateTime()));
        return value;
    }

    private Map<String, Object> releaseApproval(MesProEdhrWorkTaskAssignmentRuleDO rule,
                                                String candidateSourceType, Long candidateSourceId,
                                                List<Long> candidateUserIds) {
        Map<String, Object> value = map();
        value.put("ruleId", rule == null ? null : rule.getId());
        value.put("routeProcessId", rule == null ? null : rule.getRouteProcessId());
        value.put("scopeType", rule == null ? null : rule.getScopeType());
        value.put("scopeId", rule == null ? null : rule.getScopeId());
        value.put("taskType", rule == null ? null : rule.getTaskType());
        value.put("assigneeUserId", rule == null ? null : rule.getAssigneeUserId());
        value.put("reviewUserId", rule == null ? null : rule.getReviewUserId());
        value.put("configuredCandidateSourceType", rule == null ? null : rule.getCandidateSourceType());
        value.put("configuredCandidateSourceId", rule == null ? null : rule.getCandidateSourceId());
        value.put("dueMinutes", rule == null ? null : rule.getDueMinutes());
        value.put("enabled", rule == null ? null : rule.getEnabled());
        value.put("candidateSourceType", candidateSourceType);
        value.put("candidateSourceId", candidateSourceId);
        value.put("candidateUserIds", list(candidateUserIds).stream().sorted().toList());
        return value;
    }

    private List<Map<String, Object>> batchSignatures(MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan) {
        return list(plan.getSignatureEvidence()).stream().map(item -> signature(item.getRole(), item.getSourceType(),
                item.getSourceId(), item.getSignatureId(), item.getUserId(), item.getSignedAt(), item.getEvidenceHash()))
                .toList();
    }

    private List<Map<String, Object>> inspectionSignatures(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan) {
        return list(plan.getSignatureEvidence()).stream().map(item -> signature(item.getRole(), item.getSourceType(),
                item.getSourceId(), item.getSignatureId(), item.getUserId(), item.getSignedAt(), item.getEvidenceHash()))
                .toList();
    }

    private List<Map<String, Object>> lossSignatures(MesTeamLeaderActiveOrderReleaseLossReportPlan plan) {
        return list(plan.getSignatureEvidence()).stream().map(item -> signature(item.getRole(), item.getSourceType(),
                item.getSourceId(), item.getSignatureId(), item.getUserId(), item.getSignedAt(), item.getEvidenceHash()))
                .toList();
    }

    private Map<String, Object> signature(String role, String sourceType, Long sourceId, Long signatureId,
                                          Long userId, LocalDateTime signedAt, String evidenceHash) {
        Map<String, Object> value = map();
        value.put("role", role);
        value.put("sourceType", sourceType);
        value.put("sourceId", sourceId);
        value.put("signatureId", signatureId);
        value.put("userId", userId);
        value.put("signedAt", time(signedAt));
        value.put("evidenceHash", evidenceHash);
        return value;
    }

    private Object normalize(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value instanceof BigDecimal decimal ? decimal(decimal) : value;
        }
        if (value instanceof LocalDateTime time) {
            return time(time);
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        throw new IllegalArgumentException("Unsupported canonical source value type: " + value.getClass().getName());
    }

    private String decimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String time(LocalDateTime value) {
        return value == null ? null : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                value.truncatedTo(ChronoUnit.SECONDS));
    }

    private <T> List<T> list(List<T> values) {
        return values == null ? List.of() : values;
    }

    private <T> T required(T value) {
        return Objects.requireNonNull(value, "canonical source object is required");
    }

    private Map<String, Object> map() {
        return new TreeMap<>();
    }

    public record Input(
            Long tenantId,
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            MesMdItemDO product,
            MesProRouteDO route,
            MesProRouteVersionDO routeVersion,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> processSnapshots,
            List<MesProcessPoolOrderProcessCompletionDO> completions,
            MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchRecordPlan,
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlan processInspectionPlan,
            MesTeamLeaderActiveOrderReleaseLossReportPlan lossReportPlan,
            MesProEdhrWorkTaskAssignmentRuleDO releaseApprovalRule,
            String releaseCandidateSourceType,
            Long releaseCandidateSourceId,
            List<Long> releaseOwnerCandidateUserIds) {
    }
}
