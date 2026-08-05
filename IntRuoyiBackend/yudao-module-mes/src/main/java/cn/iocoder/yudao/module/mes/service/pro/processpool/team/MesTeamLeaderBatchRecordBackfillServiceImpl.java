package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_BINDING_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_EXECUTION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_SOURCE_VALUE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

@Service
public class MesTeamLeaderBatchRecordBackfillServiceImpl implements MesTeamLeaderBatchRecordBackfillService {

    static final String USE_TYPE_BATCH = "BATCH";
    static final String RECORD_CATEGORY_BATCH_RECORD = "BATCH_RECORD";
    static final String SCOPE_TYPE_ROUTE_VERSION = "ROUTE_VERSION";
    static final String SOURCE_TYPE_PROCESS_POOL_REPORT = "PROCESS_POOL_REPORT";

    private final MesProRouteFlowProcessBatchRecordMapper bindingMapper;
    private final MesProBatchRecordExecutionService executionService;
    private final MesProBatchRecordExecutionMapper executionMapper;
    private final MesProBatchRecordCellLinkRuleMapper ruleMapper;
    private final MesProBatchRecordExecutionFieldAuditService fieldAuditService;

    public MesTeamLeaderBatchRecordBackfillServiceImpl(MesProRouteFlowProcessBatchRecordMapper bindingMapper,
                                                       MesProBatchRecordExecutionService executionService,
                                                       MesProBatchRecordExecutionMapper executionMapper,
                                                       MesProBatchRecordCellLinkRuleMapper ruleMapper,
                                                       MesProBatchRecordExecutionFieldAuditService fieldAuditService) {
        this.bindingMapper = bindingMapper;
        this.executionService = executionService;
        this.executionMapper = executionMapper;
        this.ruleMapper = ruleMapper;
        this.fieldAuditService = fieldAuditService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderBatchRecordBackfillResult backfillCompletedProcess(
            MesTeamLeaderBatchRecordBackfillCommand command) {
        validate(command);
        MesProProcessPoolEventDO event = command.getEvent();
        MesProWorkOrderDO workOrder = command.getWorkOrder();
        MesProcessPoolReportAllocationDO allocation = command.getAllocation();
        List<MesProProcessPoolEventDO> sourceEvents = sourceEvents(command);
        List<MesProcessPoolReportAllocationDO> allocations = allocations(command);
        validateSources(event, allocation, sourceEvents, allocations);
        String aggregateHash = aggregateHash(command, sourceEvents, allocations);
        String idempotencyKey = idempotencyKey(command, event, allocation, aggregateHash);

        MesProRouteFlowProcessBatchRecordDO binding = requireFormalBinding(event.getRouteProcessId());
        MesProBatchRecordExecutionOpenOrCreateByContextRespVO opened =
                executionService.openOrCreateByContext(toOpenReq(event, workOrder, binding));
        if (opened == null || opened.getId() == null) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_EXECUTION_REQUIRED, binding.getBatchRecordReportId());
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(opened.getId());
        if (execution == null) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_EXECUTION_REQUIRED, binding.getBatchRecordReportId());
        }
        List<MesProBatchRecordCellLinkRuleDO> rules = ruleMapper.selectEnabledListByScopeAndTargetReport(
                SCOPE_TYPE_ROUTE_VERSION, binding.getBatchRecordVersionId(), binding.getBatchRecordReportId());
        if (rules.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED,
                    event.getRouteProcessId(), binding.getBatchRecordReportId(), "*");
        }
        Map<String, SnapshotField> fields = snapshotFields(execution.getExecutionSnapshotJson());
        Map<String, JsonNode> currentValues = currentValues(execution.getCellValuesJson());
        Map<Long, MesProProcessPoolEventDO> sourceEventMap = sourceEventMap(sourceEvents);
        Map<Long, JsonNode> payloadCache = new LinkedHashMap<>();
        List<MesProBatchRecordExecutionFieldAuditChange> changes = rules.stream()
                .map(rule -> toChange(event, allocations, binding, rule, fields, currentValues, sourceEventMap,
                        payloadCache))
                .toList();
        MesProBatchRecordExecutionFieldAuditSaveResult saveResult = fieldAuditService.saveSystemCellLinkChanges(
                new MesProBatchRecordExecutionFieldAuditSaveChangesCommand()
                        .setExecutionId(execution.getId())
                        .setIdempotencyKey(idempotencyKey)
                        .setBaseCellValuesHash(execution.getCellValuesHash())
                        .setBaseFieldAuditRevision(execution.getFieldAuditRevision())
                        .setBaseFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                        .setReasonCategory("OTHER")
                        .setReasonText("生产组长确认报工后自动回填正式批记录")
                        .setChanges(changes));
        return new MesTeamLeaderBatchRecordBackfillResult()
                .setExecutionId(execution.getId())
                .setAppliedFieldCount(saveResult == null || saveResult.getChangedFieldCount() == null
                        ? changes.size() : saveResult.getChangedFieldCount());
    }

    private void validate(MesTeamLeaderBatchRecordBackfillCommand command) {
        if (command == null || command.getEvent() == null || command.getAllocation() == null
                || command.getWorkOrder() == null || command.getEvent().getId() == null
                || command.getEvent().getRouteProcessId() == null || command.getEvent().getProcessId() == null
                || command.getAllocation().getId() == null || command.getAllocation().getWorkOrderId() == null
                || command.getWorkOrder().getId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "batchRecordBackfill");
        }
    }

    private List<MesProProcessPoolEventDO> sourceEvents(MesTeamLeaderBatchRecordBackfillCommand command) {
        if (command.getSourceEvents() == null || command.getSourceEvents().isEmpty()) {
            return List.of(command.getEvent());
        }
        return List.copyOf(command.getSourceEvents());
    }

    private List<MesProcessPoolReportAllocationDO> allocations(MesTeamLeaderBatchRecordBackfillCommand command) {
        if (command.getAllocations() == null || command.getAllocations().isEmpty()) {
            return List.of(command.getAllocation());
        }
        return List.copyOf(command.getAllocations());
    }

    private void validateSources(MesProProcessPoolEventDO event, MesProcessPoolReportAllocationDO allocation,
                                 List<MesProProcessPoolEventDO> sourceEvents,
                                 List<MesProcessPoolReportAllocationDO> allocations) {
        List<Long> sourceEventIds = sourceEvents.stream()
                .map(MesProProcessPoolEventDO::getId)
                .toList();
        if (sourceEvents.stream().anyMatch(source -> source == null || source.getId() == null)
                || allocations.stream().anyMatch(sourceAllocation -> sourceAllocation == null
                || sourceAllocation.getId() == null || sourceAllocation.getEventId() == null
                || !sourceEventIds.contains(sourceAllocation.getEventId())
                || !Objects.equals(allocation.getWorkOrderId(), sourceAllocation.getWorkOrderId())
                || !Objects.equals(event.getRouteProcessId(), sourceAllocation.getRouteProcessId())
                || !Objects.equals(event.getProcessId(), sourceAllocation.getProcessId()))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "batchRecordBackfillSources");
        }
    }

    private MesProRouteFlowProcessBatchRecordDO requireFormalBinding(Long routeProcessId) {
        return bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(routeProcessId), USE_TYPE_BATCH)
                .stream()
                .filter(binding -> StrUtil.isNotBlank(binding.getBatchRecordReportId()))
                .filter(binding -> Objects.equals(RECORD_CATEGORY_BATCH_RECORD, binding.getRecordCategory()))
                .filter(binding -> binding.getBatchRecordVersionId() != null)
                .findFirst()
                .orElseThrow(() -> exception(PRO_PROCESS_POOL_BATCH_RECORD_BINDING_REQUIRED, routeProcessId));
    }

    private MesProBatchRecordExecutionOpenOrCreateByContextReqVO toOpenReq(MesProProcessPoolEventDO event,
                                                                           MesProWorkOrderDO workOrder,
                                                                           MesProRouteFlowProcessBatchRecordDO binding) {
        return new MesProBatchRecordExecutionOpenOrCreateByContextReqVO()
                .setWorkOrderId(workOrder.getId())
                .setRouteId(binding.getRouteId())
                .setProcessId(event.getProcessId())
                .setRouteProcessId(event.getRouteProcessId())
                .setBatchRecordReportId(binding.getBatchRecordReportId())
                .setInstanceScope(binding.getInstanceScope())
                .setSharedFormKey(binding.getSharedFormKey())
                .setFormSlotType(binding.getFormSlotType())
                .setRecordCategory(binding.getRecordCategory())
                .setValidationProfile(binding.getValidationProfile())
                .setRecordbookEnabled(binding.getRecordbookEnabled())
                .setPermissionScopeId(binding.getPermissionScopeId())
                .setRouteBindingId(binding.getId())
                .setRouteBindingSnapshotHash(binding.getRecordCategorySnapshotHash())
                .setArchiveVisibility(binding.getArchiveVisibility())
                .setSlotConfigSnapshotHash(binding.getSlotConfigSnapshotHash())
                .setBatchCode(StrUtil.trim(workOrder.getBatchCode()));
    }

    private MesProBatchRecordExecutionFieldAuditChange toChange(
            MesProProcessPoolEventDO event,
            List<MesProcessPoolReportAllocationDO> allocations,
            MesProRouteFlowProcessBatchRecordDO binding,
            MesProBatchRecordCellLinkRuleDO rule,
            Map<String, SnapshotField> fields,
            Map<String, JsonNode> currentValues,
            Map<Long, MesProProcessPoolEventDO> sourceEventMap,
            Map<Long, JsonNode> payloadCache) {
        if (!SOURCE_TYPE_PROCESS_POOL_REPORT.equals(StrUtil.trim(rule.getSourceType()))
                || StrUtil.isBlank(rule.getSourceFieldCode())) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED,
                    event.getRouteProcessId(), binding.getBatchRecordReportId(), rule.getSourceFieldCode());
        }
        SnapshotField field = fields.get(cellKey(rule.getTargetRowIndex(), rule.getTargetColumnIndex()));
        if (field == null) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED,
                    event.getRouteProcessId(), binding.getBatchRecordReportId(), rule.getTargetCellKey());
        }
        List<Object> values = allocations.stream()
                .map(sourceAllocation -> sourceValue(sourceAllocation, rule.getSourceFieldCode(), sourceEventMap,
                        payloadCache))
                .toList();
        Object value = aggregateValue(event, binding, rule, values);
        MesProBatchRecordExecutionFieldAuditValueType valueType = valueType(rule, field);
        Object normalized = normalizeValue(valueType, value);
        String cellKey = cellKey(rule.getTargetRowIndex(), rule.getTargetColumnIndex());
        return new MesProBatchRecordExecutionFieldAuditChange()
                .setFieldPath(field.fieldPath())
                .setFieldKey(field.fieldKey())
                .setRowIndex(rule.getTargetRowIndex())
                .setColumnIndex(rule.getTargetColumnIndex())
                .setValueType(valueType)
                .setNewValueJson(normalized)
                .setNewValueDisplay(displayValue(normalized))
                .setExpectedOldValueHash(oldValueHash(valueType, currentValues.get(cellKey), field.defaultValue()));
    }

    private Object sourceValue(MesProcessPoolReportAllocationDO allocation,
                               String sourceFieldCode,
                               Map<Long, MesProProcessPoolEventDO> sourceEventMap,
                               Map<Long, JsonNode> payloadCache) {
        if ("allocatedQuantity".equals(sourceFieldCode)) {
            if (allocation.getAllocatedQuantity() == null) {
                throw exception(PRO_PROCESS_POOL_BATCH_RECORD_SOURCE_VALUE_REQUIRED,
                        allocation.getEventId(), sourceFieldCode);
            }
            return allocation.getAllocatedQuantity();
        }
        MesProProcessPoolEventDO sourceEvent = sourceEventMap.get(allocation.getEventId());
        if (sourceEvent == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "batchRecordBackfillSourceEvent");
        }
        JsonNode payload = payloadCache.computeIfAbsent(sourceEvent.getId(), ignored -> rawPayload(sourceEvent));
        JsonNode node = payload.get(sourceFieldCode);
        if (node == null || node.isNull() || (node.isTextual() && StrUtil.isBlank(node.asText()))) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_SOURCE_VALUE_REQUIRED, sourceEvent.getId(), sourceFieldCode);
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
    }

    private Object aggregateValue(MesProProcessPoolEventDO event,
                                  MesProRouteFlowProcessBatchRecordDO binding,
                                  MesProBatchRecordCellLinkRuleDO rule,
                                  List<Object> values) {
        if (values.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_SOURCE_VALUE_REQUIRED, event.getId(),
                    rule.getSourceFieldCode());
        }
        String strategy = StrUtil.trim(rule.getAggregationStrategy());
        if (StrUtil.isBlank(strategy)) {
            if (values.size() == 1) {
                return values.get(0);
            }
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED,
                    event.getRouteProcessId(), binding.getBatchRecordReportId(), rule.getSourceFieldCode());
        }
        return switch (strategy.toUpperCase()) {
            case "SUM" -> sumValues(event, binding, rule, values);
            case "LIST" -> joinValues(values);
            case "DISTINCT_LIST" -> joinDistinctValues(values);
            case "FIRST" -> values.get(0);
            case "LAST" -> values.get(values.size() - 1);
            case "MIN" -> minValue(event, binding, rule, values);
            case "MAX" -> maxValue(event, binding, rule, values);
            default -> throw exception(PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED,
                    event.getRouteProcessId(), binding.getBatchRecordReportId(), rule.getSourceFieldCode());
        };
    }

    private BigDecimal sumValues(MesProProcessPoolEventDO event,
                                 MesProRouteFlowProcessBatchRecordDO binding,
                                 MesProBatchRecordCellLinkRuleDO rule,
                                 List<Object> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object value : values) {
            total = total.add(toDecimal(event, binding, rule, value));
        }
        return total;
    }

    private BigDecimal minValue(MesProProcessPoolEventDO event,
                                MesProRouteFlowProcessBatchRecordDO binding,
                                MesProBatchRecordCellLinkRuleDO rule,
                                List<Object> values) {
        return values.stream()
                .map(value -> toDecimal(event, binding, rule, value))
                .min(BigDecimal::compareTo)
                .orElseThrow(() -> exception(PRO_PROCESS_POOL_BATCH_RECORD_SOURCE_VALUE_REQUIRED,
                        event.getId(), rule.getSourceFieldCode()));
    }

    private BigDecimal maxValue(MesProProcessPoolEventDO event,
                                MesProRouteFlowProcessBatchRecordDO binding,
                                MesProBatchRecordCellLinkRuleDO rule,
                                List<Object> values) {
        return values.stream()
                .map(value -> toDecimal(event, binding, rule, value))
                .max(BigDecimal::compareTo)
                .orElseThrow(() -> exception(PRO_PROCESS_POOL_BATCH_RECORD_SOURCE_VALUE_REQUIRED,
                        event.getId(), rule.getSourceFieldCode()));
    }

    private BigDecimal toDecimal(MesProProcessPoolEventDO event,
                                 MesProRouteFlowProcessBatchRecordDO binding,
                                 MesProBatchRecordCellLinkRuleDO rule,
                                 Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED,
                    event.getRouteProcessId(), binding.getBatchRecordReportId(), rule.getSourceFieldCode());
        }
    }

    private String joinValues(List<Object> values) {
        List<String> tokens = new ArrayList<>();
        for (Object value : values) {
            tokens.add(displayValue(value));
        }
        return String.join(",", tokens);
    }

    private String joinDistinctValues(List<Object> values) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (Object value : values) {
            tokens.add(displayValue(value));
        }
        return String.join(",", tokens);
    }

    private JsonNode rawPayload(MesProProcessPoolEventDO event) {
        if (StrUtil.isBlank(event.getRawPayload())) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_SOURCE_VALUE_REQUIRED, event.getId(), "*");
        }
        try {
            return JsonUtils.getObjectMapper().readTree(event.getRawPayload());
        } catch (Exception ex) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_SOURCE_VALUE_REQUIRED, event.getId(), "*");
        }
    }

    private Map<String, SnapshotField> snapshotFields(String executionSnapshotJson) {
        Map<String, SnapshotField> result = new LinkedHashMap<>();
        try {
            JsonNode fields = JsonUtils.getObjectMapper().readTree(executionSnapshotJson).path("fields");
            if (!fields.isArray()) {
                return result;
            }
            for (JsonNode field : fields) {
                Integer rowIndex = integer(field, "rowIndex");
                Integer columnIndex = integer(field, "columnIndex");
                String fieldPath = text(field, "fieldPath");
                String fieldKey = text(field, "fieldKey");
                if (rowIndex != null && columnIndex != null && StrUtil.isNotBlank(fieldPath)
                        && StrUtil.isNotBlank(fieldKey)) {
                    result.put(cellKey(rowIndex, columnIndex), new SnapshotField(fieldPath, fieldKey,
                            valueType(text(field, "valueType")), snapshotDefaultValue(field)));
                }
            }
            return result;
        } catch (Exception ex) {
            return result;
        }
    }

    private Map<String, JsonNode> currentValues(String cellValuesJson) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        if (StrUtil.isBlank(cellValuesJson)) {
            return result;
        }
        try {
            JsonNode cells = JsonUtils.getObjectMapper().readTree(cellValuesJson);
            if (!cells.isArray()) {
                return result;
            }
            for (JsonNode cell : cells) {
                Integer rowIndex = integer(cell, "rowIndex");
                Integer columnIndex = integer(cell, "columnIndex");
                if (rowIndex != null && columnIndex != null) {
                    JsonNode value = cell.get("value");
                    result.put(cellKey(rowIndex, columnIndex),
                            value == null || value.isMissingNode() ? NullNode.instance : value.deepCopy());
                }
            }
            return result;
        } catch (Exception ex) {
            return result;
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

    private String oldValueHash(MesProBatchRecordExecutionFieldAuditValueType valueType,
                                JsonNode currentValue,
                                JsonNode defaultValue) {
        JsonNode oldValue = oldValueNode(currentValue == null ? defaultValue : currentValue, valueType);
        String canonical = oldValue == null || oldValue.isNull() || oldValue.isMissingNode()
                ? "null"
                : MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(valueType, oldValue);
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
            if (valueType == MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN) {
                return BooleanNode.FALSE;
            }
            return NullNode.instance;
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.NUMBER) {
            return DecimalNode.valueOf(MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(new BigDecimal(text)));
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN) {
            return JsonNodeFactory.instance.booleanNode(Boolean.parseBoolean(text));
        }
        return TextNode.valueOf(text);
    }

    private MesProBatchRecordExecutionFieldAuditValueType valueType(MesProBatchRecordCellLinkRuleDO rule,
                                                                    SnapshotField field) {
        String raw = StrUtil.blankToDefault(rule.getTargetValueType(),
                field.valueType() == null ? null : field.valueType().name());
        if (StrUtil.isBlank(raw)) {
            return MesProBatchRecordExecutionFieldAuditValueType.STRING;
        }
        try {
            return MesProBatchRecordExecutionFieldAuditValueType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED,
                    rule.getScopeId(), rule.getTargetReportId(), rule.getTargetCellKey());
        }
    }

    private MesProBatchRecordExecutionFieldAuditValueType valueType(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            return MesProBatchRecordExecutionFieldAuditValueType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Object normalizeValue(MesProBatchRecordExecutionFieldAuditValueType valueType, Object value) {
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.NUMBER) {
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            return new BigDecimal(String.valueOf(value));
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN && value instanceof Boolean bool) {
            return bool;
        }
        return String.valueOf(value);
    }

    private String displayValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private Map<Long, MesProProcessPoolEventDO> sourceEventMap(List<MesProProcessPoolEventDO> sourceEvents) {
        Map<Long, MesProProcessPoolEventDO> result = new LinkedHashMap<>();
        for (MesProProcessPoolEventDO sourceEvent : sourceEvents) {
            result.putIfAbsent(sourceEvent.getId(), sourceEvent);
        }
        return result;
    }

    private String aggregateHash(MesTeamLeaderBatchRecordBackfillCommand command,
                                 List<MesProProcessPoolEventDO> sourceEvents,
                                 List<MesProcessPoolReportAllocationDO> allocations) {
        if (StrUtil.isNotBlank(command.getAggregateHash())) {
            return StrUtil.trim(command.getAggregateHash());
        }
        if (sourceEvents.size() == 1 && allocations.size() == 1) {
            return "agg-single-" + sourceEvents.get(0).getId() + "-" + allocations.get(0).getId();
        }
        StringBuilder canonical = new StringBuilder();
        for (MesProProcessPoolEventDO sourceEvent : sourceEvents) {
            canonical.append("event:")
                    .append(sourceEvent.getId()).append('|')
                    .append(sourceEvent.getRawPayload() == null ? "" : sourceEvent.getRawPayload()).append('\n');
        }
        for (MesProcessPoolReportAllocationDO sourceAllocation : allocations) {
            canonical.append("allocation:")
                    .append(sourceAllocation.getId()).append('|')
                    .append(sourceAllocation.getEventId()).append('|')
                    .append(sourceAllocation.getAllocatedQuantity()).append('|')
                    .append(sourceAllocation.getConfirmedAt()).append('\n');
        }
        return sha256(canonical.toString());
    }

    private String idempotencyKey(MesTeamLeaderBatchRecordBackfillCommand command,
                                  MesProProcessPoolEventDO event,
                                  MesProcessPoolReportAllocationDO allocation,
                                  String aggregateHash) {
        if (StrUtil.isNotBlank(command.getIdempotencyKey())) {
            return StrUtil.trim(command.getIdempotencyKey());
        }
        return "PROCESS_POOL_REPORT_BACKFILL_AGG:" + allocation.getWorkOrderId()
                + ":" + event.getRouteProcessId()
                + ":" + event.getProcessId()
                + ":" + aggregateHash;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private static String cellKey(Integer rowIndex, Integer columnIndex) {
        return rowIndex + ":" + columnIndex;
    }

    private static Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || !value.isNumber() ? null : value.intValue();
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private record SnapshotField(String fieldPath, String fieldKey,
                                 MesProBatchRecordExecutionFieldAuditValueType valueType,
                                 JsonNode defaultValue) {
    }
}
