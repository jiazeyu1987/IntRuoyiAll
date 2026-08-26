package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.PqcResultValueValidator;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcProcessInspectionAggregationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_CHANGE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH;

@Service
public class MesProcessPoolPqcInspectionCorrectionService {

    private static final String PQC_INSPECTION_TASK_SOURCE_TYPE = "MES_PQC_INSPECTION_TASK";

    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcInspectionPieceDetailMapper pieceDetailMapper;
    private final MesProcessPoolEventRevisionService revisionService;
    private final MesProBatchRecordExecutionSignatureService signatureService;
    private final MesTeamLeaderScopeService scopeService;
    private final MesReportAllocationReleaseStateService releaseStateService;
    private final MesPqcProcessInspectionAggregationService aggregationService;

    public MesProcessPoolPqcInspectionCorrectionService(
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesPqcInspectionTaskMapper pqcTaskMapper,
            MesPqcInspectionPieceDetailMapper pieceDetailMapper,
            MesProcessPoolEventRevisionService revisionService,
            MesProBatchRecordExecutionSignatureService signatureService,
            MesTeamLeaderScopeService scopeService,
            MesReportAllocationReleaseStateService releaseStateService,
            MesPqcProcessInspectionAggregationService aggregationService) {
        this.eventMapper = eventMapper;
        this.pqcRecordMapper = pqcRecordMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.pieceDetailMapper = pieceDetailMapper;
        this.revisionService = revisionService;
        this.signatureService = signatureService;
        this.scopeService = scopeService;
        this.releaseStateService = releaseStateService;
        this.aggregationService = aggregationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long correct(MesProcessPoolPqcInspectionCorrectionCommand command) {
        validateCommand(command);
        MesProProcessPoolEventDO event = eventMapper.selectByIdForUpdate(command.getEventId());
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, command.getEventId());
        }
        validatePqcEvent(event);
        scopeService.assertCanAccessEmployee(command.getActorUserId(),
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, event.getActualEmployeeId());
        MesPqcInspectionTaskDO task = pqcTaskMapper.selectByIdForUpdate(event.getFeedbackSourceId());
        validateTask(event, task);
        if (releaseStateService.findReleasedActiveOrderIdsForUpdate(List.of(task.getActiveOrderId()))
                .contains(task.getActiveOrderId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "releasedPqcInspectionForm");
        }

        MesProProcessPoolPqcRecordDO record = pqcRecordMapper.selectByEventId(event.getId());
        if (record == null || !Objects.equals(record.getTenantId(), event.getTenantId())
                || !Objects.equals(record.getEventId(), event.getId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcRecord");
        }
        List<MesPqcInspectionPieceDetailDO> existingDetails = pieceDetailMapper.selectListByTaskId(task.getId());
        validateExistingDetails(task, existingDetails);
        List<MesPqcInspectionPieceDetailDO> updatedDetails = buildUpdatedDetails(command, task, existingDetails);
        String inspectionResult = resolveInspectionResult(command.getScrapQuantity(), updatedDetails);
        ObjectNode afterPayload = buildAfterPayload(event, task, command, updatedDetails, inspectionResult);
        List<MesProcessPoolEventRevisionFieldChangeBO> changes = buildChanges(
                event, task, command, existingDetails, updatedDetails, inspectionResult);
        if (changes.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED);
        }

        String afterPayloadJson = JsonUtils.toJsonString(afterPayload);
        MesProBatchRecordExecutionFieldAuditSignatureResult signature =
                recordCorrectionSignature(command, event.getId(), afterPayloadJson);
        Long revisionId = revisionService.updatePqcInspectionRecord(
                MesProcessPoolEventRevisionUpdateReqBO.builder()
                        .eventId(event.getId())
                        .afterPayload(afterPayloadJson)
                        .changeReason(command.getChangeReason().trim())
                        .revisionSignatureId(signature.getSignatureId())
                        .revisionSignatureUserId(signature.getActorId())
                        .revisionSignatureSnapshot(JsonUtils.toJsonString(signature))
                        .modifiedByUserId(signature.getActorId())
                        .changedFields(changes)
                        .build());

