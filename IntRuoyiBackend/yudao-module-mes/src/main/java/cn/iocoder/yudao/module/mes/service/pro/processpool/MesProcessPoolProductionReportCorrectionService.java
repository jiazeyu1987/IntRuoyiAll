package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineLossReasonSnapshot;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineLossReasonValidator;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProductionReportManagementSummaryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_CHANGE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH;

@Service
public class MesProcessPoolProductionReportCorrectionService {

    private static final String FIELD_OUTPUT_QUANTITY = "OUTPUT_QUANTITY";
    private static final String FIELD_SCRAP_QUANTITY = "SCRAP_QUANTITY";

    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProProcessPoolQuantityFragmentMapper fragmentMapper;
    private final MesProcessPoolEventRevisionService revisionService;
    private final MesProBatchRecordExecutionSignatureService signatureService;
    private final MesFrontlineLossReasonValidator lossReasonValidator;
    private final MesTeamLeaderScopeService scopeService;
    private final MesProductionReportManagementSummaryService reportManagementSummaryService;

    public MesProcessPoolProductionReportCorrectionService(
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolQuantityFragmentMapper fragmentMapper,
            MesProcessPoolEventRevisionService revisionService,
            MesProBatchRecordExecutionSignatureService signatureService,
            MesFrontlineLossReasonValidator lossReasonValidator,
            MesTeamLeaderScopeService scopeService,
            MesProductionReportManagementSummaryService reportManagementSummaryService) {
        this.eventMapper = eventMapper;
        this.fragmentMapper = fragmentMapper;
        this.revisionService = revisionService;
        this.signatureService = signatureService;
        this.lossReasonValidator = lossReasonValidator;
        this.scopeService = scopeService;
        this.reportManagementSummaryService = reportManagementSummaryService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long correct(MesProcessPoolProductionReportCorrectionCommand command) {
        validateCommand(command);
        MesProProcessPoolEventDO event = eventMapper.selectByIdForUpdate(command.getEventId());
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, command.getEventId());
        }
        if (!MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionSubmitEvent");
        }
        scopeService.assertCanAccessEmployee(command.getActorUserId(), "PRODUCTION", event.getActualEmployeeId());

        ObjectNode afterPayload = requireObject(event.getRawPayload(), "rawPayload").deepCopy();
        ObjectNode fieldValues = requireObject(afterPayload.get("fieldValues"), "rawPayload.fieldValues");
        BigDecimal beforeOutput = requireDecimal(afterPayload.get("outputQuantity"), "rawPayload.outputQuantity");
        BigDecimal beforeLoss = requireDecimal(afterPayload.get("lossQuantity"), "rawPayload.lossQuantity");
        MesProProcessPoolQuantityFragmentDO outputFragment = requireOutputFragment(event.getId());
        List<MesProcessPoolEventRevisionFieldChangeBO> changes = new ArrayList<>();

        applyOutputQuantity(command.getOutputQuantity(), beforeOutput, afterPayload, fieldValues,
                outputFragment, changes);
        applyLossDetails(command.getLossDetails(), beforeLoss, event.getRouteProcessId(),
                afterPayload, fieldValues, changes);
        applyDeviceParameterReadings(command.getDeviceParameterReadings(), afterPayload, fieldValues, changes);

