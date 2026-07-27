package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillItemVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditChange;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditValueType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
public class MesProBatchRecordCellLinkAutoPersistServiceImpl implements MesProBatchRecordCellLinkAutoPersistService {

    private static final int EXECUTION_STATUS_DRAFT = 0;
    private static final String DEFAULT_TRIGGER = "EXECUTION_OPEN_OR_CREATE";
    private static final String DEFAULT_IDEMPOTENCY_NAMESPACE = "CELL_LINK_AUTO_PREFILL";
    private static final String SOURCE_TYPE_PRODUCTION_WORK_ORDER = "PRODUCTION_WORK_ORDER";
    private static final String STATUS_APPLICABLE = "APPLICABLE";
    private static final String STATUS_APPLIED = "APPLIED";
    private static final String STATUS_SOURCE_VALUE_MISSING = "SOURCE_VALUE_MISSING";
    private static final String STATUS_TARGET_ALREADY_MANUAL = "TARGET_ALREADY_MANUAL";
    private static final String STATUS_NO_CHANGE_ALREADY_APPLIED = "NO_CHANGE_ALREADY_APPLIED";

    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditBatchMapper auditBatchMapper;
    @Resource
    private MesProBatchRecordCellLinkService cellLinkService;
    @Resource
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchRecordCellLinkAutoPersistResult autoPersist(BatchRecordCellLinkAutoPersistCommand command) {
        if (command == null || command.getExecutionId() == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants
                    .PRO_BATCH_RECORD_CELL_LINK_EXECUTION_NOT_EXISTS);
        }
        String trigger = StrUtil.blankToDefault(StrUtil.trim(command.getTrigger()), DEFAULT_TRIGGER);
        String namespace = StrUtil.blankToDefault(StrUtil.trim(command.getIdempotencyNamespace()),
                DEFAULT_IDEMPOTENCY_NAMESPACE);
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(command.getExecutionId());
        if (execution == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants
                    .PRO_BATCH_RECORD_CELL_LINK_EXECUTION_NOT_EXISTS);
        }
        BatchRecordCellLinkAutoPersistResult result = new BatchRecordCellLinkAutoPersistResult()
                .setExecutionId(execution.getId())
                .setTrigger(trigger)
                .setAppliedCount(0)
                .setConflictCount(0)
                .setFieldAuditRevisionAfter(execution.getFieldAuditRevision())
                .setFieldAuditHeadHashAfter(execution.getFieldAuditHeadHash())
                .setCellValuesHashAfter(execution.getCellValuesHash());
        if (!Objects.equals(execution.getStatus(), EXECUTION_STATUS_DRAFT)) {
            return result;
        }

        BatchRecordCellLinkPrefillRespVO prefill =
                cellLinkService.getPrefill(execution.getId(), command.getWorkTaskId());
        Map<String, SnapshotField> fields = snapshotFields(execution.getExecutionSnapshotJson());
        Map<String, JsonNode> currentValues = currentValues(execution.getCellValuesJson());
        List<MesProBatchRecordExecutionFieldAuditChange> changes = new ArrayList<>();
        for (BatchRecordCellLinkPrefillItemVO conflict : nullToEmpty(prefill.getConflicts())) {
            handleConflict(result, namespace, execution, conflict);
        }
        for (BatchRecordCellLinkPrefillItemVO item : nullToEmpty(prefill.getPrefills())) {
            if (!STATUS_APPLICABLE.equals(item.getStatus())) {
                result.getItems().add(toResultItem(item, item.getStatus()));
                result.setConflictCount(result.getConflictCount() + 1);
                continue;
            }
            SnapshotField field = requireSnapshotField(fields, item);
            MesProBatchRecordExecutionFieldAuditValueType valueType =
                    resolveValueType(field.valueType(), item.getValue());
            Object newValue = normalizeValue(valueType, item.getValue());
            String display = displayValue(newValue);
            JsonNode oldValue = currentValues.getOrDefault(cellKey(item.getTargetRowIndex(),
                    item.getTargetColumnIndex()), field.defaultValue());
            changes.add(new MesProBatchRecordExecutionFieldAuditChange()
                    .setFieldPath(field.fieldPath())
                    .setFieldKey(field.fieldKey())
                    .setRowIndex(item.getTargetRowIndex())
                    .setColumnIndex(item.getTargetColumnIndex())
                    .setValueType(valueType)
                    .setNewValueJson(newValue)
                    .setNewValueDisplay(display)
                    .setExpectedOldValueHash(oldValueHash(valueType, oldValue)));
            result.getItems().add(toResultItem(item, STATUS_APPLIED));
        }
        if (changes.isEmpty()) {
            return result;
        }

        MesProBatchRecordExecutionFieldAuditSaveChangesCommand saveCommand =
                new MesProBatchRecordExecutionFieldAuditSaveChangesCommand()
                        .setExecutionId(execution.getId())
                        .setWorkTaskId(command.getWorkTaskId())
                        .setIdempotencyKey(idempotencyKey(namespace, execution.getId(), prefill.getPrefills()))
                        .setBaseCellValuesHash(execution.getCellValuesHash())
                        .setBaseFieldAuditRevision(execution.getFieldAuditRevision())
                        .setBaseFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                        .setReasonCategory("OTHER")
                        .setReasonText(reasonText(prefill.getPrefills()))
                        .setChanges(changes);
        MesProBatchRecordExecutionFieldAuditSaveResult saveResult =
                fieldAuditService.saveSystemCellLinkChanges(saveCommand);
        result.setAppliedCount(changes.size())
                .setFieldAuditRevisionAfter(saveResult.getFieldAuditRevision())
                .setFieldAuditHeadHashAfter(saveResult.getFieldAuditHeadHash())
                .setCellValuesHashAfter(saveResult.getCellValuesHash());
        return result;
    }

    private void handleConflict(BatchRecordCellLinkAutoPersistResult result, String namespace,
                                MesProBatchRecordExecutionDO execution,
                                BatchRecordCellLinkPrefillItemVO conflict) {
        if (SOURCE_TYPE_PRODUCTION_WORK_ORDER.equals(conflict.getSourceType())
                && STATUS_SOURCE_VALUE_MISSING.equals(conflict.getStatus())) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants
                            .PRO_BATCH_RECORD_CELL_LINK_AUTO_PERSIST_SOURCE_VALUE_MISSING,
                    execution.getId(), conflict.getRuleId(), conflict.getSourceFieldCode(),
                    conflict.getTargetCellKey());
        }
        String status = conflict.getStatus();
        if (STATUS_TARGET_ALREADY_MANUAL.equals(status) && hasExistingAutoPersistAudit(namespace, execution, conflict)) {
            status = STATUS_NO_CHANGE_ALREADY_APPLIED;
        } else {
            result.setConflictCount(result.getConflictCount() + 1);
        }
        result.getItems().add(toResultItem(conflict, status));
    }

    private boolean hasExistingAutoPersistAudit(String namespace, MesProBatchRecordExecutionDO execution,
                                                BatchRecordCellLinkPrefillItemVO conflict) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || conflict.getValue() == null) {
            return false;
        }
        String key = idempotencyKey(namespace, execution.getId(), List.of(conflict));
        MesProBatchRecordExecutionFieldAuditBatchDO batch =
                auditBatchMapper.selectByIdempotencyKey(tenantId, execution.getId(), key);
        return batch != null;
    }

    private BatchRecordCellLinkAutoPersistResult.Item toResultItem(BatchRecordCellLinkPrefillItemVO item,
                                                                    String status) {
        return new BatchRecordCellLinkAutoPersistResult.Item()
                .setRuleId(item.getRuleId())
                .setRuleVersion(item.getRuleVersion())
                .setTargetCellKey(item.getTargetCellKey())
                .setSourceType(item.getSourceType())
                .setSourceFieldCode(item.getSourceFieldCode())
                .setValue(item.getValue())
                .setStatus(status);
    }

    private SnapshotField requireSnapshotField(Map<String, SnapshotField> fields, BatchRecordCellLinkPrefillItemVO item) {
        SnapshotField field = fields.get(cellKey(item.getTargetRowIndex(), item.getTargetColumnIndex()));
        if (field == null) {
            throw exception(MesProBatchRecordExecutionErrorCodeConstants
                    .PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED);
        }
        return field;
    }

    private String idempotencyKey(String namespace, Long executionId, List<BatchRecordCellLinkPrefillItemVO> items) {
        List<String> parts = nullToEmpty(items).stream()
                .map(item -> item.getRuleId() + ":" + item.getRuleVersion() + ":" + item.getTargetCellKey()
                        + ":" + DigestUtil.sha256Hex(Objects.toString(item.getValue(), "")))
                .sorted()
                .toList();
        return namespace + ":" + executionId + ":" + String.join("|", parts);
    }

    private String reasonText(List<BatchRecordCellLinkPrefillItemVO> items) {
        boolean hasBatchCode = nullToEmpty(items).stream()
                .anyMatch(item -> SOURCE_TYPE_PRODUCTION_WORK_ORDER.equals(item.getSourceType())
                        && "batchCode".equals(item.getSourceFieldCode()));
        return hasBatchCode ? "系统根据单元格链接自动预填生产批号" : "系统根据单元格链接自动预填";
    }

    private Map<String, SnapshotField> snapshotFields(String executionSnapshotJson) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(executionSnapshotJson);
            Map<String, SnapshotField> result = new LinkedHashMap<>();
            JsonNode fields = root.path("fields");
            if (!fields.isArray()) {
                return result;
            }
            for (JsonNode field : fields) {
                Integer rowIndex = integer(field, "rowIndex");
                Integer columnIndex = integer(field, "columnIndex");
                String fieldPath = text(field, "fieldPath");
                String fieldKey = text(field, "fieldKey");
                if (rowIndex == null || columnIndex == null
                        || StrUtil.isBlank(fieldPath) || StrUtil.isBlank(fieldKey)) {
                    continue;
                }
                result.put(cellKey(rowIndex, columnIndex), new SnapshotField(
                        fieldPath,
                        fieldKey,
                        parseValueType(text(field, "valueType")),
                        snapshotDefaultValue(field)));
            }
            return result;
        } catch (JsonProcessingException e) {
            throw exception(MesProBatchRecordExecutionErrorCodeConstants
                    .PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED);
        }
    }

    private Map<String, JsonNode> currentValues(String cellValuesJson) {
        if (StrUtil.isBlank(cellValuesJson)) {
            return Map.of();
        }
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(cellValuesJson);
            Map<String, JsonNode> result = new LinkedHashMap<>();
            if (!root.isArray()) {
                return result;
            }
            for (JsonNode cell : root) {
                Integer rowIndex = integer(cell, "rowIndex");
                Integer columnIndex = integer(cell, "columnIndex");
                if (rowIndex != null && columnIndex != null) {
                    result.put(cellKey(rowIndex, columnIndex), cell.get("value"));
                }
            }
            return result;
        } catch (JsonProcessingException e) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants
                    .PRO_BATCH_RECORD_CELL_LINK_CELL_VALUES_INVALID, cellValuesJson);
        }
    }

    private JsonNode snapshotDefaultValue(JsonNode field) {
        JsonNode defaultValue = field.get("defaultValue");
        if (defaultValue != null && !defaultValue.isMissingNode()) {
            return defaultValue.deepCopy();
        }
        JsonNode value = field.get("value");
        return value == null || value.isMissingNode() ? NullNode.instance : value.deepCopy();
    }

    private MesProBatchRecordExecutionFieldAuditValueType resolveValueType(
            MesProBatchRecordExecutionFieldAuditValueType snapshotValueType, Object value) {
        if (snapshotValueType != null) {
            return snapshotValueType;
        }
        if (value instanceof Number) {
            return MesProBatchRecordExecutionFieldAuditValueType.NUMBER;
        }
        if (value instanceof Boolean) {
            return MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN;
        }
        return MesProBatchRecordExecutionFieldAuditValueType.STRING;
    }

    private MesProBatchRecordExecutionFieldAuditValueType parseValueType(String valueType) {
        if (StrUtil.isBlank(valueType)) {
            return null;
        }
        return MesProBatchRecordExecutionFieldAuditValueType.valueOf(valueType);
    }

    private Object normalizeValue(MesProBatchRecordExecutionFieldAuditValueType valueType, Object value) {
        return switch (valueType) {
            case STRING, DATE, DATETIME -> Objects.toString(value, "");
            case NUMBER -> {
                if (value instanceof BigDecimal decimal) {
                    yield MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(decimal);
                }
                if (value instanceof Number number) {
                    yield MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(new BigDecimal(number.toString()));
                }
                yield MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(new BigDecimal(String.valueOf(value)));
            }
            case BOOLEAN -> value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
            case NULL -> null;
            default -> value;
        };
    }

    private String oldValueHash(MesProBatchRecordExecutionFieldAuditValueType valueType, JsonNode oldValue) {
        JsonNode normalized = oldValueNode(oldValue, valueType);
        String canonical = normalized == null || normalized.isNull() || normalized.isMissingNode()
                ? "null"
                : MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(valueType, normalized);
        return MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue(canonical);
    }

    private JsonNode oldValueNode(JsonNode value, MesProBatchRecordExecutionFieldAuditValueType valueType) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return NullNode.instance;
        }
        if (!value.isTextual()) {
            return value;
        }
        String text = value.textValue();
        if (StrUtil.isBlank(text)
                && valueType != MesProBatchRecordExecutionFieldAuditValueType.STRING
                && valueType != MesProBatchRecordExecutionFieldAuditValueType.NULL) {
            return NullNode.instance;
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.NUMBER) {
            return MesProBatchRecordExecutionFieldAuditHasher.toJsonNode(new BigDecimal(text));
        }
        return TextNode.valueOf(text);
    }

    private String displayValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return String.valueOf(value);
    }

    private Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || !value.canConvertToInt() ? null : value.asInt();
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String cellKey(Integer rowIndex, Integer columnIndex) {
        return rowIndex + ":" + columnIndex;
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record SnapshotField(String fieldPath,
                                 String fieldKey,
                                 MesProBatchRecordExecutionFieldAuditValueType valueType,
                                 JsonNode defaultValue) {
    }
}
