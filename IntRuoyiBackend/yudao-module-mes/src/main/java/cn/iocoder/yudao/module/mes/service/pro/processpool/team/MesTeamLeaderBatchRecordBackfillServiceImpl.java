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
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditValueType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
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
        JsonNode payload = rawPayload(event);
        List<MesProBatchRecordExecutionFieldAuditChange> changes = rules.stream()
                .map(rule -> toChange(event, allocation, binding, rule, fields, payload))
                .toList();
        MesProBatchRecordExecutionFieldAuditSaveResult saveResult = fieldAuditService.saveSystemCellLinkChanges(
                new MesProBatchRecordExecutionFieldAuditSaveChangesCommand()
                        .setExecutionId(execution.getId())
                        .setIdempotencyKey(idempotencyKey(event, allocation))
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
                || command.getWorkOrder() == null || command.getEvent().getRouteProcessId() == null
                || command.getEvent().getProcessId() == null || command.getAllocation().getWorkOrderId() == null
                || command.getWorkOrder().getId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "batchRecordBackfill");
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

    private MesProBatchRecordExecutionFieldAuditChange toChange(MesProProcessPoolEventDO event,
                                                                MesProcessPoolReportAllocationDO allocation,
                                                                MesProRouteFlowProcessBatchRecordDO binding,
                                                                MesProBatchRecordCellLinkRuleDO rule,
                                                                Map<String, SnapshotField> fields,
                                                                JsonNode payload) {
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
        Object value = sourceValue(event, allocation, rule.getSourceFieldCode(), payload);
        MesProBatchRecordExecutionFieldAuditValueType valueType = valueType(rule, field);
        Object normalized = normalizeValue(valueType, value);
        return new MesProBatchRecordExecutionFieldAuditChange()
                .setFieldPath(field.fieldPath())
                .setFieldKey(field.fieldKey())
                .setRowIndex(rule.getTargetRowIndex())
                .setColumnIndex(rule.getTargetColumnIndex())
                .setValueType(valueType)
                .setNewValueJson(normalized)
                .setNewValueDisplay(displayValue(normalized));
    }

    private Object sourceValue(MesProProcessPoolEventDO event, MesProcessPoolReportAllocationDO allocation,
                               String sourceFieldCode, JsonNode payload) {
        if ("allocatedQuantity".equals(sourceFieldCode)) {
            return allocation.getAllocatedQuantity();
        }
        JsonNode node = payload.get(sourceFieldCode);
        if (node == null || node.isNull() || (node.isTextual() && StrUtil.isBlank(node.asText()))) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_SOURCE_VALUE_REQUIRED, event.getId(), sourceFieldCode);
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
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
                            valueType(text(field, "valueType"))));
                }
            }
            return result;
        } catch (Exception ex) {
            return result;
        }
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

    private String idempotencyKey(MesProProcessPoolEventDO event, MesProcessPoolReportAllocationDO allocation) {
        return "PROCESS_POOL_REPORT_BACKFILL:" + event.getId() + ":" + allocation.getWorkOrderId()
                + ":" + event.getRouteProcessId();
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
                                 MesProBatchRecordExecutionFieldAuditValueType valueType) {
    }
}