        if (changes.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED);
        }

        String afterPayloadJson = JsonUtils.toJsonString(afterPayload);
        String challengeHash = MesProBatchRecordExecutionFieldAuditHasher.sha256(
                event.getId() + "|" + afterPayloadJson + "|" + command.getChangeReason().trim());
        MesProBatchRecordExecutionFieldAuditSignatureResult signature =
                signatureService.recordFieldChangeSignature(
                        new MesProBatchRecordExecutionFieldAuditSignatureCommand()
                                .setExecutionId(0L)
                                .setPassword(command.getSignaturePassword())
                                .setReasonCategory("PRODUCTION_REPORT_CORRECTION")
                                .setReasonText(command.getChangeReason().trim())
                                .setSignatureChallengeHash(challengeHash));
        if (!Objects.equals(signature.getActorId(), command.getActorUserId())) {
            throw exception(PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH);
        }

        Long revisionId = revisionService.updateProductionReportRecord(MesProcessPoolEventRevisionUpdateReqBO.builder()
                .eventId(event.getId())
                .afterPayload(afterPayloadJson)
                .changeReason(command.getChangeReason().trim())
                .revisionSignatureId(signature.getSignatureId())
                .revisionSignatureUserId(signature.getActorId())
                .revisionSignatureSnapshot(JsonUtils.toJsonString(signature))
                .modifiedByUserId(signature.getActorId())
                .changedFields(changes)
                .build());

        event.setRawPayload(afterPayloadJson)
                .setReportOutputQuantity(command.getOutputQuantity());
        reportManagementSummaryService.refreshProductionEvent(event);

        if (beforeOutput.compareTo(command.getOutputQuantity()) != 0) {
            updateOutputFragment(outputFragment, command.getOutputQuantity());
        }
        return revisionId;
    }

    private void validateCommand(MesProcessPoolProductionReportCorrectionCommand command) {
        if (command == null || command.getEventId() == null || command.getEventId() <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "eventId");
        }
        if (command.getActorUserId() == null || command.getActorUserId() <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "actorUserId");
        }
        if (command.getOutputQuantity() == null
                || command.getOutputQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "outputQuantity");
        }
        if (command.getLossDetails() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails");
        }
        if (command.getLossDetails().stream().anyMatch(item -> item == null
                || item.getReasonId() == null || item.getReasonId() <= 0
                || item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails");
        }
        if (StrUtil.isBlank(command.getChangeReason())) {
            throw exception(PRO_PROCESS_POOL_REVISION_CHANGE_REASON_REQUIRED);
        }
        if (StrUtil.isBlank(command.getSignaturePassword())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "signaturePassword");
        }
    }

    private MesProProcessPoolQuantityFragmentDO requireOutputFragment(Long eventId) {
        List<MesProProcessPoolQuantityFragmentDO> fragments = fragmentMapper.selectListByEventIdForUpdate(eventId)
                .stream()
                .filter(item -> MesProProcessPoolQuantityFragmentDO.SOURCE_QUANTITY_TYPE_OUTPUT
                        .equals(item.getSourceQuantityType()))
                .toList();
        if (fragments.size() != 1) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "outputQuantityFragment");
        }
        return fragments.get(0);
    }

    private void applyOutputQuantity(BigDecimal after, BigDecimal before, ObjectNode payload,
                                     ObjectNode fieldValues, MesProProcessPoolQuantityFragmentDO fragment,
                                     List<MesProcessPoolEventRevisionFieldChangeBO> changes) {
        requireDecimal(fieldValues.get(FIELD_OUTPUT_QUANTITY), "rawPayload.fieldValues.OUTPUT_QUANTITY");
        payload.put("outputQuantity", after);
        fieldValues.put(FIELD_OUTPUT_QUANTITY, after);
        if (before.compareTo(after) == 0) {
            return;
        }
        changes.add(fieldChange(FIELD_OUTPUT_QUANTITY, "完成数量", before, after,
                true, fragment.getId(), MesProcessPoolFragmentOriginalField.OUTPUT_QUANTITY));
    }

    private void applyLossDetails(
            List<MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand> requested,
            BigDecimal beforeLoss,
            Long routeProcessId,
            ObjectNode payload,
            ObjectNode fieldValues,
            List<MesProcessPoolEventRevisionFieldChangeBO> changes) {
        requireDecimal(fieldValues.get(FIELD_SCRAP_QUANTITY), "rawPayload.fieldValues.SCRAP_QUANTITY");
        requireArray(payload.get("lossDetails"), "rawPayload.lossDetails");
        Map<Long, JsonNode> originalReasons = originalLossReasons(payload);
        Set<Long> duplicateCheck = requested.stream()
                .map(MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand::getReasonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (duplicateCheck.size() != requested.size()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails.reasonId");
        }

        ArrayNode canonicalDetails = JsonUtils.getObjectMapper().createArrayNode();
        Map<Long, JsonNode> canonicalReasons = new LinkedHashMap<>();
        BigDecimal afterLoss = BigDecimal.ZERO;
        for (MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand detail : requested) {
            if (detail == null || detail.getReasonId() == null || detail.getQuantity() == null
                    || detail.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails");
            }
            JsonNode original = originalReasons.get(detail.getReasonId());
            MesFrontlineLossReasonSnapshot snapshot = original == null
                    ? lossReasonValidator.requireEnabledLossReason(
                            routeProcessId, detail.getReasonId(), detail.getQuantity())
                    : new MesFrontlineLossReasonSnapshot(
                            detail.getReasonId(), text(original, "reasonCode"), text(original, "reasonName"));
            ObjectNode canonical = JsonUtils.getObjectMapper().createObjectNode();
            canonical.put("reasonId", snapshot.reasonId());
            putNullable(canonical, "reasonCode", snapshot.reasonCode());
            putNullable(canonical, "reasonName", snapshot.reasonName());
            canonical.put("quantity", detail.getQuantity());
            canonicalDetails.add(canonical);
            canonicalReasons.put(detail.getReasonId(), canonical);
            afterLoss = afterLoss.add(detail.getQuantity());
        }

        payload.set("lossDetails", canonicalDetails);
        payload.set("lossReasonDetails", canonicalDetails.deepCopy());
        payload.put("lossQuantity", afterLoss);
        fieldValues.put(FIELD_SCRAP_QUANTITY, afterLoss);
        if (beforeLoss.compareTo(afterLoss) != 0) {
            changes.add(fieldChange(FIELD_SCRAP_QUANTITY, "损耗数量", beforeLoss, afterLoss,
                    false, null, MesProcessPoolFragmentOriginalField.LOSS_QUANTITY));
        }
        addLossReasonChanges(originalReasons, canonicalReasons, changes);
    }

    private void applyDeviceParameterReadings(
            List<MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand> requested,
            ObjectNode payload,
            ObjectNode fieldValues,
            List<MesProcessPoolEventRevisionFieldChangeBO> changes) {
        ArrayNode original = optionalArray(payload.get("deviceParameterReadings"));
        Map<String, MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand> byKey =
                (requested == null ? List.<MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand>of()
                        : requested).stream()
                        .filter(item -> item != null
                                && item.getDeviceId() != null && item.getDeviceId() > 0
                                && StrUtil.isNotBlank(item.getParameterCode())
                                && item.getValue() != null)
                        .collect(Collectors.toMap(
                        item -> parameterKey(item.getDeviceId(), item.getParameterCode()),
                        item -> item,
                        (left, right) -> right,
                        LinkedHashMap::new));
        if (byKey.isEmpty() || original == null) {
            return;
        }

        ArrayNode updated = original.deepCopy();
        for (JsonNode node : updated) {
            if (!(node instanceof ObjectNode reading)) {
                continue;
            }
            Long deviceId = longOrNull(reading.get("deviceId"));
            String parameterCode = text(reading, "parameterCode");
            if (deviceId == null || deviceId <= 0 || StrUtil.isBlank(parameterCode)) {
                continue;
            }
            MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand change =
                    byKey.remove(parameterKey(deviceId, parameterCode));
            if (change == null) {
                continue;
            }
            BigDecimal before = decimalOrNull(reading.get("value"));
            if (before != null && before.compareTo(change.getValue()) == 0) {
                continue;
            }
            reading.put("value", change.getValue());
            reading.put("parameterStatus", resolveParameterStatus(
                    change.getValue(), decimalOrNull(reading.get("lowerLimit")),
                    decimalOrNull(reading.get("upperLimit"))));
            updateParameterCopies(payload, fieldValues, reading, parameterCode, change.getValue());
            String parameterName = StrUtil.blankToDefault(text(reading, "parameterName"), parameterCode);
            String unit = text(reading, "unit");
            String displayName = StrUtil.isBlank(unit) ? parameterName : parameterName + "（" + unit + "）";
            changes.add(fieldChange("DEVICE_PARAMETERS." + parameterCode, displayName,
                    before, change.getValue(), false, null,
                    MesProcessPoolFragmentOriginalField.DEVICE_PARAMETERS));
        }
        payload.set("deviceParameterReadings", updated);
    }

    private void updateParameterCopies(ObjectNode payload, ObjectNode fieldValues, ObjectNode reading,
                                       String parameterCode, BigDecimal value) {
        String deviceName = text(reading, "deviceName");
        if (StrUtil.isBlank(deviceName)) {
            return;
        }
        ObjectNode equipmentParameters = objectChildWhenMissing(payload, "equipmentParameters");
        if (equipmentParameters != null) {
            ObjectNode deviceParameters = objectChildWhenMissing(equipmentParameters, deviceName);
            if (deviceParameters != null) {
                deviceParameters.put(parameterCode, value);
            }
        }
        ObjectNode fieldDeviceParameters = objectChildWhenMissing(fieldValues, "DEVICE_PARAMETERS");
        if (fieldDeviceParameters != null) {
            ObjectNode fieldDevice = objectChildWhenMissing(fieldDeviceParameters, deviceName);
            if (fieldDevice != null) {
                fieldDevice.put(parameterCode, value);
            }
        }
    }

    private ObjectNode objectChildWhenMissing(ObjectNode parent, String fieldName) {
        JsonNode existing = parent.get(fieldName);
        if (existing instanceof ObjectNode object) {
            return object;
        }
        if (existing != null && !existing.isNull()) {
            return null;
        }
        ObjectNode created = JsonUtils.getObjectMapper().createObjectNode();
        parent.set(fieldName, created);
        return created;
    }

    private void updateOutputFragment(MesProProcessPoolQuantityFragmentDO fragment, BigDecimal outputQuantity) {
        BigDecimal allocated = fragment.getAllocatedQuantity() == null
                ? BigDecimal.ZERO : fragment.getAllocatedQuantity();
        BigDecimal available = outputQuantity.subtract(allocated);
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "outputQuantityFragment.availableQuantity");
        }
        int updated = fragmentMapper.updateById(new MesProProcessPoolQuantityFragmentDO()
                .setId(fragment.getId())
                .setTotalQuantity(outputQuantity)
                .setAvailableQuantity(available));
        if (updated <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "outputQuantityFragment.update");
        }
    }

    private Map<Long, JsonNode> originalLossReasons(ObjectNode payload) {
        ArrayNode details = requireArray(payload.get("lossDetails"), "rawPayload.lossDetails");
        Map<Long, JsonNode> result = new LinkedHashMap<>();
        for (JsonNode detail : details) {
            Long reasonId = requireLong(detail.get("reasonId"), "lossDetails.reasonId");
            if (result.put(reasonId, detail) != null) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails.reasonId");
            }
        }
        return result;
    }

    private void addLossReasonChanges(
            Map<Long, JsonNode> originalReasons,
            Map<Long, JsonNode> canonicalReasons,
            List<MesProcessPoolEventRevisionFieldChangeBO> changes) {
        Set<Long> reasonIds = new LinkedHashSet<>(originalReasons.keySet());
        reasonIds.addAll(canonicalReasons.keySet());
        for (Long reasonId : reasonIds) {
            JsonNode beforeReason = originalReasons.get(reasonId);
            JsonNode afterReason = canonicalReasons.get(reasonId);
            BigDecimal before = beforeReason == null
                    ? BigDecimal.ZERO : requireDecimal(beforeReason.get("quantity"), "lossDetails.quantity");
            BigDecimal after = afterReason == null
                    ? BigDecimal.ZERO : requireDecimal(afterReason.get("quantity"), "lossDetails.quantity");
            if (before.compareTo(after) == 0) {
                continue;
            }
            JsonNode reason = afterReason == null ? beforeReason : afterReason;
            String reasonName = StrUtil.blankToDefault(text(reason, "reasonName"), text(reason, "reasonCode"));
            if (StrUtil.isBlank(reasonName)) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails.reasonName");
            }
            changes.add(fieldChange("LOSS_REASON." + reasonId, "损耗原因：" + reasonName,
                    before, after, false, null, MesProcessPoolFragmentOriginalField.LOSS_QUANTITY));
        }
    }

    private MesProcessPoolEventRevisionFieldChangeBO fieldChange(
            String code, String name, Object before, Object after, boolean affectsFragment,
            Long fragmentId, MesProcessPoolFragmentOriginalField originalField) {
        return MesProcessPoolEventRevisionFieldChangeBO.builder()
                .fieldCode(code)
                .fieldName(name)
                .beforeValue(formatValue(before))
                .afterValue(formatValue(after))
                .affectsQuantityFragment(affectsFragment)
                .sourceQuantityFragmentId(fragmentId)
                .originalField(originalField)
                .build();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "--";
        }
        if (value instanceof BigDecimal number) {
            BigDecimal normalized = number.stripTrailingZeros();
            return normalized.compareTo(BigDecimal.ZERO) == 0 ? "0" : normalized.toPlainString();
        }
        return String.valueOf(value);
    }

    private String resolveParameterStatus(BigDecimal value, BigDecimal lower, BigDecimal upper) {
        if (lower != null && value.compareTo(lower) < 0) {
            return "BELOW_LOWER";
        }
        if (upper != null && value.compareTo(upper) > 0) {
            return "ABOVE_UPPER";
        }
        return "NORMAL";
    }

    private String parameterKey(Long deviceId, String parameterCode) {
        if (deviceId == null || deviceId <= 0 || StrUtil.isBlank(parameterCode)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "deviceParameterReadings");
        }
        return deviceId + ":" + parameterCode.trim();
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

    private ArrayNode requireArray(JsonNode node, String fieldName) {
        if (!(node instanceof ArrayNode array)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        return array;
    }

    private ArrayNode optionalArray(JsonNode node) {
        return node instanceof ArrayNode array ? array : null;
    }

    private BigDecimal requireDecimal(JsonNode node, String fieldName) {
        BigDecimal value = decimalOrNull(node);
        if (value == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        return value;
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        return node != null && node.isNumber() ? node.decimalValue() : null;
    }

    private Long requireLong(JsonNode node, String fieldName) {
        if (node == null || !node.canConvertToLong()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        return node.longValue();
    }

    private Long longOrNull(JsonNode node) {
        return node != null && node.canConvertToLong() ? node.longValue() : null;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText();
    }

    private void putNullable(ObjectNode node, String fieldName, String value) {
        if (value == null) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value);
        }
    }

}