        updateFormalPqcTables(record, task, command, updatedDetails, afterPayloadJson, inspectionResult);
        if (MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_AGGREGATED.equals(
                record.getProcessInspectionAggregationStatus())) {
            if (record.getProcessInspectionReviewId() == null) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "processInspectionReviewId");
            }
            aggregationService.aggregateApprovedPqcSubmission(event.getId(), record.getProcessInspectionReviewId());
        }
        return revisionId;
    }

    private void validateCommand(MesProcessPoolPqcInspectionCorrectionCommand command) {
        if (command == null || command.getEventId() == null || command.getEventId() <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "eventId");
        }
        if (command.getActorUserId() == null || command.getActorUserId() <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "actorUserId");
        }
        if (command.getActualInspectionQuantity() == null || command.getActualInspectionQuantity() <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "actualInspectionQuantity");
        }
        if (command.getScrapQuantity() == null || command.getScrapQuantity() < 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "scrapQuantity");
        }
        if (CollUtil.isEmpty(command.getItemResults())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "itemResults");
        }
        if (StrUtil.isBlank(command.getChangeReason())) {
            throw exception(PRO_PROCESS_POOL_REVISION_CHANGE_REASON_REQUIRED);
        }
        if (StrUtil.isBlank(command.getSignaturePassword())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "signaturePassword");
        }
    }

    private void validatePqcEvent(MesProProcessPoolEventDO event) {
        if (!MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())
                || !PQC_INSPECTION_TASK_SOURCE_TYPE.equals(event.getFeedbackSourceType())
                || !PQC_INSPECTION_TASK_SOURCE_TYPE.equals(event.getRecordbookSourceType())
                || event.getFeedbackSourceId() == null
                || !Objects.equals(event.getFeedbackSourceId(), event.getRecordbookSourceId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcInspectionEvent");
        }
    }

    private void validateTask(MesProProcessPoolEventDO event, MesPqcInspectionTaskDO task) {
        if (task == null
                || task.getActiveOrderId() == null
                || !Objects.equals(event.getTenantId(), task.getTenantId())
                || !Objects.equals(event.getFeedbackSourceId(), task.getId())
                || !Objects.equals(event.getWorkOrderId(), task.getWorkOrderId())
                || !Objects.equals(event.getRouteId(), task.getRouteId())
                || !Objects.equals(event.getRouteProcessId(), task.getRouteProcessId())
                || !Objects.equals(event.getProcessId(), task.getProcessId())
                || !(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED.equals(task.getTaskStatus())
                || MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus()))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcInspectionTask");
        }
    }

    private void validateExistingDetails(MesPqcInspectionTaskDO task, List<MesPqcInspectionPieceDetailDO> details) {
        if (CollUtil.isEmpty(details)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcInspectionPieceDetails");
        }
        for (MesPqcInspectionPieceDetailDO detail : details) {
            if (detail == null || detail.getId() == null || !Objects.equals(task.getTenantId(), detail.getTenantId())
                    || !Objects.equals(task.getId(), detail.getTaskId()) || detail.getSampleNo() == null
                    || StrUtil.isBlank(detail.getItemCode()) || StrUtil.isBlank(detail.getMeasuredValue())) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcInspectionPieceDetails");
            }
        }
    }

    private List<MesPqcInspectionPieceDetailDO> buildUpdatedDetails(
            MesProcessPoolPqcInspectionCorrectionCommand command,
            MesPqcInspectionTaskDO task,
            List<MesPqcInspectionPieceDetailDO> existingDetails) {
        Map<String, List<MesPqcInspectionPieceDetailDO>> existingByItem = existingDetails.stream()
                .collect(Collectors.groupingBy(MesPqcInspectionPieceDetailDO::getItemCode,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, MesProcessPoolPqcInspectionCorrectionCommand.ItemResultCommand> requestedByItem =
                command.getItemResults().stream()
                        .collect(Collectors.toMap(
                                item -> normalizeItemCode(item.getItemCode()),
                                Function.identity(),
                                (left, right) -> {
                                    throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "itemResults.itemCode");
                                },
                                LinkedHashMap::new));
        if (!existingByItem.keySet().equals(requestedByItem.keySet())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "itemResults.itemCode");
        }
        List<MesPqcInspectionPieceDetailDO> updated = new ArrayList<>();
        for (Map.Entry<String, List<MesPqcInspectionPieceDetailDO>> entry : existingByItem.entrySet()) {
            MesProcessPoolPqcInspectionCorrectionCommand.ItemResultCommand requested = requestedByItem.get(entry.getKey());
            List<String> values = requested.getSampleValues();
            if (values == null || values.size() != command.getActualInspectionQuantity()) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "itemResults.sampleValues");
            }
            MesPqcInspectionPieceDetailDO template = entry.getValue().get(0);
            for (int i = 0; i < values.size(); i += 1) {
                String measuredValue = Objects.toString(values.get(i), "").trim();
                if (StrUtil.isBlank(measuredValue)) {
                    throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "itemResults.sampleValues");
                }
                MesPqcInspectionPieceDetailDO detail = copyDetailTemplate(template, task, i + 1, measuredValue);
                detail.setSelectedEquipmentId(requested.getSelectedEquipmentId() == null
                        ? template.getSelectedEquipmentId() : requested.getSelectedEquipmentId());
                detail.setSelectedEquipmentNumber(StrUtil.blankToDefault(
                        requested.getSelectedEquipmentNumber(), template.getSelectedEquipmentNumber()));
                detail.setJudgement(resolvePieceJudgement(detail, measuredValue));
                updated.add(detail);
            }
        }
        return updated;
    }

    private MesPqcInspectionPieceDetailDO copyDetailTemplate(MesPqcInspectionPieceDetailDO template,
                                                            MesPqcInspectionTaskDO task,
                                                            int sampleNo,
                                                            String measuredValue) {
        MesPqcInspectionPieceDetailDO detail = MesPqcInspectionPieceDetailDO.builder()
                .taskId(task.getId())
                .sampleNo(sampleNo)
                .itemCode(template.getItemCode())
                .itemName(template.getItemName())
                .inspectionMethod(template.getInspectionMethod())
                .standardText(template.getStandardText())
                .selectedEquipmentId(template.getSelectedEquipmentId())
                .selectedEquipmentCode(template.getSelectedEquipmentCode())
                .selectedEquipmentName(template.getSelectedEquipmentName())
                .selectedEquipmentNumber(template.getSelectedEquipmentNumber())
                .standardLowerLimit(template.getStandardLowerLimit())
                .standardUpperLimit(template.getStandardUpperLimit())
                .standardUnit(template.getStandardUnit())
                .standardPrecision(template.getStandardPrecision())
                .resultType(template.getResultType())
                .itemResult(measuredValue)
                .measuredValue(measuredValue)
                .build();
        detail.setTenantId(task.getTenantId());
        return detail;
    }

    private ObjectNode buildAfterPayload(MesProProcessPoolEventDO event,
                                         MesPqcInspectionTaskDO task,
                                         MesProcessPoolPqcInspectionCorrectionCommand command,
                                         List<MesPqcInspectionPieceDetailDO> details,
                                         String inspectionResult) {
        ObjectNode payload = requireObject(event.getRawPayload(), "rawPayload").deepCopy();
        payload.put("activeOrderId", task.getActiveOrderId());
        payload.put("pqcTaskId", task.getId());
        payload.put("actualInspectionQuantity", command.getActualInspectionQuantity());
        payload.put("inspectionQuantity", command.getActualInspectionQuantity());
        payload.put("scrapQuantity", command.getScrapQuantity());
        payload.put("inspectionResult", inspectionResult);
        payload.remove("nonconformanceDescription");
        payload.remove("defectDescription");
        JsonNode pqcDraft = payload.get("pqcDraft");
        if (pqcDraft instanceof ObjectNode) {
            ((ObjectNode) pqcDraft).remove("defectDescription");
            ((ObjectNode) pqcDraft).remove("nonconformanceDescription");
        }
        payload.set("pqcItemDetails", buildPqcItemDetailsSnapshot(details));
        payload.put("pieceDetailCount", details.size());
        return payload;
    }

    private ArrayNode buildPqcItemDetailsSnapshot(List<MesPqcInspectionPieceDetailDO> details) {
        Map<String, ObjectNode> snapshotByItem = new LinkedHashMap<>();
        for (MesPqcInspectionPieceDetailDO detail : details) {
            ObjectNode item = snapshotByItem.computeIfAbsent(detail.getItemCode(), key -> {
                ObjectNode value = JsonUtils.getObjectMapper().createObjectNode();
                value.put("itemCode", detail.getItemCode());
                putNullable(value, "itemName", detail.getItemName());
                putNullable(value, "selectedEquipmentId", detail.getSelectedEquipmentId());
                putNullable(value, "selectedEquipmentCode", detail.getSelectedEquipmentCode());
                putNullable(value, "selectedEquipmentName", detail.getSelectedEquipmentName());
                putNullable(value, "selectedEquipmentNumber", detail.getSelectedEquipmentNumber());
                putNullable(value, "standardText", detail.getStandardText());
                putNullable(value, "standardLowerLimit", detail.getStandardLowerLimit());
                putNullable(value, "standardUpperLimit", detail.getStandardUpperLimit());
                putNullable(value, "standardUnit", detail.getStandardUnit());
                putNullable(value, "standardPrecision", detail.getStandardPrecision());
                putNullable(value, "inspectionMethod", detail.getInspectionMethod());
                putNullable(value, "resultType", detail.getResultType());
                value.set("sampleValues", JsonUtils.getObjectMapper().createArrayNode());
                putNullable(value, "judgement", detail.getJudgement());
                return value;
            });
            ((ArrayNode) item.get("sampleValues")).add(detail.getMeasuredValue());
        }
        ArrayNode array = JsonUtils.getObjectMapper().createArrayNode();
        snapshotByItem.values().forEach(array::add);
        return array;
    }

    private List<MesProcessPoolEventRevisionFieldChangeBO> buildChanges(
            MesProProcessPoolEventDO event,
            MesPqcInspectionTaskDO task,
            MesProcessPoolPqcInspectionCorrectionCommand command,
            List<MesPqcInspectionPieceDetailDO> beforeDetails,
            List<MesPqcInspectionPieceDetailDO> afterDetails,
            String afterInspectionResult) {
        List<MesProcessPoolEventRevisionFieldChangeBO> changes = new ArrayList<>();
        ObjectNode beforePayload = requireObject(event.getRawPayload(), "rawPayload");
        addChange(changes, "PQC.ACTUAL_INSPECTION_QUANTITY", "PQC检验数量",
                String.valueOf(task.getActualInspectionQuantity()),
                String.valueOf(command.getActualInspectionQuantity()));
        addChange(changes, "PQC.SCRAP_QUANTITY", "PQC损耗数量",
                text(beforePayload.get("scrapQuantity")), String.valueOf(command.getScrapQuantity()));
        addChange(changes, "PQC.INSPECTION_RESULT", "PQC判定结果",
                text(beforePayload.get("inspectionResult")), afterInspectionResult);

        Map<String, String> beforeSamples = sampleTextByItem(beforeDetails);
        Map<String, String> afterSamples = sampleTextByItem(afterDetails);
        Set<String> itemCodes = new LinkedHashSet<>(beforeSamples.keySet());
        itemCodes.addAll(afterSamples.keySet());
        for (String itemCode : itemCodes) {
            addChange(changes, "PQC.ITEM." + itemCode, "PQC项目：" + itemCode,
                    beforeSamples.get(itemCode), afterSamples.get(itemCode));
        }
        return changes;
    }

    private Map<String, String> sampleTextByItem(List<MesPqcInspectionPieceDetailDO> details) {
        return details.stream().collect(Collectors.groupingBy(MesPqcInspectionPieceDetailDO::getItemCode,
                LinkedHashMap::new,
                Collectors.mapping(MesPqcInspectionPieceDetailDO::getMeasuredValue, Collectors.joining(","))));
    }

    private void addChange(List<MesProcessPoolEventRevisionFieldChangeBO> changes, String code, String name,
                           String before, String after) {
        String beforeValue = StrUtil.blankToDefault(before, "");
        String afterValue = StrUtil.blankToDefault(after, "");
        if (Objects.equals(beforeValue, afterValue)) {
            return;
        }
        changes.add(MesProcessPoolEventRevisionFieldChangeBO.builder()
                .fieldCode(code)
                .fieldName(name)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .affectsQuantityFragment(false)
                .originalField(MesProcessPoolFragmentOriginalField.QUALITY_STATUS)
                .build());
    }

    private MesProBatchRecordExecutionFieldAuditSignatureResult recordCorrectionSignature(
            MesProcessPoolPqcInspectionCorrectionCommand command, Long eventId, String afterPayloadJson) {
        String challengeHash = MesProBatchRecordExecutionFieldAuditHasher.sha256(
                eventId + "|" + afterPayloadJson + "|" + command.getChangeReason().trim());
        MesProBatchRecordExecutionFieldAuditSignatureResult signature =
                signatureService.recordFieldChangeSignature(
                        new MesProBatchRecordExecutionFieldAuditSignatureCommand()
                                .setExecutionId(0L)
                                .setPassword(command.getSignaturePassword())
                                .setReasonCategory("PQC_INSPECTION_CORRECTION")
                                .setReasonText(command.getChangeReason().trim())
                                .setSignatureChallengeHash(challengeHash));
        if (!Objects.equals(signature.getActorId(), command.getActorUserId())) {
            throw exception(PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH);
        }
        return signature;
    }

    private void updateFormalPqcTables(MesProProcessPoolPqcRecordDO record,
                                       MesPqcInspectionTaskDO task,
                                       MesProcessPoolPqcInspectionCorrectionCommand command,
                                       List<MesPqcInspectionPieceDetailDO> updatedDetails,
                                       String afterPayloadJson,
                                       String inspectionResult) {
        int taskUpdated = pqcTaskMapper.updateById(new MesPqcInspectionTaskDO()
                .setId(task.getId())
                .setActualInspectionQuantity(command.getActualInspectionQuantity()));
        if (taskUpdated <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcInspectionTask.update");
        }
        pieceDetailMapper.deleteByTaskId(task.getId());
        if (!Boolean.TRUE.equals(pieceDetailMapper.insertBatch(updatedDetails))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcInspectionPieceDetails.update");
        }
        int recordUpdated = pqcRecordMapper.updateById(new MesProProcessPoolPqcRecordDO()
                .setId(record.getId())
                .setInspectionResult(inspectionResult)
                .setRawPayload(afterPayloadJson));
        if (recordUpdated <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcRecord.update");
        }
    }

    private String resolveInspectionResult(Integer scrapQuantity, List<MesPqcInspectionPieceDetailDO> details) {
        if (scrapQuantity != null && scrapQuantity > 0) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;
        }
        return details.stream().anyMatch(detail ->
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(detail.getJudgement()))
                ? MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE
                : MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS;
    }

    private String resolvePieceJudgement(MesPqcInspectionPieceDetailDO detail, String value) {
        try {
            return PqcResultValueValidator.validate(detail.getResultType(), value,
                    detail.getStandardLowerLimit(), detail.getStandardUpperLimit(),
                    detail.getStandardPrecision()).judgement();
        } catch (IllegalArgumentException ex) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                    "itemResults.sampleValues: " + ex.getMessage());
        }
    }

    private ObjectNode requireObject(String json, String fieldName) {
        if (StrUtil.isBlank(json)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        try {
            return requireObject(JsonUtils.parseTree(json), fieldName);
        } catch (RuntimeException ex) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private ObjectNode requireObject(JsonNode node, String fieldName) {
        if (!(node instanceof ObjectNode object)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        return object;
    }

    private String normalizeItemCode(String itemCode) {
        if (StrUtil.isBlank(itemCode)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "itemResults.itemCode");
        }
        return itemCode.trim();
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText();
    }

    private void putNullable(ObjectNode node, String fieldName, String value) {
        if (value == null) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value);
        }
    }

    private void putNullable(ObjectNode node, String fieldName, Long value) {
        if (value == null) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value);
        }
    }

    private void putNullable(ObjectNode node, String fieldName, Integer value) {
        if (value == null) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value);
        }
    }

    private void putNullable(ObjectNode node, String fieldName, BigDecimal value) {
        if (value == null) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value);
        }
    }
}
